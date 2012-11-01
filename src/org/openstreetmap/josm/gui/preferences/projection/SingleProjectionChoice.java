// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.projection.Projection;

/**
 * ProjectionChoice, that offers just one projection as choice.
 *
 * The GUI is an empty panel.
 */
public class SingleProjectionChoice implements ProjectionChoice {

    private String id;
    private String name;
    private Projection projection;

    public SingleProjectionChoice(String id, String name, Projection projection) {
        this.id = id;
        this.name = name;
        this.projection = projection;
    }

    public SingleProjectionChoice(String id, Projection projection) {
        this(id, projection.toString(), projection);
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new JPanel();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String[] allCodes() {
        return new String[] { projection.toCode() };
    }

    @Override
    public void setPreferences(Collection<String> args) {
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        return Collections.emptyList();
    }

    @Override
    public Projection getProjection() {
        return projection;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        if (code.equals(projection.toCode()))
            return Collections.emptyList();
        else
            return null;
    }
}
