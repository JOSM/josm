// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
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
    private final Collection<OsmPrimitive> validatedPrimitives;
    private final Collection<OsmPrimitive> formerValidatedPrimitives;
    private final boolean beforeUpload;
    private boolean canceled;
    private List<TestError> errors;
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
        this.validatedPrimitives = validatedPrimitives;
        this.formerValidatedPrimitives = formerValidatedPrimitives;
        this.tests = tests;
        this.beforeUpload = beforeUpload;
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

        if (!GraphicsEnvironment.isHeadless() && MainApplication.getMap() != null) {
            // update GUI on Swing EDT
            GuiHelper.runInEDT(() -> {
                MapFrame map = MainApplication.getMap();
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
            // Remove any low severity issues if they are not desired.
            if (!(Boolean.TRUE.equals(ValidatorPrefHelper.PREF_OTHER.get()) &&
                    (!this.beforeUpload || Boolean.TRUE.equals(ValidatorPrefHelper.PREF_OTHER_UPLOAD.get())))) {
                // Use >= just in case we add additional levels.
                this.errors.removeIf(error -> error.getSeverity().getLevel() >= Severity.OTHER.getLevel());
            }
            this.onFinish.accept(this.errors);
        }
    }

    @Override
    protected void realRun() {
        if (Utils.isEmpty(tests))
            return;
        errors = new ArrayList<>();
        getProgressMonitor().setTicksCount(tests.size() * validatedPrimitives.size());
        int testCounter = 0;
        for (Test test : tests) {
            if (canceled)
                return;
            testCounter++;
            getProgressMonitor().setCustomText(tr("Test {0}/{1}: Starting {2}", testCounter, tests.size(), test.getName()));
            test.setBeforeUpload(this.beforeUpload);
            // Pre-upload checks only run on a partial selection.
            test.setPartialSelection(this.beforeUpload || formerValidatedPrimitives != null);
            test.startTest(getProgressMonitor().createSubTaskMonitor(validatedPrimitives.size(), false));
            test.visit(validatedPrimitives);
            test.endTest();
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
