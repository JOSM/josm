// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Color;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

/**
 * Global mappaint settings.
 * @since 2675
 */
public final class MapPaintSettings implements PreferenceChangedListener {

    /** The unique instance **/
    public static final MapPaintSettings INSTANCE = new MapPaintSettings();

    private boolean useRealWidth;
    /** Preference: should directional arrows be displayed */
    private boolean showDirectionArrow;
    /** Preference: should arrows for oneways be displayed */
    private boolean showOnewayArrow;
    /** Preference: default width for ways segments */
    private int defaultSegmentWidth;
    /** Preference: should the segment numbers of ways be displayed */
    private boolean showOrderNumber;
    /** Preference: should the segment numbers of ways be displayed on selected way */
    private boolean showOrderNumberOnSelectedWay;
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
    /** Preference: should only the data area outline be drawn */
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
        defaultSegmentWidth = Main.pref.getInt("mappaint.segment.default-width", 2);

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
        showOrderNumberOnSelectedWay = Main.pref.getBoolean("draw.segment.order_number.on_selected", false);
        showHeadArrowOnly = Main.pref.getBoolean("draw.segment.head_only", false);

        showNamesDistance = Main.pref.getInt("mappaint.shownames", 10_000_000);
        useStrokesDistance = Main.pref.getInt("mappaint.strokes", 10_000_000);
        showIconsDistance = Main.pref.getInt("mappaint.showicons", 10_000_000);

        selectedNodeSize = Main.pref.getInt("mappaint.node.selected-size", 5);
        unselectedNodeSize = Main.pref.getInt("mappaint.node.unselected-size", 3);
        connectionNodeSize = Main.pref.getInt("mappaint.node.connection-size", 5);
        taggedNodeSize = Main.pref.getInt("mappaint.node.tagged-size", 3);
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

    /**
     * Determines if the real width of ways should be used
     * @return {@code true} if the real width of ways should be used
     */
    public boolean isUseRealWidth() {
        return useRealWidth;
    }

    /**
     * Determines if directional arrows should be displayed
     * @return {@code true} if directional arrows should be displayed
     */
    public boolean isShowDirectionArrow() {
        return showDirectionArrow;
    }

    /**
     * Determines if arrows for oneways should be displayed
     * @return {@code true} if arrows for oneways should be displayed
     */
    public boolean isShowOnewayArrow() {
        return showOnewayArrow;
    }

    /**
     * Returns color for selected objects (nodes and ways)
     * @return color for selected objects
     */
    public Color getSelectedColor() {
        return selectedColor;
    }

    /**
     * Returns color for selected objects (nodes and ways) with a given alpha
     * @param alpha alpha component in the range 0-255
     * @return color for selected objects
     */
    public Color getSelectedColor(int alpha) {
        return new Color((selectedColor.getRGB() & 0x00ffffff) | (alpha << 24), true);
    }

    /**
     * Returns default width for ways segments
     * @return default width for ways segments
     */
    public int getDefaultSegmentWidth() {
        return defaultSegmentWidth;
    }

    /**
     * Returns color for selected relations
     * @return color for selected relations
     */
    public Color getRelationSelectedColor() {
        return relationSelectedColor;
    }

    /**
     * Returns color for selected relations with a given alpha
     * @param alpha alpha component in the range 0-255
     * @return color for selected relations
     */
    public Color getRelationSelectedColor(int alpha) {
        return new Color((relationSelectedColor.getRGB() & 0x00ffffff) | (alpha << 24), true);
    }

    /**
     * Returns color for hightlighted objects
     * @return color for hightlighted objects
     */
    public Color getHighlightColor() {
        return highlightColor;
    }

    /**
     * Returns color for inactive objects
     * @return color for inactive objects
     */
    public Color getInactiveColor() {
        return inactiveColor;
    }

    /**
     * Returns color for nodes
     * @return color for nodes
     */
    public Color getNodeColor() {
        return nodeColor;
    }

    /**
     * Returns color for tagged nodes
     * @return color for tagged nodes
     */
    public Color getTaggedColor() {
        return taggedColor;
    }

    /**
     * Returns color for multiply connected nodes
     * @return color for multiply connected nodes
     */
    public Color getConnectionColor() {
        return connectionColor;
    }

    /**
     * Returns color for tagged and multiply connected nodes
     * @return color for tagged and multiply connected nodes
     */
    public Color getTaggedConnectionColor() {
        return taggedConnectionColor;
    }

    /**
     * Determines if the segment numbers of ways should be displayed
     * @return {@code true} if the segment numbers of ways should be displayed
     */
    public boolean isShowOrderNumber() {
        return showOrderNumber;
    }

    /**
     * Determines if the segment numbers of the selected way should be displayed
     * @return {@code true} if the segment numbers of the selected way should be displayed
     */
    public boolean isShowOrderNumberOnSelectedWay() {
        return showOrderNumberOnSelectedWay;
    }

    /**
     * Specifies if only the last arrow of a way should be displayed
     * @param showHeadArrowOnly {@code true} if only the last arrow of a way should be displayed
     */
    public void setShowHeadArrowOnly(boolean showHeadArrowOnly) {
        this.showHeadArrowOnly = showHeadArrowOnly;
    }

    /**
     * Determines if only the last arrow of a way should be displayed
     * @return {@code true} if only the last arrow of a way should be displayed
     */
    public boolean isShowHeadArrowOnly() {
        return showHeadArrowOnly;
    }

    /**
     * Returns the distance at which names should be drawn
     * @return the distance at which names should be drawn
     */
    public int getShowNamesDistance() {
        return showNamesDistance;
    }

    /**
     * Returns the distance at which strokes should be used
     * @return the distance at which strokes should be used
     */
    public int getUseStrokesDistance() {
        return useStrokesDistance;
    }

    /**
     * Returns the distance at which icons should be drawn
     * @return the distance at which icons should be drawn
     */
    public int getShowIconsDistance() {
        return showIconsDistance;
    }

    /**
     * Returns the size of selected nodes
     * @return the size of selected nodes
     */
    public int getSelectedNodeSize() {
        return selectedNodeSize;
    }

    /**
     * Returns the size of multiply connected nodes
     * @return the size of multiply connected nodes
     */
    public int getConnectionNodeSize() {
        return connectionNodeSize;
    }

    /**
     * Returns the size of unselected nodes
     * @return the size of unselected nodes
     */
    public int getUnselectedNodeSize() {
        return unselectedNodeSize;
    }

    /**
     * Returns the size of tagged nodes
     * @return the size of tagged nodes
     */
    public int getTaggedNodeSize() {
        return taggedNodeSize;
    }

    /**
     * Determines if selected nodes should be filled
     * @return {@code true} if selected nodes should be filled
     */
    public boolean isFillSelectedNode() {
        return fillSelectedNode;
    }

    /**
     * Determines if unselected nodes should be filled
     * @return {@code true} if unselected nodes should be filled
     */
    public boolean isFillUnselectedNode() {
        return fillUnselectedNode;
    }

    /**
     * Determines if multiply connected nodes should be filled
     * @return {@code true} if multiply connected nodes should be filled
     */
    public boolean isFillConnectionNode() {
        return fillConnectionNode;
    }

    /**
     * Determines if tagged nodes should be filled
     * @return {@code true} if tagged nodes should be filled
     */
    public boolean isFillTaggedNode() {
        return fillTaggedNode;
    }

    /**
     * Determines if only the data area outline should be drawn
     * @return {@code true} if only the data area outline should be drawn
     */
    public boolean isOutlineOnly() {
        return outlineOnly;
    }

    @Override
    public String toString() {
        // This is used for debugging exceptions.
        return "MapPaintSettings [useRealWidth=" + useRealWidth + ", showDirectionArrow=" + showDirectionArrow
                + ", showOnewayArrow=" + showOnewayArrow + ", defaultSegmentWidth=" + defaultSegmentWidth
                + ", showOrderNumber=" + showOrderNumber + ", showHeadArrowOnly=" + showHeadArrowOnly
                + ", showNamesDistance=" + showNamesDistance + ", useStrokesDistance=" + useStrokesDistance
                + ", showIconsDistance=" + showIconsDistance + ", selectedNodeSize=" + selectedNodeSize
                + ", connectionNodeSize=" + connectionNodeSize + ", unselectedNodeSize=" + unselectedNodeSize
                + ", taggedNodeSize=" + taggedNodeSize + ", fillSelectedNode=" + fillSelectedNode
                + ", fillUnselectedNode=" + fillUnselectedNode + ", fillTaggedNode=" + fillTaggedNode
                + ", fillConnectionNode=" + fillConnectionNode + ", outlineOnly=" + outlineOnly + ", selectedColor="
                + selectedColor + ", relationSelectedColor=" + relationSelectedColor + ", highlightColor="
                + highlightColor + ", inactiveColor=" + inactiveColor + ", nodeColor=" + nodeColor + ", taggedColor="
                + taggedColor + ", connectionColor=" + connectionColor + ", taggedConnectionColor="
                + taggedConnectionColor + "]";
    }

}
