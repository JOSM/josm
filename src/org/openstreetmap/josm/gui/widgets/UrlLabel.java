// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Label that contains a clickable link.
 * @since 6340
 */
public class UrlLabel extends JLabel implements MouseListener {

    private String url = "";
    private String description = "";

    /**
     * Constructs a new empty {@code UrlLabel}.
     */
    public UrlLabel() {
        addMouseListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Constructs a new {@code UrlLabel} for the given URL.
     * @param url The URL to use, also used as description
     */
    public UrlLabel(String url) {
        this (url, url, 0);
    }

    /**
     * Constructs a new {@code UrlLabel} for the given URL and font increase.
     * @param url The URL to use, also used as description
     * @param fontPlus The font increase in 1/72 of an inch units.
     */
    public UrlLabel(String url, int fontPlus) {
        this (url, url, fontPlus);
    }

    /**
     * Constructs a new {@code UrlLabel} for the given URL and description.
     * @param url The URL to use
     * @param description The description to display 
     */
    public UrlLabel(String url, String description) {
        this (url, description, 0);
    }

    /**
     * Constructs a new {@code UrlLabel} for the given URL, description and font increase.
     * @param url The URL to use
     * @param description The description to display 
     * @param fontPlus The font increase in 1/72 of an inch units.
     */
    public UrlLabel(String url, String description, int fontPlus) {
        this();
        setUrl(url);
        setDescription(description);
        if (fontPlus!=0) {
            setFont(getFont().deriveFont(0, getFont().getSize()+fontPlus));
        }
        refresh();
    }

    protected final void refresh() {
        if (url != null) {
            setText("<html><a href=\""+url+"\">"+description+"</a></html>");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(String.format("<html>%s<br/>%s</html>", url, tr("Right click = copy to clipboard")));
        } else {
            setText("<html>" + description + "</html>");
            setCursor(null);
            setToolTipText(null);
        }
    }

    /**
     * Sets the URL to be visited if the user clicks on this URL label. If null, the
     * label turns into a normal label without hyperlink.
     *
     * @param url the url. Can be null.
     */
    public final void setUrl(String url) {
        this.url = url;
        refresh();
    }

    /**
     * Sets the text part of the URL label. Defaults to the empty string if description is null.
     *
     * @param description the description
     */
    public final void setDescription(String description) {
        this.description = description == null? "" : description;
        this.description = this.description.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
        refresh();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            OpenBrowser.displayUrl(url);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            Utils.copyToClipboard(url);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        // Ignored
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        // Ignored
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        // Ignored
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        // Ignored
    }
}
