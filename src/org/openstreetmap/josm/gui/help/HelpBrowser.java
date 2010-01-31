// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.openstreetmap.josm.gui.help.HelpUtil.buildAbsoluteHelpTopic;
import static org.openstreetmap.josm.gui.help.HelpUtil.getHelpTopicEditUrl;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.HTML.Tag;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WindowGeometry;

public class HelpBrowser extends JFrame {
    static private final Logger logger = Logger.getLogger(HelpBrowser.class.getName());

    /** the unique instance */
    private static HelpBrowser instance;

    /**
     * Replies the unique instance of the help browser
     *
     * @return the unique instance of the help browser
     */
    static public HelpBrowser getInstance() {
        if (instance == null) {
            instance = new HelpBrowser();
        }
        return instance;
    }

    /**
     * Show the help page for help topic <code>helpTopic</code>.
     *
     * @param helpTopic the help topic
     */
    public static void setUrlForHelpTopic(final String helpTopic) {
        final HelpBrowser browser = getInstance();
        Runnable r = new Runnable() {
            public void run() {
                browser.openHelpTopic(helpTopic);
                browser.setVisible(true);
                browser.toFront();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    /**
     * Launches the internal help browser and directs it to the help page for
     * <code>helpTopic</code>.
     *
     * @param helpTopic the help topic
     */
    static public void launchBrowser(String helpTopic) {
        HelpBrowser browser = getInstance();
        browser.openHelpTopic(helpTopic);
        browser.setVisible(true);
        browser.toFront();
    }

    /** the help browser */
    private JEditorPane help;
    private JScrollPane spHelp;

    /** the help browser history */
    private HelpBrowserHistory history;

    /** the currently displayed URL */
    private String url;

    private HelpContentReader reader;

    /**
     * Builds the style sheet used in the internal help browser
     *
     * @return the style sheet
     */
    protected StyleSheet buildStyleSheet() {
        StyleSheet ss = new StyleSheet();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/data/help-browser.css")
                )
        );
        StringBuffer css = new StringBuffer();
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                css.append(line);
                css.append("\n");
            }
            reader.close();
        } catch(Exception e) {
            System.err.println(tr("Failed to read CSS file ''help-browser.css''. Exception is: {0}", e.toString()));
            e.printStackTrace();
            return ss;
        }
        ss.addRule(css.toString());
        return ss;
    }

    protected JToolBar buildToolBar() {
        JToolBar tb = new JToolBar();
        tb.add(new JButton(new HomeAction()));
        tb.add(new JButton(new BackAction(history)));
        tb.add(new JButton(new ForwardAction(history)));
        tb.add(new JButton(new ReloadAction()));
        tb.add(new JSeparator());
        tb.add(new JButton(new OpenInBrowserAction()));
        tb.add(new JButton(new EditAction()));
        return tb;
    }

    protected void build() {
        help = new JEditorPane();
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.setStyleSheet(buildStyleSheet());
        help.setEditorKit(kit);
        help.setEditable(false);
        help.addHyperlinkListener(new HyperlinkHandler());
        help.setContentType("text/html");
        history = new HelpBrowserHistory(this);

        JPanel p = new JPanel(new BorderLayout());
        setContentPane(p);

        p.add(spHelp = new JScrollPane(help), BorderLayout.CENTER);

        addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        p.add(buildToolBar(), BorderLayout.NORTH);
        help.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Close");
        help.getActionMap().put("Close", new AbstractAction(){
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        setMinimumSize(new Dimension(400, 200));
        setTitle(tr("JOSM Help Browser"));
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            new Dimension(600,400)
                    )
            ).applySafe(this);
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    public HelpBrowser() {
        reader = new HelpContentReader(HelpUtil.getWikiBaseUrl());
        build();
    }

    /**
     * Replies the current URL
     *
     * @return the current URL
     */

    public String getUrl() {
        return url;
    }

    /**
     * Displays a warning page when a help topic doesn't exist yet.
     *
     * @param relativeHelpTopic the help topic
     */
    protected void handleMissingHelpContent(String relativeHelpTopic) {
        String message = tr("<html><p class=\"warning-header\">Help content for help topic missing</p>"
                + "<p class=\"warning-body\">Help content for the help topic <strong>{0}</strong> is "
                + "not available yet. It is missing both in your local language ({1}) and in english.<br><br>"
                + "Please help to improve the JOSM help system and fill in the missing information. "
                + "You can both edit the <a href=\"{2}\">help topic in your local language ({1})</a> and "
                + "the <a href=\"{3}\">help topic in english</a>."
                + "</p></html>",
                relativeHelpTopic,
                Locale.getDefault().getDisplayName(),
                getHelpTopicEditUrl(buildAbsoluteHelpTopic(relativeHelpTopic)),
                getHelpTopicEditUrl(buildAbsoluteHelpTopic(relativeHelpTopic, Locale.ENGLISH))
        );
        help.setText(message);
    }

    /**
     * Displays a error page if a help topic couldn't be loaded because of network or IO error.
     *
     * @param relativeHelpTopic the help topic
     * @param e the exception
     */
    protected void handleHelpContentReaderException(String relativeHelpTopic, HelpContentReaderException e) {
        String message = tr("<html><p class=\"error-header\">Error when retrieving help information</p>"
                + "<p class=\"error-body\">The content for the help topic <strong>{0}</strong> could "
                + "not be loaded. The error message is (untranslated):<br>"
                + "<tt>{1}</tt>"
                + "</p></html>",
                relativeHelpTopic,
                e.toString()
        );
        help.setText(message);
    }

    protected void scrollToTop() {
        JScrollBar sb = spHelp.getVerticalScrollBar();
        sb.setValue(sb.getMinimum());
    }

    /**
     * Loads a help topic given by a relative help topic name (i.e. "/Action/New")
     *
     * First tries to load the language specific help topic. If it is missing, tries to
     * load the topic in english.
     *
     * @param relativeHelpTopic the relative help topic
     */
    protected void loadRelativeHelpTopic(String relativeHelpTopic) {
        String url = HelpUtil.getHelpTopicUrl(HelpUtil.buildAbsoluteHelpTopic(relativeHelpTopic));
        String content = null;
        try {
            content = reader.fetchHelpTopicContent(url);
        } catch(MissingHelpContentException e) {
            url = HelpUtil.getHelpTopicUrl(HelpUtil.buildAbsoluteHelpTopic(relativeHelpTopic, Locale.ENGLISH));
            try {
                logger.info("fetching url: " + url);
                content = reader.fetchHelpTopicContent(url);
            } catch(MissingHelpContentException e1) {
                handleMissingHelpContent(relativeHelpTopic);
                return;
            } catch(HelpContentReaderException e1) {
                e1.printStackTrace();
                handleHelpContentReaderException(relativeHelpTopic,e1);
                return;
            }
        } catch(HelpContentReaderException e) {
            e.printStackTrace();
            handleHelpContentReaderException(relativeHelpTopic, e);
            return;
        }
        help.setText(content);
        history.setCurrentUrl(url);
        this.url = url;
        scrollToTop();
    }

    /**
     * Loads a help topic given by an absolute help topic name, i.e.
     * "/De:Help/Action/New"
     *
     * @param absoluteHelpTopic the absolute help topic name
     */
    protected void loadAbsoluteHelpTopic(String absoluteHelpTopic) {
        String url = HelpUtil.getHelpTopicUrl(absoluteHelpTopic);
        String content = null;
        try {
            content = reader.fetchHelpTopicContent(url);
        } catch(MissingHelpContentException e) {
            handleMissingHelpContent(absoluteHelpTopic);
            return;
        } catch(HelpContentReaderException e) {
            e.printStackTrace();
            handleHelpContentReaderException(absoluteHelpTopic, e);
            return;
        }
        help.setText(content);
        history.setCurrentUrl(url);
        this.url = url;
        scrollToTop();
    }

    /**
     * Opens an URL and displays the content.
     *
     *  If the URL is the locator of an absolute help topic, help content is loaded from
     *  the JOSM wiki. Otherwise, the help browser loads the page from the given URL
     *
     * @param url the url
     */
    public void openUrl(String url) {
        if (!isVisible()) {
            setVisible(true);
            toFront();
        } else {
            toFront();
        }
        String helpTopic = HelpUtil.extractAbsoluteHelpTopic(url);
        if (helpTopic == null) {
            try {
                this.url = url;
                help.setPage(url);
            } catch(IOException e) {
                HelpAwareOptionPane.showOptionDialog(
                        Main.parent,
                        tr(
                                "<html>Failed to open help page for url {0}.<br>"
                                + "This is most likely due to a network problem, please check<br>"
                                + "your internet connection</html>",
                                url.toString()
                        ),
                        tr("Failed to open URL"),
                        JOptionPane.ERROR_MESSAGE,
                        null, /* no icon */
                        null, /* standard options, just OK button */
                        null, /* default is standard */
                        null /* no help context */
                );
            }
            history.setCurrentUrl(url);
        } else {
            loadAbsoluteHelpTopic(helpTopic);
        }
    }

    /**
     * Loads and displays the help information for a help topic given
     * by a relative help topic name, i.e. "/Action/New"
     *
     * @param relativeHelpTopic the relative help topic
     */
    public void openHelpTopic(String relativeHelpTopic) {
        if (!isVisible()) {
            setVisible(true);
            toFront();
        } else {
            toFront();
        }
        loadRelativeHelpTopic(relativeHelpTopic);
    }

    class OpenInBrowserAction extends AbstractAction {
        public OpenInBrowserAction() {
            //putValue(NAME, tr("Open in Browser"));
            putValue(SHORT_DESCRIPTION, tr("Open the current help page in an external browser"));
            putValue(SMALL_ICON, ImageProvider.get("help", "internet"));
        }

        public void actionPerformed(ActionEvent e) {
            OpenBrowser.displayUrl(getUrl());
        }
    }

    class EditAction extends AbstractAction {
        public EditAction() {
            // putValue(NAME, tr("Edit"));
            putValue(SHORT_DESCRIPTION, tr("Edit the current help page"));
            putValue(SMALL_ICON,ImageProvider.get("dialogs", "edit"));
        }

        public void actionPerformed(ActionEvent e) {
            if (!getUrl().startsWith(HelpUtil.getWikiBaseHelpUrl())) {
                String message = tr(
                        "<html>The current URL <tt>{0}</tt><br>"
                        + "is an external URL. Editing is only possible for help topics<br>"
                        + "on the help server <tt>{1}</tt>.</html>",
                        getUrl(),
                        HelpUtil.getWikiBaseUrl()
                );
                JOptionPane.showMessageDialog(
                        Main.parent,
                        message,
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            String url = getUrl();
            url = url.replaceAll("#[^#]*$", "");
            OpenBrowser.displayUrl(url+"?action=edit");
        }
    }

    class ReloadAction extends AbstractAction {
        public ReloadAction() {
            //putValue(NAME, tr("Reload"));
            putValue(SHORT_DESCRIPTION, tr("Reload the current help page"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
        }

        public void actionPerformed(ActionEvent e) {
            openUrl(getUrl());
        }
    }

    static class BackAction extends AbstractAction implements Observer {
        private HelpBrowserHistory history;
        public BackAction(HelpBrowserHistory history) {
            this.history = history;
            history.addObserver(this);
            //putValue(NAME, tr("Back"));
            putValue(SHORT_DESCRIPTION, tr("Go to the previous page"));
            putValue(SMALL_ICON, ImageProvider.get("help", "previous"));
            setEnabled(history.canGoBack());
        }

        public void actionPerformed(ActionEvent e) {
            history.back();
        }
        public void update(Observable o, Object arg) {
            System.out.println("BackAction: canGoBoack=" + history.canGoBack() );
            setEnabled(history.canGoBack());
        }
    }

    static class ForwardAction extends AbstractAction implements Observer {
        private HelpBrowserHistory history;
        public ForwardAction(HelpBrowserHistory history) {
            this.history = history;
            history.addObserver(this);
            //putValue(NAME, tr("Forward"));
            putValue(SHORT_DESCRIPTION, tr("Go to the next page"));
            putValue(SMALL_ICON, ImageProvider.get("help", "next"));
            setEnabled(history.canGoForward());
        }

        public void actionPerformed(ActionEvent e) {
            history.forward();
        }
        public void update(Observable o, Object arg) {
            setEnabled(history.canGoForward());
        }
    }

    class HomeAction extends AbstractAction  {
        public HomeAction() {
            //putValue(NAME, tr("Home"));
            putValue(SHORT_DESCRIPTION, tr("Go to the JOSM help home page"));
            putValue(SMALL_ICON, ImageProvider.get("help", "home"));
        }

        public void actionPerformed(ActionEvent e) {
            openHelpTopic("/");
        }
    }

    class HyperlinkHandler implements HyperlinkListener {

        /**
         * Scrolls the help browser to the element with id <code>id</code>
         *
         * @param id the id
         * @return true, if an element with this id was found and scrolling was successful; false, otherwise
         */
        protected boolean scrollToElementWithId(String id) {
            Document d = help.getDocument();
            if (d instanceof HTMLDocument) {
                HTMLDocument doc = (HTMLDocument) d;
                Element element = doc.getElement(id);
                try {
                    Rectangle r = help.modelToView(element.getStartOffset());
                    if (r != null) {
                        Rectangle vis = help.getVisibleRect();
                        r.height = vis.height;
                        help.scrollRectToVisible(r);
                        return true;
                    }
                } catch(BadLocationException e) {
                    System.err.println(tr("Warning: bad location in HTML document. Exception was: {0}", e.toString()));
                    e.printStackTrace();
                }
            }
            return false;
        }

        /**
         * Checks whether the hyperlink event originated on a <a ...> element with
         * a relative href consisting of a URL fragment only, i.e.
         * <a href="#thisIsALocalFragment">. If so, replies the fragment, i.e.
         * "thisIsALocalFragment".
         *
         * Otherwise, replies null
         *
         * @param e the hyperlink event
         * @return the local fragment
         */
        protected String getUrlFragment(HyperlinkEvent e) {
            AttributeSet set = e.getSourceElement().getAttributes();
            Object value = set.getAttribute(Tag.A);
            if (value == null || ! (value instanceof SimpleAttributeSet)) return null;
            SimpleAttributeSet atts = (SimpleAttributeSet)value;
            value = atts.getAttribute(javax.swing.text.html.HTML.Attribute.HREF);
            if (value == null) return null;
            String s = (String)value;
            if (s.matches("#.*"))
                return s.substring(1);
            return null;
        }

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
                return;
            if (e.getURL() == null) {
                // Probably hyperlink event on a an A-element with a href consisting of
                // a fragment only, i.e. "#ALocalFragment".
                //
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
                            Main.parent,
                            tr("Failed to open help page. The target URL is empty."),
                            tr("Failed to open help page"),
                            JOptionPane.ERROR_MESSAGE,
                            null, /* no icon */
                            null, /* standard options, just OK button */
                            null, /* default is standard */
                            null /* no help context */
                    );
                }
            } else if (e.getURL().toString().endsWith("action=edit")) {
                OpenBrowser.displayUrl(e.getURL().toString());
            } else {
                url = e.getURL().toString();
                openUrl(e.getURL().toString());
            }
        }
    }
}
