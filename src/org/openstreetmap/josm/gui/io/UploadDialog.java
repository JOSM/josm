// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.Setting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.MultiLineFlowLayout;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.UploadStrategy;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageOverlay;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a dialog for entering upload options like the parameters for
 * the upload changeset and the strategy for opening/closing a changeset.
 * @since 2025
 */
public class UploadDialog extends AbstractUploadDialog implements PropertyChangeListener, PreferenceChangedListener {
    /** the unique instance of the upload dialog */
    private static UploadDialog uploadDialog;

    /** list of custom components that can be added by plugins at JOSM startup */
    private static final Collection<Component> customComponents = new ArrayList<>();

    /** the "created_by" changeset OSM key */
    private static final String CREATED_BY = "created_by";

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

    /** the changeset comment model keeping the state of the changeset comment */
    private final transient ChangesetCommentModel changesetCommentModel = new ChangesetCommentModel();
    private final transient ChangesetCommentModel changesetSourceModel = new ChangesetCommentModel();
    private final transient ChangesetReviewModel changesetReviewModel = new ChangesetReviewModel();

    private transient DataSet dataSet;

    /**
     * Constructs a new {@code UploadDialog}.
     */
    public UploadDialog() {
        super(GuiHelper.getFrameForComponent(Main.parent), ModalityType.DOCUMENT_MODAL);
        build();
    }

    /**
     * Replies the unique instance of the upload dialog
     *
     * @return the unique instance of the upload dialog
     */
    public static synchronized UploadDialog getUploadDialog() {
        if (uploadDialog == null) {
            uploadDialog = new UploadDialog();
        }
        return uploadDialog;
    }

    /**
     * builds the content panel for the upload dialog
     *
     * @return the content panel
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // the panel with the list of uploaded objects
        pnlUploadedObjects = new UploadedObjectsSummaryPanel();
        pnl.add(pnlUploadedObjects, GBC.eol().fill(GBC.BOTH));

        // Custom components
        for (Component c : customComponents) {
            pnl.add(c, GBC.eol().fill(GBC.HORIZONTAL));
        }

        // a tabbed pane with configuration panels in the lower half
        tpConfigPanels = new CompactTabbedPane();

        pnlBasicUploadSettings = new BasicUploadSettingsPanel(changesetCommentModel, changesetSourceModel, changesetReviewModel);
        tpConfigPanels.add(pnlBasicUploadSettings);
        tpConfigPanels.setTitleAt(0, tr("Settings"));
        tpConfigPanels.setToolTipTextAt(0, tr("Decide how to upload the data and which changeset to use"));

        pnlTagSettings = new TagSettingsPanel(changesetCommentModel, changesetSourceModel, changesetReviewModel);
        tpConfigPanels.add(pnlTagSettings);
        tpConfigPanels.setTitleAt(1, tr("Tags of new changeset"));
        tpConfigPanels.setToolTipTextAt(1, tr("Apply tags to the changeset data is uploaded to"));

        pnlChangesetManagement = new ChangesetManagementPanel(changesetCommentModel);
        tpConfigPanels.add(pnlChangesetManagement);
        tpConfigPanels.setTitleAt(2, tr("Changesets"));
        tpConfigPanels.setToolTipTextAt(2, tr("Manage open changesets and select a changeset to upload to"));

        pnlUploadStrategySelectionPanel = new UploadStrategySelectionPanel();
        tpConfigPanels.add(pnlUploadStrategySelectionPanel);
        tpConfigPanels.setTitleAt(3, tr("Advanced"));
        tpConfigPanels.setToolTipTextAt(3, tr("Configure advanced settings"));

        pnl.add(tpConfigPanels, GBC.eol().fill(GBC.HORIZONTAL));

        pnl.add(buildActionPanel(), GBC.eol().fill(GBC.HORIZONTAL));
        return pnl;
    }

    /**
     * builds the panel with the OK and CANCEL buttons
     *
     * @return The panel with the OK and CANCEL buttons
     */
    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel(new MultiLineFlowLayout(FlowLayout.CENTER));
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // -- upload button
        btnUpload = new JButton(new UploadAction(this));
        pnl.add(btnUpload);
        btnUpload.setFocusable(true);
        InputMapUtils.enableEnter(btnUpload);
        bindCtrlEnterToAction(getRootPane(), btnUpload.getAction());

        // -- cancel button
        CancelAction cancelAction = new CancelAction(this);
        pnl.add(new JButton(cancelAction));
        InputMapUtils.addEscapeAction(getRootPane(), cancelAction);
        pnl.add(new JButton(new ContextSensitiveHelpAction(ht("/Dialog/Upload"))));
        HelpUtil.setHelpContext(getRootPane(), ht("/Dialog/Upload"));
        return pnl;
    }

    /**
     * builds the gui
     */
    protected void build() {
        setTitle(tr("Upload to ''{0}''", OsmApi.getOsmApi().getBaseUrl()));
        setContentPane(buildContentPanel());

        addWindowListener(new WindowEventHandler());


        // make sure the configuration panels listen to each other
        // changes
        //
        pnlChangesetManagement.addPropertyChangeListener(this);
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
        // We simply select the appropriate tab in the tabbed pane with the configuration dialogs.
        //
        pnlBasicUploadSettings.getUploadParameterSummaryPanel().setConfigurationParameterRequestListener(
                new ConfigurationParameterRequestHandler() {
                    @Override
                    public void handleUploadStrategyConfigurationRequest() {
                        tpConfigPanels.setSelectedIndex(3);
                    }

                    @Override
                    public void handleChangesetConfigurationRequest() {
                        tpConfigPanels.setSelectedIndex(2);
                    }
                }
        );

        pnlBasicUploadSettings.setUploadTagDownFocusTraversalHandlers(
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        btnUpload.requestFocusInWindow();
                    }
                }
        );

        setMinimumSize(new Dimension(600, 350));

        Main.pref.addPreferenceChangeListener(this);
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
     * Sets the tags for this upload based on (later items overwrite earlier ones):
     * <ul>
     * <li>previous "source" and "comment" input</li>
     * <li>the tags set in the dataset (see {@link DataSet#getChangeSetTags()})</li>
     * <li>the tags from the selected open changeset</li>
     * <li>the JOSM user agent (see {@link Version#getAgentString(boolean)})</li>
     * </ul>
     *
     * @param dataSet to obtain the tags set in the dataset
     */
    public void setChangesetTags(DataSet dataSet) {
        final Map<String, String> tags = new HashMap<>();

        // obtain from previous input
        tags.put("source", getLastChangesetSourceFromHistory());
        tags.put("comment", getLastChangesetCommentFromHistory());

        // obtain from dataset
        if (dataSet != null) {
            tags.putAll(dataSet.getChangeSetTags());
        }
        this.dataSet = dataSet;

        // obtain from selected open changeset
        if (pnlChangesetManagement.getSelectedChangeset() != null) {
            tags.putAll(pnlChangesetManagement.getSelectedChangeset().getKeys());
        }

        // set/adapt created_by
        final String agent = Version.getInstance().getAgentString(false);
        final String createdBy = tags.get(CREATED_BY);
        if (createdBy == null || createdBy.isEmpty()) {
            tags.put(CREATED_BY, agent);
        } else if (!createdBy.contains(agent)) {
            tags.put(CREATED_BY, createdBy + ';' + agent);
        }

        // remove empty values
        final Iterator<String> it = tags.keySet().iterator();
        while (it.hasNext()) {
            final String v = tags.get(it.next());
            if (v == null || v.isEmpty()) {
                it.remove();
            }
        }

        pnlTagSettings.initFromTags(tags);
        pnlTagSettings.tableChanged(null);
    }

    @Override
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
        Changeset cs = Optional.ofNullable(pnlChangesetManagement.getSelectedChangeset()).orElseGet(Changeset::new);
        cs.setKeys(pnlTagSettings.getTags(false));
        return cs;
    }

    /**
     * Sets the changeset to be used in the next upload
     *
     * @param cs the changeset
     */
    public void setSelectedChangesetForNextUpload(Changeset cs) {
        pnlChangesetManagement.setSelectedChangesetForNextUpload(cs);
    }

    @Override
    public UploadStrategySpecification getUploadStrategySpecification() {
        UploadStrategySpecification spec = pnlUploadStrategySelectionPanel.getUploadStrategySpecification();
        spec.setCloseChangesetAfterUpload(pnlChangesetManagement.isCloseChangesetAfterUpload());
        return spec;
    }

    @Override
    public String getUploadComment() {
        return changesetCommentModel.getComment();
    }

    @Override
    public String getUploadSource() {
        return changesetSourceModel.getComment();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(400, 600)
                    )
            ).applySafe(this);
            startUserInput();
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Adds a custom component to this dialog.
     * Custom components added at JOSM startup are displayed between the objects list and the properties tab pane.
     * @param c The custom component to add. If {@code null}, this method does nothing.
     * @return {@code true} if the collection of custom components changed as a result of the call
     * @since 5842
     */
    public static boolean addCustomComponent(Component c) {
        if (c != null) {
            return customComponents.add(c);
        }
        return false;
    }

    static final class CompactTabbedPane extends JTabbedPane {
        @Override
        public Dimension getPreferredSize() {
            // make sure the tabbed pane never grabs more space than necessary
            return super.getMinimumSize();
        }
    }

    /**
     * Handles an upload.
     */
    static class UploadAction extends AbstractAction {

        private final transient IUploadDialog dialog;

        UploadAction(IUploadDialog dialog) {
            this.dialog = dialog;
            putValue(NAME, tr("Upload Changes"));
            putValue(SMALL_ICON, ImageProvider.get("upload"));
            putValue(SHORT_DESCRIPTION, tr("Upload the changed primitives"));
        }

        /**
         * Displays a warning message indicating that the upload comment is empty/short.
         * @return true if the user wants to revisit, false if they want to continue
         */
        protected boolean warnUploadComment() {
            return warnUploadTag(
                    tr("Please revise upload comment"),
                    tr("Your upload comment is <i>empty</i>, or <i>very short</i>.<br /><br />" +
                            "This is technically allowed, but please consider that many users who are<br />" +
                            "watching changes in their area depend on meaningful changeset comments<br />" +
                            "to understand what is going on!<br /><br />" +
                            "If you spend a minute now to explain your change, you will make life<br />" +
                            "easier for many other mappers."),
                    "upload_comment_is_empty_or_very_short"
            );
        }

        /**
         * Displays a warning message indicating that no changeset source is given.
         * @return true if the user wants to revisit, false if they want to continue
         */
        protected boolean warnUploadSource() {
            return warnUploadTag(
                    tr("Please specify a changeset source"),
                    tr("You did not specify a source for your changes.<br />" +
                            "It is technically allowed, but this information helps<br />" +
                            "other users to understand the origins of the data.<br /><br />" +
                            "If you spend a minute now to explain your change, you will make life<br />" +
                            "easier for many other mappers."),
                    "upload_source_is_empty"
            );
        }

        protected boolean warnUploadTag(final String title, final String message, final String togglePref) {
            String[] buttonTexts = new String[] {tr("Revise"), tr("Cancel"), tr("Continue as is")};
            Icon[] buttonIcons = new Icon[] {
                    new ImageProvider("ok").setMaxSize(ImageSizes.LARGEICON).get(),
                    new ImageProvider("cancel").setMaxSize(ImageSizes.LARGEICON).get(),
                    new ImageProvider("upload").setMaxSize(ImageSizes.LARGEICON).addOverlay(
                            new ImageOverlay(new ImageProvider("warning-small"), 0.5, 0.5, 1.0, 1.0)).get()};
            String[] tooltips = new String[] {
                    tr("Return to the previous dialog to enter a more descriptive comment"),
                    tr("Cancel and return to the previous dialog"),
                    tr("Ignore this hint and upload anyway")};

            if (GraphicsEnvironment.isHeadless()) {
                return false;
            }

            ExtendedDialog dlg = new ExtendedDialog((Component) dialog, title, buttonTexts) {
                @Override
                public void setupDialog() {
                    super.setupDialog();
                    bindCtrlEnterToAction(getRootPane(), buttons.get(buttons.size() - 1).getAction());
                }
            };
            dlg.setContent("<html>" + message + "</html>");
            dlg.setButtonIcons(buttonIcons);
            dlg.setToolTipTexts(tooltips);
            dlg.setIcon(JOptionPane.WARNING_MESSAGE);
            dlg.toggleEnable(togglePref);
            dlg.setCancelButton(1, 2);
            return dlg.showDialog().getValue() != 3;
        }

        protected void warnIllegalChunkSize() {
            HelpAwareOptionPane.showOptionDialog(
                    (Component) dialog,
                    tr("Please enter a valid chunk size first"),
                    tr("Illegal chunk size"),
                    JOptionPane.ERROR_MESSAGE,
                    ht("/Dialog/Upload#IllegalChunkSize")
            );
        }

        static boolean isUploadCommentTooShort(String comment) {
            String s = comment.trim();
            boolean result = true;
            if (!s.isEmpty()) {
                UnicodeBlock block = Character.UnicodeBlock.of(s.charAt(0));
                if (block != null && block.toString().contains("CJK")) {
                    result = s.length() < 4;
                } else {
                    result = s.length() < 10;
                }
            }
            return result;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isUploadCommentTooShort(dialog.getUploadComment()) && warnUploadComment()) {
                // abort for missing comment
                dialog.handleMissingComment();
                return;
            }
            if (dialog.getUploadSource().trim().isEmpty() && warnUploadSource()) {
                // abort for missing changeset source
                dialog.handleMissingSource();
                return;
            }

            /* test for empty tags in the changeset metadata and proceed only after user's confirmation.
             * though, accept if key and value are empty (cf. xor). */
            List<String> emptyChangesetTags = new ArrayList<>();
            for (final Entry<String, String> i : dialog.getTags(true).entrySet()) {
                final boolean isKeyEmpty = i.getKey() == null || i.getKey().trim().isEmpty();
                final boolean isValueEmpty = i.getValue() == null || i.getValue().trim().isEmpty();
                final boolean ignoreKey = "comment".equals(i.getKey()) || "source".equals(i.getKey());
                if ((isKeyEmpty ^ isValueEmpty) && !ignoreKey) {
                    emptyChangesetTags.add(tr("{0}={1}", i.getKey(), i.getValue()));
                }
            }
            if (!emptyChangesetTags.isEmpty() && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                    Main.parent,
                    trn(
                            "<html>The following changeset tag contains an empty key/value:<br>{0}<br>Continue?</html>",
                            "<html>The following changeset tags contain an empty key/value:<br>{0}<br>Continue?</html>",
                            emptyChangesetTags.size(), Utils.joinAsHtmlUnorderedList(emptyChangesetTags)),
                    tr("Empty metadata"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            )) {
                dialog.handleMissingComment();
                return;
            }

            UploadStrategySpecification strategy = dialog.getUploadStrategySpecification();
            if (strategy.getStrategy().equals(UploadStrategy.CHUNKED_DATASET_STRATEGY)
                    && strategy.getChunkSize() == UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
                warnIllegalChunkSize();
                dialog.handleIllegalChunkSize();
                return;
            }
            if (dialog instanceof AbstractUploadDialog) {
                ((AbstractUploadDialog) dialog).setCanceled(false);
                ((AbstractUploadDialog) dialog).setVisible(false);
            }
        }
    }

    /**
     * Action for canceling the dialog.
     */
    static class CancelAction extends AbstractAction {

        private final transient IUploadDialog dialog;

        CancelAction(IUploadDialog dialog) {
            this.dialog = dialog;
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Cancel the upload and resume editing"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (dialog instanceof AbstractUploadDialog) {
                ((AbstractUploadDialog) dialog).setCanceled(true);
                ((AbstractUploadDialog) dialog).setVisible(false);
            }
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
        public void windowActivated(WindowEvent arg0) {
            if (tpConfigPanels.getSelectedIndex() == 0) {
                pnlBasicUploadSettings.initEditingOfUploadComment();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /* Interface PropertyChangeListener                                           */
    /* -------------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ChangesetManagementPanel.SELECTED_CHANGESET_PROP)) {
            Changeset cs = (Changeset) evt.getNewValue();
            setChangesetTags(dataSet);
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
    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey() == null || !"osm-server.url".equals(e.getKey()))
            return;
        final Setting<?> newValue = e.getNewValue();
        final String url;
        if (newValue == null || newValue.getValue() == null) {
            url = OsmApi.getOsmApi().getBaseUrl();
        } else {
            url = newValue.getValue().toString();
        }
        setTitle(tr("Upload to ''{0}''", url));
    }

    private static String getLastChangesetTagFromHistory(String historyKey, List<String> def) {
        Collection<String> history = Main.pref.getList(historyKey, def);
        int age = (int) (System.currentTimeMillis() / 1000 - Main.pref.getInt(BasicUploadSettingsPanel.HISTORY_LAST_USED_KEY, 0));
        if (history != null && age < Main.pref.getLong(BasicUploadSettingsPanel.HISTORY_MAX_AGE_KEY, TimeUnit.HOURS.toMillis(4))
                && !history.isEmpty()) {
            return history.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * Returns the last changeset comment from history.
     * @return the last changeset comment from history
     */
    public String getLastChangesetCommentFromHistory() {
        return getLastChangesetTagFromHistory(BasicUploadSettingsPanel.HISTORY_KEY, new ArrayList<String>());
    }

    /**
     * Returns the last changeset source from history.
     * @return the last changeset source from history
     */
    public String getLastChangesetSourceFromHistory() {
        return getLastChangesetTagFromHistory(BasicUploadSettingsPanel.SOURCE_HISTORY_KEY, BasicUploadSettingsPanel.getDefaultSources());
    }

    @Override
    public Map<String, String> getTags(boolean keepEmpty) {
        return pnlTagSettings.getTags(keepEmpty);
    }

    @Override
    public void handleMissingComment() {
        tpConfigPanels.setSelectedIndex(0);
        pnlBasicUploadSettings.initEditingOfUploadComment();
    }

    @Override
    public void handleMissingSource() {
        tpConfigPanels.setSelectedIndex(0);
        pnlBasicUploadSettings.initEditingOfUploadSource();
    }

    @Override
    public void handleIllegalChunkSize() {
        tpConfigPanels.setSelectedIndex(0);
    }

    private static void bindCtrlEnterToAction(JComponent component, Action actionToBind) {
        final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "ctrl_enter");
        component.getActionMap().put("ctrl_enter", actionToBind);
    }
}
