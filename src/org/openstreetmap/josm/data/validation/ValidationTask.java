// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

/**
 * Asynchronous task for running a collection of tests against a collection of primitives
 */
public class ValidationTask extends PleaseWaitRunnable {
    private Collection<Test> tests;
    private final Collection<OsmPrimitive> validatedPrimitives;
    private final Collection<OsmPrimitive> formerValidatedPrimitives;
    private boolean canceled;
    private List<TestError> errors;

    /**
     * Constructs a new {@code ValidationTask}
     *
     * @param tests                     the tests to run
     * @param validatedPrimitives       the collection of primitives to validate.
     * @param formerValidatedPrimitives the last collection of primitives being validates. May be null.
     */
    public ValidationTask(Collection<Test> tests,
                          Collection<OsmPrimitive> validatedPrimitives,
                          Collection<OsmPrimitive> formerValidatedPrimitives) {
        this(new PleaseWaitProgressMonitor(tr("Validating")), tests, validatedPrimitives, formerValidatedPrimitives);
    }

    protected ValidationTask(ProgressMonitor progressMonitor,
                             Collection<Test> tests,
                             Collection<OsmPrimitive> validatedPrimitives,
                             Collection<OsmPrimitive> formerValidatedPrimitives) {
        super(tr("Validating"), progressMonitor, false /*don't ignore exceptions */);
        this.validatedPrimitives = validatedPrimitives;
        this.formerValidatedPrimitives = formerValidatedPrimitives;
        this.tests = tests;
    }

    @Override
    protected void cancel() {
        this.canceled = true;
    }

    @Override
    protected void finish() {
        if (canceled) return;

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

    @Override
    protected void realRun() {
        if (tests == null || tests.isEmpty())
            return;
        errors = new ArrayList<>();
        getProgressMonitor().setTicksCount(tests.size() * validatedPrimitives.size());
        int testCounter = 0;
        for (Test test : tests) {
            if (canceled)
                return;
            testCounter++;
            getProgressMonitor().setCustomText(tr("Test {0}/{1}: Starting {2}", testCounter, tests.size(), test.getName()));
            test.setBeforeUpload(false);
            test.setPartialSelection(formerValidatedPrimitives != null);
            test.startTest(getProgressMonitor().createSubTaskMonitor(validatedPrimitives.size(), false));
            test.visit(validatedPrimitives);
            test.endTest();
            errors.addAll(test.getErrors());
            test.clear();
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
}
