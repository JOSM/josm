// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.Base64;

import java.net.URL;
import java.net.URLEncoder;
import org.openstreetmap.josm.tools.UrlLabel;

/**
 * An exception handler that asks the user to send a bug report.
 *
 * @author imi
 */
public final class BugReportExceptionHandler implements Thread.UncaughtExceptionHandler {

    public void uncaughtException(Thread t, Throwable e) {
        handleException(e);
    }
    public static void handleException(Throwable e) {
        e.printStackTrace();
        if (Main.parent != null) {
            if (e instanceof OutOfMemoryError) {
                // do not translate the string, as translation may raise an exception
                JOptionPane.showMessageDialog(Main.parent, "JOSM is out of memory. " +
                        "Strange things may happen.\nPlease restart JOSM with the -Xmx###M option,\n" +
                        "where ### is the the number of MB assigned to JOSM (e.g. 256).\n" +
                        "Currently, " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB are available to JOSM.",
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            if(PluginHandler.checkException(e))
                return;

            Object[] options = new String[]{tr("Do nothing"), tr("Report Bug")};
            int answer = JOptionPane.showOptionDialog(Main.parent, tr("An unexpected exception occurred.\n\n" +
                    "This is always a coding error. If you are running the latest\n" +
            "version of JOSM, please consider being kind and file a bug report."),
            tr("Unexpected Exception"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,null,
            options, options[0]);
            if (answer == 1) {
                try {
                    StringWriter stack = new StringWriter();
                    e.printStackTrace(new PrintWriter(stack));

                    String text = ShowStatusReportAction.getReportHeader()
                    + stack.getBuffer().toString();

                    URL url = new URL("http://josm.openstreetmap.de/josmticket?" +
                                      "data="+
                                      Base64.encode(
                                      // To note that it came from this code
                                      "keywords=template_report&" +
                                      "description=" + java.net.URLEncoder.encode(
                                                         // Note: This doesn't use tr() intentionally, we want bug reports in English
                                                         "What steps will reproduce the problem?\n"
                                                            + " 1. \n"
                                                            + " 2. \n"
                                                            + " 3. \n"
                                                            + "\n"
                                                            + "What is the expected result?\n\n"
                                                            + "What happens instead?\n\n"
                                                            + "Please provide any additional information below. Attach a screenshot if\n"
                                                            + "possible.\n\n"
                                                            + "{{{\n" + text + "\n}}}\n",
                                                         "UTF-8")));

                    JPanel p = new JPanel(new GridBagLayout());
                    p.add(new JLabel(tr("<html>" +
                                        "<p>You've encountered an error in JOSM. Before you file a bug<br>" +
                                        "make sure you've updated to the latest version of JOSM here:</p></html>")), GBC.eol());
                    p.add(new UrlLabel("http://josm.openstreetmap.de/#Download"), GBC.eop().insets(8,0,0,0));
                    p.add(new JLabel(tr("<html>You should also update your plugins. If neither of those help please<br>" +
                                        "file a bug in our bugtracker using this link:</p></html>")), GBC.eol());
                    p.add(new UrlLabel(url.toString(), "http://josm.openstreetmap.de/josmticket?..."), GBC.eop().insets(8,0,0,0));
                    p.add(new JLabel(tr("<html><p>" +
                                        "There the the error information provided below should already be<br>" +
                                        "filled out for you. Please include information on how to reproduce<br>" +
                                        "the error and try to supply as much detail as possible.</p></html>")), GBC.eop());
                    p.add(new JLabel(tr("<html><p>" +
                                        "Alternatively if that doesn't work you can manually fill in the information<br>" +
                                        "below at this URL:</p></html>")), GBC.eol());
                    p.add(new UrlLabel("http://josm.openstreetmap.de/newticket"), GBC.eop().insets(8,0,0,0));
                    try {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), new ClipboardOwner(){
                            public void lostOwnership(Clipboard clipboard, Transferable contents) {}
                        });
                        p.add(new JLabel(tr("(The text has already been copied to your clipboard.)")), GBC.eop());
                    }
                    catch (RuntimeException x) {}

                    JTextArea info = new JTextArea(text, 20, 60);
                    info.setCaretPosition(0);
                    info.setEditable(false);
                    p.add(new JScrollPane(info), GBC.eop());

                    JOptionPane.showMessageDialog(Main.parent, p, tr("You've encountered a bug in JOSM"), JOptionPane.ERROR_MESSAGE);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
