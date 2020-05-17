// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Class to show user input dialog for notes. It sets up a
 * simple label and text area to prompt for user input.
 * @since 7720
 */
public class NoteInputDialog extends ExtendedDialog {

    private final JosmTextArea textArea = new JosmTextArea();

    /**
     * Construct the dialog with a title and button text. A cancel button is
     * automatically added
     * @param parent The parent GUI element
     * @param title Translated string to display in the dialog's title bar
     * @param buttonText Translated string to display on the action button
     */
    public NoteInputDialog(Component parent, String title, String buttonText) {
        super(parent, title, buttonText, tr("Cancel"));
        setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(MainApplication.getMainFrame(), new Dimension(400, 300)));
    }

    /**
     * Displays the dialog to the user
     * @param message Translated message to display to the user as input prompt
     * @param icon Icon to display in the action button
     */
    public void showNoteDialog(String message, Icon icon) {
        textArea.setRows(6);
        textArea.setColumns(30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT); //without this the label gets pushed to the right

        JLabel label = new JLabel(message);
        label.setLabelFor(textArea);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(label);
        contentPanel.add(scrollPane);
        setContent(contentPanel, false);
        setButtonIcons(icon, ImageProvider.get("cancel"));

        showDialog();
    }

    /** Get the content of the text area
     * @return Text input by user
     */
    public String getInputText() {
        return textArea.getText();
    }
}
