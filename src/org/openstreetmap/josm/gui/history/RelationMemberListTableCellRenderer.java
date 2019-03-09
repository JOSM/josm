// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.gui.history.TwoColumnDiff.Item;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The {@link TableCellRenderer} for a list of relation members in {@link HistoryBrowser}.
 * @since 1709
 */
public class RelationMemberListTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Constructs a new {@code RelationMemberListTableCellRenderer}.
     */
    public RelationMemberListTableCellRenderer() {
        setOpaque(true);
    }

    protected void renderIcon(RelationMemberData member) {
        if (member == null) {
            setIcon(null);
        } else {
            setIcon(ImageProvider.get(member.getMemberType()));
        }
    }

    protected void renderRole(Item diffItem) {
        RelationMemberData member = (RelationMemberData) diffItem.value;
        String text = member == null ? "" : member.getRole();
        setText(text);
        setToolTipText(text);
        GuiHelper.setBackgroundReadable(this, diffItem.state.getColor());
    }

    protected void renderPrimitive(Item diffItem) {
        String text = "";
        RelationMemberData member = (RelationMemberData) diffItem.value;
        if (member != null) {
            switch(member.getMemberType()) {
            case NODE: text = tr("Node {0}", member.getMemberId()); break;
            case WAY: text = tr("Way {0}", member.getMemberId()); break;
            case RELATION: text = tr("Relation {0}", member.getMemberId()); break;
            default: throw new AssertionError();
            }
        }
        setText(text);
        setToolTipText(text);
        GuiHelper.setBackgroundReadable(this, diffItem.state.getColor());
    }

    // Warning: The model pads with null-rows to match the size of the opposite table. 'value' could be null
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        if (value == null) return this;
        Item member = (TwoColumnDiff.Item) value;
        renderIcon((RelationMemberData) member.value);
        switch(column) {
        case 0:
            renderRole(member);
            break;
        case 1:
            renderPrimitive(member);
            break;
        default: // Do nothing
        }

        return this;
    }
}
