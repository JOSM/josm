// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Label that contains a clickable link.
 * @author Imi
 * 5050: Simplifications by Zverikk included by akks
 */
public class UrlLabel extends JLabel implements MouseListener {

    private String url = "";
    private String description = "";
    private int fontPlus;

    public UrlLabel() {
        addMouseListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public UrlLabel(String url) {
        this (url, url, 0);
    }

    public UrlLabel(String url, int fontPlus) {
        this (url, url, fontPlus);
    }

    public UrlLabel(String url, String description) {
        this (url, url, 0);
    }

    public UrlLabel(String url, String description, int fontPlus) {
        this();
        setUrl(url);
        setDescription(description);
        this.fontPlus = fontPlus;
        if (fontPlus!=0) setFont(getFont().deriveFont(0, getFont().getSize()+fontPlus));
        refresh();
    }

    protected void refresh() {
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
    public void setUrl(String url) {
        this.url = url;
        refresh();
    }

    /**
     * Sets the text part of the URL label. Defaults to the empty string if description is null.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description == null? "" : description;
        this.description = this.description.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
        refresh();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if( SwingUtilities.isLeftMouseButton(e) ) {
            OpenBrowser.displayUrl(url);
        } else if( SwingUtilities.isRightMouseButton(e) ) {
            Utils.copyToClipboard(url);
        }
    }
    @Override
    public void mousePressed(MouseEvent e) {    }
    @Override
    public void mouseEntered(MouseEvent e) {    }
    @Override
    public void mouseExited(MouseEvent e) {    }
    @Override
    public void mouseReleased(MouseEvent e) {    }

}
