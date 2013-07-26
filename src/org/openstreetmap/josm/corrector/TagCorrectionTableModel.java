// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

public class TagCorrectionTableModel extends CorrectionTableModel<TagCorrection> {

    public TagCorrectionTableModel(List<TagCorrection> tagCorrections) {
        super(tagCorrections);
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getCorrectionColumnName(int colIndex) {
        switch (colIndex) {
        case 0:
            return tr("Old key");
        case 1:
            return tr("Old value");
        case 2:
            return tr("New key");
        case 3:
            return tr("New value");
        }
        return null;
    }

    @Override
    public Object getCorrectionValueAt(int rowIndex, int colIndex) {
        TagCorrection tagCorrection = getCorrections().get(rowIndex);

        switch (colIndex) {
        case 0:
            return tagCorrection.oldKey;
        case 1:
            return tagCorrection.oldValue;
        case 2:
            return tagCorrection.newKey;
        case 3:
            return tagCorrection.newValue;
        }
        return null;
    }

    @Override
    protected boolean isBoldCell(int row, int column) {
        TagCorrection tagCorrection = getCorrections().get(row);
        return (column == 2 && tagCorrection.isKeyChanged())
                || (column == 3 && tagCorrection.isValueChanged());
    }

}
