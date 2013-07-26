// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class SaveLayersModel extends DefaultTableModel {
    static final public String MODE_PROP = SaveLayerInfo.class.getName() + ".mode";
    public enum Mode {
        EDITING_DATA,
        UPLOADING_AND_SAVING
    }

    private List<SaveLayerInfo> layerInfo;
    private Mode mode;
    private PropertyChangeSupport support;

    // keep in sync with how the columns are ordered in SaveLayersTableColumnModel#build
    private static final int columnFilename = 0;
    private static final int columnActions = 2;

    public SaveLayersModel() {
        mode = Mode.EDITING_DATA;
        support = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }

    protected void fireModeChanged(Mode oldValue, Mode newValue) {
        support.firePropertyChange(MODE_PROP, oldValue, newValue);
    }

    public void setMode(Mode newValue) {
        Mode oldValue = this.mode;
        this.mode = newValue;
        fireModeChanged(oldValue, newValue);
    }

    public Mode getMode() {
        return mode;
    }

    public void populate(List<OsmDataLayer> layers) {
        layerInfo = new ArrayList<SaveLayerInfo>();
        if (layers == null) return;
        for (OsmDataLayer layer: layers) {
            layerInfo.add(new SaveLayerInfo(layer));
        }
        Collections.sort(
                layerInfo,
                new Comparator<SaveLayerInfo>() {
                    @Override
                    public int compare(SaveLayerInfo o1, SaveLayerInfo o2) {
                        return o1.compareTo(o2);
                    }
                }
        );
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        if (layerInfo == null) return 0;
        return layerInfo.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (layerInfo == null) return null;
        return layerInfo.get(row);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == columnFilename || column == columnActions;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        switch(column) {
        case columnFilename:
            this.layerInfo.get(row).setFile((File)value);
            this.layerInfo.get(row).setDoSaveToFile(true);
            break;
        case columnActions:
            boolean[] values = (boolean[]) value;
            this.layerInfo.get(row).setDoUploadToServer(values[0]);
            this.layerInfo.get(row).setDoSaveToFile(values[1]);
            break;
        }
        fireTableDataChanged();
    }

    public List<SaveLayerInfo> getSafeLayerInfo() {
        return this.layerInfo;
    }

    public List<SaveLayerInfo> getLayersWithoutFilesAndSaveRequest() {
        List<SaveLayerInfo> ret = new ArrayList<SaveLayerInfo>();
        for (SaveLayerInfo info: layerInfo) {
            if (info.isDoSaveToFile() && info.getFile() == null) {
                ret.add(info);
            }
        }
        return ret;
    }

    public List<SaveLayerInfo> getLayersWithIllegalFilesAndSaveRequest() {
        List<SaveLayerInfo> ret =new ArrayList<SaveLayerInfo>();
        for (SaveLayerInfo info: layerInfo) {
            if (info.isDoSaveToFile() && info.getFile() != null && info.getFile().exists() && !info.getFile().canWrite()) {
                ret.add(info);
            }
        }
        return ret;
    }

    public List<SaveLayerInfo> getLayersWithConflictsAndUploadRequest() {
        List<SaveLayerInfo> ret =new ArrayList<SaveLayerInfo>();
        for (SaveLayerInfo info: layerInfo) {
            if (info.isDoUploadToServer() && !info.getLayer().getConflicts().isEmpty()) {
                ret.add(info);
            }
        }
        return ret;
    }

    public List<SaveLayerInfo> getLayersToUpload() {
        List<SaveLayerInfo> ret =new ArrayList<SaveLayerInfo>();
        for (SaveLayerInfo info: layerInfo) {
            if (info.isDoUploadToServer()) {
                ret.add(info);
            }
        }
        return ret;
    }

    public List<SaveLayerInfo> getLayersToSave() {
        List<SaveLayerInfo> ret =new ArrayList<SaveLayerInfo>();
        for (SaveLayerInfo info: layerInfo) {
            if (info.isDoSaveToFile()) {
                ret.add(info);
            }
        }
        return ret;
    }

    public void setUploadState(OsmDataLayer layer, UploadOrSaveState state) {
        SaveLayerInfo info = getSaveLayerInfo(layer);
        if (info != null) {
            info.setUploadState(state);
        }
        fireTableDataChanged();
    }

    public void setSaveState(OsmDataLayer layer, UploadOrSaveState state) {
        SaveLayerInfo info = getSaveLayerInfo(layer);
        if (info != null) {
            info.setSaveState(state);
        }
        fireTableDataChanged();
    }

    public SaveLayerInfo getSaveLayerInfo(OsmDataLayer layer) {
        for (SaveLayerInfo info: this.layerInfo) {
            if (info.getLayer() == layer)
                return info;
        }
        return null;
    }

    public void resetSaveAndUploadState() {
        for (SaveLayerInfo info: layerInfo) {
            info.setSaveState(null);
            info.setUploadState(null);
        }
    }

    public boolean hasUnsavedData() {
        for (SaveLayerInfo info: layerInfo) {
            if (info.isDoUploadToServer() && ! UploadOrSaveState.OK.equals(info.getUploadState()))
                return true;
            if (info.isDoSaveToFile() && ! UploadOrSaveState.OK.equals(info.getSaveState()))
                return true;
        }
        return false;
    }

    public int getNumCancel() {
        int ret = 0;
        for (SaveLayerInfo info: layerInfo) {
            if (UploadOrSaveState.CANCELED.equals(info.getSaveState())
                    || UploadOrSaveState.CANCELED.equals(info.getUploadState())) {
                ret++;
            }
        }
        return ret;
    }

    public int getNumFailed() {
        int ret = 0;
        for (SaveLayerInfo info: layerInfo) {
            if (UploadOrSaveState.FAILED.equals(info.getSaveState())
                    || UploadOrSaveState.FAILED.equals(info.getUploadState())) {
                ret++;
            }
        }
        return ret;
    }
}
