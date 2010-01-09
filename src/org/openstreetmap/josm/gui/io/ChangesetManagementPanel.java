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
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * ChangesetManagementPanel allows to configure changeset to be used in the next
 * upload.
 *
 * It is displayed as one of the configuration panels in the {@see UploadDialog}.
 *
 * ChangesetManagementPanel is a source for {@see PropertyChangeEvent}s. Clients can listen
 * to
 * <ul>
 *   <li>{@see #SELECTED_CHANGESET_PROP}  - the new value in the property change event is
 *   the changeset selected by the user. The value is null if the user didn't select a
 *   a changeset or if he chosed to use a new changeset.</li>
 *   <li> {@see #CLOSE_CHANGESET_AFTER_UPLOAD} - the new value is a boolean value indicating
 *   whether the changeset should be closed after the next upload</li>
 * </ul>
 */
public class ChangesetManagementPanel extends JPanel implements ListDataListener {
    public final static String SELECTED_CHANGESET_PROP = ChangesetManagementPanel.class.getName() + ".selectedChangeset";
    public final static String CLOSE_CHANGESET_AFTER_UPLOAD = ChangesetManagementPanel.class.getName() + ".closeChangesetAfterUpload";

    private ButtonGroup bgUseNewOrExisting;
    private JRadioButton rbUseNew;
    private JRadioButton rbExisting;
    private JComboBox cbOpenChangesets;
    private JButton btnRefresh;
    private JButton btnClose;
    private JCheckBox cbCloseAfterUpload;
    private OpenChangesetComboBoxModel model;

    /**
     * builds the GUI
     */
    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

        bgUseNewOrExisting = new ButtonGroup();

        gc.gridwidth = 4;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0, 0, 5, 0);
        add(new JMultilineLabel(tr("Please decide what changeset the data is uploaded to and whether to close the changeset after the next upload.")), gc);

        gc.gridwidth = 4;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,0,0,0);
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
        cbOpenChangesets = new JComboBox(model);
        cbOpenChangesets.setToolTipText("Select an open changeset");
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
        btnRefresh = new JButton(new RefreshAction());
        btnRefresh.setMargin(new Insets(0,0,0,0));
        add(btnRefresh, gc);

        gc.gridx = 3;
        gc.gridy = 2;
        gc.gridwidth = 1;
        CloseChangesetAction closeChangesetAction = new CloseChangesetAction();
        btnClose = new JButton(closeChangesetAction);
        btnClose.setMargin(new Insets(0,0,0,0));
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
        cbCloseAfterUpload.setSelected(Main.pref.getBoolean("upload.changeset.close", true));
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

    public ChangesetManagementPanel() {
        build();
        refreshGUI();
    }

    protected void refreshGUI() {
        rbExisting.setEnabled(model.getSize() > 0);
        if (model.getSize() == 0) {
            if (!rbUseNew.isSelected()) {
                rbUseNew.setSelected(true);
            }
        }
        cbOpenChangesets.setEnabled(model.getSize() > 0 && rbExisting.isSelected());
    }

    public void setSelectedChangesetForNextUpload(Changeset cs) {
        int idx  = model.getIndexOf(cs);
        if (idx >=0) {
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
        return (Changeset)cbOpenChangesets.getSelectedItem();
    }

    /**
     * Replies true if the user has chosen to close the changeset after the
     * next upload
     *
     */
    public boolean isCloseChangesetAfterUpload() {
        return cbCloseAfterUpload.isSelected();
    }

    /**
     * Replies the default value for "created_by"
     *
     * @return the default value for "created_by"
     */
    protected String getDefaultCreatedBy() {
        Object ua = System.getProperties().get("http.agent");
        return(ua == null) ? "JOSM" : ua.toString();
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface ListDataListener                                                   */
    /* ---------------------------------------------------------------------------- */
    public void contentsChanged(ListDataEvent e) {
        refreshGUI();
    }

    public void intervalAdded(ListDataEvent e) {
        refreshGUI();
    }

    public void intervalRemoved(ListDataEvent e) {
        refreshGUI();
    }

    /**
     * Listens to changes in the selected changeset and accordingly fires property
     * change events.
     *
     */
    class ChangesetListItemStateListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            Changeset cs = (Changeset)cbOpenChangesets.getSelectedItem();
            if (rbExisting.isSelected()) {
                firePropertyChange(SELECTED_CHANGESET_PROP, null, cs);
                if (cs == null) {
                    rbUseNew.setSelected(true);
                }
            }
        }
    }

    /**
     * Listens to changes in "close after upload" flag and fires
     * property change events.
     *
     */
    class CloseAfterUploadItemStateListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getItemSelectable() != cbCloseAfterUpload)
                return;
            switch(e.getStateChange()) {
            case ItemEvent.SELECTED:
                firePropertyChange(CLOSE_CHANGESET_AFTER_UPLOAD, false, true);
                Main.pref.put("upload.changeset.close", true);
                break;
            case ItemEvent.DESELECTED:
                firePropertyChange(CLOSE_CHANGESET_AFTER_UPLOAD, true, false);
                Main.pref.put("upload.changeset.close", false);
                break;
            }
        }
    }

    /**
     * Listens to changes in the two radio buttons rbUseNew and rbUseExisting.
     *
     */
    class RadioButtonHandler implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (rbUseNew.isSelected()) {
                cbOpenChangesets.setEnabled(false);
                firePropertyChange(SELECTED_CHANGESET_PROP, null, null);
            } else {
                cbOpenChangesets.setEnabled(true);
                if (cbOpenChangesets.getSelectedItem() == null) {
                    model.selectFirstChangeset();
                }
                Changeset cs = (Changeset)cbOpenChangesets.getSelectedItem();
                firePropertyChange(SELECTED_CHANGESET_PROP, null, cs);
            }
        }
    }

    /**
     * Refreshes the list of open changesets
     *
     */
    class RefreshAction extends AbstractAction {
        public RefreshAction() {
            //putValue(NAME, tr("Reload"));
            putValue(SHORT_DESCRIPTION, tr("Load the list of your open changesets from the server"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
        }

        public void actionPerformed(ActionEvent e) {
            DownloadOpenChangesetsTask task = new DownloadOpenChangesetsTask(ChangesetManagementPanel.this);
            Main.worker.submit(task);
        }
    }

    /**
     * Closes the currently selected changeset
     *
     */
    class CloseChangesetAction extends AbstractAction implements ItemListener{
        public CloseChangesetAction() {
            //putValue(NAME, tr("Close"));
            putValue(SMALL_ICON, ImageProvider.get("closechangeset"));
            putValue(SHORT_DESCRIPTION, tr("Close the currently selected open changeset"));
            refreshEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            Changeset cs = (Changeset)cbOpenChangesets.getSelectedItem();
            if (cs == null) return;
            CloseChangesetTask task = new CloseChangesetTask(Collections.singletonList(cs));
            Main.worker.submit(task);
        }

        protected void refreshEnabledState() {
            setEnabled(
                    cbOpenChangesets.getModel().getSize() > 0
                    && cbOpenChangesets.getSelectedItem() != null
                    && rbExisting.isSelected()
            );
        }

        public void itemStateChanged(ItemEvent e) {
            refreshEnabledState();
        }
    }
}
