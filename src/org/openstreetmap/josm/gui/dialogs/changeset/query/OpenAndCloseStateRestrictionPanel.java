// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * This is the panel for selecting whether the changeset query should be restricted to
 * open or closed changesets.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class OpenAndCloseStateRestrictionPanel extends JPanel implements RestrictionPanel {

    private static final String PREF_ROOT = "changeset-query.advanced.open-restrictions";
    private static final String PREF_QUERY_TYPE = PREF_ROOT + ".query-type";

    private final JRadioButton rbOpenOnly = new JRadioButton();
    private final JRadioButton rbClosedOnly = new JRadioButton();
    private final JRadioButton rbBoth = new JRadioButton();

    /**
     * Constructs a new {@code OpenAndCloseStateRestrictionPanel}.
     */
    public OpenAndCloseStateRestrictionPanel() {
        build();
    }

    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                )
        ));
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        add(rbOpenOnly, gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(new JMultilineLabel(tr("Query open changesets only")), gc);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(rbClosedOnly, gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(new JMultilineLabel(tr("Query closed changesets only")), gc);

        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(rbBoth, gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(new JMultilineLabel(tr("Query both open and closed changesets")), gc);

        ButtonGroup bgRestrictions = new ButtonGroup();
        bgRestrictions.add(rbBoth);
        bgRestrictions.add(rbClosedOnly);
        bgRestrictions.add(rbOpenOnly);
    }

    /**
     * Initializes HMI for user input.
     */
    public void startUserInput() {
        restoreFromSettings();
    }

    /**
     * Sets the query restrictions on <code>query</code> for state based restrictions.
     * @param query the query to fill
     */
    @Override
    public void fillInQuery(ChangesetQuery query) {
        if (rbBoth.isSelected()) {
            query.beingClosed(true);
            query.beingOpen(true);
        } else if (rbOpenOnly.isSelected()) {
            query.beingOpen(true);
        } else if (rbClosedOnly.isSelected()) {
            query.beingClosed(true);
        }
    }

    /**
     * Remember settings in preferences.
     */
    public void rememberSettings() {
        if (rbBoth.isSelected()) {
            Config.getPref().put(PREF_QUERY_TYPE, "both");
        } else if (rbOpenOnly.isSelected()) {
            Config.getPref().put(PREF_QUERY_TYPE, "open");
        } else if (rbClosedOnly.isSelected()) {
            Config.getPref().put(PREF_QUERY_TYPE, "closed");
        }
    }

    /**
     * Restore settings from preferences.
     */
    public void restoreFromSettings() {
        String v = Config.getPref().get(PREF_QUERY_TYPE, "open");
        rbBoth.setSelected("both".equals(v));
        rbOpenOnly.setSelected("open".equals(v));
        rbClosedOnly.setSelected("closed".equals(v));
    }

    @Override
    public boolean isValidChangesetQuery() {
        return true;
    }

    @Override
    public void displayMessageIfInvalid() {
        // Do nothing
    }
}
