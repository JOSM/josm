// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

/**
 * Listens to commands on an input stream and delegates them to the help browser.
 *
 *
 */
public class HelpBrowserCommandProcessor implements Runnable {
    private static final Logger logger = Logger.getLogger(HelpBrowserCommandProcessor.class.getName());

    /** the controlled help browser*/
    private HelpBrowser browser;

    /**
     *
     * @param browser the controlled help browser
     */
    public HelpBrowserCommandProcessor(HelpBrowser browser) {
        this.browser = browser;
    }

    /**
     * Show the help page for help topic <code>helpTopic</code>.
     *
     * @param helpTopic the help topic
     */
    protected void setUrlForHelpTopic(final String helpTopic) {
        Runnable r = new Runnable() {
            public void run() {
                browser.openHelpTopic(helpTopic);
                browser.setVisible(true);
                browser.toFront();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * Exit the help browser
     */
    protected void exit() {
        Runnable r = new Runnable() {
            public void run() {
                browser.setVisible(false);
                System.exit(0);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    public void run() {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        System.in
                )
        );
        while(true) {
            String cmd = null;

            try {
                cmd = reader.readLine();
            } catch(IOException e) {
                System.out.println(tr("Failed to read command. Exiting help browser. Exception was:" + e.toString()));
                System.exit(1);
            }
            if (cmd.startsWith("exit")) {
                exit();
            } else if (cmd.startsWith("setUrlForHelpTopic ")) {
                String helpTopic = cmd.substring("setUrlForHelpTopic ".length());
                setUrlForHelpTopic(helpTopic);
            }
        }
    }
}
