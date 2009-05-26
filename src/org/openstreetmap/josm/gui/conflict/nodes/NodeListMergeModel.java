// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.command.WayNodesConflictResolverCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

public class NodeListMergeModel {
    private static final Logger logger = Logger.getLogger(NodeListMergeModel.class.getName());
    
    public static final String PROP_FROZEN = NodeListMergeModel.class.getName() + ".frozen";
    

    private ArrayList<Node> myNodes;
    private ArrayList<Node> theirNodes;
    private ArrayList<Node> mergedNodes;
    
    
    private DefaultTableModel myNodesTableModel;
    private DefaultTableModel theirNodesTableModel;
    private DefaultTableModel mergedNodesTableModel;
    
    private DefaultListSelectionModel myNodesSelectionModel;
    private DefaultListSelectionModel theirNodesSelectionModel;
    private DefaultListSelectionModel mergedNodesSelectionModel;
    
    private ArrayList<PropertyChangeListener> listeners;
    private boolean isFrozen = false; 
    
    
    public NodeListMergeModel() {
        myNodes = new ArrayList<Node>();
        theirNodes = new ArrayList<Node>();
        mergedNodes = new ArrayList<Node>();
        
        myNodesTableModel = new NodeListTableModel(myNodes);
        theirNodesTableModel = new NodeListTableModel(theirNodes);
        mergedNodesTableModel = new NodeListTableModel(mergedNodes);
        
        myNodesSelectionModel = new DefaultListSelectionModel();
        theirNodesSelectionModel = new DefaultListSelectionModel();
        mergedNodesSelectionModel = new DefaultListSelectionModel();
        
        listeners = new ArrayList<PropertyChangeListener>();
        
        setFrozen(true);
    }
    
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized(listeners) {
            if (listener != null && ! listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        synchronized(listeners) {
            if (listener != null && listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }
    
    protected void fireFrozenChanged(boolean oldValue, boolean newValue) {
        synchronized(listeners) {
            PropertyChangeEvent evt = new PropertyChangeEvent(this, PROP_FROZEN, oldValue, newValue);
            for (PropertyChangeListener listener: listeners) {
                listener.propertyChange(evt);
            }
        }
    }
    
    public void setFrozen(boolean isFrozen) {
        boolean oldValue = this.isFrozen;
        this.isFrozen = isFrozen;
        fireFrozenChanged(oldValue, this.isFrozen);
    }
    
    public boolean isFrozen() {
        return isFrozen;
    }
    
    public TableModel getMyNodesTableModel() {
        return myNodesTableModel;
    }
    
    public TableModel getTheirNodesTableModel() {
        return theirNodesTableModel;
    }
    
    public TableModel getMergedNodesTableModel() {
        return mergedNodesTableModel;
    }
    
    public ListSelectionModel getMyNodesSelectionModel() {
        return myNodesSelectionModel;
    }

    public ListSelectionModel getTheirNodesSelectionModel() {
        return theirNodesSelectionModel;
    }
    
    public ListSelectionModel getMergedNodesSelectionModel() {
        return mergedNodesSelectionModel;
    }
    
    
    protected void fireModelDataChanged() {
        myNodesTableModel.fireTableDataChanged();
        theirNodesTableModel.fireTableDataChanged();
        mergedNodesTableModel.fireTableDataChanged();
    }
    
    protected void copyNodesToTop(List<Node> source, int []rows) {
        if (rows == null || rows.length == 0) {
            return;
        }
        for (int i = rows.length - 1; i >= 0; i--) {
            int row = rows[i];
            Node n = source.get(row);
            mergedNodes.add(0, n);
        }
        fireModelDataChanged();
        mergedNodesSelectionModel.setSelectionInterval(0, rows.length -1);
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes at the top of the list of merged
     * nodes.  
     * 
     * @param rows the indices 
     */
    public void copyMyNodesToTop(int [] rows) {
        copyNodesToTop(myNodes, rows);        
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes at the top of the list of merged
     * nodes.  
     * 
     * @param rows the indices 
     */
    public void copyTheirNodesToTop(int [] rows) {
        copyNodesToTop(theirNodes, rows);        
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of  nodes in source to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.  
     * 
     * @param source the list of nodes to copy from
     * @param rows the indices 
     */    

    public void copyNodesToEnd(List<Node> source, int [] rows) {
        if (rows == null || rows.length == 0) {
            return;
        }
        for (int row : rows) {
            Node n = source.get(row);
            mergedNodes.add(n);
        }
        fireModelDataChanged();
        mergedNodesSelectionModel.setSelectionInterval(mergedNodes.size()-rows.length, mergedNodes.size() -1);

    }
    
    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.  
     * 
     * @param rows the indices 
     */    
    public void copyMyNodesToEnd(int [] rows) {
        copyNodesToEnd(myNodes, rows);
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes at the end of the list of merged
     * nodes.  
     * 
     * @param rows the indices 
     */    
    public void copyTheirNodesToEnd(int [] rows) {
        copyNodesToEnd(theirNodes, rows);
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of  nodes <code>source</code> to the
     * list of merged nodes. Inserts the nodes before row given by current.
     * 
     * @param source the list of nodes to copy from 
     * @param rows the indices 
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes 
     * 
     */  
    protected void copyNodesBeforeCurrent(List<Node> source, int [] rows, int current) {
        if (rows == null || rows.length == 0) {
            return; 
        }
        if (current < 0 || current >= mergedNodes.size()) {
            throw new IllegalArgumentException(tr("parameter current out of range: got {0}", current));
        }
        for (int i=rows.length -1; i>=0; i--) {
            int row = rows[i];
            Node n = source.get(row);
            mergedNodes.add(current, n);
        }
        fireModelDataChanged();
        mergedNodesSelectionModel.setSelectionInterval(current, current + rows.length-1);
     }
    
    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes before row given by current.
     * 
     * @param rows the indices 
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes 
     * 
     */       
    public void copyMyNodesBeforeCurrent(int [] rows, int current) {
        copyNodesBeforeCurrent(myNodes,rows,current);
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of their nodes to the
     * list of merged nodes. Inserts the nodes before row given by current.
     * 
     * @param rows the indices 
     * @param current the row index before which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes 
     * 
     */       
    public void copyTheirNodesBeforeCurrent(int [] rows, int current) {
        copyNodesBeforeCurrent(theirNodes,rows,current);
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of  nodes <code>source</code> to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     * 
     * @param source the list of nodes to copy from 
     * @param rows the indices 
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes 
     * 
     */       
    protected void copyNodesAfterCurrent(List<Node> source, int [] rows, int current) {
        if (rows == null || rows.length == 0) {
            return;
        }
        if (current < 0 || current >= mergedNodes.size()) {
            throw new IllegalArgumentException(tr("parameter current out of range: got {0}", current));
        }
        if (current == mergedNodes.size() -1) {
            copyMyNodesToEnd(rows);
        } else {
            for (int i=rows.length -1; i>=0; i--) {
                int row = rows[i];
                Node n = source.get(row); 
                mergedNodes.add(current+1, n);
            }
        }
        fireModelDataChanged();   
        mergedNodesSelectionModel.setSelectionInterval(current+1, current + rows.length-1);
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     * 
     * @param rows the indices 
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes 
     * 
     */       
    public void copyMyNodesAfterCurrent(int [] rows, int current) {
        copyNodesAfterCurrent(myNodes, rows, current);
    }
    
    /**
     * Copies the nodes given by indices in rows from the list of my nodes to the
     * list of merged nodes. Inserts the nodes after the row given by current.
     * 
     * @param rows the indices 
     * @param current the row index after which the nodes are inserted
     * @exception IllegalArgumentException thrown, if current < 0 or >= #nodes in list of merged nodes 
     * 
     */       
    public void copyTheirNodesAfterCurrent(int [] rows, int current) {
        copyNodesAfterCurrent(theirNodes, rows, current);
    }

    /**
     * Moves the nodes given by indices in rows  up by one position in the list
     * of merged nodes.
     * 
     * @param rows the indices 
     * 
     */
    protected void moveUpMergedNodes(int [] rows) {
        if (rows == null || rows.length == 0) {
            return;
        }
        if (rows[0] == 0) {
            // can't move up
            return;
        }
        for (int row: rows) {
           Node n = mergedNodes.get(row);
           mergedNodes.remove(row);
           mergedNodes.add(row -1, n);
        }
        fireModelDataChanged();
        mergedNodesSelectionModel.clearSelection();
        for (int row: rows) {
            mergedNodesSelectionModel.addSelectionInterval(row-1, row-1);
        }
    }

    /**
     * Moves the nodes given by indices in rows down by one position in the list
     * of merged nodes.
     * 
     * @param rows the indices 
     */
    protected void moveDownMergedNodes(int [] rows) {
        if (rows == null || rows.length == 0) {
            return;
        }
        if (rows[rows.length -1] == mergedNodes.size() -1) {
            // can't move down
            return;
        }
        for (int i = rows.length-1; i>=0;i--) {
            int row = rows[i];
            Node n = mergedNodes.get(row);
            mergedNodes.remove(row);
            mergedNodes.add(row +1, n);
         }
        fireModelDataChanged();
        mergedNodesSelectionModel.clearSelection();
        for (int row: rows) {
            mergedNodesSelectionModel.addSelectionInterval(row+1, row+1);
        }        
    }
    
    /**
     * Removes the nodes given by indices in rows from the list
     * of merged nodes.
     * 
     * @param rows the indices 
     */    
    protected void removeMergedNodes(int [] rows) {
        if (rows == null || rows.length == 0) {
            return;
        }
        for (int i = rows.length-1; i>=0;i--) {
            mergedNodes.remove(rows[i]);
         }
        fireModelDataChanged();
        mergedNodesSelectionModel.clearSelection();
    }
    

    /**
     * Replies true if the list of my nodes and the list of their
     * nodes are equal, i.e. if they consists of a list of nodes with
     * identical ids in the same order.
     * 
     * @return true, if the lists are equal; false otherwise 
     */
    protected boolean myAndTheirNodesEqual() {
        if (myNodes.size() != theirNodes.size()) {
            return false;
        }
        for (int i=0; i < myNodes.size(); i++) {
            if (myNodes.get(i).id != theirNodes.get(i).id) {
                return false; 
            }
        }
        return true; 
    }
    
    /**
     * Populates the model with the nodes in the two {@see Way}s <code>my</code> and
     * <code>their</code>.
     *  
     * @param my  my way (i.e. the way in the local dataset) 
     * @param their their way (i.e. the way in the server dataset)
     * @exception IllegalArgumentException thrown, if my is null
     * @exception IllegalArgumentException  thrown, if their is null
     */
    public void populate(Way my, Way their) {
        if (my == null) 
            throw new IllegalArgumentException("parameter 'way' must not be null");
        if (their == null) 
            throw new IllegalArgumentException("parameter 'their' must not be null");
        mergedNodes.clear();
        myNodes.clear();
        theirNodes.clear();
        for (Node n : my.nodes) {
            myNodes.add(n);
        }
        for (Node n : their.nodes) {
            theirNodes.add(n);
        }
        if (myAndTheirNodesEqual()) {
            mergedNodes = new ArrayList<Node>(myNodes);
            setFrozen(true);
        } else {
            setFrozen(false);
        }
        
        fireModelDataChanged();
    }
    
    /**
     * Builds the command to resolve conflicts in the node list of a way
     * 
     * @param my  my way. Must not be null. 
     * @param their  their way. Must not be null
     * @return the command
     * @exception IllegalArgumentException thrown, if my is null or not a {@see Way}
     * @exception IllegalArgumentException thrown, if their is null or not a {@see Way}
     * @exception IllegalStateException thrown, if the merge is not yet frozen 
     */
    public WayNodesConflictResolverCommand buildResolveCommand(Way my, Way their) {        
        if (my == null) {
            throw new IllegalArgumentException("parameter my most not be null");            
        }
        if (their == null) {
            throw new IllegalArgumentException("parameter my most not be null");            
        }
        if (! isFrozen()) {
            throw new IllegalArgumentException("merged nodes not frozen yet. Can't build resolution command");
        }
        return new WayNodesConflictResolverCommand(my, their, mergedNodes);
    }
    
    class NodeListTableModel extends DefaultTableModel {
        private ArrayList<Node> nodes;
        
        public NodeListTableModel(ArrayList<Node> nodes) {
            this.nodes = nodes; 
        }
        
        @Override
        public int getRowCount() {
            return nodes == null ? 0 : nodes.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return nodes.get(row);           
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }  
    }    


}
