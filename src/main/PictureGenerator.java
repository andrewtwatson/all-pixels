package main;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.ImageIO;


/**
 * Generates a picture with all given colors. Places one pixel at a given
 * position, then takes a random pixel and places in a place such that it
 * touches another pixel and is close in color to its neighbors
 * 
 * @author andrew
 *
 */
public class PictureGenerator {
	// CAN CHANGE THESE
	// if true, evaluates positions by the average of its differential from its
	// surroundings
	// -> seed 15 on 128x256 compares colors ~ 941295669 times
	// if false, does the minimum of its differential
	// -> seed 15 on 128x256 compares colors ~ 46397964 times
	
	private static int NUM_THREADS;
	private static int WIDTH;
	private static int HEIGHT;
	private static boolean AVERAGE;
	private static long SEED;
	private static int BITS;
	private static ArrayList<BufferedImage> FRAME_LIST;

	public static LightColor[][] rawImage;
	public static long compareColorsCounter = 0;

	/**
	 * Returns the position that fulfills the requirement the best. Might be closest
	 * to the given color, etc.
	 * 
	 * @param availableLocations list of locations you could put a pixel
	 */
	private static Position getNextLocationNoThreads(ArrayList<Position> availableLocations, LightColor color) {
		//
		//compareColorsCounter = 0;
		 //evaluate the last position in the list, then work backwards.
		int lowestEval = evaluatePosition(availableLocations.get(availableLocations.size() - 1), color);
		int lowestIndex = availableLocations.size() - 1;
		for (int i = availableLocations.size() - 2; i >= 0; i--) {
			int newEval = evaluatePosition(availableLocations.get(i), color);
			if (newEval < lowestEval) {
				lowestIndex = i;
				lowestEval = newEval;
			}
		}
//		System.out.println(compareColorsCounter);
		return availableLocations.get(lowestIndex);
	}
	
	private static Position getNextLocationWithThreads(ArrayList<Position> availableLocations, LightColor color) {
		// use threads to calculate all the positions, then compare at the end
		int realNumThreads = NUM_THREADS;
		if (realNumThreads > availableLocations.size())
			realNumThreads = availableLocations.size();
		AvailableLocFinderThread[] threadObjects = new AvailableLocFinderThread[realNumThreads];
		Thread[] threads = new Thread[realNumThreads];
		for (int i = 0; i < realNumThreads; i++) {
			int startIndex = (availableLocations.size() / realNumThreads) * i;
			int endIndex = Math.min((availableLocations.size() / realNumThreads) * (i + 1) - 1, availableLocations.size());
			threadObjects[i] = new AvailableLocFinderThread(availableLocations, color, startIndex, endIndex, WIDTH, HEIGHT, AVERAGE, rawImage);
			threads[i] = new Thread(threadObjects[i]);
			threads[i].start();
		}
		
		// join the threads back, collect answers
//		int[] lowestIndexes = new int[realNumThreads];
		try {
			for (int i = 0; i < realNumThreads; i++) {
				threads[i].join();
				compareColorsCounter += threadObjects[i].getCompareColorsCounter();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Position lowest = availableLocations.get(availableLocations.size() - 1);
		for (int i = availableLocations.size() - 2; i >= 0; i--) { 
			if (availableLocations.get(i).positionalValue < lowest.positionalValue)
				lowest = availableLocations.get(i);
		}
		
		return lowest;
	}

	/**
	 * Evaluates how good a position is for potentially receiving the next pixel
	 * 
	 * @param pos   the position
	 * @param color the color that would be going there
	 * @return an integer for how good it is. Lower is better.
	 */
	private static int evaluatePosition(Position pos, LightColor color) {
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
	private static int compareColors(LightColor colorInPic, LightColor colorToAdd) {
		compareColorsCounter++;
		int r = colorInPic.red - colorToAdd.red;
		int g = colorInPic.green - colorToAdd.green;
		int b = colorInPic.blue - colorToAdd.blue;
		return (r * r) + (g * g) + (b * b);
	}

	/**
	 * Runs the picture generator
	 * 
	 * @param average      whether to take the average of surrounding pixels or just
	 *                     the minimum
	 * @param bits         bits for each color in RGB. Also determines canvas size
	 * @param seed         seed of the random shuffler, 0 for random
	 * @param name         name of the file
	 * @param sortColorsBy how to sort the colors. can be "random", "hsv",
	 *                     "luminosity". If misspelled, it goes random
	 * @param frames       how many frames for the gif. 0 for no gif.
	 */
	public static void generate(boolean average, int bits, int threads, String name, String sortColorsBy, boolean rotate, int frames) {
		// preprocessing
		compareColorsCounter = 0;
		PictureGenerator.AVERAGE = average;
		// can go up to 12
		PictureGenerator.BITS = bits;
		PictureGenerator.NUM_THREADS = threads;

		// DONT CHANGE THESE
		int NUMCOLORS = (int) Math.pow(2, BITS);
		PictureGenerator.WIDTH = (BITS % 2 == 0 ? (int) Math.pow(2, BITS * 3 / 2)
				: (int) Math.pow(2, BITS * 3 / 2 + 1));
		PictureGenerator.HEIGHT = (int) Math.pow(2, BITS * 3 / 2);

		PictureGenerator.FRAME_LIST = new ArrayList<BufferedImage>();
		try {
			Files.createDirectory(Paths.get("files/" + name));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		PictureGenerator.rawImage = new LightColor[HEIGHT][WIDTH];

		int STARTX = WIDTH / 2;
		int STARTY = HEIGHT / 2;

		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		// generate all colors
		ArrayList<LightColor> colors = new ArrayList<LightColor>();
		for (int r = 0; r < NUMCOLORS; r++) {
			for (int g = 0; g < NUMCOLORS; g++) {
				for (int b = 0; b < NUMCOLORS; b++) {
					colors.add(new LightColor((r * 255) / (NUMCOLORS - 1), (g * 255) / (NUMCOLORS - 1),
							(b * 255) / (NUMCOLORS - 1)));
				}
			}
		}

		if (colors.size() != rawImage.length * rawImage[0].length) {
			System.out.println(colors.size());
			System.out.println(rawImage.length);
			System.out.println(rawImage[0].length);
			System.out.println(rawImage.length * rawImage[0].length);
			throw new IllegalArgumentException("sizes wrong");
		}

		if (sortColorsBy.equals("hsv")) {
			Comparator<LightColor> c = new LightColor.CompareHSB();
			Collections.sort(colors, c);
		} else {
			Random rand = new Random();
			if (SEED != 0)
				rand.setSeed(SEED);
			Collections.shuffle(colors, rand);
		}
		
		if (rotate) {
			Collections.rotate(colors, colors.size() / 2);
		}

		// this holds the available places to put a pixel. Doesnt already have a pixel
		// and is next to at least one pixel
		ArrayList<Position> availableLocations = new ArrayList<Position>();
		Iterator<LightColor> c = colors.iterator();

		// if availableLocationsis empty, add first pixel
		LightColor nextColor;
		Position bestPos;

		int num = 0;
		int frameNum = 0;

		while (c.hasNext()) {
			nextColor = c.next();

			if (availableLocations.size() == 0)
				bestPos = new Position(STARTX, STARTY);
			else {
//				bestPos = getNextLocationNoThreads(availableLocations, nextColor);
				bestPos = getNextLocationWithThreads(availableLocations, nextColor);
			}

			// add pixel
			if (rawImage[bestPos.y][bestPos.x] != null) {
				throw new IllegalArgumentException("x: " + bestPos.x + " y: " + bestPos.y + " already filled. it is "
						+ rawImage[bestPos.y][bestPos.x].toString());
			}
			rawImage[bestPos.y][bestPos.x] = nextColor;

			// print status
			if (num % (colors.size() / 100) == 0)
				System.out.println((num * 100) / colors.size());
			
			// save frame
			if (frames != 0 && num % (colors.size() / frames) == 0) {
				saveFrame(name, frameNum);
				frameNum++;
			}
			num++;
			
			// add available locations
			availableLocations.remove(bestPos);
			for (int dx = -1; dx <= 1; dx++) {
				int xdx = bestPos.x + dx;
				if (xdx == -1 || xdx == WIDTH)
					continue;
				for (int dy = -1; dy <= 1; dy++) {
					int ydy = bestPos.y + dy;
					if (ydy == -1 || ydy == HEIGHT)
						continue;
					if (rawImage[ydy][xdx] == null) {
						Position p = new Position(xdx, ydy);
						if (!availableLocations.contains(p))
							availableLocations.add(p);
					}
				}
			}
		}

		// write all colors
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				img.setRGB(x, y, rawImage[y][x].toInt());
			}
		}
		if (!c.hasNext())
			System.out.println("perfect");
		else
			System.out.println("OH NO BAD BAD BAD");

//		System.out.println("bits: " + BITS + " seed: " + SEED + " calls of compareColors: " + compareColorsCounter);
		System.out.println(compareColorsCounter);
		
		// save
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream("files/" + name + "/" + name + "FINAL.png"))) {
			ImageIO.write(img, "png", out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// create gif
		if (FRAME_LIST.size() > 0) {
			AnimatedGifEncoder e = new AnimatedGifEncoder();
			e.start("files/" + name + "/" + name + "GIF.gif");
			e.setRepeat(0);
			e.setDelay(150);
			for (int i = 0; i < FRAME_LIST.size(); i++) {
				e.addFrame(FRAME_LIST.get(i));
			}
			e.finish();
		}
	}
	
	private static void saveFrame(String name, int frameNum) {
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		// write all colors
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				if (rawImage[y][x] != null) {
					img.setRGB(x, y, rawImage[y][x].toInt());
				} else {
					img.setRGB(x, y, 0);
				}
			}
		}
		
		FRAME_LIST.add(img);
		
		// print picture
		if (frameNum % 1 == 0) {
			String frameNumStr = "" + frameNum;
			while (frameNumStr.length() < 5) {
				frameNumStr = "0" + frameNumStr;
			}
			try (OutputStream out = new BufferedOutputStream(
					new FileOutputStream("files/" + name + "/" + name + "frame" + frameNumStr + ".png"))) {
				ImageIO.write(img, "png", out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
