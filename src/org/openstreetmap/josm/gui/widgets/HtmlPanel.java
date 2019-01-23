// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Font;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.StyleSheet;

import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * This panel can be used to display larger sections of formatted text in
 * HTML.
 *
 * It displays HTML text in the same font as {@link javax.swing.JLabel}. Hyperlinks are rendered in
 * blue and they are underlined. There is also a CSS rule for the HTML tag &lt;strong&gt;
 * configured.
 * @since 2688
 */
public class HtmlPanel extends JPanel {

    private static final HyperlinkListener defaultHyperlinkListener = e -> {
        if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType()) && e.getURL() != null) {
            OpenBrowser.displayUrl(e.getURL().toString());
        }
    };

    private JosmEditorPane jepMessage;

    protected final void build() {
        setLayout(new BorderLayout());
        jepMessage = new JosmEditorPane("text/html", "");
        jepMessage.setOpaque(false);
        jepMessage.setEditable(false);
        Font f = UIManager.getFont("Label.font");
        StyleSheet ss = new StyleSheet();
        ss.addRule("body {" + MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                f.isBold() ? "bold" : "normal",
                f.isItalic() ? "italic" : "normal"
        ) + '}');
        ss.addRule("strong {" + MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                "bold",
                f.isItalic() ? "italic" : "normal"
        ) + '}');
        ss.addRule("a {text-decoration: underline; color: blue}");
        ss.addRule("ul {margin-left: 1cm; list-style-type: disc}");
        JosmHTMLEditorKit kit = new JosmHTMLEditorKit();
        kit.setStyleSheet(ss);
        jepMessage.setEditorKit(kit);

        add(jepMessage, BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code HtmlPanel}.
     */
    public HtmlPanel() {
        build();
    }

    /**
     * Constructs a new {@code HtmlPanel} with the given HTML text.
     * @param text the text to display
     */
    public HtmlPanel(String text) {
        this();
        setText(text);
    }

    /**
     * Replies the editor pane used internally to render the HTML text.
     *
     * @return the editor pane used internally to render the HTML text.
     */
    public JEditorPane getEditorPane() {
        return jepMessage;
    }

    /**
     * Sets the current text to display. <code>text</code> is a html fragment.
     * If null, empty string is assumed.
     *
     * @param text the text to display
     */
    public final void setText(String text) {
        jepMessage.setText(Optional.ofNullable(text).orElse(""));
    }

    /**
     * Opens hyperlinks on click.
     * @since 13111
     */
    public final void enableClickableHyperlinks() {
        if (!Arrays.asList(jepMessage.getHyperlinkListeners()).contains(defaultHyperlinkListener)) {
            jepMessage.addHyperlinkListener(defaultHyperlinkListener);
        }
    }
}
