// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import java.awt.Color;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is the {@see TableCellRenderer} used in the node tables of {@see NodeListMerger}.
 *
 */
public  class NodeListTableCellRenderer extends JLabel implements TableCellRenderer {
    //static private final Logger logger = Logger.getLogger(NodeListTableCellRenderer.class.getName());
    //private static DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000");
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);
    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);
    public final static Color BGCOLOR_FROZEN = new Color(234,234,234);
    public final static Color BGCOLOR_PARTICIPAING_IN_COMPARISON = Color.BLACK;
    public final static Color FGCOLOR_PARTICIPAING_IN_COMPARISON = Color.WHITE;

    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255,197,197);
    public final static Color BGCOLOR_IN_OPPOSITE = new Color(255,234,213);
    public final static Color BGCOLOR_SAME_POSITION_IN_OPPOSITE = new Color(217,255,217);

    private final ImageIcon icon;
    private final Border rowNumberBorder;

    /**
     * constructor
     */
    public NodeListTableCellRenderer() {
        icon = ImageProvider.get("data", "node");
        rowNumberBorder = BorderFactory.createEmptyBorder(0,4,0,0);
        setOpaque(true);
    }

    /**
     * build the tool tip text for an {@see OsmPrimitive}. It consist of the formatted
     * key/value pairs for this primitive.
     *
     * @param primitive
     * @return the tool tip text
     */
    public String buildToolTipText(OsmPrimitive primitive) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html>");
        // show the id
        //
        sb.append("<strong>id</strong>=")
        .append(primitive.getId())
        .append("<br>");

        // show the key/value-pairs, sorted by key
        //
        ArrayList<String> keyList = new ArrayList<String>(primitive.keySet());
        Collections.sort(keyList);
        for (int i = 0; i < keyList.size(); i++) {
            if (i > 0) {
                sb.append("<br>");
            }
            String key = keyList.get(i);
            sb.append("<strong>")
            .append(key)
            .append("</strong>")
            .append("=");
            // make sure long values are split into several rows. Otherwise
            // the tool tip window can become to wide
            //
            String value = primitive.get(key);
            while(value.length() != 0) {
                sb.append(value.substring(0,Math.min(50, value.length())));
                if (value.length() > 50) {
                    sb.append("<br>");
                    value = value.substring(50);
                } else {
                    value = "";
                }
            }
        }
        sb.append("</html>");
        return sb.toString();
    }

    /**
     * reset the renderer
     */
    protected void reset() {
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }

    /**
     * render a node
     * @param model  the model
     * @param node the node
     * @param isSelected true, if the current row is selected
     */
    protected  void renderNode(ListMergeModel<Node>.EntriesTableModel model, Node node, int row, boolean isSelected) {
        setIcon(icon);
        setBorder(null);
        if (model.getListMergeModel().isFrozen()) {
            setBackground(BGCOLOR_FROZEN);
        } else if (isSelected) {
            setBackground(BGCOLOR_SELECTED);
        } else if (model.isParticipatingInCurrentComparePair()) {
            if (model.isSamePositionInOppositeList(row)) {
                setBackground(BGCOLOR_SAME_POSITION_IN_OPPOSITE);
            } else if (model.isIncludedInOppositeList(row)) {
                setBackground(BGCOLOR_IN_OPPOSITE);
            } else {
                setBackground(BGCOLOR_NOT_IN_OPPOSITE);
            }
        }
        setText(node.getDisplayName(DefaultNameFormatter.getInstance()));
        setToolTipText(buildToolTipText(node));
    }

    /**
     * render an empty row
     */
    protected void renderEmptyRow() {
        setIcon(null);
        setBackground(BGCOLOR_EMPTY_ROW);
        setText("");
    }

    /**
     * render the row id
     * @param model  the model
     * @param row the row index
     * @param isSelected true, if the current row is selected
     */
    protected  void renderRowId( ListMergeModel<Node>.EntriesTableModel model, int row, boolean isSelected) {
        setIcon(null);
        setBorder(rowNumberBorder);
        if (model.getListMergeModel().isFrozen()) {
            setBackground(BGCOLOR_FROZEN);
        } else if (model.isParticipatingInCurrentComparePair()) {
            setBackground(BGCOLOR_PARTICIPAING_IN_COMPARISON);
            setForeground(FGCOLOR_PARTICIPAING_IN_COMPARISON);
        }
        setText(Integer.toString(row+1));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        Node node = (Node)value;
        reset();
        switch(column) {
        case 0:
            renderRowId(getModel(table),row, isSelected);
            break;
        case 1:
            if (node == null) {
                renderEmptyRow();
            } else {
                renderNode(getModel(table), node, row, isSelected);
            }
            break;
        default:
            // should not happen
            throw new RuntimeException(MessageFormat.format("Unexpected column index. Got {0}.", column));
        }
        return this;
    }

    /**
     * replies the model
     * @param table  the table
     * @return the table model
     */
    @SuppressWarnings("unchecked")
    protected ListMergeModel<Node>.EntriesTableModel getModel(JTable table) {
        return (ListMergeModel.EntriesTableModel)table.getModel();
    }
}
