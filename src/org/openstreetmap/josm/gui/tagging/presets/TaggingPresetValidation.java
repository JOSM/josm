// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static java.util.Collections.singleton;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.FilterModel;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.OpeningHourTest;
import org.openstreetmap.josm.data.validation.tests.TagChecker;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

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
            TagChecker tagChecker = OsmValidator.getTest(TagChecker.class);
            tagChecker.startTest(NullProgressMonitor.INSTANCE); //since initializeTest works if test is enabled
            OsmValidator.initializeTests(Arrays.asList(mapCSSTagChecker, openingHourTest, tagChecker));
            tagChecker.endTest();


            List<TestError> errors = new ArrayList<>();
            openingHourTest.addErrorsForPrimitive(primitive, errors);
            errors.addAll(mapCSSTagChecker.getErrorsForPrimitive(primitive, ValidatorPrefHelper.PREF_OTHER.get()));
            tagChecker.startTest(NullProgressMonitor.INSTANCE);
            tagChecker.check(primitive);
            errors.addAll(tagChecker.getErrors());
            tagChecker.endTest();

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
        DataSet ds = new DataSet();
        Collection<OsmPrimitive> primitives = FilterModel.getAffectedPrimitives(singleton(original));
        OsmPrimitive primitive = ds.clonePrimitives(
                new SubclassFilteredCollection<>(primitives, INode.class::isInstance),
                new SubclassFilteredCollection<>(primitives, IWay.class::isInstance),
                new SubclassFilteredCollection<>(primitives, IRelation.class::isInstance))
                .get(original);
        Command command = TaggingPreset.createCommand(singleton(primitive), changedTags);
        if (command != null) {
            command.executeCommand();
        }
        return primitive;
    }
}
