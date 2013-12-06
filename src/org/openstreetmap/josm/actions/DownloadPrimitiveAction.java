// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadReferrersTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.download.DownloadObjectDialog;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Download an OsmPrimitive by specifying type and ID
 *
 * @author Matthias Julius
 */
public class DownloadPrimitiveAction extends JosmAction {

    /**
     * Constructs a new {@code DownloadPrimitiveAction}.
     */
    public DownloadPrimitiveAction() {
        super(tr("Download object..."), "downloadprimitive", tr("Download OSM object by ID."),
                Shortcut.registerShortcut("system:download_primitive", tr("File: {0}", tr("Download object...")), KeyEvent.VK_O, Shortcut.CTRL_SHIFT), true);
        putValue("help", ht("/Action/DownloadObject"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        DownloadObjectDialog dialog = new DownloadObjectDialog();
        if (dialog.showDialog().getValue() != dialog.getContinueButtonIndex()) return;

        processItems(dialog.isNewLayerRequested(), dialog.getOsmIds(), dialog.isReferrersRequested(), dialog.isFullRelationRequested());
    }

    /**
     * @param newLayer if the data should be downloaded into a new layer
     * @param ids
     * @param downloadReferrers if the referrers of the object should be downloaded as well, i.e., parent relations, and for nodes, additionally, parent ways
     * @param full if the members of a relation should be downloaded as well
     */
    public static void processItems(boolean newLayer, final List<PrimitiveId> ids, boolean downloadReferrers, boolean full) {
        OsmDataLayer layer = getEditLayer();
        if ((layer == null) || newLayer) {
            layer = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            Main.main.addLayer(layer);
        }
        final DownloadPrimitivesTask task = new DownloadPrimitivesTask(layer, ids, full);
        Main.worker.submit(task);

        if (downloadReferrers) {
            for (PrimitiveId id : ids) {
                Main.worker.submit(new DownloadReferrersTask(layer, id));
            }
        }

        Runnable showErrorsAndWarnings = new Runnable() {
            @Override
            public void run() {
                final Set<PrimitiveId> errs = task.getMissingPrimitives();
                if (errs != null && !errs.isEmpty()) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                reportProblemDialog(errs,
                                        trn("Object could not be downloaded", "Some objects could not be downloaded", errs.size()),
                                        trn("One object could not be downloaded.<br>",
                                            "{0} objects could not be downloaded.<br>",
                                            errs.size(),
                                            errs.size())
                                        + tr("The server replied with response code 404.<br>"
                                            + "This usually means, the server does not know an object with the requested id."),
                                        tr("missing objects:"),
                                        JOptionPane.ERROR_MESSAGE
                                ).showDialog();
                            }
                        });
                    } catch (InterruptedException ex) {
                        Main.warn("InterruptedException while displaying error dialog");
                    } catch (InvocationTargetException ex) {
                        Main.warn(ex);
                    }
                }

                final Set<PrimitiveId> del = new TreeSet<PrimitiveId>();
                DataSet ds = getCurrentDataSet();
                for (PrimitiveId id : ids) {
                    OsmPrimitive osm = ds.getPrimitiveById(id);
                    if (osm != null && osm.isDeleted()) {
                        del.add(id);
                    }
                }
                if (!del.isEmpty()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            reportProblemDialog(del,
                                    trn("Object deleted", "Objects deleted", del.size()),
                                    trn(
                                        "One downloaded object is deleted.",
                                        "{0} downloaded objects are deleted.",
                                        del.size(),
                                        del.size()),
                                    null,
                                    JOptionPane.WARNING_MESSAGE
                            ).showDialog();
                        }
                    });
                }
            }
        };
        Main.worker.submit(showErrorsAndWarnings);
    }

    private static ExtendedDialog reportProblemDialog(Set<PrimitiveId> errs,
            String TITLE, String TEXT, String LIST_LABEL, int msgType) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new HtmlPanel(TEXT), GBC.eop());
        if (LIST_LABEL != null) {
            JLabel missing = new JLabel(LIST_LABEL);
            missing.setFont(missing.getFont().deriveFont(Font.PLAIN));
            p.add(missing, GBC.eol());
        }
        JosmTextArea txt = new JosmTextArea();
        txt.setFont(new Font("Monospaced", txt.getFont().getStyle(), txt.getFont().getSize()));
        txt.setEditable(false);
        txt.setBackground(p.getBackground());
        txt.setColumns(40);
        txt.setRows(1);
        txt.setText(Utils.join(", ", errs));
        JScrollPane scroll = new JScrollPane(txt);
        p.add(scroll, GBC.eop().weight(1.0, 0.0).fill(GBC.HORIZONTAL));

        return new ExtendedDialog(
                Main.parent,
                TITLE,
                new String[] { tr("Ok") })
            .setButtonIcons(new String[] { "ok" })
            .setIcon(msgType)
            .setContent(p, false);
    }
}
