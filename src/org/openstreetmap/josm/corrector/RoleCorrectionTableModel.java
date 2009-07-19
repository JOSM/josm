// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import org.openstreetmap.josm.gui.PrimitiveNameFormatter;

public class RoleCorrectionTableModel extends
CorrectionTableModel<RoleCorrection> {
    private static final PrimitiveNameFormatter NAME_FORMATTER = new PrimitiveNameFormatter();

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
        }
        return null;
    }

    @Override
    public Object getCorrectionValueAt(int rowIndex, int colIndex) {
        RoleCorrection roleCorrection = getCorrections().get(rowIndex);

        switch (colIndex) {
        case 0:
            return NAME_FORMATTER.getName(roleCorrection.relation);
        case 1:
            return roleCorrection.member.role;
        case 2:
            return roleCorrection.newRole;
        }
        return null;
    }

    @Override
    protected boolean isBoldCell(int row, int column) {
        return column == 2;
    }

}
