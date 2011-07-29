package org.openstreetmap.josm.gui.preferences;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

public class GPXSettingsPanel extends JPanel {
    private JRadioButton drawRawGpsLinesGlobal = new JRadioButton(tr("Use global settings."));
    private JRadioButton drawRawGpsLinesAll = new JRadioButton(tr("All"));
    private JRadioButton drawRawGpsLinesLocal = new JRadioButton(tr("Local files"));
    private JRadioButton drawRawGpsLinesNone = new JRadioButton(tr("None"));
    private ActionListener drawRawGpsLinesActionListener;
    private JTextField drawRawGpsMaxLineLength = new JTextField(8);
    private JTextField drawRawGpsMaxLineLengthLocal = new JTextField(8);
    private JCheckBox forceRawGpsLines = new JCheckBox(tr("Force lines if no segments imported."));
    private JCheckBox largeGpsPoints = new JCheckBox(tr("Draw large GPS points."));
    private JCheckBox hdopCircleGpsPoints = new JCheckBox(tr("Draw a circle form HDOP value."));
    private ButtonGroup colorGroup;
    private JRadioButton colorTypeVelocity = new JRadioButton(tr("Velocity (red = slow, green = fast)"));
    private JRadioButton colorTypeDirection = new JRadioButton(tr("Direction (red = west, yellow = north, green = east, blue = south)"));
    private JRadioButton colorTypeDilution = new JRadioButton(tr("Dilution of Position (red = high, green = low, if available)"));
    private JRadioButton colorTypeTime = new JRadioButton(tr("Track date"));
    private JRadioButton colorTypeNone = new JRadioButton(tr("Single Color (can be customized for named layers)"));
    private JRadioButton colorTypeGlobal  = new JRadioButton(tr("Use global settings."));
    private JComboBox colorTypeVelocityTune = new JComboBox(new String[] {tr("Car"), tr("Bicycle"), tr("Foot")});
    private JCheckBox makeAutoMarkers = new JCheckBox(tr("Create markers when reading GPX."));
    private JCheckBox drawGpsArrows = new JCheckBox(tr("Draw Direction Arrows"));
    private JCheckBox drawGpsArrowsFast = new JCheckBox(tr("Fast drawing (looks uglier)"));
    private JTextField drawGpsArrowsMinDist = new JTextField(8);
    private JCheckBox colorDynamic = new JCheckBox(tr("Dynamic color range based on data limits"));
    private JComboBox waypointLabel = new JComboBox(new String[] {tr("Auto"), /* gpx data field name */ trc("gpx_field", "Name"),
            /* gpx data field name */ trc("gpx_field", "Desc(ription)"), tr("Both"), tr("None")});
    private String layerName;
    private boolean local; // flag to display LocalOnly checkbox 
    private boolean nonlocal; // flag to display AllLines checkbox 

    public GPXSettingsPanel(String layerName, boolean local, boolean nonlocal) {
        super(new GridBagLayout());
        this.local=local; this.nonlocal=nonlocal;
        this.layerName = "layer "+layerName;
        initComponents();
        loadPreferences();
    }

    public GPXSettingsPanel() {
        super(new GridBagLayout());
        initComponents();
        local=false; nonlocal=false;
        loadPreferences(); // preferences -> controls
    }

    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // makeAutoMarkers
        makeAutoMarkers.setToolTipText(tr("Automatically make a marker layer from any waypoints when opening a GPX layer."));
        add(makeAutoMarkers, GBC.eol().insets(20,0,0,5));

        // drawRawGpsLines
        ButtonGroup gpsLinesGroup = new ButtonGroup();
        if (layerName!=null) gpsLinesGroup.add(drawRawGpsLinesGlobal);
        gpsLinesGroup.add(drawRawGpsLinesNone);
        gpsLinesGroup.add(drawRawGpsLinesLocal);
        gpsLinesGroup.add(drawRawGpsLinesAll);

        /* ensure that default is in data base */

        add(new JLabel(tr("Draw lines between raw GPS points")), GBC.eol().insets(20,0,0,0));
        if (layerName!=null) add(drawRawGpsLinesGlobal, GBC.eol().insets(40,0,0,0));
        add(drawRawGpsLinesNone, GBC.eol().insets(40,0,0,0));
        if (layerName==null || local) add(drawRawGpsLinesLocal, GBC.eol().insets(40,0,0,0)); 
        if (layerName==null || nonlocal) add(drawRawGpsLinesAll, GBC.eol().insets(40,0,0,0)); 

        drawRawGpsLinesActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                boolean f=drawRawGpsLinesNone.isSelected()||drawRawGpsLinesGlobal.isSelected();
                forceRawGpsLines.setEnabled(!f);
                drawRawGpsMaxLineLength.setEnabled(!(f || drawRawGpsLinesLocal.isSelected()));
                drawRawGpsMaxLineLengthLocal.setEnabled(!f);
                drawGpsArrows.setEnabled(!f);
                drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
            }
        };

        drawRawGpsLinesGlobal.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesNone.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesLocal.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesAll.addActionListener(drawRawGpsLinesActionListener);

        // drawRawGpsMaxLineLengthLocal
        drawRawGpsMaxLineLengthLocal.setToolTipText(tr("Maximum length (in meters) to draw lines for local files. Set to ''-1'' to draw all lines."));
        add(new JLabel(tr("Maximum length for local files (meters)")), GBC.std().insets(40,0,0,0));
        add(drawRawGpsMaxLineLengthLocal, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // drawRawGpsMaxLineLength
        drawRawGpsMaxLineLength.setToolTipText(tr("Maximum length (in meters) to draw lines. Set to ''-1'' to draw all lines."));
        add(new JLabel(tr("Maximum length (meters)")), GBC.std().insets(40,0,0,0));
        add(drawRawGpsMaxLineLength, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // forceRawGpsLines
        forceRawGpsLines.setToolTipText(tr("Force drawing of lines if the imported data contain no line information."));
        add(forceRawGpsLines, GBC.eop().insets(40,0,0,0));

        // drawGpsArrows
        drawGpsArrows.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
            }
        });
        drawGpsArrows.setToolTipText(tr("Draw direction arrows for lines, connecting GPS points."));
        add(drawGpsArrows, GBC.eop().insets(40,0,0,0));

        // drawGpsArrowsFast
        drawGpsArrowsFast.setToolTipText(tr("Draw the direction arrows using table lookups instead of complex math."));
        add(drawGpsArrowsFast, GBC.eop().insets(60,0,0,0));

        // drawGpsArrowsMinDist
        drawGpsArrowsMinDist.setToolTipText(tr("Do not draw arrows if they are not at least this distance away from the last one."));
        add(new JLabel(tr("Minimum distance (pixels)")), GBC.std().insets(60,0,0,0));
        add(drawGpsArrowsMinDist, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // hdopCircleGpsPoints
        hdopCircleGpsPoints.setToolTipText(tr("Draw a circle form HDOP value."));
        add(hdopCircleGpsPoints, GBC.eop().insets(20,0,0,0));

        // largeGpsPoints
        largeGpsPoints.setToolTipText(tr("Draw larger dots for the GPS points."));
        add(largeGpsPoints, GBC.eop().insets(20,0,0,0));

        // colorTracks
        colorGroup = new ButtonGroup();
        if (layerName!=null) colorGroup.add(colorTypeGlobal);
        colorGroup.add(colorTypeNone);
        colorGroup.add(colorTypeVelocity);
        colorGroup.add(colorTypeDirection);
        colorGroup.add(colorTypeDilution);
        colorGroup.add(colorTypeTime);

        colorTypeVelocity.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                colorTypeVelocityTune.setEnabled(colorTypeVelocity.isSelected());
                colorDynamic.setEnabled(colorTypeVelocity.isSelected() || colorTypeDilution.isSelected());
            }
        });
        colorTypeDilution.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                colorDynamic.setEnabled(colorTypeVelocity.isSelected() || colorTypeDilution.isSelected());
            }
        });

        colorTypeNone.setToolTipText(tr("All points and track segments will have the same color. Can be customized in Layer Manager."));
        colorTypeVelocity.setToolTipText(tr("Colors points and track segments by velocity."));
        colorTypeDirection.setToolTipText(tr("Colors points and track segments by direction."));
        colorTypeDilution.setToolTipText(tr("Colors points and track segments by dilution of position (HDOP). Your capture device needs to log that information."));
        colorTypeTime.setToolTipText(tr("Colors points and track segments by its timestamp."));

        // color Tracks by Velocity Tune
        colorTypeVelocityTune.setToolTipText(tr("Allows to tune the track coloring for different average speeds."));

        add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));

        add(new JLabel(tr("Track and Point Coloring")), GBC.eol().insets(20,0,0,0));
        if (layerName!=null) add(colorTypeGlobal, GBC.eol().insets(40,0,0,0));
        add(colorTypeNone, GBC.eol().insets(40,0,0,0));
        add(colorTypeVelocity, GBC.std().insets(40,0,0,0));
        add(colorTypeVelocityTune, GBC.eop().insets(5,0,0,5));
        add(colorTypeDirection, GBC.eol().insets(40,0,0,0));
        add(colorTypeDilution, GBC.eol().insets(40,0,0,0));
        add(colorTypeTime, GBC.eol().insets(40,0,0,0));

        colorDynamic.setToolTipText(tr("Colors points and track segments by data limits."));
        add(colorDynamic, GBC.eop().insets(40,0,0,0));

        // waypointLabel
        add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));
        add(new JLabel(tr("Waypoint labelling")), GBC.std().insets(20,0,0,0));
        if(layerName!= null)
            waypointLabel.addItem(tr("Global settings"));
        add(waypointLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
        add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
    }

    /**
     * Loads preferences to UI controls
     */
    public void loadPreferences () {
        makeAutoMarkers.setSelected(Main.pref.getBoolean("marker.makeautomarkers", true));
        if(layerName!=null && !Main.pref.hasKey("draw.rawgps.lines."+layerName)
                && !Main.pref.hasKey("draw.rawgps.lines.local."+layerName)){
            // no line preferences for layer is found
            drawRawGpsLinesGlobal.setSelected(true);
        } else {
            Boolean lf = Main.pref.getBoolean("draw.rawgps.lines.local",layerName, true);
            if(Main.pref.getBoolean("draw.rawgps.lines",layerName, true)) {
                drawRawGpsLinesAll.setSelected(true);
            } else if (lf) {
                drawRawGpsLinesLocal.setSelected(true);
            } else {
                drawRawGpsLinesNone.setSelected(true);
            }
        }

        drawRawGpsMaxLineLengthLocal.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.max-line-length.local",layerName, -1)));
        drawRawGpsMaxLineLength.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.max-line-length",layerName, 200)));
        forceRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines.force",layerName, false));
        drawGpsArrows.setSelected(Main.pref.getBoolean("draw.rawgps.direction",layerName, false));
        drawGpsArrowsFast.setSelected(Main.pref.getBoolean("draw.rawgps.alternatedirection",layerName, false));
        drawGpsArrowsMinDist.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.min-arrow-distance",layerName, 40)));
        hdopCircleGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.hdopcircle",layerName, false));
        largeGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.large",layerName, false));
        drawRawGpsLinesActionListener.actionPerformed(null);

        if(layerName!=null && !Main.pref.hasKey("draw.rawgps.colors."+layerName)) {
            colorTypeGlobal.setSelected(true);
            colorDynamic.setSelected(false);
            colorDynamic.setEnabled(false);
        } else {
         switch(Main.pref.getInteger("draw.rawgps.colors",layerName, 0)) {
            case 0: colorTypeNone.setSelected(true);   break;
            case 1: colorTypeVelocity.setSelected(true);  break;
            case 2: colorTypeDilution.setSelected(true);  break;
            case 3: colorTypeDirection.setSelected(true); break;
            case 4: colorTypeTime.setSelected(true);  break;
            }
            int ccts = Main.pref.getInteger("draw.rawgps.colorTracksTune",layerName, 45);
            colorTypeVelocityTune.setSelectedIndex(ccts==10 ? 2 : (ccts==20 ? 1 : 0));
            colorTypeVelocityTune.setEnabled(colorTypeVelocity.isSelected() && colorTypeVelocity.isEnabled());
            colorDynamic.setSelected(Main.pref.getBoolean("draw.rawgps.colors.dynamic",layerName, false));
            colorDynamic.setEnabled(colorTypeVelocity.isSelected() || colorTypeDilution.isSelected());
        }
        if(layerName != null)
          waypointLabel.setSelectedIndex(Main.pref.getInteger("draw.rawgps.layer.wpt."+layerName, 5));
        else
          waypointLabel.setSelectedIndex(Main.pref.getInteger("draw.rawgps.layer.wpt", 0));
    }


    /**
     * Save preferences from UI controls for specified layer
     * if layerName==null, global preferences are written
     */
    public boolean savePreferences (String layerName, boolean locLayer) {
        String layerNameDot = ".layer "+layerName;
        if (layerName==null) layerNameDot="";
        Main.pref.put("marker.makeautomarkers"+layerNameDot, makeAutoMarkers.isSelected());
        if (drawRawGpsLinesGlobal.isSelected()) {
            Main.pref.put("draw.rawgps.lines" + layerNameDot, null);
            Main.pref.put("draw.rawgps.max-line-length" + layerNameDot, null);
            Main.pref.put("draw.rawgps.lines.local" + layerNameDot, null);
            Main.pref.put("draw.rawgps.max-line-length.local" + layerNameDot, null);
            Main.pref.put("draw.rawgps.lines.force"+layerNameDot, null);
            Main.pref.put("draw.rawgps.direction"+layerNameDot, null);
            Main.pref.put("draw.rawgps.alternatedirection"+layerNameDot, null);
            Main.pref.put("draw.rawgps.min-arrow-distance"+layerNameDot, null);
        } else {
            if (layerName==null || !locLayer) {
                Main.pref.put("draw.rawgps.lines" +  layerNameDot, drawRawGpsLinesAll.isSelected());
                Main.pref.put("draw.rawgps.max-line-length" + layerNameDot, drawRawGpsMaxLineLength.getText());
            }
            if (layerName==null || locLayer) {
                Main.pref.put("draw.rawgps.lines.local" + layerNameDot, drawRawGpsLinesAll.isSelected() || drawRawGpsLinesLocal.isSelected());
                Main.pref.put("draw.rawgps.max-line-length.local" + layerNameDot, drawRawGpsMaxLineLengthLocal.getText());
            }
            Main.pref.put("draw.rawgps.lines.force"+layerNameDot, forceRawGpsLines.isSelected());
            Main.pref.put("draw.rawgps.direction"+layerNameDot, drawGpsArrows.isSelected());
            Main.pref.put("draw.rawgps.alternatedirection"+layerNameDot, drawGpsArrowsFast.isSelected());
            Main.pref.put("draw.rawgps.min-arrow-distance"+layerNameDot, drawGpsArrowsMinDist.getText());
        }

        Main.pref.put("draw.rawgps.hdopcircle"+layerNameDot, hdopCircleGpsPoints.isSelected());
        Main.pref.put("draw.rawgps.large"+layerNameDot, largeGpsPoints.isSelected());
        if (waypointLabel.getSelectedIndex()==5) Main.pref.put("draw.rawgps.layer.wpt"+layerNameDot,null);
        else Main.pref.putInteger("draw.rawgps.layer.wpt"+layerNameDot, waypointLabel.getSelectedIndex());

        if(colorTypeGlobal.isSelected()) {
            Main.pref.put("draw.rawgps.colors"+layerNameDot, null);
            Main.pref.put("draw.rawgps.colors.dynamic"+layerNameDot, null);
            Main.pref.put("draw.rawgps.colorTracksTunec"+layerNameDot, null);
            return false;
        } else if(colorTypeVelocity.isSelected()) {
            Main.pref.putInteger("draw.rawgps.colors"+layerNameDot, 1);
        } else if(colorTypeDilution.isSelected()) {
            Main.pref.putInteger("draw.rawgps.colors"+layerNameDot, 2);
        } else if(colorTypeDirection.isSelected()) {
            Main.pref.putInteger("draw.rawgps.colors"+layerNameDot, 3);
        } else if(colorTypeTime.isSelected()) {
            Main.pref.putInteger("draw.rawgps.colors"+layerNameDot, 4);
        } else {
            Main.pref.putInteger("draw.rawgps.colors"+layerNameDot, 0);
        }
        Main.pref.put("draw.rawgps.colors.dynamic"+layerNameDot, colorDynamic.isSelected());
        int ccti=colorTypeVelocityTune.getSelectedIndex();
        Main.pref.putInteger("draw.rawgps.colorTracksTune"+layerNameDot, ccti==2 ? 10 : (ccti==1 ? 20 : 45));
        return false;
    }

    /**
     * Save preferences from UI controls for initial layer or globally
     */
    public void savePreferences() {
        savePreferences(null, false);
    }
}
