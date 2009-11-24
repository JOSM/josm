// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * This dialog lets the user select changesets from a list of changesets.
 *
 */
public class CloseChangesetDialog extends JDialog {

    /** the list */
    private JList lstOpenChangesets;
    /** true if the user cancelled the dialog */
    private boolean canceled;
    /** the list model */
    private DefaultListModel model;

    protected JPanel buildTopPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnl.setLayout(new BorderLayout());
        pnl.add(new JLabel(tr("<html>Please select the changesets you want to close</html>")), BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildCenterPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        model = new DefaultListModel();
        pnl.add(new JScrollPane(lstOpenChangesets = new JList(model)), BorderLayout.CENTER);
        lstOpenChangesets.setCellRenderer(new ChangesetCellRenderer());
        return pnl;
    }

    protected JPanel buildSouthPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        // -- close action
        CloseAction closeAction = new CloseAction();
        lstOpenChangesets.addListSelectionListener(closeAction);
        pnl.add(new SideButton(closeAction));
        pnl.add(new SideButton(new CancelAction()));
        return pnl;
    }

    protected void build() {
        setTitle(tr("Open changesets"));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildTopPanel(), BorderLayout.NORTH);
        getContentPane().add(buildCenterPanel(), BorderLayout.CENTER);
        getContentPane().add(buildSouthPanel(), BorderLayout.SOUTH);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(Main.parent, new Dimension(300,300))
            ).apply(this);
        } else {
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    public CloseChangesetDialog() {
        super(JOptionPane.getFrameForComponent(Main.parent), true /* modal */);
        build();
    }

    class CloseAction extends AbstractAction implements ListSelectionListener {
        public CloseAction() {
            putValue(NAME, tr("Close changesets"));
            putValue(SMALL_ICON, ImageProvider.get("closechangeset"));
            putValue(SHORT_DESCRIPTION, tr("Close the selected open changesets"));
            refreshEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            setCanceled(false);
            setVisible(false);
        }

        protected void refreshEnabledState() {
            setEnabled(lstOpenChangesets.getSelectedValues() != null && lstOpenChangesets.getSelectedValues().length > 0);
        }

        public void valueChanged(ListSelectionEvent e) {
            refreshEnabledState();
        }
    }

    class CancelAction extends AbstractAction {

        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Cancel closing of changesets"));
        }

        public void actionPerformed(ActionEvent e) {
            setCanceled(true);
            setVisible(false);
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
            changesets = new ArrayList<Changeset>();
        }
        model.removeAllElements();
        for (Changeset cs: changesets) {
            model.addElement(cs);
        }
    }

    /**
     * Replies a collection with the changesets the user selected.
     * Never null, but may be empty.
     *
     * @return a collection with the changesets the user selected.
     */
    public Collection<Changeset> getSelectedChangesets() {
        Object [] sel = lstOpenChangesets.getSelectedValues();
        ArrayList<Changeset> ret = new ArrayList<Changeset>();
        for (Object o: sel) {
            ret.add((Changeset)o);
        }
        return ret;
    }
}
