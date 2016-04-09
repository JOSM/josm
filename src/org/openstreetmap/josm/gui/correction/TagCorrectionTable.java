// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.correction;

import java.util.List;

import org.openstreetmap.josm.data.correction.TagCorrection;

/**
 * Tag correction table.
 * @since 729
 */
public class TagCorrectionTable extends CorrectionTable<TagCorrectionTableModel> {

    /**
     * Constructs a new {@code TagCorrectionTable}.
     * @param tagCorrections tag corrections
     */
    public TagCorrectionTable(List<TagCorrection> tagCorrections) {
        super(new TagCorrectionTableModel(tagCorrections));
    }
}
