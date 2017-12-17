// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.data.SystemOfMeasurement.ALL_SYSTEMS;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

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

    private static final List<ProjectionChoice> projectionChoices = new ArrayList<>();
    private static final Map<String, ProjectionChoice> projectionChoicesById = new HashMap<>();

    /**
     * WGS84: Directly use latitude / longitude values as x/y.
     */
    public static final ProjectionChoice wgs84 = registerProjectionChoice(tr("WGS84 Geographic"), "core:wgs84", 4326);

    /**
     * Mercator Projection.
     *
     * The center of the mercator projection is always the 0 grad coordinate.
     *
     * See also USGS Bulletin 1532 (http://pubs.usgs.gov/bul/1532/report.pdf)
     * initially EPSG used 3785 but that has been superseded by 3857, see https://www.epsg-registry.org/
     */
    public static final ProjectionChoice mercator = registerProjectionChoice(tr("Mercator"), "core:mercator", 3857);

    /**
     * Lambert conic conform 4 zones using the French geodetic system NTF.
     *
     * This newer version uses the grid translation NTF&lt;-&gt;RGF93 provided by IGN for a submillimetric accuracy.
     * (RGF93 is the French geodetic system similar to WGS84 but not mathematically equal)
     *
     * Source: http://geodesie.ign.fr/contenu/fichiers/Changement_systeme_geodesique.pdf
     */
    public static final ProjectionChoice lambert = new LambertProjectionChoice();

    /**
     * French departements in the Caribbean Sea and Indian Ocean.
     *
     * Using the UTM transvers Mercator projection and specific geodesic settings.
     */
    public static final ProjectionChoice utm_france_dom = new UTMFranceDOMProjectionChoice();

    /**
     * Lambert Conic Conform 9 Zones projection.
     *
     * As specified by the IGN in this document
     * http://geodesie.ign.fr/contenu/fichiers/documentation/rgf93/cc9zones.pdf
     */
    public static final ProjectionChoice lambert_cc9 = new LambertCC9ZonesProjectionChoice();

    static {

        /************************
         * Global projections.
         */

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
         * SwissGrid CH1903 / L03, see https://en.wikipedia.org/wiki/Swiss_coordinate_system.
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
         * (https://www.i3s.unice.fr/~johan/gps/ , published under GPL license)
         * from which some code and constants have been reused here.
         */
        registerProjectionChoice(tr("Lambert Zone (Estonia)"), "core:lambertest", 3301);            // EE

        /**
         * Lambert conic conform 4 zones using the French geodetic system NTF.
         *
         * This newer version uses the grid translation NTF<->RGF93 provided by IGN for a submillimetric accuracy.
         * (RGF93 is the French geodetic system similar to WGS84 but not mathematically equal)
         *
         * Source: http://geodesie.ign.fr/contenu/fichiers/Changement_systeme_geodesique.pdf
         * @author Pieren
         */
        registerProjectionChoice(lambert);                                                          // FR

        /**
         * Lambert 93 projection.
         *
         * As specified by the IGN in this document
         * http://geodesie.ign.fr/contenu/fichiers/documentation/rgf93/Lambert-93.pdf
         * @author Don-vip
         */
        registerProjectionChoice(tr("Lambert 93 (France)"), "core:lambert93", 2154);                // FR

        /**
         * Lambert Conic Conform 9 Zones projection.
         *
         * As specified by the IGN in this document
         * http://geodesie.ign.fr/contenu/fichiers/documentation/rgf93/cc9zones.pdf
         * @author Pieren
         */
        registerProjectionChoice(lambert_cc9);                                                      // FR

        /**
         * French departements in the Caribbean Sea and Indian Ocean.
         *
         * Using the UTM transvers Mercator projection and specific geodesic settings.
         */
        registerProjectionChoice(utm_france_dom);                                                   // FR

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
         * Netherlands RD projection
         *
         * @author vholten
         */
        registerProjectionChoice(tr("Rijksdriehoekscoördinaten (Netherlands)"), "core:dutchrd", 28992); // NL

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
        for (String code : c.allCodes()) {
            Projections.registerProjectionSupplier(code, () -> {
                Collection<String> pref = c.getPreferencesFromCode(code);
                c.setPreferences(pref);
                try {
                    return c.getProjection();
                } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                    Logging.log(Logging.LEVEL_WARN, "Unable to get projection "+code+" with "+c+':', e);
                    return null;
                }
            });
        }
    }

    /**
     * Registers a new projection choice.
     * @param name short name of the projection choice as shown in the GUI
     * @param id short name of the projection choice as shown in the GUI
     * @param epsg the unique numeric EPSG identifier for the projection
     * @param cacheDir unused
     * @return the registered {@link ProjectionChoice}
     * @deprecated use {@link #registerProjectionChoice(String, String, Integer)} instead
     */
    @Deprecated
    public static ProjectionChoice registerProjectionChoice(String name, String id, Integer epsg, String cacheDir) {
        return registerProjectionChoice(name, id, epsg);
    }

    /**
     * Registers a new projection choice.
     * @param name short name of the projection choice as shown in the GUI
     * @param id short name of the projection choice as shown in the GUI
     * @param epsg the unique numeric EPSG identifier for the projection
     * @return the registered {@link ProjectionChoice}
     */
    private static ProjectionChoice registerProjectionChoice(String name, String id, Integer epsg) {
        ProjectionChoice pc = new SingleProjectionChoice(name, id, "EPSG:"+epsg);
        registerProjectionChoice(pc);
        return pc;
    }

    public static List<ProjectionChoice> getProjectionChoices() {
        return Collections.unmodifiableList(projectionChoices);
    }

    private static String projectionChoice;

    private static final StringProperty PROP_PROJECTION_DEFAULT = new StringProperty("projection.default", mercator.getId());
    private static final StringProperty PROP_COORDINATES = new StringProperty("coordinates", null);
    private static final ListProperty PROP_SUB_PROJECTION_DEFAULT = new ListProperty("projection.default.sub", null);
    private static final String[] unitsValues = ALL_SYSTEMS.keySet().toArray(new String[ALL_SYSTEMS.size()]);
    private static final String[] unitsValuesTr = new String[unitsValues.length];
    static {
        for (int i = 0; i < unitsValues.length; ++i) {
            unitsValuesTr[i] = tr(unitsValues[i]);
        }
    }

    /**
     * Combobox with all projections available
     */
    private final JosmComboBox<ProjectionChoice> projectionCombo;

    /**
     * Combobox with all coordinate display possibilities
     */
    private final JosmComboBox<ICoordinateFormat> coordinatesCombo;

    private final JosmComboBox<String> unitsCombo = new JosmComboBox<>(unitsValuesTr);

    /**
     * This variable holds the JPanel with the projection's preferences. If the
     * selected projection does not implement this, it will be set to an empty
     * Panel.
     */
    private JPanel projSubPrefPanel;
    private final JPanel projSubPrefPanelWrapper = new JPanel(new GridBagLayout());

    private final JLabel projectionCodeLabel = new JLabel(tr("Projection code"));
    private final Component projectionCodeGlue = GBC.glue(5, 0);
    private final JLabel projectionCode = new JLabel();
    private final JLabel projectionNameLabel = new JLabel(tr("Projection name"));
    private final Component projectionNameGlue = GBC.glue(5, 0);
    private final JLabel projectionName = new JLabel();
    private final JLabel bounds = new JLabel();

    /**
     * This is the panel holding all projection preferences
     */
    private final VerticallyScrollablePanel projPanel = new VerticallyScrollablePanel(new GridBagLayout());

    /**
     * The GridBagConstraints for the Panel containing the ProjectionSubPrefs.
     * This is required twice in the code, creating it here keeps both occurrences
     * in sync
     */
    private static final GBC projSubPrefPanelGBC = GBC.std().fill(GBC.BOTH).weight(1.0, 1.0);

    public ProjectionPreference() {
        this.projectionCombo = new JosmComboBox<>(
            projectionChoices.toArray(new ProjectionChoice[0]));
        this.coordinatesCombo = new JosmComboBox<>(
                CoordinateFormatManager.getCoordinateFormats().toArray(new ICoordinateFormat[0]));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        final ProjectionChoice pc = setupProjectionCombo();

        for (int i = 0; i < coordinatesCombo.getItemCount(); ++i) {
            if (coordinatesCombo.getItemAt(i).getId().equals(PROP_COORDINATES.get())) {
                coordinatesCombo.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < unitsValues.length; ++i) {
            if (unitsValues[i].equals(SystemOfMeasurement.PROP_SYSTEM_OF_MEASUREMENT.get())) {
                unitsCombo.setSelectedIndex(i);
                break;
            }
        }

        projPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        projPanel.add(new JLabel(tr("Projection method")), GBC.std().insets(5, 5, 0, 5));
        projPanel.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0, 5, 5, 5));
        projPanel.add(projectionCodeLabel, GBC.std().insets(25, 5, 0, 5));
        projPanel.add(projectionCodeGlue, GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCode, GBC.eop().fill(GBC.HORIZONTAL).insets(0, 5, 5, 5));
        projPanel.add(projectionNameLabel, GBC.std().insets(25, 5, 0, 5));
        projPanel.add(projectionNameGlue, GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionName, GBC.eop().fill(GBC.HORIZONTAL).insets(0, 5, 5, 5));
        projPanel.add(new JLabel(tr("Bounds")), GBC.std().insets(25, 5, 0, 5));
        projPanel.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(bounds, GBC.eop().fill(GBC.HORIZONTAL).insets(0, 5, 5, 5));
        projPanel.add(projSubPrefPanelWrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(20, 5, 5, 5));

        projectionCodeLabel.setLabelFor(projectionCode);
        projectionNameLabel.setLabelFor(projectionName);

        JButton btnSetAsDefault = new JButton(tr("Set as default"));
        projPanel.add(btnSetAsDefault, GBC.eol().insets(5, 10, 5, 5));
        btnSetAsDefault.addActionListener(e -> {
            ProjectionChoice pc2 = (ProjectionChoice) projectionCombo.getSelectedItem();
            String id = pc2.getId();
            Collection<String> prefs = pc2.getPreferences(projSubPrefPanel);
            setProjection(id, prefs, true);
            pc2.setPreferences(prefs);
            Projection proj = pc2.getProjection();
            new ExtendedDialog(gui, tr("Default projection"), tr("OK"))
                    .setButtonIcons("ok")
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setContent(tr("Default projection has been set to ''{0}''", proj.toCode()))
                    .showDialog();
        });
        ExpertToggleAction.addVisibilitySwitcher(btnSetAsDefault);

        projPanel.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 10));
        projPanel.add(new JLabel(tr("Display coordinates as")), GBC.std().insets(5, 5, 0, 5));
        projPanel.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(coordinatesCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0, 5, 5, 5));
        projPanel.add(new JLabel(tr("System of measurement")), GBC.std().insets(5, 5, 0, 5));
        projPanel.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(unitsCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0, 5, 5, 5));
        projPanel.add(GBC.glue(1, 1), GBC.std().fill(GBC.HORIZONTAL).weight(1.0, 1.0));

        gui.getMapPreference().addSubTab(this, tr("Map Projection"), projPanel.getVerticalScrollPane());

        selectedProjectionChanged(pc);
    }

    private void updateMeta(ProjectionChoice pc) {
        pc.setPreferences(pc.getPreferences(projSubPrefPanel));
        Projection proj = pc.getProjection();
        projectionCode.setText(proj.toCode());
        projectionName.setText(proj.toString());
        Bounds b = proj.getWorldBoundsLatLon();
        ICoordinateFormat cf = CoordinateFormatManager.getDefaultFormat();
        bounds.setText(cf.lonToString(b.getMin()) + ", " + cf.latToString(b.getMin()) + " : " +
                cf.lonToString(b.getMax()) + ", " + cf.latToString(b.getMax()));
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

        setProjection(id, prefs, false);

        if (PROP_COORDINATES.put(((ICoordinateFormat) coordinatesCombo.getSelectedItem()).getId())) {
            CoordinateFormatManager.setCoordinateFormat((ICoordinateFormat) coordinatesCombo.getSelectedItem());
        }

        int i = unitsCombo.getSelectedIndex();
        SystemOfMeasurement.setSystemOfMeasurement(unitsValues[i]);

        return false;
    }

    public static void setProjection() {
        setProjection(PROP_PROJECTION_DEFAULT.get(), PROP_SUB_PROJECTION_DEFAULT.get(), false);
    }

    /**
     * Set projection.
     * @param id id of the selected projection choice
     * @param pref the configuration for the selected projection choice
     * @param makeDefault true, if it is to be set as permanent default
     * false, if it is to be set for the current session
     * @since 12306
     */
    public static void setProjection(String id, Collection<String> pref, boolean makeDefault) {
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
        Config.getPref().putList("projection.sub."+id, pref == null ? null : new ArrayList<>(pref));
        if (makeDefault) {
            PROP_PROJECTION_DEFAULT.put(id);
            PROP_SUB_PROJECTION_DEFAULT.put(pref == null ? null : new ArrayList<>(pref));
        } else {
            projectionChoice = id;
        }
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
        if (size < 1)
            return;

        final ActionListener listener = e -> updateMeta(pc);

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
        String pcId = getCurrentProjectionChoiceId();
        ProjectionChoice pc = null;
        for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
            ProjectionChoice pc1 = projectionCombo.getItemAt(i);
            pc1.setPreferences(getSubprojectionPreference(pc1.getId()));
            if (pc1.getId().equals(pcId)) {
                projectionCombo.setSelectedIndex(i);
                selectedProjectionChanged(pc1);
                pc = pc1;
            }
        }
        // If the ProjectionChoice from the preferences is not available, it
        // should have been set to Mercator at JOSM start.
        if (pc == null)
            throw new JosmRuntimeException("Couldn't find the current projection in the list of available projections!");

        projectionCombo.addActionListener(e -> {
            ProjectionChoice pc1 = (ProjectionChoice) projectionCombo.getSelectedItem();
            selectedProjectionChanged(pc1);
        });
        return pc;
    }

    /**
     * Get the id of the projection choice that is currently set.
     * @return id of the projection choice that is currently set
     */
    public static String getCurrentProjectionChoiceId() {
        return projectionChoice != null ? projectionChoice : PROP_PROJECTION_DEFAULT.get();
    }

    /**
     * Get the preferences that have been selected the last time for the given
     * projection choice.
     * @param pcId id of the projection choice
     * @return projection choice parameters that have been selected by the user
     * the last time; null if user has never selected the given projection choice
     */
    public static Collection<String> getSubprojectionPreference(String pcId) {
        return Config.getPref().getList("projection.sub."+pcId, null);
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
