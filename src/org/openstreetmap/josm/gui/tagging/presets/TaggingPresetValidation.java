// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.OpeningHourTest;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Validates the preset user input a the given primitive.
 */
interface TaggingPresetValidation {

    /**
     * Asynchronously validates the user input for the given primitive.
     * @param original the primitive
     * @param validationLabel the label for validation errors
     * @param changedTags the list of tags that are set by this preset
     */
    static void validateAsync(OsmPrimitive original, JLabel validationLabel, List<Tag> changedTags) {
        OsmPrimitive primitive = applyChangedTags(original, changedTags);
        MainApplication.worker.execute(() -> validate(primitive, validationLabel));
    }

    /**
     * Validates the user input for the given primitive.
     * @param primitive the primitive
     * @param validationLabel the label for validation errors
     */
    static void validate(OsmPrimitive primitive, JLabel validationLabel) {
        try {
            MapCSSTagChecker mapCSSTagChecker = OsmValidator.getTest(MapCSSTagChecker.class);
            OpeningHourTest openingHourTest = OsmValidator.getTest(OpeningHourTest.class);
            OsmValidator.initializeTests(Arrays.asList(mapCSSTagChecker, openingHourTest));

            List<TestError> errors = new ArrayList<>();
            openingHourTest.addErrorsForPrimitive(primitive, errors);
            errors.addAll(mapCSSTagChecker.getErrorsForPrimitive(primitive, ValidatorPrefHelper.PREF_OTHER.get()));

            boolean visible = !errors.isEmpty();
            String toolTipText = "<html>" + Utils.joinAsHtmlUnorderedList(Utils.transform(errors, e ->
                    e.getDescription() == null ? e.getMessage() : tr("{0} ({1})", e.getMessage(), e.getDescription())));
            GuiHelper.runInEDTAndWait(() -> {
                validationLabel.setVisible(visible);
                validationLabel.setToolTipText(toolTipText);
            });
        } catch (Exception e) {
            Logging.warn("Failed to validate {0}", primitive);
            Logging.warn(e);
        } finally {
            primitive.getDataSet().clear();
        }
    }

    static OsmPrimitive applyChangedTags(OsmPrimitive original, List<Tag> changedTags) {
        OsmPrimitive primitive = clone(original);
        new DataSet(primitive);
        Command command = TaggingPreset.createCommand(Collections.singleton(primitive), changedTags);
        if (command != null) {
            command.executeCommand();
        }
        return primitive;
    }

    static OsmPrimitive clone(OsmPrimitive original) {
        if (original instanceof Node) {
            return new Node(((Node) original));
        } else if (original instanceof Way) {
            return new Way(((Way) original), false, false);
        } else if (original instanceof Relation) {
            return new Relation(((Relation) original), false, false);
        } else {
            throw new IllegalStateException();
        }
    }
}
