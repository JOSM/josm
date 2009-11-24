// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.io.OsmApi;

/**
 * This is a {@see TableCellRenderer} for rendering the various fields of a
 * {@see SaveLayerInfo} in the table {@see SaveLayersTable}.
 *
 *
 */
class SaveLayerInfoCellRenderer implements TableCellRenderer {
    private JLabel lblRenderer;
    private JCheckBox cbRenderer;

    public SaveLayerInfoCellRenderer() {
        lblRenderer = new JLabel();
        cbRenderer = new JCheckBox();
    }

    protected Component prepareLayerNameRenderer(SaveLayerInfo info) {
        lblRenderer.setIcon(info.getLayer().getIcon());
        lblRenderer.setText(info.getName());
        lblRenderer.setToolTipText(info.getLayer().getToolTipText());
        return lblRenderer;
    }

    protected Component prepareUploadRequiredRenderer(SaveLayerInfo info) {
        lblRenderer.setIcon(null);
        lblRenderer.setHorizontalAlignment(JLabel.CENTER);
        String text = info.getLayer().requiresUploadToServer() ? tr("Yes") : tr("No");
        lblRenderer.setText(text);
        if (info.getLayer().requiresUploadToServer()) {
            lblRenderer.setToolTipText(tr("Layer ''{0}'' has modifications which should be uploaded to the server.", info.getName()));
        } else {
            lblRenderer.setToolTipText(tr("Layer ''{0}'' has no modifications to be uploaded.", info.getName()));
        }
        return lblRenderer;
    }

    protected Component prepareSaveToFileRequired(SaveLayerInfo info) {
        lblRenderer.setIcon(null);
        lblRenderer.setHorizontalAlignment(JLabel.CENTER);
        String text = info.getLayer().requiresSaveToFile() ? tr("Yes") : tr("No");
        lblRenderer.setText(text);
        if (info.getLayer().requiresSaveToFile()) {
            lblRenderer.setToolTipText(tr("Layer ''{0}'' has modifications which should be saved to its associated file ''{1}''.", info.getName(), info.getFile().toString()));
        } else {
            lblRenderer.setToolTipText(tr("Layer ''{0}'' has no modifications to be saved.", info.getName()));
        }
        return lblRenderer;
    }

    protected boolean canWrite(File f) {
        if (f == null) return false;
        if (f.isDirectory()) return false;
        if (f.exists() && f.canWrite()) return true;
        if (!f.exists() && f.getParentFile() != null && f.getParentFile().canWrite())
            return true;
        return false;
    }

    protected Component prepareFileNameRenderer(SaveLayerInfo info) {
        lblRenderer.setIcon(null);
        if (info.getFile() == null) {
            if (!info.isDoSaveToFile()) {
                lblRenderer.setText(tr("No file associated with this layer"));
            } else {
                lblRenderer.setBackground(new Color(255,197,197));
                lblRenderer.setText(tr("Please select a file"));
            }
            lblRenderer.setFont(lblRenderer.getFont().deriveFont(Font.ITALIC));
            lblRenderer.setToolTipText(tr("Layer ''{0}'' is not backed by a file", info.getName()));
        } else {
            String text = info.getFile().getName();
            String parent = info.getFile().getParent();
            if (parent != null) {
                if (parent.length() <= 10) {
                    text = info.getFile().getPath();
                } else {
                    text = parent.substring(0, 10) + "..." + File.separator + text;
                }
            }
            lblRenderer.setText(text);
            lblRenderer.setToolTipText(info.getFile().getAbsolutePath());
            if (info.isDoSaveToFile() && !canWrite(info.getFile())) {
                lblRenderer.setBackground(new Color(255,197,197));
                lblRenderer.setToolTipText(tr("File ''{0}'' is not writable. Please enter another file name.", info.getFile().getPath()));
            }
        }
        return lblRenderer;
    }

    protected Component prepareUploadRenderer(SaveLayerInfo info){
        cbRenderer.setSelected(info.isDoUploadToServer());
        lblRenderer.setToolTipText(tr("Select to upload layer ''{0}'' to the server ''{1}''", info.getName(), OsmApi.getOsmApi().getBaseUrl()));
        return cbRenderer;
    }

    protected Component prepareSaveToFileRenderer(SaveLayerInfo info){
        cbRenderer.setSelected(info.isDoSaveToFile());
        lblRenderer.setToolTipText(tr("Select to upload layer ''{0}'' to the server ''{1}''", info.getName(), OsmApi.getOsmApi().getBaseUrl()));
        return cbRenderer;
    }

    protected void resetRenderers() {
        lblRenderer.setOpaque(true);
        lblRenderer.setBackground(UIManager.getColor("Table.background"));
        lblRenderer.setIcon(null);
        lblRenderer.setText("");
        lblRenderer.setFont(UIManager.getFont("Table.font"));
        lblRenderer.setHorizontalAlignment(JLabel.LEFT);

        cbRenderer.setSelected(false);
        cbRenderer.setOpaque(true);
        cbRenderer.setBackground(UIManager.getColor("Table.background"));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        resetRenderers();
        SaveLayerInfo info = (SaveLayerInfo)value;
        switch(column) {
            case 0: return prepareLayerNameRenderer(info);
            case 1: return prepareUploadRequiredRenderer(info);
            case 2: return prepareSaveToFileRequired(info);
            case 3: return prepareFileNameRenderer(info);
            case 4: return prepareUploadRenderer(info);
            case 5: return prepareSaveToFileRenderer(info);
        }
        return null;
    }
}
