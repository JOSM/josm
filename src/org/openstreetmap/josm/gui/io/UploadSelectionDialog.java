// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

/**
 * This dialog can be used to select individual object for uploading.
 *
 * @since 2250
 */
public class UploadSelectionDialog extends JDialog {

    private final OsmPrimitiveList lstSelectedPrimitives = new OsmPrimitiveList();
    private final OsmPrimitiveList lstDeletedPrimitives = new OsmPrimitiveList();
    private JSplitPane spLists;
    private boolean canceled;
    private JButton btnContinue;

    /**
     * Constructs a new {@code UploadSelectionDialog}.
     */
    public UploadSelectionDialog() {
        super(GuiHelper.getFrameForComponent(MainApplication.getMainFrame()), ModalityType.DOCUMENT_MODAL);
        build();
    }

    protected JPanel buildSelectedPrimitivesPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(
                tr("<html>Mark modified objects <strong>from the current selection</strong> to be uploaded to the server.</html>"));
        lbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(new JScrollPane(lstSelectedPrimitives), BorderLayout.CENTER);
        lbl.setLabelFor(lstSelectedPrimitives);
        return pnl;
    }

    protected JPanel buildDeletedPrimitivesPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(tr("<html>Mark <strong>locally deleted objects</strong> to be deleted on the server.</html>"));
        lbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(new JScrollPane(lstDeletedPrimitives), BorderLayout.CENTER);
        lbl.setLabelFor(lstDeletedPrimitives);
        return pnl;
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout());
        ContinueAction continueAction = new ContinueAction();
        btnContinue = new JButton(continueAction);
        pnl.add(btnContinue);
        btnContinue.setFocusable(true);
        lstDeletedPrimitives.getSelectionModel().addListSelectionListener(continueAction);
        lstSelectedPrimitives.getSelectionModel().addListSelectionListener(continueAction);

        pnl.add(new JButton(new CancelAction()));
        pnl.add(new JButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/UploadSelection"))));
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

    public void populate(Collection<OsmPrimitive> selected, Collection<OsmPrimitive> deleted) {
        if (selected != null) {
            lstSelectedPrimitives.getOsmPrimitiveListModel().setPrimitives(new ArrayList<>(selected));
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
            lstDeletedPrimitives.getOsmPrimitiveListModel().setPrimitives(new ArrayList<>(deleted));
        } else {
            lstDeletedPrimitives.getOsmPrimitiveListModel().setPrimitives(null);
        }
    }

    /**
     * See if the user pressed the cancel button
     * @return <code>true</code> if the user canceled the upload
     */
    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Gets the list of primitives the user selected
     * @return The primitives
     */
    public List<OsmPrimitive> getSelectedPrimitives() {
        List<OsmPrimitive> ret = new ArrayList<>();
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
                            MainApplication.getMainFrame(),
                            new Dimension(200, 400)
                    )
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    static class OsmPrimitiveList extends JList<OsmPrimitive> {
        OsmPrimitiveList() {
            this(new OsmPrimitiveListModel());
        }

        OsmPrimitiveList(OsmPrimitiveListModel model) {
            super(model);
            init();
        }

        protected void init() {
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            setCellRenderer(new PrimitiveRenderer());
        }

        public OsmPrimitiveListModel getOsmPrimitiveListModel() {
            return (OsmPrimitiveListModel) getModel();
        }
    }

    static class OsmPrimitiveListModel extends AbstractListModel<OsmPrimitive> {
        static final class OsmPrimitiveComparator implements Comparator<OsmPrimitive>, Serializable {
            @Override
            public int compare(OsmPrimitive o1, OsmPrimitive o2) {
                int ret = OsmPrimitiveType.from(o1).compareTo(OsmPrimitiveType.from(o2));
                if (ret != 0)
                    return ret;
                DefaultNameFormatter formatter = DefaultNameFormatter.getInstance();
                return o1.getDisplayName(formatter).compareTo(o1.getDisplayName(formatter));
            }
        }

        private transient List<OsmPrimitive> data;

        protected void sort() {
            if (data != null)
                data.sort(new OsmPrimitiveComparator());
        }

        public void setPrimitives(List<OsmPrimitive> data) {
            this.data = data;
            sort();
            if (data != null) {
                fireContentsChanged(this, 0, data.size());
            } else {
                fireContentsChanged(this, 0, 0);
            }
        }

        @Override
        public OsmPrimitive getElementAt(int index) {
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

        public List<OsmPrimitive> getPrimitives(int... indices) {
            if (indices == null || indices.length == 0)
                return Collections.emptyList();
            List<OsmPrimitive> ret = new ArrayList<>(indices.length);
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
        CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel uploading"));
            putValue(Action.NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            InputMapUtils.addEscapeAction(getRootPane(), this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCanceled(true);
            setVisible(false);
        }
    }

    class ContinueAction extends AbstractAction implements ListSelectionListener {
        ContinueAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Continue uploading"));
            putValue(Action.NAME, tr("Continue"));
            new ImageProvider("upload").getResource().attachImageIcon(this);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCanceled(false);
            setVisible(false);
        }

        protected void updateEnabledState() {
            setEnabled(lstSelectedPrimitives.getSelectedIndex() >= 0
                    || lstDeletedPrimitives.getSelectedIndex() >= 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }
}
