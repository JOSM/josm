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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This dialog can be used to select individual object for uploading.
 *
 *
 */
public class UploadSelectionDialog extends JDialog {

    private OsmPrimitiveList lstSelectedPrimitives;
    private OsmPrimitiveList lstDeletedPrimitives;
    private JSplitPane spLists;
    private boolean canceled;
    private SideButton btnContinue;

    protected JPanel buildSelectedPrimitivesPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        JLabel lbl = new JLabel(tr("<html>Mark modified objects <strong>from the current selection</strong> to be uploaded to the server.</html>"));
        lbl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(new JScrollPane(lstSelectedPrimitives = new OsmPrimitiveList()), BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildDeletedPrimitivesPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        JLabel lbl = new JLabel(tr("<html>Mark <strong>locally deleted objects</strong> to be deleted on the server.</html>"));
        lbl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(new JScrollPane(lstDeletedPrimitives = new OsmPrimitiveList()), BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout());
        ContinueAction continueAction = new ContinueAction();
        pnl.add(btnContinue = new SideButton(continueAction));
        btnContinue.setFocusable(true);
        lstDeletedPrimitives.getSelectionModel().addListSelectionListener(continueAction);
        lstSelectedPrimitives.getSelectionModel().addListSelectionListener(continueAction);

        pnl.add(new SideButton(new CancelAction()));
        pnl.add(new SideButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/UploadSelection"))));
        return pnl;
    }

    protected void build() {
        setLayout(new BorderLayout());
        spLists = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        spLists.setTopComponent(buildSelectedPrimitivesPanel());
        spLists.setBottomComponent(buildDeletedPrimitivesPanel());
        add(spLists, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                        spLists.setDividerLocation(0.5);
                        btnContinue.requestFocusInWindow();
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        setCanceled(true);
                    }
                }
        );
        setTitle(tr("Select objects to upload"));
        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Dialog/UploadSelection"));
    }

    public UploadSelectionDialog() {
        super(JOptionPane.getFrameForComponent(Main.parent), ModalityType.DOCUMENT_MODAL);
        build();
    }

    public void populate(Collection<OsmPrimitive> selected, Collection<OsmPrimitive> deleted) {
        if (selected != null) {
            lstSelectedPrimitives.getOsmPrimitiveListModel().setPrimitives(new ArrayList<OsmPrimitive>(selected));
            if (!selected.isEmpty()) {
                lstSelectedPrimitives.getSelectionModel().setSelectionInterval(0, selected.size()-1);
            } else {
                lstSelectedPrimitives.getSelectionModel().clearSelection();
            }
        } else {
            lstSelectedPrimitives.getOsmPrimitiveListModel().setPrimitives(null);
            lstSelectedPrimitives.getSelectionModel().clearSelection();
        }

        if (deleted != null) {
            lstDeletedPrimitives.getOsmPrimitiveListModel().setPrimitives(new ArrayList<OsmPrimitive>(deleted));
        } else {
            lstDeletedPrimitives.getOsmPrimitiveListModel().setPrimitives(null);
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public List<OsmPrimitive> getSelectedPrimitives() {
        List<OsmPrimitive> ret  = new ArrayList<OsmPrimitive>();
        ret.addAll(lstSelectedPrimitives.getOsmPrimitiveListModel().getPrimitives(lstSelectedPrimitives.getSelectedIndices()));
        ret.addAll(lstDeletedPrimitives.getOsmPrimitiveListModel().getPrimitives(lstDeletedPrimitives.getSelectedIndices()));
        return ret;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(200,400)
                    )
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    static class OsmPrimitiveList extends JList {
        protected void init() {
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            setCellRenderer(new OsmPrimitivRenderer());
        }

        public OsmPrimitiveList() {
            super(new OsmPrimitiveListModel());
            init();
        }

        public OsmPrimitiveList(OsmPrimitiveListModel model) {
            super(model);
            init();
        }

        public OsmPrimitiveListModel getOsmPrimitiveListModel() {
            return (OsmPrimitiveListModel)getModel();
        }
    }

    static class OsmPrimitiveListModel extends AbstractListModel {
        private List<OsmPrimitive> data;

        public OsmPrimitiveListModel() {
        }

        protected void sort() {
            if (data == null)
                return;
            Collections.sort(
                    data,
                    new Comparator<OsmPrimitive>() {
                        private DefaultNameFormatter formatter = DefaultNameFormatter.getInstance();
                        @Override
                        public int compare(OsmPrimitive o1, OsmPrimitive o2) {
                            int ret = OsmPrimitiveType.from(o1).compareTo(OsmPrimitiveType.from(o2));
                            if (ret != 0) return ret;
                            return o1.getDisplayName(formatter).compareTo(o1.getDisplayName(formatter));
                        }
                    }
            );
        }

        public OsmPrimitiveListModel(List<OsmPrimitive> data) {
            setPrimitives(data);
        }

        public void setPrimitives(List<OsmPrimitive> data) {
            this.data = data;
            sort();
            if (data != null) {
                fireContentsChanged(this,0, data.size());
            } else {
                fireContentsChanged(this, 0,0);
            }
        }

        @Override
        public Object getElementAt(int index) {
            if (data == null)
                return null;
            return data.get(index);
        }

        @Override
        public int getSize() {
            if (data == null)
                return 0;
            return data.size();
        }

        public List<OsmPrimitive> getPrimitives(int [] indices) {
            if (indices == null || indices.length == 0)
                return Collections.emptyList();
            List<OsmPrimitive> ret = new ArrayList<OsmPrimitive>(indices.length);
            for (int i: indices) {
                if (i < 0) {
                    continue;
                }
                ret.add(data.get(i));
            }
            return ret;
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel uploading"));
            putValue(Action.NAME, tr("Cancel"));
            putValue(Action.SMALL_ICON, ImageProvider.get("", "cancel"));
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
            getRootPane().getActionMap().put("ESCAPE", this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCanceled(true);
            setVisible(false);
        }
    }

    class ContinueAction extends AbstractAction implements ListSelectionListener {
        public ContinueAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Continue uploading"));
            putValue(Action.NAME, tr("Continue"));
            putValue(Action.SMALL_ICON, ImageProvider.get("", "upload"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCanceled(false);
            setVisible(false);
        }

        protected void updateEnabledState() {
            setEnabled(lstSelectedPrimitives.getSelectedIndex() >=0
                    || lstDeletedPrimitives.getSelectedIndex() >= 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }
}
