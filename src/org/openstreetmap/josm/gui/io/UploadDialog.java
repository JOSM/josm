package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This is a dialog for entering upload options like the parameters for
 * the upload changeset and the strategy for opening/closing a changeset.
 *
 */
public class UploadDialog extends JDialog implements PropertyChangeListener, PreferenceChangedListener{
    protected static final Logger logger = Logger.getLogger(UploadDialog.class.getName());

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

    /** the panel with the objects to upload */
    private UploadedObjectsSummaryPanel pnlUploadedObjects;
    /** the panel to select the changeset used */
    private ChangesetManagementPanel pnlChangesetManagement;

    private BasicUploadSettingsPanel pnlBasicUploadSettings;

    private UploadStrategySelectionPanel pnlUploadStrategySelectionPanel;

    /** checkbox for selecting whether an atomic upload is to be used  */
    private TagSettingsPanel pnlTagSettings;
    /** the tabbed pane used below of the list of primitives  */
    private JTabbedPane tpConfigPanels;
    /** the upload button */
    private JButton btnUpload;
    private boolean canceled = false;

    /** the changeset comment model keeping the state of the changeset comment */
    private ChangesetCommentModel changesetCommentModel;

    /**
     * builds the content panel for the upload dialog
     *
     * @return the content panel
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel();
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnl.setLayout(new BorderLayout());

        // the panel with the list of uploaded objects
        //
        pnl.add(pnlUploadedObjects = new UploadedObjectsSummaryPanel(), BorderLayout.CENTER);

        // a tabbed pane with two configuration panels in the
        // lower half
        //
        tpConfigPanels = new JTabbedPane() {
            @Override
            public Dimension getPreferredSize() {
                // make sure the tabbed pane never grabs more space than necessary
                //
                return super.getMinimumSize();
            }
        };
        tpConfigPanels.add(new JPanel());
        tpConfigPanels.add(new JPanel());
        tpConfigPanels.add(new JPanel());
        tpConfigPanels.add(new JPanel());

        changesetCommentModel = new ChangesetCommentModel();

        tpConfigPanels.setComponentAt(0, pnlBasicUploadSettings = new BasicUploadSettingsPanel(changesetCommentModel));
        tpConfigPanels.setTitleAt(0, tr("Settings"));
        tpConfigPanels.setToolTipTextAt(0, tr("Decide how to upload the data and which changeset to use"));

        tpConfigPanels.setComponentAt(1,pnlTagSettings = new TagSettingsPanel(changesetCommentModel));
        tpConfigPanels.setTitleAt(1, tr("Tags of new changeset"));
        tpConfigPanels.setToolTipTextAt(1, tr("Apply tags to the changeset data is uploaded to"));

        tpConfigPanels.setComponentAt(2,pnlChangesetManagement = new ChangesetManagementPanel(changesetCommentModel));
        tpConfigPanels.setTitleAt(2, tr("Changesets"));
        tpConfigPanels.setToolTipTextAt(2, tr("Manage open changesets and select a changeset to upload to"));

        tpConfigPanels.setComponentAt(3, pnlUploadStrategySelectionPanel = new UploadStrategySelectionPanel());
        tpConfigPanels.setTitleAt(3, tr("Advanced"));
        tpConfigPanels.setToolTipTextAt(3, tr("Configure advanced settings"));

        pnl.add(tpConfigPanels, BorderLayout.SOUTH);
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
        pnl.add(new SideButton(new ContextSensitiveHelpAction(ht("/Dialog/UploadDialog"))));
        HelpUtil.setHelpContext(getRootPane(),ht("/Dialog/UploadDialog"));
        return pnl;
    }

    /**
     * builds the gui
     */
    protected void build() {
        setTitle(tr("Upload to ''{0}''", OsmApi.getOsmApi().getBaseUrl()));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildContentPanel(), BorderLayout.CENTER);
        getContentPane().add(buildActionPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHandler());


        // make sure the the configuration panels listen to each other
        // changes
        //
        pnlChangesetManagement.addPropertyChangeListener(
                pnlBasicUploadSettings.getUploadParameterSummaryPanel()
        );
        pnlChangesetManagement.addPropertyChangeListener(this);
        pnlUploadedObjects.addPropertyChangeListener(
                pnlBasicUploadSettings.getUploadParameterSummaryPanel()
        );
        pnlUploadedObjects.addPropertyChangeListener(pnlUploadStrategySelectionPanel);
        pnlUploadStrategySelectionPanel.addPropertyChangeListener(
                pnlBasicUploadSettings.getUploadParameterSummaryPanel()
        );


        // users can click on either of two links in the upload parameter
        // summary handler. This installs the handler for these two events.
        // We simply select the appropriate tab in the tabbed pane with the
        // configuration dialogs.
        //
        pnlBasicUploadSettings.getUploadParameterSummaryPanel().setConfigurationParameterRequestListener(
                new ConfigurationParameterRequestHandler() {
                    public void handleUploadStrategyConfigurationRequest() {
                        tpConfigPanels.setSelectedIndex(3);
                    }
                    public void handleChangesetConfigurationRequest() {
                        tpConfigPanels.setSelectedIndex(2);
                    }
                }
        );

        pnlBasicUploadSettings.setUploadCommentDownFocusTraversalHandler(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        btnUpload.requestFocusInWindow();
                    }
                }
        );

        Main.pref.addPreferenceChangeListener(this);
    }

    /**
     * constructor
     */
    public UploadDialog() {
        super(JOptionPane.getFrameForComponent(Main.parent), ModalityType.DOCUMENT_MODAL);
        build();
    }

    /**
     * Sets the collection of primitives to upload
     *
     * @param toUpload the dataset with the objects to upload. If null, assumes the empty
     * set of objects to upload
     *
     */
    public void setUploadedPrimitives(APIDataSet toUpload) {
        if (toUpload == null) {
            List<OsmPrimitive> emptyList = Collections.emptyList();
            pnlUploadedObjects.setUploadedPrimitives(emptyList, emptyList, emptyList);
            return;
        }
        pnlUploadedObjects.setUploadedPrimitives(
                toUpload.getPrimitivesToAdd(),
                toUpload.getPrimitivesToUpdate(),
                toUpload.getPrimitivesToDelete()
        );
    }

    /**
     * Remembers the user input in the preference settings
     */
    public void rememberUserInput() {
        pnlBasicUploadSettings.rememberUserInput();
        pnlUploadStrategySelectionPanel.rememberUserInput();
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        tpConfigPanels.setSelectedIndex(0);
        pnlBasicUploadSettings.startUserInput();
        pnlTagSettings.startUserInput();
        pnlTagSettings.initFromChangeset(pnlChangesetManagement.getSelectedChangeset());
        pnlUploadStrategySelectionPanel.initFromPreferences();
        UploadParameterSummaryPanel pnl = pnlBasicUploadSettings.getUploadParameterSummaryPanel();
        pnl.setUploadStrategySpecification(pnlUploadStrategySelectionPanel.getUploadStrategySpecification());
        pnl.setCloseChangesetAfterNextUpload(pnlChangesetManagement.isCloseChangesetAfterUpload());
        pnl.setNumObjects(pnlUploadedObjects.getNumObjectsToUpload());
    }

    /**
     * Replies the current changeset
     *
     * @return the current changeset
     */
    public Changeset getChangeset() {
        Changeset cs = pnlChangesetManagement.getSelectedChangeset();
        if (cs == null) {
            cs = new Changeset();
        }
        cs.setKeys(pnlTagSettings.getTags());
        return cs;
    }

    public void setSelectedChangesetForNextUpload(Changeset cs) {
        pnlChangesetManagement.setSelectedChangesetForNextUpload(cs);
    }

    /**
     * Replies the {@see UploadStrategySpecification} the user entered in the dialog.
     *
     * @return the {@see UploadStrategySpecification} the user entered in the dialog.
     */
    public UploadStrategySpecification getUploadStrategySpecification() {
        UploadStrategySpecification spec = pnlUploadStrategySelectionPanel.getUploadStrategySpecification();
        spec.setCloseChangesetAfterUpload(pnlChangesetManagement.isCloseChangesetAfterUpload());
        return spec;
    }

    /**
     * Returns the current value for the upload comment
     *
     * @return the current value for the upload comment
     */
    protected String getUploadComment() {
        return changesetCommentModel.getComment();
    }

    /**
     * Returns true if the dialog was canceled
     *
     * @return true if the dialog was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets whether the dialog was canceled
     *
     * @param canceled true if the dialog is canceled
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
            ).applySafe(this);
            startUserInput();
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
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

        /**
         * returns true if the user wants to revisit, false if they
         * want to continue 
         */
        protected boolean warnUploadComment() {
            ExtendedDialog dlg = new ExtendedDialog(UploadDialog.this,
                tr("Please revise upload comment"),
                new String[] {tr("Revise"), tr("Cancel"), tr("Continue as is")});
            dlg.setContent("<html>" + 
                    tr("Your upload comment is <i>empty</i>, or <i>very short</i>.<br /><br />" + 
                       "This is technically allowed, but please consider that many users who are<br />" +
                       "watching changes in their area depend on meaningful changeset comments<br />" +
                       "to understand what is going on!<br /><br />" +
                       "If you spend a minute now to explain your change, you will make life<br />" +
                       "easier for many other mappers.") + 
                    "</html>");
            dlg.setButtonIcons(new Icon[] {
                ImageProvider.get("ok"),
                ImageProvider.get("cancel"),
                ImageProvider.overlay(
                    ImageProvider.get("upload"), 
                    new ImageIcon(ImageProvider.get("warning-small").getImage().getScaledInstance(10 , 10, Image.SCALE_SMOOTH)),
                    ImageProvider.OverlayPosition.SOUTHEAST)});
            dlg.setToolTipTexts(new String[] {
                tr("Return to the previous dialog to enter a more descriptive comment"),
                tr("Cancel and return to the previous dialog"),
                tr("Ignore this hint and upload anyway")});
            dlg.setIcon(JOptionPane.WARNING_MESSAGE);
            dlg.toggleEnable("upload_comment_is_empty_or_very_short");
            dlg.setToggleCheckboxText(tr("Do not show this message again"));
            dlg.setCancelButton(1, 2);
            return dlg.showDialog().getValue() != 3;
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
            if (getUploadComment().trim().length() < 10) {
                if (warnUploadComment())
                {
                    tpConfigPanels.setSelectedIndex(0);
                    pnlBasicUploadSettings.initEditingOfUploadComment();
                    return;
                }
            }
            UploadStrategySpecification strategy = getUploadStrategySpecification();
            if (strategy.getStrategy().equals(UploadStrategy.CHUNKED_DATASET_STRATEGY)) {
                if (strategy.getChunkSize() == UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
                    warnIllegalChunkSize();
                    tpConfigPanels.setSelectedIndex(0);
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
            //startUserInput();
        }

        @Override
        public void windowActivated(WindowEvent arg0) {
            if (tpConfigPanels.getSelectedIndex() == 0) {
                pnlBasicUploadSettings.initEditingOfUploadComment();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /* Interface PropertyChangeListener                                           */
    /* -------------------------------------------------------------------------- */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ChangesetManagementPanel.SELECTED_CHANGESET_PROP)) {
            Changeset cs = (Changeset)evt.getNewValue();
            if (cs == null) {
                tpConfigPanels.setTitleAt(1, tr("Tags of new changeset"));
            } else {
                tpConfigPanels.setTitleAt(1, tr("Tags of changeset {0}", cs.getId()));
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /* Interface PreferenceChangedListener                                        */
    /* -------------------------------------------------------------------------- */
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey() == null || ! e.getKey().equals("osm-server.url"))
            return;
        if (e.getNewValue() == null) {
            setTitle(tr("Upload"));
        } else {
            setTitle(tr("Upload to ''{0}''", e.getNewValue()));
        }
    }
}
