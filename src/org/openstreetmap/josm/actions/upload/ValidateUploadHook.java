// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.AgregatePrimitivesVisitor;
import org.openstreetmap.josm.gui.dialogs.validator.ValidatorTreePanel;
import org.openstreetmap.josm.gui.preferences.ValidatorPreference;
import org.openstreetmap.josm.tools.GBC;

/**
 * The action that does the validate thing.
 * <p>
 * This action iterates through all active tests and give them the data, so that
 * each one can test it.
 *
 * @author frsantos
 */
public class ValidateUploadHook implements UploadHook
{
    /** Serializable ID */
    private static final long serialVersionUID = -2304521273582574603L;

    /**
     * Validate the modified data before uploading
     */
    public boolean checkUpload(APIDataSet apiDataSet) {

        Collection<Test> tests = OsmValidator.getEnabledTests(true);
        if (tests.isEmpty())
            return true;

        AgregatePrimitivesVisitor v = new AgregatePrimitivesVisitor();
        v.visit(apiDataSet.getPrimitivesToAdd());
        Collection<OsmPrimitive> selection = v.visit(apiDataSet.getPrimitivesToUpdate());

        List<TestError> errors = new ArrayList<TestError>(30);
        for (Test test : tests) {
            test.setBeforeUpload(true);
            test.setPartialSelection(true);
            test.startTest(null);
            test.visit(selection);
            test.endTest();
            if (Main.pref.getBoolean(ValidatorPreference.PREF_OTHER_UPLOAD, false)) {
                errors.addAll( test.getErrors() );
            }
            else {
                for (TestError e : test.getErrors()) {
                    if (e.getSeverity() != Severity.OTHER) {
                        errors.add(e);
                    }
                }
            }
        }
        tests = null;
        if (errors == null || errors.isEmpty())
            return true;

        if (Main.pref.getBoolean(ValidatorPreference.PREF_USE_IGNORE, true)) {
            int nume = 0;
            for (TestError error : errors) {
                List<String> s = new ArrayList<String>();
                s.add(error.getIgnoreState());
                s.add(error.getIgnoreGroup());
                s.add(error.getIgnoreSubGroup());
                for (String state : s) {
                    if (state != null && OsmValidator.hasIgnoredError(state)) {
                        error.setIgnored(true);
                    }
                }
                if (!error.getIgnored()) {
                    ++nume;
                }
            }
            if (nume == 0)
                return true;
        }
        return displayErrorScreen(errors);
    }

    /**
     * Displays a screen where the actions that would be taken are displayed and
     * give the user the possibility to cancel the upload.
     * @param errors The errors displayed in the screen
     * @return <code>true</code>, if the upload should continue. <code>false</code>
     *          if the user requested cancel.
     */
    private boolean displayErrorScreen(List<TestError> errors) {
        JPanel p = new JPanel(new GridBagLayout());
        ValidatorTreePanel errorPanel = new ValidatorTreePanel(errors);
        errorPanel.expandAll();
        p.add(new JScrollPane(errorPanel), GBC.eol());

        int res = JOptionPane.showConfirmDialog(Main.parent, p,
            tr("Data with errors. Upload anyway?"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.NO_OPTION) {
            OsmValidator.initializeErrorLayer();
            Main.map.validatorDialog.unfurlDialog();
            Main.map.validatorDialog.tree.setErrors(errors);
            Main.main.getCurrentDataSet().fireSelectionChanged();
        }
        return res == JOptionPane.YES_OPTION;
    }
}
