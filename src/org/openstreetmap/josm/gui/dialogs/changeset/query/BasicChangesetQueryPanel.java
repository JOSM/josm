// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
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
    private JCheckBox cbMyChangesetsOnly;

    protected JPanel buildQueriesPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        ButtonGroup bgQueries = new ButtonGroup();
        rbQueries = new EnumMap<>(BasicQuery.class);
        SelectQueryHandler selectedQueryHandler = new SelectQueryHandler();
        for (BasicQuery q: BasicQuery.values()) {
            JRadioButton rb = new JRadioButton();
            rb.addItemListener(selectedQueryHandler);
            rbQueries.put(q, rb);
            bgQueries.add(rb);
        }

        GridBagConstraints gc = GBC.eop().fill(GridBagConstraints.HORIZONTAL);
        // -- most recent changes
        pnl.add(rbQueries.get(BasicQuery.MOST_RECENT_CHANGESETS), gc);

        // -- most recent changes
        pnl.add(rbQueries.get(BasicQuery.MY_OPEN_CHANGESETS), gc);

        // -- changesets in map view
        pnl.add(rbQueries.get(BasicQuery.CHANGESETS_IN_MAP_VIEW), gc);

        // -- checkbox my changesets only
        gc.gridwidth = 2;
        gc.insets = new Insets(5, 0, 3, 3);
        cbMyChangesetsOnly = new JCheckBox(tr("Download my changesets only"));
        pnl.add(cbMyChangesetsOnly, gc);
        cbMyChangesetsOnly.setToolTipText(
                tr("<html>Select to restrict the query to your changesets only.<br>Unselect to include all changesets in the query.</html>"));

        // grab remaining space
        pnl.add(new JPanel(), GBC.eol().insets(5, 0, 3, 3).fill());

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
        JRadioButton lbl = rbQueries.get(BasicQuery.MOST_RECENT_CHANGESETS);
        lbl.setText(tr("<html>Download the latest changesets</html>"));

        // query for open changesets only possible if we have a current user which is at least
        // partially identified
        lbl = rbQueries.get(BasicQuery.MY_OPEN_CHANGESETS);
        if (UserIdentityManager.getInstance().isAnonymous()) {
            rbQueries.get(BasicQuery.MY_OPEN_CHANGESETS).setEnabled(false);
            lbl.setText(tr("<html>Download my open changesets<br><em>Disabled. " +
                    "Please enter your OSM user name in the preferences first.</em></html>"));
        } else {
            rbQueries.get(BasicQuery.MY_OPEN_CHANGESETS).setEnabled(true);
            lbl.setText(tr("<html>Download my open changesets</html>"));
        }

        // query for changesets in the current map view only if there *is* a current map view
        lbl = rbQueries.get(BasicQuery.CHANGESETS_IN_MAP_VIEW);
        if (!MainApplication.isDisplayingMapView()) {
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
            Config.getPref().put("changeset-query.basic.query", null);
        } else {
            Config.getPref().put("changeset-query.basic.query", q.toString());
        }
        Config.getPref().putBoolean("changeset-query.basic.my-changesets-only", cbMyChangesetsOnly.isSelected());
    }

    /**
     * Restore settings from preferences.
     */
    public void restoreFromPreferences() {
        BasicQuery q;
        String value = Config.getPref().get("changeset-query.basic.query", null);
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
        boolean mineOnly = Config.getPref().getBoolean("changeset-query.basic.my-changesets-only", false);
        mineOnly = mineOnly || q == BasicQuery.MY_OPEN_CHANGESETS;
        cbMyChangesetsOnly.setSelected(mineOnly);
    }

    protected BasicQuery getSelectedQuery() {
        return Arrays.stream(BasicQuery.values())
                .filter(q -> rbQueries.get(q).isSelected())
                .findFirst().orElse(null);
    }

    /**
     * Builds the changeset query.
     * @return the changeset query
     */
    public ChangesetQuery buildChangesetQuery() {
        BasicQuery q = getSelectedQuery();
        ChangesetQuery query = new ChangesetQuery();
        UserIdentityManager im = UserIdentityManager.getInstance();
        if (q == null)
            return query;
        switch(q) {
        case MOST_RECENT_CHANGESETS:
            break;
        case MY_OPEN_CHANGESETS:
            query = query.beingOpen(true);
            break;
        case CHANGESETS_IN_MAP_VIEW:
            MapView mapView = MainApplication.getMap().mapView;
            Bounds b = mapView.getLatLonBounds(mapView.getBounds());
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
            if (q == BasicQuery.MY_OPEN_CHANGESETS) {
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
