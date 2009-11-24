// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.io.File;
import static org.openstreetmap.josm.tools.I18n.tr;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;

/**
 * This is the proxy class for the help browser running in its own process.
 *
 *
 */
public class HelpBrowserProxy {

    /** the unique instance of the proxy */
    private static HelpBrowserProxy instance;

    /**
     * replies the unique instance of the proxy
     *
     * @return the unique instance of the proxy
     */
    static public HelpBrowserProxy getInstance() {
        if (instance == null) {
            instance = new HelpBrowserProxy();
        }
        return instance;
    }

    /** the process running the help browser */
    private Process helpBrowserProcess;
    /** the print writer to the input stream of the help browser process */
    private PrintWriter pw;

    /**
     * launches the help browser in its own process
     *
     */
    protected void launch() {
        ArrayList<String> cmdLine = new ArrayList<String>();
        String javaBin = null;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            javaBin = "javaw.exe";
        } else {
            javaBin = "java";
        }
        cmdLine.add(new File(new File(System.getProperty("java.home"), "bin"), javaBin).toString());
        cmdLine.add("-classpath");
        cmdLine.add(System.getProperty("java.class.path"));
        cmdLine.add("org.openstreetmap.josm.gui.help.HelpApplication");
        if (System.getProperty("josm.home") != null) {
            cmdLine.add("-Djosm.home="+System.getProperty("josm.home"));
        }
        String[] cmds = new String[cmdLine.size()];
        cmdLine.toArray(cmds);
        try {
            helpBrowserProcess = Runtime.getRuntime().exec(cmds);
        } catch(IOException e) {
            e.printStackTrace();
        }
        if (helpBrowserProcess != null) {
            pw = new PrintWriter(
                    new OutputStreamWriter(
                            helpBrowserProcess.getOutputStream()
                    )
            );
        }
    }

    /**
     * Direct the help browser to the help page for help topic
     * <code>relativeHelpTopic</code>
     *
     * @param relativeHelpTopic the help topic
     */
    public void setUrlForHelpTopic(String relativeHelpTopic) {
        if (helpBrowserProcess == null) {
            launch();
        }
        if (helpBrowserProcess == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Failed to launch the external help browser"),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            System.err.println("Failed to launch browser");
            return;
        }
        pw.println("setUrlForHelpTopic " + relativeHelpTopic);
        pw.flush();
    }

    /**
     * Exit the help browser
     */
    public void exit() {
        if (helpBrowserProcess == null)
            return;
        pw.println("exit");
        pw.flush();
        pw.close();
        helpBrowserProcess.destroy();
    }
}
