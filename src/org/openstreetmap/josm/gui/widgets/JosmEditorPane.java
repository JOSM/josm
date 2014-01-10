// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;

import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.text.html.StyleSheet;

import org.openstreetmap.josm.tools.Utils;

/**
 * Subclass of {@link JEditorPane} that adds a "native" context menu (cut/copy/paste/select all)
 * and effectively uses JOSM user agent when performing HTTP request in {@link #setPage(URL)} method.
 * @since 5886
 */
public class JosmEditorPane extends JEditorPane {

    /**
     * Creates a new <code>JosmEditorPane</code>.
     * The document model is set to <code>null</code>.
     */
    public JosmEditorPane() {
        TextContextualPopupMenu.enableMenuFor(this);
    }

    /**
     * Creates a <code>JosmEditorPane</code> based on a specified URL for input.
     *
     * @param initialPage the URL
     * @exception IOException if the URL is <code>null</code> or cannot be accessed
     */
    public JosmEditorPane(URL initialPage) throws IOException {
        this();
        setPage(initialPage);
    }

    /**
     * Creates a <code>JosmEditorPane</code> based on a string containing
     * a URL specification.
     *
     * @param url the URL
     * @exception IOException if the URL is <code>null</code> or cannot be accessed
     */
    public JosmEditorPane(String url) throws IOException {
        this();
        setPage(url);
    }

    /**
     * Creates a <code>JosmEditorPane</code> that has been initialized
     * to the given text.  This is a convenience constructor that calls the
     * <code>setContentType</code> and <code>setText</code> methods.
     *
     * @param type mime type of the given text
     * @param text the text to initialize with; may be <code>null</code>
     * @exception NullPointerException if the <code>type</code> parameter
     *      is <code>null</code>
     */
    public JosmEditorPane(String type, String text) {
        this();
        setContentType(type);
        setText(text);
    }

    @Override
    protected InputStream getStream(URL page) throws IOException {
        URLConnection conn = Utils.setupURLConnection(page.openConnection());
        InputStream result = conn.getInputStream();
        String type = conn.getContentType();
        if (type != null) {
            setContentType(type);
        }
        return result;
    }

    /**
     * Creates a {@link JosmEditorPane} which is meant to be used as a powerful replacement of {@link javax.swing.JLabel}.
     */
    public static JosmEditorPane createJLabelLikePane() {
        final JosmEditorPane pane = new JosmEditorPane("text/html", "");
        pane.setOpaque(false);
        pane.setEditable(false);

        JosmHTMLEditorKit kit = new JosmHTMLEditorKit();
        final Font f = UIManager.getFont("Label.font");
        final StyleSheet ss = new StyleSheet();
        final String rule = MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                "bold",
                f.isItalic() ? "italic" : "normal"
        );
        ss.addRule("strong {" + rule + "}");
        ss.addRule("a {text-decoration: underline; color: blue}");
        kit.setStyleSheet(ss);
        pane.setEditorKit(kit);

        return pane;
    }
}
