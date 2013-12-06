// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.dialogs.OsmIdSelectionDialog;

/**
 * Dialog prompt to user to let him choose OSM primitives to download by specifying their type and IDs
 * @since 5765
 */
public class DownloadObjectDialog extends OsmIdSelectionDialog {

    protected final JCheckBox referrers = new JCheckBox(tr("Download referrers (parent relations)"));
    protected final JCheckBox fullRel   = new JCheckBox(tr("Download relation members"));
    protected final JCheckBox newLayer  = new JCheckBox(tr("Separate Layer"));

    /**
     * Constructs a new DownloadObjectDialog with Main.parent as parent component.
     */
    public DownloadObjectDialog() {
        this(Main.parent);
    }

    /**
     * Constructs a new DownloadObjectDialog.
     * @param parent The parent component
     */
    public DownloadObjectDialog(Component parent) {
        super(parent, tr("Download object"), new String[]{tr("Download object"), tr("Cancel")});
        init();
        setButtonIcons(new String[]{"download.png", "cancel.png"});
        setToolTipTexts(new String[]{
                tr("Start downloading"),
                tr("Close dialog and cancel downloading")
        });
        configureContextsensitiveHelp("/Action/DownloadObject", true /* show help button */);
    }

    protected Collection<Component> getComponentsBeforeHelp() {
        newLayer.setToolTipText(tr("Select if the data should be downloaded into a new layer"));
        newLayer.setSelected(Main.pref.getBoolean("download.newlayer"));

        referrers.setToolTipText(tr("Select if the referrers of the object should be downloaded as well, i.e.,"
                + "parent relations and for nodes, additionally, parent ways"));
        referrers.setSelected(Main.pref.getBoolean("downloadprimitive.referrers", true));

        fullRel.setToolTipText(tr("Select if the members of a relation should be downloaded as well"));
        fullRel.setSelected(Main.pref.getBoolean("downloadprimitive.full", true));

        cbType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                referrers.setText(cbType.getType() == OsmPrimitiveType.NODE
                        ? tr("Download referrers (parent relations and ways)")
                        : tr("Download referrers (parent relations)"));
            }
        });

        return Arrays.<Component>asList(referrers, fullRel, newLayer);
    }

    /**
     * Determines if a new layer has been requested.
     * @return true if a new layer has been requested, false otherwise
     */
    public final boolean isNewLayerRequested() {
        return newLayer.isSelected();
    }

    /**
     * Determines if relation members have been requested.
     * @return true if relation members have been requested, false otherwise
     */
    public final boolean isFullRelationRequested() {
        return fullRel.isSelected();
    }

    /**
     * Determines if referrers have been requested.
     * @return true if referrers have been requested, false otherwise
     */
    public final boolean isReferrersRequested() {
        return referrers.isSelected();
    }

    @Override
    public void windowClosed(WindowEvent e) {
        super.windowClosed(e);
        if (e != null && e.getComponent() == this && getValue() == 1) {
            Main.pref.put("downloadprimitive.referrers", referrers.isSelected());
            Main.pref.put("downloadprimitive.full", fullRel.isSelected());
            Main.pref.put("download.newlayer", newLayer.isSelected());
        }
    }
}
