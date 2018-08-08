// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * An abstract action for a conversion from a {@code T} {@link Layer} to a {@link OsmDataLayer}.
 * @param <T> the source layer class
 */
public abstract class ConvertToDataLayerAction<T extends Layer> extends AbstractAction {
    /** source layer */
    protected final transient T layer;

    /**
     * Constructs a new {@code ConvertToDataLayerAction}
     * @param layer source layer
     */
    protected ConvertToDataLayerAction(final T layer) {
        super(tr("Convert to data layer"));
        new ImageProvider("converttoosm").getResource().attachImageIcon(this, true);
        this.layer = layer;
        putValue("help", ht("/Action/ConvertToDataLayer"));
    }

    /**
     * Converts a {@link GpxLayer} to a {@link OsmDataLayer}.
     */
    public static class FromGpxLayer extends ConvertToDataLayerAction<GpxLayer> {

        private static final String GPX_SETTING = "gpx.convert-tags";
        private final DateFormat timeFormatter = DateUtils.getGpxFormat();

        /**
         * Creates a new {@code FromGpxLayer}.
         * @param layer the source layer
         */
        public FromGpxLayer(GpxLayer layer) {
            super(layer);
        }

        @Override
        public DataSet convert() {
            final DataSet ds = new DataSet();

            List<String> keys = new ArrayList<>();
            String convertTags = Config.getPref().get(GPX_SETTING, "ask");
            boolean check = "list".equals(convertTags) || "ask".equals(convertTags);
            boolean none = "no".equals(convertTags); // no need to convert tags when no dialog will be shown anyways

            for (GpxTrack trk : layer.data.getTracks()) {
                for (GpxTrackSegment segment : trk.getSegments()) {
                    List<Node> nodes = new ArrayList<>();
                    for (WayPoint p : segment.getWayPoints()) {
                        Node n = new Node(p.getCoor());
                        for (Entry<String, Object> entry : p.attr.entrySet()) {
                            String key = entry.getKey();
                            Object obj = p.get(key);
                            if (check && !keys.contains(key) && (obj instanceof String || obj instanceof Date)) {
                                keys.add(key);
                            }
                            if (obj instanceof String) {
                                String str = (String) obj;
                                if (!none) {
                                    // only convert when required
                                    n.put(key, str);
                                }
                                if (GpxConstants.PT_TIME.equals(key)) {
                                    // timestamps should always be converted
                                    try {
                                        n.setTimestamp(DateUtils.fromString(str));
                                    } catch (UncheckedParseException e) {
                                        Logging.log(Logging.LEVEL_WARN, e);
                                    }
                                }
                            } else if (obj instanceof Date && GpxConstants.PT_TIME.equals(key)) {
                                // timestamps should always be converted
                                Date date = (Date) obj;
                                if (!none) { //... but the tag will only be set when required
                                    n.put(key, timeFormatter.format(date));
                                }
                                n.setTimestamp(date);
                            }
                        }
                        ds.addPrimitive(n);
                        nodes.add(n);
                    }
                    Way w = new Way();
                    w.setNodes(nodes);
                    ds.addPrimitive(w);
                }
            }
            //gpx.convert-tags: all, list, *ask, no
            //gpx.convert-tags.last: *all, list, no
            //gpx.convert-tags.list.yes
            //gpx.convert-tags.list.no
            List<String> listPos = Config.getPref().getList(GPX_SETTING + ".list.yes");
            List<String> listNeg = Config.getPref().getList(GPX_SETTING + ".list.no");
            if (check && !keys.isEmpty()) {
                // Either "list" or "ask" was stored in the settings, so the Nodes have to be filtered after all tags have been processed
                List<String> allTags = new ArrayList<>(listPos);
                allTags.addAll(listNeg);
                if (!allTags.containsAll(keys) || "ask".equals(convertTags)) {
                    // not all keys are in positive/negative list, so we have to ask the user
                    TagConversionDialogResponse res = showTagConversionDialog(keys, listPos, listNeg);
                    if (res.sel == null) {
                        return null;
                    }
                    listPos = res.listPos;

                    if ("no".equals(res.sel)) {
                        // User just chose not to convert any tags, but that was unknown before the initial conversion
                        return filterDataSet(ds, null);
                    } else if ("all".equals(res.sel)) {
                        return ds;
                    }
                }
                if (!listPos.containsAll(keys)) {
                    return filterDataSet(ds, listPos);
                }
            }
            return ds;
        }

        /**
         * Filters the tags of the given {@link DataSet}
         * @param ds The {@link DataSet}
         * @param listPos A {@code List<String>} containing the tags to be kept, can be {@code null} if all tags are to be removed
         * @return The {@link DataSet}
         * @since 14103
         */
        public DataSet filterDataSet(DataSet ds, List<String> listPos) {
            Collection<Node> nodes = ds.getNodes();
            for (Node n : nodes) {
                for (String key : n.keySet()) {
                    if (listPos == null || !listPos.contains(key)) {
                        n.put(key, null);
                    }
                }
            }
            return ds;
        }

        /**
         * Shows the TagConversionDialog asking the user whether to keep all, some or no tags
         * @param keys The keys present during the current conversion
         * @param listPos The keys that were previously selected
         * @param listNeg The keys that were previously unselected
         * @return {@link TagConversionDialogResponse} containing the selection
         */
        private static TagConversionDialogResponse showTagConversionDialog(List<String> keys, List<String> listPos, List<String> listNeg) {
            TagConversionDialogResponse res = new TagConversionDialogResponse(listPos, listNeg);
            String lSel = Config.getPref().get(GPX_SETTING + ".last", "all");

            JPanel p = new JPanel(new GridBagLayout());
            ButtonGroup r = new ButtonGroup();

            p.add(new JLabel(
                    tr("The GPX layer contains fields that can be converted to OSM tags. How would you like to proceed?")),
                    GBC.eol());
            JRadioButton rAll = new JRadioButton(tr("Convert all fields"), "all".equals(lSel));
            r.add(rAll);
            p.add(rAll, GBC.eol());

            JRadioButton rList = new JRadioButton(tr("Only convert the following fields:"), "list".equals(lSel));
            r.add(rList);
            p.add(rList, GBC.eol());

            JPanel q = new JPanel();

            List<JCheckBox> checkList = new ArrayList<>();
            for (String key : keys) {
                JCheckBox cTmp = new JCheckBox(key, !listNeg.contains(key));
                checkList.add(cTmp);
                q.add(cTmp);
            }

            q.setBorder(BorderFactory.createEmptyBorder(0, 20, 5, 0));
            p.add(q, GBC.eol());

            JRadioButton rNone = new JRadioButton(tr("Do not convert any fields"), "no".equals(lSel));
            r.add(rNone);
            p.add(rNone, GBC.eol());

            ActionListener enabler = new TagConversionDialogRadioButtonActionListener(checkList, true);
            ActionListener disabler = new TagConversionDialogRadioButtonActionListener(checkList, false);

            if (!"list".equals(lSel)) {
                disabler.actionPerformed(null);
            }

            rAll.addActionListener(disabler);
            rList.addActionListener(enabler);
            rNone.addActionListener(disabler);

            ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Options"),
                    tr("Convert"), tr("Convert and remember selection"), tr("Cancel"))
                    .setButtonIcons("exportgpx", "exportgpx", "cancel").setContent(p);
            int ret = ed.showDialog().getValue();

            if (ret == 1 || ret == 2) {
                for (JCheckBox cItem : checkList) {
                    String key = cItem.getText();
                    if (cItem.isSelected()) {
                        if (!res.listPos.contains(key)) {
                            res.listPos.add(key);
                        }
                        res.listNeg.remove(key);
                    } else {
                        if (!res.listNeg.contains(key)) {
                            res.listNeg.add(key);
                        }
                        res.listPos.remove(key);
                    }
                }
                if (rAll.isSelected()) {
                    res.sel = "all";
                } else if (rNone.isSelected()) {
                    res.sel = "no";
                }
                Config.getPref().put(GPX_SETTING + ".last", res.sel);
                if (ret == 2) {
                    Config.getPref().put(GPX_SETTING, res.sel);
                } else {
                    Config.getPref().put(GPX_SETTING, "ask");
                }
                Config.getPref().putList(GPX_SETTING + ".list.yes", res.listPos);
                Config.getPref().putList(GPX_SETTING + ".list.no", res.listNeg);
            } else {
                res.sel = null;
            }
            return res;
        }

        private static class TagConversionDialogResponse {

            final List<String> listPos;
            final List<String> listNeg;
            String sel = "list";

            TagConversionDialogResponse(List<String> p, List<String> n) {
                listPos = new ArrayList<>(p);
                listNeg = new ArrayList<>(n);
            }
        }

        private static class TagConversionDialogRadioButtonActionListener implements ActionListener {

            private final boolean enable;
            private final List<JCheckBox> checkList;

            TagConversionDialogRadioButtonActionListener(List<JCheckBox> chks, boolean en) {
                enable = en;
                checkList = chks;
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                for (JCheckBox ch : checkList) {
                    ch.setEnabled(enable);
                }
            }
        }
    }

    /**
     * Converts a {@link MarkerLayer} to a {@link OsmDataLayer}.
     */
    public static class FromMarkerLayer extends ConvertToDataLayerAction<MarkerLayer> {

        /**
         * Converts a {@link MarkerLayer} to a {@link OsmDataLayer}.
         * @param layer marker layer
         */
        public FromMarkerLayer(MarkerLayer layer) {
            super(layer);
        }

        @Override
        public DataSet convert() {
            final DataSet ds = new DataSet();
            for (Marker marker : layer.data) {
                final Node node = new Node(marker.getCoor());
                final Collection<String> mapping = Config.getPref().getList("gpx.to-osm-mapping", Arrays.asList(
                        GpxConstants.GPX_NAME, "name",
                        GpxConstants.GPX_DESC, "description",
                        GpxConstants.GPX_CMT, "note",
                        GpxConstants.GPX_SRC, "source",
                        GpxConstants.PT_SYM, "gpxicon"));
                if (mapping.size() % 2 == 0) {
                    final Iterator<String> it = mapping.iterator();
                    while (it.hasNext()) {
                        final String gpxKey = it.next();
                        final String osmKey = it.next();
                        Optional.ofNullable(marker.getTemplateValue(gpxKey, false))
                                .map(String::valueOf)
                                .ifPresent(s -> node.put(osmKey, s));
                    }
                } else {
                    Logging.warn("Invalid gpx.to-osm-mapping Einstein setting: expecting even number of entries");
                }
                ds.addPrimitive(node);
            }
            return ds;
        }
    }

    /**
     * Performs the conversion to a {@link DataSet}.
     * @return the resulting dataset
     */
    public abstract DataSet convert();

    @Override
    public void actionPerformed(ActionEvent e) {
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JLabel(
                tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>"
                        + "If you want to upload traces, look here:</html>")),
                GBC.eol());
        msg.add(new UrlLabel(Main.getOSMWebsite() + "/traces", 2), GBC.eop());
        if (!ConditionalOptionPaneUtil.showConfirmationDialog("convert_to_data", Main.parent, msg, tr("Warning"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION)) {
            return;
        }
        final DataSet ds = convert();
        if (ds != null) {
            final OsmDataLayer osmLayer = new OsmDataLayer(ds, tr("Converted from: {0}", layer.getName()), null);
            if (layer.getAssociatedFile() != null) {
                osmLayer.setAssociatedFile(new File(layer.getAssociatedFile().getParentFile(),
                        layer.getAssociatedFile().getName() + ".osm"));
            }
            osmLayer.setUploadDiscouraged(true);
            MainApplication.getLayerManager().addLayer(osmLayer, false);
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
