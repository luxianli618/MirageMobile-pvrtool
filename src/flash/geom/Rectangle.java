package flash.geom;

/**
 *
 * @author Administrator
 */
public class Rectangle {
//    public double bottom;
//    bottomRight : Point
    public int height;
//    public double left;
//    public double right;
//    size : Point
//    public double top;
//    topLeft : Point
    public int width;
    public int x;
    public int y;

	public Rectangle() {
	}

	public Rectangle(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
}
