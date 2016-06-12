package utils;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

import pvrtool.FileUtil;
import zlib.ZLibUtils;

public class ImageUtil {
//	/**
//     * 保存图像到 JPEG 文件
//     * @param file 保存的目标文件
//     * @param image 保存的源图像
//     * @param quality 保存的 JPEG 图像质量
//     * @param listener 保存进度监听器
//     */
//    public static void writeJPEG(File file, BufferedImage image, int quality, IIOWriteProgressListener listener) throws
//            FileNotFoundException, IOException {
//        Iterator it = ImageIO.getImageWritersBySuffix("jpg");
//        if (it.hasNext()) {
//            FileImageOutputStream fileImageOutputStream = new FileImageOutputStream(file);
//            ImageWriter iw = (ImageWriter) it.next();
//            ImageWriteParam iwp = iw.getDefaultWriteParam();
//            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//            iwp.setCompressionQuality((float) quality / 100f);
//            iw.setOutput(fileImageOutputStream);
//            iw.addIIOWriteProgressListener(listener);
//            iw.write(null, new IIOImage(image, null, null), iwp);
//            iw.dispose();
//            fileImageOutputStream.flush();
//            fileImageOutputStream.close();
//        }
//    }
    
    
    /** 
     *  
     * 自己设置压缩质量来把图片压缩成byte[] 
     *  
     * @param image 
     *            压缩源图片 
     * @param quality 
     *            压缩质量，在0-1之间， 
     * @return 返回的字节数组 
     * @throws IOException 
     */  
    public static byte[] bufferedImageTobytes(BufferedImage image, float quality) throws IOException {
//        System.out.println("jpeg" + quality + "质量开始打包" + getCurrentTime());  
        // 如果图片空，返回空  
        if (image == null) {  
            return null;  
        }     
        // 得到指定Format图片的writer  
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");// 得到迭代器  
        ImageWriter writer = (ImageWriter) iter.next(); // 得到writer  
  
        // 得到指定writer的输出参数设置(ImageWriteParam )  
        ImageWriteParam iwp = writer.getDefaultWriteParam();  
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // 设置可否压缩  
        iwp.setCompressionQuality(quality); // 设置压缩质量参数  
  
        iwp.setProgressiveMode(ImageWriteParam.MODE_DISABLED); 
  
        ColorModel colorModel = ColorModel.getRGBdefault();  
        // 指定压缩时使用的色彩模式  
        iwp.setDestinationType(new javax.imageio.ImageTypeSpecifier(colorModel,  
                colorModel.createCompatibleSampleModel(16, 16)));  
  
        // 开始打包图片，写入byte[]  
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // 取得内存输出流  
        IIOImage iIamge = new IIOImage(image, null, null);  
//        try {  
            // 此处因为ImageWriter中用来接收write信息的output要求必须是ImageOutput  
            // 通过ImageIo中的静态方法，得到byteArrayOutputStream的ImageOutput  
            writer.setOutput(ImageIO.createImageOutputStream(byteArrayOutputStream));  
            writer.write(null, iIamge, iwp);  
//        } catch (IOException e) {
//            e.printStackTrace();
//        }  
//        System.out.println("jpeg" + quality + "质量完成打包-----" + getCurrentTime()  
//                + "----lenth----" + byteArrayOutputStream.toByteArray().length);  
        return byteArrayOutputStream.toByteArray();  
    }

	public static void toJpz(String path, String desPath) throws IOException {
		File srcFile = new File(path);
		String srcName = srcFile.getName();
		String extName = srcName.substring(srcName.lastIndexOf(".")+1);
		if ("jpg".equalsIgnoreCase(extName) || "jpeg".equalsIgnoreCase(extName)) {
			FileUtil.copyFileByName(path, desPath, true);
			return;
		}
				
//		try {
			BufferedImage bi = ImageIO.read(srcFile);
			int[] argb = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
			
			BufferedImage jpg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
			jpg.setRGB(0, 0, bi.getWidth(), bi.getHeight(), argb, 0, bi.getWidth());
			
//			ImageIO.write(jpg, "jpeg", new File("d:/ttt.jpg"));
			byte[] ba = bufferedImageTobytes(jpg, 0.85f);	//jpeg数据
			
			byte[] ab = new byte[argb.length];
			for (int i=0; i<argb.length; i++) {
				ab[i] = (byte) (argb[i] >> 24 & 0xff);
			}
			byte[] abc = ZLibUtils.compress(ab);	//zlib压缩的aphla
			
			// 文件头
			ByteBuffer h = ByteBuffer.allocate(16);
			h.order(ByteOrder.LITTLE_ENDIAN);
			h.put((byte)'J');
			h.put((byte)'P');
			h.put((byte)'Z');
			h.put((byte)' ');
			h.putInt(0);
			h.putInt(ba.length);
			h.putInt(0);
			
			//写文件
			File desFile = new File(desPath);
			File pfile = desFile.getParentFile();
			if (!pfile.exists()) {
				pfile.mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(desFile);
			fos.write(h.array());
			fos.write(ba);
			fos.write(abc);
			fos.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}  
	
	
	public static void toJpg(String path, String desPath) throws IOException {
		File srcFile = new File(path);
//		try {
			BufferedImage bi = ImageIO.read(srcFile);
			int[] argb = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
			
			BufferedImage jpg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
			jpg.setRGB(0, 0, bi.getWidth(), bi.getHeight(), argb, 0, bi.getWidth());
			
//			ImageIO.write(jpg, "jpeg", new File("d:/ttt.jpg"));
			byte[] ba = bufferedImageTobytes(jpg, 0.80f);	//jpeg数据
			
			//写文件
			File desFile = new File(desPath);
			File pfile = desFile.getParentFile();
			if (!pfile.exists()) {
				pfile.mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(desFile);
			fos.write(ba);
			fos.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}  
}
