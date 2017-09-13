// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.OsmIdTextField;
import org.openstreetmap.josm.gui.widgets.OsmPrimitiveTypesComboBox;
import org.openstreetmap.josm.tools.Utils;

/**
 * Dialog prompt to user to let him choose OSM primitives by specifying their type and IDs.
 * @since 6448, split from DownloadObjectDialog
 */
public class OsmIdSelectionDialog extends ExtendedDialog implements WindowListener {

    protected final JPanel panel = new JPanel();
    protected final OsmPrimitiveTypesComboBox cbType = new OsmPrimitiveTypesComboBox();
    protected final OsmIdTextField tfId = new OsmIdTextField();
    protected final HistoryComboBox cbId = new HistoryComboBox();
    protected final transient GroupLayout layout = new GroupLayout(panel);

    /**
     * Creates a new OsmIdSelectionDialog
     * @param parent       The parent element that will be used for position and maximum size
     * @param title        The text that will be shown in the window titlebar
     * @param buttonTexts  String Array of the text that will appear on the buttons. The first button is the default one.
     */
    public OsmIdSelectionDialog(Component parent, String title, String... buttonTexts) {
        super(parent, title, buttonTexts);
    }

    /**
     * Creates a new OsmIdSelectionDialog
     * @param parent The parent element that will be used for position and maximum size
     * @param title The text that will be shown in the window titlebar
     * @param buttonTexts String Array of the text that will appear on the buttons. The first button is the default one.
     * @param modal Set it to {@code true} if you want the dialog to be modal
     */
    public OsmIdSelectionDialog(Component parent, String title, String[] buttonTexts, boolean modal) {
        super(parent, title, buttonTexts, modal);
    }

    /**
     * Creates a new OsmIdSelectionDialog
     * @param parent The parent element that will be used for position and maximum size
     * @param title The text that will be shown in the window titlebar
     * @param buttonTexts String Array of the text that will appear on the buttons. The first button is the default one.
     * @param modal Set it to {@code true} if you want the dialog to be modal
     * @param disposeOnClose whether to call {@link #dispose} when closing the dialog
     */
    public OsmIdSelectionDialog(Component parent, String title, String[] buttonTexts, boolean modal, boolean disposeOnClose) {
        super(parent, title, buttonTexts, modal, disposeOnClose);
    }

    protected void init() {
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JLabel lbl1 = new JLabel(tr("Object type:"));
        lbl1.setLabelFor(cbType);

        cbType.addItem(trc("osm object types", "mixed"));
        cbType.setToolTipText(tr("Choose the OSM object type"));
        JLabel lbl2 = new JLabel(tr("Object ID:"));
        lbl2.setLabelFor(cbId);

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

        final String help1 = /* I18n: {0} and contains example strings not meant for translation. */
                tr("Object IDs can be separated by comma or space, for instance: {0}",
                        "<b>" + Utils.joinAsHtmlUnorderedList(Arrays.asList("1 2 5", "1,2,5")) + "</b>");
        final String help2 = /* I18n: {0} and contains example strings not meant for translation. {1}=n, {2}=w, {3}=r. */
                tr("In mixed mode, specify objects like this: {0}<br/>"
                                + "({1} stands for <i>node</i>, {2} for <i>way</i>, and {3} for <i>relation</i>)",
                        "<b>w123, n110, w12, r15</b>", "<b>n</b>", "<b>w</b>", "<b>r</b>");
        final String help3 = /* I18n: {0} and contains example strings not meant for translation. */
                tr("Ranges of object IDs are specified with a hyphen, for instance: {0}",
                        "<b>" + Utils.joinAsHtmlUnorderedList(Arrays.asList("w1-5", "n30-37", "r501-5")) + "</b>");
        HtmlPanel help = new HtmlPanel(help1 + "<br/>" + help2 + "<br/><br/>" + help3);
        help.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        cbType.addItemListener(e -> {
            tfId.setType(cbType.getType());
            tfId.performValidation();
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
        List<String> cmtHistory = new LinkedList<>(
                Main.pref.getList(getClass().getName() + ".primitivesHistory", new LinkedList<String>()));
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
        Main.pref.putList(getClass().getName() + ".primitivesHistory", cbHistory.getHistory());
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
        cbType.setSelectedIndex(Main.pref.getInt("downloadprimitive.lasttype", 0));
        tfId.setType(cbType.getType());
        if (Main.pref.getBoolean("downloadprimitive.autopaste", true)) {
            tryToPasteFromClipboard(tfId, cbType);
        }
        setDefaultButton(getContinueButtonIndex());
        addWindowListener(this);
        super.setupDialog();
    }

    protected void tryToPasteFromClipboard(OsmIdTextField tfId, OsmPrimitiveTypesComboBox cbType) {
        String buf = ClipboardUtils.getClipboardStringContent();
        if (buf == null || buf.isEmpty()) return;
        if (buf.length() > Main.pref.getInt("downloadprimitive.max-autopaste-length", 2000)) return;
        final List<SimplePrimitiveId> ids = SimplePrimitiveId.fuzzyParse(buf);
        if (!ids.isEmpty()) {
            final String parsedText = ids.stream().map(x -> x.getType().getAPIName().charAt(0) + String.valueOf(x.getUniqueId()))
                    .collect(Collectors.joining(", "));
            tfId.tryToPasteFrom(parsedText);
            final EnumSet<OsmPrimitiveType> types = ids.stream().map(SimplePrimitiveId::getType).collect(
                    Collectors.toCollection(() -> EnumSet.noneOf(OsmPrimitiveType.class)));
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
            Main.pref.putInt("downloadprimitive.lasttype", cbType.getSelectedIndex());

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

    @Override public void windowOpened(WindowEvent e) {
        // Do nothing
    }

    @Override public void windowClosing(WindowEvent e) {
        // Do nothing
    }

    @Override public void windowIconified(WindowEvent e) {
        // Do nothing
    }

    @Override public void windowDeiconified(WindowEvent e) {
        // Do nothing
    }

    @Override public void windowActivated(WindowEvent e) {
        // Do nothing
    }

    @Override public void windowDeactivated(WindowEvent e) {
        // Do nothing
    }
}
