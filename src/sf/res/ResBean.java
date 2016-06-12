package sf.res;

import java.util.Arrays;

/**
 *
 * @author Administrator
 */
public class ResBean {
	/**id*/
	public String id;
	/**图片路径*/
	public String path;
	/**是否缓存*/
	public int cache;

	/**atlas*/
	public String atlas;

	/**
	 * 类型，可能的值如下：
	 * 	0	资源配置文件类型，允许包含子配置文件
	 * 	1	资源类型
	 * 	2	字符串资源文件（根据当前语言进行预处理）
	 *  3	动画配置文件
	 *  4   zilb压缩的图片资源文件
	 *  5   zilb压缩的其他文件（只能是类型为 8 的文件）
	 *  6   mp4压缩文件，扩展名m4a（需要安装FormatFactory）
	 * 	8	直接拷贝
	 * 	9	忽略任何处理
	 */
	private int type;
	
	private int[] platTypes;

	public void sPlatTypes(int[] types) {
		this.platTypes = types;
	}
	public int[] gPlatTypes() {
		return platTypes;
	}


	@Override
	public ResBean clone() {
		ResBean rb = new ResBean();
		rb.id = id;
		rb.atlas = atlas;
		rb.cache = cache;
		rb.path = path;
		rb.type = type;
		if (platTypes != null) {
			rb.platTypes = Arrays.copyOf(platTypes, platTypes.length);
		}
		return rb;
	}

	/**
	 * @return the type
	 */
	public int gType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	 public void sType(int type) {
		this.type = type;
	}
}
