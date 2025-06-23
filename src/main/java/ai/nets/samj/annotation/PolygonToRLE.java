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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PolygonToRLE {
    static class Edge {
        double x0, y0, x1, y1;
        double ymin, ymax;
        // slope‐inverse: dx/dy
        double dxdy;
        public Edge(Point2D a, Point2D b) {
            // ensure y0 <= y1
            if (a.getY() <= b.getY()) {
                x0 = a.getX();  y0 = a.getY();
                x1 = b.getX();  y1 = b.getY();
            } else {
                x0 = b.getX();  y0 = b.getY();
                x1 = a.getX();  y1 = a.getY();
            }
            ymin = y0;  ymax = y1;
            dxdy = (x1 - x0) / (y1 - y0);
        }
        /** x‐intersection at scanline y+0.5 */
        public double intersectX(double scanY) {
            return x0 + dxdy * (scanY - y0);
        }
    }

    /**
     * @param contour   Must be closed (first==last) or will be treated as such.
     * @param width     Image width in pixels.
     * @param height    Image height in pixels.
     * @return COCO‐style RLE: alternating background/foreground run lengths.
     */
    public static List<Integer> contourToRLE(
            List<Point2D> contour, int width, int height) {
        // 1) Build edge table
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < contour.size() - 1; i++) {
            Point2D a = contour.get(i), b = contour.get(i+1);
            if (!a.equals(b) && a.getY() != b.getY()) {
                edges.add(new Edge(a, b));
            }
        }
        // Sort edges by ymin for activation
        edges.sort(Comparator.comparingDouble(e -> e.ymin));

        List<Integer> runs = new ArrayList<>();
        int currentVal = 0, currentRun = 0;
        // Active edges for this scan‐line
        List<Edge> active = new ArrayList<>();
        int ei = 0;  // pointer into edges

        // 2) For each scan‐line
        for (int y = 0; y < height; y++) {
            double scanY = y + 0.5;

            // Activate edges whose ymin <= scanY < ymax
            while (ei < edges.size() && edges.get(ei).ymin <= scanY) {
                if (edges.get(ei).ymax > scanY) {
                    active.add(edges.get(ei));
                }
                ei++;
            }
            // Remove edges that end at or below scanY
            active.removeIf(e -> e.ymax <= scanY);

            // 3) Compute intersections
            List<Double> xs = new ArrayList<>(active.size());
            for (Edge e : active) {
                xs.add(e.intersectX(scanY));
            }
            Collections.sort(xs);

            // 4) Walk spans and accumulate RLE
            int prevX = 0;
            for (int i = 0; i+1 < xs.size(); i += 2) {
                int x0 = (int)Math.ceil(xs.get(i));
                int x1 = (int)Math.floor(xs.get(i+1));
                if (x1 < x0) continue;
                // background run [prevX, x0)
                currentRun = appendRun(runs, currentVal, currentRun, x0 - prevX);
                currentVal = 1 - currentVal;
                // foreground run [x0, x1+1)
                currentRun = appendRun(runs, currentVal, currentRun, (x1+1) - x0);
                currentVal = 1 - currentVal;
                prevX = x1 + 1;
            }
            // final background run to end of row
            currentRun = appendRun(runs, currentVal, currentRun, width - prevX);
            // ensure we’re back to background for next row
            if (currentVal != 0) {
                currentVal = 0;
                if (currentRun > 0) {
                    runs.add(currentRun);
                    currentRun = 0;
                }
            }
        }
        // 5) Flush last run
        if (currentRun > 0) {
            runs.add(currentRun);
        }
        return runs;
    }

    /** Helper: either extend or flush & start new run. */
    private static int appendRun(List<Integer> runs, int currVal, int currRun, int length) {
        if (length <= 0) return currRun;
        if (currVal == 0) {
            // extending current background run
            return currRun + length;
        } else {
            // flush previous foreground run
            runs.add(currRun);
            // start new background run of size `length`
            return length;
        }
    }
}
