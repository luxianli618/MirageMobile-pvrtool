package pvrtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IO工具
 *
 */
public class FileUtil {

	/**
	 * 新建文件
	 *
	 * @param path
	 *            文件所在路径
	 * @param filename
	 *            文件名
	 * @param overWrite
	 *            是否强制覆盖
	 * @return 文件全路径
	 * @throws IOException
	 */
	public static String createFile(String path, String filename,
			boolean overWrite) throws IOException {
		File file = new File(path, filename);
		if (file.exists()) {
			try {
				if (file.isDirectory()) {
					throw new Exception("创建的文件和已有目录重名");
				}
			} catch (Exception e) {
				return null;
			}
			if (overWrite) {
				file.delete();
				file.createNewFile();
			}
		} else {
			file.createNewFile();
		}
		return file.getPath();
	}

	/**
	 * 新建目录
	 *
	 * @param path
	 *            目录所在路径
	 * @param dirName
	 *            目录名
	 * @return 目录全路径
	 */
	public static String createDir(String path, String dirName) {
		File dir = new File(path, dirName);
		if (dir.exists()) {
			try {
				if (dir.isFile()) {
					throw new Exception("创建的目录和已有文件重名");
				}
			} catch (Exception e) {
				return null;
			}
		} else {
			dir.mkdirs();
		}
		return dir.getPath();
	}

	/**
	 * 拷贝
	 *
	 * @param src
	 *            源文件（文件夹）
	 * @param dest
	 *            目标文件夹
	 * @param overWrite
	 *            强制覆盖
	 * @return 未拷贝文件名
	 * @throws IOException
	 */
	public static String copy(String src, String dest, boolean overWrite)
			throws IOException {
		File srcFile = new File(src);
		File destFile = new File(dest);
		StringBuffer unCopyFiles = new StringBuffer();
		if (!destFile.exists() || destFile.isFile()) {
			throw new IOException("目标路径不是一个有效路径");
		}
		if (srcFile.isFile()) {
			try {
				copyFile(srcFile, dest, overWrite);
			} catch (Exception e) {
				unCopyFiles.append(srcFile.getPath());
				unCopyFiles.append(";");
			}
		} else {
			try {
				dest = copyDir(srcFile, dest);
			} catch (Exception e) {
				unCopyFiles.append(srcFile.getPath());
				unCopyFiles.append(";");
			}
			File[] files = srcFile.listFiles();
			for (int i = 0; i < files.length; i++) {
				String temp = copy(files[i].getPath(), dest, overWrite);
				if (temp != null && !temp.isEmpty()) {
					unCopyFiles.append(temp);
					unCopyFiles.append(";");
				}
			}
		}
		return unCopyFiles.toString();
	}

	/**
	 * 剪切
	 *
	 * @param src
	 *            源文件（文件夹）
	 * @param dest
	 *            目标文件夹
	 * @param overWrite
	 *            强制覆盖
	 * @return 未剪切文件
	 * @throws IOException
	 */
	public static String cut(String src, String dest, boolean overWrite)
			throws IOException {
		File srcFile = new File(src);
//		File destFile = new File(dest);
		String unCutaFiles = null;
//		if (!destFile.exists() || destFile.isFile()) {
//			throw new IOException("目标路径不是一个有效路径");
//		}
		if (srcFile.isFile()) {
			try {
				copyFile(srcFile, dest, overWrite);
				srcFile.delete();
			} catch (Exception e) {
				unCutaFiles += srcFile.getPath() + ";";
			}
		} else {
			try {
				dest = copyDir(srcFile, dest);

			} catch (Exception e) {
				unCutaFiles += srcFile.getPath() + ";";
			}
			File[] files = srcFile.listFiles();
			for (int i = 0; i < files.length; i++) {
				String temp = cut(files[i].getPath(), dest, overWrite);
				if (temp != null) {
					unCutaFiles += temp + ";";
				}
				files[i].delete();
			}
			srcFile.delete();
		}
		return unCutaFiles;
	}

	/**
	 * 删除
	 *
	 * @param path
	 *            文件（文件夹）全路径
	 */
	public static void delete(String path) {
		File file = new File(path);
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] tmp = file.listFiles();
				for (int i = 0; i < tmp.length; i++) {
					delete(tmp[i].getPath());
				}
				file.delete();
			} else {
				file.delete();
			}

		}
	}

	/**
	 * 读取文件内容
	 *
	 * @param path
	 *            文件名
	 * @return 文件内容字符串
	 * @throws IOException
	 */
	public static String read(String path) throws IOException {
		File file = new File(path);
		if (!file.exists() || file.isDirectory()) {
			throw new FileNotFoundException();
		}
		FileInputStream fis = new FileInputStream(file);
		byte[] buf = new byte[1024];
		StringBuffer sb = new StringBuffer();
		while ((fis.read(buf)) != -1) {
			sb.append(new String(buf));
			buf = new byte[1024];// 重新生成，避免和上次读取的数据重复
		}
		fis.close();
		return sb.toString();
	}

	/**
	 * 把内容写入文件
	 *
	 * @param path
	 *            文件名
	 * @param sb
	 *            写入内容
	 * @param encoding
	 *            编码方式（默认为UTF-8）
	 * @throws IOException
	 */
	public static void write(String path, StringBuffer sb, String encoding)
			throws IOException {
		if (sb == null || sb.length() < 1) {
			return;
		}
		File file = new File(path);
		if (encoding == null) {
			encoding = "utf-8";
		}
		if (file.exists()) {
			FileOutputStream out = new FileOutputStream(file, true);
			byte[] bytes = sb.toString().getBytes(encoding);
			out.write(bytes);
			out.close();
		} else {
			throw new FileNotFoundException();
		}
	}

	private static void copyFile(File srcFile, String dest, boolean overWrite)
			throws Exception {

		String fileName = srcFile.getName();
		File file = new File(dest, fileName);

		if (file.exists()) {
			if (file.isDirectory()) {
				throw new Exception("拷贝的文件和已有目录重名");
			}
			if (overWrite) {
				file.delete();
				file.createNewFile();
			}
		} else {
			file.createNewFile();
		}
		FileInputStream in = new FileInputStream(srcFile);
		FileOutputStream out = new FileOutputStream(file);

		int c;
		byte buffer[] = new byte[1024];
		while ((c = in.read(buffer)) != -1) {
//			for (int i = 0; i < c; i++) {
//				out.write(buffer[i]);
//			}
			out.write(buffer, 0, c);
		}
		in.close();
		out.close();

	}

	private static String copyDir(File srcDir, String dest) throws Exception {
		String dirName = srcDir.getName();
		File file = new File(dest, dirName);
		if (file.exists()) {
			if (file.isFile()) {
				throw new Exception("拷贝的目录和已有文件重名");
			}
		} else {
			file.mkdirs();
		}
		return file.getPath();
	}

	public static void copyFileByName(String srcPath, String destPath, boolean overWrite) {

		File srcFile = new File(srcPath);
		File destFile = new File(destPath);

		File pd = destFile.getParentFile();
		if (!pd.exists()) {
			pd.mkdirs();
		}
		try {
			if (destFile.exists()) {
				if (overWrite) {
					destFile.delete();

					destFile.createNewFile();

				}
			} else {
				destFile.createNewFile();
			}
			FileInputStream in = new FileInputStream(srcFile);
			FileOutputStream out = new FileOutputStream(destFile);

			int c;
			byte buffer[] = new byte[4096];
			while ((c = in.read(buffer)) != -1) {
//				for (int i = 0; i < c; i++) {
//					out.write(buffer[i]);
//				}
				out.write(buffer, 0, c);
			}
			in.close();
			out.close();

		} catch (IOException ex) {
			Logger.getLogger(FileUtil.class.getName()).log(Level.SEVERE, null, ex);
		}

	}
}
