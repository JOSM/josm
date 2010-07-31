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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
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

    /** the history combo box for the upload comment */
    private HistoryComboBox hcbUploadComment;
    /** the panel with a summary of the upload parameters */
    private UploadParameterSummaryPanel pnlUploadParameterSummary;
    /** the changset comment model */
    private ChangesetCommentModel changesetCommentModel;

    protected JPanel buildUploadCommentPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        pnl.add(new JLabel(tr("Provide a brief comment for the changes you are uploading:")), GBC.eol().insets(0, 5, 10, 3));
        hcbUploadComment = new HistoryComboBox();
        hcbUploadComment.setToolTipText(tr("Enter an upload comment"));
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        // we have to reverse the history, because ComboBoxHistory will reverse it again
        // in addElement()
        //
        Collections.reverse(cmtHistory);
        hcbUploadComment.setPossibleItems(cmtHistory);
        hcbUploadComment.getEditor().addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        changesetCommentModel.setComment(hcbUploadComment.getText());
                    }
                }
        );
        hcbUploadComment.getEditor().getEditorComponent().addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        changesetCommentModel.setComment(hcbUploadComment.getText());
                    }
                }
        );
        pnl.add(hcbUploadComment, GBC.eol().fill(GBC.HORIZONTAL));
        return pnl;
    }

    protected void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        add(buildUploadCommentPanel(), BorderLayout.NORTH);
        add(pnlUploadParameterSummary = new UploadParameterSummaryPanel(), BorderLayout.CENTER);
    }

    /**
     * Creates the panel
     * 
     * @param changesetCommentModel the model for the changeset comment. Must not be null
     * @throws IllegalArgumentException thrown if {@code changesetCommentModel} is null
     */
    public BasicUploadSettingsPanel(ChangesetCommentModel changesetCommentModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        this.changesetCommentModel = changesetCommentModel;
        changesetCommentModel.addObserver(new ChangesetCommentObserver());
        build();
    }

    public void setUploadCommentDownFocusTraversalHandler(final Action handler) {
        hcbUploadComment.getEditor().addActionListener(handler);
        hcbUploadComment.getEditor().getEditorComponent().addKeyListener(
                new KeyListener() {
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_TAB) {
                            handler.actionPerformed(new ActionEvent(hcbUploadComment,0, "focusDown"));
                        }
                    }
                    public void keyReleased(KeyEvent e) {}

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
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        List<String> history = hcbUploadComment.getHistory();
        int age = (int) (System.currentTimeMillis()/1000 - Main.pref.getInteger(HISTORY_LAST_USED_KEY, 0));
        // only pre-select latest entry if used less than 4 hours ago.
        if (age < 4 * 3600 * 1000 && history != null && !history.isEmpty()) {
            hcbUploadComment.setText(history.get(0));
        }
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
     * Observes the changeset comment model and keeps the comment input field
     * in sync with the current changeset comment
     */
    class ChangesetCommentObserver implements Observer {
        public void update(Observable o, Object arg) {
            if (!(o instanceof ChangesetCommentModel)) return;
            String newComment = (String)arg;
            if (!hcbUploadComment.getText().equals(newComment)) {
                hcbUploadComment.setText(newComment);
            }
        }
    }
}
