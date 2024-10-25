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
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
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
	
	public static RandomAccessibleInterval<UnsignedByteType> getMask(long width, long height, List<Mask> objects) {
		return null;
	}
}
