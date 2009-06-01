package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A UI component for resolving conflicts in two lists of entries of type T.
 * 
 * @param T  the type of the entries
 * @see ListMergeModel
 */
public abstract class ListMerger<T> extends JPanel implements PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(ListMerger.class.getName());

    protected JTable myEntriesTable;
    protected JTable mergedEntriesTable;
    protected JTable theirEntriesTable;

    protected ListMergeModel<T> model;


    private CopyStartLeftAction copyStartLeftAction;
    private CopyBeforeCurrentLeftAction copyBeforeCurrentLeftAction;
    private CopyAfterCurrentLeftAction copyAfterCurrentLeftAction;
    private CopyEndLeftAction copyEndLeftAction;

    private CopyStartRightAction copyStartRightAction;
    private CopyBeforeCurrentRightAction copyBeforeCurrentRightAction;
    private CopyAfterCurrentRightAction copyAfterCurrentRightAction;
    private CopyEndRightAction copyEndRightAction;

    private MoveUpMergedAction moveUpMergedAction;
    private MoveDownMergedAction moveDownMergedAction;
    private RemoveMergedAction removeMergedAction;
    private FreezeAction freezeAction;



    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return pane;
    }

    abstract protected JScrollPane buildMyElementsTable();
    abstract protected JScrollPane buildMergedElementsTable();
    abstract protected JScrollPane buildTheirElementsTable();



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

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 3;
        gc.weightx = 1.0;
        freezeAction = new FreezeAction();
        JToggleButton btn = new JToggleButton(freezeAction);
        freezeAction.adapt(btn);
        btn.setName("button.freeze");
        btn.addItemListener(freezeAction);
        pnl.add(btn, gc);

        return pnl;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(10,0,10,0);
        JLabel lbl = new JLabel(tr("My version"));
        lbl.setToolTipText(tr("List of elements in my dataset, i.e. the local dataset"));
        add(lbl, gc);

        gc.gridx = 2;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        lbl = new JLabel(tr("Merged version"));
        lbl.setToolTipText(tr("List of merged elements. They will replace the my elements when the merge decisions are applied."));
        add(lbl, gc);

        gc.gridx = 4;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        lbl = new JLabel(tr("Their version"));
        lbl.setToolTipText(tr("List of elements in their dataset, i.e. the server dataset"));

        add(lbl, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.3;
        gc.weighty = 1.0;
        gc.insets = new Insets(0,0,0,0);
        add(buildMyElementsTable(), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(buildLeftButtonPanel(), gc);

        gc.gridx = 2;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.3;
        gc.weighty = 0.0;
        add(buildMergedElementsTable(), gc);

        gc.gridx = 3;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(buildRightButtonPanel(), gc);

        gc.gridx = 4;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.3;
        gc.weighty = 0.0;
        add(buildTheirElementsTable(), gc);

        gc.gridx = 2;
        gc.gridy = 2;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.3;
        gc.weighty = 0.0;
        add(buildMergedListControlButtons(), gc);

        wireActionsToSelectionModels();
    }

    public ListMerger(ListMergeModel<T> model) {
        this.model = model;
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
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected elements to the end of the list of merged elements"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected elements before the first selected element in the list of merged elements"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected elements after the first selected element in the list of merged elements"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected element to the start of the list of merged elements"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected elements to the end of the list of merged elements"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected elements before the first selected element in the list of merged elements"));
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


    class MoveUpMergedAction extends AbstractAction implements ListSelectionListener {

        public MoveUpMergedAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "moveup.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("Up"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Move up the selected elements by one position"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Move down the selected entries by one position"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Remove the selected entries from the list of merged elements"));
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
            putValue(Action.SHORT_DESCRIPTION, tr("Freeze the current list of merged elements"));
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
            //            btn.addItemListener(
            //                    new ItemListener() {
            //                        public void itemStateChanged(ItemEvent e) {
            //                            boolean isSelected = (Boolean)getValue(PROP_SELECTED);
            //                            if (isSelected != (e.getStateChange() == ItemEvent.SELECTED)) {
            //                                putValue(PROP_SELECTED, e.getStateChange() == ItemEvent.SELECTED);
            //                            }
            //                            model.setFrozen(e.getStateChange() == ItemEvent.SELECTED);
            //                        }
            //                    }
            //            );
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
                model.setFrozen(true);
                putValue(Action.NAME, tr("Unfreeze"));
                putValue(Action.SHORT_DESCRIPTION, tr("Unfreeze the list of merged elements and start merging"));
            } else if (state == ItemEvent.DESELECTED) {
                model.setFrozen(false);
                putValue(Action.NAME, tr("Freeze"));
                putValue(Action.SHORT_DESCRIPTION, tr("Freeze the current list of merged elements"));
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
    }


    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ListMergeModel.PROP_FROZEN)) {
            handlePropertyChangeFrozen((Boolean)evt.getOldValue(), (Boolean)evt.getNewValue());
        }
    }

    public ListMergeModel<T> getModel() {
        return model;
    }
}
