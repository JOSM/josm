// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.WikiReader;

/**
 * This is a panel that displays the current JOSM version and the ability to update JOSM.
 * @author Michael Zangl
 * @since 10649
 */
public class JosmUpdatePanel extends JPanel {
    private final JMultilineLabel testedVersionField;
    private final int josmVersion;

    /**
     * Create a new {@link JosmUpdatePanel}
     */
    public JosmUpdatePanel() {
        super(new GridBagLayout());
        josmVersion = Version.getInstance().getVersion();

        add(new JMultilineLabel(tr("Your current version of JOSM is {0}", Integer.toString(josmVersion))), GBC.eol().fill(GBC.HORIZONTAL));
        testedVersionField = new JMultilineLabel(tr("JOSM is searching for updates..."));
        add(testedVersionField, GBC.eol().fill(GBC.HORIZONTAL));

        checkCurrentVersion();
    }

    private void checkCurrentVersion() {
        new Thread(this::readCurrentVersion, "JOSM version checker").start();
    }

    private void readCurrentVersion() {
        int testedVersion = getTestedVersion();

        if (testedVersion < 0) {
            SwingUtilities.invokeLater(this::displayError);
        } else if (josmVersion < testedVersion) {
            SwingUtilities.invokeLater(() -> displayOutOfDate(testedVersion));
        } else {
            SwingUtilities.invokeLater(this::displayUpToDate);
        }
    }

    private static int getTestedVersion() {
        try {
            String testedString = new WikiReader().read(Main.getJOSMWebsite() + "/tested");
            return Integer.parseInt(testedString.trim());
        } catch (NumberFormatException | IOException e) {
            Logging.log(Logging.LEVEL_WARN, "Unable to detect current tested version of JOSM:", e);
            return -1;
        }
    }

    /**
     * Display that there was an error while checking the current version.
     */
    private void displayError() {
        testedVersionField.setText(tr("An error occured while checking if your JOSM instance is up to date."));
        showUpdateButton();
    }

    private void displayUpToDate() {
        testedVersionField.setText(tr("JOSM is up to date."));
    }

    private void displayOutOfDate(int testedVersion) {
        testedVersionField
                .setText(tr("JOSM is out of date. The current version is {0}. Try updating JOSM.", Integer.toString(testedVersion)));
        showUpdateButton();
    }

    private void showUpdateButton() {
        add(new JMultilineLabel(tr("Before you file a bug report make sure you have updated to the latest version of JOSM here:")), GBC.eol());
        add(new UrlLabel(Main.getJOSMWebsite(), 2), GBC.eop().insets(8, 0, 0, 0));
        JButton updateButton = new JButton(tr("Update JOSM"), ImageProvider.get("download"));
        updateButton.addActionListener(e -> openJosmUpdateSite());
        add(updateButton, GBC.eol().anchor(GBC.EAST));
    }

    private static void openJosmUpdateSite() {
        try {
            Main.platform.openUrl(Main.getJOSMWebsite());
        } catch (IOException ex) {
            Logging.log(Logging.LEVEL_WARN, "Unable to access JOSM website:", ex);
        }
    }
}
