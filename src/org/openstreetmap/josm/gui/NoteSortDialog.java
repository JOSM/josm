// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.Comparator;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.tools.Logging;

/**
 * A dialog to allow the user to choose a sorting method for the list of notes
 */
public class NoteSortDialog extends ExtendedDialog {

    private final JRadioButton defaultSort = new JRadioButton(tr("Default (open, closed, new)"));
    private final JRadioButton userSort = new JRadioButton(tr("Username"));
    private final JRadioButton dateSort = new JRadioButton(tr("Created date"));
    private final JRadioButton lastActionSort = new JRadioButton(tr("Last change date"));

    /**
     * Construct a new dialog. The constructor automatically adds a "Cancel" button.
     * @param parent - Parent component. Usually Main.parent
     * @param title - Translated text to display in the title bar of the dialog
     * @param buttonText - Translated text to be shown on the action button
     */
    public NoteSortDialog(Component parent, String title, String buttonText) {
        super(parent, title, buttonText, tr("Cancel"));
    }

    /**
     * Builds and displays the window to the user.
     * @param currentSortMode - The current sort mode which will be pre-selected in the list
     */
    public void showSortDialog(Comparator<Note> currentSortMode) {
        JLabel label = new JLabel(tr("Select note sorting method"));
        if (currentSortMode == Note.DEFAULT_COMPARATOR) {
            defaultSort.setSelected(true);
        } else if (currentSortMode == Note.DATE_COMPARATOR) {
            dateSort.setSelected(true);
        } else if (currentSortMode == Note.USER_COMPARATOR) {
            userSort.setSelected(true);
        } else if (currentSortMode == Note.LAST_ACTION_COMPARATOR) {
            lastActionSort.setSelected(true);
        } else {
            Logging.warn("sort mode not recognized");
        }

        ButtonGroup bg = new ButtonGroup();
        bg.add(defaultSort);
        bg.add(userSort);
        bg.add(dateSort);
        bg.add(lastActionSort);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(label);
        panel.add(defaultSort);
        panel.add(userSort);
        panel.add(dateSort);
        panel.add(lastActionSort);

        setContent(panel);

        showDialog();
    }

    /**
     * Returns the Note comparator that the user has selected.
     * @return Note comparator that the user has selected
     */
    public Comparator<Note> getSelectedComparator() {
        if (dateSort.isSelected()) {
            return Note.DATE_COMPARATOR;
        } else if (userSort.isSelected()) {
            return Note.USER_COMPARATOR;
        } else if (lastActionSort.isSelected()) {
            return Note.LAST_ACTION_COMPARATOR;
        } else {
            return Note.DEFAULT_COMPARATOR;
        }
    }
}
