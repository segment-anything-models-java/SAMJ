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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class DouglasPeucker {
    /**
     * Simplify a polyline using the Ramer–Douglas–Peucker algorithm.
     *
     * @param points  Original list of points (ordered, endpoints included).
     * @param epsilon Max allowed perpendicular distance (in same units as your coords).
     * @return A new List<Point2D> containing only the “significant” vertices.
     */
    public static List<Point2D> simplify(List<Point2D> points, double epsilon) {
        if (points == null || points.size() < 3) {
            // Nothing to simplify if fewer than 3 points
            return new ArrayList<>(points);
        }
        // Find the point with the maximum distance from the line between endpoints
        double maxDist = 0;
        int index = 0;
        Point2D start = points.get(0);
        Point2D end   = points.get(points.size() - 1);
        for (int i = 1; i < points.size() - 1; i++) {
            double dist = perpendicularDistance(points.get(i), start, end);
            if (dist > maxDist) {
                maxDist = dist;
                index   = i;
            }
        }

        // If max distance is greater than epsilon, recursively simplify
        if (maxDist > epsilon) {
            // Recursive calls
            List<Point2D> left  = simplify(points.subList(0, index + 1), epsilon);
            List<Point2D> right = simplify(points.subList(index, points.size()), epsilon);

            // Combine, removing duplicate at the join
            List<Point2D> result = new ArrayList<>(left);
            result.remove(result.size() - 1);
            result.addAll(right);
            return result;
        } else {
            // Otherwise, just return the two endpoints
            List<Point2D> result = new ArrayList<>();
            result.add(start);
            result.add(end);
            return result;
        }
    }

    /**
     * Compute perpendicular distance from point p to the line through p0–p1.
     */
    private static double perpendicularDistance(Point2D p, Point2D p0, Point2D p1) {
        double dx = p1.getX() - p0.getX();
        double dy = p1.getY() - p0.getY();
        // Normalize (to avoid dividing by zero)
        double mag = Math.hypot(dx, dy);
        if (mag == 0) {
            // p0 and p1 are the same point
            return p.distance(p0);
        }
        // Area of parallelogram / base length = height
        double cross = Math.abs(dy * (p.getX() - p0.getX())
                              - dx * (p.getY() - p0.getY()));
        return cross / mag;
    }

    // Example usage
    public static void main(String[] args) {
        List<Point2D> contour = new ArrayList<>();
        // ... populate contour with one Point2D per pixel along your ROI boundary ...

        double epsilon = 2.0; // e.g., remove detail smaller than 2 pixels
        List<Point2D> simplified = simplify(contour, epsilon);

        System.out.println("Original vertex count:   " + contour.size());
        System.out.println("Simplified vertex count: " + simplified.size());
    }
}
