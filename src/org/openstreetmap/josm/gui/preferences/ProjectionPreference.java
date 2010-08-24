// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.data.preferences.ParametrizedCollectionProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionSubPrefs;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;

public class ProjectionPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ProjectionPreference();
        }
    }

    public interface ProjectionChangedListener {
        void projectionChanged();
    }

    private static final StringProperty PROP_PROJECTION = new StringProperty("projection", Mercator.class.getName());
    private static final StringProperty PROP_COORDINATES = new StringProperty("coordinates", null);
    private static final CollectionProperty PROP_SUB_PROJECTION = new CollectionProperty("projection.sub", null);
    private static final ParametrizedCollectionProperty PROP_PROJECTION_SUBPROJECTION = new ParametrizedCollectionProperty(null) {
        @Override
        protected String getKey(String... params) {
            String name = params[0];
            String sname = name.substring(name.lastIndexOf(".")+1);
            return "projection.sub."+sname;
        }
    };
    private static final StringProperty PROP_SYSTEM_OF_MEASUREMENT = new StringProperty("system_of_measurement", "Metric");
    private static final String[] unitsValues = (new ArrayList<String>(NavigatableComponent.SYSTEMS_OF_MEASUREMENT.keySet())).toArray(new String[0]);
    private static final String[] unitsValuesTr = new String[unitsValues.length];
    static {
        for (int i=0; i<unitsValues.length; ++i) {
            unitsValuesTr[i] = tr(unitsValues[i]);
        }
    }

    //TODO This is not nice place for a listener code but probably only Dataset will want to listen for projection changes so it's acceptable
    private static CopyOnWriteArrayList<ProjectionChangedListener> listeners = new CopyOnWriteArrayList<ProjectionChangedListener>();

    public static void addProjectionChangedListener(ProjectionChangedListener listener) {
        listeners.addIfAbsent(listener);
    }

    public static void removeProjectionChangedListener(ProjectionChangedListener listener) {
        listeners.remove(listener);
    }

    private static void fireProjectionChanged() {
        for (ProjectionChangedListener listener: listeners) {
            listener.projectionChanged();
        }
    }


    /**
     * Combobox with all projections available
     */
    private JComboBox projectionCombo = new JComboBox(Projection.allProjections);

    /**
     * Combobox with all coordinate display possibilities
     */
    private JComboBox coordinatesCombo = new JComboBox(CoordinateFormat.values());

    private JComboBox unitsCombo = new JComboBox(unitsValuesTr);

    /**
     * This variable holds the JPanel with the projection's preferences. If the
     * selected projection does not implement this, it will be set to an empty
     * Panel.
     */
    private JPanel projSubPrefPanel;
    private JPanel projSubPrefPanelWrapper = new JPanel(new GridBagLayout());

    private JLabel projectionCode = new JLabel();
    private JLabel bounds = new JLabel();

    /**
     * This is the panel holding all projection preferences
     */
    private JPanel projPanel = new VerticallyScrollablePanel();

    /**
     * The GridBagConstraints for the Panel containing the ProjectionSubPrefs.
     * This is required twice in the code, creating it here keeps both occurrences
     * in sync
     */
    static private GBC projSubPrefPanelGBC = GBC.std().fill(GBC.BOTH).weight(1.0, 1.0);

    public void addGui(PreferenceTabbedPane gui) {
        setupProjectionCombo();

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
        projPanel.add(new JLabel(tr("Projection code")), GBC.std().insets(25,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCode, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("Bounds")), GBC.std().insets(25,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(bounds, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projSubPrefPanelWrapper.add(projSubPrefPanel, projSubPrefPanelGBC);
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
        gui.mapcontent.addTab(tr("Map Projection"), scrollpane);

        updateMeta(Main.proj);
    }

    private void updateMeta(Projection proj)
    {
        projectionCode.setText(proj.toCode());
        Bounds b = proj.getWorldBoundsLatLon();
        CoordinateFormat cf = CoordinateFormat.getDefaultFormat();
        bounds.setText(b.getMin().latToString(cf)+"; "+b.getMin().lonToString(cf)+" : "+b.getMax().latToString(cf)+"; "+b.getMax().lonToString(cf));
    }

    public boolean ok() {
        Projection proj = (Projection) projectionCombo.getSelectedItem();

        String projname = proj.getClass().getName();
        Collection<String> prefs = null;
        if(proj instanceof ProjectionSubPrefs) {
            prefs = ((ProjectionSubPrefs) proj).getPreferences(projSubPrefPanel);
        }

        PROP_PROJECTION.put(projname);
        setProjection(projname, prefs);

        if(PROP_COORDINATES.put(((CoordinateFormat)coordinatesCombo.getSelectedItem()).name())) {
            CoordinateFormat.setCoordinateFormat((CoordinateFormat)coordinatesCombo.getSelectedItem());
        }

        int i = unitsCombo.getSelectedIndex();
        PROP_SYSTEM_OF_MEASUREMENT.put(unitsValues[i]);

        return false;
    }

    static public void setProjection()
    {
        setProjection(PROP_PROJECTION.get(), PROP_SUB_PROJECTION.get());
    }

    static public void setProjection(String name, Collection<String> coll)
    {
        Bounds b = (Main.map != null && Main.map.mapView != null) ? Main.map.mapView.getRealBounds() : null;
        Projection oldProj = Main.proj;

        try {
            Main.proj = (Projection)Class.forName(name).newInstance();
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The projection {0} could not be activated. Using Mercator", name),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            coll = null;
            Main.proj = new Mercator();
            name = Main.proj.getClass().getName();
        }
        PROP_SUB_PROJECTION.put(coll);
        PROP_PROJECTION_SUBPROJECTION.put(coll, name);
        if(Main.proj instanceof ProjectionSubPrefs) {
            ((ProjectionSubPrefs) Main.proj).setPreferences(coll);
        }
        fireProjectionChanged(); // This should be probably called from the if bellow, but hashCode condition doesn't look sure enough
        if(b != null && (!Main.proj.getClass().getName().equals(oldProj.getClass().getName()) || Main.proj.hashCode() != oldProj.hashCode()))
        {
            Main.map.mapView.zoomTo(b);
            /* TODO - remove layers with fixed projection */
        }
    }

    private class SBPanel extends JPanel
    {
        private Projection p;
        public SBPanel(Projection pr)
        {
            super();
            p = pr;
        }
        @Override
        public void paint(java.awt.Graphics g)
        {
            super.paint(g);
            ((ProjectionSubPrefs) p).setPreferences(((ProjectionSubPrefs) p).getPreferences(this));
            updateMeta(p);
        }
    }

    /**
     * Handles all the work related to update the projection-specific
     * preferences
     * @param proj
     */
    private void selectedProjectionChanged(Projection proj) {
        if(!(proj instanceof ProjectionSubPrefs)) {
            projSubPrefPanel = new JPanel();
        } else {
            ProjectionSubPrefs projPref = (ProjectionSubPrefs) proj;
            projSubPrefPanel = new SBPanel(proj);
            projPref.setupPreferencePanel(projSubPrefPanel);
        }

        // Don't try to update if we're still starting up
        int size = projPanel.getComponentCount();
        if(size < 1)
            return;

        // Replace old panel with new one
        projSubPrefPanelWrapper.removeAll();
        projSubPrefPanelWrapper.add(projSubPrefPanel, projSubPrefPanelGBC);
        projPanel.revalidate();
        projSubPrefPanel.repaint();
        updateMeta(proj);
    }

    /**
     * Sets up projection combobox with default values and action listener
     */
    private void setupProjectionCombo() {
        for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
            Projection proj = (Projection)projectionCombo.getItemAt(i);
            String name = proj.getClass().getName();
            if(proj instanceof ProjectionSubPrefs) {
                ((ProjectionSubPrefs) proj).setPreferences(PROP_PROJECTION_SUBPROJECTION.get(name));
            }
            if (name.equals(PROP_PROJECTION.get())) {
                projectionCombo.setSelectedIndex(i);
                selectedProjectionChanged(proj);
                break;
            }
        }

        projectionCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                Projection proj = (Projection)cb.getSelectedItem();
                selectedProjectionChanged(proj);
            }
        });
    }
}
