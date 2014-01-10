// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;

/**
 * BasicUploadSettingsPanel allows to enter the basic parameters required for uploading
 * data.
 *
 */
public class BasicUploadSettingsPanel extends JPanel {
    public static final String HISTORY_KEY = "upload.comment.history";
    public static final String HISTORY_LAST_USED_KEY = "upload.comment.last-used";
    public static final String HISTORY_MAX_AGE_KEY = "upload.comment.max-age";
    public static final String SOURCE_HISTORY_KEY = "upload.source.history";

    /** the history combo box for the upload comment */
    private final HistoryComboBox hcbUploadComment = new HistoryComboBox();
    private final HistoryComboBox hcbUploadSource = new HistoryComboBox();
    /** the panel with a summary of the upload parameters */
    private final UploadParameterSummaryPanel pnlUploadParameterSummary = new UploadParameterSummaryPanel();
    /** the changeset comment model */
    private final ChangesetCommentModel changesetCommentModel;
    private final ChangesetCommentModel changesetSourceModel;

    protected JPanel buildUploadCommentPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        final JEditorPane commentLabel = JosmEditorPane.createJLabelLikePane();
        commentLabel.setText("<html><b>" + tr("Provide a brief comment for the changes you are uploading:"));
        pnl.add(commentLabel, GBC.eol().insets(0, 5, 10, 3).fill(GBC.HORIZONTAL));
        hcbUploadComment.setToolTipText(tr("Enter an upload comment"));
        hcbUploadComment.setMaxTextLength(Changeset.MAX_COMMENT_LENGTH);
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        Collections.reverse(cmtHistory); // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        hcbUploadComment.setPossibleItems(cmtHistory);
        final CommentModelListener commentModelListener = new CommentModelListener(hcbUploadComment, changesetCommentModel);
        hcbUploadComment.getEditor().addActionListener(commentModelListener);
        hcbUploadComment.getEditor().getEditorComponent().addFocusListener(commentModelListener);
        pnl.add(hcbUploadComment, GBC.eol().fill(GBC.HORIZONTAL));

        final JEditorPane sourceLabel = JosmEditorPane.createJLabelLikePane();
        sourceLabel.setText("<html><b>" + tr("Specify the data source for the changes")
                + "</b> (<a href=\"urn:changeset-source\">" + tr("obtain from current layers") + "</a>)<b>:</b>");
        sourceLabel.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                    hcbUploadSource.setText(Main.map.mapView.getLayerInformationForSourceTag());
                }
            }
        });
        pnl.add(sourceLabel, GBC.eol().insets(0, 8, 10, 3).fill(GBC.HORIZONTAL));

        hcbUploadSource.setToolTipText(tr("Enter a source"));
        List<String> sourceHistory = new LinkedList<String>(Main.pref.getCollection(SOURCE_HISTORY_KEY, Arrays.asList("knowledge", "survey", "Bing")));
        Collections.reverse(sourceHistory); // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        hcbUploadSource.setPossibleItems(sourceHistory);
        final CommentModelListener sourceModelListener = new CommentModelListener(hcbUploadSource, changesetSourceModel);
        hcbUploadSource.getEditor().addActionListener(sourceModelListener);
        hcbUploadSource.getEditor().getEditorComponent().addFocusListener(sourceModelListener);
        pnl.add(hcbUploadSource, GBC.eol().fill(GBC.HORIZONTAL));
        return pnl;
    }

    protected void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        add(buildUploadCommentPanel(), BorderLayout.NORTH);
        add(pnlUploadParameterSummary, BorderLayout.CENTER);
    }

    /**
     * Creates the panel
     *
     * @param changesetCommentModel the model for the changeset comment. Must not be null
     * @param changesetSourceModel the model for the changeset source. Must not be null.
     * @throws IllegalArgumentException thrown if {@code changesetCommentModel} is null
     */
    public BasicUploadSettingsPanel(ChangesetCommentModel changesetCommentModel, ChangesetCommentModel changesetSourceModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        CheckParameterUtil.ensureParameterNotNull(changesetSourceModel, "changesetSourceModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetSourceModel = changesetSourceModel;
        changesetCommentModel.addObserver(new ChangesetCommentObserver(hcbUploadComment));
        changesetSourceModel.addObserver(new ChangesetCommentObserver(hcbUploadSource));
        build();
    }

    public void setUploadTagDownFocusTraversalHandlers(final Action handler) {
        setHistoryComboBoxDownFocusTraversalHandler(handler, hcbUploadComment);
        setHistoryComboBoxDownFocusTraversalHandler(handler, hcbUploadSource);
    }

    public void setHistoryComboBoxDownFocusTraversalHandler(final Action handler, final HistoryComboBox hcb) {
        hcb.getEditor().addActionListener(handler);
        hcb.getEditor().getEditorComponent().addKeyListener(
                new KeyListener() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_TAB) {
                            handler.actionPerformed(new ActionEvent(hcb, 0, "focusDown"));
                        }
                    }
                    @Override
                    public void keyReleased(KeyEvent e) {}

                    @Override
                    public void keyPressed(KeyEvent e) {}
                }
        );
    }

    /**
     * Remembers the user input in the preference settings
     */
    public void rememberUserInput() {
        // store the history of comments
        hcbUploadComment.addCurrentItemToHistory();
        Main.pref.putCollection(HISTORY_KEY, hcbUploadComment.getHistory());
        Main.pref.putInteger(HISTORY_LAST_USED_KEY, (int) (System.currentTimeMillis() / 1000));
        // store the history of sources
        hcbUploadSource.addCurrentItemToHistory();
        Main.pref.putCollection(SOURCE_HISTORY_KEY, hcbUploadSource.getHistory());
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        hcbUploadComment.requestFocusInWindow();
        hcbUploadComment.getEditor().getEditorComponent().requestFocusInWindow();
    }

    public void initEditingOfUploadComment() {
        hcbUploadComment.getEditor().selectAll();
        hcbUploadComment.requestFocusInWindow();
    }

    public UploadParameterSummaryPanel getUploadParameterSummaryPanel() {
        return pnlUploadParameterSummary;
    }

    /**
     * Updates the changeset comment model upon changes in the input field.
     */
    static class CommentModelListener extends FocusAdapter implements ActionListener {

        final HistoryComboBox source;
        final ChangesetCommentModel destination;

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
    static class ChangesetCommentObserver implements Observer {

        private final HistoryComboBox destination;

        ChangesetCommentObserver(HistoryComboBox destination) {
            this.destination = destination;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (!(o instanceof ChangesetCommentModel)) return;
            String newComment = (String)arg;
            if (!destination.getText().equals(newComment)) {
                destination.setText(newComment);
            }
        }
    }
}
