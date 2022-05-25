// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
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
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.MultiLineFlowLayout;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.UploadStrategy;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a dialog for entering upload options like the parameters for
 * the upload changeset and the strategy for opening/closing a changeset.
 * @since 2025
 */
public class UploadDialog extends AbstractUploadDialog implements PreferenceChangedListener, PropertyChangeListener {
    /** the unique instance of the upload dialog */
    private static UploadDialog uploadDialog;

    /** the panel with the objects to upload */
    private UploadedObjectsSummaryPanel pnlUploadedObjects;

    /** the "description" tab */
    private BasicUploadSettingsPanel pnlBasicUploadSettings;

    /** the panel to select the changeset used */
    private ChangesetManagementPanel pnlChangesetManagement;
    /** the panel to select the upload strategy */
    private UploadStrategySelectionPanel pnlUploadStrategySelectionPanel;

    /** the tag editor panel */
    private TagEditorPanel pnlTagEditor;
    /** the tabbed pane used below of the list of primitives  */
    private JTabbedPane tpConfigPanels;
    /** the upload button */
    private JButton btnUpload;

    /** the model keeping the state of the changeset tags */
    private final transient UploadDialogModel model = new UploadDialogModel();

    private transient DataSet dataSet;

    /**
     * Constructs a new {@code UploadDialog}.
     */
    protected UploadDialog() {
        super(GuiHelper.getFrameForComponent(MainApplication.getMainFrame()), ModalityType.DOCUMENT_MODAL);
        build();
        pack();
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
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // the panel with the list of uploaded objects
        pnlUploadedObjects = new UploadedObjectsSummaryPanel();
        pnlUploadedObjects.setMinimumSize(new Dimension(200, 50));
        splitPane.setLeftComponent(pnlUploadedObjects);

        // a tabbed pane with configuration panels in the lower half
        tpConfigPanels = new CompactTabbedPane();
        splitPane.setRightComponent(tpConfigPanels);

        pnlBasicUploadSettings = new BasicUploadSettingsPanel(model);
        tpConfigPanels.add(pnlBasicUploadSettings);
        tpConfigPanels.setTitleAt(0, tr("Description"));
        tpConfigPanels.setToolTipTextAt(0, tr("Describe the changes you made"));

        JPanel pnlSettings = new JPanel(new GridBagLayout());
        pnlSettings.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        JPanel pnlTagEditorBorder = new JPanel(new BorderLayout());
        pnlTagEditorBorder.setBorder(BorderFactory.createTitledBorder(tr("Changeset tags:")));
        pnlTagEditor = new TagEditorPanel(model, null, Changeset.MAX_CHANGESET_TAG_LENGTH);
        pnlTagEditorBorder.add(pnlTagEditor, BorderLayout.CENTER);

        pnlChangesetManagement = new ChangesetManagementPanel();
        pnlUploadStrategySelectionPanel = new UploadStrategySelectionPanel();
        pnlSettings.add(pnlChangesetManagement, GBC.eop().fill(GridBagConstraints.HORIZONTAL));
        pnlSettings.add(pnlUploadStrategySelectionPanel, GBC.eop().fill(GridBagConstraints.HORIZONTAL));
        pnlSettings.add(pnlTagEditorBorder, GBC.eol().fill(GridBagConstraints.BOTH));

        tpConfigPanels.add(pnlSettings);
        tpConfigPanels.setTitleAt(1, tr("Settings"));
        tpConfigPanels.setToolTipTextAt(1, tr("Decide how to upload the data and which changeset to use"));

        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(splitPane, BorderLayout.CENTER);
        pnl.add(buildActionPanel(), BorderLayout.SOUTH);
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
        InputMapUtils.addCtrlEnterAction(getRootPane(), btnUpload.getAction());

        // -- cancel button
        CancelAction cancelAction = new CancelAction(this);
        pnl.add(new JButton(cancelAction));
        InputMapUtils.addEscapeAction(getRootPane(), cancelAction);

        // -- help button
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

        // make sure the configuration panels listen to each others changes
        //
        UploadParameterSummaryPanel sp = pnlBasicUploadSettings.getUploadParameterSummaryPanel();
        // the summary panel must know everything
        pnlChangesetManagement.addPropertyChangeListener(sp);
        pnlUploadedObjects.addPropertyChangeListener(sp);
        pnlUploadStrategySelectionPanel.addPropertyChangeListener(sp);

        // update tags from selected changeset
        pnlChangesetManagement.addPropertyChangeListener(this);

        // users can click on either of two links in the upload parameter
        // summary handler. This installs the handler for these two events.
        // We simply select the appropriate tab in the tabbed pane with the configuration dialogs.
        //
        pnlBasicUploadSettings.getUploadParameterSummaryPanel().setConfigurationParameterRequestListener(
                () -> tpConfigPanels.setSelectedIndex(2)
        );

        // Enable/disable the upload button if at least an upload validator rejects upload
        pnlBasicUploadSettings.getUploadTextValidators().forEach(v -> v.addChangeListener(e -> btnUpload.setEnabled(
                pnlBasicUploadSettings.getUploadTextValidators().stream().noneMatch(UploadTextComponentValidator::isUploadRejected))));

        setMinimumSize(new Dimension(600, 350));

        Config.getPref().addPreferenceChangeListener(this);
    }

    /**
     * Initializes this life cycle of the dialog.
     *
     * Initializes the dialog each time before it is made visible. We cannot do
     * this in the constructor because the dialog is a singleton.
     *
     * @param dataSet The Dataset we want to upload
     * @since 18173
     */
    public void initLifeCycle(DataSet dataSet) {
        Map<String, String> map = new HashMap<>();
        this.dataSet = dataSet;
        pnlBasicUploadSettings.initLifeCycle(map);
        pnlChangesetManagement.initLifeCycle();
        model.clear();
        model.putAll(map);          // init with tags from history
        model.putAll(this.dataSet); // overwrite with tags from the dataset
        if (Config.getPref().getBoolean("upload.source.obtainautomatically", false)
        && this.dataSet.getChangeSetTags().containsKey(UploadDialogModel.SOURCE)) {
            model.put(UploadDialogModel.SOURCE, pnlBasicUploadSettings.getSourceFromLayer());
        }

        tpConfigPanels.setSelectedIndex(0);
        pnlTagEditor.initAutoCompletion(MainApplication.getLayerManager().getEditLayer());
        pnlUploadStrategySelectionPanel.initFromPreferences();

        // update the summary
        UploadParameterSummaryPanel sumPnl = pnlBasicUploadSettings.getUploadParameterSummaryPanel();
        sumPnl.setUploadStrategySpecification(pnlUploadStrategySelectionPanel.getUploadStrategySpecification());
        sumPnl.setCloseChangesetAfterNextUpload(pnlChangesetManagement.isCloseChangesetAfterUpload());
    }

    /**
     * Sets the collection of primitives to upload
     *
     * @param toUpload the dataset with the objects to upload. If null, assumes the empty
     * set of objects to upload
     *
     */
    public void setUploadedPrimitives(APIDataSet toUpload) {
        UploadParameterSummaryPanel sumPnl = pnlBasicUploadSettings.getUploadParameterSummaryPanel();
        if (toUpload == null) {
            if (pnlUploadedObjects != null) {
                List<OsmPrimitive> emptyList = Collections.emptyList();
                pnlUploadedObjects.setUploadedPrimitives(emptyList, emptyList, emptyList);
                sumPnl.setNumObjects(0);
            }
            return;
        }
        List<OsmPrimitive> l = toUpload.getPrimitives();
        pnlBasicUploadSettings.setUploadedPrimitives(l);
        pnlUploadedObjects.setUploadedPrimitives(
                toUpload.getPrimitivesToAdd(),
                toUpload.getPrimitivesToUpdate(),
                toUpload.getPrimitivesToDelete()
        );
        sumPnl.setNumObjects(l.size());
        pnlUploadStrategySelectionPanel.setNumUploadedObjects(l.size());
    }

    /**
     * Sets the input focus to upload button.
     * @since 18173
     */
    public void setFocusToUploadButton() {
        btnUpload.requestFocus();
    }

    @Override
    public void rememberUserInput() {
        pnlBasicUploadSettings.rememberUserInput();
        pnlUploadStrategySelectionPanel.rememberUserInput();
    }

    /**
     * Returns the changeset to use complete with tags
     *
     * @return the changeset to use
     */
    public Changeset getChangeset() {
        Changeset cs = pnlChangesetManagement.getSelectedChangeset();
        cs.setKeys(getTags(true));
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

    /**
     * Get the upload dialog model.
     *
     * @return The model.
     * @since 18173
     */
    public UploadDialogModel getModel() {
        return model;
    }

    @Override
    public String getUploadComment() {
        return model.getValue(UploadDialogModel.COMMENT);
    }

    @Override
    public String getUploadSource() {
        return model.getValue(UploadDialogModel.SOURCE);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            MainApplication.getMainFrame(),
                            new Dimension(800, 600)
                    )
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    static final class CompactTabbedPane extends JTabbedPane {
        @Override
        public Dimension getPreferredSize() {
            // This probably fixes #18523. Don't know why. Don't know how. It just does.
            super.getPreferredSize();
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
            new ImageProvider("upload").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Upload the changed primitives"));
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
            String s = Utils.strip(comment);
            if (s.isEmpty()) {
                return true;
            }
            UnicodeBlock block = Character.UnicodeBlock.of(s.charAt(0));
            if (block != null && block.toString().contains("CJK")) {
                return s.length() < 4;
            } else {
                return s.length() < 10;
            }
        }

        private static String lower(String s) {
            return s.toLowerCase(Locale.ENGLISH);
        }

        static String validateUploadTag(String uploadValue, String preferencePrefix,
                List<String> defMandatory, List<String> defForbidden, List<String> defException) {
            String uploadValueLc = lower(uploadValue);
            // Check mandatory terms
            List<String> missingTerms = Config.getPref().getList(preferencePrefix+".mandatory-terms", defMandatory)
                .stream().map(UploadAction::lower).filter(x -> !uploadValueLc.contains(x)).collect(Collectors.toList());
            if (!missingTerms.isEmpty()) {
                return tr("The following required terms are missing: {0}", missingTerms);
            }
            // Check forbidden terms
            List<String> exceptions = Config.getPref().getList(preferencePrefix+".exception-terms", defException);
            List<String> forbiddenTerms = Config.getPref().getList(preferencePrefix+".forbidden-terms", defForbidden)
                    .stream().map(UploadAction::lower)
                    .filter(x -> uploadValueLc.contains(x) && exceptions.stream().noneMatch(uploadValueLc::contains))
                    .collect(Collectors.toList());
            if (!forbiddenTerms.isEmpty()) {
                return tr("The following forbidden terms have been found: {0}", forbiddenTerms);
            }
            return null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Map<String, String> tags = dialog.getTags(true);

            // If there are empty tags in the changeset proceed only after user's confirmation.
            List<String> emptyChangesetTags = new ArrayList<>();
            for (final Entry<String, String> i : tags.entrySet()) {
                final boolean isKeyEmpty = Utils.isStripEmpty(i.getKey());
                final boolean isValueEmpty = Utils.isStripEmpty(i.getValue());
                final boolean ignoreKey = UploadDialogModel.isCommentOrSource(i.getKey());
                if ((isKeyEmpty || isValueEmpty) && !ignoreKey) {
                    emptyChangesetTags.add(tr("{0}={1}", i.getKey(), i.getValue()));
                }
            }
            if (!emptyChangesetTags.isEmpty() && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                    MainApplication.getMainFrame(),
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
            if (strategy.getStrategy() == UploadStrategy.CHUNKED_DATASET_STRATEGY
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
            new ImageProvider("cancel").getResource().attachImageIcon(this, true);
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
     */
    class WindowEventHandler extends WindowAdapter {
        private boolean activatedOnce;

        @Override
        public void windowClosing(WindowEvent e) {
            setCanceled(true);
        }

        @Override
        public void windowActivated(WindowEvent e) {
            if (!activatedOnce && tpConfigPanels.getSelectedIndex() == 0) {
                pnlBasicUploadSettings.initEditingOfUploadComment();
                activatedOnce = true;
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /* Interface PropertyChangeListener                                           */
    /* -------------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ChangesetManagementPanel.SELECTED_CHANGESET_PROP)) {
            // put the tags from the newly selected changeset into the model
            Changeset cs = (Changeset) evt.getNewValue();
            if (cs != null) {
                for (Map.Entry<String, String> entry : cs.getKeys().entrySet()) {
                    String key = entry.getKey();
                    // do NOT overwrite comment and source when selecting a changeset, it is confusing
                    if (!UploadDialogModel.isCommentOrSource(key))
                        model.put(key, entry.getValue());
                }
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /* Interface PreferenceChangedListener                                        */
    /* -------------------------------------------------------------------------- */
    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey() != null
                && e.getSource() != getClass()
                && e.getSource() != BasicUploadSettingsPanel.class) {
            switch (e.getKey()) {
                case "osm-server.url":
                    osmServerUrlChanged(e.getNewValue());
                    break;
                default:
                    return;
            }
        }
    }

    private void osmServerUrlChanged(Setting<?> newValue) {
        final String url;
        if (newValue == null || newValue.getValue() == null) {
            url = OsmApi.getOsmApi().getBaseUrl();
        } else {
            url = newValue.getValue().toString();
        }
        setTitle(tr("Upload to ''{0}''", url));
    }

    /* -------------------------------------------------------------------------- */
    /* Interface IUploadDialog                                                    */
    /* -------------------------------------------------------------------------- */
    @Override
    public Map<String, String> getTags(boolean keepEmpty) {
        saveEdits();
        return model.getTags(keepEmpty);
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

    /**
     * Save all outstanding edits to the model.
     * <p>
     * The combobox editors and the tag cell editor need to be manually saved
     * because they normally save on focus loss, eg. when the "Upload" button is
     * pressed, but there's no focus change when Ctrl+Enter is pressed.
     *
     * @since 18173
     */
    public void saveEdits() {
        pnlBasicUploadSettings.saveEdits();
        pnlTagEditor.saveEdits();
    }

    /**
     * Clean dialog state and release resources.
     * @since 14251
     */
    public void clean() {
        setUploadedPrimitives(null);
        dataSet = null;
    }
}
