// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

/**
 * This dialog lets the user select changesets from a list of changesets.
 * @since 2115
 */
public class CloseChangesetDialog extends JDialog {

    /** the list */
    private JList<Changeset> lstOpenChangesets;
    /** true if the user canceled the dialog */
    private boolean canceled;
    /** the list model */
    private DefaultListModel<Changeset> model;

    private SideButton btnCloseChangesets;

    protected JPanel buildTopPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnl.add(new JLabel(tr("<html>Please select the changesets you want to close</html>")), BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildCenterPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        model = new DefaultListModel<>();
        lstOpenChangesets = new JList<>(model);
        pnl.add(new JScrollPane(lstOpenChangesets), BorderLayout.CENTER);
        lstOpenChangesets.setCellRenderer(new ChangesetCellRenderer());
        return pnl;
    }

    protected JPanel buildSouthPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // -- close action
        CloseAction closeAction = new CloseAction();
        lstOpenChangesets.addListSelectionListener(closeAction);
        btnCloseChangesets = new SideButton(closeAction);
        pnl.add(btnCloseChangesets);
        InputMapUtils.enableEnter(btnCloseChangesets);

        // -- cancel action
        SideButton btn = new SideButton(new CancelAction());
        pnl.add(btn);
        btn.setFocusable(true);
        return pnl;
    }

    protected void build() {
        setTitle(tr("Open changesets"));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildTopPanel(), BorderLayout.NORTH);
        getContentPane().add(buildCenterPanel(), BorderLayout.CENTER);
        getContentPane().add(buildSouthPanel(), BorderLayout.SOUTH);

        InputMapUtils.addEscapeAction(getRootPane(), new CancelAction());
        addWindowListener(new WindowEventHandler());
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(Main.parent, new Dimension(300, 300))
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Constructs a new {@code CloseChangesetDialog}.
     */
    public CloseChangesetDialog() {
        super(GuiHelper.getFrameForComponent(Main.parent), ModalityType.DOCUMENT_MODAL);
        build();
    }

    class CloseAction extends AbstractAction implements ListSelectionListener {
        CloseAction() {
            putValue(NAME, tr("Close changesets"));
            putValue(SMALL_ICON, ImageProvider.get("closechangeset"));
            putValue(SHORT_DESCRIPTION, tr("Close the selected open changesets"));
            refreshEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCanceled(false);
            setVisible(false);
        }

        protected void refreshEnabledState() {
            List<Changeset> list = lstOpenChangesets.getSelectedValuesList();
            setEnabled(list != null && !list.isEmpty());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            refreshEnabledState();
        }
    }

    class CancelAction extends AbstractAction {

        CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Cancel closing of changesets"));
        }

        public void cancel() {
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    }

    class WindowEventHandler extends WindowAdapter {

        @Override
        public void windowActivated(WindowEvent arg0) {
            btnCloseChangesets.requestFocusInWindow();
        }

        @Override
        public void windowClosing(WindowEvent arg0) {
            new CancelAction().cancel();
        }

    }

    /**
     * Replies true if this dialog was canceled
     * @return true if this dialog was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets whether this dialog is canceled
     *
     * @param canceled true, if this dialog is canceld
     */
    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Sets the collection of changesets to be displayed
     *
     * @param changesets the collection of changesets. Assumes an empty collection if null
     */
    public void setChangesets(Collection<Changeset> changesets) {
        if (changesets == null) {
            changesets = new ArrayList<>();
        }
        model.removeAllElements();
        for (Changeset cs: changesets) {
            model.addElement(cs);
        }
        if (!changesets.isEmpty()) {
            lstOpenChangesets.getSelectionModel().setSelectionInterval(0, changesets.size()-1);
        }
    }

    /**
     * Replies a collection with the changesets the user selected.
     * Never null, but may be empty.
     *
     * @return a collection with the changesets the user selected.
     */
    public Collection<Changeset> getSelectedChangesets() {
        return lstOpenChangesets.getSelectedValuesList();
    }
}
