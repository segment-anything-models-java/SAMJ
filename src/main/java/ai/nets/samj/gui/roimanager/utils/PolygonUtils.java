package ai.nets.samj.gui.roimanager.utils;

import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.proteovir.roimanager.utils.StraightSkeletonOffset.Point;

/**
 * Utility class for dilating (expanding) or eroding (contracting) a polygon
 * represented as an array of Point2D.Double without external dependencies.
 */
public class PolygonUtils {
    /**
     * Builds a closed Path2D from the given points.
     */
    private static Path2D.Double buildPath(Point2D.Double[] pts) {
        if (pts == null || pts.length < 3) {
            throw new IllegalArgumentException("A polygon must have at least 3 points");
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(pts[0].x, pts[0].y);
        for (int i = 1; i < pts.length; i++) {
            path.lineTo(pts[i].x, pts[i].y);
        }
        path.closePath();
        return path;
    }

    /**
     * Extracts the outline points from a shape by flattening curves to line segments.
     * 
     * @param area
     * 	the area from what we will extract the polygon
     * @param flatness
     * 	how similar to the area the polygon should be
     * @return
     */
    private static Polygon extractPoints(Area area, double flatness) {
        List<List<Point>> rings = new ArrayList<>();
        PathIterator pit = new FlatteningPathIterator(area.getPathIterator(null), flatness);
        double[] coords = new double[6];
        List<Point> current = null;
        boolean inRing = false;

        while (!pit.isDone()) {
            int type = pit.currentSegment(coords);
            switch (type) {
              case PathIterator.SEG_MOVETO:
                current = new ArrayList<Point>();
                rings.add(current);
                current.add(
                  new Point(Math.round(coords[0]), Math.round(coords[1]))
                );
                inRing = true;
                break;

              case PathIterator.SEG_LINETO:
                if (inRing) {
                    current.add(
                      new Point(Math.round(coords[0]), Math.round(coords[1]))
                    );
                }
                break;

              case PathIterator.SEG_CLOSE:
                if (inRing) {
                    // close the ring by repeating first point
                    // TODO current.add(current.get(0));
                    inRing = false;
                }
                break;
            }
            pit.next();
        }

        // 2) Pick the ring with the largest absolute area
        List<Point> outer = null;
        double maxArea = -1;
        for (List<Point> ring : rings) {
            double a = Math.abs(signedArea(ring));
            if (a > maxArea) {
                maxArea = a;
                outer = ring;
            }
        }
        if (outer == null)
        	return null;
        int n = outer.size();
        List<Point> points = new ArrayList<Point>();
        for (int i = n - 1; i >= 0; i --) {
        	if (outer.get(i).x == outer.get((i+1)%n).x
        			&& outer.get(i).y == outer.get((i+1)%n).y)
        		continue;
        	points.add(new Point(outer.get(i).x, outer.get(i).y));
        }

        return new Polygon(
        		points.stream().mapToInt(p -> (int) p.x).toArray(),
        		points.stream().mapToInt(p -> (int) p.y).toArray(),
        		points.size()
        		);
    }

    /** Shoelace formula: positive or negative depending on winding */
    private static double signedArea(List<Point> p) {
        double sum = 0;
        int n = p.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            sum += (p.get(j).x * p.get(j).y - p.get(i).x * p.get(j).y);
        }
        return sum / 2.0;
    }


    /**
     * Returns a new polygon offset outward by the given distance (dilation).
     * @param polygon Original polygon vertices (must form a closed shape)
     * @param distance Offset distance (>0 for dilation)
     * @return New polygon vertices after dilation
     */
    public static Polygon dilate(Polygon polygon, double distance) {
    	List<Point> pols = new ArrayList<Point>();
    	for (int i = 0; i < polygon.npoints; i ++) {
    		pols.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
    	}
    	List<List<Point>> possiblePols2 = StraightSkeletonOffset.computeOffset(pols, distance);
    	Polygon out2 = new Polygon(
    			possiblePols2.get(0).stream().mapToInt(p -> (int) p.x).toArray(),
    			possiblePols2.get(0).stream().mapToInt(p -> (int) p.y).toArray(),
    			possiblePols2.get(0).size()
    		);
    	List<List<Point>> possiblePols = StraightSkeletonOffset.computeOffset(pols, -distance);
    	Polygon out = new Polygon(
    			possiblePols.get(0).stream().mapToInt(p -> (int) p.x).toArray(),
    			possiblePols.get(0).stream().mapToInt(p -> (int) p.y).toArray(),
    			possiblePols.get(0).size()
    		);
    	return merge(out, out2);
    }

    /**
     * Returns a new polygon offset inward by the given distance (erosion).
     * @param polygon Original polygon vertices (must form a closed shape)
     * @param distance Offset distance (>0 for erosion amount)
     * @return New polygon vertices after erosion
     */
    public static Polygon erode(Polygon polygon, double distance) {
    	List<Point> pols = new ArrayList<Point>();
    	for (int i = 0; i < polygon.npoints; i ++) {
    		pols.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
    	}
    	List<List<Point>> possiblePols2 = StraightSkeletonOffset.computeOffset(pols, distance);
    	Polygon out2 = new Polygon(
    			possiblePols2.get(0).stream().mapToInt(p -> (int) p.x).toArray(),
    			possiblePols2.get(0).stream().mapToInt(p -> (int) p.y).toArray(),
    			possiblePols2.get(0).size()
    		);
    	List<List<Point>> possiblePols = StraightSkeletonOffset.computeOffset(pols, -distance);
    	Polygon out = new Polygon(
    			possiblePols.get(0).stream().mapToInt(p -> (int) p.x).toArray(),
    			possiblePols.get(0).stream().mapToInt(p -> (int) p.y).toArray(),
    			possiblePols.get(0).size()
    		);
    	Polygon finalPol =  intersect(out, out2);
    	if (finalPol == null)
    		return polygon;
    	else
    		return finalPol;
    }

    /**
     * Checks whether two polygons overlap (i.e., have any intersecting area).
     * @param pol1 Vertices of the first polygon
     * @param pol2 Vertices of the second polygon
     * @return true if the intersection area is non-empty
     */
    public static boolean overlaps(Polygon pol1, Polygon pol2) {
    	Point2D.Double[] pts1 = new Point2D.Double[pol1.npoints];
    	for (int i = 0; i < pol1.npoints; i ++) {
    		pts1[i] = new Point2D.Double(pol1.xpoints[i], pol1.ypoints[i]);
    	}
    	Point2D.Double[] pts2 = new Point2D.Double[pol2.npoints];
    	for (int i = 0; i < pol2.npoints; i ++) {
    		pts2[i] = new Point2D.Double(pol2.xpoints[i], pol2.ypoints[i]);
    	}
        // Build Path2D for each polygon
        Path2D.Double path1 = buildPath(pts1);
        Path2D.Double path2 = buildPath(pts2);

        // Create Area objects and intersect them
        Area area1 = new Area(path1);
        Area area2 = new Area(path2);
        area1.intersect(area2);

        // If the resulting area is not empty, they overlap
        return !area1.isEmpty();
    }

    /**
     * Merges two overlapping polygons into a single combined region.
     * @param pol1 Vertices of the first polygon
     * @param pol2 Vertices of the second polygon
     * @return Vertices of the merged polygon
     */
    public static Polygon merge(Polygon pol1, Polygon pol2) {
    	Point2D.Double[] pts1 = new Point2D.Double[pol1.npoints];
    	for (int i = 0; i < pol1.npoints; i ++) {
    		pts1[i] = new Point2D.Double(pol1.xpoints[i], pol1.ypoints[i]);
    	}
    	Point2D.Double[] pts2 = new Point2D.Double[pol2.npoints];
    	for (int i = 0; i < pol2.npoints; i ++) {
    		pts2[i] = new Point2D.Double(pol2.xpoints[i], pol2.ypoints[i]);
    	}
        // Build areas for both polygons
        Area area1 = new Area(buildPath(pts1));
        Area area2 = new Area(buildPath(pts2));

        // Union the shapes
        area1.add(area2);

        // Flatten and extract outline points
        double flatness = 0.1;
        return extractPoints(area1, flatness);
    }

    /**
     * Merges two overlapping polygons into a single combined region.
     * @param pol1 Vertices of the first polygon
     * @param pol2 Vertices of the second polygon
     * @return Vertices of the merged polygon
     */
    private static Polygon intersect(Polygon pol1, Polygon pol2) {
    	Point2D.Double[] pts1 = new Point2D.Double[pol1.npoints];
    	for (int i = 0; i < pol1.npoints; i ++) {
    		pts1[i] = new Point2D.Double(pol1.xpoints[i], pol1.ypoints[i]);
    	}
    	Point2D.Double[] pts2 = new Point2D.Double[pol2.npoints];
    	for (int i = 0; i < pol2.npoints; i ++) {
    		pts2[i] = new Point2D.Double(pol2.xpoints[i], pol2.ypoints[i]);
    	}
        // Build areas for both polygons
        Area area1 = new Area(buildPath(pts1));
        Area area2 = new Area(buildPath(pts2));

        // Union the shapes
        area1.intersect(area2);

        // Flatten and extract outline points
        double flatness = 0.1;
        return extractPoints(area1, flatness);
    }
}
