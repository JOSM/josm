// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

/**
 * This panel allows to specify a changeset query
 * @since 2689
 */
public class AdvancedChangesetQueryPanel extends JPanel {

    private final JCheckBox cbUserRestriction = new JCheckBox(tr("Select changesets owned by specific users"));
    private final JCheckBox cbOpenAndCloseRestrictions = new JCheckBox(tr("Select changesets depending on whether they are open or closed"));
    private final JCheckBox cbTimeRestrictions = new JCheckBox(tr("Select changesets based on the date/time they have been created or closed"));
    private final JCheckBox cbBoundingBoxRestriction = new JCheckBox(tr("Select only changesets related to a specific bounding box"));
    private final UserRestrictionPanel pnlUserRestriction = new UserRestrictionPanel();
    private final OpenAndCloseStateRestrictionPanel pnlOpenAndCloseRestriction = new OpenAndCloseStateRestrictionPanel();
    private final TimeRestrictionPanel pnlTimeRestriction = new TimeRestrictionPanel();
    private final BBoxRestrictionPanel pnlBoundingBoxRestriction = new BBoxRestrictionPanel();

    /**
     * Constructs a new {@code AdvancedChangesetQueryPanel}.
     */
    public AdvancedChangesetQueryPanel() {
        build();
    }

    protected JPanel buildQueryPanel() {
        ItemListener stateChangeHandler = new RestrictionGroupStateChangeHandler();
        JPanel pnl = new VerticallyScrollablePanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints gc = GBC.eol().fill(GridBagConstraints.HORIZONTAL);

        // -- select changesets by a specific user
        //
        pnl.add(cbUserRestriction, gc);
        cbUserRestriction.addItemListener(stateChangeHandler);
        pnl.add(pnlUserRestriction, gc);

        // -- restricting the query to open and closed changesets
        //
        pnl.add(cbOpenAndCloseRestrictions, gc);
        cbOpenAndCloseRestrictions.addItemListener(stateChangeHandler);
        pnl.add(pnlOpenAndCloseRestriction, gc);

        // -- restricting the query to a specific time
        //
        pnl.add(cbTimeRestrictions, gc);
        cbTimeRestrictions.addItemListener(stateChangeHandler);
        pnl.add(pnlTimeRestriction, gc);


        // -- restricting the query to a specific bounding box
        //
        pnl.add(cbBoundingBoxRestriction, gc);
        cbBoundingBoxRestriction.addItemListener(stateChangeHandler);
        pnl.add(pnlBoundingBoxRestriction, gc);

        pnl.add(new JPanel(), gc);

        return pnl;
    }

    protected final void build() {
        setLayout(new BorderLayout());
        JScrollPane spQueryPanel = GuiHelper.embedInVerticalScrollPane(buildQueryPanel());
        add(spQueryPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes HMI for user input.
     */
    public void startUserInput() {
        restoreFromSettings();
        pnlBoundingBoxRestriction.setVisible(cbBoundingBoxRestriction.isSelected());
        pnlOpenAndCloseRestriction.setVisible(cbOpenAndCloseRestrictions.isSelected());
        pnlTimeRestriction.setVisible(cbTimeRestrictions.isSelected());
        pnlUserRestriction.setVisible(cbUserRestriction.isSelected());
        pnlOpenAndCloseRestriction.startUserInput();
        pnlUserRestriction.startUserInput();
        pnlTimeRestriction.startUserInput();
    }

    /**
     * Display error message if a field is invalid.
     */
    public void displayMessageIfInvalid() {
        if (cbUserRestriction.isSelected()) {
            if (!pnlUserRestriction.isValidChangesetQuery()) {
                pnlUserRestriction.displayMessageIfInvalid();
            }
        } else if (cbTimeRestrictions.isSelected()) {
            if (!pnlTimeRestriction.isValidChangesetQuery()) {
                pnlTimeRestriction.displayMessageIfInvalid();
            }
        } else if (cbBoundingBoxRestriction.isSelected()) {
            if (!pnlBoundingBoxRestriction.isValidChangesetQuery()) {
                pnlBoundingBoxRestriction.displayMessageIfInvalid();
            }
        }
    }

    /**
     * Builds the changeset query based on the data entered in the form.
     *
     * @return the changeset query. null, if the data entered doesn't represent
     * a valid changeset query.
     */
    public ChangesetQuery buildChangesetQuery() {
        ChangesetQuery query = new ChangesetQuery();
        if (cbUserRestriction.isSelected()) {
            if (!pnlUserRestriction.isValidChangesetQuery())
                return null;
            pnlUserRestriction.fillInQuery(query);
        }
        if (cbOpenAndCloseRestrictions.isSelected()) {
            // don't have to check whether it's valid. It always is.
            pnlOpenAndCloseRestriction.fillInQuery(query);
        }
        if (cbBoundingBoxRestriction.isSelected()) {
            if (!pnlBoundingBoxRestriction.isValidChangesetQuery())
                return null;
            pnlBoundingBoxRestriction.fillInQuery(query);
        }
        if (cbTimeRestrictions.isSelected()) {
            if (!pnlTimeRestriction.isValidChangesetQuery())
                return null;
            pnlTimeRestriction.fillInQuery(query);
        }
        return query;
    }

    /**
     * Remember settings in preferences.
     */
    public void rememberSettings() {
        Config.getPref().putBoolean("changeset-query.advanced.user-restrictions", cbUserRestriction.isSelected());
        Config.getPref().putBoolean("changeset-query.advanced.open-restrictions", cbOpenAndCloseRestrictions.isSelected());
        Config.getPref().putBoolean("changeset-query.advanced.time-restrictions", cbTimeRestrictions.isSelected());
        Config.getPref().putBoolean("changeset-query.advanced.bbox-restrictions", cbBoundingBoxRestriction.isSelected());

        pnlUserRestriction.rememberSettings();
        pnlOpenAndCloseRestriction.rememberSettings();
        pnlTimeRestriction.rememberSettings();
    }

    /**
     * Restore settings from preferences.
     */
    public void restoreFromSettings() {
        cbUserRestriction.setSelected(Config.getPref().getBoolean("changeset-query.advanced.user-restrictions", false));
        cbOpenAndCloseRestrictions.setSelected(Config.getPref().getBoolean("changeset-query.advanced.open-restrictions", false));
        cbTimeRestrictions.setSelected(Config.getPref().getBoolean("changeset-query.advanced.time-restrictions", false));
        cbBoundingBoxRestriction.setSelected(Config.getPref().getBoolean("changeset-query.advanced.bbox-restrictions", false));
    }

    class RestrictionGroupStateChangeHandler implements ItemListener {
        protected void userRestrictionStateChanged() {
            pnlUserRestriction.setVisible(cbUserRestriction.isSelected());
        }

        protected void openCloseRestrictionStateChanged() {
            pnlOpenAndCloseRestriction.setVisible(cbOpenAndCloseRestrictions.isSelected());
        }

        protected void timeRestrictionsStateChanged() {
            pnlTimeRestriction.setVisible(cbTimeRestrictions.isSelected());
        }

        protected void boundingBoxRestrictionChanged() {
            pnlBoundingBoxRestriction.setVisible(cbBoundingBoxRestriction.isSelected());
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getSource() == cbUserRestriction) {
                userRestrictionStateChanged();
            } else if (e.getSource() == cbOpenAndCloseRestrictions) {
                openCloseRestrictionStateChanged();
            } else if (e.getSource() == cbTimeRestrictions) {
                timeRestrictionsStateChanged();
            } else if (e.getSource() == cbBoundingBoxRestriction) {
                boundingBoxRestrictionChanged();
            }
            validate();
            repaint();
        }
    }
}
