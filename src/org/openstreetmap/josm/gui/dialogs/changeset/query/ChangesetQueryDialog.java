// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This is a modal dialog for entering query criteria to search for changesets.
 *
 */
public class ChangesetQueryDialog extends JDialog {

    private JTabbedPane tpQueryPanels;
    private BasicChangesetQueryPanel pnlBasicChangesetQueries;
    private UrlBasedQueryPanel pnlUrlBasedQueries;
    private AdvancedChangesetQueryPanel pnlAdvancedQueries;
    private boolean canceled;

    protected JPanel buildContentPanel() {
        tpQueryPanels = new JTabbedPane();
        tpQueryPanels.add(pnlBasicChangesetQueries = new BasicChangesetQueryPanel());
        tpQueryPanels.add(pnlUrlBasedQueries = new UrlBasedQueryPanel());
        tpQueryPanels.add(pnlAdvancedQueries = new AdvancedChangesetQueryPanel());

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

        // -- query action
        pnl.add(new SideButton(new QueryAction()));

        // -- cancel action
        pnl.add(new SideButton(new CancelAction()));

        // -- help action
        pnl.add(new SideButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/ChangesetQueryDialog"))));

        return pnl;
    }


    protected void build() {
        setTitle(tr("Query changesets"));
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(buildContentPanel(), BorderLayout.CENTER);
        cp.add(buildButtonPanel(), BorderLayout.SOUTH);

        // cancel on ESC
        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new CancelAction());

        // context sensitive help
        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Dialog/ChangesetQueryDialog"));

        addWindowListener(new WindowEventHandler());
    }


    public ChangesetQueryDialog(Dialog parent) {
        super(parent, ModalityType.DOCUMENT_MODAL);
        build();
    }

    public ChangesetQueryDialog(Frame parent) {
        super(parent, ModalityType.DOCUMENT_MODAL);
        build();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void initForUserInput() {
        pnlBasicChangesetQueries.init();
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

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
                            new Dimension(400,400)
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
        public QueryAction() {
            putValue(NAME, tr("Query"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "search"));
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
                JOptionPane.showMessageDialog(ChangesetQueryDialog.this, e.getMessage(), tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class CancelAction extends AbstractAction {

        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
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
