// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionSubPrefs;
import org.openstreetmap.josm.tools.GBC;

public class ProjectionPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ProjectionPreference();
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

    /**
     * This variable holds the JPanel with the projection's preferences. If the
     * selected projection does not implement this, it will be set to an empty
     * Panel.
     */
    private JPanel projSubPrefPanel;

    private JLabel projectionCode = new JLabel();
    private JLabel bounds = new JLabel();

    /**
     * This is the panel holding all projection preferences
     */
    private JPanel projPanel = new JPanel();

    /**
     * The GridBagConstraints for the Panel containing the ProjectionSubPrefs.
     * This is required twice in the code, creating it here keeps both occurrences
     * in sync
     */
    static private GBC projSubPrefPanelGBC = GBC.eol().fill(GBC.BOTH).insets(20,5,5,5);

    public void addGui(PreferenceDialog gui) {
        clearSubProjPrefs();
        setupProjectionCombo();

        for (int i = 0; i < coordinatesCombo.getItemCount(); ++i) {
            if (((CoordinateFormat)coordinatesCombo.getItemAt(i)).name().equals(Main.pref.get("coordinates"))) {
                coordinatesCombo.setSelectedIndex(i);
                break;
            }
        }

        projPanel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        projPanel.setLayout(new GridBagLayout());
        projPanel.add(new JLabel(tr("Display coordinates as")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(coordinatesCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("Projection method")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("Projection code")), GBC.std().insets(25,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCode, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("Bounds")), GBC.std().insets(25,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(bounds, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(projSubPrefPanel, projSubPrefPanelGBC);
        JScrollPane scrollpane = new JScrollPane(projPanel);
        gui.mapcontent.addTab(tr("Map Projection"), scrollpane);

        projectionCode.setText(Main.proj.toCode());
        Bounds b = Main.proj.getWorldBoundsLatLon();
        CoordinateFormat cf = CoordinateFormat.getDefaultFormat();
        bounds.setText(b.min.latToString(cf)+"; "+b.min.lonToString(cf)+" : "+b.max.latToString(cf)+"; "+b.max.lonToString(cf));
        /* TODO: Fix bugs, refresh code line and world bounds, fix design (e.g. add border around sub-prefs-stuff */
    }

    public boolean ok() {
        Projection proj = (Projection) projectionCombo.getSelectedItem();

        String projname = proj.getClass().getName();
        Collection<String> prefs = null;
        if(projHasPrefs(proj))
            prefs = ((ProjectionSubPrefs) proj).getPreferences();

        if(Main.pref.put("projection", projname)) {
            setProjection(projname, prefs);
        }

        if(Main.pref.put("coordinates",
                ((CoordinateFormat)coordinatesCombo.getSelectedItem()).name())) {
            CoordinateFormat.setCoordinateFormat((CoordinateFormat)coordinatesCombo.getSelectedItem());
        }

        // We get the change to remove these panels on closing the preferences
        // dialog, so take it. TODO: Make this work always, even when canceling
        // the dialog
        clearSubProjPrefs();

        return false;
    }

    /**
     * Finds out if the given projection implements the ProjectionPreference
     * interface
     * @param proj
     * @return
     */
    @SuppressWarnings("unchecked")
    static private boolean projHasPrefs(Projection proj) {
        Class[] ifaces = proj.getClass().getInterfaces();
        for(int i = 0; i < ifaces.length; i++) {
            if(ifaces[i].getSimpleName().equals("ProjectionSubPrefs"))
                return true;
        }
        return false;
    }

    static public void setProjection()
    {
        setProjection(Main.pref.get("projection", Mercator.class.getName()),
        Main.pref.getCollection("projection.sub", null));
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
        }
        if(!Main.proj.equals(oldProj) && b != null)
        {
            Main.map.mapView.zoomTo(b);
            /* TODO - remove layers with fixed projection */
        }
        Main.pref.putCollection("projection.sub", coll);
        if(coll != null && projHasPrefs(Main.proj))
            ((ProjectionSubPrefs) Main.proj).setPreferences(coll);
    }

    /**
     * Handles all the work related to update the projection-specific
     * preferences
     * @param proj
     */
    private void selectedProjectionChanged(Projection proj) {
        if(!projHasPrefs(proj)) {
            projSubPrefPanel = new JPanel();
        } else {
            ProjectionSubPrefs projPref = (ProjectionSubPrefs) proj;
            projSubPrefPanel = projPref.getPreferencePanel();
        }

        // Don't try to update if we're still starting up
        int size = projPanel.getComponentCount();
        if(size < 1)
            return;

        // Replace old panel with new one
        projPanel.remove(size - 1);
        projPanel.add(projSubPrefPanel, projSubPrefPanelGBC);
        projPanel.revalidate();
    }

    /**
     * Sets up projection combobox with default values and action listener
     */
    private void setupProjectionCombo() {
        for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
            Projection proj = (Projection)projectionCombo.getItemAt(i);
            if (proj.getClass().getName().equals(Main.pref.get("projection", Mercator.class.getName()))) {
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

    /**
     * Method to clean up the preference panels made by each projection. This
     * requires them to be regenerated when the prefs dialog is opened again,
     * but this also makes them react to changes to their preferences from the
     * outside
     */
    static private void clearSubProjPrefs() {
        for(Projection proj : Projection.allProjections) {
            if(projHasPrefs(proj)) {
                ((ProjectionSubPrefs) proj).destroyCachedPanel();
            }
        }
    }
}
