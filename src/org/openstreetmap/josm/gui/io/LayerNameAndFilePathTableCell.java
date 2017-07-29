// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.gui.util.CellEditorSupport;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;

/**
 * Display and edit layer name and file path in a <code>JTable</code>.
 *
 * Note: Do not use the same object both as <code>TableCellRenderer</code> and
 * <code>TableCellEditor</code> - this can mess up the current editor component
 * by subsequent calls to the renderer (#12462).
 */
class LayerNameAndFilePathTableCell extends JPanel implements TableCellRenderer, TableCellEditor {
    private static final Color COLOR_ERROR = new Color(255, 197, 197);
    private static final String ELLIPSIS = '…' + File.separator;

    private final JLabel lblLayerName = new JLabel();
    private final JLabel lblFilename = new JLabel("");
    private final JosmTextField tfFilename = new JosmTextField();
    private final JButton btnFileChooser = new JButton(new LaunchFileChooserAction());

    private static final GBC DEFAULT_CELL_STYLE = GBC.eol().fill(GBC.HORIZONTAL).insets(2, 0, 2, 0);

    private final transient CellEditorSupport cellEditorSupport = new CellEditorSupport(this);
    private File value;

    /** constructor that sets the default on each element **/
    LayerNameAndFilePathTableCell() {
        setLayout(new GridBagLayout());

        lblLayerName.setPreferredSize(new Dimension(lblLayerName.getPreferredSize().width, 19));
        lblLayerName.setFont(lblLayerName.getFont().deriveFont(Font.BOLD));

        lblFilename.setPreferredSize(new Dimension(lblFilename.getPreferredSize().width, 19));
        lblFilename.setOpaque(true);
        lblFilename.setLabelFor(btnFileChooser);

        tfFilename.setToolTipText(tr("Either edit the path manually in the text field or click the \"...\" button to open a file chooser."));
        tfFilename.setPreferredSize(new Dimension(tfFilename.getPreferredSize().width, 19));
        tfFilename.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        tfFilename.selectAll();
                    }
                }
                );
        // hide border
        tfFilename.setBorder(BorderFactory.createLineBorder(getBackground()));

        btnFileChooser.setPreferredSize(new Dimension(20, 19));
        btnFileChooser.setOpaque(true);
    }

    /** renderer used while not editing the file path **/
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        removeAll();
        if (value == null) return this;
        SaveLayerInfo info = (SaveLayerInfo) value;
        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
          .append(addLblLayerName(info));
        if (info.isSavable()) {
            add(btnFileChooser, GBC.std());
            sb.append("<br>")
              .append(addLblFilename(info));
        }
        sb.append("</html>");
        setToolTipText(sb.toString());
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        removeAll();
        SaveLayerInfo info = (SaveLayerInfo) value;
        value = info.getFile();
        tfFilename.setText(value == null ? "" : value.toString());

        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
          .append(addLblLayerName(info));

        if (info.isSavable()) {
            add(btnFileChooser, GBC.std());
            add(tfFilename, GBC.eol().fill(GBC.HORIZONTAL).insets(1, 0, 0, 0));
            tfFilename.selectAll();

            sb.append("<br>")
              .append(tfFilename.getToolTipText());
        }
        sb.append("</html>");
        setToolTipText(sb.toString());
        return this;
    }

    private static boolean canWrite(File f) {
        if (f == null || f.isDirectory()) return false;
        if (f.exists() && f.canWrite()) return true;
        return !f.exists() && f.getParentFile() != null && f.getParentFile().canWrite();
    }

    /**
     * Adds layer name label to (this) using the given info. Returns tooltip that should be added to the panel
     * @param info information, user preferences and save/upload states of the layer
     * @return tooltip that should be added to the panel
     */
    private String addLblLayerName(SaveLayerInfo info) {
        lblLayerName.setIcon(info.getLayer().getIcon());
        lblLayerName.setText(info.getName());
        add(lblLayerName, DEFAULT_CELL_STYLE);
        return tr("The bold text is the name of the layer.");
    }

    /**
     * Adds filename label to (this) using the given info. Returns tooltip that should be added to the panel
     * @param info information, user preferences and save/upload states of the layer
     * @return tooltip that should be added to the panel
     */
    private String addLblFilename(SaveLayerInfo info) {
        String tooltip;
        boolean error = false;
        if (info.getFile() == null) {
            error = info.isDoSaveToFile();
            lblFilename.setText(tr("Click here to choose save path"));
            lblFilename.setFont(lblFilename.getFont().deriveFont(Font.ITALIC));
            tooltip = tr("Layer ''{0}'' is not backed by a file", info.getName());
        } else {
            String t = info.getFile().getPath();
            lblFilename.setText(makePathFit(t));
            tooltip = info.getFile().getAbsolutePath();
            if (info.isDoSaveToFile() && !canWrite(info.getFile())) {
                error = true;
                tooltip = tr("File ''{0}'' is not writable. Please enter another file name.", info.getFile().getPath());
            }
        }

        lblFilename.setBackground(error ? COLOR_ERROR : getBackground());
        btnFileChooser.setBackground(error ? COLOR_ERROR : getBackground());

        add(lblFilename, DEFAULT_CELL_STYLE);
        return tr("Click cell to change the file path.") + "<br/>" + tooltip;
    }

    /**
     * Makes the given path fit lblFilename, appends ellipsis on the left if it doesn't fit.
     * Idea: /home/user/josm → …/user/josm → …/josm; and take the first one that fits
     * @param t complete path
     * @return shorter path
     */
    private String makePathFit(String t) {
        boolean hasEllipsis = false;
        while (t != null && !t.isEmpty()) {
            int txtwidth = lblFilename.getFontMetrics(lblFilename.getFont()).stringWidth(t);
            if (txtwidth < lblFilename.getWidth() || t.lastIndexOf(File.separator) < ELLIPSIS.length()) {
                break;
            }
            // remove ellipsis, if present
            t = hasEllipsis ? t.substring(ELLIPSIS.length()) : t;
            // cut next block, and re-add ellipsis
            t = ELLIPSIS + t.substring(t.indexOf(File.separator) + 1);
            hasEllipsis = true;
        }
        return t;
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        cellEditorSupport.addCellEditorListener(l);
    }

    @Override
    public void cancelCellEditing() {
        cellEditorSupport.fireEditingCanceled();
    }

    @Override
    public Object getCellEditorValue() {
        return value;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        cellEditorSupport.removeCellEditorListener(l);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        if (tfFilename.getText() == null || tfFilename.getText().trim().isEmpty()) {
            value = null;
        } else {
            value = new File(tfFilename.getText());
        }
        cellEditorSupport.fireEditingStopped();
        return true;
    }

    private class LaunchFileChooserAction extends AbstractAction {
        LaunchFileChooserAction() {
            putValue(NAME, "...");
            putValue(SHORT_DESCRIPTION, tr("Launch a file chooser to select a file"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File f = SaveActionBase.createAndOpenSaveFileChooser(tr("Select filename"), "osm");
            if (f != null) {
                tfFilename.setText(f.toString());
                stopCellEditing();
            }
        }
    }
}
