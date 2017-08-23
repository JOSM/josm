// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * BasicUploadSettingsPanel allows to enter the basic parameters required for uploading data.
 * @since 2599
 */
public class BasicUploadSettingsPanel extends JPanel {
    /**
     * Preference name for history collection
     */
    public static final String HISTORY_KEY = "upload.comment.history";
    /**
     * Preference name for last used upload comment
     */
    public static final String HISTORY_LAST_USED_KEY = "upload.comment.last-used";
    /**
     * Preference name for the max age search comments may have
     */
    public static final String HISTORY_MAX_AGE_KEY = "upload.comment.max-age";
    /**
     * Preference name for the history of source values
     */
    public static final String SOURCE_HISTORY_KEY = "upload.source.history";

    /** the history combo box for the upload comment */
    private final HistoryComboBox hcbUploadComment = new HistoryComboBox();
    private final HistoryComboBox hcbUploadSource = new HistoryComboBox();
    /** the panel with a summary of the upload parameters */
    private final UploadParameterSummaryPanel pnlUploadParameterSummary = new UploadParameterSummaryPanel();
    /** the changeset comment model */
    private final transient ChangesetCommentModel changesetCommentModel;
    private final transient ChangesetCommentModel changesetSourceModel;

    protected JPanel buildUploadCommentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        JEditorPane commentLabel = new JMultilineLabel("<html><b>" + tr("Provide a brief comment for the changes you are uploading:"));
        pnl.add(commentLabel, GBC.eol().insets(0, 5, 10, 3).fill(GBC.HORIZONTAL));
        hcbUploadComment.setToolTipText(tr("Enter an upload comment"));
        hcbUploadComment.setMaxTextLength(Changeset.MAX_CHANGESET_TAG_LENGTH);
        List<String> cmtHistory = new LinkedList<>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        Collections.reverse(cmtHistory); // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        hcbUploadComment.setPossibleItems(cmtHistory);
        CommentModelListener commentModelListener = new CommentModelListener(hcbUploadComment, changesetCommentModel);
        hcbUploadComment.getEditor().addActionListener(commentModelListener);
        hcbUploadComment.getEditorComponent().addFocusListener(commentModelListener);
        pnl.add(hcbUploadComment, GBC.eol().fill(GBC.HORIZONTAL));

        JEditorPane sourceLabel = new JMultilineLabel("<html><b>" + tr("Specify the data source for the changes")
                + "</b> (<a href=\"urn:changeset-source\">" + tr("obtain from current layers") + "</a>)<b>:</b>");
        sourceLabel.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                final String source = MainApplication.getMap().mapView.getLayerInformationForSourceTag();
                hcbUploadSource.setText(Utils.shortenString(source, Changeset.MAX_CHANGESET_TAG_LENGTH));
                // Fix #9965
                changesetSourceModel.setComment(hcbUploadSource.getText());
            }
        });
        pnl.add(sourceLabel, GBC.eol().insets(0, 8, 10, 3).fill(GBC.HORIZONTAL));

        hcbUploadSource.setToolTipText(tr("Enter a source"));
        hcbUploadSource.setMaxTextLength(Changeset.MAX_CHANGESET_TAG_LENGTH);
        List<String> sourceHistory = new LinkedList<>(Main.pref.getCollection(SOURCE_HISTORY_KEY, getDefaultSources()));
        Collections.reverse(sourceHistory); // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        hcbUploadSource.setPossibleItems(sourceHistory);
        CommentModelListener sourceModelListener = new CommentModelListener(hcbUploadSource, changesetSourceModel);
        hcbUploadSource.getEditor().addActionListener(sourceModelListener);
        hcbUploadSource.getEditorComponent().addFocusListener(sourceModelListener);
        pnl.add(hcbUploadSource, GBC.eol().fill(GBC.HORIZONTAL));
        return pnl;
    }

    /**
     * Returns the default list of sources.
     * @return the default list of sources
     */
    public static List<String> getDefaultSources() {
        return Arrays.asList("knowledge", "survey", "Bing");
    }

    protected void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        add(buildUploadCommentPanel(), BorderLayout.NORTH);
        add(pnlUploadParameterSummary, BorderLayout.CENTER);
    }

    /**
     * Creates the panel
     *
     * @param changesetCommentModel the model for the changeset comment. Must not be null
     * @param changesetSourceModel the model for the changeset source. Must not be null.
     * @throws IllegalArgumentException if {@code changesetCommentModel} is null
     */
    public BasicUploadSettingsPanel(ChangesetCommentModel changesetCommentModel, ChangesetCommentModel changesetSourceModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        CheckParameterUtil.ensureParameterNotNull(changesetSourceModel, "changesetSourceModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetSourceModel = changesetSourceModel;
        changesetCommentModel.addChangeListener(new ChangesetCommentChangeListener(hcbUploadComment));
        changesetSourceModel.addChangeListener(new ChangesetCommentChangeListener(hcbUploadSource));
        build();
    }

    public void setUploadTagDownFocusTraversalHandlers(final Action handler) {
        setHistoryComboBoxDownFocusTraversalHandler(handler, hcbUploadComment);
        setHistoryComboBoxDownFocusTraversalHandler(handler, hcbUploadSource);
    }

    public void setHistoryComboBoxDownFocusTraversalHandler(final Action handler, final HistoryComboBox hcb) {
        hcb.getEditor().addActionListener(handler);
        hcb.getEditorComponent().addKeyListener(new HistoryComboBoxKeyAdapter(hcb, handler));
    }

    /**
     * Remembers the user input in the preference settings
     */
    public void rememberUserInput() {
        // store the history of comments
        hcbUploadComment.addCurrentItemToHistory();
        Main.pref.putCollection(HISTORY_KEY, hcbUploadComment.getHistory());
        Main.pref.putInteger(HISTORY_LAST_USED_KEY, (int) (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        // store the history of sources
        hcbUploadSource.addCurrentItemToHistory();
        Main.pref.putCollection(SOURCE_HISTORY_KEY, hcbUploadSource.getHistory());
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        hcbUploadComment.requestFocusInWindow();
        hcbUploadComment.getEditorComponent().requestFocusInWindow();
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

    public UploadParameterSummaryPanel getUploadParameterSummaryPanel() {
        return pnlUploadParameterSummary;
    }

    static final class HistoryComboBoxKeyAdapter extends KeyAdapter {
        private final HistoryComboBox hcb;
        private final Action handler;

        HistoryComboBoxKeyAdapter(HistoryComboBox hcb, Action handler) {
            this.hcb = hcb;
            this.handler = handler;
        }

        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                handler.actionPerformed(new ActionEvent(hcb, 0, "focusDown"));
            }
        }
    }

    /**
     * Updates the changeset comment model upon changes in the input field.
     */
    static class CommentModelListener extends FocusAdapter implements ActionListener {

        private final HistoryComboBox source;
        private final ChangesetCommentModel destination;

        CommentModelListener(HistoryComboBox source, ChangesetCommentModel destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            destination.setComment(source.getText());
        }

        @Override
        public void focusLost(FocusEvent e) {
            destination.setComment(source.getText());
        }
    }

    /**
     * Observes the changeset comment model and keeps the comment input field
     * in sync with the current changeset comment
     */
    static class ChangesetCommentChangeListener implements ChangeListener {

        private final HistoryComboBox destination;

        ChangesetCommentChangeListener(HistoryComboBox destination) {
            this.destination = destination;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!(e.getSource() instanceof ChangesetCommentModel)) return;
            String newComment = ((ChangesetCommentModel) e.getSource()).getComment();
            if (!destination.getText().equals(newComment)) {
                destination.setText(newComment);
            }
        }
    }
}
