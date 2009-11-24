// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.io.SaveLayersModel.Mode;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.SwingRenderingProgressMonitor;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

public class SaveLayersDialog extends JDialog {
    static public enum UserAction {
        /**
         * save/upload layers was successful, proceed with operation
         */
        PROCEED,
        /**
         * save/upload of layers was not successful or user cancelled
         * operation
         */
        CANCEL
    }

    private SaveLayersModel model;
    private UserAction action = UserAction.CANCEL;
    private UploadAndSaveProgressRenderer pnlUploadLayers;

    private SaveAndProceedAction saveAndProceedAction;
    private DiscardAndProceedAction discardAndProceedAction;
    private CancelAction cancelAction;
    private SaveAndUploadTask saveAndUploadTask;

    /**
     * builds the GUI
     */
    protected void build() {
        WindowGeometry geometry = WindowGeometry.centerOnScreen(new Dimension(600,300));
        geometry.apply(this);
        getContentPane().setLayout(new BorderLayout());

        model = new SaveLayersModel();
        SaveLayersTable table;
        JScrollPane pane = new JScrollPane(table = new SaveLayersTable(model));
        model.addPropertyChangeListener(table);
        getContentPane().add(pane, BorderLayout.CENTER);
        getContentPane().add(buildButtonRow(), BorderLayout.SOUTH);
        table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getWidth(), 40));

        addWindowListener(new WindowClosingAdapter());
    }

    /**
     * builds the button row
     *
     * @return the panel with the button row
     */
    protected JPanel buildButtonRow() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        saveAndProceedAction = new SaveAndProceedAction();
        model.addPropertyChangeListener(saveAndProceedAction);
        pnl.add(new SideButton(saveAndProceedAction));

        discardAndProceedAction = new DiscardAndProceedAction();
        model.addPropertyChangeListener(discardAndProceedAction);
        pnl.add(new SideButton(discardAndProceedAction));

        cancelAction = new CancelAction();
        pnl.add(new SideButton(cancelAction));

        JPanel pnl2 = new JPanel();
        pnl2.setLayout(new BorderLayout());
        pnl2.add(pnlUploadLayers = new UploadAndSaveProgressRenderer(), BorderLayout.CENTER);
        model.addPropertyChangeListener(pnlUploadLayers);
        pnl2.add(pnl, BorderLayout.SOUTH);
        return pnl2;
    }

    public void prepareForSavingAndUpdatingLayersBeforeExit() {
        setTitle(tr("Unsaved changes - Save/Upload before exiting?"));
        this.saveAndProceedAction.initForSaveAndExit();
        this.discardAndProceedAction.initForDiscardAndExit();
    }

    public void prepareForSavingAndUpdatingLayersBeforeDelete() {
        setTitle(tr("Unsaved changes - Save/Upload before deleting?"));
        this.saveAndProceedAction.initForSaveAndDelete();
        this.discardAndProceedAction.initForDiscardAndDelete();
    }

    public SaveLayersDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), true /* modal */);
        build();
    }

    public UserAction getUserAction() {
        return this.action;
    }

    public SaveLayersModel getModel() {
        return model;
    }

    protected void launchSafeAndUploadTask() {
        ProgressMonitor monitor = new SwingRenderingProgressMonitor(pnlUploadLayers);
        monitor.beginTask(tr("Uploading and saving modified layers ..."));
        this.saveAndUploadTask = new SaveAndUploadTask(model, monitor);
        new Thread(saveAndUploadTask).start();
    }

    protected void cancelSafeAndUploadTask() {
        if (this.saveAndUploadTask != null) {
            this.saveAndUploadTask.cancel();
        }
        model.setMode(Mode.EDITING_DATA);
    }

    private static class  LayerListWarningMessagePanel extends JPanel {
        private JLabel lblMessage;
        private JList lstLayers;

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            add(lblMessage = new JLabel(), gc);
            lblMessage.setHorizontalAlignment(JLabel.LEFT);
            lstLayers = new JList();
            lstLayers.setCellRenderer(
                    new DefaultListCellRenderer() {
                        @Override
                        public Component getListCellRendererComponent(JList list, Object value, int index,
                                boolean isSelected, boolean cellHasFocus) {
                            SaveLayerInfo info = (SaveLayerInfo)value;
                            setIcon(info.getLayer().getIcon());
                            setText(info.getName());
                            return this;
                        }
                    }
            );
            gc.gridx = 0;
            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(lstLayers,gc);
        }

        public LayerListWarningMessagePanel(String msg, List<SaveLayerInfo> infos) {
            build();
            lblMessage.setText(msg);
            lstLayers.setListData(infos.toArray());
        }
    }

    protected void warnLayersWithConflictsAndUploadRequest(List<SaveLayerInfo> infos) {
        String msg = trn("<html>{0} layer has unresolved conflicts.<br>"
                + "Either resolve them first or discard the modifications.<br>"
                + "Layer with conflicts:</html>",
                "<html>{0} layers have unresolved conflicts.<br>"
                + "Either resolve them first or discard the modifications.<br>"
                + "Layers with conflicts:</html>",
                infos.size(),
                infos.size());
        JOptionPane.showConfirmDialog(
                Main.parent,
                new LayerListWarningMessagePanel(msg, infos),
                tr("Unsaved data and conflicts"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
    }

    protected void warnLayersWithoutFilesAndSaveRequest(List<SaveLayerInfo> infos) {
        String msg = trn("<html>{0} layer needs saving but has no associated file.<br>"
                + "Either select a file for this layer or discard the changes.<br>"
                + "Layer without a file:</html>",
                "<html>{0} layers need saving but have no associated file.<br>"
                + "Either select a file for each of them or discard the changes.<br>"
                + "Layers without a file:</html>",
                infos.size(),
                infos.size());
        JOptionPane.showConfirmDialog(
                Main.parent,
                new LayerListWarningMessagePanel(msg, infos),
                tr("Unsaved data and missing associated file"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
    }

    protected void warnLayersWithIllegalFilesAndSaveRequest(List<SaveLayerInfo> infos) {
        String msg = trn("<html>{0} layer needs saving but has an associated file<br>"
                + "which can't be written.<br>"
                + "Either select another file for this layer or discard the changes.<br>"
                + "Layer with a non-writable file:</html>",
                "<html>{0} layers need saving but have associated files<br>"
                + "which can't be written.<br>"
                + "Either select another file for each of them or discard the changes.<br>"
                + "Layers with non-writable files:</html>",
                infos.size(),
                infos.size());
        JOptionPane.showConfirmDialog(
                Main.parent,
                new LayerListWarningMessagePanel(msg, infos),
                tr("Unsaved data non-writable files"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
    }

    protected boolean confirmSaveLayerInfosOK() {
        List<SaveLayerInfo> layerInfos = model.getLayersWithConflictsAndUploadRequest();
        if (!layerInfos.isEmpty()) {
            warnLayersWithConflictsAndUploadRequest(layerInfos);
            return false;
        }

        layerInfos = model.getLayersWithoutFilesAndSaveRequest();
        if (!layerInfos.isEmpty()) {
            warnLayersWithoutFilesAndSaveRequest(layerInfos);
            return false;
        }

        layerInfos = model.getLayersWithIllegalFilesAndSaveRequest();
        if (!layerInfos.isEmpty()) {
            warnLayersWithIllegalFilesAndSaveRequest(layerInfos);
            return false;
        }

        return true;
    }

    protected void setUserAction(UserAction action) {
        this.action = action;
    }

    class WindowClosingAdapter extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            cancelAction.cancel();
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SHORT_DESCRIPTION, tr("Close this dialog and resume editing in JOSM"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
        }

        protected void cancelWhenInEditingModel() {
            setUserAction(UserAction.CANCEL);
            setVisible(false);
        }

        protected void cancelWhenInSaveAndUploadingMode() {
            cancelSafeAndUploadTask();
        }

        public void cancel() {
            switch(model.getMode()) {
                case EDITING_DATA: cancelWhenInEditingModel(); break;
                case UPLOADING_AND_SAVING: cancelSafeAndUploadTask(); break;
            }
        }

        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    }

    class DiscardAndProceedAction extends AbstractAction  implements PropertyChangeListener {
        public DiscardAndProceedAction() {
            initForDiscardAndExit();
        }

        public void initForDiscardAndExit() {
            putValue(NAME, tr("Discard and Exit"));
            putValue(SHORT_DESCRIPTION, tr("Exit JOSM without saving. Unsaved changes are lost."));
            putValue(SMALL_ICON, ImageProvider.get("exit"));
        }

        public void initForDiscardAndDelete() {
            putValue(NAME, tr("Discard and Delete"));
            putValue(SHORT_DESCRIPTION, tr("Delete layers without saving. Unsaved changes are lost."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
        }

        public void actionPerformed(ActionEvent e) {
            setUserAction(UserAction.PROCEED);
            setVisible(false);
        }
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SaveLayersModel.MODE_PROP)) {
                Mode mode = (Mode)evt.getNewValue();
                switch(mode) {
                    case EDITING_DATA: setEnabled(true); break;
                    case UPLOADING_AND_SAVING: setEnabled(false); break;
                }
            }
        }
    }

    class SaveAndProceedAction extends AbstractAction implements PropertyChangeListener {
        public SaveAndProceedAction() {
            initForSaveAndExit();
        }

        public void initForSaveAndExit() {
            putValue(NAME, tr("Save/Upload and Exit"));
            putValue(SHORT_DESCRIPTION, tr("Exit JOSM with saving. Unsaved changes are uploaded and/or saved."));
            putValue(SMALL_ICON, ImageProvider.get("exit"));
        }

        public void initForSaveAndDelete() {
            putValue(NAME, tr("Save/Upload and Delete"));
            putValue(SHORT_DESCRIPTION, tr("Save/Upload layers before deleting. Unsaved changes are not lost."));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
        }

        public void actionPerformed(ActionEvent e) {
            if (! confirmSaveLayerInfosOK())
                return;
            launchSafeAndUploadTask();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SaveLayersModel.MODE_PROP)) {
                SaveLayersModel.Mode mode = (SaveLayersModel.Mode)evt.getNewValue();
                switch(mode) {
                    case EDITING_DATA: setEnabled(true); break;
                    case UPLOADING_AND_SAVING: setEnabled(false); break;
                }
            }
        }
    }

    /**
     * This is the asynchronous task which uploads modified layers to the server and
     * saves them to files, if requested by the user.
     *
     */
    protected class SaveAndUploadTask implements Runnable {

        private SaveLayersModel model;
        private ProgressMonitor monitor;
        private ExecutorService worker;
        private boolean cancelled;
        private Future<?> currentFuture;
        private AbstractIOTask currentTask;

        public SaveAndUploadTask(SaveLayersModel model, ProgressMonitor monitor) {
            this.model = model;
            this.monitor = monitor;
            this.worker = Executors.newSingleThreadExecutor();
        }

        protected void uploadLayers(List<SaveLayerInfo> toUpload) {
            for (final SaveLayerInfo layerInfo: toUpload) {
                if (cancelled) {
                    model.setUploadState(layerInfo.getLayer(), UploadOrSaveState.CANCELLED);
                    continue;
                }
                monitor.subTask(tr("Preparing layer ''{0}'' for upload ...", layerInfo.getName()));

                if (!new UploadAction().checkPreUploadConditions(layerInfo.getLayer())) {
                    model.setUploadState(layerInfo.getLayer(), UploadOrSaveState.FAILED);
                    continue;
                }

                currentTask = new UploadLayerTask(
                        layerInfo.getLayer(),
                        monitor,
                        UploadDialog.getUploadDialog().getChangeset(),
                        UploadDialog.getUploadDialog().isDoCloseAfterUpload()
                );
                currentFuture = worker.submit(currentTask);
                try {
                    // wait for the asynchronous task to complete
                    //
                    currentFuture.get();
                } catch(CancellationException e) {
                    model.setUploadState(layerInfo.getLayer(), UploadOrSaveState.CANCELLED);
                } catch(Exception e) {
                    e.printStackTrace();
                    model.setUploadState(layerInfo.getLayer(), UploadOrSaveState.FAILED);
                    ExceptionDialogUtil.explainException(e);
                }
                if (currentTask.isCancelled()) {
                    model.setUploadState(layerInfo.getLayer(), UploadOrSaveState.CANCELLED);
                } else if (currentTask.isFailed()) {
                    currentTask.getLastException().printStackTrace();
                    ExceptionDialogUtil.explainException(currentTask.getLastException());
                    model.setUploadState(layerInfo.getLayer(), UploadOrSaveState.FAILED);
                } else {
                    model.setUploadState(layerInfo.getLayer(), UploadOrSaveState.OK);
                }
                currentTask = null;
                currentFuture = null;
            }
        }

        protected void saveLayers(List<SaveLayerInfo> toSave) {
            for (final SaveLayerInfo layerInfo: toSave) {
                if (cancelled) {
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.CANCELLED);
                    continue;
                }
                currentTask= new SaveLayerTask(layerInfo, monitor);
                currentFuture = worker.submit(currentTask);

                try {
                    // wait for the asynchronous task to complete
                    //
                    currentFuture.get();
                } catch(CancellationException e) {
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.CANCELLED);
                } catch(Exception e) {
                    e.printStackTrace();
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.FAILED);
                    ExceptionDialogUtil.explainException(e);
                }
                if (currentTask.isCancelled()) {
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.CANCELLED);
                } else if (currentTask.isFailed()) {
                    if (currentTask.getLastException() != null) {
                        currentTask.getLastException().printStackTrace();
                        ExceptionDialogUtil.explainException(currentTask.getLastException());
                    }
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.FAILED);
                } else {
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.OK);
                }
                this.currentTask = null;
                this.currentFuture = null;
            }
        }

        protected void warnBecauseOfUnsavedData() {
            int numProblems = model.getNumCancel() + model.getNumFailed();
            if (numProblems == 0) return;
            String msg = tr(
                    "<html>An upload and/or save operation of one layer with modifications<br>"
                    + "was cancelled or has failed.</html>",
                    "<html>Upload and/or save operations of {0} layers with modifications<br>"
                    + "were cancelled or have failed.</html>",
                    numProblems,
                    numProblems
            );
            JOptionPane.showMessageDialog(
                    Main.parent,
                    msg,
                    tr("Incomplete upload and/or save"),
                    JOptionPane.WARNING_MESSAGE
            );
        }

        public void run() {
            model.setMode(SaveLayersModel.Mode.UPLOADING_AND_SAVING);
            List<SaveLayerInfo> toUpload = model.getLayersToUpload();
            if (!toUpload.isEmpty()) {
                uploadLayers(toUpload);
            }
            List<SaveLayerInfo> toSave = model.getLayersToSave();
            if (!toSave.isEmpty()) {
                saveLayers(toSave);
            }
            model.setMode(SaveLayersModel.Mode.EDITING_DATA);
            if (model.hasUnsavedData()) {
                warnBecauseOfUnsavedData();
                model.setMode(Mode.EDITING_DATA);
                if (cancelled) {
                    setUserAction(UserAction.CANCEL);
                    setVisible(false);
                }
            } else {
                setUserAction(UserAction.PROCEED);
                setVisible(false);
            }
        }

        public void cancel() {
            if (currentTask != null) {
                currentTask.cancel();
            }
            cancelled = true;
        }
    }
}
