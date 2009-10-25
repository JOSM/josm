// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open an URL input dialog and load data from the given URL.
 *
 * @author imi
 */
public class OpenLocationAction extends JosmAction {

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenLocationAction() {
        super(tr("Open Location..."), "openlocation", tr("Open an URL."),
                Shortcut.registerShortcut("system:open_location", tr("File: {0}", tr("Open Location...")), KeyEvent.VK_L, Shortcut.GROUP_MENU), true);
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

    public void actionPerformed(ActionEvent e) {

        JCheckBox layer = new JCheckBox(tr("Separate Layer"));
        layer.setToolTipText(tr("Select if the data should be downloaded in a new layer"));
        layer.setSelected(Main.pref.getBoolean("download.newlayer"));
        JPanel all = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        all.add(new JLabel(tr("Enter URL to download:")), gc);
        HistoryComboBox uploadAdresses = new HistoryComboBox();
        uploadAdresses.setToolTipText(tr("Enter an URL from where data should be downloaded"));
        restoreUploadAddressHistory(uploadAdresses);
        gc.gridy = 1;
        all.add(uploadAdresses, gc);
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
        remindUploadAddressHistory(uploadAdresses);
        openUrl(layer.isSelected(), uploadAdresses.getText());
    }

    /**
     * Open the given file.
     */
    public void openUrl(boolean new_layer, String url) {
        DownloadOsmTask task = new DownloadOsmTask();
        PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download Data"));
        Future<?> future = task.loadUrl(new_layer, url, monitor);
        Main.worker.submit(new PostDownloadHandler(task, future));
    }
}
