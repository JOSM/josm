// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BasicStroke;
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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxExtension;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.IGpxLayerPrefs;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.preferences.StrokeProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.CustomizeColor;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToMarkerLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToNextMarker;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToPreviousMarker;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.geoimage.IGeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.RemoteEntry;
import org.openstreetmap.josm.gui.layer.gpx.ConvertFromMarkerLayerAction;
import org.openstreetmap.josm.gui.preferences.display.GPXSettingsPanel;
import org.openstreetmap.josm.io.audio.AudioPlayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * A layer holding markers.
 * <p>
 * Markers are GPS points with a name and, optionally, a symbol code attached;
 * marker layers can be created from waypoints when importing raw GPS data,
 * but they may also come from other sources.
 * <p>
 * The symbol code is for future use.
 * <p>
 * The data is read only.
 */
public class MarkerLayer extends Layer implements JumpToMarkerLayer, IGeoImageLayer {

    /**
     * A list of markers.
     */
    public final MarkerData data;
    private boolean mousePressed;
    public GpxLayer fromLayer;
    private Marker currentMarker;
    public AudioMarker syncAudioMarker;
    private Color color;
    private Color realcolor;
    final int markerSize = new IntegerProperty("draw.rawgps.markers.size", 4).get();
    final BasicStroke markerStroke = new StrokeProperty("draw.rawgps.markers.stroke", "1").get();

    private final ListenerList<IGeoImageLayer.ImageChangeListener> imageChangeListenerListenerList = ListenerList.create();

    /**
     * The default color that is used for drawing markers.
     */
    public static final NamedColorProperty DEFAULT_COLOR_PROPERTY = new NamedColorProperty(marktr("gps marker"), Color.magenta);

    /**
     * Constructs a new {@code MarkerLayer}.
     * @param indata The GPX data for this layer
     * @param name The marker layer name
     * @param associatedFile The associated GPX file
     * @param fromLayer The associated GPX layer
     */
    public MarkerLayer(GpxData indata, String name, File associatedFile, GpxLayer fromLayer) {
        super(name);
        this.setAssociatedFile(associatedFile);
        this.data = new MarkerData();
        this.fromLayer = fromLayer;
        double firstTime = -1.0;
        String lastLinkedFile = "";

        if (fromLayer == null || fromLayer.data == null) {
            data.ownLayerPrefs = indata.getLayerPrefs();
        }

        String cs = GPXSettingsPanel.tryGetDataPrefLocal(data, "markers.color");
        Color c = null;
        if (cs != null) {
            c = ColorHelper.html2color(cs);
            if (c == null) {
                Logging.warn("Could not read marker color: " + cs);
            }
        }
        setPrivateColors(c);

        for (WayPoint wpt : indata.waypoints) {
            /* calculate time differences in waypoints */
            double time = wpt.getTime();
            boolean wptHasLink = wpt.attr.containsKey(GpxConstants.META_LINKS);
            if (firstTime < 0 && wptHasLink) {
                firstTime = time;
                for (GpxLink oneLink : wpt.<GpxLink>getCollection(GpxConstants.META_LINKS)) {
                    lastLinkedFile = oneLink.uri;
                    break;
                }
            }
            if (wptHasLink) {
                for (GpxLink oneLink : wpt.<GpxLink>getCollection(GpxConstants.META_LINKS)) {
                    String uri = oneLink.uri;
                    if (uri != null) {
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
            GpxExtension offsetExt = wpt.getExtensions().get("josm", "offset");
            if (offsetExt != null && offsetExt.getValue() != null) {
                try {
                    offset = Double.valueOf(offsetExt.getValue());
                } catch (NumberFormatException nfe) {
                    Logging.warn(nfe);
                }
            }
            if (offset == null) {
                offset = time - firstTime;
            }
            final Collection<Marker> markers = Marker.createMarkers(wpt, indata.storageFile, this, time, offset);
            if (markers != null) {
                data.addAll(markers);
            }
        }
    }

    @Override
    public synchronized void destroy() {
        if (data.contains(AudioMarker.recentlyPlayedMarker())) {
            AudioMarker.resetRecentlyPlayedMarker();
        }
        syncAudioMarker = null;
        currentMarker = null;
        fromLayer = null;
        data.forEach(Marker::destroy);
        data.clear();
        super.destroy();
    }

    @Override
    public LayerPainter attachToMapView(MapViewEvent event) {
        event.getMapView().addMouseListener(new MarkerMouseAdapter());

        if (event.getMapView().playHeadMarker == null) {
            event.getMapView().playHeadMarker = PlayHeadMarker.create();
        }

        return super.attachToMapView(event);
    }

    /**
     * Return a static icon.
     */
    @Override
    public Icon getIcon() {
        return ImageProvider.get("layer", "marker_small");
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        boolean showTextOrIcon = isTextOrIconShown();
        g.setColor(realcolor);
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

    @Override
    public String getToolTipText() {
        return Integer.toString(data.size())+' '+trn("marker", "markers", data.size());
    }

    @Override
    public void mergeFrom(Layer from) {
        if (from instanceof MarkerLayer) {
            data.addAll(((MarkerLayer) from).data);
            data.sort(Comparator.comparingDouble(o -> o.time));
        }
    }

    @Override public boolean isMergable(Layer other) {
        return other instanceof MarkerLayer;
    }

    @Override public void visitBoundingBox(BoundingXYVisitor v) {
        for (Marker mkr : data) {
            v.visit(mkr);
        }
    }

    @Override public Object getInfoComponent() {
        return "<html>"+trn("{0} consists of {1} marker", "{0} consists of {1} markers",
                data.size(), Utils.escapeReservedCharactersHTML(getName()), data.size()) + "</html>";
    }

    @Override public Action[] getMenuEntries() {
        Collection<Action> components = new ArrayList<>();
        components.add(LayerListDialog.getInstance().createShowHideLayerAction());
        components.add(new ShowHideMarkerText(this));
        components.add(LayerListDialog.getInstance().createDeleteLayerAction());
        components.add(MainApplication.getMenu().autoScaleActions.get(AutoScaleAction.AutoScaleMode.LAYER));
        components.add(LayerListDialog.getInstance().createMergeLayerAction(this));
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new CustomizeColor(this));
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new SynchronizeAudio());
        if (Config.getPref().getBoolean("marker.traceaudio", true)) {
            components.add(new MoveAudio());
        }
        components.add(new JumpToNextMarker(this));
        components.add(new JumpToPreviousMarker(this));
        components.add(new ConvertFromMarkerLayerAction(this));
        components.add(new RenameLayerAction(getAssociatedFile(), this));
        components.add(SeparatorLayerAction.INSTANCE);
        components.add(new LayerListPopup.InfoAction(this));
        return components.toArray(new Action[0]);
    }

    public boolean synchronizeAudioMarkers(final AudioMarker startMarker) {
        syncAudioMarker = startMarker;
        if (syncAudioMarker != null && !data.contains(syncAudioMarker)) {
            syncAudioMarker = null;
        }
        if (syncAudioMarker == null) {
            // find the first audioMarker in this layer
            syncAudioMarker = Utils.filteredCollection(data, AudioMarker.class).stream()
                    .findFirst().orElse(syncAudioMarker);
        }
        if (syncAudioMarker == null)
            return false;

        // apply adjustment to all subsequent audio markers in the layer
        double adjustment = AudioPlayer.position() - syncAudioMarker.offset; // in seconds
        boolean seenStart = false;
        try {
            URI uri = syncAudioMarker.url().toURI();
            for (Marker m : data) {
                if (m == syncAudioMarker) {
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
            Logging.warn(e);
        }
        return true;
    }

    public AudioMarker addAudioMarker(double time, LatLon coor) {
        // find first audio marker to get absolute start time
        double offset = 0.0;
        AudioMarker am = null;
        for (Marker m : data) {
            if (m.getClass() == AudioMarker.class) {
                am = (AudioMarker) m;
                offset = time - am.time;
                break;
            }
        }
        if (am == null) {
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
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
        Collection<Marker> newData = new ArrayList<>();
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
        MainApplication.getMap().mapView.zoomTo(currentMarker);
    }

    @Override
    public void jumpToPreviousMarker() {
        if (currentMarker == null) {
            currentMarker = data.get(data.size() - 1);
        } else {
            boolean foundCurrent = false;
            for (int i = data.size() - 1; i >= 0; i--) {
                Marker m = data.get(i);
                if (foundCurrent) {
                    currentMarker = m;
                    break;
                } else if (currentMarker == m) {
                    foundCurrent = true;
                }
            }
        }
        MainApplication.getMap().mapView.zoomTo(currentMarker);
    }

    /**
     * Set the current marker
     * @param newMarker The marker to set
     */
    void setCurrentMarker(Marker newMarker) {
        this.currentMarker = newMarker;
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
                } else if (marker.getClass() == AudioMarker.class) {
                    if (nextTime || startMarker == null)
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
        if (!MainApplication.isDisplayingMapView())
            return;
        Marker m = null;
        Layer l = MainApplication.getLayerManager().getActiveLayer();
        if (l != null) {
            m = getAdjacentMarker(startMarker, next, l);
        }
        if (m == null) {
            for (Layer layer : MainApplication.getLayerManager().getLayers()) {
                m = getAdjacentMarker(startMarker, next, layer);
                if (m != null) {
                    break;
                }
            }
        }
        if (m != null) {
            ((AudioMarker) m).play();
        }
    }

    /**
     * Get state of text display.
     * @return <code>true</code> if text should be shown, <code>false</code> otherwise.
     */
    private boolean isTextOrIconShown() {
        return Boolean.parseBoolean(GPXSettingsPanel.getDataPref(data, "markers.show-text"));
    }

    @Override
    public boolean hasColor() {
        return true;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        setPrivateColors(color);
        String cs = null;
        if (color != null) {
            cs = ColorHelper.color2html(color);
        }
        GPXSettingsPanel.putDataPrefLocal(data, "markers.color", cs);
        invalidate();
    }

    private void setPrivateColors(Color color) {
        this.color = color;
        this.realcolor = Optional.ofNullable(color).orElse(DEFAULT_COLOR_PROPERTY.get());
    }

    @Override
    public void clearSelection() {
        this.currentMarker = null;
    }

    @Override
    public List<? extends IImageEntry<?>> getSelection() {
        if (this.currentMarker instanceof ImageMarker) {
            final RemoteEntry remoteEntry = ((ImageMarker) this.currentMarker).getRemoteEntry();
            if (remoteEntry != null) {
                return Collections.singletonList(remoteEntry);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean containsImage(IImageEntry<?> imageEntry) {
        if (imageEntry instanceof RemoteEntry) {
            RemoteEntry entry = (RemoteEntry) imageEntry;
            if (entry.getPos() != null && entry.getPos().isLatLonKnown()) {
                List<Marker> markers = this.data.search(new BBox(entry.getPos()));
                return checkIfListContainsEntry(markers, entry);
            } else if (entry.getExifCoor() != null && entry.getExifCoor().isLatLonKnown()) {
                List<Marker> markers = this.data.search(new BBox(entry.getExifCoor()));
                return checkIfListContainsEntry(markers, entry);
            } else {
                return checkIfListContainsEntry(this.data, entry);
            }
        }
        return false;
    }

    /**
     * Check if a list contains an entry
     * @param markerList The list to look through
     * @param imageEntry The image entry to check
     * @return {@code true} if the entry is in the list
     */
    private static boolean checkIfListContainsEntry(List<Marker> markerList, RemoteEntry imageEntry) {
        for (Marker marker : markerList) {
            if (marker instanceof ImageMarker) {
                ImageMarker imageMarker = (ImageMarker) marker;
                try {
                    if (Objects.equals(imageMarker.imageUrl.toURI(), imageEntry.getImageURI())) {
                        return true;
                    }
                } catch (URISyntaxException e) {
                    Logging.trace(e);
                }
            }
        }
        return false;
    }

    @Override
    public void addImageChangeListener(ImageChangeListener listener) {
        this.imageChangeListenerListenerList.addListener(listener);
    }

    @Override
    public void removeImageChangeListener(ImageChangeListener listener) {
        this.imageChangeListenerListenerList.removeListener(listener);
    }

    private final class MarkerMouseAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1)
                return;
            boolean mousePressedInButton = data.stream().anyMatch(mkr -> mkr.containsPoint(e.getPoint()));
            if (!mousePressedInButton)
                return;
            mousePressed = true;
            if (isVisible()) {
                invalidate();
            }
        }

        @Override
        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() != MouseEvent.BUTTON1 || !mousePressed)
                return;
            mousePressed = false;
            if (!isVisible())
                return;
            for (Marker mkr : data) {
                if (mkr.containsPoint(ev.getPoint())) {
                    mkr.actionPerformed(new ActionEvent(this, 0, null));
                }
            }
            invalidate();
        }
    }

    public static final class ShowHideMarkerText extends AbstractAction implements LayerAction {
        private final transient MarkerLayer layer;

        public ShowHideMarkerText(MarkerLayer layer) {
            super(tr("Show Text/Icons"));
            new ImageProvider("dialogs", "showhide").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Toggle visible state of the marker text and icons."));
            putValue("help", ht("/Action/ShowHideTextIcons"));
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GPXSettingsPanel.putDataPrefLocal(layer.data, "markers.show-text", Boolean.toString(!layer.isTextOrIconShown()));
            layer.invalidate();
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

        /**
         * Constructs a new {@code SynchronizeAudio} action.
         */
        SynchronizeAudio() {
            super(tr("Synchronize Audio"));
            new ImageProvider("audio-sync").getResource().attachImageIcon(this, true);
            putValue("help", ht("/Action/SynchronizeAudio"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!AudioPlayer.paused()) {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("You need to pause audio at the moment when you hear your synchronization cue."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                        );
                return;
            }
            AudioMarker recent = AudioMarker.recentlyPlayedMarker();
            if (synchronizeAudioMarkers(recent)) {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("Audio synchronized at point {0}.", syncAudioMarker.getText()),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                        );
            } else {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("Unable to synchronize in layer being played."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            }
        }
    }

    private class MoveAudio extends AbstractAction {

        MoveAudio() {
            super(tr("Make Audio Marker at Play Head"));
            new ImageProvider("addmarkers").getResource().attachImageIcon(this, true);
            putValue("help", ht("/Action/MakeAudioMarkerAtPlayHead"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!AudioPlayer.paused()) {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("You need to have paused audio at the point on the track where you want the marker."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                        );
                return;
            }
            PlayHeadMarker playHeadMarker = MainApplication.getMap().mapView.playHeadMarker;
            if (playHeadMarker == null)
                return;
            addAudioMarker(playHeadMarker.time, playHeadMarker.getCoor());
            invalidate();
        }
    }

    /**
     * the data of a MarkerLayer
     * @since 18287
     */
    public class MarkerData extends QuadBuckets<Marker> implements List<Marker>, IGpxLayerPrefs {

        private Map<String, String> ownLayerPrefs;
        private final List<Marker> markerList = new ArrayList<>();

        @Override
        public Map<String, String> getLayerPrefs() {
            if (ownLayerPrefs == null && fromLayer != null && fromLayer.data != null) {
                return fromLayer.data.getLayerPrefs();
            }
            // fallback to own layerPrefs if the corresponding gpxLayer has already been deleted
            // by the user or never existed when loaded from a session file
            if (ownLayerPrefs == null) {
                ownLayerPrefs = new HashMap<>();
            }
            return ownLayerPrefs;
        }

        /**
         * Transfers the layerPrefs from the GpxData to MarkerData (when GpxData is deleted)
         * @param gpxLayerPrefs the layerPrefs from the GpxData object
         */
        public void transferLayerPrefs(Map<String, String> gpxLayerPrefs) {
            ownLayerPrefs = new HashMap<>(gpxLayerPrefs);
        }

        @Override
        public void setModified(boolean value) {
            if (fromLayer != null && fromLayer.data != null) {
                fromLayer.data.setModified(value);
            }
        }

        @Override
        public boolean addAll(int index, Collection<? extends Marker> c) {
            c.forEach(this::add);
            return this.markerList.addAll(index, c);
        }

        @Override
        public boolean addAll(Collection<? extends Marker> objects) {
            return this.markerList.addAll(objects) && super.addAll(objects);
        }

        @Override
        public Marker get(int index) {
            return this.markerList.get(index);
        }

        @Override
        public Marker set(int index, Marker element) {
            Marker original = this.markerList.set(index, element);
            this.remove(original);
            return original;
        }

        @Override
        public void add(int index, Marker element) {
            this.add(element);
            this.markerList.add(index, element);
        }

        @Override
        public Marker remove(int index) {
            Marker toRemove = this.markerList.remove(index);
            this.remove(toRemove);
            return toRemove;
        }

        @Override
        public int indexOf(Object o) {
            return this.markerList.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return this.markerList.lastIndexOf(o);
        }

        @Override
        public ListIterator<Marker> listIterator() {
            return this.markerList.listIterator();
        }

        @Override
        public ListIterator<Marker> listIterator(int index) {
            return this.markerList.listIterator(index);
        }

        @Override
        public List<Marker> subList(int fromIndex, int toIndex) {
            return this.markerList.subList(fromIndex, toIndex);
        }

        @Override
        public boolean retainAll(Collection<?> objects) {
            return this.markerList.retainAll(objects) && super.retainAll(objects);
        }

        @Override
        public boolean contains(Object o) {
            return this.markerList.contains(o) && super.contains(o);
        }
    }
}
