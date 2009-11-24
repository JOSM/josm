// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.RelationMember;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The {@see TableCellRenderer} for a list of relation members in {@see HistoryBrower}
 *
 *
 */
public class RelationMemberListTableCellRenderer extends JLabel implements TableCellRenderer {

    public final static Color BGCOLOR_EMPTY_ROW = new Color(234,234,234);
    public final static Color BGCOLOR_NOT_IN_OPPOSITE = new Color(255,197,197);
    public final static Color BGCOLOR_IN_OPPOSITE = new Color(255,234,213);
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    private HashMap<OsmPrimitiveType, ImageIcon> icons;

    public RelationMemberListTableCellRenderer(){
        setOpaque(true);
        icons = new HashMap<OsmPrimitiveType, ImageIcon>();
        icons.put(OsmPrimitiveType.NODE, ImageProvider.get("data", "node"));
        icons.put(OsmPrimitiveType.WAY, ImageProvider.get("data", "way"));
        icons.put(OsmPrimitiveType.RELATION, ImageProvider.get("data", "relation"));
    }

    protected void renderIcon(RelationMember member) {
        if (member == null) {
            setIcon(null);
        } else {
            setIcon(icons.get(member.getPrimitiveType()));
        }
    }

    protected void renderRole( HistoryBrowserModel.RelationMemberTableModel model, RelationMember member, int row, boolean isSelected) {
        String text = "";
        Color bgColor = Color.WHITE;
        if (member == null) {
            bgColor = BGCOLOR_EMPTY_ROW;
        } else {
            text = member.getRole();
            if (model.isSameInOppositeWay(row)) {
                bgColor = Color.WHITE;
            } else if (model.isInOppositeWay(row)) {
                bgColor = BGCOLOR_IN_OPPOSITE;
            } else {
                bgColor = BGCOLOR_NOT_IN_OPPOSITE;
            }
        }
        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
    }

    protected void renderPrimitive( HistoryBrowserModel.RelationMemberTableModel model, RelationMember member, int row, boolean isSelected) {
        String text = "";
        Color bgColor = Color.WHITE;
        if (member == null) {
            bgColor = BGCOLOR_EMPTY_ROW;
        } else {
            text = "";
            switch(member.getPrimitiveType()) {
            case NODE: text = tr("Node {0}", member.getPrimitiveId()); break;
            case WAY: text = tr("Way {0}", member.getPrimitiveId()); break;
            case RELATION: text = tr("Relation {0}", member.getPrimitiveId()); break;
            }
            if (model.isSameInOppositeWay(row)) {
                bgColor = Color.WHITE;
            } else if (model.isInOppositeWay(row)) {
                bgColor = BGCOLOR_IN_OPPOSITE;
            } else {
                bgColor = BGCOLOR_NOT_IN_OPPOSITE;
            }
        }
        setText(text);
        setToolTipText(text);
        setBackground(bgColor);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        HistoryBrowserModel.RelationMemberTableModel model = gteRelationMemberTableModel(table);
        RelationMember member = (RelationMember)value;
        renderIcon(member);
        switch(column) {
        case 0:
            renderRole(model, member, row, isSelected);
            break;
        case 1:
            renderPrimitive(model, member, row, isSelected);
            break;
        }

        return this;
    }

    protected HistoryBrowserModel.RelationMemberTableModel gteRelationMemberTableModel(JTable table) {
        return (HistoryBrowserModel.RelationMemberTableModel) table.getModel();
    }
}
