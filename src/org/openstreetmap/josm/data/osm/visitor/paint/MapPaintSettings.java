// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Color;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

public final class MapPaintSettings implements PreferenceChangedListener {

    public static final MapPaintSettings INSTANCE = new MapPaintSettings();

    private boolean useRealWidth;
    /** Preference: should directional arrows be displayed */
    private boolean showDirectionArrow;
    /** Preference: should arrows for oneways be displayed */
    private boolean showOnewayArrow;
    private int defaultSegmentWidth;
    /** Preference: should the segement numbers of ways be displayed */
    private boolean showOrderNumber;
    /** Preference: should only the last arrow of a way be displayed */
    private boolean showHeadArrowOnly;
    private int showNamesDistance;
    private int useStrokesDistance;
    private int showIconsDistance;
    /** Preference: size of selected nodes */
    private int selectedNodeSize;
    /** Preference: size of multiply connected nodes */
    private int connectionNodeSize;
    /** Preference: size of unselected nodes */
    private int unselectedNodeSize;
    /** Preference: size of tagged nodes */
    private int taggedNodeSize;
    /** Preference: should selected nodes be filled */
    private boolean fillSelectedNode;
    /** Preference: should unselected nodes be filled */
    private boolean fillUnselectedNode;
    /** Preference: should tagged nodes be filled */
    private boolean fillTaggedNode;
    /** Preference: should multiply connected nodes be filled */
    private boolean fillConnectionNode;
    private boolean outlineOnly;
    /** Color Preference for selected objects */
    private Color selectedColor;
    private Color relationSelectedColor;
    /** Color Preference for hightlighted objects */
    private Color highlightColor;
    /** Color Preference for inactive objects */
    private Color inactiveColor;
    /** Color Preference for nodes */
    private Color nodeColor;
    /** Color Preference for tagged nodes */
    private Color taggedColor;
    /** Color Preference for multiply connected nodes */
    private Color connectionColor;
    /** Color Preference for tagged and multiply connected nodes */
    private Color taggedConnectionColor;

    private MapPaintSettings() {
        load();
        Main.pref.addPreferenceChangeListener(this);
    }

    private void load() {
        showDirectionArrow = Main.pref.getBoolean("draw.segment.direction", false);
        showOnewayArrow = Main.pref.getBoolean("draw.oneway", true);
        useRealWidth = Main.pref.getBoolean("mappaint.useRealWidth", false);
        defaultSegmentWidth = Main.pref.getInteger("mappaint.segment.default-width", 2);

        selectedColor = PaintColors.SELECTED.get();
        relationSelectedColor = PaintColors.RELATIONSELECTED.get();
        highlightColor = PaintColors.HIGHLIGHT.get();
        inactiveColor = PaintColors.INACTIVE.get();
        nodeColor = PaintColors.NODE.get();
        taggedColor = PaintColors.TAGGED.get();
        connectionColor = PaintColors.CONNECTION.get();
        if (taggedColor != nodeColor) {
            taggedConnectionColor = taggedColor;
        } else {
            taggedConnectionColor = connectionColor;
        }


        showOrderNumber = Main.pref.getBoolean("draw.segment.order_number", false);
        showHeadArrowOnly = Main.pref.getBoolean("draw.segment.head_only", false);

        showNamesDistance = Main.pref.getInteger("mappaint.shownames", 10000000);
        useStrokesDistance = Main.pref.getInteger("mappaint.strokes", 10000000);
        showIconsDistance = Main.pref.getInteger("mappaint.showicons", 10000000);

        selectedNodeSize = Main.pref.getInteger("mappaint.node.selected-size", 5);
        unselectedNodeSize = Main.pref.getInteger("mappaint.node.unselected-size", 3);
        connectionNodeSize = Main.pref.getInteger("mappaint.node.connection-size", 5);
        taggedNodeSize = Main.pref.getInteger("mappaint.node.tagged-size", 3);
        fillSelectedNode = Main.pref.getBoolean("mappaint.node.fill-selected", true);
        fillUnselectedNode = Main.pref.getBoolean("mappaint.node.fill-unselected", false);
        fillTaggedNode = Main.pref.getBoolean("mappaint.node.fill-tagged", true);
        fillConnectionNode = Main.pref.getBoolean("mappaint.node.fill-connection", false);

        outlineOnly = Main.pref.getBoolean("draw.data.area_outline_only", false);

    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        load();
    }

    public boolean isUseRealWidth() {
        return useRealWidth;
    }

    public boolean isShowDirectionArrow() {
        return showDirectionArrow;
    }

    public boolean isShowOnewayArrow() {
        return showOnewayArrow;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public int getDefaultSegmentWidth() {
        return defaultSegmentWidth;
    }

    public Color getSelectedColor(int alpha) {
        return new Color(selectedColor.getRGB() & 0x00ffffff | (alpha << 24), true);
    }

    public Color getRelationSelectedColor() {
        return relationSelectedColor;
    }

    public Color getRelationSelectedColor(int alpha) {
        return new Color(relationSelectedColor.getRGB() & 0x00ffffff | (alpha << 24), true);
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

    public Color getConnectionColor() {
        return connectionColor;
    }

    public Color getTaggedConnectionColor() {
        return taggedConnectionColor;
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

    public int getConnectionNodeSize() {
        return connectionNodeSize;
    }

    public int getUnselectedNodeSize() {
        return unselectedNodeSize;
    }

    public int getTaggedNodeSize() {
        return taggedNodeSize;
    }

    public boolean isFillSelectedNode() {
        return fillSelectedNode;
    }

    public boolean isFillUnselectedNode() {
        return fillUnselectedNode;
    }

    public boolean isFillConnectionNode() {
        return fillConnectionNode;
    }

    public boolean isFillTaggedNode() {
        return fillTaggedNode;
    }

    public boolean isOutlineOnly() {
        return outlineOnly;
    }
}
