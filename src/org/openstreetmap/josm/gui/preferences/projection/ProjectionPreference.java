// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.GBC;

/**
 * Projection preferences.
 *
 * How to add new Projections:
 *  - Find EPSG code for the projection.
 *  - Look up the parameter string for Proj4, e.g. on http://spatialreference.org/
 *      and add it to the file 'data/projection/epsg' in JOSM trunk
 *  - Search for official references and verify the parameter values. These
 *      documents are often available in the local language only.
 *  - Use {@link #registerProjectionChoice}, to make the entry known to JOSM.
 *
 * In case there is no EPSG code:
 *  - override {@link AbstractProjectionChoice#getProjection()} and provide
 *    a manual implementation of the projection. Use {@link CustomProjection}
 *    if possible.
 */
public class ProjectionPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code ProjectionPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ProjectionPreference();
        }
    }

    private static List<ProjectionChoice> projectionChoices = new ArrayList<ProjectionChoice>();
    private static Map<String, ProjectionChoice> projectionChoicesById = new HashMap<String, ProjectionChoice>();

    // some ProjectionChoices that are referenced from other parts of the code
    public static final ProjectionChoice wgs84, mercator, lambert, utm_france_dom, lambert_cc9;

    static {

        /************************
         * Global projections.
         */

        /**
         * WGS84: Directly use latitude / longitude values as x/y.
         */
        wgs84 = registerProjectionChoice(tr("WGS84 Geographic"), "core:wgs84", 4326, "epsg4326");

        /**
         * Mercator Projection.
         *
         * The center of the mercator projection is always the 0 grad
         * coordinate.
         *
         * See also USGS Bulletin 1532
         * (http://egsc.usgs.gov/isb/pubs/factsheets/fs08799.html)
         * initially EPSG used 3785 but that has been superseded by 3857,
         * see http://www.epsg-registry.org/
         */
        mercator = registerProjectionChoice(tr("Mercator"), "core:mercator", 3857);

        /**
         * UTM.
         */
        registerProjectionChoice(new UTMProjectionChoice());

        /************************
         * Regional - alphabetical order by country code.
         */

        /**
         * Belgian Lambert 72 projection.
         *
         * As specified by the Belgian IGN in this document:
         * http://www.ngi.be/Common/Lambert2008/Transformation_Geographic_Lambert_FR.pdf
         *
         * @author Don-vip
         */
        registerProjectionChoice(tr("Belgian Lambert 1972"), "core:belgianLambert1972", 31370);     // BE
        /**
         * Belgian Lambert 2008 projection.
         *
         * As specified by the Belgian IGN in this document:
         * http://www.ngi.be/Common/Lambert2008/Transformation_Geographic_Lambert_FR.pdf
         *
         * @author Don-vip
         */
        registerProjectionChoice(tr("Belgian Lambert 2008"), "core:belgianLambert2008", 3812);      // BE

        /**
         * SwissGrid CH1903 / L03, see http://de.wikipedia.org/wiki/Swiss_Grid.
         *
         * Actually, what we have here, is CH1903+ (EPSG:2056), but without
         * the additional false easting of 2000km and false northing 1000 km.
         *
         * To get to CH1903, a shift file is required. So currently, there are errors
         * up to 1.6m (depending on the location).
         */
        registerProjectionChoice(new SwissGridProjectionChoice());                                  // CH

        registerProjectionChoice(new GaussKruegerProjectionChoice());                               // DE

        /**
         * Estonian Coordinate System of 1997.
         *
         * Thanks to Johan Montagnat and its geoconv java converter application
         * (http://www.i3s.unice.fr/~johan/gps/ , published under GPL license)
         * from which some code and constants have been reused here.
         */
        registerProjectionChoice(tr("Lambert Zone (Estonia)"), "core:lambertest", 3301);            // EE

        /**
         * Lambert conic conform 4 zones using the French geodetic system NTF.
         *
         * This newer version uses the grid translation NTF<->RGF93 provided by IGN for a submillimetric accuracy.
         * (RGF93 is the French geodetic system similar to WGS84 but not mathematically equal)
         *
         * Source: http://professionnels.ign.fr/DISPLAY/000/526/700/5267002/transformation.pdf
         * @author Pieren
         */
        registerProjectionChoice(lambert = new LambertProjectionChoice());                          // FR
        /**
         * Lambert 93 projection.
         *
         * As specified by the IGN in this document
         * http://professionnels.ign.fr/DISPLAY/000/526/702/5267026/NTG_87.pdf
         * @author Don-vip
         */
        registerProjectionChoice(tr("Lambert 93 (France)"), "core:lambert93", 2154);                // FR
        /**
         * Lambert Conic Conform 9 Zones projection.
         *
         * As specified by the IGN in this document
         * http://professionnels.ign.fr/DISPLAY/000/526/700/5267002/transformation.pdf
         * @author Pieren
         */
        registerProjectionChoice(lambert_cc9 = new LambertCC9ZonesProjectionChoice());                            // FR
        /**
         * French departements in the Caribbean Sea and Indian Ocean.
         *
         * Using the UTM transvers Mercator projection and specific geodesic settings.
         */
        registerProjectionChoice(utm_france_dom = new UTMFranceDOMProjectionChoice());                            // FR

        /**
         * LKS-92/ Latvia TM projection.
         *
         * Based on data from spatialreference.org.
         * http://spatialreference.org/ref/epsg/3059/
         *
         * @author Viesturs Zarins
         */
        registerProjectionChoice(tr("LKS-92 (Latvia TM)"), "core:tmerclv", 3059);                   // LV

        /**
         * PUWG 1992 and 2000 are the official cordinate systems in Poland.
         *
         * They use the same math as UTM only with different constants.
         *
         * @author steelman
         */
        registerProjectionChoice(new PuwgProjectionChoice());                                       // PL

        /**
         * SWEREF99 13 30 projection. Based on data from spatialreference.org.
         * http://spatialreference.org/ref/epsg/3008/
         *
         * @author Hanno Hecker
         */
        registerProjectionChoice(tr("SWEREF99 13 30 / EPSG:3008 (Sweden)"), "core:sweref99", 3008); // SE

        /************************
         * Projection by Code.
         */
        registerProjectionChoice(new CodeProjectionChoice());

        /************************
         * Custom projection.
         */
        registerProjectionChoice(new CustomProjectionChoice());
    }

    public static void registerProjectionChoice(ProjectionChoice c) {
        projectionChoices.add(c);
        projectionChoicesById.put(c.getId(), c);
    }

    public static ProjectionChoice registerProjectionChoice(String name, String id, Integer epsg, String cacheDir) {
        ProjectionChoice pc = new SingleProjectionChoice(name, id, "EPSG:"+epsg, cacheDir);
        registerProjectionChoice(pc);
        return pc;
    }

    private static ProjectionChoice registerProjectionChoice(String name, String id, Integer epsg) {
        ProjectionChoice pc = new SingleProjectionChoice(name, id, "EPSG:"+epsg);
        registerProjectionChoice(pc);
        return pc;
    }

    public static List<ProjectionChoice> getProjectionChoices() {
        return Collections.unmodifiableList(projectionChoices);
    }

    private static final StringProperty PROP_PROJECTION = new StringProperty("projection", mercator.getId());
    private static final StringProperty PROP_COORDINATES = new StringProperty("coordinates", null);
    private static final CollectionProperty PROP_SUB_PROJECTION = new CollectionProperty("projection.sub", null);
    public static final StringProperty PROP_SYSTEM_OF_MEASUREMENT = new StringProperty("system_of_measurement", "Metric");
    private static final String[] unitsValues = (new ArrayList<String>(NavigatableComponent.SYSTEMS_OF_MEASUREMENT.keySet())).toArray(new String[0]);
    private static final String[] unitsValuesTr = new String[unitsValues.length];
    static {
        for (int i=0; i<unitsValues.length; ++i) {
            unitsValuesTr[i] = tr(unitsValues[i]);
        }
    }

    /**
     * Combobox with all projections available
     */
    private JosmComboBox projectionCombo = new JosmComboBox(projectionChoices.toArray());

    /**
     * Combobox with all coordinate display possibilities
     */
    private JosmComboBox coordinatesCombo = new JosmComboBox(CoordinateFormat.values());

    private JosmComboBox unitsCombo = new JosmComboBox(unitsValuesTr);

    /**
     * This variable holds the JPanel with the projection's preferences. If the
     * selected projection does not implement this, it will be set to an empty
     * Panel.
     */
    private JPanel projSubPrefPanel;
    private JPanel projSubPrefPanelWrapper = new JPanel(new GridBagLayout());

    private JLabel projectionCodeLabel;
    private Component projectionCodeGlue;
    private JLabel projectionCode = new JLabel();
    private JLabel projectionNameLabel;
    private Component projectionNameGlue;
    private JLabel projectionName = new JLabel();
    private JLabel bounds = new JLabel();

    /**
     * This is the panel holding all projection preferences
     */
    private JPanel projPanel = new JPanel(new GridBagLayout());

    /**
     * The GridBagConstraints for the Panel containing the ProjectionSubPrefs.
     * This is required twice in the code, creating it here keeps both occurrences
     * in sync
     */
    static private GBC projSubPrefPanelGBC = GBC.std().fill(GBC.BOTH).weight(1.0, 1.0);

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        ProjectionChoice pc = setupProjectionCombo();

        for (int i = 0; i < coordinatesCombo.getItemCount(); ++i) {
            if (((CoordinateFormat)coordinatesCombo.getItemAt(i)).name().equals(PROP_COORDINATES.get())) {
                coordinatesCombo.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < unitsValues.length; ++i) {
            if (unitsValues[i].equals(PROP_SYSTEM_OF_MEASUREMENT.get())) {
                unitsCombo.setSelectedIndex(i);
                break;
            }
        }

        projPanel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        projPanel.setLayout(new GridBagLayout());
        projPanel.add(new JLabel(tr("Projection method")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(projectionCodeLabel = new JLabel(tr("Projection code")), GBC.std().insets(25,5,0,5));
        projPanel.add(projectionCodeGlue = GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCode, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(projectionNameLabel = new JLabel(tr("Projection name")), GBC.std().insets(25,5,0,5));
        projPanel.add(projectionNameGlue = GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionName, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("Bounds")), GBC.std().insets(25,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(bounds, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(projSubPrefPanelWrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(20,5,5,5));

        projPanel.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0,5,0,10));
        projPanel.add(new JLabel(tr("Display coordinates as")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(coordinatesCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("System of measurement")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(unitsCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(GBC.glue(1,1), GBC.std().fill(GBC.HORIZONTAL).weight(1.0, 1.0));

        JScrollPane scrollpane = new JScrollPane(projPanel);
        gui.getMapPreference().addSubTab(this, tr("Map Projection"), scrollpane);

        selectedProjectionChanged(pc);
    }

    private void updateMeta(ProjectionChoice pc) {
        pc.setPreferences(pc.getPreferences(projSubPrefPanel));
        Projection proj = pc.getProjection();
        projectionCode.setText(proj.toCode());
        projectionName.setText(proj.toString());
        Bounds b = proj.getWorldBoundsLatLon();
        CoordinateFormat cf = CoordinateFormat.getDefaultFormat();
        bounds.setText(b.getMin().lonToString(cf)+", "+b.getMin().latToString(cf)+" : "+b.getMax().lonToString(cf)+", "+b.getMax().latToString(cf));
        boolean showCode = true;
        boolean showName = false;
        if (pc instanceof SubPrefsOptions) {
            showCode = ((SubPrefsOptions) pc).showProjectionCode();
            showName = ((SubPrefsOptions) pc).showProjectionName();
        }
        projectionCodeLabel.setVisible(showCode);
        projectionCodeGlue.setVisible(showCode);
        projectionCode.setVisible(showCode);
        projectionNameLabel.setVisible(showName);
        projectionNameGlue.setVisible(showName);
        projectionName.setVisible(showName);
    }

    @Override
    public boolean ok() {
        ProjectionChoice pc = (ProjectionChoice) projectionCombo.getSelectedItem();

        String id = pc.getId();
        Collection<String> prefs = pc.getPreferences(projSubPrefPanel);

        setProjection(id, prefs);

        if(PROP_COORDINATES.put(((CoordinateFormat)coordinatesCombo.getSelectedItem()).name())) {
            CoordinateFormat.setCoordinateFormat((CoordinateFormat)coordinatesCombo.getSelectedItem());
        }

        int i = unitsCombo.getSelectedIndex();
        NavigatableComponent.setSystemOfMeasurement(unitsValues[i]);

        return false;
    }

    static public void setProjection() {
        setProjection(PROP_PROJECTION.get(), PROP_SUB_PROJECTION.get());
    }

    static public void setProjection(String id, Collection<String> pref) {
        ProjectionChoice pc = projectionChoicesById.get(id);

        if (pc == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The projection {0} could not be activated. Using Mercator", id),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            pref = null;
            pc = mercator;
        }
        id = pc.getId();
        PROP_PROJECTION.put(id);
        PROP_SUB_PROJECTION.put(pref);
        Main.pref.putCollection("projection.sub."+id, pref);
        pc.setPreferences(pref);
        Projection proj = pc.getProjection();
        Main.setProjection(proj);
    }

    /**
     * Handles all the work related to update the projection-specific
     * preferences
     * @param pc the choice class representing user selection
     */
    private void selectedProjectionChanged(final ProjectionChoice pc) {
        // Don't try to update if we're still starting up
        int size = projPanel.getComponentCount();
        if(size < 1)
            return;

        final ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMeta(pc);
            }
        };

        // Replace old panel with new one
        projSubPrefPanelWrapper.removeAll();
        projSubPrefPanel = pc.getPreferencePanel(listener);
        projSubPrefPanelWrapper.add(projSubPrefPanel, projSubPrefPanelGBC);
        projPanel.revalidate();
        projSubPrefPanel.repaint();
        updateMeta(pc);
    }

    /**
     * Sets up projection combobox with default values and action listener
     * @return the choice class for user selection
     */
    private ProjectionChoice setupProjectionCombo() {
        ProjectionChoice pc = null;
        for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
            ProjectionChoice pc1 = (ProjectionChoice) projectionCombo.getItemAt(i);
            pc1.setPreferences(getSubprojectionPreference(pc1));
            if (pc1.getId().equals(PROP_PROJECTION.get())) {
                projectionCombo.setSelectedIndex(i);
                selectedProjectionChanged(pc1);
                pc = pc1;
            }
        }
        // If the ProjectionChoice from the preferences is not available, it
        // should have been set to Mercator at JOSM start.
        if (pc == null)
            throw new RuntimeException("Couldn't find the current projection in the list of available projections!");

        projectionCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProjectionChoice pc = (ProjectionChoice) projectionCombo.getSelectedItem();
                selectedProjectionChanged(pc);
            }
        });
        return pc;
    }

    private Collection<String> getSubprojectionPreference(ProjectionChoice pc) {
        return Main.pref.getCollection("projection.sub."+pc.getId(), null);
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getMapPreference();
    }

    /**
     * Selects the given projection.
     * @param projection The projection to select.
     * @since 5604
     */
    public void selectProjection(ProjectionChoice projection) {
        if (projectionCombo != null && projection != null) {
            projectionCombo.setSelectedItem(projection);
        }
    }
}
