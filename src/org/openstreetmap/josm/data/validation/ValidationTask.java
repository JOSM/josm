// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.util.AggregatePrimitivesVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.ValidatorLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Utils;

/**
 * Asynchronous task for running a collection of tests against a collection of primitives
 */
public class ValidationTask extends PleaseWaitRunnable {
    private final Consumer<List<TestError>> onFinish;
    private Collection<Test> tests;
    private final Collection<OsmPrimitive> initialPrimitives;
    private final Collection<OsmPrimitive> formerValidatedPrimitives;
    private final boolean beforeUpload;
    private boolean canceled;
    private final List<TestError> errors = new ArrayList<>();
    private BiConsumer<ValidationTask, Test> testConsumer;

    /**
     * Constructs a new {@code ValidationTask}
     *
     * @param tests                     the tests to run
     * @param validatedPrimitives       the collection of primitives to validate.
     * @param formerValidatedPrimitives the last collection of primitives being validated. May be null.
     */
    public ValidationTask(Collection<Test> tests,
                          Collection<OsmPrimitive> validatedPrimitives,
                          Collection<OsmPrimitive> formerValidatedPrimitives) {
        this(new PleaseWaitProgressMonitor(tr("Validating")), tests, validatedPrimitives, formerValidatedPrimitives);
    }

    /**
     * Constructs a new {@code ValidationTask}
     *
     * @param onFinish                  called when the tests are finished
     * @param progressMonitor           the progress monitor to update with test progress
     * @param tests                     the tests to run
     * @param validatedPrimitives       the collection of primitives to validate.
     * @param formerValidatedPrimitives the last collection of primitives being validated. May be null.
     * @param beforeUpload              {@code true} if this is being run prior to upload
     * @since 18752
     */
    public ValidationTask(Consumer<List<TestError>> onFinish,
            ProgressMonitor progressMonitor,
            Collection<Test> tests,
            Collection<OsmPrimitive> validatedPrimitives,
            Collection<OsmPrimitive> formerValidatedPrimitives,
            boolean beforeUpload) {
        super(tr("Validating"),
                progressMonitor != null ? progressMonitor : new PleaseWaitProgressMonitor(tr("Validating")),
                false /*don't ignore exceptions */);
        this.onFinish = onFinish;
        this.initialPrimitives = validatedPrimitives;
        this.formerValidatedPrimitives = formerValidatedPrimitives;
        this.tests = tests;
        this.beforeUpload = beforeUpload;
    }

    /**
     * Find objects parent objects of given objects which should be checked for geometry problems
     * or mismatches between child tags and parent tags.
     * @param primitives the given objects
     * @return the collection of relevant parent objects
     */
    private static Set<OsmPrimitive> getRelevantParents(Collection<OsmPrimitive> primitives) {
        Set<OsmPrimitive> addedWays = new HashSet<>();
        Set<OsmPrimitive> addedRelations = new HashSet<>();
        for (OsmPrimitive p : primitives) {
            for (OsmPrimitive parent : p.getReferrers()) {
                if (parent.isDeleted())
                    continue;
                if (parent instanceof Way)
                    addedWays.add(parent);
                else
                    addedRelations.add(parent);
            }
        }

        // allow to find invalid multipolygon relations caused by moved nodes
        OsmPrimitive.getParentRelations(addedWays).stream().filter(r -> r.isMultipolygon() && !r.isDeleted())
                .forEach(addedRelations::add);
        HashSet<OsmPrimitive> extendedSet = new HashSet<>();
        extendedSet.addAll(addedWays);
        extendedSet.addAll(addedRelations);
        return extendedSet;

    }

    protected ValidationTask(ProgressMonitor progressMonitor,
            Collection<Test> tests,
            Collection<OsmPrimitive> validatedPrimitives,
            Collection<OsmPrimitive> formerValidatedPrimitives) {
        this(null, progressMonitor, tests, validatedPrimitives, formerValidatedPrimitives, false);
    }

    @Override
    protected void cancel() {
        this.canceled = true;
    }

    @Override
    protected void finish() {
        if (canceled) return;

        // Remove any low severity issues if they are not desired.
        if (!(Boolean.TRUE.equals(ValidatorPrefHelper.PREF_OTHER.get()) &&
                (!this.beforeUpload || Boolean.TRUE.equals(ValidatorPrefHelper.PREF_OTHER_UPLOAD.get())))) {
            // Use >= just in case we add additional levels.
            this.errors.removeIf(error -> error.getSeverity().getLevel() >= Severity.OTHER.getLevel());
        }

        if (!GraphicsEnvironment.isHeadless() && MainApplication.getMap() != null) {
            MapFrame map = MainApplication.getMap();
            // update GUI on Swing EDT
            GuiHelper.runInEDT(() -> {
                // see #23440 why this is inside the EDT
                if (!map.validatorDialog.isShowing() && errors.isEmpty() && beforeUpload)
                    return;
                map.validatorDialog.unfurlDialog();
                map.validatorDialog.tree.setErrors(errors);
                //FIXME: nicer way to find / invalidate the corresponding error layer
                MainApplication.getLayerManager().getLayersOfType(ValidatorLayer.class).forEach(ValidatorLayer::invalidate);
                if (!errors.isEmpty()) {
                    OsmValidator.initializeErrorLayer();
                }
            });
        }
        if (this.onFinish != null) {
            this.onFinish.accept(this.errors);
        }
    }

    @Override
    protected void realRun() {
        if (Utils.isEmpty(tests))
            return;
        int testCounter = 0;
        final boolean isPartial = this.beforeUpload || formerValidatedPrimitives != null;
        Set<OsmPrimitive> filter = null;
        Collection<OsmPrimitive> validatedPrimitives = initialPrimitives;
        if (isPartial) {
            Set<OsmPrimitive> other = Collections.emptySet();
            if (Boolean.TRUE.equals(ValidatorPrefHelper.PREF_ADD_PARENTS.get())) {
                other = getRelevantParents(initialPrimitives);
            }
            HashSet<OsmPrimitive> extendedSet = new HashSet<>();
            AggregatePrimitivesVisitor v = new AggregatePrimitivesVisitor();
            extendedSet.addAll(v.visit(initialPrimitives));
            extendedSet.addAll(other);
            validatedPrimitives = extendedSet;
            filter = new HashSet<>(initialPrimitives);
            filter.addAll(other);
        }
        getProgressMonitor().setTicksCount(tests.size() * validatedPrimitives.size());

        for (Test test : tests) {
            if (canceled)
                return;
            testCounter++;
            getProgressMonitor().setCustomText(tr("Test {0}/{1}: Starting {2}", testCounter, tests.size(), test.getName()));
            test.setBeforeUpload(this.beforeUpload);
            // Pre-upload checks only run on a partial selection.
            test.setPartialSelection(isPartial);
            test.startTest(getProgressMonitor().createSubTaskMonitor(validatedPrimitives.size(), false));
            test.visit(validatedPrimitives);
            test.endTest();
            if (isPartial && Boolean.TRUE.equals(ValidatorPrefHelper.PREF_REMOVE_IRRELEVANT.get())) {
                // #23397: remove errors for objects which were not in the initial list of primitives
                test.removeIrrelevantErrors(filter);
            }

            errors.addAll(test.getErrors());
            if (this.testConsumer != null) {
                this.testConsumer.accept(this, test);
            }
            test.clear();
            test.setBeforeUpload(false);
        }
        tests = null;
        if (Boolean.TRUE.equals(ValidatorPrefHelper.PREF_USE_IGNORE.get())) {
            getProgressMonitor().setCustomText("");
            getProgressMonitor().subTask(tr("Updating ignored errors ..."));
            for (TestError error : errors) {
                if (canceled) return;
                error.updateIgnored();
            }
        }

        if (errors.stream().anyMatch(e -> e.getPrimitives().stream().anyMatch(OsmPrimitive::isDisabledAndHidden))) {
            final String msg = "<b>" + tr("Validation results contain elements hidden by a filter.") + "</b><br/>"
                    + tr("Please review active filters to see the hidden results.");
            GuiHelper.runInEDT(() -> new Notification(msg)
                    .setDuration(Notification.TIME_LONG)
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .setHelpTopic("Dialog/Validator")
                    .show());
        }
    }

    /**
     * Gets the validation errors accumulated until this moment.
     * @return The list of errors
     */
    public List<TestError> getErrors() {
        return errors;
    }

    /**
     * A test consumer to avoid filling up memory. A test consumer <i>may</i> remove tests it has consumed.
     * @param testConsumer The consumer which takes a {@link ValidationTask} ({@code this}) and the test that finished.
     * @since 18752
     */
    public void setTestConsumer(BiConsumer<ValidationTask, Test> testConsumer) {
        this.testConsumer = testConsumer;
    }
}
