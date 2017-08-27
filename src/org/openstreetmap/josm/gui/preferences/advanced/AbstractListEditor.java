// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JPanel;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.WindowGeometry;

/**
 * Abstract superclass of {@link ListEditor} and {@link AbstractTableListEditor}.
 * @param <T> type of elements
 * @since 9505
 */
public abstract class AbstractListEditor<T> extends ExtendedDialog {

    protected final transient PrefEntry entry;

    /**
     * Constructs a new {@code AbstractListEditor}.
     * @param parent       The parent element that will be used for position and maximum size
     * @param title        The text that will be shown in the window titlebar
     * @param entry        Preference entry
     */
    public AbstractListEditor(Component parent, String title, PrefEntry entry) {
        super(parent, title, tr("OK"), tr("Cancel"));
        this.entry = entry;
        setButtonIcons("ok", "cancel");
        setRememberWindowGeometry(getClass().getName() + ".geometry", WindowGeometry.centerInWindow(parent, new Dimension(500, 350)));
    }

    /**
     * Returns the list of values.
     * @return The list of values.
     */
    public abstract List<T> getData();

    protected abstract JPanel build();
}
