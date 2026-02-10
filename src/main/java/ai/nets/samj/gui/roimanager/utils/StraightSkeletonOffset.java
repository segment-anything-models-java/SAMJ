package ai.nets.samj.gui.roimanager.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * StraightSkeletonOffset: compute the straight skeleton offset contours
 * of a simple polygon using the wavefront propagation (Aichholzer et al.)
 *
 * Supports convex and non-convex simple polygons (without holes).
 * Handles both edge events (edge collapse) and split events (reflex vertices).
 */
public class StraightSkeletonOffset {
    private static final double EPS = 1e-9;

    public static void main(String[] args) {
        // Example: non-convex polygon
        List<Point> poly = Arrays.asList(
            new Point(0,0), new Point(4,0), new Point(4,3),
            new Point(3,2), new Point(0,3)
        );
        double r = 10.0;
        List<List<Point>> contours = computeOffset(poly, r);
        contours.forEach(c -> System.out.println(c));
    }

    /**
     * Compute offset contours at distance r from input polygon.
     */
    public static List<List<Point>> computeOffset(List<Point> polygon, double r) {
        // 1) Build initial wavefront edges
        List<WaveEdge> wave = new ArrayList<>();
        int n = polygon.size();
        for (int i = n - 1; i >= 0; i --) {
        	if (polygon.get(i).x == polygon.get((i+1)%polygon.size()).x 
        			&& polygon.get(i).y == polygon.get((i+1)%polygon.size()).y)
        		polygon.remove(i);
        }
        n = polygon.size();
        for (int i = 0; i < n; i++) {
            WaveEdge e = new WaveEdge(polygon.get(i), polygon.get((i+1)%n));
            wave.add(e);
        }
        for (int i = 0; i < wave.size(); i++) {
            WaveEdge e = wave.get(i);
            e.prev = wave.get((i - 1 + wave.size()) % wave.size());
            e.next = wave.get(( i + 1) % wave.size());
        }

        /**
        PriorityQueue<Event> pq = new PriorityQueue<>();
        for (WaveEdge e : wave) {
            scheduleEdgeEvent(e, pq);
            //scheduleSplitEvent(e, wave, pq);
        }
        */
        // 3) Propagate until PQ empty or next event time > r
        List<WaveEdge> current = new ArrayList<>(wave);
        /**
        while (!pq.isEmpty()) {
            Event ev = pq.poll();
            if (ev.time > r) break;
            if (!current.contains(ev.edge)) continue; // stale
            if (ev.type == Event.Type.EDGE) {
                handleEdgeEvent(ev.edge, current, pq);
            } else {
                handleSplitEvent(ev, current, pq);
            }
        }
        */

        // 4) Extract offset contours at distance r
        return extractContours(current, r);
    }

    /** Schedule collapse of edge (when its length goes to 0). */
    private static void scheduleEdgeEvent(WaveEdge e, PriorityQueue<Event> pq) {
        if (e.prev == null || e.next == null) return;
        double t = computeEdgeEventTime(e.prev, e, e.next);
        if (t > EPS && !Double.isInfinite(t)) {
            pq.add(new Event(t, e, Event.Type.EDGE));
        }
    }

    /** Compute time when edge e collapses: find t s.t. offset edge length = 0. */
    private static double computeEdgeEventTime(WaveEdge a, WaveEdge b, WaveEdge c) {
        double lo = 0, hi = b.length()/2;
        /**
         * The hardcoded 40 here represents a fixed number of iterations for the binary search,
         * trading off between speed and precision. Each iteration halves the search interval,
         * so 40 iterations yields a precision on the order of ≈1e-12 (since (0.5)^40 ≈ 9.1e-13).
         * To make this clearer and more maintainable, you could replace the fixed count with a while
         * loop that continues until hi - lo < EPS or some tolerance threshold, ensuring you achieve
         * the required accuracy without over-iterating.
         */
        for (int i = 0; i < 40; i++) {
            double mid = 0.5*(lo+hi);
            Point p1 = intersection(a.getOffsetLine(mid), b.getOffsetLine(mid));
            Point p2 = intersection(b.getOffsetLine(mid), c.getOffsetLine(mid));
            if (p1.distance(p2) <= EPS) hi = mid;
            else lo = mid;
        }
        return hi;
    }

    /** Schedule split events for reflex vertices: when reflex offset front hits another edge. */
    private static void scheduleSplitEvent(WaveEdge e, List<WaveEdge> wave, PriorityQueue<Event> pq) {
        // only at reflex corner between e.prev->e
        if (e.prev == null) return;
        if (isConvex(e.prev.a, e.prev.b, e.b)) return;
        // compute initial vertex location and bisector
        Point v = e.prev.b;
        Vector n1 = e.prev.normal();
        Vector n2 = e.normal();
        Vector bis = n1.add(n2).normalize();
        // ray: v + bis * t hits offset of other edges
        for (WaveEdge f : wave) {
            if (f == e || f == e.prev || f == e.next) continue;
            Line L = f.supportingLine();
            // intersect ray with supporting line
            double denom = L.a*bis.x + L.b*bis.y;
            if (Math.abs(denom) < EPS) continue;
            double d0 = ( -L.c - L.a*v.x - L.b*v.y ) / denom;
            if (d0 <= EPS) continue;
            // ensure intersection lies within f segment at time d0 (approx)
            Point ip = new Point(v.x + bis.x*d0, v.y + bis.y*d0);
            if (!f.containsProjection(ip)) continue;
            pq.add(new Event(d0, e, Event.Type.SPLIT, f));
        }
    }

    private static boolean isConvex(Point a, Point b, Point c) {
        return cross(b.subtract(a), c.subtract(b)) >= 0;
    }

    /** Handle edge collapse: remove e, relink, reschedule neighbors. */
    private static void handleEdgeEvent(WaveEdge e, List<WaveEdge> wave, PriorityQueue<Event> pq) {
        WaveEdge p = e.prev;
        WaveEdge q = e.next;
        if (p == null || q == null) return;
        // link
        p.next = q;
        q.prev = p;
        wave.remove(e);
        // reschedule p, q
        pq.removeIf(x -> x.edge == p);
        pq.removeIf(x -> x.edge == q);
        scheduleEdgeEvent(p, pq);
        //scheduleSplitEvent(p, wave, pq);
        scheduleEdgeEvent(q, pq);
        //scheduleSplitEvent(q, wave, pq);
    }

    /** Handle split event: insert new vertex and two new edges. */
    private static void handleSplitEvent(Event ev, List<WaveEdge> wave, PriorityQueue<Event> pq) {
        WaveEdge e = ev.edge;
        WaveEdge f = ev.target;
        // compute split point at time t
        Point v = e.prev.b;
        Vector n1 = e.prev.normal();
        Vector n2 = e.normal();
        Vector bis = n1.add(n2).normalize();
        Point sp = new Point(v.x + bis.x*ev.time, v.y + bis.y*ev.time);
        // split f at sp: replace f by f1,f2
        WaveEdge f1 = new WaveEdge(f.a, sp);
        WaveEdge f2 = new WaveEdge(sp, f.b);
        // relink new edges
        f1.prev = f.prev;
        if (f.prev!=null) f.prev.next = f1;
        f1.next = f2;
        f2.prev = f1;
        f2.next = f.next;
        if (f.next!=null) f.next.prev = f2;
        // update list
        wave.remove(f);
        wave.add(f1); wave.add(f2);
        // schedule events on new edges
        scheduleEdgeEvent(f1, pq);
        scheduleSplitEvent(f1, wave, pq);
        scheduleEdgeEvent(f2, pq);
        scheduleSplitEvent(f2, wave, pq);
    }

    /** Extract final contours: intersect offset lines at distance r. */
    private static List<List<Point>> extractContours(List<WaveEdge> wave, double r) {
        List<List<Point>> contours = new ArrayList<>();
        Set<WaveEdge> seen = new HashSet<>();
        for (WaveEdge e0 : wave) {
            if (seen.contains(e0)) continue;
            List<Point> contour = new ArrayList<>();
            WaveEdge e = e0;
            do {
                seen.add(e);
                if (e.b.x == 706 && e.b.y == 628)
                	System.out.print(false);
                Line L1 = e.getOffsetLine(r);
                Line L2 = e.next.getOffsetLine(r);
                if (L1.a / L1.b == L2.a / L2.b) {
                	Point startPoint = e.a;
                	e = e.next;
                	e.a = startPoint;
                	continue;
                }
                	
                Point inter = intersection(L1, L2);
                contour.add(inter);
                e = e.next;
            } while (e!=null && e!=e0);
            if (!contour.isEmpty()) contours.add(contour);
        }
        boolean hasBad = contours.get(0).stream()
        	    .anyMatch(p -> !Double.isFinite(p.x) || !Double.isFinite(p.y));
        if (hasBad)
        	System.out.println();
        return contours;
    }

    // ================= Helpers =================
    static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
        Point subtract(Point p) { return new Point(x-p.x, y-p.y); }
        double distance(Point p) { return Math.hypot(x-p.x, y-p.y); }
        public String toString() { return String.format("(%.3f,%.3f)", x,y); }
    }

    static class Vector {
        double x, y;
        Vector(double x, double y){ this.x=x; this.y=y;}        
        Vector add(Vector v){ return new Vector(x+v.x, y+v.y);}    
        Vector normalize(){ double l=Math.hypot(x,y); return new Vector(x/l,y/l);}    
    }

    static class WaveEdge {
        Point a, b;
        WaveEdge prev, next;
        WaveEdge(Point a, Point b){ this.a=a; this.b=b; }
        double length(){ return a.distance(b); }
        /** inward normal (unit) for CCW polygon */
        Vector normal(){
        	double dx=b.x-a.x, dy=b.y-a.y;
        	double L=Math.hypot(dx,dy);
        	return new Vector(-dy/L, dx/L);
        }
        /** supporting line ax+by+c=0 */
        Line supportingLine(){
        	double A=b.y-a.y, B=a.x-b.x, C=-(A*a.x+B*a.y);
        	return new Line(A,B,C);
        }
        /** offset line at distance d inward */
        Line getOffsetLine(double d){
        	Line L=supportingLine();
        	double normLen=Math.hypot(L.a,L.b);
        	return new Line(L.a, L.b, L.c - d*normLen);
        }
        /** check if projection of p lies within segment a-b */
        boolean containsProjection(Point p){
        	double minx=Math.min(a.x,b.x)-EPS, maxx=Math.max(a.x,b.x)+EPS;
            double miny=Math.min(a.y,b.y)-EPS, maxy=Math.max(a.y,b.y)+EPS;
            return p.x>=minx&&p.x<=maxx&&p.y>=miny&&p.y<=maxy;
        }
    }

    static class Line { double a,b,c; Line(double a,double b,double c){this.a=a;this.b=b;this.c=c;} }

    static Point intersection(Line L, Line M){
    	double det=L.a*M.b-M.a*L.b;
        return new Point(Math.round((M.b*(-L.c)-L.b*(-M.c))/det),Math.round((L.a*(-M.c)-M.a*(-L.c))/det));
    }

    static double cross(Point u, Point v){ return u.x*v.y-u.y*v.x; }

    static class Event implements Comparable<Event> {
        enum Type{EDGE,SPLIT}
        double time; WaveEdge edge; Type type; WaveEdge target;
        Event(double t, WaveEdge e, Type ty){ time=t; edge=e; type=ty; }
        Event(double t, WaveEdge e, Type ty, WaveEdge trg){ this(t,e,ty); target=trg; }
        public int compareTo(Event o){ return Double.compare(time,o.time); }
    }
}
