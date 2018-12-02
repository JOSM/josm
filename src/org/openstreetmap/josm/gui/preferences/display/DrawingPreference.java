// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.autofilter.AutoFilterManager;
import org.openstreetmap.josm.gui.autofilter.AutoFilterRule;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

/**
 * Map drawing preferences.
 */
public class DrawingPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code DrawingPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new DrawingPreference();
        }
    }

    private GPXSettingsPanel gpxPanel;
    private final JCheckBox directionHint = new JCheckBox(tr("Draw Direction Arrows"));
    private final JCheckBox headArrow = new JCheckBox(tr("Only on the head of a way."));
    private final JCheckBox onewayArrow = new JCheckBox(tr("Draw oneway arrows."));
    private final JCheckBox segmentOrderNumber = new JCheckBox(tr("Draw segment order numbers"));
    private final JCheckBox segmentOrderNumberOnSelectedWay = new JCheckBox(tr("Draw segment order numbers on selected way"));
    private final JCheckBox sourceBounds = new JCheckBox(tr("Draw boundaries of downloaded data"));
    private final JCheckBox virtualNodes = new JCheckBox(tr("Draw virtual nodes in select mode"));
    private final JCheckBox inactive = new JCheckBox(tr("Draw inactive layers in other color"));
    private final JCheckBox discardableKeys = new JCheckBox(tr("Display discardable keys"));
    private final JCheckBox autoFilters = new JCheckBox(tr("Use auto filters"));
    private final JLabel lblRule = new JLabel(tr("Rule"));
    private final JosmComboBox<AutoFilterRule> autoFilterRules = new JosmComboBox<>(
            AutoFilterManager.getInstance().getAutoFilterRules().toArray(new AutoFilterRule[] {}));

    // Options that affect performance
    private final JCheckBox useHighlighting = new JCheckBox(tr("Highlight target ways and nodes"));
    private final JCheckBox drawHelperLine = new JCheckBox(tr("Draw rubber-band helper line"));
    private final JCheckBox useAntialiasing = new JCheckBox(tr("Smooth map graphics (antialiasing)"));
    private final JCheckBox useWireframeAntialiasing = new JCheckBox(tr("Smooth map graphics in wireframe mode (antialiasing)"));
    private final JCheckBox outlineOnly = new JCheckBox(tr("Draw only outlines of areas"));
    private final JCheckBox hideLabelsWhileDragging = new JCheckBox(tr("Hide labels while dragging the map"));

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gpxPanel = new GPXSettingsPanel();
        gui.addValidationListener(gpxPanel);
        JPanel panel = gpxPanel;

        JScrollPane scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        GuiHelper.setDefaultIncrement(scrollpane);
        gui.getDisplayPreference().addSubTab(this, tr("GPS Points"), scrollpane);
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // directionHint
        directionHint.addActionListener(e -> {
            if (directionHint.isSelected()) {
                headArrow.setSelected(Config.getPref().getBoolean("draw.segment.head_only", false));
            } else {
                headArrow.setSelected(false);
            }
            headArrow.setEnabled(directionHint.isSelected());
        });
        directionHint.setToolTipText(tr("Draw direction hints for way segments."));
        directionHint.setSelected(Config.getPref().getBoolean("draw.segment.direction", false));

        // only on the head of a way
        headArrow.setToolTipText(tr("Only on the head of a way."));
        headArrow.setSelected(Config.getPref().getBoolean("draw.segment.head_only", false));
        headArrow.setEnabled(directionHint.isSelected());

        // draw oneway arrows
        onewayArrow.setToolTipText(tr("Draw arrows in the direction of oneways and other directed features."));
        onewayArrow.setSelected(Config.getPref().getBoolean("draw.oneway", true));

        // segment order number
        segmentOrderNumber.setToolTipText(tr("Draw the order numbers of all segments within their way."));
        segmentOrderNumber.setSelected(Config.getPref().getBoolean("draw.segment.order_number", false));
        segmentOrderNumberOnSelectedWay.setToolTipText(tr("Draw the order numbers of all segments within their way."));
        segmentOrderNumberOnSelectedWay.setSelected(Config.getPref().getBoolean("draw.segment.order_number.on_selected", false));

        // downloaded area
        sourceBounds.setToolTipText(tr("Draw the boundaries of data loaded from the server."));
        sourceBounds.setSelected(Config.getPref().getBoolean("draw.data.downloaded_area", true));

        // virtual nodes
        virtualNodes.setToolTipText(tr("Draw virtual nodes in select mode for easy way modification."));
        virtualNodes.setSelected(Config.getPref().getInt("mappaint.node.virtual-size", 8) != 0);

        // background layers in inactive color
        inactive.setToolTipText(tr("Draw the inactive data layers in a different color."));
        inactive.setSelected(Config.getPref().getBoolean("draw.data.inactive_color", true));

        // antialiasing
        useAntialiasing.setToolTipText(tr("Apply antialiasing to the map view resulting in a smoother appearance."));
        useAntialiasing.setSelected(Config.getPref().getBoolean("mappaint.use-antialiasing", true));

        // wireframe mode antialiasing
        useWireframeAntialiasing.setToolTipText(tr("Apply antialiasing to the map view in wireframe mode resulting in a smoother appearance."));
        useWireframeAntialiasing.setSelected(Config.getPref().getBoolean("mappaint.wireframe.use-antialiasing", false));

        // highlighting
        useHighlighting.setToolTipText(tr("Hightlight target nodes and ways while drawing or selecting"));
        useHighlighting.setSelected(Config.getPref().getBoolean("draw.target-highlight", true));

        drawHelperLine.setToolTipText(tr("Draw rubber-band helper line"));
        drawHelperLine.setSelected(Config.getPref().getBoolean("draw.helper-line", true));

        // outlineOnly
        outlineOnly.setToolTipText(tr("This option suppresses the filling of areas, overriding anything specified in the selected style."));
        outlineOnly.setSelected(Config.getPref().getBoolean("draw.data.area_outline_only", false));

        // hideLabelsWhileDragging
        hideLabelsWhileDragging.setToolTipText(tr("This option hides the textual labels of OSM objects while dragging the map."));
        hideLabelsWhileDragging.setSelected(OsmDataLayer.PROPERTY_HIDE_LABELS_WHILE_DRAGGING.get());

        // discardable keys
        discardableKeys.setToolTipText(tr("Display keys which have been deemed uninteresting to the point that they can be silently removed."));
        discardableKeys.setSelected(Config.getPref().getBoolean("display.discardable-keys", false));

        // auto filters
        autoFilters.setToolTipText(tr("Display buttons to automatically filter numeric values of a predefined tag"));
        autoFilters.setSelected(AutoFilterManager.PROP_AUTO_FILTER_ENABLED.get());
        autoFilters.addActionListener(e -> {
            lblRule.setEnabled(autoFilters.isSelected());
            autoFilterRules.setEnabled(autoFilters.isSelected());
        });
        autoFilterRules.setToolTipText("Rule defining which tag will provide automatic filters, below a certain zoom level");
        autoFilterRules.setSelectedItem(AutoFilterManager.getInstance().getAutoFilterRule(AutoFilterManager.PROP_AUTO_FILTER_RULE.get()));

        JLabel performanceLabel = new JLabel(tr("Options that affect drawing performance"));

        panel.add(new JLabel(tr("Segment drawing options")),
                GBC.eop().insets(5, 10, 0, 0));
        panel.add(directionHint, GBC.eop().insets(20, 0, 0, 0));
        panel.add(headArrow, GBC.eop().insets(40, 0, 0, 0));
        panel.add(onewayArrow, GBC.eop().insets(20, 0, 0, 0));
        panel.add(segmentOrderNumber, GBC.eop().insets(20, 0, 0, 0));
        panel.add(segmentOrderNumberOnSelectedWay, GBC.eop().insets(20, 0, 0, 0));

        panel.add(new JLabel(tr("Select and draw mode options")),
                GBC.eop().insets(5, 10, 0, 0));
        panel.add(virtualNodes, GBC.eop().insets(20, 0, 0, 0));
        panel.add(drawHelperLine, GBC.eop().insets(20, 0, 0, 0));

        panel.add(performanceLabel, GBC.eop().insets(5, 10, 0, 0));
        panel.add(useAntialiasing, GBC.eop().insets(20, 0, 0, 0));
        panel.add(useWireframeAntialiasing, GBC.eop().insets(20, 0, 0, 0));
        panel.add(useHighlighting, GBC.eop().insets(20, 0, 0, 0));
        panel.add(outlineOnly, GBC.eol().insets(20, 0, 0, 0));
        panel.add(hideLabelsWhileDragging, GBC.eol().insets(20, 0, 0, 0));

        panel.add(new JLabel(tr("Other options")),
                GBC.eop().insets(5, 10, 0, 0));
        panel.add(sourceBounds, GBC.eop().insets(20, 0, 0, 0));
        panel.add(inactive, GBC.eop().insets(20, 0, 0, 0));
        panel.add(discardableKeys, GBC.eop().insets(20, 0, 0, 0));
        panel.add(autoFilters, GBC.eop().insets(20, 0, 0, 0));
        panel.add(lblRule, GBC.std().insets(40, 0, 0, 0));
        panel.add(autoFilterRules, GBC.eop().fill(GBC.HORIZONTAL).insets(5, 0, 0, 0));

        ExpertToggleAction.addVisibilitySwitcher(performanceLabel);
        ExpertToggleAction.addVisibilitySwitcher(useAntialiasing);
        ExpertToggleAction.addVisibilitySwitcher(useWireframeAntialiasing);
        ExpertToggleAction.addVisibilitySwitcher(useHighlighting);
        ExpertToggleAction.addVisibilitySwitcher(outlineOnly);
        ExpertToggleAction.addVisibilitySwitcher(hideLabelsWhileDragging);
        ExpertToggleAction.addVisibilitySwitcher(discardableKeys);

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        GuiHelper.setDefaultIncrement(scrollpane);
        gui.getDisplayPreference().addSubTab(this, tr("OSM Data"), scrollpane);
    }

    @Override
    public boolean ok() {
        boolean restart = gpxPanel.savePreferences();
        OsmDataLayer.PROPERTY_HIDE_LABELS_WHILE_DRAGGING.put(hideLabelsWhileDragging.isSelected());
        Config.getPref().putBoolean("draw.data.area_outline_only", outlineOnly.isSelected());
        Config.getPref().putBoolean("draw.segment.direction", directionHint.isSelected());
        Config.getPref().putBoolean("draw.segment.head_only", headArrow.isSelected());
        Config.getPref().putBoolean("draw.oneway", onewayArrow.isSelected());
        Config.getPref().putBoolean("draw.segment.order_number", segmentOrderNumber.isSelected());
        Config.getPref().putBoolean("draw.segment.order_number.on_selected", segmentOrderNumberOnSelectedWay.isSelected());
        Config.getPref().putBoolean("draw.data.downloaded_area", sourceBounds.isSelected());
        Config.getPref().putBoolean("draw.data.inactive_color", inactive.isSelected());
        Config.getPref().putBoolean("mappaint.use-antialiasing", useAntialiasing.isSelected());
        Config.getPref().putBoolean("mappaint.wireframe.use-antialiasing", useWireframeAntialiasing.isSelected());
        Config.getPref().putBoolean("draw.target-highlight", useHighlighting.isSelected());
        Config.getPref().putBoolean("draw.helper-line", drawHelperLine.isSelected());
        Config.getPref().putBoolean("display.discardable-keys", discardableKeys.isSelected());
        AutoFilterManager.PROP_AUTO_FILTER_ENABLED.put(autoFilters.isSelected());
        AutoFilterManager.PROP_AUTO_FILTER_RULE.put(((AutoFilterRule) autoFilterRules.getSelectedItem()).getKey());
        int vn = Config.getPref().getInt("mappaint.node.virtual-size", 8);
        if (virtualNodes.isSelected()) {
            if (vn < 1) {
                vn = 8;
            }
        } else {
            vn = 0;
        }
        Config.getPref().putInt("mappaint.node.virtual-size", vn);
        return restart;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getDisplayPreference();
    }
}
