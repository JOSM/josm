// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dialog;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * The asynchronous task for downloading relation members.
 *
 */
public class DownloadRelationMemberTask extends PleaseWaitRunnable {
    private boolean canceled;
    private Exception lastException;
    private final Set<Relation> parents = new HashSet<>();
    private final Collection<OsmPrimitive> children;
    private final OsmDataLayer curLayer;
    private MultiFetchServerObjectReader objectReader;

    public DownloadRelationMemberTask(Relation parent, Collection<OsmPrimitive> children, OsmDataLayer curLayer, Dialog dialog) {
        super(tr("Download relation members"), new PleaseWaitProgressMonitor(dialog), false /* don't ignore exception */);
        if (parent != null)
            this.parents.add(parent);
        this.children = children;
        this.curLayer = curLayer;
    }

    public DownloadRelationMemberTask(Relation parent, Collection<OsmPrimitive> children, OsmDataLayer curLayer) {
        super(tr("Download relation members"), false /* don't ignore exception */);
        if (parent != null)
            this.parents.add(parent);
        this.children = children;
        this.curLayer = curLayer;
    }

    /**
     * Creates a download task for downloading the child primitives {@code children} for all parent
     * relations in {@code parents}.
     *
     * @param parents the collection of parent relations
     * @param children the collection of child primitives to download
     * @param curLayer the current OSM layer
     */
    public DownloadRelationMemberTask(Collection<Relation> parents, Collection<OsmPrimitive> children, OsmDataLayer curLayer) {
        super(tr("Download relation members"), false /* don't ignore exception */);
        this.parents.addAll(parents);
        this.children = children;
        this.curLayer = curLayer;
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
            if (objectReader != null) {
                objectReader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        MainApplication.getMap().repaint();
        if (canceled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
        }
    }

    protected String buildDownloadFeedbackMessage() {
        if (parents.isEmpty()) {
            return trn("Downloading {0} incomplete object",
                    "Downloading {0} incomplete objects",
                    children.size(),
                    children.size());
        } else if (parents.size() == 1) {
            Relation parent = parents.iterator().next();
            return trn("Downloading {0} incomplete child of relation ''{1}''",
                    "Downloading {0} incomplete children of relation ''{1}''",
                    children.size(),
                    children.size(),
                    parent.getDisplayName(DefaultNameFormatter.getInstance()));
        } else {
            return trn("Downloading {0} incomplete child of {1} parent relations",
                    "Downloading {0} incomplete children of {1} parent relations",
                    children.size(),
                    children.size(),
                    parents.size());
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            synchronized (this) {
                if (canceled) return;
                objectReader = MultiFetchServerObjectReader.create();
            }
            objectReader.append(children);
            progressMonitor.indeterminateSubTask(
                    buildDownloadFeedbackMessage()
            );
            final DataSet dataSet = objectReader.parseOsm(progressMonitor
                    .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            if (dataSet == null)
                return;
            dataSet.deleteInvisible();
            synchronized (this) {
                if (canceled) return;
                objectReader = null;
            }

            SwingUtilities.invokeLater(() -> {
                curLayer.mergeFrom(dataSet);
                curLayer.onPostDownloadFromServer();
            });
        } catch (OsmTransferException e) {
            if (canceled) {
                Logging.warn(tr("Ignoring exception because task was canceled. Exception: {0}", e.toString()));
                return;
            }
            lastException = e;
        }
    }
}
