// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
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
    private JCheckBox directionHint = new JCheckBox(tr("Draw Direction Arrows"));
    private JCheckBox headArrow = new JCheckBox(tr("Only on the head of a way."));
    private JCheckBox onewayArrow = new JCheckBox(tr("Draw oneway arrows."));
    private JCheckBox segmentOrderNumber = new JCheckBox(tr("Draw segment order numbers"));
    private JCheckBox sourceBounds = new JCheckBox(tr("Draw boundaries of downloaded data"));
    private JCheckBox virtualNodes = new JCheckBox(tr("Draw virtual nodes in select mode"));
    private JCheckBox inactive = new JCheckBox(tr("Draw inactive layers in other color"));
    private JCheckBox discardableKeys = new JCheckBox(tr("Display discardable keys"));

    // Options that affect performance
    private JCheckBox useHighlighting = new JCheckBox(tr("Highlight target ways and nodes"));
    private JCheckBox drawHelperLine = new JCheckBox(tr("Draw rubber-band helper line"));
    private JCheckBox useAntialiasing = new JCheckBox(tr("Smooth map graphics (antialiasing)"));
    private JCheckBox useWireframeAntialiasing = new JCheckBox(tr("Smooth map graphics in wireframe mode (antialiasing)"));
    private JCheckBox outlineOnly = new JCheckBox(tr("Draw only outlines of areas"));

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gpxPanel = new GPXSettingsPanel();
        gui.addValidationListener(gpxPanel);
        JPanel panel = gpxPanel;

        JScrollPane scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.getDisplayPreference().addSubTab(this, tr("GPS Points"), scrollpane);
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // directionHint
        directionHint.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (directionHint.isSelected()){
                    headArrow.setSelected(Main.pref.getBoolean("draw.segment.head_only", false));
                }else{
                    headArrow.setSelected(false);
                }
                headArrow.setEnabled(directionHint.isSelected());
            }
        });
        directionHint.setToolTipText(tr("Draw direction hints for way segments."));
        directionHint.setSelected(Main.pref.getBoolean("draw.segment.direction", false));

        // only on the head of a way
        headArrow.setToolTipText(tr("Only on the head of a way."));
        headArrow.setSelected(Main.pref.getBoolean("draw.segment.head_only", false));
        headArrow.setEnabled(directionHint.isSelected());

        // draw oneway arrows
        onewayArrow.setToolTipText(tr("Draw arrows in the direction of oneways and other directed features."));
        onewayArrow.setSelected(Main.pref.getBoolean("draw.oneway", true));

        // segment order number
        segmentOrderNumber.setToolTipText(tr("Draw the order numbers of all segments within their way."));
        segmentOrderNumber.setSelected(Main.pref.getBoolean("draw.segment.order_number", false));

        // downloaded area
        sourceBounds.setToolTipText(tr("Draw the boundaries of data loaded from the server."));
        sourceBounds.setSelected(Main.pref.getBoolean("draw.data.downloaded_area", true));

        // virtual nodes
        virtualNodes.setToolTipText(tr("Draw virtual nodes in select mode for easy way modification."));
        virtualNodes.setSelected(Main.pref.getInteger("mappaint.node.virtual-size", 8) != 0);

        // background layers in inactive color
        inactive.setToolTipText(tr("Draw the inactive data layers in a different color."));
        inactive.setSelected(Main.pref.getBoolean("draw.data.inactive_color", true));

        // antialiasing
        useAntialiasing.setToolTipText(tr("Apply antialiasing to the map view resulting in a smoother appearance."));
        useAntialiasing.setSelected(Main.pref.getBoolean("mappaint.use-antialiasing", true));

        // wireframe mode antialiasing
        useWireframeAntialiasing.setToolTipText(tr("Apply antialiasing to the map view in wireframe mode resulting in a smoother appearance."));
        useWireframeAntialiasing.setSelected(Main.pref.getBoolean("mappaint.wireframe.use-antialiasing", false));

        // highlighting
        useHighlighting.setToolTipText(tr("Hightlight target nodes and ways while drawing or selecting"));
        useHighlighting.setSelected(Main.pref.getBoolean("draw.target-highlight", true));

        drawHelperLine.setToolTipText(tr("Draw rubber-band helper line"));
        drawHelperLine.setSelected(Main.pref.getBoolean("draw.helper-line", true));

        // outlineOnly
        outlineOnly.setToolTipText(tr("This option suppresses the filling of areas, overriding anything specified in the selected style."));
        outlineOnly.setSelected(Main.pref.getBoolean("draw.data.area_outline_only", false));

        // discardable keys
        discardableKeys.setToolTipText(tr("Display keys which have been deemed uninteresting to the point that they can be silently removed."));
        discardableKeys.setSelected(Main.pref.getBoolean("display.discardable-keys", false));

        JLabel performanceLabel = new JLabel(tr("Options that affect drawing performance"));

        panel.add(new JLabel(tr("Segment drawing options")),
                GBC.eop().insets(5,10,0,0));
        panel.add(directionHint, GBC.eop().insets(20,0,0,0));
        panel.add(headArrow, GBC.eop().insets(40, 0, 0, 0));
        panel.add(onewayArrow, GBC.eop().insets(20,0,0,0));
        panel.add(segmentOrderNumber, GBC.eop().insets(20,0,0,0));

        panel.add(new JLabel(tr("Select and draw mode options")),
                GBC.eop().insets(5,10,0,0));
        panel.add(virtualNodes, GBC.eop().insets(20,0,0,0));
        panel.add(drawHelperLine, GBC.eop().insets(20, 0, 0, 0));

        panel.add(performanceLabel, GBC.eop().insets(5,10,0,0));
        panel.add(useAntialiasing, GBC.eop().insets(20,0,0,0));
        panel.add(useWireframeAntialiasing, GBC.eop().insets(20, 0, 0, 0));
        panel.add(useHighlighting, GBC.eop().insets(20,0,0,0));
        panel.add(outlineOnly, GBC.eol().insets(20,0,0,0));

        panel.add(new JLabel(tr("Other options")),
                GBC.eop().insets(5,10,0,0));
        panel.add(sourceBounds, GBC.eop().insets(20,0,0,0));
        panel.add(inactive, GBC.eop().insets(20,0,0,0));
        panel.add(discardableKeys, GBC.eop().insets(20,0,0,0));

        ExpertToggleAction.addVisibilitySwitcher(performanceLabel);
        ExpertToggleAction.addVisibilitySwitcher(useAntialiasing);
        ExpertToggleAction.addVisibilitySwitcher(useWireframeAntialiasing);
        ExpertToggleAction.addVisibilitySwitcher(useHighlighting);
        ExpertToggleAction.addVisibilitySwitcher(outlineOnly);
        ExpertToggleAction.addVisibilitySwitcher(discardableKeys);

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.getDisplayPreference().addSubTab(this, tr("OSM Data"), scrollpane);
    }

    @Override
    public boolean ok() {
        boolean restart = gpxPanel.savePreferences();
        Main.pref.put("draw.data.area_outline_only", outlineOnly.isSelected());
        Main.pref.put("draw.segment.direction", directionHint.isSelected());
        Main.pref.put("draw.segment.head_only", headArrow.isSelected());
        Main.pref.put("draw.oneway", onewayArrow.isSelected());
        Main.pref.put("draw.segment.order_number", segmentOrderNumber.isSelected());
        Main.pref.put("draw.data.downloaded_area", sourceBounds.isSelected());
        Main.pref.put("draw.data.inactive_color", inactive.isSelected());
        Main.pref.put("mappaint.use-antialiasing", useAntialiasing.isSelected());
        Main.pref.put("mappaint.wireframe.use-antialiasing", useWireframeAntialiasing.isSelected());
        Main.pref.put("draw.target-highlight", useHighlighting.isSelected());
        Main.pref.put("draw.helper-line", drawHelperLine.isSelected());
        Main.pref.put("display.discardable-keys", discardableKeys.isSelected());
        int vn = Main.pref.getInteger("mappaint.node.virtual-size", 8);
        if (virtualNodes.isSelected()) {
            if (vn < 1) {
                vn = 8;
            }
        }
        else {
            vn = 0;
        }
        Main.pref.putInteger("mappaint.node.virtual-size", vn);
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
