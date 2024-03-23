// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.ValidationTask;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.validator.ValidatorTreePanel;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;

/**
 * The action that does the validate thing.
 * <p>
 * This action iterates through all active tests and gives them the data, so that
 * each one can test it.
 *
 * @author frsantos
 * @since 3669
 */
public class ValidateUploadHook implements UploadHook {

    /**
     * Validate the modified data before uploading
     * @param apiDataSet contains primitives to be uploaded
     * @return {@code true} if upload should continue, else false
     */
    @Override
    public boolean checkUpload(APIDataSet apiDataSet) {
        AtomicBoolean returnCode = new AtomicBoolean();
        Collection<OsmPrimitive> toCheck = new HashSet<>();
        toCheck.addAll(apiDataSet.getPrimitivesToAdd());
        toCheck.addAll(apiDataSet.getPrimitivesToUpdate());
        OsmValidator.initializeTests();
        new ValidationTask(errors -> {
            if (errors.stream().allMatch(TestError::isIgnored)) {
                returnCode.set(true);
            } else {
                // Unfortunately, the progress monitor is not "finished" until after `finish` is called, so we will
                // have a ProgressMonitor open behind the error screen. Fortunately, the error screen appears in front
                // of the progress monitor.
                GuiHelper.runInEDTAndWait(() -> returnCode.set(displayErrorScreen(errors)));
            }
        }, null, OsmValidator.getEnabledTests(true), toCheck, null, true).run();

        return returnCode.get();
    }

    /**
     * Displays a screen where the actions that would be taken are displayed and
     * give the user the possibility to cancel the upload.
     * @param errors The errors displayed in the screen
     * @return {@code true}, if the upload should continue.<br>
     *         {@code false}, if the user requested cancel.
     */
    private static boolean displayErrorScreen(List<TestError> errors) {
        JPanel p = new JPanel(new GridBagLayout());
        ValidatorTreePanel errorPanel = new ValidatorTreePanel(errors);
        errorPanel.expandAll();
        HtmlPanel pnlMessage = new HtmlPanel();
        pnlMessage.setText("<html><body>"
                + tr("The JOSM data validator partially checked the objects to be"
                + " uploaded and found some problems. Try fixing them, but do not"
                + " harm valid data. When in doubt ignore the findings.<br>"
                + " You can see the findings in the Validator Results panel too."
                + " Further checks on all data can be started from that panel.")
                + "<table align=\"center\">"
                + "<tr><td align=\"left\"><b>"+tr("Errors")
                + "&nbsp;</b></td><td align=\"left\">"
                + tr("Usually this should be fixed.")+"</td></tr>"
                + "<tr><td align=\"left\"><b>"+tr("Warnings")
                + "&nbsp;</b></td><td align=\"left\">"
                + tr("Fix these when possible.")+"</td></tr>"
                + "<tr><td align=\"left\"><b>"+tr("Other")
                + "&nbsp;</b></td><td align=\"left\">"
                + tr("Informational hints, expect many false entries.")+"</td></tr>"
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
        return rc == 1;
    }
}
