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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Class that contains the information required to create the contour and mask about the object annotated in an image
 * in a fast and efficient manner.
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class Mask {

	private Polygon contour;
	
	private String name;
	
	private final String uuid = UUID.randomUUID().toString();
	
	public long[] rleEncoding;
	
	// TODO does it make sense to measure the level of simplification once a polygon is give?
	// TODO at the moment we assume it is always one vertix per pixel
	private double simplification = 0;
	
	private HashMap<Double, Polygon> memory = new HashMap<Double, Polygon>();
	
	private boolean rleValid = true;
	
	private static final double COMPLEXITY_DELTA = 0.5;
	
	private Mask(Polygon contour, long[] rleEncoding) {
		this.contour = contour;
		this.rleEncoding = rleEncoding;
		memory.put(simplification, contour);
	}
	
	public static Mask build(Polygon contour, long[] rleEncoding) {
		return new Mask(contour, rleEncoding);
	}
	
	public void simplify() {
		if (this.memory.get((this.simplification + COMPLEXITY_DELTA)) != null) {
			simplification += COMPLEXITY_DELTA;
			this.rleValid = simplification == 0;
			this.contour = memory.get(simplification);
			return;
		}
		List<Point2D> points = new ArrayList<Point2D>();
		for (int i = 0; i < getContour().npoints; i ++) {
			points.add(new Point2D.Double(getContour().xpoints[i], getContour().ypoints[i]));
		}
		List<Point2D> simple = DouglasPeucker.simplify(points, (this.simplification + COMPLEXITY_DELTA));
		simplification += COMPLEXITY_DELTA;
		this.rleValid = simplification == 0;

		int[] xArr = simple.stream().mapToInt(pp -> (int) pp.getX()).toArray();
		int[] yArr = simple.stream().mapToInt(pp -> (int) pp.getY()).toArray();
		Polygon outer = new Polygon(xArr, yArr, xArr.length);
		
        contour = new Polygon();
        int n = outer.npoints;
        for (int i = n - 1; i >= 0; i --) {
        	if (outer.xpoints[i] == outer.xpoints[((i+1)%n)]
        			&& outer.ypoints[i] == outer.xpoints[((i+1)%n)])
        		continue;
        	contour.addPoint(outer.xpoints[i], outer.ypoints[i]);
        }
		memory.put(simplification, contour);
	}
	
	public void complicate() {
		if (this.memory.get((this.simplification - COMPLEXITY_DELTA)) != null) {
			simplification -= COMPLEXITY_DELTA;
			this.rleValid = simplification == 0;
			this.contour = memory.get(simplification);
			return;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		Objects.requireNonNull(name, "argument 'name' cannot be null");
		this.name = name;
	}
	
	public String getUUID() {
		return uuid;
	}
	
	public Polygon getContour() {
		return this.contour;
	}
	
	public long[] getRLEMask() {
		if (this.rleValid)
			return this.rleEncoding;
		// TODO implement
		List<Integer> rle = PolygonToRLE.contourToRLE(null, 200, 200);
		return this.rleEncoding;
	}
	
	public double getComplicationLevel() {
		return this.simplification;
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
	public static RandomAccessibleInterval<UnsignedShortType> getMask(long width, long height, List<Mask> masks) {
		short[] arr = new short[(int) (width * height)];
		int n = 1;
		for (Mask mask : masks) {
			long[] rle = mask.getRLEMask();
			for (int i = 0; i < rle.length; i += 2) {
				int start = (int) mask.getRLEMask()[i];
				int len = (int) mask.getRLEMask()[i+ 1];
				Arrays.fill(arr, start, start + len, (short) n);
			}
			n ++;
		}
		//return Utils.transpose(ArrayImgs.unsignedBytes(arr, new long[] {height, width}));
		return (ArrayImgs.unsignedShorts(arr, new long[] {width, height}));
	}

	public void clear() {
		this.contour = null;
		this.memory = new HashMap<>();
		this.rleValid = false;
		this.rleEncoding = new long[0];
		this.simplification = 0;
	}

	public void setContour(Polygon pol) {
		this.clear();
		this.contour = pol;		
	}
}
