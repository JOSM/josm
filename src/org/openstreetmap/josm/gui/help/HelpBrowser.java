// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WikiReader;
import org.openstreetmap.josm.tools.WindowGeometry;

public class HelpBrowser extends JFrame {

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
     * Launches the internal help browser and directs it to the help page for
     * <code>helpTopic</code>.
     * 
     * @param helpTopic the help topic
     */
    static public void launchBrowser(String helpTopic) {
        HelpBrowser browser = getInstance();
        browser.setUrlForHelpTopic(helpTopic);
        browser.setVisible(true);
        browser.toFront();
    }

    /** the help browser */
    private JEditorPane help;
    /** the help browser history */
    private HelpBrowserHistory history;

    /** the currently displayed URL */
    private String url;

    private String languageCode = LanguageInfo.getLanguageCodeWiki();
    private String baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
    private String pathbase = Main.pref.get("help.pathbase", "/wiki/");
    private WikiReader reader = new WikiReader(baseurl);

    /**
     * Builds the style sheet used in the internal help browser
     * 
     * @return the style sheet
     */
    protected StyleSheet buildStyleSheet() {
        StyleSheet ss = new StyleSheet();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("help-browser.css")
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
        help.addHyperlinkListener(new HyperlinkListener(){
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
                    return;
                if (e.getURL() == null) {
                    help.setText("<html>404 not found</html>");
                } else if (e.getURL().toString().endsWith("action=edit")) {
                    OpenBrowser.displayUrl(e.getURL().toString());
                } else {
                    url = e.getURL().toString();
                    setUrl(e.getURL().toString());
                }
            }
        });
        help.setContentType("text/html");


        history = new HelpBrowserHistory(this);

        JPanel p = new JPanel(new BorderLayout());
        setContentPane(p);

        p.add(new JScrollPane(help), BorderLayout.CENTER);

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

        setTitle(tr("JOSM Help Browser"));
    }

    public HelpBrowser() {
        build();
    }

    public String getUrl() {
        return url;
    }

    protected void loadUrl(String url) {
        String langurl = url;
        if(url.startsWith(baseurl+pathbase)){
            int i = pathbase.length()+baseurl.length();
            String title = url.substring(i);
            if(languageCode.length() != 0 && !title.startsWith(languageCode)) {
                title = languageCode + title;
            }
            langurl = url.substring(0, i) + title;
        }
        boolean loaded = false;
        if(!langurl.equals(this.url) && !langurl.equals(url)){
            loaded = loadHelpUrl(url, langurl, true);
        }
        if(!loaded) {
            loaded = loadHelpUrl(url, langurl, false);
        }
        if(!loaded) {
            help.setText(tr("Error while loading page {0}",url));
        }
    }

    public void setUrl(String url) {
        loadUrl(url);
        if (!isVisible()) {
            setVisible(true);
            toFront();
        } else {
            toFront();
        }
        history.setCurrentUrl(url);
    }

    public void setUrlForHelpTopic(String topic) {
        setUrl(baseurl+pathbase+ topic);
    }

    protected boolean loadHelpUrl(String url, String localizedUrl, boolean useLocalizedUrl){
        this.url = useLocalizedUrl ? localizedUrl : url;
        boolean loaded = false;
        try {
            String txt = reader.read(this.url);
            if(txt.length() == 0){
                if(useLocalizedUrl)
                    throw new IOException();
                if(url.equals(localizedUrl)){
                    txt = ("<HTML>"+tr("Help page missing. Create it in <A HREF=\"{0}\">English</A>.",
                            url+"?action=edit")+"</HTML>");
                } else{
                    txt = ("<HTML>"+tr("Help page missing. Create it in <A HREF=\"{0}\">English</A> or <A HREF=\"{1}\">your language</A>.",
                            url+"?action=edit", localizedUrl+"?action=edit")+"</HTML>");
                }
            }
            help.setText(txt);
            help.setCaretPosition(0);
            loaded = true;
        } catch (IOException ex) {
        }
        return loaded;
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
            if (!getUrl().startsWith(baseurl)) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Can only edit help pages from JOSM Online Help"),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
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
            setUrl(url);
        }
    }

    class BackAction extends AbstractAction implements Observer {
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

    class ForwardAction extends AbstractAction implements Observer {
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
            setUrlForHelpTopic("Help");
        }
    }
}
