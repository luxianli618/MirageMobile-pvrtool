package pvrtool;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import potato.movie.MovieBean;
import sf.res.ResBean;
import sf.res.SubBean;
import utils.EncodingDetect;
import utils.ImageUtil;
import zlib.ZLibUtils;
import flash.geom.Rectangle;
import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Output;
import flex.messaging.io.amf.AmfTrace;

/**
 *
 * @author Administrator
 */
public class Main {

	private static final int CONFIGITEMNUM = 5;  //配置文件的最小项目数

	// 格式前缀和索引，从0开始
	private static final String[] PLAT_PREFIX = {"pvr", "dxt", "atc", "png"};
	private static final int[] PLAT_INDEX = {0, 1, 2, 3};
	
	private static final String PC_PLAT_PREFIX = "pc";
	
	///////////// 配置文件里可用的文件路径常量 ///////////////
	private static final String COMMON = "@common";			//通用资源的文件夹，会被替换为 common
	private static final String LANGUAGE = "@language";		//语言地区，会被替换为 -l 参数传入的值
	private static final String RESOURCE = "@resource";		//资源格式，会被替换为 pvr, dxt, atc, png 中的一个
	
	//"C:/PROGRA~2/TexturePacker/bin/TexturePacker.exe" -t "C:/PROGRA~2/AMD/THECOM~1.50/TheCompressonator.exe"
	private static String TEXTURE = "C:\\PROGRA~2\\TexturePacker\\bin\\TexturePacker.exe";
	private static String TXTCOM = "C:\\PROGRA~2\\AMD\\THECOM~1.50\\TheCompressonator.exe";
	private static String SUNDFF = "C:\\PROGRA~2\\FreeTime\\FormatFactory\\FormatFactory.exe";

	private static boolean onlyConfig = false;
	private static SerializationContext context;

	private static String selPlat = null;
	private static List<String> selPlatList = null;
	private static int exeVer = 2;
	private static String language = "zh_CN"; //语言，一次只能处理一种

	// 原始图片的md5值，用来判断是否重新生成
	private static Map<String, FileChange> md5Map;
	private static int md5FileVer = 0;
	private static final int CURRMD5FILEVER = 1;
//	// 当前图片的md5值，用来判断是否重新生成
//	private static Map<String, String> md5MapCurr;
	
	private static int processNum;		//已经处理的文件数


	private String lastResId;

	private Hashtable<String, SubBean> subTexDic = new Hashtable<String, SubBean>();
	private Hashtable<String, ResBean> resDic = new Hashtable<String, ResBean>();

	private Hashtable<String, ResBean> configs = new Hashtable<String, ResBean>();
	
	private String mainCfg;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		context = new SerializationContext();
		context.legacyCollection = true;
		
		if (args.length < 4) {
			System.out.println("必须传入主配置文件和输出文件夹。");
			return;
		}

		String mainCfg = "";
		String outDir = "";
		String srcDir = "";

		for (int i = 0; i < args.length; i++) {
			if (args[i].trim().equals("-c")) {			//主配置文件
				mainCfg = args[++i];
			} else if (args[i].trim().equals("-s")) {	//输入文件夹
				srcDir = args[++i];
			} else if (args[i].trim().equals("-o")) {	//输出文件夹
				outDir = args[++i];
			} else if (args[i].trim().equals("-p")) {	//TexturePacker路径
				TEXTURE = args[++i];
			} else if (args[i].trim().equals("-t")) {	//The Compressonator路径
				TXTCOM = args[++i];
			} else if (args[i].trim().equals("-m")) {	//FormatFactory路径
				SUNDFF = args[++i];
			} else if (args[i].trim().equals("-l")) {	//处理的语言，默认为 zh_CN
				language = args[++i];
			} else if (args[i].trim().equals("-co")) {	//config only, 只处理配置文件
				onlyConfig = true;
			} else if (args[i].trim().equals("-f")) {	//只处理设置格式， 用逗号分割
				selPlat = args[++i];
				if (selPlat == null || selPlat.length() == 0) {
					printErr(args[i]);
					return;
				}
				String[] sa = selPlat.split(",");
				if (sa.length > 0) {
					selPlatList = Arrays.asList(sa);
				}
			} else if (args[i].trim().equals("-v")) {	//使用3.0+版本
				String sv = args[++i];
				if (sv == null || sv.length() == 0) {
					printErr(args[i]);
					return;
				}
				exeVer = Integer.parseInt(sv);
			} else {
				printErr(args[i]);
				return;
			}
		}

		if (selPlatList == null || selPlatList.isEmpty()) {
			selPlatList = Arrays.asList(PLAT_PREFIX);
		}

		System.out.println("主配置文件：" + mainCfg + "， 输入文件夹：" + srcDir + "， 输出文件夹：" + outDir);

		for (int pi=0; pi<PLAT_PREFIX.length; pi++) {
			if (selPlatList.indexOf(PLAT_PREFIX[pi]) >= 0) {
				// 初始化md5
				initMd5(srcDir, pi);
//				md5MapCurr = new HashMap<String, String>();
				
				String packDir = "/" + language + "_" + PLAT_PREFIX[pi];
				File cf = new File(mainCfg);
				String cfName = cf.getName();
				String dstFile = language + "/" + PLAT_PREFIX[pi] + "/" + cfName;

				new Main().run(mainCfg, srcDir, outDir + packDir, pi, dstFile);

//				writeMd5ToFile(srcDir, pi);
				if (md5Fw != null) {
					try {
						md5Fw.close();
						md5Fw = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if (!onlyConfig) { 
					File md5src = new File(srcDir + File.separator + "convert" + pi + ".md5");
					if (md5src.exists()) md5src.delete();
					File md5File = new File(srcDir + File.separator + "convert" + pi + ".md5_");
					md5File.renameTo(md5src);
				}
			}
		}
	}

	private static void printErr(String s) {
		System.out.println("参数错误。[" + s + "]");
		System.out.println("可接收参数:");
		System.out.println("-c 配置文件名");
		System.out.println("-s 输入文件夹，一般选择资源的根目录");
		System.out.println("-o 输出文件夹");
		System.out.println("-l 语言和地区，默认为zh_CN。一次只能处理一种语言版本");
		System.out.println("-p TexturePacker路径, 不带该参数为TexturePacker默认安装路径（win7）");
		System.out.println("-t The Compressonator路径, 不带该参数为The Compressonator默认安装路径（win7）");
		System.out.println("-m FormatFactory路径, 不带该参数为FormatFactory默认安装路径（win7）");
		System.out.println("-co (只处理配置文件。后面不需参数值)");
		System.out.println("-f pvr,dxt,atc,png(只处理指定格式，用逗号分割)");
		System.out.println("-v 3表示支持新版3.0+，默认是2.4");
	}

	private static void initMd5(String srcDir, int platIndex) {
		md5Map = new HashMap<String, FileChange>();
		LineNumberReader lnr = null;
		try {
			String md5FilePath = srcDir + File.separator + "convert" + platIndex + ".md5";
			if (new File(md5FilePath).exists() == false) {
				md5FileVer = CURRMD5FILEVER;		//默认文件版本
				return;
			}
			
			lnr = new LineNumberReader(new FileReader(md5FilePath));
			String s;
			String v;
			FileChange fc;
			final int LEN = 3;
			String t[] = new String[LEN];
			while ((s = lnr.readLine()) != null) {
				if (s.startsWith("FC.VER=")) {	//附带版本号
					md5FileVer = Integer.parseInt(s.replace("FC.VER=", ""));
					continue;
				}
				fc = new FileChange();
				switch (md5FileVer) {
				case 0:
					fc.md5 = lnr.readLine();
					break;
				case 1:
					v = lnr.readLine();
					t = v.split(",");
					if (LEN != t.length) {
						throw new Exception("md5文件"+md5FilePath+"的项目数不对：[" + lnr + "], len="+LEN);
					}
					fc = new FileChange();
					fc.md5 = t[0];
					fc.type = Integer.parseInt(t[1]);
					fc.platType = Integer.parseInt(t[2]);
					break;
				}
					
				md5Map.put(s, fc);
			}
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (lnr != null) lnr.close();
			} catch (IOException ex) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private static FileWriter md5Fw = null;
	private static void writeMd5ToFile(String srcDir, int platIndex, String srcPath) {
		try {
			if (md5Fw == null) {
				File md5File = new File(srcDir + File.separator + "convert" + platIndex + ".md5_");
				if (md5File.exists()) md5File.delete();
				md5Fw = new FileWriter(md5File, true);
				md5Fw.write("FC.VER="+CURRMD5FILEVER+"\n");
			}
//			Iterator<String> it = md5MapCurr.keySet().iterator();
//			String key;
//			while (it.hasNext()) {
//				key = it.next();
			FileChange fc = md5Map.get(srcPath);
				String fcstr = fc.md5 + "," + fc.type + "," + fc.platType;
				md5Fw.write(srcPath+"\n");
				md5Fw.write(fcstr+"\n");
//				md5Fw.append(md5MapCurr.get(key)+"\n");
//			}
			md5Fw.flush();
//			md5Fw.close();
		} catch (IOException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * 
	 * @param mainCfg 资源配置文件的绝对路径
	 * @param srcDir
	 * @param outDir
	 * @param platIndex
	 * @param dstFile 处理完后资源配置文件的相对路径
	 */
	private void run(String mainCfg, String srcDir, String outDir, int platIndex, String dstFile) {
		this.mainCfg = mainCfg;
		try {
			String atlasFile = dstFile.substring(0, dstFile.lastIndexOf(".")) + "_a.mbf";

			//分析配置
			parseCfg(platIndex);

			File pcDir = new File(srcDir);
			if (!pcDir.exists() || !pcDir.isDirectory()) {
				System.out.println("没有找到输入文件夹[" + pcDir + "]");
				return;
			}

			//创建对应平台的文件夹
			String platPath = outDir  + File.separator + language + File.separator + PLAT_PREFIX[platIndex];
			File platDir = new File(platPath);
			platDir.mkdirs();
			
			File df = new File(outDir + File.separator + dstFile);
			df.getParentFile().mkdirs();

			Iterator<String> it = configs.keySet().iterator();
			while (it.hasNext()) {
				process(it.next(), pcDir, outDir, platIndex);
			}

			// amf
			if (subTexDic.size() > 0) {
				resDic.get(lastResId).atlas = atlasFile;
				writeObject(subTexDic, outDir + File.separator + atlasFile);

				////// 将 _a.mbf 文件也加到资源配置里，这条用来更新这个配置文件
				ResBean cb = new ResBean();
				cb.id = atlasFile;
				cb.path = atlasFile;
				cb.cache = 0;
				cb.sType(0);
				resDic.put(cb.id, cb);
			}

			//// 写
			writeObject(resDic, outDir + File.separator + dstFile);
			
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(1);
			return;
		}
	}

	private void writeObject(Object o, String file) {
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();

		Amf3Output avmPlusOutput = new Amf3Output(context);
        avmPlusOutput.setOutputStream(outBuffer);
        avmPlusOutput.setDebugTrace(new AmfTrace());

		System.out.println("****生成配置文件: " + file);

		try {
			avmPlusOutput.writeObject(o);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ZLibUtils.compress(outBuffer.toByteArray(), out);

			FileOutputStream fos = new FileOutputStream(file);
			out.writeTo(fos);
			fos.close();
			avmPlusOutput.close();

			//未压缩版
			FileOutputStream fos1 = new FileOutputStream(file+"1");
			fos1.write(outBuffer.toByteArray());
			fos1.close();
		} catch (IOException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private boolean checkFileChange(String srcPath, int type, int platType, String md5) {
		if (!md5Map.containsKey(srcPath)) {
			FileChange fc = new FileChange();
			fc.md5 = md5;
			fc.type = type;
			fc.platType = platType; 
			md5Map.put(srcPath, fc);
			return true;
		}
		FileChange fc = md5Map.get(srcPath);
		if (md5FileVer == 0) {	//版本0时没有写入类型
			fc.type = type;
			fc.platType = platType;
		}
		if (!fc.md5.equals(md5) || fc.type != type || fc.platType != platType) {
			// 写入新值
			fc.md5 = md5;
			fc.type = type;
			fc.platType = platType;
			return true;
		}
		return false;
	}

	private void process(String next, File srcDir, String outDir, int platIndex) throws IOException, InterruptedException, Exception {
		ResBean cfg = configs.get(next);
		String srcPath = cfg.path;
		
		ResBean crb = resDic.get(next);
		String desPath = crb.path;

		System.out.println("----处理文件: " + srcPath);

		if (cfg.gType() == 1 || cfg.gType() == 4) {	//资源类型
			if (!onlyConfig) {

				String srcFilePath = srcDir + File.separator + srcPath;
				File srcFile = new File(srcFilePath);

//				System.out.println("+++++++++++++++" + getFileMD5String(srcFile));
				String md5 = getFileMD5String(srcFile);
				
				int pType = cfg.gPlatTypes()[PLAT_INDEX[platIndex]];
				if (checkFileChange(srcPath, cfg.gType(), pType, md5)) {
//				if (!md5Map.containsKey(srcPath) || !md5.equals(md5Map.get(srcPath))) {
					if ("pvr".equals(PLAT_PREFIX[platIndex])) {
						processPvr(srcDir + File.separator + srcPath, outDir + File.separator + desPath, pType);
					} else if ("dxt".equals(PLAT_PREFIX[platIndex])) {
						processDxt(srcDir + File.separator + srcPath, outDir + File.separator + desPath, pType);
					} else if ("atc".equals(PLAT_PREFIX[platIndex])) {
						processAtc(srcDir + File.separator + srcPath, outDir + File.separator + desPath, pType);
					} else if ("png".equals(PLAT_PREFIX[platIndex])) {
						processPng(srcDir + File.separator + srcPath, outDir + File.separator + desPath, pType);
					}
					
				}
				
				//// 每处理一个文件都更新md5文件
//				md5MapCurr.put(srcPath, md5);
				writeMd5ToFile(srcDir.getPath(), platIndex, srcPath);
				
				// 需要压缩
				if (cfg.gType() == 4) {
					processZlib(outDir + File.separator + desPath, outDir + File.separator + desPath, true);
				}
			}
		} else if (cfg.gType() == 0) {	//配置文件类型
			new Main().run(srcDir + File.separator + srcPath, srcDir.getAbsolutePath(), outDir, platIndex, desPath);
		} else if (cfg.gType() == 2) {	//字符串配置文件
			processLangCfg(srcDir + File.separator + srcPath, outDir + File.separator + desPath);
		} else if (cfg.gType() == 3) {	//动画配置文件
			processMovieCfg(srcDir + File.separator + srcPath, outDir + File.separator + desPath);
		} else if (cfg.gType() == 5) {	//zlib压缩文件，这个类型处理完后会在原文件名后附加m3z扩展名，在flash下载后去掉这个扩展名
			if (!onlyConfig) {
				processZlib(srcDir + File.separator + srcPath, outDir + File.separator + desPath, false);
			}
		} else if (cfg.gType() == 6) {	//音乐文件转mp4
			if (!onlyConfig) {
				String srcFilePath = srcDir + File.separator + srcPath;
				File srcFile = new File(srcFilePath);
				String md5 = getFileMD5String(srcFile);
//				if (!md5Map.containsKey(srcPath) || !md5.equals(md5Map.get(srcPath))) {
				int pType = cfg.gPlatTypes()[PLAT_INDEX[platIndex]];
				if (checkFileChange(srcPath, cfg.gType(), pType, md5)) {
					processSound(srcDir + File.separator + srcPath, outDir + File.separator + desPath);
				}
				
				//// 每处理一个文件都更新md5文件
//				md5MapCurr.put(srcPath, md5);
				writeMd5ToFile(srcDir.getPath(), platIndex, srcPath);
			}
		} else if (cfg.gType() == 8) { // 8拷贝类型
			if (!onlyConfig) {
				FileUtil.copyFileByName(srcDir + File.separator + srcPath, outDir + File.separator + desPath, true);
			}
		} else {	//9忽略类型
			
		}
		
		System.out.println("*##*"+processNum++);

		// 处理atlas文件
		if (cfg.atlas != null && cfg.atlas.length() > 0) {
			String atlasPath = srcDir + File.separator + cfg.atlas;
			File atlasFile = new File(atlasPath);
			if (atlasFile.exists() == false) {
				throw new Exception("Atlas 配置的文件不存在。["+cfg.atlas+"]");
			} else {
				processAtlas(atlasFile, cfg.id);
			}
		}
	}

	/**
	 * 声音文件
	 * @param path
	 * @param desPath
	 * @throws Exception 
	 */
	private void processSound(String path, String desPath) throws Exception {
		//先创建文件夹
		File pd = new File(desPath).getParentFile();
		if (!pd.exists()) {
			pd.mkdirs();
		}
		File desFile = new File(desPath); 
		if (desFile.exists()) {
			desFile.delete();
		}
		
//		String mp4Path = desPath;
//		boolean noMp4Ext = false;
//		if (desPath.indexOf(".m4a") <= 0) {
//			mp4Path = desPath + ".m4a";
//			noMp4Ext = true;
//		}
		

		/////// mp4 格式
//		cmd = SUNDFF + " \"-> M4A\" \"High quality\" ";
//		cmd += "\"" + path + "\" \"" + "c:/ddd.m4a\"";
		List<String> command = new ArrayList<String>();
		command.add(SUNDFF);
		command.add("-> M4A");
		command.add("High quality");
		command.add(path);
		command.add(desPath);
		ProcessBuilder pb = new ProcessBuilder(command);
		Process p = pb.start();
		
		p.waitFor();
		Thread.sleep(1);
		
//		if (noMp4Ext) {
//			FileUtil.copyFileByName(mp4Path, desPath, true);
//			FileUtil.delete(mp4Path);
//		}

		if (new File(desPath).exists() == false) {
			System.out.println(so + "\n" + se);
			throw new Exception("生成[" + desPath + "]出错。");
		}
	}

	/**
	 * 
	 * @param string
	 * @param string2
	 * @throws Exception 
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	private void processLangCfg(String path, String desPath) throws NumberFormatException, IOException, Exception {
		Hashtable<String, String> mdic = new Hashtable<String, String>();
		boolean isUtf = false;
		
		String ext = "";
		String langPath = path;
		int id = path.lastIndexOf(".");
		if (id > 0) {
			langPath = path.substring(0, id);
			ext = path.substring(id);
		}
		langPath = langPath + "_" + language + ext;
		File langCfg = new File(langPath);
		if (!langCfg.exists()) {
			langCfg = new File(path);
		}

		//判断文件是不是 UTF-8
		EncodingDetect ed = new EncodingDetect();
		int ei = ed.detectEncoding(langCfg);
		if (ei == 6) { // utf-8
			isUtf = true;
		}

		BufferedReader lnr = null;
		InputStreamReader read;
		if (isUtf) {
			read = new InputStreamReader(new FileInputStream(langCfg), "utf-8");
		} else {
			read = new InputStreamReader(new FileInputStream(langCfg));
		}
		lnr = new BufferedReader(read);
		String ln;
		while ((ln = lnr.readLine()) != null) {
			ln = ln.trim();
			if (ln.startsWith("#") || ln.length() == 0) {
				continue;
			}

			String[] sc = ln.split("	");
			if (sc.length < 2) {
				throw new Exception("lang配置文件"+path+"的项目数不对：[" + ln + "], len="+sc.length);
			}

			String key = sc[0].trim();
			if (key.length() == 0) {
				throw new Exception("lang配置文件"+path+"的key为空：[" + ln + "]");
			}
			String value = sc[1];
			
			if (mdic.containsKey(key)) {
				System.out.println("***** lang配置文件"+path+"的key重复：[" + ln + "]");
			}

			mdic.put(key, value);
		}
		read.close();
		lnr.close();
		
		// 保存的文件
		String extd = "";
		int idd = desPath.lastIndexOf(".");
		if (id > 0) {
			extd = desPath.substring(idd);
			desPath = desPath.substring(0, idd);
		}
		desPath = desPath + "_" + language + extd;
		//写入文件
		writeObject(mdic, desPath);
	}

	/**
	 * 需要zlib压缩的文件
	 * @param path
	 * @param desPath
	 * @throws Exception
	 */
	private void processZlib(String path, String desPath, boolean delSrc) throws Exception {
		FileInputStream is = new FileInputStream(path);
		byte[] ba = new byte[is.available()];
		is.read(ba, 0, is.available());
		is.close();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZLibUtils.compress(ba, out);
		
		FileOutputStream fos = new FileOutputStream(desPath+".m3z");
		fos.write(out.toByteArray());
		fos.close();
		
		if (delSrc) {
			new File(path).delete();
		}
	}

	/**
	 * 处理动画配置文件
	 * @param path
	 * @param desPath
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws Exception
	 */
	private void processMovieCfg(String path, String desPath) throws NumberFormatException, IOException, Exception {
		Hashtable<String, MovieBean> mdic = new Hashtable<String, MovieBean>();
		boolean isUtf = false;

		//判断文件是不是 UTF-8
		EncodingDetect ed = new EncodingDetect();
		int ei = ed.detectEncoding(new File(path));
		if (ei == 6) { // utf-8
			isUtf = true;
		}

		BufferedReader lnr = null;
		InputStreamReader read;
		if (isUtf) {
			read = new InputStreamReader(new FileInputStream(path), "utf-8");
		} else {
			read = new InputStreamReader(new FileInputStream(path));
		}
		lnr = new BufferedReader(read);
		String ln;
		int i = 0;
		while ((ln = lnr.readLine()) != null) {
			ln = ln.trim();
			if (ln.startsWith("#") || ln.length() == 0) {
				continue;
			}

			String[] sc = ln.split("	");
			if (sc.length < 5) {
				throw new Exception("movie配置文件"+path+"的项目数不对：[" + ln + "], len="+sc.length);
			}

			MovieBean cb = new MovieBean();
			if (i == 0) {
				i++;
				continue;	//头一行是列名
			}
			String movieName = sc[0].trim();
			if (movieName.length() == 0) {
				throw new Exception("movie配置文件"+path+"的movieName为空：[" + ln + "]");
			}
			
			cb.movieName = movieName;
			cb.frameNumber = Integer.parseInt(sc[1]);
			cb.speed = Integer.parseInt(sc[2]);
			cb.footX = Integer.parseInt(sc[3]);
			cb.footY = Integer.parseInt(sc[4]);
			if (sc.length >= 6 && sc[5] != null && sc[5].length() > 0) {
				cb.dirNumber = Integer.parseInt(sc[5]);
			}
			cb.allFrame = 0;
			cb.frameArr = new int[cb.frameNumber];
			if (sc.length >= 8 && sc[7] != null && sc[7].length() > 0) {
				String[] sa = sc[7].split(",");
				if (sa.length != cb.frameNumber) {
					throw new Exception("movie配置文件"+path+"的frameArr项目数不对：[" + ln + "]");
				}
				int frameNum;
				for (int m=0; m<cb.frameNumber; m++) {
					frameNum = Integer.parseInt(sa[m]);
					cb.frameArr[m] = cb.speed * frameNum;
					cb.allFrame += Math.abs(frameNum);
				}
			} else {
				for (int m=0; m<cb.frameNumber; m++) {
					cb.frameArr[m] = cb.speed;
				}
				cb.allFrame = cb.frameNumber;
			}
			
			if (sc.length >= 9 && sc[8] != null && sc[8].length() > 0) {
				cb.scale = Double.parseDouble(sc[8]);
			}

			if (mdic.containsKey(movieName)) {
				System.out.println("***** movie配置文件"+path+"的name重复：[" + ln + "]");
			}

			mdic.put(movieName, cb);
		}
		read.close();
		lnr.close();
		
		//写入文件
		writeObject(mdic, desPath);
	}

	private void processPvr(String path, String desPath, int pType) throws IOException, InterruptedException, Exception {
		switch (pType) {
			case 1: {
				runCmdPvr(path, desPath, 1);
				break;
			}
			case 2: {
				FileUtil.copyFileByName(path, desPath, true);
				break;
			}
			case 3: {
				runCmdPvr(path, desPath, 3);
				break;
			}
			case 4: {
				runCmdPvr(path, desPath, 4);
				break;
			}
			case 5: {
				runCmdPvr(path, desPath, 5);
				break;
			}
			case 6: {
				runCmdPvr(path, desPath, 1);
				break;
			}
			case 7: {
				runCmdPvr(path, desPath, 10);
				break;
			}
			case 8: {
				runCmdPvr(path, desPath, 13);
				break;
			}
			case 9: {
				ImageUtil.toJpz(path, desPath);
				break;
			}
			case 10: {
				ImageUtil.toJpg(path, desPath);
				break;
			}
		}
	}

	private void processDxt(String path, String desPath, int pType) throws IOException, InterruptedException, Exception {
		switch (pType) {
			case 1: {
				runCmdDxt(path, desPath);
				break;
			}
			case 2: {
				FileUtil.copyFileByName(path, desPath, true);
				break;
			}
			case 3: {
				runCmdDxt(path, desPath);
				break;
			}
			case 4: {
				runCmdDxt(path, desPath);
				break;
			}
			case 5: {
				runCmdEtc(path, desPath);
				break;
			}
			case 6: {
				runCmdDxt(path, desPath);
				break;
			}
			case 7: {
				runCmdPvr(path, desPath, 10);
				break;
			}
			case 8: {
				runCmdPvr(path, desPath, 13);
				break;
			}
			case 9: {
				ImageUtil.toJpz(path, desPath);
				break;
			}
			case 10: {
				ImageUtil.toJpg(path, desPath);
				break;
			}
		}
	}

	private void processAtc(String path, String desPath, int pType) throws IOException, InterruptedException, Exception {
		switch (pType) {
			case 1: {
				runCmdAtc(path, desPath);
				break;
			}
			case 2: {
				FileUtil.copyFileByName(path, desPath, true);
				break;
			}
			case 3: {
				runCmdAtc(path, desPath);
				break;
			}
			case 4: {
				runCmdAtc(path, desPath);
				break;
			}
			case 5: {
				runCmdEtc(path, desPath);
				break;
			}
			case 6: {
				runCmdAtc(path, desPath);
				break;
			}
			case 7: {
				runCmdPvr(path, desPath, 10);
				break;
			}
			case 8: {
				runCmdPvr(path, desPath, 13);
				break;
			}
			case 9: {
				ImageUtil.toJpz(path, desPath);
				break;
			}
			case 10: {
				ImageUtil.toJpg(path, desPath);
				break;
			}
		}
	}

	private void processPng(String path, String desPath, int pType) throws IOException, InterruptedException, Exception {
		switch (pType) {
			case 1: {
				FileUtil.copyFileByName(path, desPath, true);
				break;
			}
			case 2: {
				FileUtil.copyFileByName(path, desPath, true);
				break;
			}
			case 3: {
				runCmdPvr(path, desPath, 10);
				break;
			}
			case 4: {
				runCmdPvr(path, desPath, 11);
				break;
			}
			case 5: {
				runCmdEtc(path, desPath);
				break;
			}
			case 6: {
				runCmdPvr(path, desPath, 12);
				break;
			}
			case 7: {
				runCmdPvr(path, desPath, 10);
				break;
			}
			case 8: {
				runCmdPvr(path, desPath, 13);
				break;
			}
			case 9: {
				ImageUtil.toJpz(path, desPath);
				break;
			}
			case 10: {
				ImageUtil.toJpg(path, desPath);
				break;
			}
		}
	}

	private static String so;
	private static String se;

	private void runCmdAtc(String path, String desPath) throws IOException, InterruptedException, Exception {
		//先创建文件夹
		File pd = new File(desPath).getParentFile();
		if (!pd.exists()) {
			pd.mkdirs();
		}

		String cmd = TXTCOM + " -convert ";
		cmd += path + " " + desPath;
		cmd += " -codec ATICompressor.dll +speed Normal +fourCC ATCI ";

		Process p = Runtime.getRuntime().exec(cmd);
		so = "";
		se = "";
		//获取进程的标准输入流
		final InputStream is1 = p.getInputStream();
		//获取进城的错误流
		final InputStream is2 = p.getErrorStream();
		//启动两个线程，一个线程负责读标准输出流，另一个负责读标准错误流
		new Thread() {
			public void run() {
				BufferedReader br1 = new BufferedReader(new InputStreamReader(is1));
				try {
					String line1 = null;
					while ((line1 = br1.readLine()) != null) {
						if (line1 != null) {
							so += line1;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is1.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();

		new Thread() {
			public void run() {
				BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
				try {
					String line2 = null;
					while ((line2 = br2.readLine()) != null) {
						if (line2 != null) {
							so += line2;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is2.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();

		p.waitFor();
		Thread.sleep(1);

		if (new File(desPath).exists() == false) {
			System.out.println(so + "\n" + se);
			throw new Exception("生成[" + desPath + "]出错。");
		}
	}

	private void runCmdDxt(String path, String desPath) throws IOException, InterruptedException, Exception {
		//先创建文件夹
		File pd = new File(desPath).getParentFile();
		if (!pd.exists()) {
			pd.mkdirs();
		}

		String cmd = TXTCOM + " -convert ";
		cmd += path + " " + desPath;
		cmd += " -codec ATICompressor.dll +speed Normal +fourCC DXT3 ";

//		System.out.println(cmd);

		Process p = Runtime.getRuntime().exec(cmd);
		so = "";
		se = "";
		//获取进程的标准输入流
		final InputStream is1 = p.getInputStream();
		//获取进城的错误流
		final InputStream is2 = p.getErrorStream();
		//启动两个线程，一个线程负责读标准输出流，另一个负责读标准错误流
		new Thread() {
			public void run() {
				BufferedReader br1 = new BufferedReader(new InputStreamReader(is1));
				try {
					String line1 = null;
					while ((line1 = br1.readLine()) != null) {
						if (line1 != null) {
							so += line1;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is1.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();

		new Thread() {
			public void run() {
				BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
				try {
					String line2 = null;
					while ((line2 = br2.readLine()) != null) {
						if (line2 != null) {
							so += line2;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is2.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();

		p.waitFor();
		Thread.sleep(1);

		if (new File(desPath).exists() == false) {
			System.out.println(so + "\n" + se);
			throw new Exception("生成[" + desPath + "]出错。");
		}
	}

	private void runCmdEtc(String path, String desPath) throws IOException, InterruptedException, Exception {
		//先创建文件夹
		File pd = new File(desPath).getParentFile();
		if (!pd.exists()) {
			pd.mkdirs();
		}

		String cmd = TXTCOM + " -convert ";
		cmd += path + " " + desPath;
		cmd += " -codec ATICompressor.dll +speed Normal +fourCC ETC ";

		Process p = Runtime.getRuntime().exec(cmd);
		so = "";
		se = "";
		//获取进程的标准输入流
		final InputStream is1 = p.getInputStream();
		//获取进城的错误流
		final InputStream is2 = p.getErrorStream();
		//启动两个线程，一个线程负责读标准输出流，另一个负责读标准错误流
		new Thread() {
			public void run() {
				BufferedReader br1 = new BufferedReader(new InputStreamReader(is1));
				try {
					String line1 = null;
					while ((line1 = br1.readLine()) != null) {
						if (line1 != null) {
							so += line1;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is1.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();

		new Thread() {
			public void run() {
				BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
				try {
					String line2 = null;
					while ((line2 = br2.readLine()) != null) {
						if (line2 != null) {
							so += line2;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is2.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();

		p.waitFor();
		Thread.sleep(1);

		if (new File(desPath).exists() == false) {
			System.out.println(so + "\n" + se);
			throw new Exception("生成[" + desPath + "]出错。");
		}
	}



	private void runCmdPvr(String path, String desPath, int pType) throws IOException, InterruptedException, Exception {
		//先创建文件夹
		File pd = new File(desPath).getParentFile();
		if (!pd.exists()) {
			pd.mkdirs();
		}

		String cmd = "";
		String cczPath = "";

		if (pType < 10) {
			/////// pvr 格式
			cmd = TEXTURE + " --max-size 2048 --extrude 0 --shape-padding 0 --border-padding 0 --disable-rotation";
			if (exeVer == 3) { //texture packer 3.0以上版本才有
				cmd += " --premultiply-alpha";
			}
			String format = " --opt PVRTC4 ";
			if (pType == 3) {
				format = " --opt PVRTC2 ";
			} else if (pType == 4) {
				format = " --opt \"PVRTC4 (no alpha)\" ";
			} else if (pType == 5) {
				format = " --opt \"PVRTC2 (no alpha)\" ";
			}
			cmd += format + "--reduce-border-artifacts --data main-sd.tmp --format xml --sheet ";
			cmd += desPath + " " + path;
		} else {
			////// ccz 格式
			if (pType == 10) { // 生成 pvr ccz(RGBA4444)
				cmd = TEXTURE + " --max-size 2048 --extrude 0 --shape-padding 0 --border-padding 0 --disable-rotation --opt RGBA4444 --texture-format pvr2ccz --dither-fs-alpha --data main-sd.tmp --format xml --sheet ";
			} else if (pType == 11) { //pvr ccz(RGB565)
				cmd = TEXTURE + " --max-size 2048 --extrude 0 --shape-padding 0 --border-padding 0 --disable-rotation --opt RGB565 --texture-format pvr2ccz --dither-fs-alpha --data main-sd.tmp --format xml --sheet ";
			} else if (pType == 12) { //pvr ccz(RGBA5551)
				cmd = TEXTURE + " --max-size 2048 --extrude 0 --shape-padding 0 --border-padding 0 --disable-rotation --opt RGBA5551 --texture-format pvr2ccz --dither-fs-alpha --data main-sd.tmp --format xml --sheet ";
			} else if (pType == 13) { 
				cmd = TEXTURE + " --max-size 2048 --extrude 0 --shape-padding 0 --border-padding 0 --disable-rotation --opt RGBA4444 --texture-format pvr2ccz --size-constraints AnySize --trim-mode None --dither-fs-alpha --data main-sd.tmp --format xml --sheet ";
			} else {
				throw  new Exception("配置文件的 type2 出错。错误发生在[" + path + "]处");
			}
			if (desPath.endsWith(".DDS")) {
				cczPath = desPath.replaceFirst("\\.DDS", ".pvr.ccz");
			} else if (desPath.endsWith(".pvr")) {
				cczPath = desPath.replaceFirst("\\.pvr", ".pvr.ccz");
			}
			cmd += cczPath + " " + path;
		}
		
		System.out.println(cmd);

		Process p = Runtime.getRuntime().exec(cmd);

		so = "";
		se = "";
		//获取进程的标准输入流
		final InputStream is1 = p.getInputStream();
		//获取进城的错误流
		final InputStream is2 = p.getErrorStream();
		//启动两个线程，一个线程负责读标准输出流，另一个负责读标准错误流
		new Thread() {
			public void run() {
				BufferedReader br1 = new BufferedReader(new InputStreamReader(is1));
				try {
					String line1 = null;
					while ((line1 = br1.readLine()) != null) {
						if (line1 != null) {
							so += line1;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is1.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();

		new Thread() {
			public void run() {
				BufferedReader br2 = new BufferedReader(new InputStreamReader(is2));
				try {
					String line2 = null;
					while ((line2 = br2.readLine()) != null) {
						if (line2 != null) {
							so += line2;
						}
					}
				} catch (IOException e) {
				} finally {
					try {
						is2.close();
					} catch (IOException e) {
					}
				}
			}
		}.start();
		
		p.waitFor();
		Thread.sleep(1);

		if (pType >= 10) {
			FileUtil.copyFileByName(cczPath, desPath, true);
			FileUtil.delete(cczPath);
		}

		if (new File(desPath).exists() == false) {
			System.out.println(so + "\n" + se);
			throw new Exception("生成[" + desPath + "]出错。");
		}
	}

	/**
	 * 将atlas文件解析成bean
	 * @param srcFile
	 * @param parentId
	 */
	private void processAtlas(File srcFile, String parentId) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(srcFile);
			NodeList nl = doc.getChildNodes();
			Node node;
			for (int i=0; i<nl.getLength(); i++) {
				node = nl.item(i);
//				System.out.println(node.getNodeName() + "----" + node.getNodeType());
				if (node.hasChildNodes() && node.getNodeType() == Node.ELEMENT_NODE
						&& "TextureAtlas".equalsIgnoreCase(node.getNodeName())) {
					NodeList nlTa = node.getChildNodes();
					for (int j=0; j<nlTa.getLength(); j++) {
						node = nlTa.item(j);
						if (node.getNodeType() == Node.ELEMENT_NODE && node.hasAttributes()) {
							NamedNodeMap nnm = node.getAttributes();
							String name = null;
							Rectangle region = new Rectangle();
							boolean hasFrame = false;
							Rectangle frame = new Rectangle();
							
							for (int k=0; k<nnm.getLength(); k++) {
								Node namedNode = nnm.item(k);
//								System.out.println(namedNode.getNodeName() + "====" + namedNode.getNodeValue());
								if ("n".equals(namedNode.getNodeName())) {
									name = namedNode.getNodeValue();
								} else if ("x".equals(namedNode.getNodeName())) {
									region.x = Integer.parseInt(namedNode.getNodeValue());
								} else if ("y".equals(namedNode.getNodeName())) {
									region.y = Integer.parseInt(namedNode.getNodeValue());
								} else if ("w".equals(namedNode.getNodeName())) {
									region.width = Integer.parseInt(namedNode.getNodeValue());
								} else if ("h".equals(namedNode.getNodeName())) {
									region.height = Integer.parseInt(namedNode.getNodeValue());
								} else if ("oX".equals(namedNode.getNodeName())) {
									hasFrame = true;
									frame.x = Integer.parseInt(namedNode.getNodeValue());
								} else if ("oY".equals(namedNode.getNodeName())) {
									hasFrame = true;
									frame.y = Integer.parseInt(namedNode.getNodeValue());
								} else if ("oW".equals(namedNode.getNodeName())) {
									hasFrame = true;
									frame.width = Integer.parseInt(namedNode.getNodeValue());
								} else if ("oH".equals(namedNode.getNodeName())) {
									hasFrame = true;
									frame.height = Integer.parseInt(namedNode.getNodeValue());
								}
							}
							
							if (!hasFrame) {
								frame = null;
							}

							SubBean subBean = new SubBean();
							subBean.parent = parentId;
							subBean.x1 = region.x;
							subBean.y1 = region.y;
							subBean.w1 = region.width;
							subBean.h1 = region.height;

							if (hasFrame) {
								subBean.x2 = frame.x;
								subBean.y2 = frame.y;
								subBean.w2 = frame.width;
								subBean.h2 = frame.height;
							}

							subTexDic.put(name, subBean);
						}
					}
				}
			}
		} catch (SAXException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ParserConfigurationException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * 分析配置文件
	 * @param platIndex
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	private void parseCfg(int platIndex) throws FileNotFoundException, IOException, Exception {
		boolean isUtf = false;

		//判断文件是不是 UTF-8
		EncodingDetect ed = new EncodingDetect();
		int ei = ed.detectEncoding(new File(mainCfg));
		if (ei == 6) { // utf-8
			isUtf = true;
		}

		BufferedReader lnr = null;
		InputStreamReader read;
		if (isUtf) {
			read = new InputStreamReader(new FileInputStream(mainCfg), "utf-8");
		} else {
			read = new InputStreamReader(new FileInputStream(mainCfg));
		}
		lnr = new BufferedReader(read);
//		LineNumberReader lnr = new LineNumberReader(new FileReader(mainCfg));
		String ln;
		int i = 0;
		while ((ln = lnr.readLine()) != null) {
			ln = ln.trim();
			if (ln.startsWith("#") || ln.length() == 0) {
				continue;
			}

			String[] sc = ln.split("	");
			if (sc.length < CONFIGITEMNUM) {
				throw new Exception("res配置文件"+mainCfg+"的项目数不对：[" + ln + "], len="+sc.length);
			}

			ResBean cb = new ResBean();
			String id = sc[0].trim();
			if (i == 0) {
				i++;
				continue;	//头一行是列名
			}
			if (id.length() == 0) {
				throw new Exception("res配置文件"+mainCfg+"的id为空：[" + ln + "]");
			}
			String path = sc[1].trim();
			if (path.length() == 0) {
				throw new Exception("res配置文件"+mainCfg+"的path为空：[" + ln + "]");
			}
			
			cb.id = id;
			cb.path = parseSrcPath(path);
			if (sc[2] != null && sc[2].length() > 0) {
				cb.atlas = parseSrcPath(sc[2]);
			}
			cb.cache = Integer.parseInt(sc[3]);
			cb.sType(Integer.parseInt(sc[4]));

			if (sc.length > 5) {
				int[] platType = new int[4];
				platType[0] = Integer.parseInt(sc[5]);	//ipad -- pvr
				platType[1] = platType[2] = platType[3] = Integer.parseInt(sc[6]);	//android
				cb.sPlatTypes(platType);
			}

			if (configs.containsKey(id)) {
				System.out.println("***** res配置文件"+mainCfg+"的id重复：[" + ln + "]");
			}

			configs.put(id, cb);

			// 修改文件扩展名
			ResBean crb = cb.clone();
			crb.path = parseDstPath(path, platIndex);
			if (crb.gType() == 1 || crb.gType() == 4) {
				int m = crb.path.lastIndexOf(".png");
				if (m == -1) m = crb.path.lastIndexOf(".jpg");
				if (m == -1) m = crb.path.lastIndexOf(".jpeg");
				if (m==-1) {
					throw new Exception("图片资源配置"+mainCfg+"错误，扩展名不是png或jpg。["+crb.path+"]");
				}
				if ("pvr".equals(PLAT_PREFIX[platIndex])) {
					crb.path = crb.path.replaceFirst("\\.(png|jpg|jpeg)", ".pvr");
				} else {
					crb.path = crb.path.replaceFirst("\\.(png|jpg|jpeg)", ".DDS");
				}
			} else if (crb.gType() == 6) {
				int ip = crb.path.lastIndexOf(".");
				String tp = crb.path.substring(0, ip);
				crb.path = tp + ".m4a";
			}
			
			crb.atlas = null;
			lastResId = crb.id;
			resDic.put(id, crb);

			i++;
		}
		read.close();
		lnr.close();
	}
	
	/**
	 * 处理配置文件原始路径中的变量
	 * @param path
	 * @param platIndex
	 * @return
	 */
	private String parseSrcPath(String path) {
		path = path.trim();
		String s;
		if (path.startsWith("/")) {
			s = path.substring(1, path.length());
			s = s.replaceAll(COMMON, "common");
			s = s.replaceAll(LANGUAGE, language);
			s = s.replaceAll(RESOURCE, PC_PLAT_PREFIX);
		} else {  //不是以“/”开头的，默认加上语言地区和资源前缀
			s = language + "/" + PC_PLAT_PREFIX + "/" + path;
		}
		return s;
	}
	/**
	 * 处理资源文件生成的最终路径中的变量
	 * @param path
	 * @param platIndex
	 * @return
	 */
	private String parseDstPath(String path, int platIndex) {
		String s;
		if (path.startsWith("/")) {
			s = path.substring(1, path.length());
			s = s.replaceAll(COMMON, "common");
			s = s.replaceAll(LANGUAGE, language);
			s = s.replaceAll(RESOURCE, PLAT_PREFIX[platIndex]);
		} else {  //不是以“/”开头的，默认加上语言地区和资源前缀
			s = language + "/" + PLAT_PREFIX[platIndex] + "/" + path;
		}
		return s;
	}




	///////////////////// md5 //////////////////////////////
	 /**
      * 默认的密码字符串组合，用来将字节转换成 16 进制表示的字符
	  * apache校验下载的文件的正确性用的就是默认的这个组合
     */
    protected static char hexDigits[]   = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    protected static MessageDigest messagedigest = null;
    static {
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String getFileMD5String(File file) throws IOException {
        InputStream fis;
        fis = new FileInputStream(file);
        byte[] buffer = new byte[2048];
        int numRead = 0;
        while ((numRead = fis.read(buffer)) > 0) {
            messagedigest.update(buffer, 0, numRead);
        }
        fis.close();
        return bufferToHex(messagedigest.digest());
    }

    private static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length);
    }

    private static String bufferToHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = hexDigits[(bt & 0xf0) >> 4];// 取字节中高 4 位的数字转换
        // 为逻辑右移，将符号位一起右移,此处未发现两种符号有何不同
        char c1 = hexDigits[bt & 0xf];// 取字节中低 4 位的数字转换
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }
}
