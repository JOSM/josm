// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This is a dialog for entering upload options like the parameters for
 * the upload changeset and the strategy for opening/closing a changeset.
 *
 */
public class UploadDialog extends JDialog {

    public static final String HISTORY_KEY = "upload.comment.history";

    /**  the unique instance of the upload dialog */
    static private UploadDialog uploadDialog;

    /**
     * Replies the unique instance of the upload dialog
     *
     * @return the unique instance of the upload dialog
     */
    static public UploadDialog getUploadDialog() {
        if (uploadDialog == null) {
            uploadDialog = new UploadDialog();
        }
        return uploadDialog;
    }

    /** the list with the added primitives */
    private PrimitiveList lstAdd;
    private JLabel lblAdd;
    private JScrollPane spAdd;
    /** the list with the updated primitives */
    private PrimitiveList lstUpdate;
    private JLabel lblUpdate;
    private JScrollPane spUpdate;
    /** the list with the deleted primitives */
    private PrimitiveList lstDelete;
    private JLabel lblDelete;
    private JScrollPane spDelete;
    /** the panel containing the widgets for the lists of primitives */
    private JPanel pnlLists;
    /** checkbox for selecting whether an atomic upload is to be used  */
    private TagEditorPanel tagEditorPanel;
    /** the tabbed pane used below of the list of primitives  */
    private JTabbedPane southTabbedPane;
    /** the upload button */
    private JButton btnUpload;

    private ChangesetSelectionPanel pnlChangesetSelection;
    private boolean canceled = false;

    /**
     * builds the panel with the lists of primitives
     *
     * @return the panel with the lists of primitives
     */
    protected JPanel buildListsPanel() {
        pnlLists = new JPanel();
        pnlLists.setLayout(new GridBagLayout());
        // we don't add the lists yet, see setUploadPrimitives()
        //
        return pnlLists;
    }

    /**
     * builds the content panel for the upload dialog
     *
     * @return the content panel
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnl.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // first the panel with the list in the upper half
        //
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(buildListsPanel(), gc);

        // a tabbed pane with two configuration panels in the
        // lower half
        //
        southTabbedPane = new JTabbedPane();
        southTabbedPane.add(new JPanel());
        tagEditorPanel = new TagEditorPanel();
        southTabbedPane.add(tagEditorPanel);
        southTabbedPane.setComponentAt(0, pnlChangesetSelection = new ChangesetSelectionPanel());
        southTabbedPane.setTitleAt(0, tr("Settings"));
        southTabbedPane.setToolTipTextAt(0, tr("Decide how to upload the data and which changeset to use"));
        southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));
        southTabbedPane.setToolTipTextAt(1, tr("Apply tags to the changeset data is uploaded to"));
        southTabbedPane.addChangeListener(new TabbedPaneChangeLister());
        JPanel pnl1 = new JPanel();
        pnl1.setLayout(new BorderLayout());
        pnl1.add(southTabbedPane,BorderLayout.CENTER);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 1;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(pnl1, gc);
        return pnl;
    }

    /**
     * builds the panel with the OK and CANCEL buttons
     *
     * @return
     */
    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // -- upload button
        UploadAction uploadAction = new UploadAction();
        pnl.add(btnUpload = new SideButton(uploadAction));
        btnUpload.setFocusable(true);
        InputMap inputMap = btnUpload.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "doUpload");
        btnUpload.getActionMap().put("doUpload", uploadAction);

        // -- cancel button
        CancelAction cancelAction = new CancelAction();
        pnl.add(new SideButton(cancelAction));
        getRootPane().registerKeyboardAction(
                cancelAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        pnl.add(new SideButton(new ContextSensitiveHelpAction(ht("/Dialogs/UploadDialog"))));
        HelpUtil.setHelpContext(getRootPane(),ht("/Dialogs/UploadDialog"));
        return pnl;
    }

    /**
     * builds the gui
     */
    protected void build() {
        setTitle(tr("Upload"));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildContentPanel(), BorderLayout.CENTER);
        getContentPane().add(buildActionPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHandler());
    }

    /**
     * constructor
     */
    public UploadDialog() {
        super(JOptionPane.getFrameForComponent(Main.parent), true /* modal */);
        OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();

        // initialize the three lists for primitives
        //
        lstAdd = new PrimitiveList();
        lstAdd.setCellRenderer(renderer);
        lstAdd.setVisibleRowCount(Math.min(lstAdd.getModel().getSize(), 10));
        spAdd = new JScrollPane(lstAdd);
        lblAdd = new JLabel(tr("Objects to add:"));

        lstUpdate = new PrimitiveList();
        lstUpdate.setCellRenderer(renderer);
        lstUpdate.setVisibleRowCount(Math.min(lstUpdate.getModel().getSize(), 10));
        spUpdate = new JScrollPane(lstUpdate);
        lblUpdate = new JLabel(tr("Objects to modify:"));

        lstDelete = new PrimitiveList();
        lstDelete.setCellRenderer(renderer);
        lstDelete.setVisibleRowCount(Math.min(lstDelete.getModel().getSize(), 10));
        spDelete = new JScrollPane(lstDelete);
        lblDelete = new JLabel(tr("Objects to delete:"));

        // build the GUI
        //
        build();
    }

    /**
     * sets the collection of primitives which will be uploaded
     *
     * @param add  the collection of primitives to add
     * @param update the collection of primitives to update
     * @param delete the collection of primitives to delete
     */
    public void setUploadedPrimitives(List<OsmPrimitive> add, List<OsmPrimitive> update, List<OsmPrimitive> delete) {
        lstAdd.getPrimitiveListModel().setPrimitives(add);
        lstUpdate.getPrimitiveListModel().setPrimitives(update);
        lstDelete.getPrimitiveListModel().setPrimitives(delete);

        GridBagConstraints gcLabel = new GridBagConstraints();
        gcLabel.fill = GridBagConstraints.HORIZONTAL;
        gcLabel.weightx = 1.0;
        gcLabel.weighty = 0.0;
        gcLabel.anchor = GridBagConstraints.FIRST_LINE_START;

        GridBagConstraints gcList = new GridBagConstraints();
        gcList.fill = GridBagConstraints.BOTH;
        gcList.weightx = 1.0;
        gcList.weighty = 1.0;
        gcList.anchor = GridBagConstraints.CENTER;
        pnlLists.removeAll();
        int y = -1;
        if (!add.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblAdd.setText(trn("{0} object to add:", "{0} objects to add:", add.size(),add.size()));
            pnlLists.add(lblAdd, gcLabel);
            y++;
            gcList.gridy = y;
            pnlLists.add(spAdd, gcList);
        }
        if (!update.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblUpdate.setText(trn("{0} object to modify:", "{0} objects to modify:", update.size(),update.size()));
            pnlLists.add(lblUpdate, gcLabel);
            y++;
            gcList.gridy = y;
            pnlLists.add(spUpdate, gcList);
        }
        if (!delete.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblDelete.setText(trn("{0} object to delete:", "{0} objects to delete:", delete.size(),delete.size()));
            pnlLists.add(lblDelete, gcLabel);
            y++;
            gcList.gridy = y;
            pnlLists.add(spDelete, gcList);
        }
        pnlChangesetSelection.setNumUploadedObjects(add.size() + update.size() + delete.size());
    }

    /**
     * Remembers the user input in the preference settings
     */
    public void rememberUserInput() {
        pnlChangesetSelection.rememberUserInput();
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        tagEditorPanel.initAutoCompletion(Main.main.getEditLayer());
        pnlChangesetSelection.startUserInput();
    }

    /**
     * Replies the current changeset
     *
     * @return the current changeset
     */
    public Changeset getChangeset() {
        Changeset cs = pnlChangesetSelection.getChangeset();
        tagEditorPanel.getModel().applyToPrimitive(cs);
        cs.put("comment", getUploadComment());
        return cs;
    }

    /**
     * Replies the {@see UploadStrategySpecification} the user entered in the dialog.
     * 
     * @return the {@see UploadStrategySpecification} the user entered in the dialog.
     */
    public UploadStrategySpecification getUploadStrategySpecification() {
        return pnlChangesetSelection.getUploadStrategySpecification();
    }

    /**
     * Sets or updates the changeset cs.
     * If cs is null, does nothing.
     * If cs.getId() == 0 does nothing.
     * If cs.getId() > 0 and cs is open, adds it to the list of open
     * changesets. If it is closed, removes it from the list of open
     * changesets.
     *
     * @param cs the changeset
     */
    public void setOrUpdateChangeset(Changeset cs) {
        pnlChangesetSelection.setOrUpdateChangeset(cs);
    }

    /**
     * Removes <code>cs</code> from the list of open changesets in the upload
     * dialog
     *
     * @param cs the changeset. Ignored if null.
     */
    public void removeChangeset(Changeset cs) {
        if (cs == null) return;
        pnlChangesetSelection.removeChangeset(cs);
    }

    /**
     * Replies true if the changeset is to be closed after the
     * next upload
     *
     * @return true if the changeset is to be closed after the
     * next upload; false, otherwise
     */
    public boolean isDoCloseAfterUpload() {
        return pnlChangesetSelection.isCloseAfterUpload();
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

    /**
     * Replies the current value for the upload comment
     *
     * @return the current value for the upload comment
     */
    protected String getUploadComment() {
        switch(southTabbedPane.getSelectedIndex()) {
        case 0:
            return pnlChangesetSelection.getUploadComment();
        case 1:
            TagModel tm = tagEditorPanel.getModel().get("comment");
            return tm == null? "" : tm.getValue();
        }
        return "";
    }

    /**
     * Replies true, if the dialog was canceled
     *
     * @return true, if the dialog was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets whether the dialog was canceld
     *
     * @param canceled true, if the dialog is canceled
     */
    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(400,600)
                    )
            ).apply(this);
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * This change listener is triggered when current tab in the tabbed pane in
     * the lower half of the dialog is changed.
     *
     * It's main purpose is to keep the content in the text field for the changeset
     * comment in sync with the changeset tag "comment".
     *
     */
    class TabbedPaneChangeLister implements ChangeListener {

        protected boolean hasCommentTag() {
            TagEditorModel model = tagEditorPanel.getModel();
            return model.get("comment") != null;
        }

        protected TagModel getEmptyTag() {
            TagEditorModel model = tagEditorPanel.getModel();
            TagModel tm = model.get("");
            if (tm != null) return tm;
            tm = new TagModel("", "");
            model.add(tm);
            return tm;
        }
        protected TagModel getOrCreateCommentTag() {
            TagEditorModel model = tagEditorPanel.getModel();
            if (hasCommentTag())
                return model.get("comment");
            TagModel tm = getEmptyTag();
            tm.setName("comment");
            return tm;
        }

        protected void removeCommentTag() {
            TagEditorModel model = tagEditorPanel.getModel();
            model.delete("comment");
        }

        protected void refreshCommentTag() {
            TagModel tm = getOrCreateCommentTag();
            tm.setName("comment");
            tm.setValue(pnlChangesetSelection.getUploadComment().trim());
            if (pnlChangesetSelection.getUploadComment().trim().equals("")) {
                removeCommentTag();
            }
            tagEditorPanel.getModel().fireTableDataChanged();
        }

        public void stateChanged(ChangeEvent e) {
            if (southTabbedPane.getSelectedIndex() ==0) {
                TagModel tm = tagEditorPanel.getModel().get("comment");
                pnlChangesetSelection.initEditingOfUploadComment(tm == null ? "" : tm.getValue());
            } else if (southTabbedPane.getSelectedIndex() == 1) {
                refreshCommentTag();
            }
        }
    }

    /**
     * Handles an upload
     *
     */
    class UploadAction extends AbstractAction {
        public UploadAction() {
            putValue(NAME, tr("Upload Changes"));
            putValue(SMALL_ICON, ImageProvider.get("upload"));
            putValue(SHORT_DESCRIPTION, tr("Upload the changed primitives"));
        }

        protected void warnIllegalUploadComment() {
            HelpAwareOptionPane.showOptionDialog(
                    UploadDialog.this,
                    tr("Please enter a comment for this upload changeset (min. 3 characters)"),
                    tr("Illegal upload comment"),
                    JOptionPane.ERROR_MESSAGE,
                    ht("/Dialog/UploadDialog#IllegalUploadComment")

            );
        }

        protected void warnIllegalChunkSize() {
            HelpAwareOptionPane.showOptionDialog(
                    UploadDialog.this,
                    tr("Please enter a valid chunk size first"),
                    tr("Illegal chunk size"),
                    JOptionPane.ERROR_MESSAGE,
                    ht("/Dialog/UploadDialog#IllegalChunkSize")
            );
        }


        public void actionPerformed(ActionEvent e) {
            if (getUploadComment().trim().length() < 3) {
                warnIllegalUploadComment();
                southTabbedPane.setSelectedIndex(0);
                pnlChangesetSelection.initEditingOfUploadComment(getUploadComment());
                return;
            }
            UploadStrategySpecification strategy = getUploadStrategySpecification();
            if (strategy.getStrategy().equals(UploadStrategy.CHUNKED_DATASET_STRATEGY)) {
                if (strategy.getChunkSize() == UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
                    warnIllegalChunkSize();
                    southTabbedPane.setSelectedIndex(0);
                    pnlChangesetSelection.initEditingOfChunkSize();
                    return;
                }
            }
            setCanceled(false);
            setVisible(false);
        }
    }

    /**
     * Action for canceling the dialog
     *
     */
    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Cancel the upload and resume editing"));
        }

        public void actionPerformed(ActionEvent e) {
            setCanceled(true);
            setVisible(false);
        }
    }

    /**
     * A simple list of OSM primitives.
     *
     */
    class PrimitiveList extends JList {
        public PrimitiveList() {
            super(new PrimitiveListModel());
        }

        public PrimitiveListModel getPrimitiveListModel() {
            return (PrimitiveListModel)getModel();
        }
    }

    /**
     * A list model for a list of OSM primitives.
     *
     */
    class PrimitiveListModel extends AbstractListModel{
        private List<OsmPrimitive> primitives;

        public PrimitiveListModel() {
            primitives = new ArrayList<OsmPrimitive>();
        }

        public PrimitiveListModel(List<OsmPrimitive> primitives) {
            setPrimitives(primitives);
        }

        public void setPrimitives(List<OsmPrimitive> primitives) {
            if (primitives == null) {
                this.primitives = new ArrayList<OsmPrimitive>();
            } else {
                this.primitives = primitives;
            }
            fireContentsChanged(this,0,getSize());
        }

        public Object getElementAt(int index) {
            if (primitives == null) return null;
            return primitives.get(index);
        }

        public int getSize() {
            if (primitives == null) return 0;
            return primitives.size();
        }
    }

    /**
     * Listens to window closing events and processes them as cancel events.
     * Listens to window open events and initializes user input
     *
     */
    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            setCanceled(true);
        }

        @Override
        public void windowOpened(WindowEvent e) {
            startUserInput();
        }
    }

    /**
     * The panel which provides various UI widgets for controlling how to use
     * changesets during upload.
     *
     */
    class ChangesetSelectionPanel extends JPanel implements ListDataListener{

        private ButtonGroup bgUseNewOrExisting;
        private JRadioButton rbUseNew;
        private JRadioButton rbExisting;
        private JComboBox cbOpenChangesets;
        private JButton btnRefresh;
        private JButton btnClose;
        private JCheckBox cbCloseAfterUpload;
        private OpenChangesetModel model;
        private HistoryComboBox cmt;
        private UploadStrategySelectionPanel pnlUploadStrategy;

        /**
         * build the panel with the widgets for controlling whether an atomic upload
         * should be used or not
         *
         * @return the panel
         */
        protected JPanel buildUploadStrategySelectionPanel() {
            pnlUploadStrategy = new UploadStrategySelectionPanel();
            pnlUploadStrategy.initFromPreferences();
            return pnlUploadStrategy;
        }

        protected JPanel buildUploadCommentPanel() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new GridBagLayout());
            pnl.add(new JLabel(tr("Provide a brief comment for the changes you are uploading:")), GBC.eol().insets(0, 5, 10, 3));
            cmt = new HistoryComboBox();
            cmt.setToolTipText(tr("Enter an upload comment (min. 3 characters)"));
            List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
            // we have to reverse the history, because ComboBoxHistory will reverse it again
            // in addElement()
            //
            Collections.reverse(cmtHistory);
            cmt.setPossibleItems(cmtHistory);
            cmt.getEditor().addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            TagModel tm = tagEditorPanel.getModel().get("comment");
                            if (tm == null) {
                                tagEditorPanel.getModel().add(new TagModel("comment", cmt.getText()));
                            } else {
                                tm.setValue(cmt.getText());
                            }
                            tagEditorPanel.getModel().fireTableDataChanged();
                        }
                    }
            );
            cmt.getEditor().addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            btnUpload.requestFocusInWindow();
                        }
                    }
            );
            pnl.add(cmt, GBC.eol().fill(GBC.HORIZONTAL));
            return pnl;
        }

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();

            bgUseNewOrExisting = new ButtonGroup();

            // -- atomic upload
            gc.gridwidth = 4;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.anchor = GridBagConstraints.FIRST_LINE_START;
            add(buildUploadStrategySelectionPanel(), gc);

            // -- changeset command
            gc.gridwidth = 4;
            gc.gridy = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.anchor = GridBagConstraints.FIRST_LINE_START;
            add(buildUploadCommentPanel(), gc);

            gc.gridwidth = 4;
            gc.gridy = 2;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.anchor = GridBagConstraints.FIRST_LINE_START;
            rbUseNew = new JRadioButton(tr("Open a new changeset"));
            rbUseNew.setToolTipText(tr("Open a new changeset and use it in the next upload"));
            bgUseNewOrExisting.add(rbUseNew);
            add(rbUseNew, gc);

            gc.gridx = 0;
            gc.gridy = 3;
            gc.gridwidth = 1;
            rbExisting = new JRadioButton(tr("Use an open changeset"));
            rbExisting.setToolTipText(tr("Upload data to an already opened changeset"));
            bgUseNewOrExisting.add(rbExisting);
            add(rbExisting, gc);

            gc.gridx = 1;
            gc.gridy = 3;
            gc.gridwidth = 1;
            gc.weightx = 1.0;
            model = new OpenChangesetModel();
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
            gc.gridy = 3;
            gc.gridwidth = 1;
            gc.weightx = 0.0;
            btnRefresh = new JButton(new RefreshAction());
            add(btnRefresh, gc);

            gc.gridx = 3;
            gc.gridy = 3;
            gc.gridwidth = 1;
            gc.weightx = 0.0;
            CloseChangesetAction closeChangesetAction = new CloseChangesetAction();
            btnClose = new JButton(closeChangesetAction);
            cbOpenChangesets.addItemListener(closeChangesetAction);
            add(btnClose, gc);

            gc.gridx = 0;
            gc.gridy = 4;
            gc.gridwidth = 4;
            cbCloseAfterUpload = new JCheckBox(tr("Close changeset after upload"));
            cbCloseAfterUpload.setToolTipText(tr("Select to close the changeset after the next upload"));
            add(cbCloseAfterUpload, gc);
            cbCloseAfterUpload.setSelected(true);

            rbUseNew.getModel().addItemListener(new RadioButtonHandler());
            rbExisting.getModel().addItemListener(new RadioButtonHandler());

            refreshGUI();
        }

        public ChangesetSelectionPanel() {
            build();
        }

        /**
         * Remembers the user input in the preference settings
         */
        public void rememberUserInput() {
            // store the history of comments
            cmt.addCurrentItemToHistory();
            Main.pref.putCollection(HISTORY_KEY, cmt.getHistory());
            pnlUploadStrategy.saveToPreferences();
        }

        /**
         * Initializes the panel for user input
         */
        public void startUserInput() {
            List<String> history = cmt.getHistory();
            if (history != null && !history.isEmpty()) {
                cmt.setText(history.get(0));
            }
            cmt.requestFocusInWindow();
            cmt.getEditor().getEditorComponent().requestFocusInWindow();
        }

        public void prepareDialogForNextUpload(Changeset cs) {
            if (cs == null || cs.getId() == 0) {
                rbUseNew.setSelected(true);
                cbCloseAfterUpload.setSelected(true);
            } if (cs.getId() == 0) {
                rbUseNew.setSelected(true);
                cbCloseAfterUpload.setSelected(true);
            } else if (cs.isOpen()) {
                rbExisting.setSelected(true);
                cbCloseAfterUpload.setSelected(false);
            } else {
                rbUseNew.setSelected(true);
                cbCloseAfterUpload.setSelected(true);
            }
        }

        /**
         * Replies the current upload comment
         *
         * @return
         */
        public String getUploadComment() {
            return cmt.getText();
        }

        /**
         * Replies the current upload comment
         *
         * @return
         */
        public void setUploadComment(String uploadComment) {
            cmt.setText(uploadComment);
        }

        public void initEditingOfUploadComment(String comment) {
            setUploadComment(comment);
            cmt.getEditor().selectAll();
            cmt.requestFocusInWindow();
        }

        public void initEditingOfChunkSize() {
            pnlUploadStrategy.initEditingOfChunkSize();
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

        public void contentsChanged(ListDataEvent e) {
            refreshGUI();
        }

        public void intervalAdded(ListDataEvent e) {
            refreshGUI();
        }

        public void intervalRemoved(ListDataEvent e) {
            refreshGUI();
        }

        public Changeset getChangeset() {
            if (rbUseNew.isSelected() || cbOpenChangesets.getSelectedItem() == null)
                return new Changeset();
            Changeset cs = (Changeset)cbOpenChangesets.getSelectedItem();
            if (cs == null)
                return new Changeset();
            return cs;
        }

        /**
         * Replies the {@see UploadStrategySpecification} the user entered in the dialog.
         * 
         * @return the {@see UploadStrategySpecification} the user entered in the dialog.
         */
        public UploadStrategySpecification getUploadStrategySpecification() {
            return pnlUploadStrategy.getUploadStrategySpecification();
        }

        public void setOrUpdateChangeset(Changeset cs) {
            if (cs == null) {
                cs = new Changeset();
                cs.put("created_by", getDefaultCreatedBy());
                tagEditorPanel.getModel().initFromPrimitive(cs);
                tagEditorPanel.getModel().appendNewTag();
                prepareDialogForNextUpload(cs);
            } else if (cs.getId() == 0) {
                if (cs.get("created_by") == null) {
                    cs.put("created_by", getDefaultCreatedBy());
                }
                tagEditorPanel.getModel().initFromPrimitive(cs);
                tagEditorPanel.getModel().appendNewTag();
                prepareDialogForNextUpload(cs);
            } else if (cs.getId() > 0 && cs.isOpen()){
                if (cs.get("created_by") == null) {
                    cs.put("created_by", getDefaultCreatedBy());
                }
                tagEditorPanel.getModel().initFromPrimitive(cs);
                model.addOrUpdate(cs);
                cs = model.getChangesetById(cs.getId());
                cbOpenChangesets.setSelectedItem(cs);
                prepareDialogForNextUpload(cs);
            } else if (cs.getId() > 0 && !cs.isOpen()){
                removeChangeset(cs);
            }
        }

        /**
         * Remove a changeset from the list of open changeset
         *
         * @param cs the changeset to be removed. Ignored if null.
         */
        public void removeChangeset(Changeset cs) {
            if (cs ==  null) return;
            Changeset selected = (Changeset)model.getSelectedItem();
            model.removeChangeset(cs);
            if (model.getSize() == 0 || selected == cs) {
                // no more changesets or removed changeset is the currently selected
                // changeset? Switch to using a new changeset.
                //
                rbUseNew.setSelected(true);
                model.setSelectedItem(null);
                southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));

                cs = new Changeset();
                if (cs.get("created_by") == null) {
                    cs.put("created_by", getDefaultCreatedBy());
                    cs.put("comment", getUploadComment());
                }
                tagEditorPanel.getModel().initFromPrimitive(cs);
            }
            prepareDialogForNextUpload(cs);
        }

        /**
         * Sets whether a new changeset is to be used
         *
         */
        public void setUseNewChangeset() {
            rbUseNew.setSelected(true);
        }

        /**
         * Sets whether an existing changeset is to be used
         */
        public void setUseExistingChangeset() {
            rbExisting.setSelected(true);
            if (cbOpenChangesets.getSelectedItem() == null && model.getSize() > 0) {
                cbOpenChangesets.setSelectedItem(model.getElementAt(0));
            }
        }

        /**
         * Replies true if the selected changeset should be closed after the
         * next upload
         *
         * @return true if the selected changeset should be closed after the
         * next upload
         */
        public boolean isCloseAfterUpload() {
            return cbCloseAfterUpload.isSelected();
        }

        public void setNumUploadedObjects(int numUploadedObjects) {
            pnlUploadStrategy.setNumUploadedObjects(numUploadedObjects);
        }

        class RadioButtonHandler implements ItemListener {
            public void itemStateChanged(ItemEvent e) {
                if (rbUseNew.isSelected()) {
                    southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));
                    // init a new changeset from the currently edited tags
                    // and the comment field
                    //
                    Changeset cs = new Changeset();
                    tagEditorPanel.getModel().applyToPrimitive(cs);
                    if (cs.get("created_by") == null) {
                        cs.put("created_by", getDefaultCreatedBy());
                    }
                    cs.put("comment", cmt.getText());
                    tagEditorPanel.getModel().initFromPrimitive(cs);
                } else {
                    if (cbOpenChangesets.getSelectedItem() == null) {
                        model.selectFirstChangeset();
                    }
                    Changeset cs = (Changeset)cbOpenChangesets.getSelectedItem();
                    if (cs != null) {
                        cs.put("comment", cmt.getText());
                        southTabbedPane.setTitleAt(1, tr("Tags of changeset {0}", cs.getId()));
                        tagEditorPanel.getModel().initFromPrimitive(cs);
                    }
                }
                refreshGUI();
            }
        }

        class ChangesetListItemStateListener implements ItemListener {
            public void itemStateChanged(ItemEvent e) {
                Changeset cs = (Changeset)cbOpenChangesets.getSelectedItem();
                if (cs == null) {
                    southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));
                    // init a new changeset from the currently edited tags
                    // and the comment field
                    //
                    cs = new Changeset();
                    tagEditorPanel.getModel().applyToPrimitive(cs);
                    if (cs.get("created_by") == null) {
                        cs.put("created_by", getDefaultCreatedBy());
                    }
                    cs.put("comment", cmt.getText());
                    tagEditorPanel.getModel().initFromPrimitive(cs);
                } else {
                    southTabbedPane.setTitleAt(1, tr("Tags of changeset {0}", cs.getId()));
                    if (cs.get("created_by") == null) {
                        cs.put("created_by", getDefaultCreatedBy());
                    }
                    tagEditorPanel.getModel().initFromPrimitive(cs);
                    if (cs.get("comment") != null) {
                        cmt.setText(cs.get("comment"));
                    }
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
                DownloadOpenChangesetsTask task = new DownloadOpenChangesetsTask(model);
                Main.worker.submit(task);
            }
        }

        class CloseChangesetAction extends AbstractAction implements ItemListener{
            public CloseChangesetAction() {
                putValue(NAME, tr("Close"));
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
                setEnabled(cbOpenChangesets.getModel().getSize() > 0 && cbOpenChangesets.getSelectedItem() != null);
            }

            public void itemStateChanged(ItemEvent e) {
                refreshEnabledState();
            }
        }
    }

    /**
     * A combobox model for the list of open changesets
     *
     */
    public class OpenChangesetModel extends DefaultComboBoxModel {
        private List<Changeset> changesets;
        private long uid;
        private Changeset selectedChangeset = null;

        protected Changeset getChangesetById(long id) {
            for (Changeset cs : changesets) {
                if (cs.getId() == id) return cs;
            }
            return null;
        }

        public OpenChangesetModel() {
            this.changesets = new ArrayList<Changeset>();
        }

        protected void internalAddOrUpdate(Changeset cs) {
            Changeset other = getChangesetById(cs.getId());
            if (other != null) {
                cs.cloneFrom(other);
            } else {
                changesets.add(cs);
            }
        }

        public void addOrUpdate(Changeset cs) {
            if (cs.getId() <= 0 )
                throw new IllegalArgumentException(tr("Changeset ID > 0 expected. Got {0}.", cs.getId()));
            internalAddOrUpdate(cs);
            fireContentsChanged(this, 0, getSize());
        }

        public void remove(long id) {
            Changeset cs = getChangesetById(id);
            if (cs != null) {
                changesets.remove(cs);
            }
            fireContentsChanged(this, 0, getSize());
        }

        public void setChangesets(Collection<Changeset> changesets) {
            this.changesets.clear();
            if (changesets != null) {
                for (Changeset cs: changesets) {
                    internalAddOrUpdate(cs);
                }
            }
            fireContentsChanged(this, 0, getSize());
            if (getSelectedItem() == null && !this.changesets.isEmpty()) {
                setSelectedItem(this.changesets.get(0));
            } else if (getSelectedItem() != null) {
                if (changesets.contains(getSelectedItem())) {
                    setSelectedItem(getSelectedItem());
                } else if (!this.changesets.isEmpty()){
                    setSelectedItem(this.changesets.get(0));
                } else {
                    setSelectedItem(null);
                }
            } else {
                setSelectedItem(null);
            }
        }

        public void setUserId(long uid) {
            this.uid = uid;
        }

        public long getUserId() {
            return uid;
        }

        public void selectFirstChangeset() {
            if (changesets == null || changesets.isEmpty()) return;
            setSelectedItem(changesets.get(0));
        }

        public void removeChangeset(Changeset cs) {
            if (cs == null) return;
            changesets.remove(cs);
            if (selectedChangeset == cs) {
                selectFirstChangeset();
            }
            fireContentsChanged(this, 0, getSize());
        }
        /* ------------------------------------------------------------------------------------ */
        /* ComboBoxModel                                                                        */
        /* ------------------------------------------------------------------------------------ */
        @Override
        public Object getElementAt(int index) {
            return changesets.get(index);
        }

        @Override
        public int getIndexOf(Object anObject) {
            return changesets.indexOf(anObject);
        }

        @Override
        public int getSize() {
            return changesets.size();
        }

        @Override
        public Object getSelectedItem() {
            return selectedChangeset;
        }

        @Override
        public void setSelectedItem(Object anObject) {
            if (anObject == null) {
                this.selectedChangeset = null;
                super.setSelectedItem(null);
                return;
            }
            if (! (anObject instanceof Changeset)) return;
            Changeset cs = (Changeset)anObject;
            if (cs.getId() == 0 || ! cs.isOpen()) return;
            Changeset candidate = getChangesetById(cs.getId());
            if (candidate == null) return;
            this.selectedChangeset = candidate;
            super.setSelectedItem(selectedChangeset);
        }
    }
}
