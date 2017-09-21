// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;

/**
 * Editor for String preference entries.
 */
public class StringEditor extends ExtendedDialog {

    private final transient PrefEntry entry;
    private JosmTextField tvalue;

    /**
     * Constructs a new {@code StringEditor}.
     * @param gui The parent component
     * @param entry preference entry
     * @param setting string setting
     */
    public StringEditor(final JComponent gui, PrefEntry entry, StringSetting setting) {
        super(gui, tr("Change string setting"), tr("OK"), tr("Cancel"));
        this.entry = entry;
        setButtonIcons("ok", "cancel");
        setContent(build(setting.getValue() == null ? "" : setting.getValue()));
    }

    /**
     * Returns the data.
     * @return the preference data
     */
    public String getData() {
        return tvalue.getText();
    }

    protected final JPanel build(String orig) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key: {0}", entry.getKey())), GBC.eol().insets(0, 0, 5, 0));

        p.add(new JLabel(tr("Value: ")), GBC.std());
        tvalue = new JosmTextField(orig, 50);
        p.add(tvalue, GBC.eop().insets(5, 0, 0, 0).fill(GBC.HORIZONTAL));

        return p;
    }
}
