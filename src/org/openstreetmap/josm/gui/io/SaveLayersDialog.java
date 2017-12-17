// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SessionSaveAsAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.io.SaveLayersModel.Mode;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.SwingRenderingProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Dialog that pops up when the user closes a layer with modified data.
 *
 * It asks for confirmation that all modification should be discarded and offers
 * to save the layers to file or upload to server, depending on the type of layer.
 */
public class SaveLayersDialog extends JDialog implements TableModelListener {

    /**
     * The cause for requesting an action on unsaved modifications
     */
    public enum Reason {
        /** deleting a layer */
        DELETE,
        /** exiting JOSM */
        EXIT,
        /** restarting JOSM */
        RESTART
    }

    private enum UserAction {
        /** save/upload layers was successful, proceed with operation */
        PROCEED,
        /** save/upload of layers was not successful or user canceled operation */
        CANCEL
    }

    private final SaveLayersModel model = new SaveLayersModel();
    private UserAction action = UserAction.CANCEL;
    private final UploadAndSaveProgressRenderer pnlUploadLayers = new UploadAndSaveProgressRenderer();

    private final SaveAndProceedAction saveAndProceedAction = new SaveAndProceedAction();
    private final SaveSessionAction saveSessionAction = new SaveSessionAction();
    private final DiscardAndProceedAction discardAndProceedAction = new DiscardAndProceedAction();
    private final CancelAction cancelAction = new CancelAction();
    private transient SaveAndUploadTask saveAndUploadTask;

    private final JButton saveAndProceedActionButton = new JButton(saveAndProceedAction);

    /**
     * Asks user to perform "save layer" operations (save on disk and/or upload data to server) before data layers deletion.
     *
     * @param selectedLayers The layers to check. Only instances of {@link AbstractModifiableLayer} are considered.
     * @param reason the cause for requesting an action on unsaved modifications
     * @return {@code true} if there was nothing to save, or if the user wants to proceed to save operations.
     *         {@code false} if the user cancels.
     * @since 11093
     */
    public static boolean saveUnsavedModifications(Iterable<? extends Layer> selectedLayers, Reason reason) {
        if (!GraphicsEnvironment.isHeadless()) {
            SaveLayersDialog dialog = new SaveLayersDialog(Main.parent);
            List<AbstractModifiableLayer> layersWithUnmodifiedChanges = new ArrayList<>();
            for (Layer l: selectedLayers) {
                if (!(l instanceof AbstractModifiableLayer)) {
                    continue;
                }
                AbstractModifiableLayer odl = (AbstractModifiableLayer) l;
                if (odl.isModified() &&
                        ((!odl.isSavable() && !odl.isUploadable()) ||
                                odl.requiresSaveToFile() ||
                                (odl.requiresUploadToServer() && !odl.isUploadDiscouraged()))) {
                    layersWithUnmodifiedChanges.add(odl);
                }
            }
            dialog.prepareForSavingAndUpdatingLayers(reason);
            if (!layersWithUnmodifiedChanges.isEmpty()) {
                dialog.getModel().populate(layersWithUnmodifiedChanges);
                dialog.setVisible(true);
                switch(dialog.getUserAction()) {
                    case PROCEED: return true;
                    case CANCEL:
                    default: return false;
                }
            }
        }

        return true;
    }

    /**
     * Constructs a new {@code SaveLayersDialog}.
     * @param parent parent component
     */
    public SaveLayersDialog(Component parent) {
        super(GuiHelper.getFrameForComponent(parent), ModalityType.DOCUMENT_MODAL);
        build();
    }

    /**
     * builds the GUI
     */
    protected void build() {
        WindowGeometry geometry = WindowGeometry.centerOnScreen(new Dimension(650, 300));
        geometry.applySafe(this);
        getContentPane().setLayout(new BorderLayout());

        SaveLayersTable table = new SaveLayersTable(model);
        JScrollPane pane = new JScrollPane(table);
        model.addPropertyChangeListener(table);
        table.getModel().addTableModelListener(this);

        getContentPane().add(pane, BorderLayout.CENTER);
        getContentPane().add(buildButtonRow(), BorderLayout.SOUTH);

        addWindowListener(new WindowClosingAdapter());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * builds the button row
     *
     * @return the panel with the button row
     */
    protected JPanel buildButtonRow() {
        JPanel pnl = new JPanel(new GridBagLayout());

        model.addPropertyChangeListener(saveAndProceedAction);
        pnl.add(saveAndProceedActionButton, GBC.std(0, 0).insets(5, 5, 0, 0).fill(GBC.HORIZONTAL));

        pnl.add(new JButton(saveSessionAction), GBC.std(1, 0).insets(5, 5, 5, 0).fill(GBC.HORIZONTAL));

        model.addPropertyChangeListener(discardAndProceedAction);
        pnl.add(new JButton(discardAndProceedAction), GBC.std(0, 1).insets(5, 5, 0, 5).fill(GBC.HORIZONTAL));

        pnl.add(new JButton(cancelAction), GBC.std(1, 1).insets(5, 5, 5, 5).fill(GBC.HORIZONTAL));

        JPanel pnl2 = new JPanel(new BorderLayout());
        pnl2.add(pnlUploadLayers, BorderLayout.CENTER);
        model.addPropertyChangeListener(pnlUploadLayers);
        pnl2.add(pnl, BorderLayout.SOUTH);
        return pnl2;
    }

    public void prepareForSavingAndUpdatingLayers(final Reason reason) {
        switch (reason) {
            case EXIT:
                setTitle(tr("Unsaved changes - Save/Upload before exiting?"));
                break;
            case DELETE:
                setTitle(tr("Unsaved changes - Save/Upload before deleting?"));
                break;
            case RESTART:
                setTitle(tr("Unsaved changes - Save/Upload before restarting?"));
                break;
        }
        this.saveAndProceedAction.initForReason(reason);
        this.discardAndProceedAction.initForReason(reason);
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
        new Thread(saveAndUploadTask, saveAndUploadTask.getClass().getName()).start();
    }

    protected void cancelSafeAndUploadTask() {
        if (this.saveAndUploadTask != null) {
            this.saveAndUploadTask.cancel();
        }
        model.setMode(Mode.EDITING_DATA);
    }

    private static class LayerListWarningMessagePanel extends JPanel {
        static final class LayerCellRenderer implements ListCellRenderer<SaveLayerInfo> {
            private final DefaultListCellRenderer def = new DefaultListCellRenderer();

            @Override
            public Component getListCellRendererComponent(JList<? extends SaveLayerInfo> list, SaveLayerInfo info, int index,
                    boolean isSelected, boolean cellHasFocus) {
                def.setIcon(info.getLayer().getIcon());
                def.setText(info.getName());
                return def;
            }
        }

        private final JLabel lblMessage = new JLabel();
        private final JList<SaveLayerInfo> lstLayers = new JList<>();

        LayerListWarningMessagePanel(String msg, List<SaveLayerInfo> infos) {
            super(new GridBagLayout());
            build();
            lblMessage.setText(msg);
            lstLayers.setListData(infos.toArray(new SaveLayerInfo[0]));
        }

        protected void build() {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            add(lblMessage, gc);
            lblMessage.setHorizontalAlignment(JLabel.LEFT);
            lstLayers.setCellRenderer(new LayerCellRenderer());
            gc.gridx = 0;
            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(lstLayers, gc);
        }
    }

    private static void warn(String msg, List<SaveLayerInfo> infos, String title) {
        JPanel panel = new LayerListWarningMessagePanel(msg, infos);
        // For unit test coverage in headless mode
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showConfirmDialog(Main.parent, panel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
        }
    }

    protected static void warnLayersWithConflictsAndUploadRequest(List<SaveLayerInfo> infos) {
        warn(trn("<html>{0} layer has unresolved conflicts.<br>"
                + "Either resolve them first or discard the modifications.<br>"
                + "Layer with conflicts:</html>",
                "<html>{0} layers have unresolved conflicts.<br>"
                + "Either resolve them first or discard the modifications.<br>"
                + "Layers with conflicts:</html>",
                infos.size(),
                infos.size()),
             infos, tr("Unsaved data and conflicts"));
    }

    protected static void warnLayersWithoutFilesAndSaveRequest(List<SaveLayerInfo> infos) {
        warn(trn("<html>{0} layer needs saving but has no associated file.<br>"
                + "Either select a file for this layer or discard the changes.<br>"
                + "Layer without a file:</html>",
                "<html>{0} layers need saving but have no associated file.<br>"
                + "Either select a file for each of them or discard the changes.<br>"
                + "Layers without a file:</html>",
                infos.size(),
                infos.size()),
             infos, tr("Unsaved data and missing associated file"));
    }

    protected static void warnLayersWithIllegalFilesAndSaveRequest(List<SaveLayerInfo> infos) {
        warn(trn("<html>{0} layer needs saving but has an associated file<br>"
                + "which cannot be written.<br>"
                + "Either select another file for this layer or discard the changes.<br>"
                + "Layer with a non-writable file:</html>",
                "<html>{0} layers need saving but have associated files<br>"
                + "which cannot be written.<br>"
                + "Either select another file for each of them or discard the changes.<br>"
                + "Layers with non-writable files:</html>",
                infos.size(),
                infos.size()),
             infos, tr("Unsaved data non-writable files"));
    }

    static boolean confirmSaveLayerInfosOK(SaveLayersModel model) {
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

    /**
     * Closes this dialog and frees all native screen resources.
     */
    public void closeDialog() {
        setVisible(false);
        dispose();
    }

    class WindowClosingAdapter extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            cancelAction.cancel();
        }
    }

    class CancelAction extends AbstractAction {
        CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SHORT_DESCRIPTION, tr("Close this dialog and resume editing in JOSM"));
            new ImageProvider("cancel").getResource().attachImageIcon(this, true);
            InputMapUtils.addEscapeAction(getRootPane(), this);
        }

        protected void cancelWhenInEditingModel() {
            setUserAction(UserAction.CANCEL);
            closeDialog();
        }

        public void cancel() {
            switch(model.getMode()) {
            case EDITING_DATA: cancelWhenInEditingModel();
                break;
            case UPLOADING_AND_SAVING: cancelSafeAndUploadTask();
                break;
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    }

    class DiscardAndProceedAction extends AbstractAction implements PropertyChangeListener {
        DiscardAndProceedAction() {
            initForReason(Reason.EXIT);
        }

        public void initForReason(Reason reason) {
            switch (reason) {
                case EXIT:
                    putValue(NAME, tr("Exit now!"));
                    putValue(SHORT_DESCRIPTION, tr("Exit JOSM without saving. Unsaved changes are lost."));
                    new ImageProvider("exit").getResource().attachImageIcon(this, true);
                    break;
                case RESTART:
                    putValue(NAME, tr("Restart now!"));
                    putValue(SHORT_DESCRIPTION, tr("Restart JOSM without saving. Unsaved changes are lost."));
                    new ImageProvider("restart").getResource().attachImageIcon(this, true);
                    break;
                case DELETE:
                    putValue(NAME, tr("Delete now!"));
                    putValue(SHORT_DESCRIPTION, tr("Delete layers without saving. Unsaved changes are lost."));
                    new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
                    break;
            }

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setUserAction(UserAction.PROCEED);
            closeDialog();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SaveLayersModel.MODE_PROP)) {
                Mode mode = (Mode) evt.getNewValue();
                switch(mode) {
                case EDITING_DATA: setEnabled(true);
                    break;
                case UPLOADING_AND_SAVING: setEnabled(false);
                    break;
                }
            }
        }
    }

    class SaveSessionAction extends SessionSaveAsAction {

        SaveSessionAction() {
            super(false, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                saveSession();
                setUserAction(UserAction.PROCEED);
                closeDialog();
            } catch (UserCancelException ignore) {
                Logging.trace(ignore);
            }
        }
    }

    final class SaveAndProceedAction extends AbstractAction implements PropertyChangeListener {
        private static final int ICON_SIZE = 24;
        private static final String BASE_ICON = "BASE_ICON";
        private final transient Image save = getImage("save", false);
        private final transient Image upld = getImage("upload", false);
        private final transient Image saveDis = getImage("save", true);
        private final transient Image upldDis = getImage("upload", true);

        SaveAndProceedAction() {
            initForReason(Reason.EXIT);
        }

        Image getImage(String name, boolean disabled) {
            ImageIcon img = new ImageProvider(name).setDisabled(disabled).get();
            return img != null ? img.getImage() : null;
        }

        public void initForReason(Reason reason) {
            switch (reason) {
                case EXIT:
                    putValue(NAME, tr("Perform actions before exiting"));
                    putValue(SHORT_DESCRIPTION, tr("Exit JOSM with saving. Unsaved changes are uploaded and/or saved."));
                    putValue(BASE_ICON, ImageProvider.get("exit"));
                    break;
                case RESTART:
                    putValue(NAME, tr("Perform actions before restarting"));
                    putValue(SHORT_DESCRIPTION, tr("Restart JOSM with saving. Unsaved changes are uploaded and/or saved."));
                    putValue(BASE_ICON, ImageProvider.get("restart"));
                    break;
                case DELETE:
                    putValue(NAME, tr("Perform actions before deleting"));
                    putValue(SHORT_DESCRIPTION, tr("Save/Upload layers before deleting. Unsaved changes are not lost."));
                    putValue(BASE_ICON, ImageProvider.get("dialogs", "delete"));
                    break;
            }
            redrawIcon();
        }

        public void redrawIcon() {
            Image base = ((ImageIcon) getValue(BASE_ICON)).getImage();
            BufferedImage newIco = new BufferedImage(ICON_SIZE*3, ICON_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = newIco.createGraphics();
            // CHECKSTYLE.OFF: SingleSpaceSeparator
            g.drawImage(model.getLayersToUpload().isEmpty() ? upldDis : upld, ICON_SIZE*0, 0, ICON_SIZE, ICON_SIZE, null);
            g.drawImage(model.getLayersToSave().isEmpty()   ? saveDis : save, ICON_SIZE*1, 0, ICON_SIZE, ICON_SIZE, null);
            g.drawImage(base,                                                 ICON_SIZE*2, 0, ICON_SIZE, ICON_SIZE, null);
            // CHECKSTYLE.ON: SingleSpaceSeparator
            putValue(SMALL_ICON, new ImageIcon(newIco));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!confirmSaveLayerInfosOK(model))
                return;
            launchSafeAndUploadTask();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(SaveLayersModel.MODE_PROP)) {
                SaveLayersModel.Mode mode = (SaveLayersModel.Mode) evt.getNewValue();
                switch(mode) {
                case EDITING_DATA: setEnabled(true);
                    break;
                case UPLOADING_AND_SAVING: setEnabled(false);
                    break;
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

        private final SaveLayersModel model;
        private final ProgressMonitor monitor;
        private final ExecutorService worker;
        private boolean canceled;
        private AbstractIOTask currentTask;

        public SaveAndUploadTask(SaveLayersModel model, ProgressMonitor monitor) {
            this.model = model;
            this.monitor = monitor;
            this.worker = Executors.newSingleThreadExecutor(Utils.newThreadFactory(getClass() + "-%d", Thread.NORM_PRIORITY));
        }

        protected void uploadLayers(List<SaveLayerInfo> toUpload) {
            for (final SaveLayerInfo layerInfo: toUpload) {
                AbstractModifiableLayer layer = layerInfo.getLayer();
                if (canceled) {
                    model.setUploadState(layer, UploadOrSaveState.CANCELED);
                    continue;
                }
                monitor.subTask(tr("Preparing layer ''{0}'' for upload ...", layerInfo.getName()));

                if (!UploadAction.checkPreUploadConditions(layer)) {
                    model.setUploadState(layer, UploadOrSaveState.FAILED);
                    continue;
                }

                AbstractUploadDialog dialog = layer.getUploadDialog();
                if (dialog != null) {
                    dialog.setVisible(true);
                    if (dialog.isCanceled()) {
                        model.setUploadState(layer, UploadOrSaveState.CANCELED);
                        continue;
                    }
                    dialog.rememberUserInput();
                }

                currentTask = layer.createUploadTask(monitor);
                if (currentTask == null) {
                    model.setUploadState(layer, UploadOrSaveState.FAILED);
                    continue;
                }
                Future<?> currentFuture = worker.submit(currentTask);
                try {
                    // wait for the asynchronous task to complete
                    currentFuture.get();
                } catch (CancellationException e) {
                    Logging.trace(e);
                    model.setUploadState(layer, UploadOrSaveState.CANCELED);
                } catch (InterruptedException | ExecutionException e) {
                    Logging.error(e);
                    model.setUploadState(layer, UploadOrSaveState.FAILED);
                    ExceptionDialogUtil.explainException(e);
                }
                if (currentTask.isCanceled()) {
                    model.setUploadState(layer, UploadOrSaveState.CANCELED);
                } else if (currentTask.isFailed()) {
                    Logging.error(currentTask.getLastException());
                    ExceptionDialogUtil.explainException(currentTask.getLastException());
                    model.setUploadState(layer, UploadOrSaveState.FAILED);
                } else {
                    model.setUploadState(layer, UploadOrSaveState.OK);
                }
                currentTask = null;
            }
        }

        protected void saveLayers(List<SaveLayerInfo> toSave) {
            for (final SaveLayerInfo layerInfo: toSave) {
                if (canceled) {
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.CANCELED);
                    continue;
                }
                // Check save preconditions earlier to avoid a blocking reentring call to EDT (see #10086)
                if (layerInfo.isDoCheckSaveConditions()) {
                    if (!layerInfo.getLayer().checkSaveConditions()) {
                        continue;
                    }
                    layerInfo.setDoCheckSaveConditions(false);
                }
                currentTask = new SaveLayerTask(layerInfo, monitor);
                Future<?> currentFuture = worker.submit(currentTask);

                try {
                    // wait for the asynchronous task to complete
                    //
                    currentFuture.get();
                } catch (CancellationException e) {
                    Logging.trace(e);
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.CANCELED);
                } catch (InterruptedException | ExecutionException e) {
                    Logging.error(e);
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.FAILED);
                    ExceptionDialogUtil.explainException(e);
                }
                if (currentTask.isCanceled()) {
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.CANCELED);
                } else if (currentTask.isFailed()) {
                    if (currentTask.getLastException() != null) {
                        Logging.error(currentTask.getLastException());
                        ExceptionDialogUtil.explainException(currentTask.getLastException());
                    }
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.FAILED);
                } else {
                    model.setSaveState(layerInfo.getLayer(), UploadOrSaveState.OK);
                }
                this.currentTask = null;
            }
        }

        protected void warnBecauseOfUnsavedData() {
            int numProblems = model.getNumCancel() + model.getNumFailed();
            if (numProblems == 0)
                return;
            Logging.warn(numProblems + " problems occured during upload/save");
            String msg = trn(
                    "<html>An upload and/or save operation of one layer with modifications<br>"
                    + "was canceled or has failed.</html>",
                    "<html>Upload and/or save operations of {0} layers with modifications<br>"
                    + "were canceled or have failed.</html>",
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

        @Override
        public void run() {
            GuiHelper.runInEDTAndWait(() -> {
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
                    if (canceled) {
                        setUserAction(UserAction.CANCEL);
                        closeDialog();
                    }
                } else {
                    setUserAction(UserAction.PROCEED);
                    closeDialog();
                }
            });
            worker.shutdownNow();
        }

        public void cancel() {
            if (currentTask != null) {
                currentTask.cancel();
            }
            worker.shutdown();
            canceled = true;
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        boolean dis = model.getLayersToSave().isEmpty() && model.getLayersToUpload().isEmpty();
        if (saveAndProceedActionButton != null) {
            saveAndProceedActionButton.setEnabled(!dis);
        }
        saveAndProceedAction.redrawIcon();
    }
}
