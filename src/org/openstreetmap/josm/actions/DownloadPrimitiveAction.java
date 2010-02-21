// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadPrimitiveTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadReferrersTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.OsmIdTextField;
import org.openstreetmap.josm.gui.widgets.OsmPrimitiveTypesComboBox;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Download an OsmPrimitive by specifying type and ID
 *
 * @author Matthias Julius
 */
public class DownloadPrimitiveAction extends JosmAction {

    public DownloadPrimitiveAction() {
        super(tr("Download object..."), "downloadprimitive", tr("Download OSM object by ID."),
                Shortcut.registerShortcut("system:download_primitive", tr("File: {0}", tr("Download Object...")), KeyEvent.VK_O, Shortcut.GROUP_MENU + Shortcut.GROUPS_ALT1), true);
        putValue("help", ht("/Action/DownloadObject"));
    }

    public void actionPerformed(ActionEvent e) {
        JCheckBox layer = new JCheckBox(tr("Separate Layer"));
        layer.setToolTipText(tr("Select if the data should be downloaded into a new layer"));
        layer.setSelected(Main.pref.getBoolean("download.newlayer"));
        JCheckBox referrers = new JCheckBox(tr("Download referrers"));
        referrers.setToolTipText(tr("Select if the referrers of the object should be downloaded as well"));
        referrers.setSelected(Main.pref.getBoolean("downloadprimitive.referrers"));
        JPanel all = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weightx = 0;
        all.add(new JLabel(tr("Object type:")), gc);
        OsmPrimitiveTypesComboBox cbType = new OsmPrimitiveTypesComboBox();
        cbType.setToolTipText("Choose the OSM object type");
        gc.weightx = 1;
        all.add(cbType, gc);
        gc.gridy = 1;
        gc.weightx = 0;
        all.add(new JLabel(tr("Object ID:")), gc);
        OsmIdTextField tfId = new OsmIdTextField();
        tfId.setToolTipText(tr("Enter the ID of the object that should be downloaded"));
        // forward the enter key stroke to the download button
        tfId.getKeymap().removeKeyStrokeBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));
        gc.weightx = 1;
        all.add(tfId, gc);
        gc.gridy = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;
        gc.weightx = 0;
        all.add(referrers, gc);
        gc.gridy = 3;
        all.add(layer, gc);
        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("Download Object"),
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
        dialog.showDialog();
        if (dialog.getValue() != 1) return;
        Main.pref.put("downloadprimitive.referrers", referrers.isSelected());
        Main.pref.put("download.newlayer", layer.isSelected());
        download(layer.isSelected(), cbType.getType(), tfId.getOsmId(), referrers.isSelected());
    }

    /**
     * Download the given primitive.
     */
    public void download(boolean newLayer, OsmPrimitiveType type, int id, boolean downloadReferrers) {
        OsmDataLayer layer = getEditLayer();
        if ((layer == null) || newLayer) {
            layer = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            Main.main.addLayer(layer);
        }
        Main.worker.submit(new DownloadPrimitiveTask(new SimplePrimitiveId(id, type), layer));
        if (downloadReferrers) {
            Main.worker.submit(new DownloadReferrersTask(layer, id, type));
        }
    }
}
