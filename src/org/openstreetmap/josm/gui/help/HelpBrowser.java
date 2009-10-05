// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WikiReader;
import org.openstreetmap.josm.tools.WindowGeometry;

public class HelpBrowser extends JFrame {

    private JEditorPane help;

    /** the currently displayed URL */
    private String url;

    private String languageCode = LanguageInfo.getLanguageCodeWiki();
    private String baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
    private String pathbase = Main.pref.get("help.pathbase", "/wiki/");
    private WikiReader reader = new WikiReader(baseurl);


    protected void build() {
        help = new JEditorPane();
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
                    setUrl(e.getURL().toString());
                }
            }
        });
        help.setContentType("text/html");

        JPanel p = new JPanel(new BorderLayout());
        setContentPane(p);

        p.add(new JScrollPane(help), BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        p.add(buttons, BorderLayout.SOUTH);

        buttons.add(new SideButton(new OpenInBrowserAction()));
        buttons.add(new SideButton(new EditAction()));
        buttons.add(new SideButton(new ReloadAction()));

        addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

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

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(800,600)
                    )
            ).applySafe(this);
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
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
        if (!isVisible()) {
            setVisible(true);
            toFront();
        } else {
            toFront();
        }
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
            putValue(NAME, tr("Open in Browser"));
            putValue(SHORT_DESCRIPTION, tr("Open the current help page in an external browser"));
            // provide icon
        }

        public void actionPerformed(ActionEvent e) {
            OpenBrowser.displayUrl(getUrl());
        }
    }

    class EditAction extends AbstractAction {
        public EditAction() {
            putValue(NAME, tr("Edit"));
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
            putValue(NAME, tr("Reload"));
            putValue(SHORT_DESCRIPTION, tr("Reload the current help page"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
        }

        public void actionPerformed(ActionEvent e) {
            setUrl(url);
        }
    }
}
