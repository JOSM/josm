// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.AudioUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * Import audio files into a GPX layer to enable audio playback functions.
 * @since 5715
 */
public class ImportAudioAction extends AbstractAction {
    private final GpxLayer layer;

    private static class Markers {
        public boolean timedMarkersOmitted = false;
        public boolean untimedMarkersOmitted = false;
    }

    /**
     * Constructs a new {@code ImportAudioAction}.
     * @param layer The associated GPX layer
     */
    public ImportAudioAction(final GpxLayer layer) {
        super(tr("Import Audio"), ImageProvider.get("importaudio"));
        this.layer = layer;
        putValue("help", ht("/Action/ImportAudio"));
    }

    private void warnCantImportIntoServerLayer(GpxLayer layer) {
        String msg = tr("<html>The data in the GPX layer ''{0}'' has been downloaded from the server.<br>" + "Because its way points do not include a timestamp we cannot correlate them with audio data.</html>", layer.getName());
        HelpAwareOptionPane.showOptionDialog(Main.parent, msg, tr("Import not possible"), JOptionPane.WARNING_MESSAGE, ht("/Action/ImportAudio#CantImportIntoGpxLayerFromServer"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (layer.data.fromServer) {
            warnCantImportIntoServerLayer(layer);
            return;
        }
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".wav");
            }

            @Override
            public String getDescription() {
                return tr("Wave Audio files (*.wav)");
            }
        };
        JFileChooser fc = DiskAccessAction.createAndOpenFileChooser(true, true, null, filter, JFileChooser.FILES_ONLY, "markers.lastaudiodirectory");
        if (fc != null) {
            File[] sel = fc.getSelectedFiles();
            // sort files in increasing order of timestamp (this is the end time, but so
            // long as they don't overlap, that's fine)
            if (sel.length > 1) {
                Arrays.sort(sel, new Comparator<File>() {
                    @Override
                    public int compare(File a, File b) {
                        return a.lastModified() <= b.lastModified() ? -1 : 1;
                    }
                });
            }
            String names = null;
            for (File file : sel) {
                if (names == null) {
                    names = " (";
                } else {
                    names += ", ";
                }
                names += file.getName();
            }
            if (names != null) {
                names += ")";
            } else {
                names = "";
            }
            MarkerLayer ml = new MarkerLayer(new GpxData(), tr("Audio markers from {0}", layer.getName()) + names, layer.getAssociatedFile(), layer);
            double firstStartTime = sel[0].lastModified() / 1000.0 - AudioUtil.getCalibratedDuration(sel[0]);
            Markers m = new Markers();
            for (File file : sel) {
                importAudio(file, ml, firstStartTime, m);
            }
            Main.main.addLayer(ml);
            Main.map.repaint();
        }
    }

    /**
     * Makes a new marker layer derived from this GpxLayer containing at least one audio marker
     * which the given audio file is associated with. Markers are derived from the following (a)
     * explict waypoints in the GPX layer, or (b) named trackpoints in the GPX layer, or (d)
     * timestamp on the wav file (e) (in future) voice recognised markers in the sound recording (f)
     * a single marker at the beginning of the track
     * @param wavFile : the file to be associated with the markers in the new marker layer
     * @param markers : keeps track of warning messages to avoid repeated warnings
     */
    private void importAudio(File wavFile, MarkerLayer ml, double firstStartTime, Markers markers) {
        URL url = Utils.fileToURL(wavFile);
        boolean hasTracks = layer.data.tracks != null && !layer.data.tracks.isEmpty();
        boolean hasWaypoints = layer.data.waypoints != null && !layer.data.waypoints.isEmpty();
        Collection<WayPoint> waypoints = new ArrayList<WayPoint>();
        boolean timedMarkersOmitted = false;
        boolean untimedMarkersOmitted = false;
        double snapDistance = Main.pref.getDouble("marker.audiofromuntimedwaypoints.distance", 1.0e-3); 
        // about 25 m
        WayPoint wayPointFromTimeStamp = null;

        // determine time of first point in track
        double firstTime = -1.0;
        if (hasTracks) {
            for (GpxTrack track : layer.data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        firstTime = w.time;
                        break;
                    }
                    if (firstTime >= 0.0) {
                        break;
                    }
                }
                if (firstTime >= 0.0) {
                    break;
                }
            }
        }
        if (firstTime < 0.0) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No GPX track available in layer to associate audio with."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
                    );
            return;
        }

        // (a) try explicit timestamped waypoints - unless suppressed
        if (Main.pref.getBoolean("marker.audiofromexplicitwaypoints", true) && hasWaypoints) {
            for (WayPoint w : layer.data.waypoints) {
                if (w.time > firstTime) {
                    waypoints.add(w);
                } else if (w.time > 0.0) {
                    timedMarkersOmitted = true;
                }
            }
        }

        // (b) try explicit waypoints without timestamps - unless suppressed
        if (Main.pref.getBoolean("marker.audiofromuntimedwaypoints", true) && hasWaypoints) {
            for (WayPoint w : layer.data.waypoints) {
                if (waypoints.contains(w)) {
                    continue;
                }
                WayPoint wNear = layer.data.nearestPointOnTrack(w.getEastNorth(), snapDistance);
                if (wNear != null) {
                    WayPoint wc = new WayPoint(w.getCoor());
                    wc.time = wNear.time;
                    if (w.attr.containsKey("name")) {
                        wc.attr.put("name", w.getString("name"));
                    }
                    waypoints.add(wc);
                } else {
                    untimedMarkersOmitted = true;
                }
            }
        }

        // (c) use explicitly named track points, again unless suppressed
        if ((Main.pref.getBoolean("marker.audiofromnamedtrackpoints", false)) && layer.data.tracks != null
                && !layer.data.tracks.isEmpty()) {
            for (GpxTrack track : layer.data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        if (w.attr.containsKey("name") || w.attr.containsKey("desc")) {
                            waypoints.add(w);
                        }
                    }
                }
            }
        }

        // (d) use timestamp of file as location on track
        if ((Main.pref.getBoolean("marker.audiofromwavtimestamps", false)) && hasTracks) {
            double lastModified = wavFile.lastModified() / 1000.0; // lastModified is in
            // milliseconds
            double duration = AudioUtil.getCalibratedDuration(wavFile);
            double startTime = lastModified - duration;
            startTime = firstStartTime + (startTime - firstStartTime)
                    / Main.pref.getDouble("audio.calibration", 1.0 /* default, ratio */);
            WayPoint w1 = null;
            WayPoint w2 = null;

            for (GpxTrack track : layer.data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        if (startTime < w.time) {
                            w2 = w;
                            break;
                        }
                        w1 = w;
                    }
                    if (w2 != null) {
                        break;
                    }
                }
            }

            if (w1 == null || w2 == null) {
                timedMarkersOmitted = true;
            } else {
                wayPointFromTimeStamp = new WayPoint(w1.getCoor().interpolate(w2.getCoor(),
                        (startTime - w1.time) / (w2.time - w1.time)));
                wayPointFromTimeStamp.time = startTime;
                String name = wavFile.getName();
                int dot = name.lastIndexOf('.');
                if (dot > 0) {
                    name = name.substring(0, dot);
                }
                wayPointFromTimeStamp.attr.put("name", name);
                waypoints.add(wayPointFromTimeStamp);
            }
        }

        // (e) analyse audio for spoken markers here, in due course

        // (f) simply add a single marker at the start of the track
        if ((Main.pref.getBoolean("marker.audiofromstart") || waypoints.isEmpty()) && hasTracks) {
            boolean gotOne = false;
            for (GpxTrack track : layer.data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        WayPoint wStart = new WayPoint(w.getCoor());
                        wStart.attr.put("name", "start");
                        wStart.time = w.time;
                        waypoints.add(wStart);
                        gotOne = true;
                        break;
                    }
                    if (gotOne) {
                        break;
                    }
                }
                if (gotOne) {
                    break;
                }
            }
        }

        /* we must have got at least one waypoint now */

        Collections.sort((ArrayList<WayPoint>) waypoints, new Comparator<WayPoint>() {
            @Override
            public int compare(WayPoint a, WayPoint b) {
                return a.time <= b.time ? -1 : 1;
            }
        });

        firstTime = -1.0; /* this time of the first waypoint, not first trackpoint */
        for (WayPoint w : waypoints) {
            if (firstTime < 0.0) {
                firstTime = w.time;
            }
            double offset = w.time - firstTime;
            AudioMarker am = new AudioMarker(w.getCoor(), w, url, ml, w.time, offset);
            /*
             * timeFromAudio intended for future use to shift markers of this type on
             * synchronization
             */
            if (w == wayPointFromTimeStamp) {
                am.timeFromAudio = true;
            }
            ml.data.add(am);
        }

        if (timedMarkersOmitted && !markers.timedMarkersOmitted) {
            JOptionPane
            .showMessageDialog(
                    Main.parent,
                    tr("Some waypoints with timestamps from before the start of the track or after the end were omitted or moved to the start."));
            markers.timedMarkersOmitted = timedMarkersOmitted;
        }
        if (untimedMarkersOmitted && !markers.untimedMarkersOmitted) {
            JOptionPane
            .showMessageDialog(
                    Main.parent,
                    tr("Some waypoints which were too far from the track to sensibly estimate their time were omitted."));
            markers.untimedMarkersOmitted = untimedMarkersOmitted;
        }
    }
}
