// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.PlayHeadDragMode;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.tools.AudioPlayer;

/**
 * Singleton marker class to track position of audio.
 *
 * @author David Earl &lt;david@frankieandshadow.com&gt;
 * @since 572
 */
public final class PlayHeadMarker extends Marker {

    private Timer timer = null;
    private double animationInterval = 0.0; // seconds
    private static volatile PlayHeadMarker playHead = null;
    private MapMode oldMode = null;
    private LatLon oldCoor;
    private boolean enabled;
    private boolean wasPlaying = false;
    private int dropTolerance; /* pixels */
    private boolean jumpToMarker = false;

    /**
     * Returns the unique instance of {@code PlayHeadMarker}.
     * @return The unique instance of {@code PlayHeadMarker}.
     */
    public static PlayHeadMarker create() {
        if (playHead == null) {
            try {
                playHead = new PlayHeadMarker();
            } catch (Exception ex) {
                return null;
            }
        }
        return playHead;
    }

    private PlayHeadMarker() {
        super(new LatLon(0.0,0.0), "",
                Main.pref.get("marker.audiotracericon", "audio-tracer"),
                null, -1.0, 0.0);
        enabled = Main.pref.getBoolean("marker.traceaudio", true);
        if (! enabled) return;
        dropTolerance = Main.pref.getInteger("marker.playHeadDropTolerance", 50);
        Main.map.mapView.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent ev) {
                Point p = ev.getPoint();
                if (ev.getButton() != MouseEvent.BUTTON1 || p == null)
                    return;
                if (playHead.containsPoint(p)) {
                    /* when we get a click on the marker, we need to switch mode to avoid
                     * getting confused with other drag operations (like select) */
                    oldMode = Main.map.mapMode;
                    oldCoor = getCoor();
                    PlayHeadDragMode playHeadDragMode = new PlayHeadDragMode(playHead);
                    Main.map.selectMapMode(playHeadDragMode);
                    playHeadDragMode.mousePressed(ev);
                }
            }
        });
    }

    @Override public boolean containsPoint(Point p) {
        Point screen = Main.map.mapView.getPoint(getEastNorth());
        Rectangle r = new Rectangle(screen.x, screen.y, symbol.getIconWidth(),
                symbol.getIconHeight());
        return r.contains(p);
    }

    /**
     * called back from drag mode to say when we started dragging for real
     * (at least a short distance)
     */
    public void startDrag() {
        if (timer != null) {
            timer.stop();
        }
        wasPlaying = AudioPlayer.playing();
        if (wasPlaying) {
            try { AudioPlayer.pause();
            } catch (Exception ex) { AudioPlayer.audioMalfunction(ex);}
        }
    }

    /**
     * reinstate the old map mode after switching temporarily to do a play head drag
     */
    private void endDrag(boolean reset) {
        if (! wasPlaying || reset) {
            try { AudioPlayer.pause();
            } catch (Exception ex) { AudioPlayer.audioMalfunction(ex);}
        }
        if (reset) {
            setCoor(oldCoor);
        }
        Main.map.selectMapMode(oldMode);
        Main.map.mapView.repaint();
        timer.start();
    }

    /**
     * apply the new position resulting from a drag in progress
     * @param en the new position in map terms
     */
    public void drag(EastNorth en) {
        setEastNorth(en);
        Main.map.mapView.repaint();
    }

    /**
     * reposition the play head at the point on the track nearest position given,
     * providing we are within reasonable distance from the track; otherwise reset to the
     * original position.
     * @param en the position to start looking from
     */
    public void reposition(EastNorth en) {
        WayPoint cw = null;
        AudioMarker recent = AudioMarker.recentlyPlayedMarker();
        if (recent != null && recent.parentLayer != null && recent.parentLayer.fromLayer != null) {
            /* work out EastNorth equivalent of 50 (default) pixels tolerance */
            Point p = Main.map.mapView.getPoint(en);
            EastNorth enPlus25px = Main.map.mapView.getEastNorth(p.x+dropTolerance, p.y);
            cw = recent.parentLayer.fromLayer.data.nearestPointOnTrack(en, enPlus25px.east() - en.east());
        }

        AudioMarker ca = null;
        /* Find the prior audio marker (there should always be one in the
         * layer, even if it is only one at the start of the track) to
         * offset the audio from */
        if (cw != null && recent != null && recent.parentLayer != null) {
            for (Marker m : recent.parentLayer.data) {
                if (m instanceof AudioMarker) {
                    AudioMarker a = (AudioMarker) m;
                    if (a.time > cw.time) {
                        break;
                    }
                    ca = a;
                }
            }
        }

        if (ca == null) {
            /* Not close enough to track, or no audio marker found for some other reason */
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("You need to drag the play head near to the GPX track whose associated sound track you were playing (after the first marker)."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
                    );
            endDrag(true);
        } else {
            setCoor(cw.getCoor());
            ca.play(cw.time - ca.time);
            endDrag(false);
        }
    }

    /**
     * Synchronize the audio at the position where the play head was paused before
     * dragging with the position on the track where it was dropped.
     * If this is quite near an audio marker, we use that
     * marker as the sync. location, otherwise we create a new marker at the
     * trackpoint nearest the end point of the drag point to apply the
     * sync to.
     * @param en : the EastNorth end point of the drag
     */
    public void synchronize(EastNorth en) {
        AudioMarker recent = AudioMarker.recentlyPlayedMarker();
        if(recent == null)
            return;
        /* First, see if we dropped onto an existing audio marker in the layer being played */
        Point startPoint = Main.map.mapView.getPoint(en);
        AudioMarker ca = null;
        if (recent.parentLayer != null) {
            double closestAudioMarkerDistanceSquared = 1.0E100;
            for (Marker m : recent.parentLayer.data) {
                if (m instanceof AudioMarker) {
                    double distanceSquared = m.getEastNorth().distanceSq(en);
                    if (distanceSquared < closestAudioMarkerDistanceSquared) {
                        ca = (AudioMarker) m;
                        closestAudioMarkerDistanceSquared = distanceSquared;
                    }
                }
            }
        }

        /* We found the closest marker: did we actually hit it? */
        if (ca != null && ! ca.containsPoint(startPoint)) {
            ca = null;
        }

        /* If we didn't hit an audio marker, we need to create one at the nearest point on the track */
        if (ca == null) {
            /* work out EastNorth equivalent of 50 (default) pixels tolerance */
            Point p = Main.map.mapView.getPoint(en);
            EastNorth enPlus25px = Main.map.mapView.getEastNorth(p.x+dropTolerance, p.y);
            WayPoint cw = recent.parentLayer.fromLayer.data.nearestPointOnTrack(en, enPlus25px.east() - en.east());
            if (cw == null) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("You need to SHIFT-drag the play head onto an audio marker or onto the track point where you want to synchronize."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                        );
                endDrag(true);
                return;
            }
            ca = recent.parentLayer.addAudioMarker(cw.time, cw.getCoor());
        }

        /* Actually do the synchronization */
        if(ca == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Unable to create new audio marker."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
                    );
            endDrag(true);
        } else if (recent.parentLayer.synchronizeAudioMarkers(ca)) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Audio synchronized at point {0}.", recent.parentLayer.syncAudioMarker.getText()),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
                    );
            setCoor(recent.parentLayer.syncAudioMarker.getCoor());
            endDrag(false);
        } else {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Unable to synchronize in layer being played."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
                    );
            endDrag(true);
        }
    }

    /**
     * Paint the marker icon in the given graphics context.
     * @param g The graphics context
     * @param mv The map
     */
    public void paint(Graphics g, MapView mv) {
        if (time < 0.0) return;
        Point screen = mv.getPoint(getEastNorth());
        paintIcon(mv, g, screen.x, screen.y);
    }

    /**
     * Animates the marker along the track.
     */
    public void animate() {
        if (! enabled) return;
        jumpToMarker = true;
        if (timer == null) {
            animationInterval = Main.pref.getDouble("marker.audioanimationinterval", 1.0); //milliseconds
            timer = new Timer((int)(animationInterval * 1000.0), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    timerAction();
                }
            });
            timer.setInitialDelay(0);
        } else {
            timer.stop();
        }
        timer.start();
    }

    /**
     * callback for moving play head marker according to audio player position
     */
    public void timerAction() {
        AudioMarker recentlyPlayedMarker = AudioMarker.recentlyPlayedMarker();
        if (recentlyPlayedMarker == null)
            return;
        double audioTime = recentlyPlayedMarker.time +
                AudioPlayer.position() -
                recentlyPlayedMarker.offset -
                recentlyPlayedMarker.syncOffset;
        if (Math.abs(audioTime - time) < animationInterval)
            return;
        if (recentlyPlayedMarker.parentLayer == null) return;
        GpxLayer trackLayer = recentlyPlayedMarker.parentLayer.fromLayer;
        if (trackLayer == null)
            return;
        /* find the pair of track points for this position (adjusted by the syncOffset)
         * and interpolate between them
         */
        WayPoint w1 = null;
        WayPoint w2 = null;

        for (GpxTrack track : trackLayer.data.tracks) {
            for (GpxTrackSegment trackseg : track.getSegments()) {
                for (WayPoint w: trackseg.getWayPoints()) {
                    if (audioTime < w.time) {
                        w2 = w;
                        break;
                    }
                    w1 = w;
                }
                if (w2 != null) {
                    break;
                }
            }
            if (w2 != null) {
                break;
            }
        }

        if (w1 == null)
            return;
        setEastNorth(w2 == null ?
                w1.getEastNorth() :
                    w1.getEastNorth().interpolate(w2.getEastNorth(),
                            (audioTime - w1.time)/(w2.time - w1.time)));
        time = audioTime;
        if (jumpToMarker) {
            jumpToMarker = false;
            Main.map.mapView.zoomTo(w1.getEastNorth());
        }
        Main.map.mapView.repaint();
    }
}
