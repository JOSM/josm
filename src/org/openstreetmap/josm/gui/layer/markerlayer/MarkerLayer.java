// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.Extensions;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.CustomizeColor;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToMarkerLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToNextMarker;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToPreviousMarker;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer holding markers.
 *
 * Markers are GPS points with a name and, optionally, a symbol code attached;
 * marker layers can be created from waypoints when importing raw GPS data,
 * but they may also come from other sources.
 *
 * The symbol code is for future use.
 *
 * The data is read only.
 */
public class MarkerLayer extends Layer implements JumpToMarkerLayer {

    /**
     * A list of markers.
     */
    public final List<Marker> data;
    private boolean mousePressed = false;
    public GpxLayer fromLayer = null;
    private Marker currentMarker;

    public MarkerLayer(GpxData indata, String name, File associatedFile, GpxLayer fromLayer) {
        super(name);
        this.setAssociatedFile(associatedFile);
        this.data = new ArrayList<Marker>();
        this.fromLayer = fromLayer;
        double firstTime = -1.0;
        String lastLinkedFile = "";

        for (WayPoint wpt : indata.waypoints) {
            /* calculate time differences in waypoints */
            double time = wpt.time;
            boolean wpt_has_link = wpt.attr.containsKey(GpxConstants.META_LINKS);
            if (firstTime < 0 && wpt_has_link) {
                firstTime = time;
                for (Object oneLink : wpt.getCollection(GpxConstants.META_LINKS)) {
                    if (oneLink instanceof GpxLink) {
                        lastLinkedFile = ((GpxLink)oneLink).uri;
                        break;
                    }
                }
            }
            if (wpt_has_link) {
                for (Object oneLink : wpt.getCollection(GpxConstants.META_LINKS)) {
                    if (oneLink instanceof GpxLink) {
                        String uri = ((GpxLink)oneLink).uri;
                        if (!uri.equals(lastLinkedFile)) {
                            firstTime = time;
                        }
                        lastLinkedFile = uri;
                        break;
                    }
                }
            }
            Double offset = null;
            // If we have an explicit offset, take it.
            // Otherwise, for a group of markers with the same Link-URI (e.g. an
            // audio file) calculate the offset relative to the first marker of
            // that group. This way the user can jump to the corresponding
            // playback positions in a long audio track.
            Extensions exts = (Extensions) wpt.get(GpxConstants.META_EXTENSIONS);
            if (exts != null && exts.containsKey("offset")) {
                try {
                    offset = Double.parseDouble(exts.get("offset"));
                } catch (NumberFormatException nfe) {
                    Main.warn(nfe);
                }
            }
            if (offset == null) {
                offset = time - firstTime;
            }
            Marker m = Marker.createMarker(wpt, indata.storageFile, this, time, offset);
            if (m != null) {
                data.add(m);
            }
        }
    }

    @Override
    public void hookUpMapView() {
        Main.map.mapView.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1)
                    return;
                boolean mousePressedInButton = false;
                if (e.getPoint() != null) {
                    for (Marker mkr : data) {
                        if (mkr.containsPoint(e.getPoint())) {
                            mousePressedInButton = true;
                            break;
                        }
                    }
                }
                if (! mousePressedInButton)
                    return;
                mousePressed  = true;
                if (isVisible()) {
                    Main.map.mapView.repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent ev) {
                if (ev.getButton() != MouseEvent.BUTTON1 || ! mousePressed)
                    return;
                mousePressed = false;
                if (!isVisible())
                    return;
                if (ev.getPoint() != null) {
                    for (Marker mkr : data) {
                        if (mkr.containsPoint(ev.getPoint())) {
                            mkr.actionPerformed(new ActionEvent(this, 0, null));
                        }
                    }
                }
                Main.map.mapView.repaint();
            }
        });
    }

    /**
     * Return a static icon.
     */
    @Override public Icon getIcon() {
        return ImageProvider.get("layer", "marker_small");
    }

    @Override
    public Color getColor(boolean ignoreCustom)
    {
        String name = getName();
        return Main.pref.getColor(marktr("gps marker"), name != null ? "layer "+name : null, Color.gray);
    }

    /* for preferences */
    static public Color getGenericColor()
    {
        return Main.pref.getColor(marktr("gps marker"), Color.gray);
    }

    @Override public void paint(Graphics2D g, MapView mv, Bounds box) {
        boolean showTextOrIcon = isTextOrIconShown();
        g.setColor(getColor(true));

        if (mousePressed) {
            boolean mousePressedTmp = mousePressed;
            Point mousePos = mv.getMousePosition(); // Get mouse position only when necessary (it's the slowest part of marker layer painting)
            for (Marker mkr : data) {
                if (mousePos != null && mkr.containsPoint(mousePos)) {
                    mkr.paint(g, mv, mousePressedTmp, showTextOrIcon);
                    mousePressedTmp = false;
                }
            }
        } else {
            for (Marker mkr : data) {
                mkr.paint(g, mv, false, showTextOrIcon);
            }
        }
    }

    @Override public String getToolTipText() {
        return data.size()+" "+trn("marker", "markers", data.size());
    }

    @Override public void mergeFrom(Layer from) {
        MarkerLayer layer = (MarkerLayer)from;
        data.addAll(layer.data);
        Collections.sort(data, new Comparator<Marker>() {
            @Override
            public int compare(Marker o1, Marker o2) {
                return Double.compare(o1.time, o2.time);
            }
        });
    }

    @Override public boolean isMergable(Layer other) {
        return other instanceof MarkerLayer;
    }

    @Override public void visitBoundingBox(BoundingXYVisitor v) {
        for (Marker mkr : data) {
            v.visit(mkr.getEastNorth());
        }
    }

    @Override public Object getInfoComponent() {
        return "<html>"+trn("{0} consists of {1} marker", "{0} consists of {1} markers", data.size(), getName(), data.size()) + "</html>";
    }

    @Override public Action[] getMenuEntries() {
        Collection<Action> components = new ArrayList<Action>();
        components.add(LayerListDialog.getInstance().createShowHideLayerAction());
        components.add(new ShowHideMarkerText(this));
        components.add(LayerListDialog.getInstance().createDeleteLayerAction());
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new CustomizeColor(this));
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new SynchronizeAudio());
        if (Main.pref.getBoolean("marker.traceaudio", true)) {
            components.add (new MoveAudio());
        }
        components.add(new JumpToNextMarker(this));
        components.add(new JumpToPreviousMarker(this));
        components.add(new RenameLayerAction(getAssociatedFile(), this));
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new LayerListPopup.InfoAction(this));
        return components.toArray(new Action[components.size()]);
    }

    public boolean synchronizeAudioMarkers(AudioMarker startMarker) {
        if (startMarker != null && ! data.contains(startMarker)) {
            startMarker = null;
        }
        if (startMarker == null) {
            // find the first audioMarker in this layer
            for (Marker m : data) {
                if (m instanceof AudioMarker) {
                    startMarker = (AudioMarker) m;
                    break;
                }
            }
        }
        if (startMarker == null)
            return false;

        // apply adjustment to all subsequent audio markers in the layer
        double adjustment = AudioPlayer.position() - startMarker.offset; // in seconds
        boolean seenStart = false;
        try {
            URI uri = startMarker.url().toURI();
            for (Marker m : data) {
                if (m == startMarker) {
                    seenStart = true;
                }
                if (seenStart && m instanceof AudioMarker) {
                    AudioMarker ma = (AudioMarker) m;
                    // Do not ever call URL.equals but use URI.equals instead to avoid Internet connection
                    // See http://michaelscharf.blogspot.fr/2006/11/javaneturlequals-and-hashcode-make.html for details
                    if (ma.url().toURI().equals(uri)) {
                        ma.adjustOffset(adjustment);
                    }
                }
            }
        } catch (URISyntaxException e) {
            Main.warn(e);
        }
        return true;
    }

    public AudioMarker addAudioMarker(double time, LatLon coor) {
        // find first audio marker to get absolute start time
        double offset = 0.0;
        AudioMarker am = null;
        for (Marker m : data) {
            if (m.getClass() == AudioMarker.class) {
                am = (AudioMarker)m;
                offset = time - am.time;
                break;
            }
        }
        if (am == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No existing audio markers in this layer to offset from."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
                    );
            return null;
        }

        // make our new marker
        AudioMarker newAudioMarker = new AudioMarker(coor,
                null, AudioPlayer.url(), this, time, offset);

        // insert it at the right place in a copy the collection
        Collection<Marker> newData = new ArrayList<Marker>();
        am = null;
        AudioMarker ret = newAudioMarker; // save to have return value
        for (Marker m : data) {
            if (m.getClass() == AudioMarker.class) {
                am = (AudioMarker) m;
                if (newAudioMarker != null && offset < am.offset) {
                    newAudioMarker.adjustOffset(am.syncOffset()); // i.e. same as predecessor
                    newData.add(newAudioMarker);
                    newAudioMarker = null;
                }
            }
            newData.add(m);
        }

        if (newAudioMarker != null) {
            if (am != null) {
                newAudioMarker.adjustOffset(am.syncOffset()); // i.e. same as predecessor
            }
            newData.add(newAudioMarker); // insert at end
        }

        // replace the collection
        data.clear();
        data.addAll(newData);
        return ret;
    }

    @Override
    public void jumpToNextMarker() {
        if (currentMarker == null) {
            currentMarker = data.get(0);
        } else {
            boolean foundCurrent = false;
            for (Marker m: data) {
                if (foundCurrent) {
                    currentMarker = m;
                    break;
                } else if (currentMarker == m) {
                    foundCurrent = true;
                }
            }
        }
        Main.map.mapView.zoomTo(currentMarker.getEastNorth());
    }

    @Override
    public void jumpToPreviousMarker() {
        if (currentMarker == null) {
            currentMarker = data.get(data.size() - 1);
        } else {
            boolean foundCurrent = false;
            for (int i=data.size() - 1; i>=0; i--) {
                Marker m = data.get(i);
                if (foundCurrent) {
                    currentMarker = m;
                    break;
                } else if (currentMarker == m) {
                    foundCurrent = true;
                }
            }
        }
        Main.map.mapView.zoomTo(currentMarker.getEastNorth());
    }

    public static void playAudio() {
        playAdjacentMarker(null, true);
    }

    public static void playNextMarker() {
        playAdjacentMarker(AudioMarker.recentlyPlayedMarker(), true);
    }

    public static void playPreviousMarker() {
        playAdjacentMarker(AudioMarker.recentlyPlayedMarker(), false);
    }

    private static Marker getAdjacentMarker(Marker startMarker, boolean next, Layer layer) {
        Marker previousMarker = null;
        boolean nextTime = false;
        if (layer.getClass() == MarkerLayer.class) {
            MarkerLayer markerLayer = (MarkerLayer) layer;
            for (Marker marker : markerLayer.data) {
                if (marker == startMarker) {
                    if (next) {
                        nextTime = true;
                    } else {
                        if (previousMarker == null) {
                            previousMarker = startMarker; // if no previous one, play the first one again
                        }
                        return previousMarker;
                    }
                }
                else if (marker.getClass() == AudioMarker.class)
                {
                    if(nextTime || startMarker == null)
                        return marker;
                    previousMarker = marker;
                }
            }
            if (nextTime) // there was no next marker in that layer, so play the last one again
                return startMarker;
        }
        return null;
    }

    private static void playAdjacentMarker(Marker startMarker, boolean next) {
        Marker m = null;
        if (!Main.isDisplayingMapView())
            return;
        Layer l = Main.map.mapView.getActiveLayer();
        if(l != null) {
            m = getAdjacentMarker(startMarker, next, l);
        }
        if(m == null)
        {
            for (Layer layer : Main.map.mapView.getAllLayers())
            {
                m = getAdjacentMarker(startMarker, next, layer);
                if(m != null) {
                    break;
                }
            }
        }
        if(m != null) {
            ((AudioMarker)m).play();
        }
    }

    /**
     * Get state of text display.
     * @return <code>true</code> if text should be shown, <code>false</code> otherwise.
     */
    private boolean isTextOrIconShown() {
        String current = Main.pref.get("marker.show "+getName(),"show");
        return "show".equalsIgnoreCase(current);
    }

    public static final class ShowHideMarkerText extends AbstractAction implements LayerAction {
        private final MarkerLayer layer;

        public ShowHideMarkerText(MarkerLayer layer) {
            super(tr("Show Text/Icons"), ImageProvider.get("dialogs", "showhide"));
            putValue(SHORT_DESCRIPTION, tr("Toggle visible state of the marker text and icons."));
            putValue("help", ht("/Action/ShowHideTextIcons"));
            this.layer = layer;
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            Main.pref.put("marker.show "+layer.getName(), layer.isTextOrIconShown() ? "hide" : "show");
            Main.map.mapView.repaint();
        }


        @Override
        public Component createMenuComponent() {
            JCheckBoxMenuItem showMarkerTextItem = new JCheckBoxMenuItem(this);
            showMarkerTextItem.setState(layer.isTextOrIconShown());
            return showMarkerTextItem;
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return layers.size() == 1 && layers.get(0) instanceof MarkerLayer;
        }
    }


    private class SynchronizeAudio extends AbstractAction {

        public SynchronizeAudio() {
            super(tr("Synchronize Audio"), ImageProvider.get("audio-sync"));
            putValue("help", ht("/Action/SynchronizeAudio"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (! AudioPlayer.paused()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("You need to pause audio at the moment when you hear your synchronization cue."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                        );
                return;
            }
            AudioMarker recent = AudioMarker.recentlyPlayedMarker();
            if (synchronizeAudioMarkers(recent)) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Audio synchronized at point {0}.", recent.getText()),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                        );
            } else {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Unable to synchronize in layer being played."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            }
        }
    }

    private class MoveAudio extends AbstractAction {

        public MoveAudio() {
            super(tr("Make Audio Marker at Play Head"), ImageProvider.get("addmarkers"));
            putValue("help", ht("/Action/MakeAudioMarkerAtPlayHead"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (! AudioPlayer.paused()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("You need to have paused audio at the point on the track where you want the marker."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                        );
                return;
            }
            PlayHeadMarker playHeadMarker = Main.map.mapView.playHeadMarker;
            if (playHeadMarker == null)
                return;
            addAudioMarker(playHeadMarker.time, playHeadMarker.getCoor());
            Main.map.mapView.repaint();
        }
    }

}
