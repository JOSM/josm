// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.OsmIdTextField;
import org.openstreetmap.josm.gui.widgets.OsmPrimitiveTypesComboBox;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.gui.widgets.JosmTextField;

/**
 * Dialog prompt to user to let him choose OSM primitives to download by specifying their type and IDs
 * @since 5765
 */
public class DownloadObjectDialog extends ExtendedDialog implements WindowListener {

    protected final JPanel panel = new JPanel();
    protected final OsmPrimitiveTypesComboBox cbType = new OsmPrimitiveTypesComboBox();
    protected final OsmIdTextField tfId = new OsmIdTextField();
    protected final HistoryComboBox cbId = new HistoryComboBox();
    
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
        super(parent, tr("Download object"), new String[] {tr("Download object"), tr("Cancel")});
        init();
    }
    
    protected void init() {
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JLabel lbl1 = new JLabel(tr("Object type:"));
        
        cbType.addItem(trc("osm object types", "mixed"));
        cbType.setToolTipText(tr("Choose the OSM object type"));
        JLabel lbl2 = new JLabel(tr("Object ID:"));
        
        cbId.setEditor(new BasicComboBoxEditor() {
            @Override
            protected JosmTextField createEditorComponent() {
                return tfId;
            }
        });
        cbId.setToolTipText(tr("Enter the ID of the object that should be downloaded"));
        restorePrimitivesHistory(cbId);
        
        // forward the enter key stroke to the download button
        tfId.getKeymap().removeKeyStrokeBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));
        tfId.setPreferredSize(new Dimension(400, tfId.getPreferredSize().height));
        
        newLayer.setToolTipText(tr("Select if the data should be downloaded into a new layer"));
        newLayer.setSelected(Main.pref.getBoolean("download.newlayer"));
        
        referrers.setToolTipText(tr("Select if the referrers of the object should be downloaded as well, i.e.,"
                + "parent relations and for nodes, additionally, parent ways"));
        referrers.setSelected(Main.pref.getBoolean("downloadprimitive.referrers", true));
        
        fullRel.setToolTipText(tr("Select if the members of a relation should be downloaded as well"));
        fullRel.setSelected(Main.pref.getBoolean("downloadprimitive.full", true));
        
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
                .addComponent(cbId, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
            .addComponent(referrers)
            .addComponent(fullRel)
            .addComponent(newLayer)
            .addComponent(help)
        );

        cbType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                tfId.setType(cbType.getType());
                tfId.performValidation();
                referrers.setText(cbType.getType() == OsmPrimitiveType.NODE
                        ? tr("Download referrers (parent relations and ways)")
                        : tr("Download referrers (parent relations)"));
            }
        });

        layout.setHorizontalGroup(layout.createParallelGroup()
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                    .addComponent(lbl1)
                    .addComponent(lbl2)
                )
                .addGroup(layout.createParallelGroup()
                    .addComponent(cbType)
                    .addComponent(cbId))
                )
            .addComponent(referrers)
            .addComponent(fullRel)
            .addComponent(newLayer)
            .addComponent(help)
        );
    }

    @Override
    public void setupDialog() {
        
        setContent(panel, false);
        setButtonIcons(new String[] {"download.png", "cancel.png"});
        setToolTipTexts(new String[] {
                tr("Start downloading"),
                tr("Close dialog and cancel downloading")
        });
        setDefaultButton(1);
        configureContextsensitiveHelp("/Action/DownloadObject", true /* show help button */);
        cbType.setSelectedIndex(Main.pref.getInteger("downloadprimitive.lasttype", 0));
        tfId.setType(cbType.getType());
        if (Main.pref.getBoolean("downloadprimitive.autopaste", true)) { 
            tryToPasteFromClipboard(tfId, cbType);
        }
        
        addWindowListener(this);
        super.setupDialog();
    }
    
    /**
     * Restore the current history from the preferences
     *
     * @param cbHistory
     */
    protected void restorePrimitivesHistory(HistoryComboBox cbHistory) {
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(getClass().getName() + ".primitivesHistory", new LinkedList<String>()));
        // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        Collections.reverse(cmtHistory);
        cbHistory.setPossibleItems(cmtHistory);
    }
    
    /**
     * Remind the current history in the preferences
     * @param cbHistory
     */
    protected void remindPrimitivesHistory(HistoryComboBox cbHistory) {
        cbHistory.addCurrentItemToHistory();
        Main.pref.putCollection(getClass().getName() + ".primitivesHistory", cbHistory.getHistory());
    }

    protected void tryToPasteFromClipboard(OsmIdTextField tfId, OsmPrimitiveTypesComboBox cbType) {
        String buf = Utils.getClipboardContent();
        if (buf != null) {
            if (buf.contains("node")) cbType.setSelectedIndex(0);
            if (buf.contains("way")) cbType.setSelectedIndex(1);
            if (buf.contains("relation")) cbType.setSelectedIndex(2);
            String[] res = buf.split("/");
            String txt;
            if (res.length>0) {
                txt = res[res.length-1];
                if (txt.isEmpty() && txt.length()>1) txt=res[res.length-2];
            } else {
                txt=buf;
            }
            if (buf.length() <= Main.pref.getInteger("downloadprimitive.max-autopaste-length", 2000)) {
                tfId.tryToPasteFrom(txt);
            }
        }
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
    
    /**
     * Gets the requested OSM object IDs.
     * @return The list of requested OSM object IDs
     */
    public final List<PrimitiveId> getOsmIds() {
        return tfId.getIds();
    }

    @Override public void windowClosed(WindowEvent e) {
        if (e != null && e.getComponent() == this && getValue() == 1) {
            Main.pref.putInteger("downloadprimitive.lasttype", cbType.getSelectedIndex());
            Main.pref.put("downloadprimitive.referrers", referrers.isSelected());
            Main.pref.put("downloadprimitive.full", fullRel.isSelected());
            Main.pref.put("download.newlayer", newLayer.isSelected());
            
            if (!tfId.readIds()) {
                JOptionPane.showMessageDialog(getParent(),
                        tr("Invalid ID list specified\n"
                        + "Cannot download object."),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            remindPrimitivesHistory(cbId);
        }
    }

    @Override public void windowOpened(WindowEvent e) {}
    @Override public void windowClosing(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}
}
