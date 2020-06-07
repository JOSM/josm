// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Data;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.gpx.IGpxTrack.GpxTrackChangeListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.ListeningCollection;

/**
 * Objects of this class represent a gpx file with tracks, waypoints and routes.
 * It uses GPX v1.1, see <a href="http://www.topografix.com/GPX/1/1/">the spec</a>
 * for details.
 *
 * @author Raphael Mack &lt;ramack@raphael-mack.de&gt;
 */
public class GpxData extends WithAttributes implements Data {

    /**
     * Constructs a new GpxData.
     */
    public GpxData() {}

    /**
     * Constructs a new GpxData that is currently being initialized, so no listeners will be fired until {@link #endUpdate()} is called.
     * @param initializing true
     * @since 15496
     */
    public GpxData(boolean initializing) {
        this.initializing = initializing;
    }

    /**
     * The disk file this layer is stored in, if it is a local layer. May be <code>null</code>.
     */
    public File storageFile;
    /**
     * A boolean flag indicating if the data was read from the OSM server.
     */
    public boolean fromServer;

    /**
     * Creator metadata for this file (usually software)
     */
    public String creator;

    /**
     * A list of tracks this file consists of
     */
    private final ArrayList<IGpxTrack> privateTracks = new ArrayList<>();
    /**
     * GPX routes in this file
     */
    private final ArrayList<GpxRoute> privateRoutes = new ArrayList<>();
    /**
     * Additional waypoints for this file.
     */
    private final ArrayList<WayPoint> privateWaypoints = new ArrayList<>();
    /**
     * All namespaces read from the original file
     */
    private final List<XMLNamespace> namespaces = new ArrayList<>();
    /**
     * The layer specific prefs formerly saved in the preferences, e.g. drawing options.
     * NOT the track specific settings (e.g. color, width)
     */
    private final Map<String, String> layerPrefs = new HashMap<>();

    private final GpxTrackChangeListener proxy = e -> invalidate();
    private boolean modified, updating, initializing;
    private boolean suppressedInvalidate;

    /**
     * Tracks. Access is discouraged, use {@link #getTracks()} to read.
     * @see #getTracks()
     */
    public final Collection<IGpxTrack> tracks = new ListeningCollection<IGpxTrack>(privateTracks, this::invalidate) {

        @Override
        protected void removed(IGpxTrack cursor) {
            cursor.removeListener(proxy);
            super.removed(cursor);
        }

        @Override
        protected void added(IGpxTrack cursor) {
            super.added(cursor);
            cursor.addListener(proxy);
        }
    };

    /**
     * Routes. Access is discouraged, use {@link #getTracks()} to read.
     * @see #getRoutes()
     */
    public final Collection<GpxRoute> routes = new ListeningCollection<>(privateRoutes, this::invalidate);

    /**
     * Waypoints. Access is discouraged, use {@link #getTracks()} to read.
     * @see #getWaypoints()
     */
    public final Collection<WayPoint> waypoints = new ListeningCollection<>(privateWaypoints, this::invalidate);

    /**
     * All data sources (bounds of downloaded bounds) of this GpxData.<br>
     * Not part of GPX standard but rather a JOSM extension, needed by the fact that
     * OSM API does not provide {@code <bounds>} element in its GPX reply.
     * @since 7575
     */
    public final Set<DataSource> dataSources = new HashSet<>();

    private final ListenerList<GpxDataChangeListener> listeners = ListenerList.create();

    private List<GpxTrackSegmentSpan> segSpans;

    /**
     * Merges data from another object.
     * @param other existing GPX data
     */
    public synchronized void mergeFrom(GpxData other) {
        mergeFrom(other, false, false);
    }

    /**
     * Merges data from another object.
     * @param other existing GPX data
     * @param cutOverlapping whether overlapping parts of the given track should be removed
     * @param connect whether the tracks should be connected on cuts
     * @since 14338
     */
    public synchronized void mergeFrom(GpxData other, boolean cutOverlapping, boolean connect) {
        if (storageFile == null && other.storageFile != null) {
            storageFile = other.storageFile;
        }
        fromServer = fromServer && other.fromServer;

        for (Map.Entry<String, Object> ent : other.attr.entrySet()) {
            // TODO: Detect conflicts.
            String k = ent.getKey();
            if (META_LINKS.equals(k) && attr.containsKey(META_LINKS)) {
                Collection<GpxLink> my = super.<GpxLink>getCollection(META_LINKS);
                @SuppressWarnings("unchecked")
                Collection<GpxLink> their = (Collection<GpxLink>) ent.getValue();
                my.addAll(their);
            } else {
                put(k, ent.getValue());
            }
        }

        if (cutOverlapping) {
            for (IGpxTrack trk : other.privateTracks) {
                cutOverlapping(trk, connect);
            }
        } else {
            other.privateTracks.forEach(this::addTrack);
        }
        other.privateRoutes.forEach(this::addRoute);
        other.privateWaypoints.forEach(this::addWaypoint);
        dataSources.addAll(other.dataSources);
        invalidate();
    }

    private void cutOverlapping(IGpxTrack trk, boolean connect) {
        List<IGpxTrackSegment> segsOld = new ArrayList<>(trk.getSegments());
        List<IGpxTrackSegment> segsNew = new ArrayList<>();
        for (IGpxTrackSegment seg : segsOld) {
            GpxTrackSegmentSpan s = GpxTrackSegmentSpan.tryGetFromSegment(seg);
            if (s != null && anySegmentOverlapsWith(s)) {
                List<WayPoint> wpsNew = new ArrayList<>();
                List<WayPoint> wpsOld = new ArrayList<>(seg.getWayPoints());
                if (s.isInverted()) {
                    Collections.reverse(wpsOld);
                }
                boolean split = false;
                WayPoint prevLastOwnWp = null;
                Date prevWpTime = null;
                for (WayPoint wp : wpsOld) {
                    Date wpTime = wp.getDate();
                    boolean overlap = false;
                    if (wpTime != null) {
                        for (GpxTrackSegmentSpan ownspan : getSegmentSpans()) {
                            if (wpTime.after(ownspan.firstTime) && wpTime.before(ownspan.lastTime)) {
                                overlap = true;
                                if (connect) {
                                    if (!split) {
                                        wpsNew.add(ownspan.getFirstWp());
                                    } else {
                                        connectTracks(prevLastOwnWp, ownspan, trk.getAttributes());
                                    }
                                    prevLastOwnWp = ownspan.getLastWp();
                                }
                                split = true;
                                break;
                            } else if (connect && prevWpTime != null
                                    && prevWpTime.before(ownspan.firstTime)
                                    && wpTime.after(ownspan.lastTime)) {
                                // the overlapping high priority track is shorter than the distance
                                // between two waypoints of the low priority track
                                if (split) {
                                    connectTracks(prevLastOwnWp, ownspan, trk.getAttributes());
                                    prevLastOwnWp = ownspan.getLastWp();
                                } else {
                                    wpsNew.add(ownspan.getFirstWp());
                                    // splitting needs to be handled here,
                                    // because other high priority tracks between the same waypoints could follow
                                    if (!wpsNew.isEmpty()) {
                                        segsNew.add(new GpxTrackSegment(wpsNew));
                                    }
                                    if (!segsNew.isEmpty()) {
                                        privateTracks.add(new GpxTrack(segsNew, trk.getAttributes()));
                                    }
                                    segsNew = new ArrayList<>();
                                    wpsNew = new ArrayList<>();
                                    wpsNew.add(ownspan.getLastWp());
                                    // therefore no break, because another segment could overlap, see above
                                }
                            }
                        }
                        prevWpTime = wpTime;
                    }
                    if (!overlap) {
                        if (split) {
                            //track has to be split, because we have an overlapping short track in the middle
                            if (!wpsNew.isEmpty()) {
                                segsNew.add(new GpxTrackSegment(wpsNew));
                            }
                            if (!segsNew.isEmpty()) {
                                privateTracks.add(new GpxTrack(segsNew, trk.getAttributes()));
                            }
                            segsNew = new ArrayList<>();
                            wpsNew = new ArrayList<>();
                            if (connect && prevLastOwnWp != null) {
                                wpsNew.add(new WayPoint(prevLastOwnWp));
                            }
                            prevLastOwnWp = null;
                            split = false;
                        }
                        wpsNew.add(new WayPoint(wp));
                    }
                }
                if (!wpsNew.isEmpty()) {
                    segsNew.add(new GpxTrackSegment(wpsNew));
                }
            } else {
                segsNew.add(seg);
            }
        }
        if (segsNew.equals(segsOld)) {
            privateTracks.add(trk);
        } else if (!segsNew.isEmpty()) {
            privateTracks.add(new GpxTrack(segsNew, trk.getAttributes()));
        }
    }

    private void connectTracks(WayPoint prevWp, GpxTrackSegmentSpan span, Map<String, Object> attr) {
        if (prevWp != null && !span.lastEquals(prevWp)) {
            privateTracks.add(new GpxTrack(Arrays.asList(Arrays.asList(new WayPoint(prevWp), span.getFirstWp())), attr));
        }
    }

    static class GpxTrackSegmentSpan {

        final Date firstTime;
        final Date lastTime;
        private final boolean inv;
        private final WayPoint firstWp;
        private final WayPoint lastWp;

        GpxTrackSegmentSpan(WayPoint a, WayPoint b) {
            Date at = a.getDate();
            Date bt = b.getDate();
            inv = bt.before(at);
            if (inv) {
                firstWp = b;
                firstTime = bt;
                lastWp = a;
                lastTime = at;
            } else {
                firstWp = a;
                firstTime = at;
                lastWp = b;
                lastTime = bt;
            }
        }

        WayPoint getFirstWp() {
            return new WayPoint(firstWp);
        }

        WayPoint getLastWp() {
            return new WayPoint(lastWp);
        }

        // no new instances needed, therefore own methods for that

        boolean firstEquals(Object other) {
            return firstWp.equals(other);
        }

        boolean lastEquals(Object other) {
            return lastWp.equals(other);
        }

        public boolean isInverted() {
            return inv;
        }

        boolean overlapsWith(GpxTrackSegmentSpan other) {
            return (firstTime.before(other.lastTime) && other.firstTime.before(lastTime))
                || (other.firstTime.before(lastTime) && firstTime.before(other.lastTime));
        }

        static GpxTrackSegmentSpan tryGetFromSegment(IGpxTrackSegment seg) {
            WayPoint b = getNextWpWithTime(seg, true);
            if (b != null) {
                WayPoint e = getNextWpWithTime(seg, false);
                if (e != null) {
                    return new GpxTrackSegmentSpan(b, e);
                }
            }
            return null;
        }

        private static WayPoint getNextWpWithTime(IGpxTrackSegment seg, boolean forward) {
            List<WayPoint> wps = new ArrayList<>(seg.getWayPoints());
            for (int i = forward ? 0 : wps.size() - 1; i >= 0 && i < wps.size(); i += forward ? 1 : -1) {
                if (wps.get(i).hasDate()) {
                    return wps.get(i);
                }
            }
            return null;
        }
    }

    /**
     * Get a list of SegmentSpans containing the beginning and end of each segment
     * @return the list of SegmentSpans
     * @since 14338
     */
    public synchronized List<GpxTrackSegmentSpan> getSegmentSpans() {
        if (segSpans == null) {
            segSpans = new ArrayList<>();
            for (IGpxTrack trk : privateTracks) {
                for (IGpxTrackSegment seg : trk.getSegments()) {
                    GpxTrackSegmentSpan s = GpxTrackSegmentSpan.tryGetFromSegment(seg);
                    if (s != null) {
                        segSpans.add(s);
                    }
                }
            }
            segSpans.sort((o1, o2) -> o1.firstTime.compareTo(o2.firstTime));
        }
        return segSpans;
    }

    private boolean anySegmentOverlapsWith(GpxTrackSegmentSpan other) {
        return getSegmentSpans().stream().anyMatch(s -> s.overlapsWith(other));
    }

    /**
     * Get all tracks contained in this data set.
     * @return The tracks.
     */
    public synchronized Collection<IGpxTrack> getTracks() {
        return Collections.unmodifiableCollection(privateTracks);
    }

    /**
     * Get stream of track segments.
     * @return {@code Stream<GPXTrack>}
     */
    public synchronized Stream<IGpxTrackSegment> getTrackSegmentsStream() {
        return getTracks().stream().flatMap(trk -> trk.getSegments().stream());
    }

    /**
     * Clear all tracks, empties the current privateTracks container,
     * helper method for some gpx manipulations.
     */
    private synchronized void clearTracks() {
        privateTracks.forEach(t -> t.removeListener(proxy));
        privateTracks.clear();
    }

    /**
     * Add a new track
     * @param track The new track
     * @since 12156
     */
    public synchronized void addTrack(IGpxTrack track) {
        if (privateTracks.stream().anyMatch(t -> t == track)) {
            throw new IllegalArgumentException(MessageFormat.format("The track was already added to this data: {0}", track));
        }
        privateTracks.add(track);
        track.addListener(proxy);
        invalidate();
    }

    /**
     * Remove a track
     * @param track The old track
     * @since 12156
     */
    public synchronized void removeTrack(IGpxTrack track) {
        if (!privateTracks.removeIf(t -> t == track)) {
            throw new IllegalArgumentException(MessageFormat.format("The track was not in this data: {0}", track));
        }
        track.removeListener(proxy);
        invalidate();
    }

    /**
     * Combine tracks into a single, segmented track.
     * The attributes of the first track are used, the rest discarded.
     *
     * @since 13210
     */
    public synchronized void combineTracksToSegmentedTrack() {
        List<IGpxTrackSegment> segs = getTrackSegmentsStream()
                .collect(Collectors.toCollection(ArrayList<IGpxTrackSegment>::new));
        Map<String, Object> attrs = new HashMap<>(privateTracks.get(0).getAttributes());

        // do not let the name grow if split / combine operations are called iteratively
        Object name = attrs.get("name");
        if (name != null) {
            attrs.put("name", name.toString().replaceFirst(" #\\d+$", ""));
        }

        clearTracks();
        addTrack(new GpxTrack(segs, attrs));
    }

    /**
     * Ensures a unique name among gpx layers
     * @param attrs attributes of/for an gpx track, written to if the name appeared previously in {@code counts}.
     * @param counts a {@code HashMap} of previously seen names, associated with their count.
     * @param srcLayerName Source layer name
     * @return the unique name for the gpx track.
     *
     * @since 15397
     */
    public static String ensureUniqueName(Map<String, Object> attrs, Map<String, Integer> counts, String srcLayerName) {
        String name = attrs.getOrDefault("name", srcLayerName).toString().replaceFirst(" #\\d+$", "");
        Integer count = counts.getOrDefault(name, 0) + 1;
        counts.put(name, count);

        attrs.put("name", MessageFormat.format("{0}{1}", name, " #" + count));
        return attrs.get("name").toString();
    }

    /**
     * Split tracks so that only single-segment tracks remain.
     * Each segment will make up one individual track after this operation.
     *
     * @param srcLayerName Source layer name
     *
     * @since 15397
     */
    public synchronized void splitTrackSegmentsToTracks(String srcLayerName) {
        final HashMap<String, Integer> counts = new HashMap<>();

        List<GpxTrack> trks = getTracks().stream()
            .flatMap(trk -> trk.getSegments().stream().map(seg -> {
                    HashMap<String, Object> attrs = new HashMap<>(trk.getAttributes());
                    ensureUniqueName(attrs, counts, srcLayerName);
                    return new GpxTrack(Arrays.asList(seg), attrs);
                }))
            .collect(Collectors.toCollection(ArrayList<GpxTrack>::new));

        clearTracks();
        trks.stream().forEachOrdered(this::addTrack);
    }

    /**
     * Split tracks into layers, the result is one layer for each track.
     * If this layer currently has only one GpxTrack this is a no-operation.
     *
     * The new GpxLayers are added to the LayerManager, the original GpxLayer
     * is untouched as to preserve potential route or wpt parts.
     *
     * @param srcLayerName Source layer name
     *
     * @since 15397
     */
    public synchronized void splitTracksToLayers(String srcLayerName) {
        final HashMap<String, Integer> counts = new HashMap<>();

        getTracks().stream()
            .filter(trk -> privateTracks.size() > 1)
            .map(trk -> {
                HashMap<String, Object> attrs = new HashMap<>(trk.getAttributes());
                GpxData d = new GpxData();
                d.addTrack(trk);
                return new GpxLayer(d, ensureUniqueName(attrs, counts, srcLayerName));
            })
            .forEachOrdered(layer -> MainApplication.getLayerManager().addLayer(layer));
    }

    /**
     * Replies the current number of tracks in this GpxData
     * @return track count
     * @since 13210
     */
    public synchronized int getTrackCount() {
        return privateTracks.size();
    }

    /**
     * Replies the accumulated total of all track segments,
     * the sum of segment counts for each track present.
     * @return track segments count
     * @since 13210
     */
    public synchronized int getTrackSegsCount() {
        return privateTracks.stream().mapToInt(t -> t.getSegments().size()).sum();
    }

    /**
     * Gets the list of all routes defined in this data set.
     * @return The routes
     * @since 12156
     */
    public synchronized Collection<GpxRoute> getRoutes() {
        return Collections.unmodifiableCollection(privateRoutes);
    }

    /**
     * Add a new route
     * @param route The new route
     * @since 12156
     */
    public synchronized void addRoute(GpxRoute route) {
        if (privateRoutes.stream().anyMatch(r -> r == route)) {
            throw new IllegalArgumentException(MessageFormat.format("The route was already added to this data: {0}", route));
        }
        privateRoutes.add(route);
        invalidate();
    }

    /**
     * Remove a route
     * @param route The old route
     * @since 12156
     */
    public synchronized void removeRoute(GpxRoute route) {
        if (!privateRoutes.removeIf(r -> r == route)) {
            throw new IllegalArgumentException(MessageFormat.format("The route was not in this data: {0}", route));
        }
        invalidate();
    }

    /**
     * Gets a list of all way points in this data set.
     * @return The way points.
     * @since 12156
     */
    public synchronized Collection<WayPoint> getWaypoints() {
        return Collections.unmodifiableCollection(privateWaypoints);
    }

    /**
     * Add a new waypoint
     * @param waypoint The new waypoint
     * @since 12156
     */
    public synchronized void addWaypoint(WayPoint waypoint) {
        if (privateWaypoints.stream().anyMatch(w -> w == waypoint)) {
            throw new IllegalArgumentException(MessageFormat.format("The route was already added to this data: {0}", waypoint));
        }
        privateWaypoints.add(waypoint);
        invalidate();
    }

    /**
     * Remove a waypoint
     * @param waypoint The old waypoint
     * @since 12156
     */
    public synchronized void removeWaypoint(WayPoint waypoint) {
        if (!privateWaypoints.removeIf(w -> w == waypoint)) {
            throw new IllegalArgumentException(MessageFormat.format("The route was not in this data: {0}", waypoint));
        }
        invalidate();
    }

    /**
     * Determines if this GPX data has one or more track points
     * @return {@code true} if this GPX data has track points, {@code false} otherwise
     */
    public synchronized boolean hasTrackPoints() {
        return getTrackPoints().findAny().isPresent();
    }

    /**
     * Gets a stream of all track points in the segments of the tracks of this data.
     * @return The stream
     * @see #getTracks()
     * @see IGpxTrack#getSegments()
     * @see IGpxTrackSegment#getWayPoints()
     * @since 12156
     */
    public synchronized Stream<WayPoint> getTrackPoints() {
        return getTracks().stream().flatMap(trk -> trk.getSegments().stream()).flatMap(trkseg -> trkseg.getWayPoints().stream());
    }

    /**
     * Determines if this GPX data has one or more route points
     * @return {@code true} if this GPX data has route points, {@code false} otherwise
     */
    public synchronized boolean hasRoutePoints() {
        return privateRoutes.stream().anyMatch(rte -> !rte.routePoints.isEmpty());
    }

    /**
     * Determines if this GPX data is empty (i.e. does not contain any point)
     * @return {@code true} if this GPX data is empty, {@code false} otherwise
     */
    public synchronized boolean isEmpty() {
        return !hasRoutePoints() && !hasTrackPoints() && waypoints.isEmpty();
    }

    /**
     * Returns the bounds defining the extend of this data, as read in metadata, if any.
     * If no bounds is defined in metadata, {@code null} is returned. There is no guarantee
     * that data entirely fit in this bounds, as it is not recalculated. To get recalculated bounds,
     * see {@link #recalculateBounds()}. To get downloaded areas, see {@link #dataSources}.
     * @return the bounds defining the extend of this data, or {@code null}.
     * @see #recalculateBounds()
     * @see #dataSources
     * @since 7575
     */
    public Bounds getMetaBounds() {
        Object value = get(META_BOUNDS);
        if (value instanceof Bounds) {
            return (Bounds) value;
        }
        return null;
    }

    /**
     * Calculates the bounding box of available data and returns it.
     * The bounds are not stored internally, but recalculated every time
     * this function is called.<br>
     * To get bounds as read from metadata, see {@link #getMetaBounds()}.<br>
     * To get downloaded areas, see {@link #dataSources}.<br>
     *
     * FIXME might perhaps use visitor pattern?
     * @return the bounds
     * @see #getMetaBounds()
     * @see #dataSources
     */
    public synchronized Bounds recalculateBounds() {
        Bounds bounds = null;
        for (WayPoint wpt : privateWaypoints) {
            if (bounds == null) {
                bounds = new Bounds(wpt.getCoor());
            } else {
                bounds.extend(wpt.getCoor());
            }
        }
        for (GpxRoute rte : privateRoutes) {
            for (WayPoint wpt : rte.routePoints) {
                if (bounds == null) {
                    bounds = new Bounds(wpt.getCoor());
                } else {
                    bounds.extend(wpt.getCoor());
                }
            }
        }
        for (IGpxTrack trk : privateTracks) {
            Bounds trkBounds = trk.getBounds();
            if (trkBounds != null) {
                if (bounds == null) {
                    bounds = new Bounds(trkBounds);
                } else {
                    bounds.extend(trkBounds);
                }
            }
        }
        return bounds;
    }

    /**
     * calculates the sum of the lengths of all track segments
     * @return the length in meters
     */
    public synchronized double length() {
        return privateTracks.stream().mapToDouble(IGpxTrack::length).sum();
    }

    /**
     * returns minimum and maximum timestamps in the track
     * @param trk track to analyze
     * @return  minimum and maximum dates in array of 2 elements
     */
    public static Date[] getMinMaxTimeForTrack(IGpxTrack trk) {
        final LongSummaryStatistics statistics = trk.getSegments().stream()
                .flatMap(seg -> seg.getWayPoints().stream())
                .mapToLong(WayPoint::getTimeInMillis)
                .summaryStatistics();
        return statistics.getCount() == 0
                ? null
                : new Date[]{new Date(statistics.getMin()), new Date(statistics.getMax())};
    }

    /**
    * Returns minimum and maximum timestamps for all tracks
    * Warning: there are lot of track with broken timestamps,
    * so we just ignore points from future and from year before 1970 in this method
    * @return minimum and maximum dates in array of 2 elements
    * @since 7319
    */
    public synchronized Date[] getMinMaxTimeForAllTracks() {
        long now = System.currentTimeMillis();
        final LongSummaryStatistics statistics = tracks.stream()
                .flatMap(trk -> trk.getSegments().stream())
                .flatMap(seg -> seg.getWayPoints().stream())
                .mapToLong(WayPoint::getTimeInMillis)
                .filter(t -> t > 0 && t <= now)
                .summaryStatistics();
        return statistics.getCount() == 0
                ? new Date[0]
                : new Date[]{new Date(statistics.getMin()), new Date(statistics.getMax())};
    }

    /**
     * Makes a WayPoint at the projection of point p onto the track providing p is less than
     * tolerance away from the track
     *
     * @param p : the point to determine the projection for
     * @param tolerance : must be no further than this from the track
     * @return the closest point on the track to p, which may be the first or last point if off the
     * end of a segment, or may be null if nothing close enough
     */
    public synchronized WayPoint nearestPointOnTrack(EastNorth p, double tolerance) {
        /*
         * assume the coordinates of P are xp,yp, and those of a section of track between two
         * trackpoints are R=xr,yr and S=xs,ys. Let N be the projected point.
         *
         * The equation of RS is Ax + By + C = 0 where A = ys - yr B = xr - xs C = - Axr - Byr
         *
         * Also, note that the distance RS^2 is A^2 + B^2
         *
         * If RS^2 == 0.0 ignore the degenerate section of track
         *
         * PN^2 = (Axp + Byp + C)^2 / RS^2 that is the distance from P to the line
         *
         * so if PN^2 is less than PNmin^2 (initialized to tolerance) we can reject the line
         * otherwise... determine if the projected poijnt lies within the bounds of the line: PR^2 -
         * PN^2 <= RS^2 and PS^2 - PN^2 <= RS^2
         *
         * where PR^2 = (xp - xr)^2 + (yp-yr)^2 and PS^2 = (xp - xs)^2 + (yp-ys)^2
         *
         * If so, calculate N as xn = xr + (RN/RS) B yn = y1 + (RN/RS) A
         *
         * where RN = sqrt(PR^2 - PN^2)
         */

        double pnminsq = tolerance * tolerance;
        EastNorth bestEN = null;
        double bestTime = Double.NaN;
        double px = p.east();
        double py = p.north();
        double rx = 0.0, ry = 0.0, sx, sy, x, y;
        for (IGpxTrack track : privateTracks) {
            for (IGpxTrackSegment seg : track.getSegments()) {
                WayPoint r = null;
                for (WayPoint wpSeg : seg.getWayPoints()) {
                    EastNorth en = wpSeg.getEastNorth(ProjectionRegistry.getProjection());
                    if (r == null) {
                        r = wpSeg;
                        rx = en.east();
                        ry = en.north();
                        x = px - rx;
                        y = py - ry;
                        double pRsq = x * x + y * y;
                        if (pRsq < pnminsq) {
                            pnminsq = pRsq;
                            bestEN = en;
                            if (r.hasDate()) {
                                bestTime = r.getTime();
                            }
                        }
                    } else {
                        sx = en.east();
                        sy = en.north();
                        double a = sy - ry;
                        double b = rx - sx;
                        double c = -a * rx - b * ry;
                        double rssq = a * a + b * b;
                        if (rssq == 0) {
                            continue;
                        }
                        double pnsq = a * px + b * py + c;
                        pnsq = pnsq * pnsq / rssq;
                        if (pnsq < pnminsq) {
                            x = px - rx;
                            y = py - ry;
                            double prsq = x * x + y * y;
                            x = px - sx;
                            y = py - sy;
                            double pssq = x * x + y * y;
                            if (prsq - pnsq <= rssq && pssq - pnsq <= rssq) {
                                double rnoverRS = Math.sqrt((prsq - pnsq) / rssq);
                                double nx = rx - rnoverRS * b;
                                double ny = ry + rnoverRS * a;
                                bestEN = new EastNorth(nx, ny);
                                if (r.hasDate() && wpSeg.hasDate()) {
                                    bestTime = r.getTime() + rnoverRS * (wpSeg.getTime() - r.getTime());
                                }
                                pnminsq = pnsq;
                            }
                        }
                        r = wpSeg;
                        rx = sx;
                        ry = sy;
                    }
                }
                if (r != null) {
                    EastNorth c = r.getEastNorth(ProjectionRegistry.getProjection());
                    /* if there is only one point in the seg, it will do this twice, but no matter */
                    rx = c.east();
                    ry = c.north();
                    x = px - rx;
                    y = py - ry;
                    double prsq = x * x + y * y;
                    if (prsq < pnminsq) {
                        pnminsq = prsq;
                        bestEN = c;
                        if (r.hasDate()) {
                            bestTime = r.getTime();
                        }
                    }
                }
            }
        }
        if (bestEN == null)
            return null;
        WayPoint best = new WayPoint(ProjectionRegistry.getProjection().eastNorth2latlon(bestEN));
        if (!Double.isNaN(bestTime)) {
            best.setTimeInMillis((long) (bestTime * 1000));
        }
        return best;
    }

    /**
     * Iterate over all track segments and over all routes.
     *
     * @param trackVisibility An array indicating which tracks should be
     * included in the iteration. Can be null, then all tracks are included.
     * @return an Iterable object, which iterates over all track segments and
     * over all routes
     */
    public Iterable<Line> getLinesIterable(final boolean... trackVisibility) {
        return () -> new LinesIterator(this, trackVisibility);
    }

    /**
     * Resets the internal caches of east/north coordinates.
     */
    public synchronized void resetEastNorthCache() {
        privateWaypoints.forEach(WayPoint::invalidateEastNorthCache);
        getTrackPoints().forEach(WayPoint::invalidateEastNorthCache);
        for (GpxRoute route: getRoutes()) {
            if (route.routePoints == null) {
                continue;
            }
            for (WayPoint wp: route.routePoints) {
                wp.invalidateEastNorthCache();
            }
        }
    }

    /**
     * Iterates over all track segments and then over all routes.
     */
    public static class LinesIterator implements Iterator<Line> {

        private Iterator<IGpxTrack> itTracks;
        private int idxTracks;
        private Iterator<IGpxTrackSegment> itTrackSegments;
        private final Iterator<GpxRoute> itRoutes;

        private Line next;
        private final boolean[] trackVisibility;
        private Map<String, Object> trackAttributes;
        private IGpxTrack curTrack;

        /**
         * Constructs a new {@code LinesIterator}.
         * @param data GPX data
         * @param trackVisibility An array indicating which tracks should be
         * included in the iteration. Can be null, then all tracks are included.
         */
        public LinesIterator(GpxData data, boolean... trackVisibility) {
            itTracks = data.tracks.iterator();
            idxTracks = -1;
            itRoutes = data.routes.iterator();
            this.trackVisibility = trackVisibility;
            next = getNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Line next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Line current = next;
            next = getNext();
            return current;
        }

        private Line getNext() {
            if (itTracks != null) {
                if (itTrackSegments != null && itTrackSegments.hasNext()) {
                    return new Line(itTrackSegments.next(), trackAttributes, curTrack.getColor());
                } else {
                    while (itTracks.hasNext()) {
                        curTrack = itTracks.next();
                        trackAttributes = curTrack.getAttributes();
                        idxTracks++;
                        if (trackVisibility != null && !trackVisibility[idxTracks])
                            continue;
                        itTrackSegments = curTrack.getSegments().iterator();
                        if (itTrackSegments.hasNext()) {
                            return new Line(itTrackSegments.next(), trackAttributes, curTrack.getColor());
                        }
                    }
                    // if we get here, all the Tracks are finished; Continue with Routes
                    trackAttributes = null;
                    itTracks = null;
                }
            }
            if (itRoutes.hasNext()) {
                return new Line(itRoutes.next());
            }
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Collection<DataSource> getDataSources() {
        return Collections.unmodifiableCollection(dataSources);
    }

    /**
     * The layer specific prefs formerly saved in the preferences, e.g. drawing options.
     * NOT the track specific settings (e.g. color, width)
     * @return Modifiable map
     * @since 15496
     */
    public Map<String, String> getLayerPrefs() {
        return layerPrefs;
    }

    /**
     * All XML namespaces read from the original file
     * @return Modifiable list
     * @since 15496
     */
    public List<XMLNamespace> getNamespaces() {
        return namespaces;
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(
                super.hashCode(),
                namespaces,
                layerPrefs,
                dataSources,
                privateRoutes,
                privateTracks,
                privateWaypoints
        );
    }

    @Override
    public synchronized boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        GpxData other = (GpxData) obj;
        if (dataSources == null) {
            if (other.dataSources != null)
                return false;
        } else if (!dataSources.equals(other.dataSources))
            return false;
        if (layerPrefs == null) {
            if (other.layerPrefs != null)
                return false;
        } else if (!layerPrefs.equals(other.layerPrefs))
            return false;
        if (privateRoutes == null) {
            if (other.privateRoutes != null)
                return false;
        } else if (!privateRoutes.equals(other.privateRoutes))
            return false;
        if (privateTracks == null) {
            if (other.privateTracks != null)
                return false;
        } else if (!privateTracks.equals(other.privateTracks))
            return false;
        if (privateWaypoints == null) {
            if (other.privateWaypoints != null)
                return false;
        } else if (!privateWaypoints.equals(other.privateWaypoints))
            return false;
        if (namespaces == null) {
            if (other.namespaces != null)
                return false;
        } else if (!namespaces.equals(other.namespaces))
            return false;
        return true;
    }

    @Override
    public void put(String key, Object value) {
        super.put(key, value);
        invalidate();
    }

    /**
     * Adds a listener that gets called whenever the data changed.
     * @param listener The listener
     * @since 12156
     */
    public void addChangeListener(GpxDataChangeListener listener) {
        listeners.addListener(listener);
    }

    /**
     * Adds a listener that gets called whenever the data changed. It is added with a weak link
     * @param listener The listener
     */
    public void addWeakChangeListener(GpxDataChangeListener listener) {
        listeners.addWeakListener(listener);
    }

    /**
     * Removes a listener that gets called whenever the data changed.
     * @param listener The listener
     * @since 12156
     */
    public void removeChangeListener(GpxDataChangeListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Fires event listeners and sets the modified flag to true.
     */
    public void invalidate() {
        fireInvalidate(true);
    }

    private void fireInvalidate(boolean setModified) {
        if (updating || initializing) {
            suppressedInvalidate = true;
        } else {
            if (setModified) {
                modified = true;
            }
            if (listeners.hasListeners()) {
                GpxDataChangeEvent e = new GpxDataChangeEvent(this);
                listeners.fireEvent(l -> l.gpxDataChanged(e));
            }
        }
    }

    /**
     * Begins updating this GpxData and prevents listeners from being fired.
     * @since 15496
     */
    public void beginUpdate() {
        updating = true;
    }

    /**
     * Finishes updating this GpxData and fires listeners if required.
     * @since 15496
     */
    public void endUpdate() {
        boolean setModified = updating;
        updating = initializing = false;
        if (suppressedInvalidate) {
            fireInvalidate(setModified);
            suppressedInvalidate = false;
        }
    }

    /**
     * A listener that listens to GPX data changes.
     * @author Michael Zangl
     * @since 12156
     */
    @FunctionalInterface
    public interface GpxDataChangeListener {
        /**
         * Called when the gpx data changed.
         * @param e The event
         */
        void gpxDataChanged(GpxDataChangeEvent e);
    }

    /**
     * A data change event in any of the gpx data.
     * @author Michael Zangl
     * @since 12156
     */
    public static class GpxDataChangeEvent {
        private final GpxData source;

        GpxDataChangeEvent(GpxData source) {
            super();
            this.source = source;
        }

        /**
         * Get the data that was changed.
         * @return The data.
         */
        public GpxData getSource() {
            return source;
        }
    }

    /**
     * Determines whether anything has been modified.
     * @return whether anything has been modified (e.g. colors)
     * @since 15496
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Sets the modified flag to the value.
     * @param value modified flag
     * @since 15496
     */
    public void setModified(boolean value) {
        modified = value;
    }

    /**
     * A class containing prefix, URI and location of a namespace
     * @since 15496
     */
    public static class XMLNamespace {
        private final String uri, prefix;
        private String location;

        /**
         * Creates a schema with prefix and URI, tries to determine prefix from URI
         * @param fallbackPrefix the namespace prefix, if not determined from URI
         * @param uri the namespace URI
         */
        public XMLNamespace(String fallbackPrefix, String uri) {
            this.prefix = Optional.ofNullable(GpxExtension.findPrefix(uri)).orElse(fallbackPrefix);
            this.uri = uri;
        }

        /**
         * Creates a schema with prefix, URI and location.
         * Does NOT try to determine prefix from URI!
         * @param prefix XML namespace prefix
         * @param uri XML namespace URI
         * @param location XML namespace location
         */
        public XMLNamespace(String prefix, String uri, String location) {
            this.prefix = prefix;
            this.uri = uri;
            this.location = location;
        }

        /**
         * Returns the URI of the namespace.
         * @return the URI of the namespace
         */
        public String getURI() {
            return uri;
        }

        /**
         * Returns the prefix of the namespace.
         * @return the prefix of the namespace, determined from URI if possible
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Returns the location of the schema.
         * @return the location of the schema
         */
        public String getLocation() {
            return location;
        }

        /**
         * Sets the location of the schema
         * @param location the location of the schema
         */
        public void setLocation(String location) {
            this.location = location;
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, uri, location);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            XMLNamespace other = (XMLNamespace) obj;
            if (prefix == null) {
                if (other.prefix != null)
                    return false;
            } else if (!prefix.equals(other.prefix))
                return false;
            if (uri == null) {
                if (other.uri != null)
                    return false;
            } else if (!uri.equals(other.uri))
                return false;
            if (location == null) {
                if (other.location != null)
                    return false;
            } else if (!location.equals(other.location))
                return false;
            return true;
        }
    }
}
