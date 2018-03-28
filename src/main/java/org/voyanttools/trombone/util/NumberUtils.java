package org.voyanttools.trombone.util;

import java.util.stream.IntStream;

public class NumberUtils {

	public static double[] getDoubles(float[] floats) {
		return IntStream.range(0, floats.length).mapToDouble(i -> floats[i]).toArray();
	}
	public static float[] getFloats(double[] doubles) {
		float[] floats = new float[doubles.length];
		for (int i=0; i<floats.length; i++) {
			floats[i] = (float) doubles[i];
		}
		return floats;
	}
}
