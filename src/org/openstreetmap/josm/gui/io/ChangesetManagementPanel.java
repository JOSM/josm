// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * ChangesetManagementPanel allows to configure changeset to be used in the next
 * upload.
 *
 * It is displayed as one of the configuration panels in the {@link UploadDialog}.
 *
 * ChangesetManagementPanel is a source for {@link java.beans.PropertyChangeEvent}s. Clients can listen
 * to
 * <ul>
 *   <li>{@link #SELECTED_CHANGESET_PROP}  - the new value in the property change event is
 *   the changeset selected by the user. The value is null if the user didn't select a
 *   a changeset or if he chosed to use a new changeset.</li>
 *   <li> {@link #CLOSE_CHANGESET_AFTER_UPLOAD} - the new value is a boolean value indicating
 *   whether the changeset should be closed after the next upload</li>
 * </ul>
 */
public class ChangesetManagementPanel extends JPanel implements ListDataListener {
    static final String SELECTED_CHANGESET_PROP = ChangesetManagementPanel.class.getName() + ".selectedChangeset";
    static final String CLOSE_CHANGESET_AFTER_UPLOAD = ChangesetManagementPanel.class.getName() + ".closeChangesetAfterUpload";

    private JRadioButton rbUseNew;
    private JRadioButton rbExisting;
    private JosmComboBox<Changeset> cbOpenChangesets;
    private JCheckBox cbCloseAfterUpload;
    private OpenChangesetComboBoxModel model;

    /**
     * Constructs a new {@code ChangesetManagementPanel}.
     *
     * @param changesetCommentModel the changeset comment model. Must not be null.
     * @throws IllegalArgumentException if {@code changesetCommentModel} is null
     */
    public ChangesetManagementPanel(ChangesetCommentModel changesetCommentModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        build();
        refreshGUI();
    }

    /**
     * builds the GUI
     */
    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        ButtonGroup bgUseNewOrExisting = new ButtonGroup();

        gc.gridwidth = 4;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0, 0, 5, 0);
        add(new JMultilineLabel(
                tr("Please decide what changeset the data is uploaded to and whether to close the changeset after the next upload.")), gc);

        gc.gridwidth = 4;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        rbUseNew = new JRadioButton(tr("Upload to a new changeset"));
        rbUseNew.setToolTipText(tr("Open a new changeset and use it in the next upload"));
        bgUseNewOrExisting.add(rbUseNew);
        add(rbUseNew, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        rbExisting = new JRadioButton(tr("Upload to an existing changeset"));
        rbExisting.setToolTipText(tr("Upload data to an already existing and open changeset"));
        bgUseNewOrExisting.add(rbExisting);
        add(rbExisting, gc);

        gc.gridx = 1;
        gc.gridy = 2;
        gc.gridwidth = 1;
        gc.weightx = 1.0;
        model = new OpenChangesetComboBoxModel();
        ChangesetCache.getInstance().addChangesetCacheListener(model);
        cbOpenChangesets = new JosmComboBox<>(model);
        cbOpenChangesets.setToolTipText(tr("Select an open changeset"));
        cbOpenChangesets.setRenderer(new ChangesetCellRenderer());
        cbOpenChangesets.addItemListener(new ChangesetListItemStateListener());
        Dimension d = cbOpenChangesets.getPreferredSize();
        d.width = 200;
        cbOpenChangesets.setPreferredSize(d);
        d.width = 100;
        cbOpenChangesets.setMinimumSize(d);
        model.addListDataListener(this);
        add(cbOpenChangesets, gc);

        gc.gridx = 2;
        gc.gridy = 2;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        JButton btnRefresh = new JButton(new RefreshAction());
        btnRefresh.setMargin(new Insets(0, 0, 0, 0));
        add(btnRefresh, gc);

        gc.gridx = 3;
        gc.gridy = 2;
        gc.gridwidth = 1;
        CloseChangesetAction closeChangesetAction = new CloseChangesetAction();
        JButton btnClose = new JButton(closeChangesetAction);
        btnClose.setMargin(new Insets(0, 0, 0, 0));
        cbOpenChangesets.addItemListener(closeChangesetAction);
        rbExisting.addItemListener(closeChangesetAction);
        add(btnClose, gc);

        gc.gridx = 0;
        gc.gridy = 3;
        gc.gridwidth = 4;
        gc.weightx = 1.0;
        cbCloseAfterUpload = new JCheckBox(tr("Close changeset after upload"));
        cbCloseAfterUpload.setToolTipText(tr("Select to close the changeset after the next upload"));
        add(cbCloseAfterUpload, gc);
        cbCloseAfterUpload.setSelected(Config.getPref().getBoolean("upload.changeset.close", true));
        cbCloseAfterUpload.addItemListener(new CloseAfterUploadItemStateListener());

        gc.gridx = 0;
        gc.gridy = 5;
        gc.gridwidth = 4;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gc);

        rbUseNew.getModel().addItemListener(new RadioButtonHandler());
        rbExisting.getModel().addItemListener(new RadioButtonHandler());
    }

    protected void refreshGUI() {
        rbExisting.setEnabled(model.getSize() > 0);
        if (model.getSize() == 0 && !rbUseNew.isSelected()) {
            rbUseNew.setSelected(true);
        }
        cbOpenChangesets.setEnabled(model.getSize() > 0 && rbExisting.isSelected());
    }

    /**
     * Sets the changeset to be used in the next upload
     *
     * @param cs the changeset
     */
    public void setSelectedChangesetForNextUpload(Changeset cs) {
        int idx = model.getIndexOf(cs);
        if (idx >= 0) {
            rbExisting.setSelected(true);
            model.setSelectedItem(cs);
        }
    }

    /**
     * Replies the currently selected changeset. null, if no changeset is
     * selected or if the user has chosen to use a new changeset.
     *
     * @return the currently selected changeset. null, if no changeset is
     * selected.
     */
    public Changeset getSelectedChangeset() {
        if (rbUseNew.isSelected())
            return null;
        return (Changeset) cbOpenChangesets.getSelectedItem();
    }

    /**
     * Determines if the user has chosen to close the changeset after the next upload.
     * @return {@code true} if the user has chosen to close the changeset after the next upload
     */
    public boolean isCloseChangesetAfterUpload() {
        return cbCloseAfterUpload.isSelected();
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface ListDataListener                                                   */
    /* ---------------------------------------------------------------------------- */
    @Override
    public void contentsChanged(ListDataEvent e) {
        refreshGUI();
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        refreshGUI();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        refreshGUI();
    }

    /**
     * Listens to changes in the selected changeset and fires property change events.
     */
    class ChangesetListItemStateListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            Changeset cs = (Changeset) cbOpenChangesets.getSelectedItem();
            if (cs == null) return;
            if (rbExisting.isSelected()) {
                firePropertyChange(SELECTED_CHANGESET_PROP, null, cs);
            }
        }
    }

    /**
     * Listens to changes in "close after upload" flag and fires property change events.
     */
    class CloseAfterUploadItemStateListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getItemSelectable() != cbCloseAfterUpload)
                return;
            switch(e.getStateChange()) {
            case ItemEvent.SELECTED:
                firePropertyChange(CLOSE_CHANGESET_AFTER_UPLOAD, false, true);
                Config.getPref().putBoolean("upload.changeset.close", true);
                break;
            case ItemEvent.DESELECTED:
                firePropertyChange(CLOSE_CHANGESET_AFTER_UPLOAD, true, false);
                Config.getPref().putBoolean("upload.changeset.close", false);
                break;
            default: // Do nothing
            }
        }
    }

    /**
     * Listens to changes in the two radio buttons rbUseNew and rbUseExisting.
     */
    class RadioButtonHandler implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (rbUseNew.isSelected()) {
                cbOpenChangesets.setEnabled(false);
                firePropertyChange(SELECTED_CHANGESET_PROP, null, null);
            } else if (rbExisting.isSelected()) {
                cbOpenChangesets.setEnabled(true);
                if (cbOpenChangesets.getSelectedItem() == null) {
                    model.selectFirstChangeset();
                }
                Changeset cs = (Changeset) cbOpenChangesets.getSelectedItem();
                if (cs == null) return;
                firePropertyChange(SELECTED_CHANGESET_PROP, null, cs);
            }
        }
    }

    /**
     * Refreshes the list of open changesets
     *
     */
    class RefreshAction extends AbstractAction {
        RefreshAction() {
            putValue(SHORT_DESCRIPTION, tr("Load the list of your open changesets from the server"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MainApplication.worker.submit(new DownloadOpenChangesetsTask(ChangesetManagementPanel.this));
        }
    }

    /**
     * Closes the currently selected changeset
     *
     */
    class CloseChangesetAction extends AbstractAction implements ItemListener {
        CloseChangesetAction() {
            putValue(SMALL_ICON, ImageProvider.get("closechangeset"));
            putValue(SHORT_DESCRIPTION, tr("Close the currently selected open changeset"));
            refreshEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Changeset cs = (Changeset) cbOpenChangesets.getSelectedItem();
            if (cs == null) return;
            MainApplication.worker.submit(new CloseChangesetTask(Collections.singletonList(cs)));
        }

        protected void refreshEnabledState() {
            setEnabled(
                    cbOpenChangesets.getModel().getSize() > 0
                    && cbOpenChangesets.getSelectedItem() != null
                    && rbExisting.isSelected()
            );
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            refreshEnabledState();
        }
    }
}
