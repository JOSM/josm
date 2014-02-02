// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmChangeCompressedTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmChangeTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmCompressedTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmUrlTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadSessionTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Open an URL input dialog and load data from the given URL.
 *
 * @author imi
 */
public class OpenLocationAction extends JosmAction {

    protected final List<Class<? extends DownloadTask>> downloadTasks;

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenLocationAction() {
        /* I18N: Command to download a specific location/URL */
        super(tr("Open Location..."), "openlocation", tr("Open an URL."),
                Shortcut.registerShortcut("system:open_location", tr("File: {0}", tr("Open Location...")), KeyEvent.VK_L, Shortcut.CTRL), true);
        putValue("help", ht("/Action/OpenLocation"));
        this.downloadTasks = new ArrayList<Class<? extends DownloadTask>>();
        addDownloadTaskClass(DownloadOsmTask.class);
        addDownloadTaskClass(DownloadGpsTask.class);
        addDownloadTaskClass(DownloadOsmChangeTask.class);
        addDownloadTaskClass(DownloadOsmUrlTask.class);
        addDownloadTaskClass(DownloadOsmCompressedTask.class);
        addDownloadTaskClass(DownloadOsmChangeCompressedTask.class);
        addDownloadTaskClass(DownloadSessionTask.class);
    }

    /**
     * Restore the current history from the preferences
     *
     * @param cbHistory
     */
    protected void restoreUploadAddressHistory(HistoryComboBox cbHistory) {
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(getClass().getName() + ".uploadAddressHistory", new LinkedList<String>()));
        // we have to reverse the history, because ComboBoxHistory will reverse it again
        // in addElement()
        //
        Collections.reverse(cmtHistory);
        cbHistory.setPossibleItems(cmtHistory);
    }

    /**
     * Remind the current history in the preferences
     * @param cbHistory
     */
    protected void remindUploadAddressHistory(HistoryComboBox cbHistory) {
        cbHistory.addCurrentItemToHistory();
        Main.pref.putCollection(getClass().getName() + ".uploadAddressHistory", cbHistory.getHistory());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        JCheckBox layer = new JCheckBox(tr("Separate Layer"));
        layer.setToolTipText(tr("Select if the data should be downloaded into a new layer"));
        layer.setSelected(Main.pref.getBoolean("download.newlayer"));
        JPanel all = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        all.add(new JLabel(tr("Enter URL to download:")), gc);
        HistoryComboBox uploadAddresses = new HistoryComboBox();
        uploadAddresses.setToolTipText(tr("Enter an URL from where data should be downloaded"));
        restoreUploadAddressHistory(uploadAddresses);
        gc.gridy = 1;
        all.add(uploadAddresses, gc);
        gc.gridy = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;
        all.add(layer, gc);
        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("Download Location"),
                new String[] {tr("Download URL"), tr("Cancel")}
        );
        dialog.setContent(all, false /* don't embedded content in JScrollpane  */);
        dialog.setButtonIcons(new String[] {"download.png", "cancel.png"});
        dialog.setToolTipTexts(new String[] {
                tr("Start downloading data"),
                tr("Close dialog and cancel downloading")
        });
        dialog.configureContextsensitiveHelp("/Action/OpenLocation", true /* show help button */);
        dialog.showDialog();
        if (dialog.getValue() != 1) return;
        remindUploadAddressHistory(uploadAddresses);
        openUrl(layer.isSelected(), Utils.strip(uploadAddresses.getText()));
    }

    /**
     * Replies the list of download tasks accepting the given url.
     * @param url The URL to open
     * @return The list of download tasks accepting the given url.
     * @since 5691
     */
    public Collection<DownloadTask> findDownloadTasks(final String url) {
        List<DownloadTask> result = new ArrayList<DownloadTask>();
        for (Class<? extends DownloadTask> taskClass : downloadTasks) {
            if (taskClass != null) {
                try {
                    DownloadTask task = taskClass.getConstructor().newInstance();
                    if (task.acceptsUrl(url)) {
                        result.add(task);
                    }
                } catch (Exception e) {
                    Main.error(e);
                }
            }
        }
        return result;
    }

    /**
     * Summarizes acceptable urls for error message purposes.
     * @return The HTML message to be displayed
     * @since 6031
     */
    public String findSummaryDocumentation() {
        StringBuilder result = new StringBuilder("<table>");
        for (Class<? extends DownloadTask> taskClass : downloadTasks) {
            if (taskClass != null) {
                try {
                    DownloadTask task = taskClass.getConstructor().newInstance();
                    result.append(task.acceptsDocumentationSummary());
                } catch (Exception e) {
                    Main.error(e);
                }
            }
        }
        result.append("</table>");
        return result.toString();
    }

    /**
     * Open the given URL.
     * @param new_layer true if the URL needs to be opened in a new layer, false otherwise
     * @param url The URL to open
     */
    public void openUrl(boolean new_layer, final String url) {
        PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download Data"));
        Collection<DownloadTask> tasks = findDownloadTasks(url);
        DownloadTask task = null;
        Future<?> future = null;
        if (!tasks.isEmpty()) {
            // TODO: handle multiple suitable tasks ?
            try {
                task = tasks.iterator().next();
                future = task.loadUrl(new_layer, url, monitor);
            } catch (IllegalArgumentException e) {
                Main.error(e);
            }
        }
        if (future != null) {
            Main.worker.submit(new PostDownloadHandler(task, future));
        } else {
            final String details = findSummaryDocumentation();    // Explain what patterns are supported
            HelpAwareOptionPane.showMessageDialogInEDT(Main.parent, "<html><p>" + tr(
                    "Cannot open URL ''{0}''<br>The following download tasks accept the URL patterns shown:<br>{1}",
                    url, details) + "</p></html>", tr("Download Location"), JOptionPane.ERROR_MESSAGE, HelpUtil.ht("/Action/OpenLocation"));
        }
    }

    /**
     * Adds a new download task to the supported ones.
     * @param taskClass The new download task to add
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public boolean addDownloadTaskClass(Class<? extends DownloadTask> taskClass) {
        return this.downloadTasks.add(taskClass);
    }
}
