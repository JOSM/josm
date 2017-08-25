// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.correction;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import org.openstreetmap.josm.data.correction.RoleCorrection;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;

/**
 * Role correction table model.
 * @since 1001
 */
public class RoleCorrectionTableModel extends CorrectionTableModel<RoleCorrection> {

    /**
     * Constructs a new {@code RoleCorrectionTableModel}.
     * @param roleCorrections list of role corrections
     */
    public RoleCorrectionTableModel(List<RoleCorrection> roleCorrections) {
        super(roleCorrections);
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getCorrectionColumnName(int colIndex) {
        switch (colIndex) {
        case 0:
            return tr("Relation");
        case 1:
            return tr("Old role");
        case 2:
            return tr("New role");
        default:
            return null;
        }
    }

    @Override
    public Object getCorrectionValueAt(int rowIndex, int colIndex) {
        RoleCorrection roleCorrection = getCorrections().get(rowIndex);

        switch (colIndex) {
        case 0:
            return roleCorrection.relation.getDisplayName(DefaultNameFormatter.getInstance());
        case 1:
            return roleCorrection.member.getRole();
        case 2:
            return roleCorrection.newRole;
        default:
            return null;
        }
    }

    @Override
    protected boolean isBoldCell(int row, int column) {
        return column == 2;
    }
}
