// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.plugins.PluginHandler;

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
                        "where ### is the number of MB assigned to JOSM (e.g. 256).\n" +
                        "Currently, " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB are available to JOSM.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Give the user a chance to deactivate the plugin which threw the exception (if it
            // was thrown from a plugin)
            //
            PluginHandler.disablePluginAfterException(e);

            // Then ask for submitting a bug report, for exceptions thrown from a plugin too
            //
            Object[] options = new String[]{tr("Do nothing"), tr("Report Bug")};
            int answer = JOptionPane.showOptionDialog(
                    Main.parent,
                    "<html>"
                    + tr("An unexpected exception occurred.<br>" +
                            "This is always a coding error. If you are running the latest<br>" +
                            "version of JOSM, please consider being kind and file a bug report."
                    )
                    + "</html>",
                    tr("Unexpected Exception"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    options, options[0]
            );
            if (answer != 1)  return;

            try {
                final int maxlen = 7000;
                StringWriter stack = new StringWriter();
                e.printStackTrace(new PrintWriter(stack));

                String text = ShowStatusReportAction.getReportHeader()
                + stack.getBuffer().toString();
                String urltext = text.replaceAll("\r",""); /* strip useless return chars */
                if(urltext.length() > maxlen)
                {
                    urltext = urltext.substring(0,maxlen);
                    int idx = urltext.lastIndexOf("\n");
                    /* cut whole line when not loosing too much */
                    if(maxlen-idx < 200) {
                        urltext = urltext.substring(0,idx+1);
                    }
                    urltext += "...<snip>...\n";
                }

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
                                        + "{{{\n" + urltext + "\n}}}\n",
                                "UTF-8")));

                JPanel p = new JPanel(new GridBagLayout());
                p.add(new JMultilineLabel(
                        tr("You have encountered an error in JOSM. Before you file a bug report " +
                           "make sure you have updated to the latest version of JOSM here:")), GBC.eol());
                p.add(new UrlLabel("http://josm.openstreetmap.de/#Download"), GBC.eop().insets(8,0,0,0));
                p.add(new JMultilineLabel(
                        tr("You should also update your plugins. If neither of those help please " +
                           "file a bug report in our bugtracker using this link:")), GBC.eol());
                p.add(new UrlLabel(url.toString(), "http://josm.openstreetmap.de/josmticket?..."), GBC.eop().insets(8,0,0,0));
                p.add(new JMultilineLabel(
                        tr("There the error information provided below should already be " +
                           "filled in for you. Please include information on how to reproduce " +
                           "the error and try to supply as much detail as possible.")), GBC.eop());
                p.add(new JMultilineLabel(
                        tr("Alternatively, if that does not work you can manually fill in the information " +
                           "below at this URL:")), GBC.eol());
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

                for (Component c: p.getComponents()) {
                    if (c instanceof JMultilineLabel) {
                        ((JMultilineLabel)c).setMaxWidth(400);
                    }
                }

                JOptionPane.showMessageDialog(Main.parent, p, tr("You have encountered a bug in JOSM"), JOptionPane.ERROR_MESSAGE);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
