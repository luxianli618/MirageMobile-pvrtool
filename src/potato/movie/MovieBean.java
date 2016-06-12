package potato.movie;


/**
 * 配置文件bean
 * @author Floyd
 * Jun 6, 2012
 */
public class MovieBean
{
	/**动画名*/
	public String movieName;
	/**方向数*/
	public int dirNumber = 1;
	/**总帧数量*/
	public int frameNumber;
	/**播放速度*/
	public int speed;
	/**设置基准点X*/
	public int footX;
	/**设置基准点Y*/
	public int footY;
	
	/**全部帧*/
	public int allFrame;
	/**
	 * 图片播放帧数,  第n帧延时 = frameArr[n] * speed
	 */		
	public int[] frameArr;

	/**
	 *  当前动画影片的缩放系数，它直接影响当前实例的 scaleX 和 scaleY
	 */
	public double scale = 1;
	
//	public boolean inited = true;
	
	public MovieBean()
	{
	}
}
