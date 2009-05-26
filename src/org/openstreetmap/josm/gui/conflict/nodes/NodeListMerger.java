package org.openstreetmap.josm.gui.conflict.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
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

import org.openstreetmap.josm.data.osm.Way;

/**
 * A UI component for resolving conflicts in the node lists of two {@see Way}s.
 * 
 */
public class NodeListMerger extends JPanel implements PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(NodeListMerger.class.getName());
    
    private JTable myNodes;
    private JTable mergedNodes;
    private JTable theirNodes;
    
    private NodeListMergeModel model;
    
    
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
    
    protected JScrollPane buildMyNodesTable() {
        myNodes  = new JTable(
            model.getMyNodesTableModel(),
            new NodeListColumnModel(
               new NodeListTableCellRenderer()
            ),
            model.getMyNodesSelectionModel()
         );
         myNodes.setName("table.mynodes");
         return embeddInScrollPane(myNodes);
    }

    protected JScrollPane buildMergedNodesTable() {
        mergedNodes  = new JTable(
            model.getMergedNodesTableModel(),
            new NodeListColumnModel(
                new NodeListTableCellRenderer()
            ),
            model.getMergedNodesSelectionModel()
         );
         mergedNodes.setName("table.mergednodes");
         return embeddInScrollPane(mergedNodes);
    }
    
    protected JScrollPane buildTheirNodesTable() {
        theirNodes  = new JTable(
            model.getTheirNodesTableModel(),
            new NodeListColumnModel(
                new NodeListTableCellRenderer()
            ),
            model.getTheirNodesSelectionModel()
         );
        theirNodes.setName("table.theirnodes");
        return embeddInScrollPane(theirNodes);
    }
    
    protected void wireActionsToSelectionModels() {
        myNodes.getSelectionModel().addListSelectionListener(copyStartLeftAction);
        
        myNodes.getSelectionModel().addListSelectionListener(copyBeforeCurrentLeftAction);
        mergedNodes.getSelectionModel().addListSelectionListener(copyBeforeCurrentLeftAction);
        
        myNodes.getSelectionModel().addListSelectionListener(copyAfterCurrentLeftAction);
        mergedNodes.getSelectionModel().addListSelectionListener(copyAfterCurrentLeftAction);
        
        myNodes.getSelectionModel().addListSelectionListener(copyEndLeftAction);
        
        
        theirNodes.getSelectionModel().addListSelectionListener(copyStartRightAction);
        
        theirNodes.getSelectionModel().addListSelectionListener(copyBeforeCurrentRightAction);
        mergedNodes.getSelectionModel().addListSelectionListener(copyBeforeCurrentRightAction);
        
        theirNodes.getSelectionModel().addListSelectionListener(copyAfterCurrentRightAction);
        mergedNodes.getSelectionModel().addListSelectionListener(copyAfterCurrentRightAction);
        
        theirNodes.getSelectionModel().addListSelectionListener(copyEndRightAction);      
        
        mergedNodes.getSelectionModel().addListSelectionListener(moveUpMergedAction);
        mergedNodes.getSelectionModel().addListSelectionListener(moveDownMergedAction);
        mergedNodes.getSelectionModel().addListSelectionListener(removeMergedAction);
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
        JLabel lbl = new JLabel(tr("Nodes in my version (local dataset)"));
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
        add(lbl, gc);

        gc.gridx = 4;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        lbl = new JLabel(tr("Nodes in their version (server dataset)"));
        add(lbl, gc);
        

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.3;
        gc.weighty = 1.0;
        add(buildMyNodesTable(), gc);
        
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
        add(buildMergedNodesTable(), gc);
        
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
        add(buildTheirNodesTable(), gc);
        
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
        
    public NodeListMerger() {
        model = new NodeListMergeModel();        
        build();
        model.addPropertyChangeListener(this);
    }
    
    public void populate(Way my, Way their) {
        model.populate(my, their);
    }
    
    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes at the beginning of the list of merged nodes. 
     *
     */  
    abstract class AbstractNodeManipulationAction extends AbstractAction {

        /**
         * load an icon given by iconName 
         * 
         * @param iconName  the name of the icon (without path, i.e. <tt>copystartleft.png</tt>
         * @return the icon; null, if the icon was not found 
         */
        protected ImageIcon getIcon(String iconName) {
            String fullIconName  = "/images/dialogs/conflict/" + iconName;
            URL imageURL   = this.getClass().getResource(fullIconName);            
            if (imageURL == null) {
                System.out.println(tr("WARNING: failed to load resource {0}", fullIconName));
                return null;
            }
            return new ImageIcon(imageURL);
        }
    }

    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes at the beginning of the list of merged nodes. 
     *
     */ 
    class CopyStartLeftAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyStartLeftAction() {            
            ImageIcon icon = getIcon("copystartleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("> top"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected nodes to the start of the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = myNodes.getSelectedRows();
            model.copyMyNodesToTop(rows);            
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!myNodes.getSelectionModel().isSelectionEmpty());            
        }
    }
    
    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes at the end of the list of merged nodes. 
     *
     */ 
    class CopyEndLeftAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyEndLeftAction() {            
            ImageIcon icon = getIcon("copyendleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("> bottom"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected nodes to the end of the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = myNodes.getSelectedRows();
            model.copyMyNodesToEnd(rows);  
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!myNodes.getSelectionModel().isSelectionEmpty());            
        }
    }
    
    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes before the first selected row in the list of merged nodes. 
     *
     */
    class CopyBeforeCurrentLeftAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyBeforeCurrentLeftAction() {            
            ImageIcon icon = getIcon("copybeforecurrentleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "> before");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected nodes before the first selected node in the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = myNodes.getSelectedRows();
            int [] mergedRows = mergedNodes.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0) {
                return;
            }
            int current = mergedRows[0];            
            model.copyMyNodesBeforeCurrent(myRows, current);            
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {        
            setEnabled(
                    !myNodes.getSelectionModel().isSelectionEmpty()
                  && ! mergedNodes.getSelectionModel().isSelectionEmpty()
            );            
        }
    }
    
    /**
     * Action for copying selected nodes in the list of my nodes to the list of merged
     * nodes. Inserts the nodes after the first selected row in the list of merged nodes. 
     *
     */    
    class CopyAfterCurrentLeftAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyAfterCurrentLeftAction() {            
            ImageIcon icon = getIcon("copyaftercurrentleft.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "> after");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy my selected nodes after the first selected node in the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = myNodes.getSelectedRows();
            int [] mergedRows = mergedNodes.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0) {
                return;
            }
            int current = mergedRows[0];            
            model.copyMyNodesAfterCurrent(myRows, current);                        
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {        
            setEnabled(
                    !myNodes.getSelectionModel().isSelectionEmpty()
                  && ! mergedNodes.getSelectionModel().isSelectionEmpty()
            );            
        }
    }
    
    
    class CopyStartRightAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyStartRightAction() {            
            ImageIcon icon = getIcon("copystartright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< top");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected nodes to the start of the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = theirNodes.getSelectedRows();
            model.copyTheirNodesToTop(rows);                        
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!theirNodes.getSelectionModel().isSelectionEmpty());            
        }
    }
    
    
    class CopyEndRightAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyEndRightAction() {            
            ImageIcon icon = getIcon("copyendright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< bottom");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected nodes to the end of the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = theirNodes.getSelectedRows();
            model.copyTheirNodesToEnd(rows);  
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(!theirNodes.getSelectionModel().isSelectionEmpty());            
        }
    }
    
    class CopyBeforeCurrentRightAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyBeforeCurrentRightAction() {            
            ImageIcon icon = getIcon("copybeforecurrentright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< before");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected nodes before the first selected node in the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = theirNodes.getSelectedRows();
            int [] mergedRows = mergedNodes.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0) {
                return;
            }
            int current = mergedRows[0];            
            model.copyTheirNodesBeforeCurrent(myRows, current);            
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {        
            setEnabled(
                    !theirNodes.getSelectionModel().isSelectionEmpty()
                  && ! mergedNodes.getSelectionModel().isSelectionEmpty()
            );            
        }
    }
    
    
    class CopyAfterCurrentRightAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public CopyAfterCurrentRightAction() {            
            ImageIcon icon = getIcon("copyaftercurrentright.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, "< after");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Copy their selected nodes after the first selected node in the merged node list"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] myRows = theirNodes.getSelectedRows();
            int [] mergedRows = mergedNodes.getSelectedRows();
            if (mergedRows == null || mergedRows.length == 0) {
                return;
            }
            int current = mergedRows[0];            
            model.copyTheirNodesAfterCurrent(myRows, current);                        
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {        
            setEnabled(
                    !theirNodes.getSelectionModel().isSelectionEmpty()
                  && ! mergedNodes.getSelectionModel().isSelectionEmpty()
            );            
        }
    }
    
    
    class MoveUpMergedAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public MoveUpMergedAction() {            
            ImageIcon icon = getIcon("moveup.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("Up"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Move up the selected nodes by one position"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedNodes.getSelectedRows();
            model.moveUpMergedNodes(rows);            
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int [] rows = mergedNodes.getSelectedRows();
            setEnabled(
                    rows != null
                  && rows.length > 0
                  && rows[0] != 0
            );            
        }
    }
    
    class MoveDownMergedAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public MoveDownMergedAction() {            
            ImageIcon icon = getIcon("movedown.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("Down"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Move down the selected nodes by one position"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedNodes.getSelectedRows();
            model.moveDownMergedNodes(rows);                        
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int [] rows = mergedNodes.getSelectedRows();
            setEnabled(
                    rows != null
                  && rows.length > 0
                  && rows[rows.length -1] != mergedNodes.getRowCount() -1
            );            
        }
    }
    
    class RemoveMergedAction extends AbstractNodeManipulationAction implements ListSelectionListener {

        public RemoveMergedAction() {            
            ImageIcon icon = getIcon("remove.png");
            putValue(Action.SMALL_ICON, icon);
            if (icon == null) {
                putValue(Action.NAME, tr("Remove"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Remove the selected nodes from the list of merged nodes"));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedNodes.getSelectedRows();
            model.removeMergedNodes(rows);                        
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int [] rows = mergedNodes.getSelectedRows();
            setEnabled(
                    rows != null
                  && rows.length > 0
            );            
        }
    }
    
    class FreezeAction extends AbstractNodeManipulationAction implements ItemListener  {

        public FreezeAction() {            
            // FIXME 
//            ImageIcon icon = getIcon("remove.png");
//            putValue(Action.SMALL_ICON, icon);
//            if (icon == null) {
//                putValue(Action.NAME, tr("Remove"));
//            }
            putValue(Action.NAME, tr("Freeze"));
            putValue(Action.SHORT_DESCRIPTION, tr("Freeze the current list of merged nodes."));
            putValue(Action.SELECTED_KEY, false);
            setEnabled(true);
            
        }
        @Override
        public void actionPerformed(ActionEvent arg0) {
            int [] rows = mergedNodes.getSelectedRows();
            model.removeMergedNodes(rows);                        
        }
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            int state = e.getStateChange();
            if (state == ItemEvent.SELECTED) {
                model.setFrozen(true);
                putValue(Action.NAME, tr("Unfreeze"));
                putValue(Action.SHORT_DESCRIPTION, tr("Unfreeze the list of merged nodes and start merging"));
            } else if (state == ItemEvent.DESELECTED) {
                model.setFrozen(false);
                putValue(Action.NAME, tr("Freeze"));
                putValue(Action.SHORT_DESCRIPTION, tr("Freeze the current list of merged nodes"));
            }            
        }  
    }

    protected void handlePropertyChangeFrozen(boolean oldValue, boolean newValue) {
        myNodes.getSelectionModel().clearSelection();
        myNodes.setEnabled(!newValue);        
        theirNodes.getSelectionModel().clearSelection();
        theirNodes.setEnabled(!newValue);
        mergedNodes.getSelectionModel().clearSelection();
        mergedNodes.setEnabled(!newValue);
        freezeAction.putValue(Action.SELECTED_KEY, newValue);
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(NodeListMergeModel.PROP_FROZEN)) {
            handlePropertyChangeFrozen((Boolean)evt.getOldValue(), (Boolean)evt.getNewValue());
        }
        
    }
    
    public NodeListMergeModel getModel() {
        return model;
    }
    
    
    
}
