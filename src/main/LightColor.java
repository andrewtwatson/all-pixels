package main;

import java.awt.Color;
import java.util.Comparator;

/**
 * Holds info on the color. Can convert to an int form as well
 * 
 * @author andrew
 *
 */
public class LightColor {
	// given to be numbers between 0 and 255
	public int red;
	public int green;
	public int blue;
	public float[] hsb;

	public LightColor(int red, int green, int blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.hsb = Color.RGBtoHSB(red, green, blue, hsb);
	}

	public int toInt() {
		return (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
	}

	@Override
	public String toString() {
		return "Color [red=" + red + ", green=" + green + ", blue=" + blue + "]";
	}

	public static class CompareHSB implements Comparator<LightColor> {
		@Override
		public int compare(LightColor o1, LightColor o2) {
			if(o1.hsb[0] > o2.hsb[0])
				return 1;
			else if (o1.hsb[0] < o2.hsb[0])
				return -1;
			else {
				if(o1.hsb[1] > o2.hsb[1])
					return 1;
				else if (o1.hsb[1] < o2.hsb[1])
					return -1;
				else {
					if(o1.hsb[2] > o2.hsb[2])
						return 1;
					else if (o1.hsb[2] < o2.hsb[2])
						return -1;
					else
						return 0;
				}
			}
		}
		
	}
}
