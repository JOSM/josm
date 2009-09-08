// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.historycombobox.SuggestingJHistoryComboBox;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.io.ChangesetProcessingType;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This is a dialog for entering upload options like the parameters for
 * the upload changeset and the strategy for opening/closing a changeset.
 * 
 * 
 */
public class UploadDialog extends JDialog {

    public static final String HISTORY_KEY = "upload.comment.history";

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
    private JCheckBox cbUseAtomicUpload;
    /** input field for changeset comment */
    private SuggestingJHistoryComboBox cmt;
    /** ui component for editing changeset tags */
    private TagEditorPanel tagEditorPanel;
    /** the tabbed pane used below of the list of primitives  */
    private JTabbedPane southTabbedPane;
    /** the button group with the changeset processing types */
    private ButtonGroup bgChangesetHandlingOptions;
    /** radio buttons for selecting a changeset processing type */
    private Map<ChangesetProcessingType, JRadioButton> rbChangesetHandlingOptions;

    private boolean canceled = false;

    /**
     * builds the panel with the lists of primitives
     * 
     * @return the panel with the lists of primitives
     */
    protected JPanel buildListsPanel() {
        pnlLists = new JPanel();
        pnlLists.setLayout(new GridBagLayout());
        // we don't add the lists yet, see setUploadPrimivies()
        //
        return pnlLists;
    }

    /**
     * builds the panel with the ui components for controlling how the changeset
     * should be processed (opening/closing a changeset)
     * 
     * @return the panel with the ui components for controlling how the changeset
     * should be processed
     */
    protected JPanel buildChangesetHandlingControlPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        bgChangesetHandlingOptions = new ButtonGroup();
        rbChangesetHandlingOptions = new HashMap<ChangesetProcessingType, JRadioButton>();
        ChangesetProcessingTypeChangedAction a = new ChangesetProcessingTypeChangedAction();
        for(ChangesetProcessingType type: ChangesetProcessingType.values()) {
            rbChangesetHandlingOptions.put(type, new JRadioButton());
            rbChangesetHandlingOptions.get(type).addActionListener(a);
        }
        JRadioButton rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_CLOSE);
        rb.setText(tr("Use a new changeset and close it"));
        rb.setToolTipText(tr("Select to upload the data using a new changeset and to close the changeset after the upload"));

        rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_LEAVE_OPEN);
        rb.setText(tr("Use a new changeset and leave it open"));
        rb.setToolTipText(tr("Select to upload the data using a new changeset and to leave the changeset open after the upload"));

        pnl.add(new JLabel(tr("Upload to a new or to an existing changeset?")));
        pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_CLOSE));
        pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_NEW_AND_LEAVE_OPEN));
        pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_CLOSE));
        pnl.add(rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_LEAVE_OPEN));

        for(ChangesetProcessingType type: ChangesetProcessingType.values()) {
            rbChangesetHandlingOptions.get(type).setVisible(false);
            bgChangesetHandlingOptions.add(rbChangesetHandlingOptions.get(type));
        }
        return pnl;
    }

    /**
     * build the panel with the widgets for controlling how the changeset should be processed
     * (atomic upload or not, comment, opening/closing changeset)
     * 
     * @return
     */
    protected JPanel buildChangesetControlPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.add(cbUseAtomicUpload = new JCheckBox(tr("upload all changes in one request")));
        cbUseAtomicUpload.setToolTipText(tr("Enable to upload all changes in one request, disable to use one request per changed primitive"));
        boolean useAtomicUpload = Main.pref.getBoolean("osm-server.atomic-upload", true);
        cbUseAtomicUpload.setSelected(useAtomicUpload);
        cbUseAtomicUpload.setEnabled(OsmApi.getOsmApi().hasSupportForDiffUploads());

        pnl.add(buildChangesetHandlingControlPanel());
        return pnl;
    }

    /**
     * builds the upload control panel
     * 
     * @return
     */
    protected JPanel buildUploadControlPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        pnl.add(new JLabel(tr("Provide a brief comment for the changes you are uploading:")), GBC.eol().insets(0, 5, 10, 3));
        cmt = new SuggestingJHistoryComboBox();
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        cmt.setHistory(cmtHistory);
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
        pnl.add(cmt, GBC.eol().fill(GBC.HORIZONTAL));

        // configuration options for atomic upload
        //
        pnl.add(buildChangesetControlPanel(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        return pnl;
    }

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
        southTabbedPane.add(buildUploadControlPanel());
        tagEditorPanel = new TagEditorPanel();
        southTabbedPane.add(tagEditorPanel);
        southTabbedPane.setTitleAt(0, tr("Settings"));
        southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));
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

    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // -- upload button
        pnl.add(new SideButton(new UploadAction()));

        // -- cancel button
        pnl.add(new SideButton(new CancelAction()));

        return pnl;
    }

    /**
     * builds the gui
     */
    protected void build() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildContentPanel(), BorderLayout.CENTER);
        getContentPane().add(buildActionPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowClosingAdapter());
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
            lblUpdate.setText(trn("{0} object to modifiy:", "{0} objects to modify:", update.size(),update.size()));
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
    }

    /**
     * Replies true if a valid changeset comment has been entered in this dialog
     * 
     * @return true if a valid changeset comment has been entered in this dialog
     */
    public boolean hasChangesetComment() {
        if (!getChangesetProcessingType().isUseNew())
            return true;
        return cmt.getText().trim().length() >= 3;
    }

    /**
     * Remembers the user input in the preference settings
     */
    public void rememberUserInput() {
        // store the history of comments
        cmt.addCurrentItemToHistory();
        Main.pref.putCollection(HISTORY_KEY, cmt.getHistory());
        Main.pref.put("osm-server.atomic-upload", cbUseAtomicUpload.isSelected());
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        tagEditorPanel.initAutoCompletion(Main.main.getEditLayer());
        initChangesetProcessingType();
        cmt.getEditor().selectAll();
        cmt.requestFocus();
    }

    /**
     * Replies the current changeset processing type
     * 
     * @return the current changeset processing type
     */
    public ChangesetProcessingType getChangesetProcessingType() {
        ChangesetProcessingType changesetProcessingType = null;
        for (ChangesetProcessingType type: ChangesetProcessingType.values()) {
            if (rbChangesetHandlingOptions.get(type).isSelected()) {
                changesetProcessingType = type;
                break;
            }
        }
        return changesetProcessingType == null ?
                ChangesetProcessingType.USE_NEW_AND_CLOSE :
                    changesetProcessingType;
    }

    /**
     * Replies the current changeset
     * 
     * @return the current changeset
     */
    public Changeset getChangeset() {
        Changeset changeset = new Changeset();
        tagEditorPanel.getModel().applyToPrimitive(changeset);
        changeset.put("comment", cmt.getText());
        return changeset;
    }

    /**
     * initializes the panel depending on the possible changeset processing
     * types
     */
    protected void initChangesetProcessingType() {
        for (ChangesetProcessingType type: ChangesetProcessingType.values()) {
            // show options for new changeset, disable others
            //
            rbChangesetHandlingOptions.get(type).setVisible(type.isUseNew());
        }
        if (OsmApi.getOsmApi().getCurrentChangeset() != null) {
            Changeset cs = OsmApi.getOsmApi().getCurrentChangeset();
            for (ChangesetProcessingType type: ChangesetProcessingType.values()) {
                // show options for using existing changeset
                //
                if (!type.isUseNew()) {
                    rbChangesetHandlingOptions.get(type).setVisible(true);
                }
            }
            JRadioButton rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_CLOSE);
            rb.setText(tr("Use the existing changeset {0} and close it after upload",cs.getId()));
            rb.setToolTipText(tr("Select to upload to the existing changeset {0} and to close the changeset after this upload",cs.getId()));

            rb = rbChangesetHandlingOptions.get(ChangesetProcessingType.USE_EXISTING_AND_LEAVE_OPEN);
            rb.setText(tr("Use the existing changeset {0} and leave it open",cs.getId()));
            rb.setToolTipText(tr("Select to upload to the existing changeset {0} and to leave the changeset open for further uploads",cs.getId()));

            rbChangesetHandlingOptions.get(getChangesetProcessingType()).setSelected(true);

        } else {
            ChangesetProcessingType type = getChangesetProcessingType();
            if (!type.isUseNew()) {
                type = ChangesetProcessingType.USE_NEW_AND_CLOSE;
            }
            rbChangesetHandlingOptions.get(type).setSelected(true);
        }
        ChangesetProcessingType type = getChangesetProcessingType();
        if (type.isUseNew() || (! type.isUseNew() && OsmApi.getOsmApi().getCurrentChangeset() == null)) {
            Changeset cs = new Changeset();
            cs.put("created_by", getDefaultCreatedBy());
            tagEditorPanel.getModel().initFromPrimitive(cs);
        } else {
            Changeset cs = OsmApi.getOsmApi().getCurrentChangeset();
            tagEditorPanel.getModel().initFromPrimitive(cs);
        }
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
     * refreshes  the panel depending on a changeset processing type
     * 
     * @param type the changeset processing type
     */
    protected void switchToProcessingType(ChangesetProcessingType type) {
        if (type.isUseNew()) {
            southTabbedPane.setTitleAt(1, tr("Tags of new changeset"));
            // init a new changeset from the currently edited tags
            // and the comment field
            //
            Changeset cs = new Changeset(getChangeset());
            if (cs.get("created_by") == null) {
                cs.put("created_by", getDefaultCreatedBy());
            }
            cs.put("comment", this.cmt.getText());
            tagEditorPanel.getModel().initFromPrimitive(cs);
        } else {
            Changeset cs = OsmApi.getOsmApi().getCurrentChangeset();
            if (cs != null) {
                cs.put("comment", this.cmt.getText());
                cs.setKeys(getChangeset().getKeys());
                southTabbedPane.setTitleAt(1, tr("Tags of changeset {0}", cs.getId()));
                tagEditorPanel.getModel().initFromPrimitive(cs);
            }
        }
    }

    public String getUploadComment() {
        switch(southTabbedPane.getSelectedIndex()) {
        case 0: return cmt.getText();
        case 1:
            TagModel tm = tagEditorPanel.getModel().get("comment");
            return tm == null? "" : tm.getValue();
        }
        return "";
    }

    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            JOptionPane.getFrameForComponent(Main.parent),
                            new Dimension(400,600)
                    )
            ).apply(this);
            startUserInput();
        } else {
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    class ChangesetProcessingTypeChangedAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ChangesetProcessingType type = getChangesetProcessingType();
            switchToProcessingType(type);
        }
    }


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
            tm.setValue(cmt.getText().trim());
            if (cmt.getText().trim().equals("")) {
                removeCommentTag();
            }
            tagEditorPanel.getModel().fireTableDataChanged();
        }


        public void stateChanged(ChangeEvent e) {
            if (southTabbedPane.getSelectedIndex() ==0) {
                TagModel tm = tagEditorPanel.getModel().get("comment");
                cmt.setText(tm == null ? "" : tm.getValue());
                cmt.getEditor().selectAll();
                cmt.requestFocus();
            } else if (southTabbedPane.getSelectedIndex() == 1) {
                refreshCommentTag();
            }
        }
    }

    class UploadAction extends AbstractAction {
        public UploadAction() {
            putValue(NAME, tr("Upload Changes"));
            putValue(SMALL_ICON, ImageProvider.get("upload"));
            putValue(SHORT_DESCRIPTION, tr("Upload the changed primitives"));
        }

        protected void warnIllegalUploadComment() {
            JOptionPane.showMessageDialog(
                    UploadDialog.this,
                    tr("Please enter a comment for this upload changeset (min. 3 characters)"),
                    tr("Illegal upload comment"),
                    JOptionPane.ERROR_MESSAGE

            );
        }
        public void actionPerformed(ActionEvent e) {
            if (getUploadComment().trim().length() < 3) {
                warnIllegalUploadComment();
                cmt.getEditor().selectAll();
                cmt.requestFocus();
                return;
            }
            setCanceled(false);
            setVisible(false);

        }
    }

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

    class PrimitiveList extends JList {
        public PrimitiveList() {
            super(new PrimitiveListModel());
        }

        public PrimitiveListModel getPrimitiveListModel() {
            return (PrimitiveListModel)getModel();
        }
    }

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

    class WindowClosingAdapter extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            setCanceled(true);
        }
    }
}