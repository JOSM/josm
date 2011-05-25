// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadReferrersTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.OsmIdTextField;
import org.openstreetmap.josm.gui.widgets.OsmPrimitiveTypesComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Download an OsmPrimitive by specifying type and ID
 *
 * @author Matthias Julius
 */
public class DownloadPrimitiveAction extends JosmAction {

    public DownloadPrimitiveAction() {
        super(tr("Download object..."), "downloadprimitive", tr("Download OSM object by ID."),
                Shortcut.registerShortcut("system:download_primitive", tr("File: {0}", tr("Download object...")), KeyEvent.VK_O, Shortcut.GROUP_MENU + Shortcut.GROUPS_ALT1), true);
        putValue("help", ht("/Action/DownloadObject"));
    }

    public void actionPerformed(ActionEvent e) {

        JPanel all = new JPanel();
        GroupLayout layout = new GroupLayout(all);
        all.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JLabel lbl1 = new JLabel(tr("Object type:"));
        OsmPrimitiveTypesComboBox cbType = new OsmPrimitiveTypesComboBox();
        cbType.addItem(new SimpleListItem("mixed", trc("osm object types", "mixed")));
        cbType.setToolTipText(tr("Choose the OSM object type"));
        JLabel lbl2 = new JLabel(tr("Object ID:"));
        OsmIdTextField tfId = new OsmIdTextField();
        tfId.setToolTipText(tr("Enter the ID of the object that should be downloaded"));
        // forward the enter key stroke to the download button
        tfId.getKeymap().removeKeyStrokeBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));
        JCheckBox layer = new JCheckBox(tr("Separate Layer"));
        layer.setToolTipText(tr("Select if the data should be downloaded into a new layer"));
        layer.setSelected(Main.pref.getBoolean("download.newlayer"));
        JCheckBox referrers = new JCheckBox(tr("Download referrers"));
        referrers.setToolTipText(tr("Select if the referrers of the object should be downloaded as well"));
        referrers.setSelected(Main.pref.getBoolean("downloadprimitive.referrers"));
        HtmlPanel help = new HtmlPanel(tr("Object IDs can be separated by comma or space.<br/>"
                + " Examples: <b><ul><li>1 2 5</li><li>1,2,5</li></ul><br/></b>"
                + " In mixed mode, specify objects like this: <b>w123, n110, w12, r15</b><br/>"));
        help.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                .addComponent(lbl1)
                .addComponent(cbType, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createParallelGroup()
                .addComponent(lbl2)
                .addComponent(tfId, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
            .addComponent(referrers)
            .addComponent(layer)
            .addComponent(help)
        );

        layout.setHorizontalGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                    .addComponent(lbl1)
                    .addComponent(lbl2)
                )
                .addGroup(layout.createParallelGroup()
                    .addComponent(cbType)
                    .addComponent(tfId))
                )
            .addComponent(referrers)
            .addComponent(layer)
            .addComponent(help)
        );

        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("Download object"),
                new String[] {tr("Download object"), tr("Cancel")}
        );
        dialog.setContent(all, false);
        dialog.setButtonIcons(new String[] {"download.png", "cancel.png"});
        dialog.setToolTipTexts(new String[] {
                tr("Start downloading"),
                tr("Close dialog and cancel downloading")
        });
        dialog.setDefaultButton(1);
        dialog.configureContextsensitiveHelp("/Action/DownloadObject", true /* show help button */);
        cbType.setSelectedIndex(Main.pref.getInteger("downloadprimitive.lasttype", 0));
        dialog.showDialog();
        if (dialog.getValue() != 1) return;
        Main.pref.putInteger("downloadprimitive.lasttype", cbType.getSelectedIndex());
        Main.pref.put("downloadprimitive.referrers", referrers.isSelected());
        Main.pref.put("download.newlayer", layer.isSelected());

        tfId.setType(cbType.getType());
        if(tfId.readOsmIds()==false) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Invalid ID list specified\n"
                    + " Cannot download object."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        processItems(layer.isSelected(), cbType.getType(), tfId.getIds(), referrers.isSelected());
    }

    void processItems(boolean newLayer, OsmPrimitiveType type,
            final List<PrimitiveId> ids,
            boolean downloadReferrers) {
        OsmDataLayer layer = getEditLayer();
        if ((layer == null) || newLayer) {
            layer = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            Main.main.addLayer(layer);
        }
        final DownloadPrimitivesTask task = new DownloadPrimitivesTask(layer, ids);
        Main.worker.submit(task);

        if (downloadReferrers) {
            for (PrimitiveId id : ids) {
                Main.worker.submit(new DownloadReferrersTask(layer, id));
            }
        }

        Runnable showErrorsAndWarnings = new Runnable() {
            @Override
            public void run() {
                Set<PrimitiveId> errs = task.getMissingPrimitives();
                if (errs != null && !errs.isEmpty()) {
                    final ExtendedDialog dlg = reportProblemDialog(errs,
                            trn("Object could not be downloaded", "Some objects could not be downloaded", errs.size()),
                            trn("One object could not be downloaded.<br>",
                                "{0} objects could not be downloaded.<br>",
                                errs.size(),
                                errs.size())
                            + tr("The server replied with response code 404.<br>"
                                + "This usually means, the server does not know an object with the requested id."),
                            tr("missing objects:"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                dlg.showDialog();
                            }
                        });
                    } catch (InterruptedException ex) {
                    } catch (InvocationTargetException ex) {
                    }
                }

                Set<PrimitiveId> del = new TreeSet<PrimitiveId>();
                DataSet ds = getCurrentDataSet();
                for (PrimitiveId id : ids) {
                    OsmPrimitive osm = ds.getPrimitiveById(id);
                    if (osm != null && osm.isDeleted()) {
                        del.add(id);
                    }
                }
                if (del != null && !del.isEmpty()) {
                    final ExtendedDialog dlg = reportProblemDialog(del,
                            trn("Object deleted", "Objects deleted", del.size()),
                            trn(
                                "One downloaded object is deleted.",
                                "{0} downloaded objects are deleted.",
                                del.size(),
                                del.size()),
                            null,
                            JOptionPane.WARNING_MESSAGE
                    );
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            dlg.showDialog();
                        }
                    });
                }
            }
        };
        Main.worker.submit(showErrorsAndWarnings);
    }

    private ExtendedDialog reportProblemDialog(Set<PrimitiveId> errs,
            String TITLE, String TEXT, String LIST_LABEL, int msgType) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new HtmlPanel(TEXT), GBC.eop());
        if (LIST_LABEL != null) {
            JLabel missing = new JLabel(LIST_LABEL);
            missing.setFont(missing.getFont().deriveFont(Font.PLAIN));
            p.add(missing, GBC.eol());
        }
        JTextArea txt = new JTextArea();
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

    private static class SimpleListItem  {
        final String data;
        final String text;

        public SimpleListItem(String data, String text) {
            this.data = data;
            this.text = text;
        }

        @Override public String toString() {
            return text;
        }
    }
}
