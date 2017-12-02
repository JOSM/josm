// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JPanel;

/**
 * ProjectionChoice, that offers just one projection as choice.
 *
 * The GUI is an empty panel.
 */
public class SingleProjectionChoice extends AbstractProjectionChoice {

    protected String code;

    /**
     * Constructs a new {@code SingleProjectionChoice}.
     *
     * @param name short name of the projection choice as shown in the GUI
     * @param id unique identifier for the projection choice, e.g. "core:thisproj"
     * @param code the unique identifier for the projection, e.g. "EPSG:1234"
     * @param cacheDir unused
     * @deprecated use {@link #SingleProjectionChoice(String, String, String)} instead
     */
    @Deprecated
    public SingleProjectionChoice(String name, String id, String code, String cacheDir) {
        this(name, id, code);
    }

    /**
     * Constructs a new {@code SingleProjectionChoice}.
     *
     * @param name short name of the projection choice as shown in the GUI
     * @param id unique identifier for the projection choice, e.g. "core:thisproj"
     * @param code the unique identifier for the projection, e.g. "EPSG:1234"
     */
    public SingleProjectionChoice(String name, String id, String code) {
        super(name, id);
        this.code = code;
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new JPanel();
    }

    @Override
    public String[] allCodes() {
        return new String[] {code};
    }

    @Override
    public void setPreferences(Collection<String> args) {
        // Do nothing
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        if (code.equals(this.code))
            return Collections.emptyList();
        else
            return null;
    }

    @Override
    public String getCurrentCode() {
        return code;
    }

    @Override
    public String getProjectionName() {
        return name; // the same name as the projection choice
    }

}
