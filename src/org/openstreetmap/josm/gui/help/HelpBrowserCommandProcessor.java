// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.SwingUtilities;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Listens to commands on an input stream and delegates them to the help browser.
 * 
 *
 */
public class HelpBrowserCommandProcessor implements Runnable {

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
     * @param helpTopics the help topic
     */
    protected void setUrlForHelpTopics(final String helpTopics) {
        Runnable r = new Runnable() {
            public void run() {
                browser.setUrlForHelpTopic(helpTopics);
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
            } else if (cmd.startsWith("setUrlForHelpTopics ")) {
                String helpTopics = cmd.substring("setUrlForHelpTopics ".length());
                setUrlForHelpTopics(helpTopics);
            }
        }
    }
}

