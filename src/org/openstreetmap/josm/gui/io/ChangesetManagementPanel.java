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
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetCacheEvent;
import org.openstreetmap.josm.data.osm.ChangesetCacheListener;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmComboBoxModel;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * ChangesetManagementPanel allows to configure changeset to be used in the next upload.
 *
 * It is displayed as one of the configuration panels in the {@link UploadDialog}.
 *
 * ChangesetManagementPanel is a source for {@link java.beans.PropertyChangeEvent}s. Clients can listen to
 * <ul>
 *   <li>{@link #SELECTED_CHANGESET_PROP}  - the new value in the property change event is
 *   the changeset selected by the user. The value is null if the user didn't select a
 *   a changeset or if he chose to use a new changeset.</li>
 *   <li> {@link #CLOSE_CHANGESET_AFTER_UPLOAD} - the new value is a boolean value indicating
 *   whether the changeset should be closed after the next upload</li>
 * </ul>
 */
public class ChangesetManagementPanel extends JPanel implements ItemListener, ChangesetCacheListener {
    static final String SELECTED_CHANGESET_PROP = ChangesetManagementPanel.class.getName() + ".selectedChangeset";
    static final String CLOSE_CHANGESET_AFTER_UPLOAD = ChangesetManagementPanel.class.getName() + ".closeChangesetAfterUpload";

    private JosmComboBox<Changeset> cbOpenChangesets;
    private JosmComboBoxModel<Changeset> model;
    private JCheckBox cbCloseAfterUpload;

    /**
     * Constructs a new {@code ChangesetManagementPanel}.
     *
     * @since 18283 (signature)
     */
    public ChangesetManagementPanel() {
        build();
    }

    /**
     * Initializes this life cycle of the panel.
     *
     * @since 18283
     */
    public void initLifeCycle() {
        refreshChangesets();
    }

    /**
     * Returns the model in use.
     * @return the model
     */
    public JosmComboBoxModel<Changeset> getModel() {
        return model;
    }

    /**
     * builds the GUI
     */
    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(tr("Please select a changeset:")));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(3, 3, 3, 3);

        gc.gridwidth = 3;
        add(new JMultilineLabel(tr(
            "Please select which changeset the data shall be uploaded to and whether to close that changeset after the next upload."
            )), gc);

        gc.gridwidth = 1;
        gc.gridy++;
        model = new JosmComboBoxModel<>();
        cbOpenChangesets = new JosmComboBox<>(model);
        cbOpenChangesets.setToolTipText(tr("Select a changeset"));
        cbOpenChangesets.setRenderer(new ChangesetCellRenderer());
        Dimension d = cbOpenChangesets.getPreferredSize();
        d.width = 200;
        cbOpenChangesets.setPreferredSize(d);
        d.width = 100;
        cbOpenChangesets.setMinimumSize(d);
        add(cbOpenChangesets, gc);
        int h = cbOpenChangesets.getPreferredSize().height;
        Dimension prefSize = new Dimension(h, h);

        gc.gridx++;
        gc.weightx = 0.0;
        JButton btnRefresh = new JButton(new RefreshAction());
        btnRefresh.setPreferredSize(prefSize);
        btnRefresh.setMinimumSize(prefSize);
        add(btnRefresh, gc);

        gc.gridx++;
        CloseChangesetAction closeChangesetAction = new CloseChangesetAction();
        JButton btnClose = new JButton(closeChangesetAction);
        btnClose.setPreferredSize(prefSize);
        btnClose.setMinimumSize(prefSize);
        add(btnClose, gc);

        gc.gridy++;
        gc.gridx = 0;
        gc.gridwidth = 3;
        gc.weightx = 1.0;
        cbCloseAfterUpload = new JCheckBox(tr("Close changeset after upload"));
        cbCloseAfterUpload.setToolTipText(tr("Select to close the changeset after the next upload"));
        add(cbCloseAfterUpload, gc);

        cbOpenChangesets.addItemListener(this);
        cbOpenChangesets.addItemListener(closeChangesetAction);

        cbCloseAfterUpload.setSelected(Config.getPref().getBoolean("upload.changeset.close", true));
        cbCloseAfterUpload.addItemListener(new CloseAfterUploadItemStateListener());

        ChangesetCache.getInstance().addChangesetCacheListener(this);
    }

    /**
     * Sets the changeset to be used in the next upload
     * <p>
     * Note: The changeset may be a new changeset that was automatically opened because the old
     * changeset overflowed.  In that case it was already added to the changeset cache and the
     * combobox.
     *
     * @param cs the changeset
     * @see UploadPrimitivesTask#handleChangesetFullResponse
     */
    public void setSelectedChangesetForNextUpload(Changeset cs) {
        model.setSelectedItem(cs);
    }

    /**
     * Returns the currently selected changeset or an empty new one.
     *
     * @return the currently selected changeset
     */
    public Changeset getSelectedChangeset() {
        return Optional.ofNullable((Changeset) model.getSelectedItem()).orElse(new Changeset());
    }

    /**
     * Determines if the user has chosen to close the changeset after the next upload.
     * @return {@code true} if the user has chosen to close the changeset after the next upload
     */
    public boolean isCloseChangesetAfterUpload() {
        return cbCloseAfterUpload.isSelected();
    }

    /**
     * Listens to changes in the selected changeset and fires property change events.
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        firePropertyChange(SELECTED_CHANGESET_PROP, null, model.getSelectedItem());
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
     * Refreshes the list of open changesets
     */
    class RefreshAction extends AbstractAction {
        RefreshAction() {
            putValue(SHORT_DESCRIPTION, tr("Load the list of your open changesets from the server"));
            new ImageProvider("dialogs", "refresh").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MainApplication.worker.submit(new DownloadOpenChangesetsTask(ChangesetManagementPanel.this));
        }
    }

    /**
     * Closes the currently selected changeset
     */
    class CloseChangesetAction extends AbstractAction implements ItemListener {
        CloseChangesetAction() {
            new ImageProvider("closechangeset").getResource().attachImageIcon(this, true);
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
            setEnabled(!getSelectedChangeset().isNew());
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            refreshEnabledState();
        }
    }

    /**
     * Refreshes the changesets combobox form the server.
     * <p>
     * Note: This calls into {@link #refreshCombo} through {@link #changesetCacheUpdated}
     *
     * @see ChangesetCache#refreshChangesetsFromServer
     */
    protected void refreshChangesets() {
        try {
            ChangesetCache.getInstance().refreshChangesetsFromServer();
        } catch (OsmTransferException e) {
            return;
        }
    }

    private void refreshCombo() {
        Changeset selected = (Changeset) cbOpenChangesets.getSelectedItem();
        model.removeAllElements();
        model.addElement(new Changeset());
        model.addAllElements(ChangesetCache.getInstance().getOpenChangesetsForCurrentUser());
        cbOpenChangesets.setSelectedItem(selected != null && model.getIndexOf(selected) != -1 ? selected : model.getElementAt(0));
    }

    @Override
    public void changesetCacheUpdated(ChangesetCacheEvent event) {
        // This listener might have been called by a background task.
        SwingUtilities.invokeLater(this::refreshCombo);
    }
}
