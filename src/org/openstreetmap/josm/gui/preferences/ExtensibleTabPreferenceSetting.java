// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static java.awt.GridBagConstraints.BOTH;

import org.openstreetmap.josm.gui.widgets.HideableTabbedPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Abstract base class for {@link TabPreferenceSetting} implementations extensible solely by inner tabs.
 *
 * Support for common functionality, like icon, title and adding a tab ({@link SubPreferenceSetting}).
 * @since 17314
 */
public abstract class ExtensibleTabPreferenceSetting extends DefaultTabPreferenceSetting {

    /**
     * Constructs a new {@code ExtensibleTabPreferenceSetting}.
     */
    protected ExtensibleTabPreferenceSetting() {
        this(null, null, null);
    }

    protected ExtensibleTabPreferenceSetting(String iconName, String title, String description) {
        this(iconName, title, description, false);
    }

    protected ExtensibleTabPreferenceSetting(String iconName, String title, String description, boolean isExpert) {
        super(iconName, title, description, isExpert, new HideableTabbedPane());
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.createPreferenceTab(this).add(getTabPane(), GBC.eol().fill(BOTH));
    }
}
