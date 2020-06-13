// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.actions.downloadtasks.DownloadReferrersTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.MultiFetchOverpassObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.SAXException;

/**
 * Task for downloading a set of primitives with all referrers.
 */
public class DownloadPrimitivesWithReferrersTask extends PleaseWaitRunnable {
    /** If true download into a new layer */
    private final boolean newLayer;
    /** List of primitives id to download */
    private final List<PrimitiveId> ids;
    /** If true, download members for relation */
    private final boolean full;
    /** If true, download also referrers */
    private final boolean downloadReferrers;

    /** Temporary layer where downloaded primitives are put */
    private final OsmDataLayer tmpLayer;
    /** Flag indicated that user ask for cancel this task */
    private boolean canceled;
    /** Reference to the task currently running */
    private PleaseWaitRunnable currentTask;

    /** set of missing ids, with overpass API these are also deleted objects */
    private Set<PrimitiveId> missingPrimitives;

    /**
     * Constructor
     *
     * @param newLayer if the data should be downloaded into a new layer
     * @param ids List of primitive id to download
     * @param downloadReferrers if the referrers of the object should be downloaded as well,
     *     i.e., parent relations, and for nodes, additionally, parent ways
     * @param full if the members of a relation should be downloaded as well
     * @param newLayerName the name to use for the new layer, can be null.
     * @param monitor ProgressMonitor to use, or null to create a new one
     */
    public DownloadPrimitivesWithReferrersTask(boolean newLayer, List<PrimitiveId> ids, boolean downloadReferrers,
            boolean full, String newLayerName, ProgressMonitor monitor) {
        super(tr("Download objects"), monitor, false);
        this.ids = ids;
        this.downloadReferrers = downloadReferrers;
        this.full = full;
        this.newLayer = newLayer;
        // Check we don't try to download new primitives
        for (PrimitiveId primitiveId : ids) {
            if (primitiveId.isNew()) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "Cannot download new primitives (ID {0})", primitiveId.getUniqueId()));
            }
        }
        // All downloaded primitives are put in a tmpLayer
        tmpLayer = new OsmDataLayer(new DataSet(), newLayerName != null ? newLayerName : OsmDataLayer.createNewName(), null);
    }

    /**
     * Cancel recursively the task. Do not call directly
     * @see DownloadPrimitivesWithReferrersTask#operationCanceled()
     */
    @Override
    protected void cancel() {
        synchronized (this) {
            canceled = true;
            if (currentTask != null)
                currentTask.operationCanceled();
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        if (Boolean.TRUE.equals(OverpassDownloadReader.FOR_MULTI_FETCH.get())) {
            useOverpassApi();
        } else {
            useOSMApi();
        }
    }

    private void useOverpassApi() {
        String request = MultiFetchOverpassObjectReader.genOverpassQuery(ids, true, downloadReferrers, full);
        currentTask = new DownloadFromOverpassTask(request, tmpLayer.data, getProgressMonitor().createSubTaskMonitor(1, false));
        currentTask.run();
        missingPrimitives = ids.stream()
                .filter(id -> tmpLayer.data.getPrimitiveById(id) == null)
                .collect(Collectors.toSet());
    }

    private void useOSMApi() {
        getProgressMonitor().setTicksCount(ids.size()+1);
        // First, download primitives
        DownloadPrimitivesTask mainTask = new DownloadPrimitivesTask(tmpLayer, ids, full,
                getProgressMonitor().createSubTaskMonitor(1, false));
        synchronized (this) {
            currentTask = mainTask;
            if (canceled) {
                currentTask = null;
                return;
            }
        }
        currentTask.run();

        missingPrimitives = mainTask.getMissingPrimitives();

        // Then, download referrers for each primitive
        if (downloadReferrers && tmpLayer.data != null) {
            // see #18895: don't try to download parents for invisible objects
            List<PrimitiveId> visible = ids.stream().map(tmpLayer.data::getPrimitiveById)
                    .filter(p -> p != null && p.isVisible()).collect(Collectors.toList());
            if (!visible.isEmpty()) {
                currentTask = new DownloadReferrersTask(tmpLayer, visible);
                currentTask.run();
                synchronized (this) {
                    if (currentTask.getProgressMonitor().isCanceled())
                        cancel();
                }
            }
        }
        currentTask = null;
    }

    @Override
    protected void finish() {
        synchronized (this) {
            if (canceled)
                return;
        }

        // Append downloaded data to JOSM
        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
        if (layer == null || this.newLayer || !layer.isDownloadable())
            MainApplication.getLayerManager().addLayer(tmpLayer);
        else
            layer.mergeFrom(tmpLayer);

        // Collect known deleted primitives
        final Set<PrimitiveId> del = new HashSet<>();
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        for (PrimitiveId id : ids) {
            OsmPrimitive osm = ds.getPrimitiveById(id);
            if (osm != null && osm.isDeleted()) {
                del.add(id);
            }
        }
        final Set<PrimitiveId> errs;
        if (missingPrimitives != null) {
            errs = missingPrimitives.stream().filter(id -> !del.contains(id)).collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            errs = Collections.emptySet();
        }

        // Warm about missing primitives
        if (!errs.isEmpty()) {
            final String assumedApiRC;
            if (Boolean.TRUE.equals(OverpassDownloadReader.FOR_MULTI_FETCH.get())) {
                assumedApiRC = trn("The server did not return data for the requested object, it was either deleted or does not exist.",
                        "The server did not return data for the requested objects, they were either deleted or do not exist.",
                        errs.size());

            } else {
                assumedApiRC = tr("The server replied with response code 404.<br>"
                        + "This usually means, the server does not know an object with the requested id.");
            }
            GuiHelper.runInEDTAndWait(() -> reportProblemDialog(errs,
                    trn("Object could not be downloaded", "Some objects could not be downloaded", errs.size()),
                    trn("One object could not be downloaded.<br>",
                            "{0} objects could not be downloaded.<br>",
                            errs.size(),
                            errs.size())
                            + assumedApiRC,
                    tr("missing objects:"),
                    JOptionPane.ERROR_MESSAGE
                    ).showDialog());
        }

        // Warm about deleted primitives
        if (!del.isEmpty())
            GuiHelper.runInEDTAndWait(() -> reportProblemDialog(del,
                    trn("Object deleted", "Objects deleted", del.size()),
                    trn(
                        "One downloaded object is deleted.",
                        "{0} downloaded objects are deleted.",
                        del.size(),
                        del.size()),
                    null,
                    JOptionPane.WARNING_MESSAGE
            ).showDialog());
    }

    /**
     * Return ids of really downloaded primitives.
     * @return List of primitives id or null if no primitives were downloaded
     */
    public List<PrimitiveId> getDownloadedId() {
        synchronized (this) {
            if (canceled)
                return null;
        }
        List<PrimitiveId> downloaded = new ArrayList<>(ids);
        downloaded.removeAll(missingPrimitives);
        return downloaded;
    }

    /**
     * Dialog for report a problem during download.
     * @param errs Primitives involved
     * @param title Title of dialog
     * @param text Detail message
     * @param listLabel List of primitives description
     * @param msgType Type of message, see {@link JOptionPane}
     * @return The Dialog object
     */
    public static ExtendedDialog reportProblemDialog(Set<PrimitiveId> errs,
            String title, String text, String listLabel, int msgType) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new HtmlPanel(text), GBC.eop());
        JosmTextArea txt = new JosmTextArea();
        if (listLabel != null) {
            JLabel missing = new JLabel(listLabel);
            missing.setFont(missing.getFont().deriveFont(Font.PLAIN));
            missing.setLabelFor(txt);
            p.add(missing, GBC.eol());
        }
        txt.setFont(GuiHelper.getMonospacedFont(txt));
        txt.setEditable(false);
        txt.setBackground(p.getBackground());
        txt.setColumns(40);
        txt.setRows(1);
        txt.setText(errs.stream().map(pid -> pid.getType().getAPIName().substring(0, 1) + pid.getUniqueId())
                .collect(Collectors.joining(", ")));
        JScrollPane scroll = new JScrollPane(txt);
        p.add(scroll, GBC.eop().weight(1.0, 0.0).fill(GBC.HORIZONTAL));

        return new ExtendedDialog(
                MainApplication.getMainFrame(),
                title,
                tr("Ok"))
        .setButtonIcons("ok")
        .setIcon(msgType)
        .setContent(p, false);
    }
}
