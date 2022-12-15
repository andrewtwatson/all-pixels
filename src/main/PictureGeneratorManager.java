package main;


public class PictureGeneratorManager {
	public static void main(String[] args) {
		// make 5 8 bit normal, 5 8-bit average,
		// 3 9 bit normal, 2 9 bit average
		// 1 10 bit normal
		// 1 11 bit normal
		System.out.println("starting");
		
		long startTime = System.currentTimeMillis();
		PictureGenerator.generate(false, 7, 2, "weird1copy", "hsv", true, 100);
		long endTime = System.currentTimeMillis();
		System.out.println((endTime - startTime));

//		startTime = System.currentTimeMillis();
//		PictureGenerator.generate(false, 5, 2, "test", "hsv", false, 15);
//		endTime = System.currentTimeMillis();
//		System.out.println((endTime - startTime));
//		
//		startTime = System.currentTimeMillis();
//		PictureGenerator.generate(false, 5, 3, "test", "hsv", false, 15);
//		endTime = System.currentTimeMillis();
//		System.out.println((endTime - startTime));
//		
//		startTime = System.currentTimeMillis();
//		PictureGenerator.generate(false, 5, 4, "test", "hsv", false, 15);
//		endTime = System.currentTimeMillis();
//		System.out.println((endTime - startTime));
//
//		startTime = System.currentTimeMillis();
//		PictureGenerator.generate(false, 5, 5, "test", "hsv", false, 15);
//		endTime = System.currentTimeMillis();
//		System.out.println((endTime - startTime));
//		
//		startTime = System.currentTimeMillis();
//		PictureGenerator.generate(false, 5, 10, "test", "hsv", false, 15);
//		endTime = System.currentTimeMillis();
//		System.out.println((endTime - startTime));
	}
}
