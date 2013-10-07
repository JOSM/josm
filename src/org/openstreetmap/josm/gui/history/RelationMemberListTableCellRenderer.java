// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.gui.history.TwoColumnDiff.Item;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The {@link TableCellRenderer} for a list of relation members in {@link HistoryBrowser}
 *
 *
 */
public class RelationMemberListTableCellRenderer extends JLabel implements TableCellRenderer {

    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);
    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255,197,197);
    public final static Color BGCOLOR_IN_OPPOSITE = new Color(255,234,213);
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    private Map<OsmPrimitiveType, ImageIcon> icons;

    public RelationMemberListTableCellRenderer(){
        setOpaque(true);
        icons = new HashMap<OsmPrimitiveType, ImageIcon>();
        icons.put(OsmPrimitiveType.NODE, ImageProvider.get("data", "node"));
        icons.put(OsmPrimitiveType.WAY, ImageProvider.get("data", "way"));
        icons.put(OsmPrimitiveType.RELATION, ImageProvider.get("data", "relation"));
    }

    protected void renderIcon(RelationMemberData member) {
        if (member == null) {
            setIcon(null);
        } else {
            setIcon(icons.get(member.getMemberType()));
        }
    }

    protected void renderRole(Item diffItem, int row, boolean isSelected) {
        String text = "";
        Color bgColor = diffItem.state.getColor();
        RelationMemberData member = (RelationMemberData) diffItem.value;
        text = member == null?"":member.getRole();
        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
    }

    protected void renderPrimitive(Item diffItem, int row, boolean isSelected) {
        String text = "";
        Color bgColor = diffItem.state.getColor();
        RelationMemberData member = (RelationMemberData) diffItem.value;
        text = "";
        if (member != null) {
            switch(member.getMemberType()) {
            case NODE: text = tr("Node {0}", member.getMemberId()); break;
            case WAY: text = tr("Way {0}", member.getMemberId()); break;
            case RELATION: text = tr("Relation {0}", member.getMemberId()); break;
            }
        }
        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
    }

    // Warning: The model pads with null-rows to match the size of the opposite table. 'value' could be null
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        Item member = (TwoColumnDiff.Item)value;
        renderIcon((RelationMemberData) member.value);
        switch(column) {
        case 0:
            renderRole(member, row, isSelected);
            break;
        case 1:
            renderPrimitive(member, row, isSelected);
            break;
        }

        return this;
    }

    protected DiffTableModel getRelationMemberTableModel(JTable table) {
        return (DiffTableModel) table.getModel();
    }
}
