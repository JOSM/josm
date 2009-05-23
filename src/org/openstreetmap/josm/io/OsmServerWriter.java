// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.gui.historycombobox.JHistoryComboBox;
import org.openstreetmap.josm.gui.historycombobox.StringUtils;

/**
 * Class that uploads all changes to the osm server.
 *
 * This is done like this: - All objects with id = 0 are uploaded as new, except
 * those in deleted, which are ignored - All objects in deleted list are
 * deleted. - All remaining objects with modified flag set are updated.
 */
public class OsmServerWriter {

    /**
     * This list contains all successfully processed objects. The caller of
     * upload* has to check this after the call and update its dataset.
     *
     * If a server connection error occurs, this may contain fewer entries
     * than where passed in the list to upload*.
     */
    public Collection<OsmPrimitive> processed;
   

    private OsmApi api = new OsmApi();
    
    private static final int MSECS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MSECS_PER_MINUTE = MSECS_PER_SECOND * SECONDS_PER_MINUTE;

    long uploadStartTime;

    public String timeLeft(int progress, int list_size) {
        long now = System.currentTimeMillis();
        long elapsed = now - uploadStartTime;
        if (elapsed == 0)
            elapsed = 1;
        float uploads_per_ms = (float)progress / elapsed;
        float uploads_left = list_size - progress;
        int ms_left = (int)(uploads_left / uploads_per_ms);
        int minutes_left = ms_left / MSECS_PER_MINUTE;
        int seconds_left = (ms_left / MSECS_PER_SECOND) % SECONDS_PER_MINUTE ;
        String time_left_str = Integer.toString(minutes_left) + ":";
        if (seconds_left < 10)
            time_left_str += "0";
        time_left_str += Integer.toString(seconds_left);
        return time_left_str;
    }

    /**
     * Send the dataset to the server. 
     * @param the_version version of the data set
     * @param list list of objects to send
     */
    public void uploadOsm(String the_version, Collection<OsmPrimitive> list) {
        processed = new LinkedList<OsmPrimitive>();
        
        // initialize API. Abort upload in case of configuration or network
        // errors
        //
        try {
            api.initialize();
        } catch(Exception e) {
            JOptionPane.showMessageDialog(
                null,
                tr(   "Failed to initialize communication with the OSM server {0}.\n"
                    + "Check the server URL in your preferences and your internet connection.",
                    Main.pref.get("osm-server.url")
                ),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
            return;
        }
        

        Main.pleaseWaitDlg.progress.setMaximum(list.size());
        Main.pleaseWaitDlg.progress.setValue(0);

        boolean useChangesets = api.hasChangesetSupport();
        
        // controls whether or not we try and upload the whole bunch in one go
        boolean useDiffUploads = Main.pref.getBoolean("osm-server.atomic-upload",
            "0.6".equals(api.getVersion()));

        // create changeset if required
        try {
            if (useChangesets) {
                // add the last entered comment to the changeset
                String cmt = "";
                List<String> history = StringUtils.stringToList(Main.pref.get(UploadAction.HISTORY_KEY), JHistoryComboBox.DELIM);
                if(history.size() > 0) {
                    cmt = history.get(0);
                }
                api.createChangeset(cmt);
            }
        } catch (OsmTransferException ex) {
            dealWithTransferException(ex);
            return;
        }

        try {
            if (useDiffUploads) {
                // all in one go
                processed.addAll(api.uploadDiff(list));
            } else {
                // upload changes individually (90% of code is for the status display...)
                NameVisitor v = new NameVisitor();
                uploadStartTime = System.currentTimeMillis();
                for (OsmPrimitive osm : list) {
                    osm.visit(v);
                    int progress = Main.pleaseWaitDlg.progress.getValue();
                    String time_left_str = timeLeft(progress, list.size());
                    Main.pleaseWaitDlg.currentAction.setText(
                            tr("{0}% ({1}/{2}), {3} left. Uploading {4}: {5} (id: {6})",
                                    Math.round(100.0*progress/list.size()), progress,
                                    list.size(), time_left_str, tr(v.className), v.name, osm.id));
                    makeApiRequest(osm);
                    processed.add(osm);
                    Main.pleaseWaitDlg.progress.setValue(progress+1);
                }
            }
            if (useChangesets) api.stopChangeset();
        } catch (OsmTransferException e) {
            try {
                if (useChangesets) api.stopChangeset();
            } catch (Exception ee) {
                // ignore nested exception
            }
            dealWithTransferException(e);
        }
    }

    void makeApiRequest(OsmPrimitive osm) throws OsmTransferException {
        if (osm.deleted) {
            api.deletePrimitive(osm);
        } else if (osm.id == 0) {
            api.createPrimitive(osm);
        } else {
            api.modifyPrimitive(osm);
        }
    }

    private void dealWithTransferException (OsmTransferException e) {
        if (e instanceof OsmTransferCancelledException) {
            // ignore - don't bother the user with yet another message that he
            // has successfully cancelled the data upload
            //
            return; 
        }
        
        JOptionPane.showMessageDialog(Main.parent, 
            /* tr("Error during upload: ") + */ e.getMessage());
    }
}
