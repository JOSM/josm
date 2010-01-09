// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Font;
import java.text.MessageFormat;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * This panel can be used to display larger larger sections of formatted text in
 * HTML.
 *
 * It displays HTML text in the same font as {@see JLabel}. Hyperlinks are rendered in
 * blue and they are underlined. There is also a CSS rule for the HTML tag &lt;strong&gt;
 * configured.
 *
 */
public class HtmlPanel extends JPanel {
    private JEditorPane jepMessage;

    protected void build() {
        setLayout(new BorderLayout());
        jepMessage = new JEditorPane("text/html", "");
        jepMessage.setOpaque(false);
        jepMessage.setEditable(false);
        Font f = UIManager.getFont("Label.font");
        StyleSheet ss = new StyleSheet();
        String rule = MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                f.isBold() ? "bold" : "normal",
                        f.isItalic() ? "italic" : "normal"
        );
        rule = "body {" + rule + "}";
        rule = MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                "bold",
                f.isItalic() ? "italic" : "normal"
        );
        rule = "strong {" + rule + "}";
        ss.addRule(rule);
        ss.addRule("a {text-decoration: underline; color: blue}");
        ss.addRule("ul {margin-left: 1cm; list-style-type: disc}");
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.setStyleSheet(ss);
        jepMessage.setEditorKit(kit);

        add(jepMessage, BorderLayout.CENTER);
    }

    public HtmlPanel() {
        build();
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
    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        jepMessage.setText(text);
    }
}
