// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dialog;
import java.io.IOException;
import java.util.Collection;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The asynchronous task for downloading relation members.
 *
 */
public class DownloadRelationMemberTask extends PleaseWaitRunnable {
    private boolean cancelled;
    private Exception lastException;
    private Relation parent;
    private Collection<OsmPrimitive> children;
    private OsmDataLayer curLayer;
    private MultiFetchServerObjectReader objectReader;

    public DownloadRelationMemberTask(Relation parent, Collection<OsmPrimitive> children, OsmDataLayer curLayer, MemberTableModel memberTableModel, Dialog dialog) {
        super(tr("Download relation members"), new PleaseWaitProgressMonitor(dialog), false /* don't ignore exception */);
        this.parent = parent;
        this.children = children;
        this.curLayer = curLayer;
    }

    public DownloadRelationMemberTask(Relation parent, Collection<OsmPrimitive> children, OsmDataLayer curLayer, MemberTableModel memberTableModel) {
        super(tr("Download relation members"), false /* don't ignore exception */);
        this.parent = parent;
        this.children = children;
        this.curLayer = curLayer;
    }

    @Override
    protected void cancel() {
        cancelled = true;
        synchronized(this) {
            if (objectReader != null) {
                objectReader.cancel();
            }
        }
    }

    @Override
    protected void finish() {
        Main.map.repaint();
        if (cancelled)
            return;
        if (lastException != null) {
            ExceptionDialogUtil.explainException(lastException);
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            synchronized (this) {
                if (cancelled) return;
                objectReader = new MultiFetchServerObjectReader();
            }
            objectReader.append(children);
            progressMonitor.indeterminateSubTask(
                    trn("Downloading {0} incomplete child of relation ''{1}''",
                            "Downloading {0} incomplete children of relation ''{1}''",
                            children.size(),
                            children.size(),
                            parent.getDisplayName(DefaultNameFormatter.getInstance())
                    )
            );
            final DataSet dataSet = objectReader.parseOsm(progressMonitor
                    .createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            if (dataSet == null)
                return;
            synchronized (this) {
                if (cancelled) return;
                objectReader = null;
            }

            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            curLayer.mergeFrom(dataSet);
                            curLayer.onPostDownloadFromServer();
                        }
                    }
            );

        } catch (Exception e) {
            if (cancelled) {
                System.out.println(tr("Warning: ignoring exception because task is cancelled. Exception: {0}", e
                        .toString()));
                return;
            }
            lastException = e;
        }
    }
}
