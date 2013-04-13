// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.TableCellEditorSupport;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.tools.GBC;

/**
 * 
 * @author master
 * 
 * Dialog to add tags as part of the remotecontrol
 * Existing Keys get grey color and unchecked selectboxes so they will not overwrite the old Key-Value-Pairs by default.
 * You can choose the tags you want to add by selectboxes. You can edit the tags before you apply them.
 *
 */
public class AddTagsDialog extends ExtendedDialog implements SelectionChangedListener {


    private final JTable propertyTable;
    private Collection<? extends OsmPrimitive> sel;
    int[] count;

    static class DeleteTagMarker {
        int num;
        public DeleteTagMarker(int num) {
            this.num = num;
        }
        public String toString() {
            return tr("<delete from {0} objects>", num);
        }
    }
    
            
    public AddTagsDialog(String[][] tags) {
        super(Main.parent, tr("Add tags to selected objects"), new String[] { tr("Add selected tags"), tr("Add all tags"),  tr("Cancel")},
                false,
                true);
        setToolTipTexts(new String[]{tr("Add checked tags to selected objects"), tr("Shift+Enter: Add all tags to selected objects"), ""});
     
        DataSet.addSelectionListener(this);


        DefaultTableModel tm = new DefaultTableModel(new String[] {tr("Assume"), tr("Key"), tr("Value")}, tags.length) {
            final Class<?> types[] = {Boolean.class, String.class, Object.class};
            @Override
            public Class getColumnClass(int c) {
                return types[c];
            }

        };

        sel = Main.main.getCurrentDataSet().getSelected();
        count = new int[tags.length];
        
        for (int i = 0; i<tags.length; i++) {
            count[i] = 0;
            String key = tags[i][0];
            String value = tags[i][1];
            Boolean b = Boolean.TRUE;
            for (OsmPrimitive osm : sel) {
                if (osm.keySet().contains(key) && !osm.get(key).equals(value)) {
                    b = Boolean.FALSE;
                    count[i]++;
                    break;
                }
            }
            tm.setValueAt(b, i, 0);
            tm.setValueAt(tags[i][0], i, 1);
            tm.setValueAt(tags[i][1].isEmpty() ? new DeleteTagMarker(count[i]) : tags[i][1], i, 2);
        }

        propertyTable = new JTable(tm) {

            private static final long serialVersionUID = 1L;
            ///private final DefaultCellEditor textEditor = new DefaultCellEditor( new JTextField() );

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (count[row]>0) {
                    c.setFont(c.getFont().deriveFont(Font.ITALIC));
                    c.setForeground(new Color(100, 100, 100));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    c.setForeground(new Color(0, 0, 0));
                }
                return c;
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                Object value = getValueAt(row,column);
                System.out.println(value);
                if (value instanceof DeleteTagMarker) return null;
                return getDefaultEditor(value.getClass());
            }
        };
        
        propertyTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        // a checkbox has a size of 15 px
        propertyTable.getColumnModel().getColumn(0).setMaxWidth(15);
        TableHelper.adjustColumnWidth(propertyTable, 1, 200);
        TableHelper.adjustColumnWidth(propertyTable, 2, 700);
        // get edit results if the table looses the focus, for example if a user clicks "add tags"
        propertyTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        propertyTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_MASK), "shiftenter");
        propertyTable.getActionMap().put("shiftenter", new AbstractAction() {
            @Override  public void actionPerformed(ActionEvent e) { 
                buttonAction(1, e); // add all tags on Shift-Enter
            }
        });

        // set the content of this AddTagsDialog consisting of the tableHeader and the table itself.
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new GridBagLayout());
        tablePanel.add(propertyTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
        tablePanel.add(propertyTable, GBC.eol().fill(GBC.BOTH));
        setContent(tablePanel);
        setDefaultButton(2);
    }

    /**
     * This method looks for existing tags in the current selection and sets the corresponding boolean in the boolean array existing[]
     */
    private void findExistingTags() {
        TableModel tm = propertyTable.getModel();
        for (int i=0; i<tm.getRowCount(); i++) {
            String key = (String)tm.getValueAt(i, 1);
            String value = (String)tm.getValueAt(i, 1);
            count[i] = 0;
            for (OsmPrimitive osm : sel) {
                if (osm.keySet().contains(key) && !osm.get(key).equals(value)) {
                    count[i]++;
                    break;
                }
            }
        }
        propertyTable.repaint();
    }

    /**
     * If you click the "Add tags" button build a ChangePropertyCommand for every key that has a checked checkbox to apply the key value pair to all selected osm objects.
     * You get a entry for every key in the command queue.
     */
    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        // if layer all layers were closed, ignore all actions
        if (Main.main.getCurrentDataSet() != null  && buttonIndex != 2) {
            TableModel tm = propertyTable.getModel();
            for (int i=0; i<tm.getRowCount(); i++) {
                if (buttonIndex==1 || (Boolean)tm.getValueAt(i, 0)) {
                    String key =(String)tm.getValueAt(i, 1);
                    Object value = tm.getValueAt(i, 2);
                    Main.main.undoRedo.add(new ChangePropertyCommand(sel,
                            key, value instanceof String ? (String) value : ""));
                }
            }
        }
        setVisible(false);
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        sel = newSelection;
        findExistingTags();
    }

}
