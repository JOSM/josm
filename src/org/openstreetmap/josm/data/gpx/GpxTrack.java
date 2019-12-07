// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;

/**
 * GPX track.
 * Note that the color attributes are not immutable and may be modified by the user.
 * @since 15496
 */
public class GpxTrack extends WithAttributes implements IGpxTrack {

    private final List<IGpxTrackSegment> segments;
    private final double length;
    private final Bounds bounds;
    private Color colorCache;
    private final ListenerList<IGpxTrack.GpxTrackChangeListener> listeners = ListenerList.create();
    private static final HashMap<Color, String> closestGarminColorCache = new HashMap<>();
    private ColorFormat colorFormat;

    /**
     * Constructs a new {@code GpxTrack}.
     * @param trackSegs track segments
     * @param attributes track attributes
     */
    public GpxTrack(Collection<Collection<WayPoint>> trackSegs, Map<String, Object> attributes) {
        List<IGpxTrackSegment> newSegments = new ArrayList<>();
        for (Collection<WayPoint> trackSeg: trackSegs) {
            if (trackSeg != null && !trackSeg.isEmpty()) {
                newSegments.add(new GpxTrackSegment(trackSeg));
            }
        }
        this.segments = Collections.unmodifiableList(newSegments);
        this.length = calculateLength();
        this.bounds = calculateBounds();
        this.attr = new HashMap<>(attributes);
    }

    /**
     * Constructs a new {@code GpxTrack} from {@code GpxTrackSegment} objects.
     * @param trackSegs The segments to build the track from.  Input is not deep-copied,
     *                 which means the caller may reuse the same segments to build
     *                 multiple GpxTrack instances from.  This should not be
     *                 a problem, since this object cannot modify {@code this.segments}.
     * @param attributes Attributes for the GpxTrack, the input map is copied.
     */
    public GpxTrack(List<IGpxTrackSegment> trackSegs, Map<String, Object> attributes) {
        this.attr = new HashMap<>(attributes);
        this.segments = Collections.unmodifiableList(trackSegs);
        this.length = calculateLength();
        this.bounds = calculateBounds();
    }

    private double calculateLength() {
        double result = 0.0; // in meters

        for (IGpxTrackSegment trkseg : segments) {
            result += trkseg.length();
        }
        return result;
    }

    private Bounds calculateBounds() {
        Bounds result = null;
        for (IGpxTrackSegment segment: segments) {
            Bounds segBounds = segment.getBounds();
            if (segBounds != null) {
                if (result == null) {
                    result = new Bounds(segBounds);
                } else {
                    result.extend(segBounds);
                }
            }
        }
        return result;
    }

    @Override
    public void setColor(Color color) {
        setColorExtension(color);
        colorCache = color;
    }

    private void setColorExtension(Color color) {
        getExtensions().findAndRemove("gpxx", "DisplayColor");
        if (color == null) {
            getExtensions().findAndRemove("gpxd", "color");
        } else {
            getExtensions().addOrUpdate("gpxd", "color", String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
        }
        fireInvalidate();
    }

    @Override
    public Color getColor() {
        if (colorCache == null) {
            colorCache = getColorFromExtension();
        }
        return colorCache;
    }

    private Color getColorFromExtension() {
        GpxExtension gpxd = getExtensions().find("gpxd", "color");
        if (gpxd != null) {
            colorFormat = ColorFormat.GPXD;
            String cs = gpxd.getValue();
            try {
                return Color.decode(cs);
            } catch (NumberFormatException ex) {
                Logging.warn("Could not read gpxd color: " + cs);
            }
        } else {
            GpxExtension gpxx = getExtensions().find("gpxx", "DisplayColor");
            if (gpxx != null) {
                colorFormat = ColorFormat.GPXX;
                String cs = gpxx.getValue();
                if (cs != null) {
                    Color cc = GARMIN_COLORS.get(cs);
                    if (cc != null) {
                        return cc;
                    }
                }
                Logging.warn("Could not read garmin color: " + cs);
            }
        }
        return null;
    }

    /**
     * Converts the color to the given format, if present.
     * @param cFormat can be a {@link GpxConstants.ColorFormat}
     */
    public void convertColor(ColorFormat cFormat) {
        Color c = getColor();
        if (c == null) return;

        if (cFormat != this.colorFormat) {
            if (cFormat == null) {
                // just hide the extensions, don't actually remove them
                Optional.ofNullable(getExtensions().find("gpxx", "DisplayColor")).ifPresent(GpxExtension::hide);
                Optional.ofNullable(getExtensions().find("gpxd", "color")).ifPresent(GpxExtension::hide);
            } else if (cFormat == ColorFormat.GPXX) {
                getExtensions().findAndRemove("gpxd", "color");
                String colorString = null;
                if (closestGarminColorCache.containsKey(c)) {
                    colorString = closestGarminColorCache.get(c);
                } else {
                    //find closest garmin color
                    double closestDiff = -1;
                    for (Entry<String, Color> e : GARMIN_COLORS.entrySet()) {
                        double diff = colorDist(e.getValue(), c);
                        if (closestDiff < 0 || diff < closestDiff) {
                            colorString = e.getKey();
                            closestDiff = diff;
                            if (closestDiff == 0) break;
                        }
                    }
                }
                closestGarminColorCache.put(c, colorString);
                getExtensions().addIfNotPresent("gpxx", "TrackExtension").getExtensions().addOrUpdate("gpxx", "DisplayColor", colorString);
            } else if (cFormat == ColorFormat.GPXD) {
                setColor(c);
            }
            colorFormat = cFormat;
        }
    }

    private double colorDist(Color c1, Color c2) {
        // Simple Euclidean distance between two colors
        return Math.sqrt(Math.pow(c1.getRed() - c2.getRed(), 2)
                + Math.pow(c1.getGreen() - c2.getGreen(), 2)
                + Math.pow(c1.getBlue() - c2.getBlue(), 2));
    }

    @Override
    public void put(String key, Object value) {
        super.put(key, value);
        fireInvalidate();
    }

    private void fireInvalidate() {
        listeners.fireEvent(l -> l.gpxDataChanged(new IGpxTrack.GpxTrackChangeEvent(this)));
    }

    @Override
    public Bounds getBounds() {
        return bounds == null ? null : new Bounds(bounds);
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public Collection<IGpxTrackSegment> getSegments() {
        return segments;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + ((segments == null) ? 0 : segments.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        GpxTrack other = (GpxTrack) obj;
        if (segments == null) {
            if (other.segments != null)
                return false;
        } else if (!segments.equals(other.segments))
            return false;
        return true;
    }

    @Override
    public void addListener(IGpxTrack.GpxTrackChangeListener l) {
        listeners.addListener(l);
    }

    @Override
    public void removeListener(IGpxTrack.GpxTrackChangeListener l) {
        listeners.removeListener(l);
    }

    /**
     * Resets the color cache
     */
    public void invalidate() {
        colorCache = null;
    }

    /**
     * A listener that listens to GPX track changes.
     * @deprecated use {@link IGpxTrack.GpxTrackChangeListener} instead
     */
    @Deprecated
    @FunctionalInterface
    interface GpxTrackChangeListener {
        void gpxDataChanged(GpxTrackChangeEvent e);
    }

    /**
     * A track change event for the current track.
     * @deprecated use {@link IGpxTrack.GpxTrackChangeEvent} instead
     */
    @Deprecated
    static class GpxTrackChangeEvent extends IGpxTrack.GpxTrackChangeEvent {
        GpxTrackChangeEvent(IGpxTrack source) {
            super(source);
        }
    }

}
