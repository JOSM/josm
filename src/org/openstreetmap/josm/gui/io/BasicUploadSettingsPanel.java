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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;

/**
 * BasicUploadSettingsPanel allows to enter the basic parameters required for uploading
 * data.
 *
 */
public class BasicUploadSettingsPanel extends JPanel implements PropertyChangeListener{
    static public final String UPLOAD_COMMENT_PROP = BasicUploadSettingsPanel.class.getName() + ".uploadComment";
    public static final String HISTORY_KEY = "upload.comment.history";

    /** the history combo box for the upload comment */
    private HistoryComboBox hcbUploadComment;
    /** the panel with a summary of the upload parameters */
    private UploadParameterSummaryPanel pnlUploadParameterSummary;

    protected JPanel buildUploadCommentPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        pnl.add(new JLabel(tr("Provide a brief comment for the changes you are uploading:")), GBC.eol().insets(0, 5, 10, 3));
        hcbUploadComment = new HistoryComboBox();
        hcbUploadComment.setToolTipText(tr("Enter an upload comment (min. 3 characters)"));
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        // we have to reverse the history, because ComboBoxHistory will reverse it again
        // in addElement()
        //
        Collections.reverse(cmtHistory);
        hcbUploadComment.setPossibleItems(cmtHistory);
        hcbUploadComment.getEditor().addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        firePropertyChange(UPLOAD_COMMENT_PROP, null, hcbUploadComment.getText());
                    }
                }
        );
        hcbUploadComment.getEditor().getEditorComponent().addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        firePropertyChange(UPLOAD_COMMENT_PROP, null, hcbUploadComment.getText());
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

    public BasicUploadSettingsPanel() {
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
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        List<String> history = hcbUploadComment.getHistory();
        if (history != null && !history.isEmpty()) {
            hcbUploadComment.setText(history.get(0));
        }
        hcbUploadComment.requestFocusInWindow();
        hcbUploadComment.getEditor().getEditorComponent().requestFocusInWindow();
    }

    /**
     * Replies the current upload comment
     *
     * @return
     */
    public String getUploadComment() {
        return hcbUploadComment.getText();
    }

    /**
     * Sets the current upload comment
     *
     * @return
     */
    public void setUploadComment(String uploadComment) {
        if (uploadComment == null) {
            uploadComment = "";
        }
        if (!uploadComment.equals(hcbUploadComment.getText())) {
            hcbUploadComment.setText(uploadComment);
        }
    }

    public void initEditingOfUploadComment(String comment) {
        setUploadComment(comment);
        hcbUploadComment.getEditor().selectAll();
        hcbUploadComment.requestFocusInWindow();
    }

    public UploadParameterSummaryPanel getUploadParameterSummaryPanel() {
        return pnlUploadParameterSummary;
    }

    /* -------------------------------------------------------------------------- */
    /* Interface PropertyChangeListener                                           */
    /* -------------------------------------------------------------------------- */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TagSettingsPanel.UPLOAD_COMMENT_PROP)) {
            String comment = (String)evt.getNewValue();
            if (comment == null) {
                comment = "";
            }
            if (comment.equals(hcbUploadComment.getText()))
                // nothing to change - return
                return;
            hcbUploadComment.setText(comment);
        }
    }
}
