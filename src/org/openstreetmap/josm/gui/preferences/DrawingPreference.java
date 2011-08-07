// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class DrawingPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
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
    private JCheckBox useAntialiasing = new JCheckBox(tr("Smooth map graphics (antialiasing)"));
    private JCheckBox outlineOnly = new JCheckBox(tr("Draw only outlines of areas"));

    public void addGui(PreferenceTabbedPane gui) {
        gui.display.setPreferredSize(new Dimension(400,600));
        gpxPanel = new GPXSettingsPanel();
        gui.addValidationListener(gpxPanel);
        JPanel panel = gpxPanel;

        JScrollPane scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.displaycontent.addTab(tr("GPS Points"), scrollpane);
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // directionHint
        directionHint.addActionListener(new ActionListener(){
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
        panel.add(directionHint, GBC.eop().insets(20,0,0,0));

        // only on the head of a way
        headArrow.setToolTipText(tr("Only on the head of a way."));
        headArrow.setSelected(Main.pref.getBoolean("draw.segment.head_only", false));
        headArrow.setEnabled(directionHint.isSelected());
        panel.add(headArrow, GBC.eop().insets(40, 0, 0, 0));

        // draw oneway arrows
        onewayArrow.setToolTipText(tr("Draw arrows in the direction of oneways and other directed features."));
        onewayArrow.setSelected(Main.pref.getBoolean("draw.oneway", true));
        panel.add(onewayArrow, GBC.eop().insets(20,0,0,0));

        // segment order number
        segmentOrderNumber.setToolTipText(tr("Draw the order numbers of all segments within their way."));
        segmentOrderNumber.setSelected(Main.pref.getBoolean("draw.segment.order_number", false));
        panel.add(segmentOrderNumber, GBC.eop().insets(20,0,0,0));

        // antialiasing
        useAntialiasing.setToolTipText(tr("Apply antialiasing to the map view resulting in a smoother appearance."));
        useAntialiasing.setSelected(Main.pref.getBoolean("mappaint.use-antialiasing", true));
        panel.add(useAntialiasing, GBC.eop().insets(20,0,0,0));

        // downloaded area
        sourceBounds.setToolTipText(tr("Draw the boundaries of data loaded from the server."));
        sourceBounds.setSelected(Main.pref.getBoolean("draw.data.downloaded_area", true));
        panel.add(sourceBounds, GBC.eop().insets(20,0,0,0));

        // virtual nodes
        virtualNodes.setToolTipText(tr("Draw virtual nodes in select mode for easy way modification."));
        virtualNodes.setSelected(Main.pref.getInteger("mappaint.node.virtual-size", 8) != 0);
        panel.add(virtualNodes, GBC.eop().insets(20,0,0,0));

        // background layers in inactive color
        inactive.setToolTipText(tr("Draw the inactive data layers in a different color."));
        inactive.setSelected(Main.pref.getBoolean("draw.data.inactive_color", true));
        panel.add(inactive, GBC.eop().insets(20,0,0,0));

        // outlineOnly
        outlineOnly.setSelected(Main.pref.getBoolean("draw.data.area_outline_only", false));
        outlineOnly.setToolTipText(tr("This option suppresses the filling of areas, overriding anything specified in the selected style."));
        panel.add(outlineOnly, GBC.eol().insets(20,0,0,5));

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.displaycontent.addTab(tr("OSM Data"), scrollpane);
    }

    public boolean ok() {
        gpxPanel.savePreferences();
        Main.pref.put("draw.data.area_outline_only", outlineOnly.isSelected());
        Main.pref.put("draw.segment.direction", directionHint.isSelected());
        Main.pref.put("draw.segment.head_only", headArrow.isSelected());
        Main.pref.put("draw.oneway", onewayArrow.isSelected());
        Main.pref.put("draw.segment.order_number", segmentOrderNumber.isSelected());
        Main.pref.put("draw.data.downloaded_area", sourceBounds.isSelected());
        Main.pref.put("draw.data.inactive_color", inactive.isSelected());
        Main.pref.put("mappaint.use-antialiasing", useAntialiasing.isSelected());
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
        return false;
    }
}
