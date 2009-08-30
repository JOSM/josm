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

                    JPanel p = new JPanel(new GridBagLayout());
                    p.add(new JLabel("<html>" + tr("Please report a ticket at {0}","http://josm.openstreetmap.de/newticket") +
                            "<br>" + tr("Include your steps to get to the error (as detailed as possible)!") +
                            "<br>" + tr("Try updating to the newest version of JOSM and all plugins before reporting a bug.") +
                            "<br>" + tr("Be sure to include the following information:") + "</html>"), GBC.eol());
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

                    JOptionPane.showMessageDialog(Main.parent, p, tr("Warning"), JOptionPane.WARNING_MESSAGE);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
