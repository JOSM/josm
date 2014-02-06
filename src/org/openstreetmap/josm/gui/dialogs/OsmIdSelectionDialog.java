// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.OsmIdTextField;
import org.openstreetmap.josm.gui.widgets.OsmPrimitiveTypesComboBox;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

/**
 * Dialog prompt to user to let him choose OSM primitives by specifying their type and IDs.
 * @since 6448, split from DownloadObjectDialog
 */
public class OsmIdSelectionDialog extends ExtendedDialog implements WindowListener {

    protected final JPanel panel = new JPanel();
    protected final OsmPrimitiveTypesComboBox cbType = new OsmPrimitiveTypesComboBox();
    protected final OsmIdTextField tfId = new OsmIdTextField();
    protected final HistoryComboBox cbId = new HistoryComboBox();
    protected final GroupLayout layout = new GroupLayout(panel);

    public OsmIdSelectionDialog(Component parent, String title, String[] buttonTexts) {
        super(parent, title, buttonTexts);
    }

    public OsmIdSelectionDialog(Component parent, String title, String[] buttonTexts, boolean modal) {
        super(parent, title, buttonTexts, modal);
    }

    public OsmIdSelectionDialog(Component parent, String title, String[] buttonTexts, boolean modal, boolean disposeOnClose) {
        super(parent, title, buttonTexts, modal, disposeOnClose);
    }

    protected void init() {
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

        HtmlPanel help = new HtmlPanel(/* I18n: {0} and {1} contains example strings not meant for translation. {2}=n, {3}=w, {4}=r. */
                tr("Object IDs can be separated by comma or space.<br/>"
                        + "Examples: {0}<br/>"
                        + "In mixed mode, specify objects like this: {1}<br/>"
                        + "({2} stands for <i>node</i>, {3} for <i>way</i>, and {4} for <i>relation</i>)",
                        "<b>" + Utils.joinAsHtmlUnorderedList(Arrays.asList("1 2 5", "1,2,5")) + "</b>",
                        "<b>w123, n110, w12, r15</b>",
                        "<b>n</b>", "<b>w</b>", "<b>r</b>"
                ));
        help.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        cbType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                tfId.setType(cbType.getType());
                tfId.performValidation();
            }
        });

        final GroupLayout.SequentialGroup sequentialGroup = layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                        .addComponent(lbl1)
                        .addComponent(cbType, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup()
                        .addComponent(lbl2)
                        .addComponent(cbId, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));

        final GroupLayout.ParallelGroup parallelGroup = layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup()
                                .addComponent(lbl1)
                                .addComponent(lbl2)
                        )
                        .addGroup(layout.createParallelGroup()
                                .addComponent(cbType)
                                .addComponent(cbId))
                );

        for (Component i : getComponentsBeforeHelp()) {
            sequentialGroup.addComponent(i);
            parallelGroup.addComponent(i);
        }

        layout.setVerticalGroup(sequentialGroup.addComponent(help));
        layout.setHorizontalGroup(parallelGroup.addComponent(help));
    }

    /**
     * Let subclasses add custom components between the id input field and the help text
     * @return the collections to add
     */
    protected Collection<Component> getComponentsBeforeHelp() {
        return Collections.emptySet();
    }

    /**
     * Allows subclasses to specify a different continue button index. If this button is pressed, the history is updated.
     * @return the button index
     */
    public int getContinueButtonIndex() {
        return 1;
    }

    /**
     * Restore the current history from the preferences
     *
     * @param cbHistory the {@link HistoryComboBox} to which the history is restored to
     */
    protected void restorePrimitivesHistory(HistoryComboBox cbHistory) {
        java.util.List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(getClass().getName() + ".primitivesHistory", new LinkedList<String>()));
        // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        Collections.reverse(cmtHistory);
        cbHistory.setPossibleItems(cmtHistory);
    }

    /**
     * Remind the current history in the preferences
     *
     * @param cbHistory the {@link HistoryComboBox} of which to restore the history
     */
    protected void remindPrimitivesHistory(HistoryComboBox cbHistory) {
        cbHistory.addCurrentItemToHistory();
        Main.pref.putCollection(getClass().getName() + ".primitivesHistory", cbHistory.getHistory());
    }

    /**
     * Gets the requested OSM object IDs.
     *
     * @return The list of requested OSM object IDs
     */
    public final List<PrimitiveId> getOsmIds() {
        return tfId.getIds();
    }

    @Override
    public void setupDialog() {
        setContent(panel, false);
        cbType.setSelectedIndex(Main.pref.getInteger("downloadprimitive.lasttype", 0));
        tfId.setType(cbType.getType());
        if (Main.pref.getBoolean("downloadprimitive.autopaste", true)) {
            tryToPasteFromClipboard(tfId, cbType);
        }
        setDefaultButton(getContinueButtonIndex());
        addWindowListener(this);
        super.setupDialog();
    }

    protected void tryToPasteFromClipboard(OsmIdTextField tfId, OsmPrimitiveTypesComboBox cbType) {
        String buf = Utils.getClipboardContent();
        if (buf == null || buf.length()==0) return;
        if (buf.length() > Main.pref.getInteger("downloadprimitive.max-autopaste-length", 2000)) return;
        final List<SimplePrimitiveId> ids = SimplePrimitiveId.fuzzyParse(buf);
        if (!ids.isEmpty()) {
            final String parsedText = Utils.join(", ", Utils.transform(ids, new Utils.Function<SimplePrimitiveId, String>() {
                @Override
                public String apply(SimplePrimitiveId x) {
                    return x.getType().getAPIName().charAt(0) + String.valueOf(x.getUniqueId());
                }
            }));
            tfId.tryToPasteFrom(parsedText);
            final Set<OsmPrimitiveType> types = new HashSet<OsmPrimitiveType>(Utils.transform(ids, new Utils.Function<SimplePrimitiveId, OsmPrimitiveType>() {
                @Override
                public OsmPrimitiveType apply(SimplePrimitiveId x) {
                    return x.getType();
                }
            }));
            if (types.size() == 1) {
                // select corresponding type
                cbType.setSelectedItem(types.iterator().next());
            } else {
                // select "mixed"
                cbType.setSelectedIndex(3);
            }
        } else if (buf.matches("[\\d,v\\s]+")) { 
            //fallback solution for id1,id2,id3 format 
            tfId.tryToPasteFrom(buf);
        }
    }

    @Override public void windowClosed(WindowEvent e) {
        if (e != null && e.getComponent() == this && getValue() == getContinueButtonIndex()) {
            Main.pref.putInteger("downloadprimitive.lasttype", cbType.getSelectedIndex());

            if (!tfId.readIds()) {
                JOptionPane.showMessageDialog(getParent(),
                        tr("Invalid ID list specified\n"
                                + "Cannot continue."),
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
