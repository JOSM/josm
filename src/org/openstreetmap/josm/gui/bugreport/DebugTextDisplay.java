// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import java.awt.Dimension;

import javax.swing.JScrollPane;

import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This is a text area that displays the debug text with scroll bars.
 * @author Michael Zangl
 * @since 10055
 */
public class DebugTextDisplay extends JScrollPane {
    private static final String CODE_PATTERN = "{{{%n%s%n}}}";
    private String text;
    private JosmTextArea textArea;

    /**
     * Creates a new text area.
     * @since 10585
     */
    private DebugTextDisplay() {
        textArea = new JosmTextArea();
        textArea.setCaretPosition(0);
        textArea.setEditable(false);
        setViewportView(textArea);
        setPreferredSize(new Dimension(600, 270));
    }

    /**
     * Creates a new text area with an inital text to display
     * @param textToDisplay The text to display.
     */
    public DebugTextDisplay(String textToDisplay) {
        this();
        setCodeText(textToDisplay);
    }

    /**
     * Creates a new text area that displays the bug report data
     * @param report The bug report data to display.
     * @since 10585
     */
    public DebugTextDisplay(BugReport report) {
        this();
        setCodeText(report.getReportText(ShowStatusReportAction.getReportHeader()));
        report.addChangeListener(e -> setCodeText(report.getReportText(ShowStatusReportAction.getReportHeader())));
    }

    /**
     * Sets the text that should be displayed in this view.
     * @param textToDisplay The text
     */
    private void setCodeText(String textToDisplay) {
        text = Utils.strip(textToDisplay).replaceAll("\r", "");
        textArea.setText(String.format(CODE_PATTERN, text));
        textArea.setCaretPosition(0);
    }

    /**
     * Copies the debug text to the clipboard. This includes the code tags for trac.
     * @return <code>true</code> if copy was successful
     * @since 11102 (typo)
     */
    public boolean copyToClipboard() {
        return ClipboardUtils.copyString(String.format(CODE_PATTERN, text));
    }

    /**
     * Gets the text this are displays, without the code tag.
     * @return The stripped text set by {@link #setCodeText(String)}
     * @since 10585
     */
    public String getCodeText() {
        return text;
    }
}
