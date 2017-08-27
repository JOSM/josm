// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is a modal dialog for entering query criteria to search for changesets.
 * @since 2689
 */
public class ChangesetQueryDialog extends JDialog {

    private JTabbedPane tpQueryPanels;
    private final BasicChangesetQueryPanel pnlBasicChangesetQueries = new BasicChangesetQueryPanel();
    private final UrlBasedQueryPanel pnlUrlBasedQueries = new UrlBasedQueryPanel();
    private final AdvancedChangesetQueryPanel pnlAdvancedQueries = new AdvancedChangesetQueryPanel();
    private boolean canceled;

    /**
     * Constructs a new {@code ChangesetQueryDialog}.
     * @param parent parent window
     */
    public ChangesetQueryDialog(Window parent) {
        super(parent, ModalityType.DOCUMENT_MODAL);
        build();
    }

    protected JPanel buildContentPanel() {
        tpQueryPanels = new JTabbedPane();
        tpQueryPanels.add(pnlBasicChangesetQueries);
        tpQueryPanels.add(pnlUrlBasedQueries);
        tpQueryPanels.add(pnlAdvancedQueries);

        tpQueryPanels.setTitleAt(0, tr("Basic"));
        tpQueryPanels.setToolTipTextAt(0, tr("Download changesets using predefined queries"));

        tpQueryPanels.setTitleAt(1, tr("From URL"));
        tpQueryPanels.setToolTipTextAt(1, tr("Query changesets from a server URL"));

        tpQueryPanels.setTitleAt(2, tr("Advanced"));
        tpQueryPanels.setToolTipTextAt(2, tr("Use a custom changeset query"));

        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(tpQueryPanels, BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        pnl.add(new JButton(new QueryAction()));
        pnl.add(new JButton(new CancelAction()));
        pnl.add(new JButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/ChangesetQuery"))));

        return pnl;
    }

    protected final void build() {
        setTitle(tr("Query changesets"));
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(buildContentPanel(), BorderLayout.CENTER);
        cp.add(buildButtonPanel(), BorderLayout.SOUTH);

        // cancel on ESC
        InputMapUtils.addEscapeAction(getRootPane(), new CancelAction());

        // context sensitive help
        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Dialog/ChangesetQueryDialog"));

        addWindowListener(new WindowEventHandler());
    }

    /**
     * Determines if the dialog has been canceled.
     * @return {@code true} if the dialog has been canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Initializes HMI for user input.
     */
    public void initForUserInput() {
        pnlBasicChangesetQueries.init();
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Returns the changeset query.
     * @return the changeset query
     */
    public ChangesetQuery getChangesetQuery() {
        if (isCanceled())
            return null;
        switch(tpQueryPanels.getSelectedIndex()) {
        case 0:
            return pnlBasicChangesetQueries.buildChangesetQuery();
        case 1:
            return pnlUrlBasedQueries.buildChangesetQuery();
        case 2:
            return pnlAdvancedQueries.buildChangesetQuery();
        default:
            // FIXME: extend with advanced queries
            return null;
        }
    }

    /**
     * Initializes HMI for user input.
     */
    public void startUserInput() {
        pnlUrlBasedQueries.startUserInput();
        pnlAdvancedQueries.startUserInput();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            new Dimension(400, 400)
                    )
            ).applySafe(this);
            setCanceled(false);
            startUserInput();
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
            pnlAdvancedQueries.rememberSettings();
        }
        super.setVisible(visible);
    }

    class QueryAction extends AbstractAction {
        QueryAction() {
            putValue(NAME, tr("Query"));
            new ImageProvider("dialogs", "search").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Query and download changesets"));
        }

        protected void alertInvalidChangesetQuery() {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetQueryDialog.this,
                    tr("Please enter a valid changeset query URL first."),
                    tr("Illegal changeset query URL"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetQueryDialog#EnterAValidChangesetQueryUrlFirst")
            );
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                switch(tpQueryPanels.getSelectedIndex()) {
                case 0:
                    // currently, query specifications can't be invalid in the basic query panel.
                    // We select from a couple of predefined queries and there is always a query
                    // selected
                    break;
                case 1:
                    if (getChangesetQuery() == null) {
                        alertInvalidChangesetQuery();
                        pnlUrlBasedQueries.startUserInput();
                        return;
                    }
                    break;
                case 2:
                    if (getChangesetQuery() == null) {
                        pnlAdvancedQueries.displayMessageIfInvalid();
                        return;
                    }
                }
                setCanceled(false);
                setVisible(false);
            } catch (IllegalStateException e) {
                Logging.error(e);
                JOptionPane.showMessageDialog(ChangesetQueryDialog.this, e.getMessage(), tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class CancelAction extends AbstractAction {

        CancelAction() {
            putValue(NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Close the dialog and abort querying of changesets"));
        }

        public void cancel() {
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            cancel();
        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent arg0) {
            new CancelAction().cancel();
        }
    }
}
