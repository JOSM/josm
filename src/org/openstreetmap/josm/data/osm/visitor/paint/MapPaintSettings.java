// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Color;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

public class MapPaintSettings implements PreferenceChangedListener {

    public static final MapPaintSettings INSTANCE = new MapPaintSettings();

    private boolean useRealWidth;
    private boolean showDirectionArrow;
    private boolean showRelevantDirectionsOnly;
    private int defaultSegmentWidth;
    private boolean showOrderNumber;
    private boolean showHeadArrowOnly;
    private int showNamesDistance;
    private int useStrokesDistance;
    private int showIconsDistance;
    private int selectedNodeSize;
    private int junctionNodeSize;
    private int unselectedNodeSize;
    private boolean fillSelectedNode;
    private boolean fillUnselectedNode;
    private boolean fillTaggedNode;
    private Color selectedColor;
    private Color highlightColor;
    private Color inactiveColor;
    private Color nodeColor;
    private Color taggedColor;

    private MapPaintSettings() {
        load();
        Main.pref.addPreferenceChangeListener(this);
    }

    private void load() {
        showDirectionArrow = Main.pref.getBoolean("draw.segment.direction", true);
        showRelevantDirectionsOnly = Main.pref.getBoolean("draw.segment.relevant_directions_only", true);
        useRealWidth = Main.pref.getBoolean("mappaint.useRealWidth", false);
        defaultSegmentWidth = Main.pref.getInteger("mappaint.segment.default-width", 2);

        selectedColor = PaintColors.SELECTED.get();
        highlightColor = PaintColors.HIGHLIGHT.get();
        inactiveColor = PaintColors.INACTIVE.get();
        nodeColor = PaintColors.NODE.get();
        taggedColor = PaintColors.TAGGED.get();

        showOrderNumber = Main.pref.getBoolean("draw.segment.order_number", false);
        showHeadArrowOnly = Main.pref.getBoolean("draw.segment.head_only", false);

        showNamesDistance = Main.pref.getInteger("mappaint.shownames", 10000000);
        useStrokesDistance = Main.pref.getInteger("mappaint.strokes", 10000000);
        showIconsDistance = Main.pref.getInteger("mappaint.showicons", 10000000);

        selectedNodeSize = Main.pref.getInteger("mappaint.node.selected-size", 5);
        unselectedNodeSize = Main.pref.getInteger("mappaint.node.unselected-size", 3);
        junctionNodeSize = Main.pref.getInteger("mappaint.node.junction-size", 5);
        fillSelectedNode = Main.pref.getBoolean("mappaint.node.fill-selected", true);
        fillUnselectedNode = Main.pref.getBoolean("mappaint.node.fill-unselected", false);
        fillTaggedNode = Main.pref.getBoolean("mappaint.node.fill-tagged", true);
    }

    public void preferenceChanged(PreferenceChangeEvent e) {
        load();
    }

    public boolean isUseRealWidth() {
        return useRealWidth;
    }

    public boolean isShowDirectionArrow() {
        return showDirectionArrow;
    }

    public boolean isShowRelevantDirectionsOnly() {
        return showRelevantDirectionsOnly;
    }

    public int getDefaultSegmentWidth() {
        return defaultSegmentWidth;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public Color getInactiveColor() {
        return inactiveColor;
    }

    public Color getNodeColor() {
        return nodeColor;
    }

    public Color getTaggedColor() {
        return taggedColor;
    }

    public boolean isShowOrderNumber() {
        return showOrderNumber;
    }

    public void setShowHeadArrowOnly(boolean showHeadArrowOnly) {
        this.showHeadArrowOnly = showHeadArrowOnly;
    }

    public boolean isShowHeadArrowOnly() {
        return showHeadArrowOnly;
    }

    public int getShowNamesDistance() {
        return showNamesDistance;
    }

    public int getUseStrokesDistance() {
        return useStrokesDistance;
    }

    public int getShowIconsDistance() {
        return showIconsDistance;
    }

    public int getSelectedNodeSize() {
        return selectedNodeSize;
    }

    public int getJunctionNodeSize() {
        return junctionNodeSize;
    }

    public int getUnselectedNodeSize() {
        return unselectedNodeSize;
    }

    public boolean isFillSelectedNode() {
        return fillSelectedNode;
    }

    public boolean isFillUnselectedNode() {
        return fillUnselectedNode;
    }

    public boolean isFillTaggedNode() {
        return fillTaggedNode;
    }
}
