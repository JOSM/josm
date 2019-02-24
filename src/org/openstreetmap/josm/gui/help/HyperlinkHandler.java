// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Rectangle;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * Handles cliks on hyperlinks inside {@link HelpBrowser}.
 * @since 14807
 */
public class HyperlinkHandler implements HyperlinkListener {

    private final IHelpBrowser browser;
    private final JosmEditorPane help;

    /**
     * Constructs a new {@code HyperlinkHandler}.
     * @param browser help browser
     * @param help inner help pane
     */
    public HyperlinkHandler(IHelpBrowser browser, JosmEditorPane help) {
        this.browser = Objects.requireNonNull(browser);
        this.help = Objects.requireNonNull(help);
    }

    /**
     * Scrolls the help browser to the element with id <code>id</code>
     *
     * @param id the id
     * @return true, if an element with this id was found and scrolling was successful; false, otherwise
     */
    protected boolean scrollToElementWithId(String id) {
        Document d = help.getDocument();
        if (d instanceof HTMLDocument) {
            Element element = ((HTMLDocument) d).getElement(id);
            try {
                if (element != null) {
                    // Deprecated API to replace only when migrating to Java 9 (replacement not available in Java 8)
                    @SuppressWarnings("deprecation")
                    Rectangle r = help.modelToView(element.getStartOffset());
                    if (r != null) {
                        Rectangle vis = help.getVisibleRect();
                        r.height = vis.height;
                        help.scrollRectToVisible(r);
                        return true;
                    }
                }
            } catch (BadLocationException e) {
                Logging.warn(tr("Bad location in HTML document. Exception was: {0}", e.toString()));
                Logging.error(e);
            }
        }
        return false;
    }

    /**
     * Checks whether the hyperlink event originated on a &lt;a ...&gt; element with
     * a relative href consisting of a URL fragment only, i.e.
     * &lt;a href="#thisIsALocalFragment"&gt;. If so, replies the fragment, i.e. "thisIsALocalFragment".
     *
     * Otherwise, replies <code>null</code>
     *
     * @param e the hyperlink event
     * @return the local fragment or <code>null</code>
     */
    protected String getUrlFragment(HyperlinkEvent e) {
        AttributeSet set = e.getSourceElement().getAttributes();
        Object value = set.getAttribute(Tag.A);
        if (!(value instanceof SimpleAttributeSet))
            return null;
        SimpleAttributeSet atts = (SimpleAttributeSet) value;
        value = atts.getAttribute(javax.swing.text.html.HTML.Attribute.HREF);
        if (value == null)
            return null;
        String s = (String) value;
        Matcher m = Pattern.compile("(?:"+browser.getUrl()+")?#(.+)").matcher(s);
        if (m.matches())
            return m.group(1);
        return null;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
            return;
        if (e.getURL() == null || e.getURL().toExternalForm().startsWith(browser.getUrl()+'#')) {
            // Probably hyperlink event on a an A-element with a href consisting of a fragment only, i.e. "#ALocalFragment".
            String fragment = getUrlFragment(e);
            if (fragment != null) {
                // first try to scroll to an element with id==fragment. This is the way
                // table of contents are built in the JOSM wiki. If this fails, try to
                // scroll to a <A name="..."> element.
                //
                if (!scrollToElementWithId(fragment)) {
                    help.scrollToReference(fragment);
                }
            } else {
                HelpAwareOptionPane.showOptionDialog(
                        HelpBrowser.getInstance(),
                        tr("Failed to open help page. The target URL is empty."),
                        tr("Failed to open help page"),
                        JOptionPane.ERROR_MESSAGE,
                        null, /* no icon */
                        null, /* standard options, just OK button */
                        null, /* default is standard */
                        null /* no help context */
                );
            }
        } else if (e.getURL().toExternalForm().endsWith("action=edit")) {
            OpenBrowser.displayUrl(e.getURL().toExternalForm());
        } else {
            String url = e.getURL().toExternalForm();
            browser.setUrl(url);
            if (url.startsWith(HelpUtil.getWikiBaseUrl())) {
                browser.openUrl(url);
            } else {
                OpenBrowser.displayUrl(url);
            }
        }
    }
}
