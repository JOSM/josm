// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * Dialog to add tags as part of the remotecontrol.
 * Existing Keys get grey color and unchecked selectboxes so they will not overwrite the old Key-Value-Pairs by default.
 * You can choose the tags you want to add by selectboxes. You can edit the tags before you apply them.
 * @author master
 * @since 3850
 */
public class AddTagsDialog extends ExtendedDialog {

    private final JTable propertyTable;
    private final transient Collection<? extends OsmPrimitive> sel;
    private final int[] count;

    private final String sender;
    private static final Set<String> trustedSenders = new HashSet<>();

    /**
     * Class for displaying "delete from ... objects" in the table
     */
    static class DeleteTagMarker {
        private int num;
        public DeleteTagMarker(int num) {
            this.num = num;
        }
        @Override
        public String toString() {
            return tr("<delete from {0} objects>", num);
        }
    }

    /**
     * Class for displaying list of existing tag values in the table
     */
    static class ExistingValues {
        private String tag;
        private Map<String, Integer> valueCount;
        public ExistingValues(String tag) {
            this.tag=tag; valueCount=new HashMap<>();
        }

        int addValue(String val) {
            Integer c = valueCount.get(val);
            int r = c==null? 1 : (c.intValue()+1);
            valueCount.put(val, r);
            return r;
        }

        @Override
        public String toString() {
            StringBuilder sb=new StringBuilder();
            for (String k: valueCount.keySet()) {
                if (sb.length()>0) sb.append(", ");
                sb.append(k);
            }
            return sb.toString();
        }

        private String getToolTip() {
            StringBuilder sb = new StringBuilder();
            sb.append("<html>")
              .append(tr("Old values of"))
              .append(" <b>")
              .append(tag)
              .append("</b><br/>");
            for (Entry<String, Integer> e : valueCount.entrySet()) {
                sb.append("<b>")
                  .append(e.getValue())
                  .append(" x </b>")
                  .append(e.getKey())
                  .append("<br/>");
            }
            sb.append("</html>");
            return sb.toString();
        }
    }

    /**
     * Constructs a new {@code AddTagsDialog}.
     */
    public AddTagsDialog(String[][] tags, String senderName, Collection<? extends OsmPrimitive> primitives) {
        super(Main.parent, tr("Add tags to selected objects"), new String[] { tr("Add selected tags"), tr("Add all tags"),  tr("Cancel")},
                false,
                true);
        setToolTipTexts(new String[]{tr("Add checked tags to selected objects"), tr("Shift+Enter: Add all tags to selected objects"), ""});

        this.sender = senderName;

        final DefaultTableModel tm = new DefaultTableModel(new String[] {tr("Assume"), tr("Key"), tr("Value"), tr("Existing values")}, tags.length) {
            private final Class<?>[] types = {Boolean.class, String.class, Object.class, ExistingValues.class};
            @Override
            public Class<?> getColumnClass(int c) {
                return types[c];
            }
        };

        sel = primitives;
        count = new int[tags.length];

        for (int i = 0; i<tags.length; i++) {
            count[i] = 0;
            String key = tags[i][0];
            String value = tags[i][1], oldValue;
            Boolean b = Boolean.TRUE;
            ExistingValues old = new ExistingValues(key);
            for (OsmPrimitive osm : sel) {
                oldValue  = osm.get(key);
                if (oldValue!=null) {
                    old.addValue(oldValue);
                    if (!oldValue.equals(value)) {
                        b = Boolean.FALSE;
                        count[i]++;
                    }
                }
            }
            tm.setValueAt(b, i, 0);
            tm.setValueAt(tags[i][0], i, 1);
            tm.setValueAt(tags[i][1].isEmpty() ? new DeleteTagMarker(count[i]) : tags[i][1], i, 2);
            tm.setValueAt(old , i, 3);
        }

        propertyTable = new JTable(tm) {

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
                if (value instanceof DeleteTagMarker) return null;
                if (value instanceof ExistingValues) return null;
                return getDefaultEditor(value.getClass());
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                int r = rowAtPoint(event.getPoint());
                int c = columnAtPoint(event.getPoint());
                Object o = getValueAt(r, c);
                if (c==1 || c==2) return o.toString();
                if (c==3) return ((ExistingValues)o).getToolTip();
                return tr("Enable the checkbox to accept the value");
            }
        };

        propertyTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        // a checkbox has a size of 15 px
        propertyTable.getColumnModel().getColumn(0).setMaxWidth(15);
        TableHelper.adjustColumnWidth(propertyTable, 1, 150);
        TableHelper.adjustColumnWidth(propertyTable, 2, 400);
        TableHelper.adjustColumnWidth(propertyTable, 3, 300);
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
        if (!sender.isEmpty() && !trustedSenders.contains(sender)) {
            final JCheckBox c = new JCheckBox();
            c.setAction(new AbstractAction(tr("Accept all tags from {0} for this session", sender) ) {
                @Override public void actionPerformed(ActionEvent e) {
                    if (c.isSelected())
                        trustedSenders.add(sender);
                    else
                        trustedSenders.remove(sender);
                }
            } );
            tablePanel.add(c , GBC.eol().insets(20,10,0,0));
        }
        setContent(tablePanel);
        setDefaultButton(2);
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
        if (buttonIndex == 2) {
            trustedSenders.remove(sender);
        }
        setVisible(false);
    }

    /**
     * parse addtags parameters Example URL (part):
     * addtags=wikipedia:de%3DResidenzschloss Dresden|name:en%3DDresden Castle
     */
    public static void addTags(final Map<String, String> args, final String sender, final Collection<? extends OsmPrimitive> primitives) {
        if (args.containsKey("addtags")) {
            GuiHelper.executeByMainWorkerInEDT(new Runnable() {

                @Override
                public void run() {
                    String[] tags = null;
                    tags = Utils.decodeUrl(args.get("addtags")).split("\\|");
                    Set<String> tagSet = new HashSet<>();
                    for (String tag : tags) {
                        if (!tag.trim().isEmpty() && tag.contains("=")) {
                            tagSet.add(tag.trim());
                        }
                    }
                    if (!tagSet.isEmpty()) {
                        String[][] keyValue = new String[tagSet.size()][2];
                        int i = 0;
                        for (String tag : tagSet) {
                            // support a  =   b===c as "a"="b===c"
                            String [] pair = tag.split("\\s*=\\s*",2);
                            keyValue[i][0] = pair[0];
                            keyValue[i][1] = pair.length<2 ? "": pair[1];
                            i++;
                        }
                        addTags(keyValue, sender, primitives);
                    }
                }
            });
        }
    }

    /**
     * Ask user and add the tags he confirm.
     * @param keyValue is a table or {{tag1,val1},{tag2,val2},...}
     * @param sender is a string for skipping confirmations. Use empty string for always confirmed adding.
     * @param primitives OSM objects that will be modified
     * @since 7521
     */
    public static void addTags(String[][] keyValue, String sender, Collection<? extends OsmPrimitive> primitives) {
        if (trustedSenders.contains(sender)) {
            if (Main.main.getCurrentDataSet() != null) {
                for (String[] row : keyValue) {
                    Main.main.undoRedo.add(new ChangePropertyCommand(primitives, row[0], row[1]));
                }
            }
        } else {
            new AddTagsDialog(keyValue, sender, primitives).showDialog();
        }
    }
}
