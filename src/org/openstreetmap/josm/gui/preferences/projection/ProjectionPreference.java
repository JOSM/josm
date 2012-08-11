// License: GPL. Copyright 2007 by Immanuel Scholz and others
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
import org.openstreetmap.josm.data.projection.BelgianLambert1972;
import org.openstreetmap.josm.data.projection.BelgianLambert2008;
import org.openstreetmap.josm.data.projection.Epsg3008;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.data.projection.Lambert93;
import org.openstreetmap.josm.data.projection.LambertEST;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.TransverseMercatorLV;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.GBC;

public class ProjectionPreference implements SubPreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ProjectionPreference();
        }
    }

    private static List<ProjectionChoice> projectionChoices = new ArrayList<ProjectionChoice>();
    private static Map<String, ProjectionChoice> projectionChoicesById = new HashMap<String, ProjectionChoice>();
    private static Map<String, String> aliasNormalizer = new HashMap<String, String>();

    public static ProjectionChoice mercator = new SingleProjectionChoice("core:mercator", new Mercator());
    static {
        // global projections
        registerProjectionChoice("core:wgs84", new Epsg4326());
        registerProjectionChoice(mercator);
        registerProjectionChoice(new UTMProjectionChoice());
        // regional - alphabetical order by country code
        registerProjectionChoice("core:belambert1972", new BelgianLambert1972());   // BE
        registerProjectionChoice("core:belambert2008", new BelgianLambert2008());   // BE
        registerProjectionChoice(new SwissGridProjectionChoice());                  // CH
        registerProjectionChoice(new GaussKruegerProjectionChoice());               // DE
        registerProjectionChoice("core:lambertest", new LambertEST());              // EE
        registerProjectionChoice(new LambertProjectionChoice());                    // FR
        registerProjectionChoice("core:lambert93", new Lambert93());                // FR
        registerProjectionChoice(new LambertCC9ZonesProjectionChoice());            // FR
        registerProjectionChoice(new UTM_France_DOM_ProjectionChoice());            // FR
        registerProjectionChoice("core:tmerclv", new TransverseMercatorLV());       // LV
        registerProjectionChoice(new PuwgProjectionChoice());                       // PL
        registerProjectionChoice("core:sweref99", new Epsg3008());                  // SE
        registerProjectionChoice(new CustomProjectionChoice());
    }

    public static void registerProjectionChoice(ProjectionChoice c) {
        projectionChoices.add(c);
        projectionChoicesById.put(c.getId(), c);
        aliasNormalizer.put(c.getId(), c.getId());
        if (c instanceof Alias) {
            String alias = ((Alias) c).getAlias();
            projectionChoicesById.put(alias, c);
            aliasNormalizer.put(alias, c.getId());
        }
    }

    public static void registerProjectionChoice(String id, Projection projection) {
        registerProjectionChoice(new SingleProjectionChoice(id, projection));
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
        gui.getMapPreference().mapcontent.addTab(tr("Map Projection"), scrollpane);

        selectedProjectionChanged(pc);
    }

    private void updateMeta(ProjectionChoice pc) {
        pc.setPreferences(pc.getPreferences(projSubPrefPanel));
        Projection proj = pc.getProjection();
        projectionCode.setText(proj.toCode());
        Bounds b = proj.getWorldBoundsLatLon();
        CoordinateFormat cf = CoordinateFormat.getDefaultFormat();
        bounds.setText(b.getMin().lonToString(cf)+", "+b.getMin().latToString(cf)+" : "+b.getMax().lonToString(cf)+", "+b.getMax().latToString(cf));
        boolean showCode = true;
        if (pc instanceof SubPrefsOptions) {
            showCode = ((SubPrefsOptions) pc).showProjectionCode();
        }
        projectionCodeLabel.setVisible(showCode);
        projectionCodeGlue.setVisible(showCode);
        projectionCode.setVisible(showCode);
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
        PROP_SYSTEM_OF_MEASUREMENT.put(unitsValues[i]);

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
     * @param proj
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
     */
    private ProjectionChoice setupProjectionCombo() {
        ProjectionChoice pc = null;
        for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
            ProjectionChoice pc1 = (ProjectionChoice) projectionCombo.getItemAt(i);
            pc1.setPreferences(getSubprojectionPreference(pc1));
            if (pc1.getId().equals(aliasNormalizer.get(PROP_PROJECTION.get()))) {
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
            public void actionPerformed(ActionEvent e) {
                ProjectionChoice pc = (ProjectionChoice) projectionCombo.getSelectedItem();
                selectedProjectionChanged(pc);
            }
        });
        return pc;
    }

    private Collection<String> getSubprojectionPreference(ProjectionChoice pc) {
        Collection<String> c1 = Main.pref.getCollection("projection.sub."+pc.getId(), null);
        if (c1 != null)
            return c1;
        if (pc instanceof Alias) {
            String alias = ((Alias) pc).getAlias();
            String sname = alias.substring(alias.lastIndexOf(".")+1);
            return Main.pref.getCollection("projection.sub."+sname, null);
        }
        return null;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getMapPreference();
    }
}
