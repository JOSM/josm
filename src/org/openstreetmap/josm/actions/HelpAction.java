// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WikiReader;

/**
 * Open a help browser and displays lightweight online help.
 *
 * @author imi
 */
public class HelpAction extends AbstractAction {

    public interface Helpful {
        String helpTopic();
    }

    private String languageCode = LanguageInfo.getLanguageCodeWiki();
    private JFrame helpBrowser = new JFrame(tr("JOSM Online Help"));
    private String baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
    private String pathbase = Main.pref.get("help.pathbase", "/wiki/");
    private String pathhelp = Main.pref.get("help.pathhelp", "Help/");
    private String pathmenu = Main.pref.get("help.pathmenu", "Menu/");
    private JEditorPane help = new JEditorPane();
    private WikiReader reader = new WikiReader(baseurl);
    private String url;

    public HelpAction() {
        super(tr("Help"), ImageProvider.get("help"));
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
                    setHelpUrl(e.getURL().toString());
                }
            }
        });
        help.setContentType("text/html");

        JPanel p = new JPanel(new BorderLayout());
        helpBrowser.setContentPane(p);

        p.add(new JScrollPane(help), BorderLayout.CENTER);
        String[] bounds = Main.pref.get("help.window.bounds", "0,0,800,600").split(",");
        helpBrowser.setBounds(
                Integer.parseInt(bounds[0]),
                Integer.parseInt(bounds[1]),
                Integer.parseInt(bounds[2]),
                Integer.parseInt(bounds[3]));

        JPanel buttons = new JPanel();
        p.add(buttons, BorderLayout.SOUTH);
        createButton(buttons, tr("Open in Browser"));
        createButton(buttons, tr("Edit"));
        createButton(buttons, tr("Reload"));

        helpBrowser.addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(WindowEvent e) {
                closeHelp();
            }
        });

        help.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Close");
        help.getActionMap().put("Close", new AbstractAction(){
            public void actionPerformed(ActionEvent e) {
                closeHelp();
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        if (tr("Open in Browser").equals(e.getActionCommand())) {
            OpenBrowser.displayUrl(url);
        } else if (tr("Edit").equals(e.getActionCommand())) {
            if (!url.startsWith(baseurl)) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Can only edit help pages from JOSM Online Help"),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            OpenBrowser.displayUrl(url+"?action=edit");
        } else if (tr("Reload").equals(e.getActionCommand())) {
            setHelpUrl(url);
        } else if (e.getActionCommand() == null) {
            String topic = null;
            if (e.getSource() instanceof Component) {
                Component c = SwingUtilities.getRoot((Component)e.getSource());
                Point mouse = c.getMousePosition();
                c = SwingUtilities.getDeepestComponentAt(c, mouse.x, mouse.y);
                topic = contextSensitiveHelp(c);
            } else {
                Point mouse = Main.parent.getMousePosition();
                topic = contextSensitiveHelp(SwingUtilities.getDeepestComponentAt(Main.parent, mouse.x, mouse.y));
            }
            if (topic == null) {
                helpBrowser.setVisible(false);
                setHelpUrl(baseurl+pathbase+"Help");
            } else {
                help(topic);
            }
        } else {
            helpBrowser.setVisible(false);
            setHelpUrl(baseurl+pathbase+"Help");
        }
    }

    /**
     * @return The topic of the help. <code>null</code> for "don't know"
     */
    private String contextSensitiveHelp(Object c) {
        if (c == null)
            return null;
        if (c instanceof Helpful)
            return ((Helpful)c).helpTopic();
        if (c instanceof JMenu) {
            JMenu b = (JMenu)c;
            if (b.getClientProperty("help") != null)
                return (String)b.getClientProperty("help");
            return pathmenu+b.getText();
        }
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton)c;
            if (b.getClientProperty("help") != null)
                return (String)b.getClientProperty("help");
            return contextSensitiveHelp(((AbstractButton)c).getAction());
        }
        if (c instanceof Action)
            return (String)((Action)c).getValue("help");
        if (c instanceof JComponent && ((JComponent)c).getClientProperty("help") != null)
            return (String)((JComponent)c).getClientProperty("help");
        if (c instanceof Component)
            return contextSensitiveHelp(((Component)c).getParent());
        return null;
    }

    /**
     * Displays the help (or browse on the already open help) on the online page
     * with the given help topic. Use this for larger help descriptions.
     */
    public void help(String topic) {
        helpBrowser.setVisible(false);
        setHelpUrl(baseurl+pathbase+pathhelp+topic);
    }

    /**
     * Set the content of the help window to a specific text (in html format)
     * @param url The url this content is the representation of
     */
    public void setHelpUrl(String url) {
        String langurl = url;
        if(url.startsWith(baseurl+pathbase))
        {
            int i = pathbase.length()+baseurl.length();
            String title = url.substring(i);
            if(languageCode.length() != 0 && !title.startsWith(languageCode)) {
                title = languageCode + title;
            }
            langurl = url.substring(0, i) + title;
        }
        boolean loaded = false;
        if(!langurl.equals(this.url) && !langurl.equals(url))
        {
            loaded = loadHelpUrl(url, langurl, true);
        }
        if(!loaded) {
            loaded = loadHelpUrl(url, langurl, false);
        }
        if(!loaded) {
            help.setText(tr("Error while loading page {0}",url));
        }
        helpBrowser.setVisible(true);
    }

    private boolean loadHelpUrl(String url, String langurl, boolean lang)
    {
        this.url = lang ? langurl : url;
        boolean loaded = false;
        try {
            String txt = reader.read(this.url);
            if(txt.length() == 0)
            {
                if(lang)
                    throw new IOException();
                else
                {
                    if(url.equals(langurl))
                    {
                        txt = ("<HTML>"+tr("Help page missing. Create it in <A HREF=\"{0}\">English</A>.",
                                url+"?action=edit")+"</HTML>");
                    }
                    else
                    {
                        txt = ("<HTML>"+tr("Help page missing. Create it in <A HREF=\"{0}\">English</A> or <A HREF=\"{1}\">your language</A>.",
                                url+"?action=edit", langurl+"?action=edit")+"</HTML>");
                    }
                }
            }
            help.setText(txt);
            help.setCaretPosition(0);
            loaded = true;
        } catch (IOException ex) {
        }
        return loaded;
    }

    /**
     * Closes the help window
     */
    public void closeHelp() {
        String bounds = helpBrowser.getX()+","+helpBrowser.getY()+","+helpBrowser.getWidth()+","+helpBrowser.getHeight();
        Main.pref.put("help.window.bounds", bounds);
        helpBrowser.setVisible(false);
    }

    private void createButton(JPanel buttons, String name) {
        JButton b = new JButton(tr(name));
        b.setActionCommand(name);
        b.addActionListener(this);
        buttons.add(b);
    }
}
