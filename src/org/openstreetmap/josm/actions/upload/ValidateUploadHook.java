// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.AggregatePrimitivesVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.validator.ValidatorTreePanel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.ValidatorLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;

/**
 * The action that does the validate thing.
 * <p>
 * This action iterates through all active tests and give them the data, so that
 * each one can test it.
 *
 * @author frsantos
 * @since 3669
 */
public class ValidateUploadHook implements UploadHook {

    /**
     * Validate the modified data before uploading
     * @param apiDataSet contains primitives to be uploaded
     * @return true if upload should continue, else false
     */
    @Override
    public boolean checkUpload(APIDataSet apiDataSet) {

        OsmValidator.initializeTests();
        Collection<Test> tests = OsmValidator.getEnabledTests(true);
        if (tests.isEmpty())
            return true;

        AggregatePrimitivesVisitor v = new AggregatePrimitivesVisitor();
        v.visit(apiDataSet.getPrimitivesToAdd());
        Collection<OsmPrimitive> selection = v.visit(apiDataSet.getPrimitivesToUpdate());

        List<TestError> errors = new ArrayList<>(30);
        for (Test test : tests) {
            test.setBeforeUpload(true);
            test.setPartialSelection(true);
            test.startTest(null);
            test.visit(selection);
            test.endTest();
            if (ValidatorPrefHelper.PREF_OTHER.get() && ValidatorPrefHelper.PREF_OTHER_UPLOAD.get()) {
                errors.addAll(test.getErrors());
            } else {
                for (TestError e : test.getErrors()) {
                    if (e.getSeverity() != Severity.OTHER) {
                        errors.add(e);
                    }
                }
            }
            test.clear();
            test.setBeforeUpload(false);
        }

        if (ValidatorPrefHelper.PREF_USE_IGNORE.get()) {
            boolean allIgnored = true;
            for (TestError error : errors) {
                if (!error.updateIgnored()) {
                    allIgnored = false;
                }
            }
            if (allIgnored)
                return true;
        }

        OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        if (editLayer != null) {
            editLayer.validationErrors.clear();
            editLayer.validationErrors.addAll(errors);
        }
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.validatorDialog.tree.setErrors(errors);
        }
        if (errors.isEmpty())
            return true;

        return displayErrorScreen(errors);
    }

    /**
     * Displays a screen where the actions that would be taken are displayed and
     * give the user the possibility to cancel the upload.
     * @param errors The errors displayed in the screen
     * @return <code>true</code>, if the upload should continue. <code>false</code>
     *          if the user requested cancel.
     */
    private static boolean displayErrorScreen(List<TestError> errors) {
        JPanel p = new JPanel(new GridBagLayout());
        ValidatorTreePanel errorPanel = new ValidatorTreePanel(errors);
        errorPanel.expandAll();
        HtmlPanel pnlMessage = new HtmlPanel();
        pnlMessage.setText("<html><body>"
                + tr("The following are results of automatic validation. Try fixing"
                + " these, but be careful (don''t destroy valid data)."
                + " When in doubt ignore them.<br>When you"
                + " cancel this dialog, you can find the entries in the validator"
                + " side panel to inspect them.")
                + "<table align=\"center\">"
                + "<tr><td align=\"left\"><b>"+tr("Errors")
                + "&nbsp;</b></td><td align=\"left\">"
                + tr("Usually this should be fixed.")+"</td></tr>"
                + "<tr><td align=\"left\"><b>"+tr("Warnings")
                + "&nbsp;</b></td><td align=\"left\">"
                + tr("Fix these when possible.")+"</td></tr>"
                + "<tr><td align=\"left\"><b>"+tr("Other")
                + "&nbsp;</b></td><td align=\"left\">"
                + tr("Informational warnings, expect many false entries.")+"</td></tr>"
                + "</table>"
        );
        pnlMessage.setPreferredSize(new Dimension(500, 150));
        p.add(pnlMessage, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(new JScrollPane(errorPanel), GBC.eol().fill(GBC.BOTH));

        ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(),
                tr("Suspicious data found. Upload anyway?"),
                tr("Continue upload"), tr("Cancel"))
            .setButtonIcons("ok", "cancel")
            .setContent(p);
        int rc = ed.showDialog().getValue();
        GuiHelper.destroyComponents(ed, false);
        ed.dispose();
        if (rc != 1) {
            OsmValidator.initializeTests();
            OsmValidator.initializeErrorLayer();
            MainApplication.getMap().validatorDialog.unfurlDialog();
            MainApplication.getLayerManager().getLayersOfType(ValidatorLayer.class).forEach(ValidatorLayer::invalidate);
            return false;
        }
        return true;
    }
}
