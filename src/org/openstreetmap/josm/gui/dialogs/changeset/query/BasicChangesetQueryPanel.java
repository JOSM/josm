// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.tools.Logging;

/**
 * This panel presents a list of basic queries for changesets.
 * @since 2689
 */
public class BasicChangesetQueryPanel extends JPanel {

    /**
     * Enumeration of basic, predefined queries
     */
    private enum BasicQuery {
        MOST_RECENT_CHANGESETS,
        MY_OPEN_CHANGESETS,
        CHANGESETS_IN_MAP_VIEW;
    }

    private transient Map<BasicQuery, JRadioButton> rbQueries;
    private transient Map<BasicQuery, JMultilineLabel> lblQueries;
    private JCheckBox cbMyChangesetsOnly;

    protected JPanel buildQueriesPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        ButtonGroup bgQueries = new ButtonGroup();
        rbQueries = new EnumMap<>(BasicQuery.class);
        lblQueries = new EnumMap<>(BasicQuery.class);
        SelectQueryHandler selectedQueryHandler = new SelectQueryHandler();
        for (BasicQuery q: BasicQuery.values()) {
            JRadioButton rb = new JRadioButton();
            rb.addItemListener(selectedQueryHandler);
            rbQueries.put(q, rb);
            bgQueries.add(rb);
            lblQueries.put(q, new JMultilineLabel(""));
        }

        GridBagConstraints gc = new GridBagConstraints();
        // -- most recent changes
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(0, 0, 5, 3);
        pnl.add(rbQueries.get(BasicQuery.MOST_RECENT_CHANGESETS), gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(lblQueries.get(BasicQuery.MOST_RECENT_CHANGESETS), gc);

        // -- most recent changes
        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0.0;
        pnl.add(rbQueries.get(BasicQuery.MY_OPEN_CHANGESETS), gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(lblQueries.get(BasicQuery.MY_OPEN_CHANGESETS), gc);

        // -- changesets in map view
        gc.gridx = 0;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0.0;
        pnl.add(rbQueries.get(BasicQuery.CHANGESETS_IN_MAP_VIEW), gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(lblQueries.get(BasicQuery.CHANGESETS_IN_MAP_VIEW), gc);

        // -- checkbox my changesets only
        gc.gridx = 0;
        gc.gridy = 3;
        gc.gridwidth = 2;
        gc.insets = new Insets(5, 0, 3, 3);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        cbMyChangesetsOnly = new JCheckBox(tr("Download my changesets only"));
        pnl.add(cbMyChangesetsOnly, gc);
        cbMyChangesetsOnly.setToolTipText(
                tr("<html>Select to restrict the query to your changesets only.<br>Unselect to include all changesets in the query.</html>"));

        // grab remaining space
        gc.gridx = 0;
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.insets = new Insets(5, 0, 3, 3);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);

        return pnl;
    }

    protected JPanel buildInfoPanel() {
        HtmlPanel pnlInfos = new HtmlPanel();
        pnlInfos.setText(tr("<html>Please select one the following <strong>standard queries</strong>."
                + "Select <strong>Download my changesets only</strong>"
                + " if you only want to download changesets created by yourself.<br>"
                + "Note that JOSM will download max. 100 changesets.</html>")
        );
        return pnlInfos;
    }

    protected final void build() {
        setLayout(new BorderLayout(0, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(buildInfoPanel(), BorderLayout.NORTH);
        add(buildQueriesPanel(), BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code BasicChangesetQueryPanel}.
     */
    public BasicChangesetQueryPanel() {
        build();
    }

    /**
     * Initializes the panel.
     */
    public void init() {
        JMultilineLabel lbl = lblQueries.get(BasicQuery.MOST_RECENT_CHANGESETS);
        lbl.setText(tr("<html>Download the latest changesets</html>"));

        // query for open changesets only possible if we have a current user which is at least
        // partially identified
        lbl = lblQueries.get(BasicQuery.MY_OPEN_CHANGESETS);
        if (JosmUserIdentityManager.getInstance().isAnonymous()) {
            rbQueries.get(BasicQuery.MY_OPEN_CHANGESETS).setEnabled(false);
            lbl.setText(tr("<html>Download my open changesets<br><em>Disabled. " +
                    "Please enter your OSM user name in the preferences first.</em></html>"));
        } else {
            rbQueries.get(BasicQuery.MY_OPEN_CHANGESETS).setEnabled(true);
            lbl.setText(tr("<html>Download my open changesets</html>"));
        }

        // query for changesets in the current map view only if there *is* a current
        // map view
        lbl = lblQueries.get(BasicQuery.CHANGESETS_IN_MAP_VIEW);
        if (!Main.isDisplayingMapView()) {
            rbQueries.get(BasicQuery.CHANGESETS_IN_MAP_VIEW).setEnabled(false);
            lbl.setText(tr("<html>Download changesets in the current map view.<br><em>Disabled. " +
                    "There is currently no map view active.</em></html>"));
        } else {
            rbQueries.get(BasicQuery.CHANGESETS_IN_MAP_VIEW).setEnabled(true);
            lbl.setText(tr("<html>Download changesets in the current map view</html>"));
        }

        restoreFromPreferences();
    }

    /**
     * Remember settings in preferences.
     */
    public void rememberInPreferences() {
        BasicQuery q = getSelectedQuery();
        if (q == null) {
            Main.pref.put("changeset-query.basic.query", null);
        } else {
            Main.pref.put("changeset-query.basic.query", q.toString());
        }
        Main.pref.put("changeset-query.basic.my-changesets-only", cbMyChangesetsOnly.isSelected());
    }

    /**
     * Restore settings from preferences.
     */
    public void restoreFromPreferences() {
        BasicQuery q;
        String value = Main.pref.get("changeset-query.basic.query", null);
        if (value == null) {
            q = BasicQuery.MOST_RECENT_CHANGESETS;
        } else {
            try {
                q = BasicQuery.valueOf(BasicQuery.class, value);
            } catch (IllegalArgumentException e) {
                Logging.log(Logging.LEVEL_WARN, tr("Unexpected value for preference ''{0}'', got ''{1}''. Resetting to default query.",
                        "changeset-query.basic.query", value), e);
                q = BasicQuery.MOST_RECENT_CHANGESETS;
            }
        }
        rbQueries.get(q).setSelected(true);
        boolean mineOnly = Main.pref.getBoolean("changeset-query.basic.my-changesets-only", false);
        mineOnly = mineOnly || q.equals(BasicQuery.MY_OPEN_CHANGESETS);
        cbMyChangesetsOnly.setSelected(mineOnly);
    }

    protected BasicQuery getSelectedQuery() {
        for (BasicQuery q : BasicQuery.values()) {
            if (rbQueries.get(q).isSelected())
                return q;
        }
        return null;
    }

    /**
     * Builds the changeset query.
     * @return the changeset query
     */
    public ChangesetQuery buildChangesetQuery() {
        BasicQuery q = getSelectedQuery();
        ChangesetQuery query = new ChangesetQuery();
        JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
        if (q == null)
            return query;
        switch(q) {
        case MOST_RECENT_CHANGESETS:
            break;
        case MY_OPEN_CHANGESETS:
            query = query.beingOpen(true);
            break;
        case CHANGESETS_IN_MAP_VIEW:
            Bounds b = Main.map.mapView.getLatLonBounds(Main.map.mapView.getBounds());
            query = query.inBbox(b);
            break;
        }

        if (cbMyChangesetsOnly.isSelected()) {
            if (im.isPartiallyIdentified()) {
                query = query.forUser(im.getUserName());
            } else if (im.isFullyIdentified()) {
                query = query.forUser(im.getUserId()).beingOpen(true);
            } else
                // anonymous -- can happen with a fresh config.
                throw new IllegalStateException(tr("Cannot create changeset query for open changesets of anonymous user"));
        }

        return query;
    }

    /**
     * Responds to changes in the selected query
     */
    class SelectQueryHandler implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            BasicQuery q = getSelectedQuery();
            if (q == null) return;
            if (q.equals(BasicQuery.MY_OPEN_CHANGESETS)) {
                cbMyChangesetsOnly.setSelected(true);
                cbMyChangesetsOnly.setEnabled(false);
            } else {
                if (!cbMyChangesetsOnly.isEnabled()) {
                    cbMyChangesetsOnly.setEnabled(true);
                }
            }
        }
    }
}
