// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.AdjustmentSynchronizer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTable;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A UI component for resolving conflicts in two lists of entries of type T.
 *
 * @param <T>  the type of the entries
 * @see ListMergeModel
 */
public abstract class ListMerger<T extends PrimitiveId> extends JPanel implements PropertyChangeListener, Observer {
    protected OsmPrimitivesTable myEntriesTable;
    protected OsmPrimitivesTable mergedEntriesTable;
    protected OsmPrimitivesTable theirEntriesTable;

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

    private  JLabel lblMyVersion;
    private  JLabel lblMergedVersion;
    private  JLabel lblTheirVersion;

    private  JLabel lblFrozenState;

    abstract protected JScrollPane buildMyElementsTable();
    abstract protected JScrollPane buildMergedElementsTable();
    abstract protected JScrollPane buildTheirElementsTable();

    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
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
        JosmComboBox cbComparePair = new JosmComboBox(model.getComparePairListModel());
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
        lblMergedVersion.setToolTipText(tr("List of merged elements. They will replace the list of my elements when the merge decisions are applied."));
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
        JCheckBox cbLockMyScrolling = new JCheckBox();
        cbLockMyScrolling.setName("checkbox.lockmyscrolling");
        add(buildAdjustmentLockControlPanel(cbLockMyScrolling), gc);

        gc.gridx = 2;
        gc.gridy = 1;
        JCheckBox cbLockMergedScrolling = new JCheckBox();
        cbLockMergedScrolling.setName("checkbox.lockmergedscrolling");
        add(buildAdjustmentLockControlPanel(cbLockMergedScrolling), gc);

        gc.gridx = 4;
        gc.gridy = 1;
        JCheckBox cbLockTheirScrolling = new JCheckBox();
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

    /**
     * Constructs a new {@code ListMerger}.
     * @param model
     */
    public ListMerger(ListMergeModel<T> model) {
        this.model = model;
        model.addObserver(this);
        build();
        model.addPropertyChangeListener(this);
    }

    /**
     * Base class of all other Copy* inner classes.
     */
    abstract class CopyAction extends AbstractAction implements ListSelectionListener {
        
        protected CopyAction(String icon_name, String action_name, String short_description) {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", icon_name+".png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, action_name);
            }
            putValue(Action.SHORT_DESCRIPTION, short_description);
            setEnabled(false);
        }
    }
    
    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes at the beginning of the list of merged nodes.
     */
    class CopyStartLeftAction extends CopyAction {

        public CopyStartLeftAction() {
            super("copystartleft", tr("> top"), tr("Copy my selected nodes to the start of the merged node list"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.copyMyToTop(myEntriesTable.getSelectedRows());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!myEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes at the end of the list of merged nodes.
     */
    class CopyEndLeftAction extends CopyAction {

        public CopyEndLeftAction() {
            super("copyendleft", tr("> bottom"), tr("Copy my selected elements to the end of the list of merged elements."));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.copyMyToEnd(myEntriesTable.getSelectedRows());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!myEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes before the first selected row in the list of merged nodes.
     */
    class CopyBeforeCurrentLeftAction extends CopyAction {

        public CopyBeforeCurrentLeftAction() {
            super("copybeforecurrentleft", tr("> before"),
                    tr("Copy my selected elements before the first selected element in the list of merged elements."));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int [] myRows = myEntriesTable.getSelectedRows();
            int current = mergedRows[0];
            model.copyMyBeforeCurrent(myRows, current);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !myEntriesTable.getSelectionModel().isSelectionEmpty()
                    && !mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes after the first selected row in the list of merged nodes.
     */
    class CopyAfterCurrentLeftAction extends CopyAction {

        public CopyAfterCurrentLeftAction() {
            super("copyaftercurrentleft", tr("> after"),
                    tr("Copy my selected elements after the first selected element in the list of merged elements."));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int [] myRows = myEntriesTable.getSelectedRows();
            int current = mergedRows[0];
            model.copyMyAfterCurrent(myRows, current);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !myEntriesTable.getSelectionModel().isSelectionEmpty()
                    && !mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    class CopyStartRightAction extends CopyAction {

        public CopyStartRightAction() {
            super("copystartright", tr("< top"), tr("Copy their selected element to the start of the list of merged elements."));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.copyTheirToTop(theirEntriesTable.getSelectedRows());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!theirEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    class CopyEndRightAction extends CopyAction {

        public CopyEndRightAction() {
            super("copyendright", tr("< bottom"), tr("Copy their selected elements to the end of the list of merged elements."));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            model.copyTheirToEnd(theirEntriesTable.getSelectedRows());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!theirEntriesTable.getSelectionModel().isSelectionEmpty());
        }
    }

    class CopyBeforeCurrentRightAction extends CopyAction {

        public CopyBeforeCurrentRightAction() {
            super("copybeforecurrentright", tr("< before"),
                    tr("Copy their selected elements before the first selected element in the list of merged elements."));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int [] myRows = theirEntriesTable.getSelectedRows();
            int current = mergedRows[0];
            model.copyTheirBeforeCurrent(myRows, current);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !theirEntriesTable.getSelectionModel().isSelectionEmpty()
                    && !mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    class CopyAfterCurrentRightAction extends CopyAction {

        public CopyAfterCurrentRightAction() {
            super("copyaftercurrentright", tr("< after"),
                    tr("Copy their selected element after the first selected element in the list of merged elements"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int [] mergedRows = mergedEntriesTable.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0)
                return;
            int [] myRows = theirEntriesTable.getSelectedRows();
            int current = mergedRows[0];
            model.copyTheirAfterCurrent(myRows, current);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(
                    !theirEntriesTable.getSelectionModel().isSelectionEmpty()
                    && !mergedEntriesTable.getSelectionModel().isSelectionEmpty()
            );
        }
    }

    class CopyAllLeft extends AbstractAction implements Observer, PropertyChangeListener {

        public CopyAllLeft() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "useallleft.png");
            putValue(Action.SMALL_ICON, icon);
            putValue(Action.SHORT_DESCRIPTION, tr("Copy all my elements to the target"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            model.copyAll(ListRole.MY_ENTRIES);
            model.setFrozen(true);
        }

        private void updateEnabledState() {
            setEnabled(model.getMergedEntries().isEmpty() && !model.isFrozen());
        }

        @Override
        public void update(Observable o, Object arg) {
            updateEnabledState();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateEnabledState();
        }
    }

    class CopyAllRight extends AbstractAction implements Observer, PropertyChangeListener {

        public CopyAllRight() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "useallright.png");
            putValue(Action.SMALL_ICON, icon);
            putValue(Action.SHORT_DESCRIPTION, tr("Copy all their elements to the target"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            model.copyAll(ListRole.THEIR_ENTRIES);
            model.setFrozen(true);
        }

        private void updateEnabledState() {
            setEnabled(model.getMergedEntries().isEmpty() && !model.isFrozen());
        }

        @Override
        public void update(Observable o, Object arg) {
            updateEnabledState();
        }

        @Override
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
            putValue(Action.SHORT_DESCRIPTION, tr("Move up the selected entries by one position."));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            model.moveUpMerged(rows);
        }

        @Override
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

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            model.moveDownMerged(rows);
        }

        @Override
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

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedEntriesTable.getSelectedRows();
            model.removeMerged(rows);
        }

        @Override
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

        @Override
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
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals(PROP_SELECTED)) {
                                btn.setSelected((Boolean)evt.getNewValue());
                            }
                        }
                    }
            );
        }

        @Override
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ListMergeModel.FROZEN_PROP)) {
            handlePropertyChangeFrozen((Boolean)evt.getOldValue(), (Boolean)evt.getNewValue());
        }
    }

    public ListMergeModel<T> getModel() {
        return model;
    }

    @Override
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

    public void unlinkAsListener() {
        myEntriesTable.unlinkAsListener();
        mergedEntriesTable.unlinkAsListener();
        theirEntriesTable.unlinkAsListener();
    }

    protected final <P extends OsmPrimitive> OsmDataLayer findLayerFor(P primitive) {
        if (primitive != null) {
            Iterable<OsmDataLayer> layers = Main.map.mapView.getLayersOfType(OsmDataLayer.class);
            // Find layer with same dataset
            for (OsmDataLayer layer : layers) {
                if (layer.data == primitive.getDataSet()) {
                    return layer;
                }
            }
            // Conflict after merging layers: a dataset could be no more in any layer, try to find another layer with same primitive
            for (OsmDataLayer layer : layers) {
                final Collection<? extends OsmPrimitive> collection;
                if (primitive instanceof Way) {
                    collection = layer.data.getWays();
                } else if (primitive instanceof Relation) {
                    collection = layer.data.getRelations();
                } else {
                    collection = layer.data.allPrimitives();
                }
                for (OsmPrimitive p : collection) {
                    if (p.getPrimitiveId().equals(primitive.getPrimitiveId())) {
                        return layer;
                    }
                }
            }
        }
        return null;
    }
}
