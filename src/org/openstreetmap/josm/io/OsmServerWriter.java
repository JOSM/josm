// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 * Class that uploads all changes to the osm server.
 *
 * This is done like this: - All objects with id = 0 are uploaded as new, except
 * those in deleted, which are ignored - All objects in deleted list are
 * deleted. - All remaining objects with modified flag set are updated.
 */
public class OsmServerWriter {
    static private final Logger logger = Logger.getLogger(OsmServerWriter.class.getName());

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
        if (elapsed == 0) {
            elapsed = 1;
        }
        float uploads_per_ms = (float)progress / elapsed;
        float uploads_left = list_size - progress;
        int ms_left = (int)(uploads_left / uploads_per_ms);
        int minutes_left = ms_left / MSECS_PER_MINUTE;
        int seconds_left = (ms_left / MSECS_PER_SECOND) % SECONDS_PER_MINUTE ;
        String time_left_str = Integer.toString(minutes_left) + ":";
        if (seconds_left < 10) {
            time_left_str += "0";
        }
        time_left_str += Integer.toString(seconds_left);
        return time_left_str;
    }

    /**
     * retrieves the most recent changeset comment from the preferences
     * 
     * @return the most recent changeset comment
     */
    protected String getChangesetComment() {
        String cmt = "";
        List<String> history = new LinkedList<String>(
                Main.pref.getCollection(UploadAction.HISTORY_KEY, new LinkedList<String>()));
        if(history.size() > 0) {
            cmt = history.get(0);
        }
        return cmt;
    }

    /**
     * Send the dataset to the server.
     * 
     * @param apiVersion version of the data set
     * @param primitives list of objects to send
     */
    public void uploadOsm(String apiVersion, Collection<OsmPrimitive> primitives) throws OsmTransferException {
        processed = new LinkedList<OsmPrimitive>();

        // initialize API. Abort upload in case of configuration or network
        // errors
        //
        try {
            api.initialize();
        } catch(Exception e) {
            throw new OsmApiInitializationException(e);
        }

        Main.pleaseWaitDlg.progress.setMaximum(primitives.size());
        Main.pleaseWaitDlg.progress.setValue(0);

        // check whether we can use changeset
        //
        boolean canUseChangeset = api.hasChangesetSupport();
        boolean useChangeset = Main.pref.getBoolean("osm-server.atomic-upload", apiVersion.compareTo("0.6")>=0);
        if (useChangeset && ! canUseChangeset) {
            System.out.println(tr("WARNING: preference '{0}' or api version {1} of dataset requires to use changesets, but API is not handle them. Ignoring changesets.", "osm-server.atomic-upload", apiVersion));
            useChangeset = false;
        }

        if (useChangeset) {
            // upload everything in one changeset
            //
            try {
                api.createChangeset(getChangesetComment());
                processed.addAll(api.uploadDiff(primitives));
            } catch(OsmTransferException e) {
                throw e;
            } finally {
                try {
                    if (canUseChangeset) {
                        api.stopChangeset();
                    }
                } catch (Exception ee) {
                    // ignore nested exception
                }
            }
        } else {
            // upload changes individually (90% of code is for the status display...)
            //
            NameVisitor v = new NameVisitor();
            uploadStartTime = System.currentTimeMillis();
            for (OsmPrimitive osm : primitives) {
                osm.visit(v);
                int progress = Main.pleaseWaitDlg.progress.getValue();
                String time_left_str = timeLeft(progress, primitives.size());
                Main.pleaseWaitDlg.currentAction.setText(
                        tr("{0}% ({1}/{2}), {3} left. Uploading {4}: {5} (id: {6})",
                                Math.round(100.0*progress/primitives.size()), progress,
                                primitives.size(), time_left_str, tr(v.className), v.name, osm.id));
                makeApiRequest(osm);
                processed.add(osm);
                Main.pleaseWaitDlg.progress.setValue(progress+1);
            }
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

    public void disconnectActiveConnection() {
        if (api != null && api.activeConnection != null) {
            api.activeConnection.disconnect();
        }
    }
}
