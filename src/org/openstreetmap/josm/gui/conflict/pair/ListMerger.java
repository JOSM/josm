package org.openstreetmap.josm.gui.conflict.pair;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Adjustable;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A UI component for resolving conflicts in two lists of entries of type T.
 *
 * @param T  the type of the entries
 * @see ListMergeModel
 */
public abstract class ListMerger<T> extends JPanel implements PropertyChangeListener, Observer {
    //private static final Logger logger = Logger.getLogger(ListMerger.class.getName());

    protected JTable myEntriesTable;
    protected JTable mergedEntriesTable;
    protected JTable theirEntriesTable;

    protected ListMergeModel<T> model;

    private CopyStartLeftAction copyStartLeftAction;
    private CopyBeforeCurrentLeftAction copyBeforeCurrentLeftAction;
    private CopyAfterCurrentLeftAction copyAfterCurrentLeftAction;
    private CopyEndLeftAction copyEndLeftAction;
    private CopyAllLeft copyAllLeft;

    private CopyStartRightAction copyStartRightAction;
    private CopyBeforeCurrentRightAction copyBeforeCurrentRightAction;
    private CopyAfterCurrentRightAction copyAfterCurrentRightAction;
    private CopyEndRightAction copyEndRightAction;
    private CopyAllRight copyAllRight;

    private MoveUpMergedAction moveUpMergedAction;
    private MoveDownMergedAction moveDownMergedAction;
    private RemoveMergedAction removeMergedAction;
    private FreezeAction freezeAction;

    private AdjustmentSynchronizer adjustmentSynchronizer;

    private  JCheckBox cbLockMyScrolling;
    private  JCheckBox cbLockMergedScrolling;
    private  JCheckBox cbLockTheirScrolling;

    private  JLabel lblMyVersion;
    private  JLabel lblMergedVersion;
    private  JLabel lblTheirVersion;

    private  JLabel lblFrozenState;

    abstract protected JScrollPane buildMyElementsTable();
    abstract protected JScrollPane buildMergedElementsTable();
    abstract protected JScrollPane buildTheirElementsTable();

    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        if (adjustmentSynchronizer == null) {
            adjustmentSynchronizer = new AdjustmentSynchronizer();
        }
        return pane;
    }

    protected void wireActionsToSelectionModels() {
        myEntriesTable.getSelectionModel().addListSelectionListener(copyStartLeftAction);

        myEntriesTable.getSelectionModel().addListSelectionListener(copyBeforeCurrentLeftAction);
        mergedEntriesTable.getSelectionModel().addListSelectionListener(copyBeforeCurrentLeftAction);

        myEntriesTable.getSelectionModel().addListSelectionListener(copyAfterCurrentLeftAction);
        mergedEntriesTable.getSelectionModel().addListSelectionListener(copyAfterCurrentLeftAction);

        myEntriesTable.getSelectionModel().addListSelectionListener(copyEndLeftAction);

        theirEntriesTable.getSelectionModel().addListSelectionListener(copyStartRightAction);

        theirEntriesTable.getSelectionModel().addListSelectionListener(copyBeforeCurrentRightAction);
        mergedEntriesTable.getSelectionModel().addListSelectionListener(copyBeforeCurrentRightAction);

        theirEntriesTable.getSelectionModel().addListSelectionListener(copyAfterCurrentRightAction);
        mergedEntriesTable.getSelectionModel().addListSelectionListener(copyAfterCurrentRightAction);

        theirEntriesTable.getSelectionModel().addListSelectionListener(copyEndRightAction);

        mergedEntriesTable.getSelectionModel().addListSelectionListener(moveUpMergedAction);
        mergedEntriesTable.getSelectionModel().addListSelectionListener(moveDownMergedAction);
        mergedEntriesTable.getSelectionModel().addListSelectionListener(removeMergedAction);

        model.addObserver(copyAllLeft);
        model.addObserver(copyAllRight);
        model.addPropertyChangeListener(copyAllLeft);
        model.addPropertyChangeListener(copyAllRight);
    }

    protected JPanel buildLeftButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 0;
        copyStartLeftAction = new CopyStartLeftAction();
        JButton btn = new JButton(copyStartLeftAction);
        btn.setName("button.copystartleft");
        pnl.add(btn, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        copyBeforeCurrentLeftAction = new CopyBeforeCurrentLeftAction();
        btn = new JButton(copyBeforeCurrentLeftAction);
        btn.setName("button.copybeforecurrentleft");
        pnl.add(btn, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        copyAfterCurrentLeftAction = new CopyAfterCurrentLeftAction();
        btn = new JButton(copyAfterCurrentLeftAction);
        btn.setName("button.copyaftercurrentleft");
        pnl.add(btn, gc);

        gc.gridx = 0;
        gc.gridy = 3;
        copyEndLeftAction = new CopyEndLeftAction();
        btn = new JButton(copyEndLeftAction);
        btn.setName("button.copyendleft");
        pnl.add(btn, gc);

        gc.gridx = 0;
        gc.gridy = 4;
        copyAllLeft = new CopyAllLeft();
        btn = new JButton(copyAllLeft);
        btn.setName("button.copyallleft");
        pnl.add(btn, gc);

        return pnl;
    }

    protected JPanel buildRightButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 0;
        copyStartRightAction = new CopyStartRightAction();
        pnl.add(new JButton(copyStartRightAction), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        copyBeforeCurrentRightAction = new CopyBeforeCurrentRightAction();
        pnl.add(new JButton(copyBeforeCurrentRightAction), gc);

        gc.gridx = 0;
        gc.gridy = 2;
        copyAfterCurrentRightAction = new CopyAfterCurrentRightAction();
        pnl.add(new JButton(copyAfterCurrentRightAction), gc);

        gc.gridx = 0;
        gc.gridy = 3;
        copyEndRightAction = new CopyEndRightAction();
        pnl.add(new JButton(copyEndRightAction), gc);

        gc.gridx = 0;
        gc.gridy = 4;
        copyAllRight = new CopyAllRight();
        pnl.add(new JButton(copyAllRight), gc);

        return pnl;
    }

    protected JPanel buildMergedListControlButtons() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.3;
        gc.weighty = 0.0;
        moveUpMergedAction = new MoveUpMergedAction();
        pnl.add(new JButton(moveUpMergedAction), gc);

        gc.gridx = 1;
        gc.gridy = 0;
        moveDownMergedAction = new MoveDownMergedAction();
        pnl.add(new JButton(moveDownMergedAction), gc);

        gc.gridx = 2;
        gc.gridy = 0;
        removeMergedAction = new RemoveMergedAction();
        pnl.add(new JButton(removeMergedAction), gc);

        return pnl;
    }

    protected JPanel buildAdjustmentLockControlPanel(JCheckBox cb) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        panel.add(new JLabel(tr("lock scrolling")));
        panel.add(cb);
        return panel;
    }

    protected JPanel buildComparePairSelectionPanel() {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel(tr("Compare ")));
        JComboBox cbComparePair =new JComboBox(model.getComparePairListModel());
        cbComparePair.setRenderer(new ComparePairListCellRenderer());
        p.add(cbComparePair);
        return p;
    }

    protected JPanel buildFrozeStateControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        lblFrozenState = new JLabel();
        p.add(lblFrozenState);
        freezeAction = new FreezeAction();
        JToggleButton btn = new JToggleButton(freezeAction);
        freezeAction.adapt(btn);
        btn.setName("button.freeze");
        p.add(btn);

        return p;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // ------------------
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(10,0,0,0);
        lblMyVersion = new JLabel(tr("My version"));
        lblMyVersion.setToolTipText(tr("List of elements in my dataset, i.e. the local dataset"));
        add(lblMyVersion, gc);

        gc.gridx = 2;
        gc.gridy = 0;
        lblMergedVersion = new JLabel(tr("Merged version"));
        lblMergedVersion.setToolTipText(tr("List of merged elements. They will replace the my elements when the merge decisions are applied."));
        add(lblMergedVersion, gc);

        gc.gridx = 4;
        gc.gridy = 0;
        lblTheirVersion = new JLabel(tr("Their version"));
        lblTheirVersion.setToolTipText(tr("List of elements in their dataset, i.e. the server dataset"));
        add(lblTheirVersion, gc);

        // ------------------------------
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,0,0,0);
        cbLockMyScrolling = new JCheckBox();
        cbLockMyScrolling.setName("checkbox.lockmyscrolling");
        add(buildAdjustmentLockControlPanel(cbLockMyScrolling), gc);

        gc.gridx = 2;
        gc.gridy = 1;
        cbLockMergedScrolling = new JCheckBox();
        cbLockMergedScrolling.setName("checkbox.lockmergedscrolling");
        add(buildAdjustmentLockControlPanel(cbLockMergedScrolling), gc);

        gc.gridx = 4;
        gc.gridy = 1;
        cbLockTheirScrolling = new JCheckBox();
        cbLockTheirScrolling.setName("checkbox.locktheirscrolling");
        add(buildAdjustmentLockControlPanel(cbLockTheirScrolling), gc);

        // --------------------------------
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.33;
        gc.weighty = 1.0;
        gc.insets = new Insets(0,0,0,0);
        JScrollPane pane = buildMyElementsTable();
        adjustmentSynchronizer.adapt(cbLockMyScrolling, pane.getVerticalScrollBar());
        add(pane, gc);

        gc.gridx = 1;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(buildLeftButtonPanel(), gc);

        gc.gridx = 2;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        pane = buildMergedElementsTable();
        adjustmentSynchronizer.adapt(cbLockMergedScrolling, pane.getVerticalScrollBar());
        add(pane, gc);

        gc.gridx = 3;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(buildRightButtonPanel(), gc);

        gc.gridx = 4;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        pane = buildTheirElementsTable();
        adjustmentSynchronizer.adapt(cbLockTheirScrolling, pane.getVerticalScrollBar());
        add(pane, gc);

        // ----------------------------------
        gc.gridx = 2;
        gc.gridy = 3;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(buildMergedListControlButtons(), gc);

        // -----------------------------------
        gc.gridx = 0;
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(buildComparePairSelectionPanel(), gc);

        gc.gridx = 2;
        gc.gridy = 4;
        gc.gridwidth = 3;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(buildFrozeStateControlPanel(), gc);

        wireActionsToSelectionModels();
    }

    public ListMerger(ListMergeModel<T> model) {
        this.model = model;
        model.addObserver(this);
        build();
        model.addPropertyChangeListener(this);
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes at the beginning of the list of merged nodes.
     *
     */
    class CopyStartLeftAction extends AbstractAction implements ListSelectionListener {

        public CopyStartLeftAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copystartleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("> top"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected nodes to the start of the merged node list"));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] rows = myEntriesTable.getSelectedRows();
            model.copyMyToTop(rows);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!myEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes at the end of the list of merged nodes.
     *
     */
    class CopyEndLeftAction extends AbstractAction implements ListSelectionListener {

        public CopyEndLeftAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copyendleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("> bottom"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected elements to the end of the list of merged elements."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] rows = myEntriesTable.getSelectedRows();
            model.copyMyToEnd(rows);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!myEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes before the first selected row in the list of merged nodes.
     *
     */
    class CopyBeforeCurrentLeftAction extends AbstractAction implements ListSelectionListener {

        public CopyBeforeCurrentLeftAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copybeforecurrentleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "> before");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected elements before the first selected element in the list of merged elements."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = myEntriesTable.getSelectedRows();
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int current = mergedRows[0];
            model.copyMyBeforeCurrent(myRows, current);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !myEntriesTable.getSelectionModel().isSelectionEmpty()
                    && ! mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes after the first selected row in the list of merged nodes.
     *
     */
    class CopyAfterCurrentLeftAction extends AbstractAction implements ListSelectionListener {

        public CopyAfterCurrentLeftAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copyaftercurrentleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "> after");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected elements after the first selected element in the list of merged elements."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = myEntriesTable.getSelectedRows();
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int current = mergedRows[0];
            model.copyMyAfterCurrent(myRows, current);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !myEntriesTable.getSelectionModel().isSelectionEmpty()
                    && ! mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    class CopyStartRightAction extends AbstractAction implements ListSelectionListener {

        public CopyStartRightAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copystartright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< top");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected element to the start of the list of merged elements."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] rows = theirEntriesTable.getSelectedRows();
            model.copyTheirToTop(rows);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!theirEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    class CopyEndRightAction extends AbstractAction implements ListSelectionListener {

        public CopyEndRightAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copyendright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< bottom");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected elements to the end of the list of merged elements."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] rows = theirEntriesTable.getSelectedRows();
            model.copyTheirToEnd(rows);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!theirEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    class CopyBeforeCurrentRightAction extends AbstractAction implements ListSelectionListener {

        public CopyBeforeCurrentRightAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copybeforecurrentright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< before");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected elements before the first selected element in the list of merged elements."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = theirEntriesTable.getSelectedRows();
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int current = mergedRows[0];
            model.copyTheirBeforeCurrent(myRows, current);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !theirEntriesTable.getSelectionModel().isSelectionEmpty()
                    && ! mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    class CopyAfterCurrentRightAction extends AbstractAction implements ListSelectionListener {

        public CopyAfterCurrentRightAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "copyaftercurrentright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< after");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected element after the first selected element in the list of merged elements"));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = theirEntriesTable.getSelectedRows();
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int current = mergedRows[0];
            model.copyTheirAfterCurrent(myRows, current);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !theirEntriesTable.getSelectionModel().isSelectionEmpty()
                    && ! mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    class CopyAllLeft extends AbstractAction implements Observer, PropertyChangeListener {

        public CopyAllLeft() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "useallleft.png");
            putValue(Action.SMALL_ICON, icon);
            putValue(Action.SHORT_DESCRIPTION, tr("Use all my elements"));
        }

        public void actionPerformed(ActionEvent arg0) {
            model.copyAll(ListRole.MY_ENTRIES);
            model.setFrozen(true);
        }

        private void updateEnabledState() {
            setEnabled(model.getMergedEntries().isEmpty() && !model.isFrozen());
        }

        public void update(Observable o, Object arg) {
            updateEnabledState();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    class CopyAllRight extends AbstractAction implements Observer, PropertyChangeListener {

        public CopyAllRight() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "useallright.png");
            putValue(Action.SMALL_ICON, icon);
            putValue(Action.SHORT_DESCRIPTION, tr("Use all their elements"));
        }

        public void actionPerformed(ActionEvent arg0) {
            model.copyAll(ListRole.THEIR_ENTRIES);
            model.setFrozen(true);
        }

        private void updateEnabledState() {
            setEnabled(model.getMergedEntries().isEmpty() && !model.isFrozen());
        }

        public void update(Observable o, Object arg) {
            updateEnabledState();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    class MoveUpMergedAction extends AbstractAction implements ListSelectionListener {

        public MoveUpMergedAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "moveup.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("Up"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Move up the selected elements by one position."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            model.moveUpMerged(rows);
        }

        public void valueChanged(ListSelectionEvent e) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            setEnabled(
                    rows != null
                    && rows.length > 0
                    && rows[0] != 0
            );
        }
    }

    /**
     * Action for moving the currently selected entries in the list of merged entries
     * one position down
     *
     */
    class MoveDownMergedAction extends AbstractAction implements ListSelectionListener {

        public MoveDownMergedAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "movedown.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("Down"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Move down the selected entries by one position."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            model.moveDownMerged(rows);
        }

        public void valueChanged(ListSelectionEvent e) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            setEnabled(
                    rows != null
                    && rows.length > 0
                    && rows[rows.length -1] != mergedEntriesTable.getRowCount() -1
            );
        }
    }

    /**
     * Action for removing the selected entries in the list of merged entries
     * from the list of merged entries.
     *
     */
    class RemoveMergedAction extends AbstractAction implements ListSelectionListener {

        public RemoveMergedAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "remove.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("Remove"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Remove the selected entries from the list of merged elements."));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            model.removeMerged(rows);
        }

        public void valueChanged(ListSelectionEvent e) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            setEnabled(
                    rows != null
                    && rows.length > 0
            );
        }
    }

    static public interface FreezeActionProperties {
        String PROP_SELECTED = FreezeActionProperties.class.getName() + ".selected";
    }

    /**
     * Action for freezing the current state of the list merger
     *
     */
    class FreezeAction extends AbstractAction implements ItemListener, FreezeActionProperties  {

        public FreezeAction() {
            putValue(Action.NAME, tr("Freeze"));
            putValue(Action.SHORT_DESCRIPTION, tr("Freeze the current list of merged elements."));
            putValue(PROP_SELECTED, false);
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent arg0) {
            // do nothing
        }

        /**
         * Java 1.5 doesn't known Action.SELECT_KEY. Wires a toggle button to this action
         * such that the action gets notified about item state changes and the button gets
         * notified about selection state changes of the action.
         *
         * @param btn a toggle button
         */
        public void adapt(final JToggleButton btn) {
            btn.addItemListener(this);
            addPropertyChangeListener(
                    new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals(PROP_SELECTED)) {
                                btn.setSelected((Boolean)evt.getNewValue());
                            }
                        }
                    }
            );
        }

        public void itemStateChanged(ItemEvent e) {
            int state = e.getStateChange();
            if (state == ItemEvent.SELECTED) {
                putValue(Action.NAME, tr("Unfreeze"));
                putValue(Action.SHORT_DESCRIPTION, tr("Unfreeze the list of merged elements and start merging."));
                model.setFrozen(true);
            } else if (state == ItemEvent.DESELECTED) {
                putValue(Action.NAME, tr("Freeze"));
                putValue(Action.SHORT_DESCRIPTION, tr("Freeze the current list of merged elements."));
                model.setFrozen(false);
            }
            boolean isSelected = (Boolean)getValue(PROP_SELECTED);
            if (isSelected != (e.getStateChange() == ItemEvent.SELECTED)) {
                putValue(PROP_SELECTED, e.getStateChange() == ItemEvent.SELECTED);
            }

        }
    }

    protected void handlePropertyChangeFrozen(boolean oldValue, boolean newValue) {
        myEntriesTable.getSelectionModel().clearSelection();
        myEntriesTable.setEnabled(!newValue);
        theirEntriesTable.getSelectionModel().clearSelection();
        theirEntriesTable.setEnabled(!newValue);
        mergedEntriesTable.getSelectionModel().clearSelection();
        mergedEntriesTable.setEnabled(!newValue);
        freezeAction.putValue(FreezeActionProperties.PROP_SELECTED, newValue);
        if (newValue) {
            lblFrozenState.setText(
                    tr("<html>Click <strong>{0}</strong> to start merging my and their entries.</html>",
                            freezeAction.getValue(Action.NAME))
            );
        } else {
            lblFrozenState.setText(
                    tr("<html>Click <strong>{0}</strong> to finish merging my and their entries.</html>",
                            freezeAction.getValue(Action.NAME))
            );
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ListMergeModel.FROZEN_PROP)) {
            handlePropertyChangeFrozen((Boolean)evt.getOldValue(), (Boolean)evt.getNewValue());
        }
    }

    public ListMergeModel<T> getModel() {
        return model;
    }

    public void update(Observable o, Object arg) {
        lblMyVersion.setText(
                trn("My version ({0} entry)", "My version ({0} entries)", model.getMyEntriesSize(), model.getMyEntriesSize())
        );
        lblMergedVersion.setText(
                trn("Merged version ({0} entry)", "Merged version ({0} entries)", model.getMergedEntriesSize(), model.getMergedEntriesSize())
        );
        lblTheirVersion.setText(
                trn("Their version ({0} entry)", "Their version ({0} entries)", model.getTheirEntriesSize(), model.getTheirEntriesSize())
        );
    }

    /**
     * Synchronizes scrollbar adjustments between a set of
     * {@see Adjustable}s. Whenever the adjustment of one of
     * the registerd Adjustables is updated the adjustment of
     * the other registered Adjustables is adjusted too.
     *
     */
    class AdjustmentSynchronizer implements AdjustmentListener {

        private final  ArrayList<Adjustable> synchronizedAdjustables;
        private final  HashMap<Adjustable, Boolean> enabledMap;

        private final Observable observable;

        public AdjustmentSynchronizer() {
            synchronizedAdjustables = new ArrayList<Adjustable>();
            enabledMap = new HashMap<Adjustable, Boolean>();
            observable = new Observable();
        }

        /**
         * registers an {@see Adjustable} for participation in synchronized
         * scrolling.
         *
         * @param adjustable the adjustable
         */
        public void participateInSynchronizedScrolling(Adjustable adjustable) {
            if (adjustable == null)
                return;
            if (synchronizedAdjustables.contains(adjustable))
                return;
            synchronizedAdjustables.add(adjustable);
            setParticipatingInSynchronizedScrolling(adjustable, true);
            adjustable.addAdjustmentListener(this);
        }

        /**
         * event handler for {@see AdjustmentEvent}s
         *
         */
        public void adjustmentValueChanged(AdjustmentEvent e) {
            if (! enabledMap.get(e.getAdjustable()))
                return;
            for (Adjustable a : synchronizedAdjustables) {
                if (a != e.getAdjustable() && isParticipatingInSynchronizedScrolling(a)) {
                    a.setValue(e.getValue());
                }
            }
        }

        /**
         * sets whether adjustable participates in adjustment synchronization
         * or not
         *
         * @param adjustable the adjustable
         */
        protected void setParticipatingInSynchronizedScrolling(Adjustable adjustable, boolean isParticipating) {
            CheckParameterUtil.ensureParameterNotNull(adjustable, "adjustable");
            if (! synchronizedAdjustables.contains(adjustable))
                throw new IllegalStateException(tr("Adjustable {0} not registered yet. Cannot set participation in synchronized adjustment.", adjustable));

            enabledMap.put(adjustable, isParticipating);
            observable.notifyObservers();
        }

        /**
         * returns true if an adjustable is participating in synchronized scrolling
         *
         * @param adjustable the adjustable
         * @return true, if the adjustable is participating in synchronized scrolling, false otherwise
         * @throws IllegalStateException thrown, if adjustable is not registered for synchronized scrolling
         */
        protected boolean isParticipatingInSynchronizedScrolling(Adjustable adjustable) throws IllegalStateException {
            if (! synchronizedAdjustables.contains(adjustable))
                throw new IllegalStateException(tr("Adjustable {0} not registered yet.", adjustable));

            return enabledMap.get(adjustable);
        }

        /**
         * wires a {@see JCheckBox} to  the adjustment synchronizer, in such a way  that:
         * <li>
         *   <ol>state changes in the checkbox control whether the adjustable participates
         *      in synchronized adjustment</ol>
         *   <ol>state changes in this {@see AdjustmentSynchronizer} are reflected in the
         *      {@see JCheckBox}</ol>
         * </li>
         *
         *
         * @param view  the checkbox to control whether an adjustable participates in synchronized
         *      adjustment
         * @param adjustable the adjustable
         * @exception IllegalArgumentException thrown, if view is null
         * @exception IllegalArgumentException thrown, if adjustable is null
         */
        protected void adapt(final JCheckBox view, final Adjustable adjustable) throws IllegalStateException {
            CheckParameterUtil.ensureParameterNotNull(adjustable, "adjustable");
            CheckParameterUtil.ensureParameterNotNull(view, "view");

            if (! synchronizedAdjustables.contains(adjustable)) {
                participateInSynchronizedScrolling(adjustable);
            }

            // register an item lister with the check box
            //
            view.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    switch(e.getStateChange()) {
                    case ItemEvent.SELECTED:
                        if (!isParticipatingInSynchronizedScrolling(adjustable)) {
                            setParticipatingInSynchronizedScrolling(adjustable, true);
                        }
                        break;
                    case ItemEvent.DESELECTED:
                        if (isParticipatingInSynchronizedScrolling(adjustable)) {
                            setParticipatingInSynchronizedScrolling(adjustable, false);
                        }
                        break;
                    }
                }
            });

            observable.addObserver(
                    new Observer() {
                        public void update(Observable o, Object arg) {
                            boolean sync = isParticipatingInSynchronizedScrolling(adjustable);
                            if (view.isSelected() != sync) {
                                view.setSelected(sync);
                            }
                        }
                    }
            );
            setParticipatingInSynchronizedScrolling(adjustable, true);
            view.setSelected(true);
        }
    }
}
