package main;

import java.util.ArrayList;

public class AvailableLocFinderThread implements Runnable {

	private ArrayList<Position> availableLocations;
	private LightColor c;
	private int startIndex;
	private int endIndex;
	private int compareColorsCounter;
	private int WIDTH;
	private int HEIGHT;
	private boolean AVERAGE;
	private LightColor[][] rawImage;
//	private int lowestIndex;
	
	/**
	 * Object for running threads
	 * @param startIndex inclusive
	 * @param endIndex inclusive
	 */
	public AvailableLocFinderThread(ArrayList<Position> availableLocations, LightColor c, int startIndex, int endIndex, int WIDTH,
			int HEIGHT, boolean AVERAGE, LightColor[][] rawImage) {
		this.availableLocations = availableLocations;
		this.c = c;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.compareColorsCounter = 0;
		this.WIDTH = WIDTH;
		this.HEIGHT = HEIGHT;
		this.AVERAGE = AVERAGE;
		this.rawImage = rawImage;
	}

	@Override
	public void run() {
//		int lowestEval = evaluatePosition(availableLocations.get(endIndex), c);
//		availableLocations.get(endIndex).positionalValue = lowestEval;
//		int lowestIndex = endIndex;
//		for (int i = endIndex - 1; i >= startIndex; i--) {
//			int newEval = evaluatePosition(availableLocations.get(i), c);
//			availableLocations.get(i).positionalValue = newEval;
//			if (newEval < lowestEval) {
//				lowestIndex = i;
//				lowestEval = newEval;
//			}
//		}
//		
//		this.lowestIndex = lowestIndex;
		
		for (int i = startIndex; i <= endIndex; i++) {
			availableLocations.get(i).positionalValue = evaluatePosition(availableLocations.get(i), c);
		}
	}

	/**
	 * Evaluates how good a position is for potentially receiving the next pixel
	 * 
	 * @param pos   the position
	 * @param color the color that would be going there
	 * @return an integer for how good it is. Lower is better.
	 */
	private int evaluatePosition(Position pos, LightColor color) {
		ArrayList<Integer> surrounding = new ArrayList<Integer>();
		for (int dx = -1; dx <= 1; dx++) {
			int xdx = pos.x + dx;
			if (xdx == -1 || xdx == WIDTH)
				continue;
			for (int dy = -1; dy <= 1; dy++) {
				int ydy = pos.y + dy;
				if (ydy == -1 || ydy == HEIGHT)
					continue;
				if (rawImage[ydy][xdx] == null)
					continue;
				surrounding.add(compareColors(rawImage[ydy][xdx], color));
			}
		}

		int ret;
		if (AVERAGE) {
			int sum = 0;
			for (Integer i : surrounding)
				sum += i;
			ret = sum / surrounding.size();
		} else {
			int min = surrounding.get(0);
			for (int i = 1; i < surrounding.size(); i++)
				min = Math.min(min, surrounding.get(i));
			ret = min;
		}
		return ret;
	}

	/**
	 * says how close the colors are to each other
	 */
	private int compareColors(LightColor colorInPic, LightColor colorToAdd) {
		compareColorsCounter++;
		int r = colorInPic.red - colorToAdd.red;
		int g = colorInPic.green - colorToAdd.green;
		int b = colorInPic.blue - colorToAdd.blue;
		return (r * r) + (g * g) + (b * b);
	}
	
	public int getCompareColorsCounter() {
		return compareColorsCounter;
	}
	
//	public int getLowestIndex() {
//		return lowestIndex;
//	}
}
