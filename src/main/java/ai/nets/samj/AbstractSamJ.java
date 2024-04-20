/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.awt.Polygon;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Class that contains methods that can be sued by SAMJ models
 * @author Vladimir Ulman
 * @author Carlos Garcia
 */
public class AbstractSamJ {
	
	protected static int LOWER_REENCODE_THRESH = 50;
	
	protected static int OPTIMAL_BBOX_IM_RATIO = 10;
	
	protected static double UPPER_REENCODE_THRESH = 1.1;
	
	public static long MAX_ENCODED_IMAGE_SIZE = 2048;
	
	protected static long ENCODE_MARGIN = 20;

	/** Essentially, a syntactic-shortcut for a String consumer */
	public interface DebugTextPrinter { void printText(String text); }
	/**
	 * Default String consumer that just prints the Strings that are inputed with {@link System#out}
	 */
	protected DebugTextPrinter debugPrinter = System.out::println;
	/**
	 * Whether the SAMJ model instance is verbose or not
	 */
	protected boolean isDebugging = true;

	/**
	 * Set an empty consumer as {@link DebugTextPrinter} to avoid the SAMJ model instance
	 * to communicate its process
	 */
	public void disableDebugPrinting() {
		debugPrinter = (text) -> {};
	}
	
	/**
	 * Set the {@link DebugTextPrinter} that wants to be used in the model
	 * @param lineComsumer
	 * 	the {@link DebugTextPrinter} (which is basically a String consumer used to communicate the
	 *  SAMJ model instance process) that wants to be used
	 */
	public void setDebugPrinter(final DebugTextPrinter lineComsumer) {
		if (lineComsumer != null) debugPrinter = lineComsumer;
	}
	
	/**
	 * Set whether the SAMJ model instance has to be more verbose or not
	 * @param newState
	 * 	whether the new model is verbose or not
	 */
	public void setDebugging(boolean newState) {
		isDebugging = newState;
	}
	
	/**
	 * 
	 * @return true if the SAMJ model instance is verbose or not
	 */
	public boolean isDebugging() {
		return isDebugging;
	}

	/**
	 * Method that prints the String in the script parameter to the {@link DebugTextPrinter}
	 * 
	 * @param script
	 * 	text that wants to be printed, usually a Python script
	 * @param designationOfTheScript
	 * 	the name (or some string to design) of the text that is going to be printed
	 */
	public void printScript(final String script, final String designationOfTheScript) {
		if (!isDebugging) return;
		debugPrinter.printText("START: =========== "+designationOfTheScript+" ===========");
		debugPrinter.printText(LocalDateTime.now().toString());
		debugPrinter.printText(script);
		debugPrinter.printText("END:   =========== "+designationOfTheScript+" ===========");
	}

	/**
	 * Get the maximum and minimum pixel values of an {@link IterableInterval}
	 * @param <T>
	 * 	the ImgLib2 data types that the {@link IterableInterval} can have
	 * @param inImg
	 * 	the {@link IterableInterval} from which the max and min values are going to be found
	 * @param outMinMax
	 * 	double array where the max and min values of the {@link IterableInterval} will be written
	 */
	public static <T extends RealType<T> & NativeType<T>>
	void getMinMaxPixelValue(final IterableInterval<T> inImg, final double[] outMinMax) {
		double min = inImg.firstElement().getRealDouble();
		double max = min;

		for (T px : inImg) {
			double val = px.getRealDouble();
			min = Math.min(min,val);
			max = Math.max(max,val);
		}

		if (outMinMax.length > 1) {
			outMinMax[0] = min;
			outMinMax[1] = max;
		}
	}

	/**
	 * Whether the values in the length 2 array are between 0 and 1
	 * @param inMinMax
	 * 	the interval to be evaluated
	 * @return true if the values are between 0 and 1 and false otherwise
	 */
	public static boolean isNormalizedInterval(final double[] inMinMax) {
		return (inMinMax[0] >= 0 && inMinMax[0] <= 1
			&& inMinMax[1] >= 0 && inMinMax[1] <= 1);
	}

	/**
	 * Normalize the {@link RandomAccessibleInterval} with the position 0 of the inMimMax array as the min
	 * and the position 1 as the max
	 * @param <T>
	 * 	the ImgLib2 data types that the {@link RandomAccessibleInterval} can have
	 * @param inImg
	 *  {@link RandomAccessibleInterval} to be normalized
	 * @param inMinMax
	 * 	 the values to which the {@link RandomAccessibleInterval} will be normalized. Should be a double array of length
	 *   2 with the smaller value at position 0
	 * @return the normalized {@link RandomAccessibleInterval}
	 */
	private static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<FloatType> normalizedView(final RandomAccessibleInterval<T> inImg, final double[] inMinMax) {
		final double min = inMinMax[0];
		final double range = inMinMax[1] - min;
		return Converters.convert(inImg, (i, o) -> o.setReal((i.getRealFloat() - min) / (range + 1e-9)), new FloatType());
	}

	/**
	 * Checks the input RAI if its min and max pixel values are between [0,1].
	 * If they are not, the RAI will be subject to {@link Converters#convert(RandomAccessibleInterval, Converter, Type)}
	 * with here-created Converter that knows how to bring the pixel values into the interval [0,1].
	 *
	 * @param <T>
	 * 	the ImgLib2 data types that the {@link RandomAccessibleInterval} can have
	 * @param inImg
	 *  RAI to be potentially normalized.
	 * @return The input image itself or a View of it with {@link FloatType} data type
	 */
	public <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<FloatType> normalizedView(final RandomAccessibleInterval<T> inImg) {
		final double[] minMax = new double[2];
		getMinMaxPixelValue(Views.iterable(inImg), minMax);
		///debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ NORMALIZED, returning Converted view");
		//return normalizedView(inImg, minMax);
		if (isNormalizedInterval(minMax) && Util.getTypeFromInterval(inImg) instanceof FloatType) {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS NORMALIZED, returning directly itself");
			return Cast.unchecked(inImg);
		} else if (isNormalizedInterval(minMax)) {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS NORMALIZED, returning directly itself");
			return  Converters.convert(inImg, (i, o) -> o.setReal(i.getRealFloat()), new FloatType());
		} else {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ NORMALIZED, returning Converted view");
			return normalizedView(inImg, minMax);
		}
	}

	private static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<UnsignedByteType> convertViewToRGB(final RandomAccessibleInterval<T> inImg, final double[] inMinMax) {
		final double min = inMinMax[0];
		final double range = inMinMax[1] - min;
		return Converters.convert(inImg, (i, o) -> o.setReal(255 * (i.getRealDouble() - min) / range), new UnsignedByteType());
	}

	/**
	 * Checks the input RAI if its min and max pixel values are between [0,255] and if it is of {@link UnsignedByteType} type.
	 * If they are not, the RAI will be subject to {@link Converters#convert(RandomAccessibleInterval, Converter, Type)}
	 * with here-created Converter that knows how to bring the pixel values into the interval [0,255].
	 *
	 * @param inImg
	 *  RAI to be potentially converted to RGB.
	 * @return The input image itself or a View of it in {@link UnsignedByteType} data type
	 */
	public <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<UnsignedByteType> convertViewToRGB(final RandomAccessibleInterval<T> inImg) {
		if (Util.getTypeFromInterval(inImg) instanceof UnsignedByteType) {
			debugPrinter.printText("IMAGE IS RGB, returning directly itself");
			return Cast.unchecked(inImg);
		}
		final double[] minMax = new double[2];
		debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ RGB, returning Converted view");
		getMinMaxPixelValue(Views.iterable(inImg), minMax);
		return convertViewToRGB(inImg, minMax);
	}
	
	protected static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> 
	reescaleIfNeeded(RandomAccessibleInterval<T> rai) {
		if ((rai.dimensionsAsLongArray()[0] > rai.dimensionsAsLongArray()[1])
				&& (rai.dimensionsAsLongArray()[0] > MAX_ENCODED_IMAGE_SIZE)) {
			// TODO reescale
			return rai;
		} else if ((rai.dimensionsAsLongArray()[0] < rai.dimensionsAsLongArray()[1])
				&& (rai.dimensionsAsLongArray()[1] > MAX_ENCODED_IMAGE_SIZE)) {
			// TODO reescale
			return rai;
		} else {
			return rai;
		}
	}
	
	/**
	 * Calculate the coordinates of the encoded image with respect to the coordinates
	 * of the bounding box that contain the area that we need to crop and encode for SAM to have
	 * the needed resolution to sement properly the object of interest.
	 * 
	 * This will only be used on big images.
	 * 
	 * @param boundingBox
	 * 	coordinates of the bounding box
	 * @param imageSize
	 * 	total size of the image that wants to be annotated
	 * @return the distance in each direction with respect to the coordinates of the bounding box that
	 * 	indicate the crop that needs to be encoded
	 */
	protected long[] calculateEncodingNewCoords(int[] boundingBox, long[] imageSize) {
		long xSize = boundingBox[2] - boundingBox[0]; 
		long ySize = boundingBox[3] - boundingBox[1];
		long smallerSize = ySize < xSize ? ySize * OPTIMAL_BBOX_IM_RATIO : xSize * OPTIMAL_BBOX_IM_RATIO;
		long biggerSize = smallerSize * 3;
		if ((ySize < xSize) && (ySize * 3 > xSize)) {
			biggerSize = xSize * OPTIMAL_BBOX_IM_RATIO;
		} else if ((ySize > xSize) && (xSize * 3 > ySize)) {
			biggerSize = ySize * OPTIMAL_BBOX_IM_RATIO;
		}
		long[] newSize = new long[] {biggerSize, smallerSize};
		if (ySize > xSize) newSize = new long[] {smallerSize, biggerSize};
		long[] posWrtBbox = new long[4];
		posWrtBbox[0] = (long) Math.max(0, Math.ceil((boundingBox[0] + xSize / 2) - newSize[0] / 2));
		posWrtBbox[1] = (long) Math.max(0, Math.ceil((boundingBox[1] + ySize / 2) - newSize[1] / 2));
		posWrtBbox[2] = (long) Math.min(imageSize[1], Math.floor((boundingBox[2] + xSize / 2) + newSize[0] / 2));
		posWrtBbox[3] = (long) Math.min(imageSize[0], Math.floor((boundingBox[3] + ySize / 2) + newSize[1] / 2));
		return posWrtBbox;
	}
	
	/**
	 * Method that recalculates the coordinates of the polygons outputed by SAMJ.
	 * 
	 * This method is usually for big images. In order to create encoding with enough resolution
	 * to detect small objects compared to the size of the whole image, SAMJ might encode crops of
	 * the total image, thus the coordinates of the polygons obtained need to be shifted in order
	 * to match the original image.
	 * @param polys
	 * 	polys obtained by SAMJ on the encoded crop
	 * @param encodeCoords
	 * 	position of the crop in the total image
	 */
	protected void recalculatePolys(List<Polygon> polys, long[] encodeCoords) {
		polys.stream().forEach(pp -> {
			pp.xpoints = Arrays.stream(pp.xpoints).map(x -> x + (int) encodeCoords[0]).toArray();
			pp.ypoints = Arrays.stream(pp.ypoints).map(y -> y + (int) encodeCoords[1]).toArray();
		});
	}
}
