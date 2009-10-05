// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Label that contains a clickable link.
 * @author Imi
 */
public class UrlLabel extends JEditorPane implements HyperlinkListener {

    private String url = "";
    private String description = "";

    public UrlLabel() {
        addHyperlinkListener(this);
        setEditable(false);
        setOpaque(false);
    }

    public UrlLabel(String url) {
        this (url, url);
    }

    public UrlLabel(String url, String description) {
        this();
        setUrl(url);
        setDescription(description);
        refresh();
    }

    protected void refresh() {
        setContentType("text/html");
        if (url != null) {
            setText("<html><a href=\""+url+"\">"+description+"</a></html>");
        } else {
            setText("<html>" + description + "</html>");
        }
        setToolTipText(url);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            OpenBrowser.displayUrl(url);
        }
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url;
        refresh();
    }

    public void setDescription(String description) {
        this.description = description == null? "" : description;
        refresh();
    }
}
