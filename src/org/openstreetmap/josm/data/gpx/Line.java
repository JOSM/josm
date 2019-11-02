// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.Color;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Line represents a linear collection of GPX waypoints with the ordered/unordered distinction.
 * @since 14451
 */
public class Line implements Collection<WayPoint> {
    private final Collection<WayPoint> waypoints;
    private final boolean unordered;
    private final Color color;

    /**
     * Constructs a new {@code Line}.
     * @param waypoints collection of waypoints
     * @param attributes track/route attributes
     * @param color color of the track
     * @since 15496
     */
    public Line(Collection<WayPoint> waypoints, Map<String, Object> attributes, Color color) {
        this.color = color;
        this.waypoints = Objects.requireNonNull(waypoints);
        unordered = attributes.isEmpty() && waypoints.stream().allMatch(x -> x.get(GpxConstants.PT_TIME) == null);
    }

    /**
     * Constructs a new {@code Line}.
     * @param trackSegment track segment
     * @param trackAttributes track attributes
     * @param color color of the track
     * @since 15496
     */
    public Line(IGpxTrackSegment trackSegment, Map<String, Object> trackAttributes, Color color) {
        this(trackSegment.getWayPoints(), trackAttributes, color);
    }

    /**
     * Constructs a new {@code Line}.
     * @param route route
     */
    public Line(GpxRoute route) {
        this(route.routePoints, route.attr, null);
    }

    /**
     * Determines if waypoints are ordered.
     * @return {@code true} if waypoints are ordered
     */
    public boolean isUnordered() {
        return unordered;
    }

    /**
     * Returns the track/route color
     * @return the color
     * @since 15496
     */
    public Color getColor() {
        return color;
    }

    @Override
    public int size() {
        return waypoints.size();
    }

    @Override
    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return waypoints.contains(o);
    }

    @Override
    public Iterator<WayPoint> iterator() {
        return waypoints.iterator();
    }

    @Override
    public Object[] toArray() {
        return waypoints.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return waypoints.toArray(a);
    }

    @Override
    public boolean add(WayPoint e) {
        return waypoints.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return waypoints.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return waypoints.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends WayPoint> c) {
        return waypoints.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return waypoints.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return waypoints.retainAll(c);
    }

    @Override
    public void clear() {
        waypoints.clear();
    }
}
