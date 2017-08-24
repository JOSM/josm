// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesUrlBoundsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesUrlIdTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmChangeCompressedTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmChangeTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmCompressedTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmIdTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmUrlTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadSessionTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Open an URL input dialog and load data from the given URL.
 *
 * @author imi
 */
public class OpenLocationAction extends JosmAction {
    /**
     * true if the URL needs to be opened in a new layer, false otherwise
     */
    private static final BooleanProperty USE_NEW_LAYER = new BooleanProperty("download.newlayer", false);
    /**
     * the list of download tasks
     */
    protected final transient List<Class<? extends DownloadTask>> downloadTasks;

    static class WhichTasksToPerformDialog extends ExtendedDialog {
        WhichTasksToPerformDialog(JList<DownloadTask> list) {
            super(Main.parent, tr("Which tasks to perform?"), new String[]{tr("Ok"), tr("Cancel")}, true);
            setButtonIcons("ok", "cancel");
            final JPanel pane = new JPanel(new GridLayout(2, 1));
            pane.add(new JLabel(tr("Which tasks to perform?")));
            pane.add(list);
            setContent(pane);
        }
    }

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenLocationAction() {
        /* I18N: Command to download a specific location/URL */
        super(tr("Open Location..."), "openlocation", tr("Open an URL."),
                Shortcut.registerShortcut("system:open_location", tr("File: {0}", tr("Open Location...")),
                        KeyEvent.VK_L, Shortcut.CTRL), true);
        putValue("help", ht("/Action/OpenLocation"));
        this.downloadTasks = new ArrayList<>();
        addDownloadTaskClass(DownloadOsmTask.class);
        addDownloadTaskClass(DownloadGpsTask.class);
        addDownloadTaskClass(DownloadNotesTask.class);
        addDownloadTaskClass(DownloadOsmChangeTask.class);
        addDownloadTaskClass(DownloadOsmUrlTask.class);
        addDownloadTaskClass(DownloadOsmIdTask.class);
        addDownloadTaskClass(DownloadOsmCompressedTask.class);
        addDownloadTaskClass(DownloadOsmChangeCompressedTask.class);
        addDownloadTaskClass(DownloadSessionTask.class);
        addDownloadTaskClass(DownloadNotesUrlBoundsTask.class);
        addDownloadTaskClass(DownloadNotesUrlIdTask.class);
    }

    /**
     * Restore the current history from the preferences
     *
     * @param cbHistory the history combo box
     */
    protected void restoreUploadAddressHistory(HistoryComboBox cbHistory) {
        List<String> cmtHistory = new LinkedList<>(Main.pref.getCollection(getClass().getName() + ".uploadAddressHistory",
                new LinkedList<String>()));
        // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        //
        Collections.reverse(cmtHistory);
        cbHistory.setPossibleItems(cmtHistory);
    }

    /**
     * Remind the current history in the preferences
     * @param cbHistory the history combo box
     */
    protected void remindUploadAddressHistory(HistoryComboBox cbHistory) {
        cbHistory.addCurrentItemToHistory();
        Main.pref.putCollection(getClass().getName() + ".uploadAddressHistory", cbHistory.getHistory());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JPanel all = new JPanel(new GridBagLayout());

        // download URL selection
        all.add(new JLabel(tr("Enter URL to download:")), GBC.eol());
        HistoryComboBox uploadAddresses = new HistoryComboBox();
        uploadAddresses.setToolTipText(tr("Enter an URL from where data should be downloaded"));
        restoreUploadAddressHistory(uploadAddresses);
        all.add(uploadAddresses, GBC.eop().fill(GBC.BOTH));

        // use separate layer
        JCheckBox layer = new JCheckBox(tr("Separate Layer"));
        layer.setToolTipText(tr("Select if the data should be downloaded into a new layer"));
        layer.setSelected(USE_NEW_LAYER.get());
        all.add(layer, GBC.eop().fill(GBC.BOTH));

        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("Download Location"),
                tr("Download URL"), tr("Cancel"))
            .setContent(all, false /* don't embedded content in JScrollpane  */)
            .setButtonIcons("download", "cancel")
            .setToolTipTexts(
                tr("Start downloading data"),
                tr("Close dialog and cancel downloading"))
            .configureContextsensitiveHelp("/Action/OpenLocation", true /* show help button */);
        if (dialog.showDialog().getValue() == 1) {
            USE_NEW_LAYER.put(layer.isSelected());
            remindUploadAddressHistory(uploadAddresses);
            openUrl(Utils.strip(uploadAddresses.getText()));
        }
    }

    /**
     * Replies the list of download tasks accepting the given url.
     * @param url The URL to open
     * @param isRemotecontrol True if download request comes from remotecontrol.
     * @return The list of download tasks accepting the given url.
     * @since 5691
     */
    public Collection<DownloadTask> findDownloadTasks(final String url, boolean isRemotecontrol) {
        return downloadTasks.stream()
                .filter(Objects::nonNull)
                .map(taskClass -> {
                    try {
                        return taskClass.getConstructor().newInstance();
                    } catch (ReflectiveOperationException e) {
                        Logging.error(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(task -> task.acceptsUrl(url, isRemotecontrol))
                .collect(Collectors.toList());
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
                } catch (ReflectiveOperationException e) {
                    Logging.error(e);
                }
            }
        }
        result.append("</table>");
        return result.toString();
    }

    /**
     * Open the given URL.
     * @param newLayer true if the URL needs to be opened in a new layer, false otherwise
     * @param url The URL to open
     * @return the list of tasks that have been started successfully (can be empty).
     * @since 11986 (return type)
     */
    public List<Future<?>> openUrl(boolean newLayer, String url) {
        return realOpenUrl(newLayer, url);
    }

    /**
     * Open the given URL. This class checks the {@link #USE_NEW_LAYER} preference to check if a new layer should be used.
     * @param url The URL to open
     * @return the list of tasks that have been started successfully (can be empty).
     * @since 11986 (return type)
     */
    public List<Future<?>> openUrl(String url) {
        return realOpenUrl(USE_NEW_LAYER.get(), url);
    }

    private List<Future<?>> realOpenUrl(boolean newLayer, String url) {
        Collection<DownloadTask> tasks = findDownloadTasks(url, false);

        if (tasks.size() > 1) {
            tasks = askWhichTasksToLoad(tasks);
        } else if (tasks.isEmpty()) {
            warnNoSuitableTasks(url);
            return Collections.emptyList();
        }

        PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download Data"));

        List<Future<?>> result = new ArrayList<>();
        for (final DownloadTask task : tasks) {
            try {
                result.add(MainApplication.worker.submit(new PostDownloadHandler(task, task.loadUrl(newLayer, url, monitor))));
            } catch (IllegalArgumentException e) {
                Logging.error(e);
            }
        }
        return result;
    }

    /**
     * Asks the user which of the possible tasks to perform.
     * @param tasks a list of possible tasks
     * @return the selected tasks from the user or an empty list if the dialog has been canceled
     */
    Collection<DownloadTask> askWhichTasksToLoad(final Collection<DownloadTask> tasks) {
        final JList<DownloadTask> list = new JList<>(tasks.toArray(new DownloadTask[tasks.size()]));
        list.addSelectionInterval(0, tasks.size() - 1);
        final ExtendedDialog dialog = new WhichTasksToPerformDialog(list);
        dialog.showDialog();
        return dialog.getValue() == 1 ? list.getSelectedValuesList() : Collections.<DownloadTask>emptyList();
    }

    /**
     * Displays an error message dialog that no suitable tasks have been found for the given url.
     * @param url the given url
     */
    protected void warnNoSuitableTasks(final String url) {
        final String details = findSummaryDocumentation();    // Explain what patterns are supported
        HelpAwareOptionPane.showMessageDialogInEDT(Main.parent, "<html><p>" + tr(
                "Cannot open URL ''{0}''<br>The following download tasks accept the URL patterns shown:<br>{1}",
                url, details) + "</p></html>", tr("Download Location"), JOptionPane.ERROR_MESSAGE, ht("/Action/OpenLocation"));
    }

    /**
     * Adds a new download task to the supported ones.
     * @param taskClass The new download task to add
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    public final boolean addDownloadTaskClass(Class<? extends DownloadTask> taskClass) {
        return this.downloadTasks.add(taskClass);
    }
}
