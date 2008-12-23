// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import java.util.List;

public class TagCorrectionTable extends
        CorrectionTable<TagCorrectionTableModel> {

    public TagCorrectionTable(List<TagCorrection> tagCorrections) {
        super(new TagCorrectionTableModel(tagCorrections));
    }

}
