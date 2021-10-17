// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.UploadTextComponentValidator.UploadAreaValidator;
import org.openstreetmap.josm.gui.io.UploadTextComponentValidator.UploadCommentValidator;
import org.openstreetmap.josm.gui.io.UploadTextComponentValidator.UploadSourceValidator;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * BasicUploadSettingsPanel allows to enter the basic parameters required for uploading data.
 * @since 2599
 */
public class BasicUploadSettingsPanel extends JPanel implements ActionListener, FocusListener, ItemListener, KeyListener, TableModelListener {
    /**
     * Preference name for the history of comments
     */
    public static final String COMMENT_HISTORY_KEY = "upload.comment.history";
    /**
     * Preference name for last used upload comment
     */
    public static final String COMMENT_LAST_USED_KEY = "upload.comment.last-used";
    /**
     * Preference name for the max age search comments may have
     */
    public static final String COMMENT_MAX_AGE_KEY = "upload.comment.max-age";
    /**
     * Preference name for the history of sources
     */
    public static final String SOURCE_HISTORY_KEY = "upload.source.history";

    /** the history combo box for the upload comment */
    private final HistoryComboBox hcbUploadComment = new HistoryComboBox();
    private final HistoryComboBox hcbUploadSource = new HistoryComboBox();
    private final transient JCheckBox obtainSourceAutomatically = new JCheckBox(
            tr("Automatically obtain source from current layers"));
    /** the panel with a summary of the upload parameters */
    private final UploadParameterSummaryPanel pnlUploadParameterSummary = new UploadParameterSummaryPanel();
    /** the checkbox to request feedback from other users */
    private final JCheckBox cbRequestReview = new JCheckBox(tr("I would like someone to review my edits."));
    private final JLabel areaValidatorFeedback = new JLabel();
    private final UploadAreaValidator areaValidator = new UploadAreaValidator(new JTextField(), areaValidatorFeedback);
    /** the changeset comment model */
    private final transient UploadDialogModel model;
    private final transient JLabel uploadCommentFeedback = new JLabel();
    private final transient UploadCommentValidator uploadCommentValidator = new UploadCommentValidator(
            hcbUploadComment.getEditorComponent(), uploadCommentFeedback);
    private final transient JLabel hcbUploadSourceFeedback = new JLabel();
    private final transient UploadSourceValidator uploadSourceValidator = new UploadSourceValidator(
            hcbUploadSource.getEditorComponent(), hcbUploadSourceFeedback);

    /** a lock to prevent loops in notifications */
    private boolean locked;

    /**
     * Creates the panel
     *
     * @param model The tag editor model.
     *
     * @since 18173 (signature)
     */
    public BasicUploadSettingsPanel(UploadDialogModel model) {
        this.model = model;
        this.model.addTableModelListener(this);
        build();
    }

    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        GBC gbc = GBC.eop().fill(GBC.HORIZONTAL);
        add(buildUploadCommentPanel(), gbc);
        add(buildUploadSourcePanel(), gbc);
        add(pnlUploadParameterSummary, gbc);
        if (Config.getPref().getBoolean("upload.show.review.request", true)) {
            add(cbRequestReview, gbc);
            cbRequestReview.addItemListener(this);
        }
        add(areaValidatorFeedback, gbc);
        add(new JPanel(), GBC.std().fill(GBC.BOTH));
    }

    protected JPanel buildUploadCommentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createTitledBorder(tr("Provide a brief comment for the changes you are uploading:")));

        hcbUploadComment.setToolTipText(tr("Enter an upload comment"));
        hcbUploadComment.getEditorComponent().setMaxTextLength(Changeset.MAX_CHANGESET_TAG_LENGTH);
        JTextField editor = hcbUploadComment.getEditorComponent();
        editor.getDocument().putProperty("tag", "comment");
        editor.addKeyListener(this);
        editor.addFocusListener(this);
        editor.addActionListener(this);
        GBC gbc = GBC.eol().insets(3).fill(GBC.HORIZONTAL);
        pnl.add(hcbUploadComment, gbc);
        pnl.add(uploadCommentFeedback, gbc);
        return pnl;
    }

    protected JPanel buildUploadSourcePanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createTitledBorder(tr("Specify the data source for the changes")));

        JEditorPane obtainSourceOnce = new JMultilineLabel(
                "<html>(<a href=\"urn:changeset-source\">" + tr("just once") + "</a>)</html>");
        obtainSourceOnce.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                saveEdits();
                model.put("source", getSourceFromLayer());
            }
        });
        obtainSourceAutomatically.setSelected(Config.getPref().getBoolean("upload.source.obtainautomatically", false));
        obtainSourceAutomatically.addActionListener(e -> {
            if (obtainSourceAutomatically.isSelected()) {
                model.put("source", getSourceFromLayer());
            }
            obtainSourceOnce.setVisible(!obtainSourceAutomatically.isSelected());
        });
        JPanel obtainSource = new JPanel(new GridBagLayout());
        obtainSource.add(obtainSourceAutomatically, GBC.std().anchor(GBC.WEST));
        obtainSource.add(obtainSourceOnce, GBC.std().anchor(GBC.WEST));
        obtainSource.add(new JLabel(), GBC.eol().fill(GBC.HORIZONTAL));

        hcbUploadSource.setToolTipText(tr("Enter a source"));
        hcbUploadSource.getEditorComponent().setMaxTextLength(Changeset.MAX_CHANGESET_TAG_LENGTH);
        JTextField editor = hcbUploadSource.getEditorComponent();
        editor.getDocument().putProperty("tag", "source");
        editor.addKeyListener(this);
        editor.addFocusListener(this);
        editor.addActionListener(this);
        GBC gbc = GBC.eol().insets(3).fill(GBC.HORIZONTAL);
        if (Config.getPref().getBoolean("upload.show.automatic.source", true)) {
            pnl.add(obtainSource, gbc);
        }
        pnl.add(hcbUploadSource, gbc);
        pnl.add(hcbUploadSourceFeedback, gbc);
        return pnl;
    }

    /**
     * Initializes this life cycle of the panel.
     *
     * Adds the comment and source tags from history, and/or obtains the source from the layer if
     * the user said so.
     *
     * @param map Map where tags are added to.
     * @since 18173
     */
    public void initLifeCycle(Map<String, String> map) {
        Optional.ofNullable(getLastChangesetTagFromHistory(COMMENT_HISTORY_KEY, new ArrayList<>())).ifPresent(
                x -> map.put("comment", x));
        Optional.ofNullable(getLastChangesetTagFromHistory(SOURCE_HISTORY_KEY, getDefaultSources())).ifPresent(
                x -> map.put("source", x));
        if (obtainSourceAutomatically.isSelected()) {
            map.put("source", getSourceFromLayer());
        }
        hcbUploadComment.getModel().prefs().load(COMMENT_HISTORY_KEY);
        hcbUploadComment.discardAllUndoableEdits();
        hcbUploadSource.getModel().prefs().load(SOURCE_HISTORY_KEY, getDefaultSources());
        hcbUploadSource.discardAllUndoableEdits();
        hcbUploadComment.getEditorComponent().requestFocusInWindow();
        uploadCommentValidator.validate();
        uploadSourceValidator.validate();
    }

    /**
     * Get a key's value from the model.
     * @param key The key
     * @return The value or ""
     * @since 18173
     */
    private String get(String key) {
        TagModel tm = model.get(key);
        return tm == null ? "" : tm.getValue();
    }

    /**
     * Get the topmost item from the history if not expired.
     *
     * @param historyKey The preferences key.
     * @param def A default history.
     * @return The history item (may be null).
     * @since 18173 (signature)
     */
    public static String getLastChangesetTagFromHistory(String historyKey, List<String> def) {
        Collection<String> history = Config.getPref().getList(historyKey, def);
        long age = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - getHistoryLastUsedKey();
        if (age < getHistoryMaxAgeKey() && !history.isEmpty()) {
            return history.iterator().next();
        }
        return null;
    }

    /**
     * Add the "source" tag
     * @return The source from the layer info.
     */
    private String getSourceFromLayer() {
        String source = MainApplication.getMap().mapView.getLayerInformationForSourceTag();
        return Utils.shortenString(source, Changeset.MAX_CHANGESET_TAG_LENGTH);
    }

    /**
     * Returns the default list of sources.
     * @return the default list of sources
     */
    public static List<String> getDefaultSources() {
        return Arrays.asList("knowledge", "survey", "Bing");
    }

    /**
     * Returns the list of {@link UploadTextComponentValidator} defined by this panel.
     * @return the list of {@code UploadTextComponentValidator} defined by this panel.
     * @since 17238
     */
    protected List<UploadTextComponentValidator> getUploadTextValidators() {
        return Arrays.asList(areaValidator, uploadCommentValidator, uploadSourceValidator);
    }

    /**
     * Remembers the user input in the preference settings
     */
    public void rememberUserInput() {
        // store the history of comments
        if (getHistoryMaxAgeKey() > 0) {
            hcbUploadComment.addCurrentItemToHistory();
            hcbUploadComment.getModel().prefs().save(COMMENT_HISTORY_KEY);
            Config.getPref().putLong(COMMENT_LAST_USED_KEY, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        }
        // store the history of sources
        hcbUploadSource.addCurrentItemToHistory();
        hcbUploadSource.getModel().prefs().save(SOURCE_HISTORY_KEY);

        // store current value of obtaining source automatically
        Config.getPref().putBoolean("upload.source.obtainautomatically", obtainSourceAutomatically.isSelected());
    }

    /**
     * Initializes editing of upload comment.
     */
    public void initEditingOfUploadComment() {
        hcbUploadComment.getEditor().selectAll();
        hcbUploadComment.requestFocusInWindow();
    }

    /**
     * Initializes editing of upload source.
     */
    public void initEditingOfUploadSource() {
        hcbUploadSource.getEditor().selectAll();
        hcbUploadSource.requestFocusInWindow();
    }

    void setUploadedPrimitives(List<OsmPrimitive> primitives) {
        areaValidator.computeArea(primitives);
    }

    /**
     * Returns the panel that displays a summary of data the user is about to upload.
     * @return the upload parameter summary panel
     */
    public UploadParameterSummaryPanel getUploadParameterSummaryPanel() {
        return pnlUploadParameterSummary;
    }

    static long getHistoryMaxAgeKey() {
        return Config.getPref().getLong(COMMENT_MAX_AGE_KEY, TimeUnit.HOURS.toSeconds(4));
    }

    static long getHistoryLastUsedKey() {
        return Config.getPref().getLong(COMMENT_LAST_USED_KEY, 0);
    }

    /**
     * Updates the combobox histories when a combobox editor loses focus.
     *
     * @param text The {@code JTextField} of the combobox editor.
     */
    private void updateHistory(JTextField text) {
        String tag = (String) text.getDocument().getProperty("tag"); // tag is either "comment" or "source"
        if ("comment".equals(tag)) {
            hcbUploadComment.addCurrentItemToHistory();
        } else if ("source".equals(tag)) {
            hcbUploadSource.addCurrentItemToHistory();
        }
    }

    /**
     * Updates the table editor model with changes in the comboboxes.
     *
     * The lock prevents loops in change notifications, eg. the combobox
     * notifies the table model and the table model notifies the combobox, which
     * throws IllegalStateException.
     *
     * @param text The {@code JTextField} of the combobox editor.
     */
    private void updateModel(JTextField text) {
        if (!locked) {
            locked = true;
            try {
                String tag = (String) text.getDocument().getProperty("tag"); // tag is either "comment" or "source"
                String value = text.getText();
                model.put(tag, value.isEmpty() ? null : value); // remove tags with empty values
            } finally {
                locked = false;
            }
        }
    }

    /**
     * Save all outstanding edits to the model.
     * @see UploadDialog#saveEdits
     * @since 18173
     */
    public void saveEdits() {
        updateModel(hcbUploadComment.getEditorComponent());
        hcbUploadComment.addCurrentItemToHistory();
        updateModel(hcbUploadSource.getEditorComponent());
        hcbUploadSource.addCurrentItemToHistory();
    }

    /**
     * Returns the UplodDialog that is our ancestor
     *
     * @return the UploadDialog or null
     */
    private UploadDialog getDialog() {
        Component d = getRootPane();
        while ((d = d.getParent()) != null) {
            if (d instanceof UploadDialog)
                return (UploadDialog) d;
        }
        return null;
    }

    /**
     * Update the model when the selection changes in a combobox.
     * @param e The action event.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        setFocusToUploadButton();
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    /**
     * Update the model and combobox history when a combobox editor loses focus.
     */
    @Override
    public void focusLost(FocusEvent e) {
        Object c = e.getSource();
        if (c instanceof JTextField) {
            updateModel((JTextField) c);
            updateHistory((JTextField) c);
        }
    }

    /**
     * Updates the table editor model upon changes in the "review" checkbox.
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (!locked) {
            locked = true;
            try {
                model.put("review_requested", e.getStateChange() == ItemEvent.SELECTED ? "yes" : null);
            } finally {
                locked = false;
            }
        }
    }

    /**
     * Updates the controls upon changes in the table editor model.
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        if (!locked) {
            locked = true;
            try {
                hcbUploadComment.setText(get("comment"));
                hcbUploadSource.setText(get("source"));
                cbRequestReview.setSelected(get("review_requested").equals("yes"));
            } finally {
                locked = false;
            }
        }
    }

    /**
     * Set the focus directly to the upload button if "Enter" key is pressed in any combobox.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
            setFocusToUploadButton();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void setFocusToUploadButton() {
        Optional.ofNullable(getDialog()).ifPresent(UploadDialog::setFocusToUploadButton);
    }
}
