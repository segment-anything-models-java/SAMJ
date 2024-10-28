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
package ai.nets.samj.annotation;

import java.awt.Polygon;
import java.util.Arrays;
import java.util.List;

import io.bioimage.modelrunner.tensor.Utils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;

/**
 * Class that contains the information required to create the contour and mask about the object annotated in an image
 * in a fast and efficient manner.
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class Mask {

	private final Polygon contour;
	
	private final long[] rleEncoding;
	
	private Mask(Polygon contour, long[] rleEncoding) {
		this.contour = contour;
		this.rleEncoding = rleEncoding;
	}
	
	public static Mask build(Polygon contour, long[] rleEncoding) {
		return new Mask(contour, rleEncoding);
	}
	
	public Polygon getContour() {
		return this.contour;
	}
	
	public long[] getRLEMask() {
		return this.rleEncoding;
	}
	
	/**
	 * Mehtod that creates an annotation mask from several object masks in an efficient manner using RLE algorithm
	 * @param width
	 * 	width of the image
	 * @param height
	 * 	height of the image
	 * @param masks
	 * 	all the masks of the objects image
	 * @return the whole mask with all the objects
	 */
	public static RandomAccessibleInterval<UnsignedByteType> getMask(long width, long height, List<Mask> masks) {
		byte[] arr = new byte[(int) (width * height)];
		
		for (Mask mask : masks) {
			long[] rle = mask.getRLEMask();
			for (int i = 0; i < rle.length; i += 2) {
				int start = (int) mask.getRLEMask()[i];
				int len = (int) mask.getRLEMask()[i+ 1];
				Arrays.fill(arr, start, start + len, (byte) 1);
			}
		}
		return Utils.transpose(ArrayImgs.unsignedBytes(arr, new long[] {height, width}));
	}
}
