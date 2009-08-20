//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.UrlLabel;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Nice about screen. I guess every application need one these days.. *sigh*
 *
 * The REVISION resource is read and if present, it shows the revision
 * information of the jar-file.
 *
 * @author imi
 */
/**
 * @author Stephan
 *
 */
public class AboutAction extends JosmAction {

    private static final String version;

    private final static JTextArea revision;
    private static String time;

    static {
        boolean manifest = false;
        URL u = Main.class.getResource("/REVISION");
        if(u == null) {
            try {
                manifest = true;
                u = new URL("jar:" + Main.class.getProtectionDomain().getCodeSource().getLocation().toString()
                    + "!/META-INF/MANIFEST.MF");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        revision = loadFile(u, manifest);

        Pattern versionPattern = Pattern.compile(".*?(?:Revision|Main-Version): ([0-9]*(?: SVN)?).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
        Matcher match = versionPattern.matcher(revision.getText());
        version = match.matches() ? match.group(1) : tr("UNKNOWN");

        Pattern timePattern = Pattern.compile(".*?(?:Last Changed Date|Main-Date): ([^\n]*).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
        match = timePattern.matcher(revision.getText());
        time = match.matches() ? match.group(1) : tr("UNKNOWN");
    }

    /**
     * Return string describing version.
     * Note that the strinc contains the version number plus an optional suffix of " SVN" to indicate an unofficial development build.
     * @return version string
     */
    static public String getVersionString() {
        return version;
    }

    static public String getTextBlock() {
        return revision.getText();
    }

    static public void setUserAgent() {
        Properties sysProp = System.getProperties();
        sysProp.put("http.agent", "JOSM/1.5 ("+(version.equals(tr("UNKNOWN"))?"UNKNOWN":version)+" "+LanguageInfo.getLanguageCode()+")");
        System.setProperties(sysProp);
    }

    /**
     * Return the number part of the version string.
     * @return integer part of version number or Integer.MAX_VALUE if not available
     */
    public static int getVersionNumber() {
        int myVersion=Integer.MAX_VALUE;
        try {
            myVersion = Integer.parseInt(version.split(" ")[0]);
        } catch (NumberFormatException e) {
            // e.printStackTrace();
        }
        return myVersion;
    }

    /**
     * check whether the version is a development build out of SVN.
     * @return true if it is a SVN unofficial build
     */
    public static boolean isDevelopmentVersion() {
        return version.endsWith(" SVN") || version.equals(tr("UNKNOWN"));
    }

    public AboutAction() {
        super(tr("About"), "about", tr("Display the about screen."), Shortcut.registerShortcut("system:about", tr("About"), KeyEvent.VK_F1, Shortcut.GROUP_DIRECT, Shortcut.SHIFT_DEFAULT), true);
    }

    public void actionPerformed(ActionEvent e) {
        JTabbedPane about = new JTabbedPane();

        JTextArea readme = loadFile(Main.class.getResource("/README"), false);
        JTextArea contribution = loadFile(Main.class.getResource("/CONTRIBUTION"), false);
        JTextArea license = loadFile(Main.class.getResource("/LICENSE"), false);

        JPanel info = new JPanel(new GridBagLayout());
        JLabel caption = new JLabel("JOSM - " + tr("Java OpenStreetMap Editor"));
        caption.setFont(new Font("Helvetica", Font.BOLD, 20));
        info.add(caption, GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,10), GBC.eol());
        info.add(new JLabel(tr("Version {0}",version)), GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,5), GBC.eol());
        info.add(new JLabel(tr("Last change at {0}",time)), GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,5), GBC.eol());
        info.add(new JLabel(tr("Java Version {0}",System.getProperty("java.version"))), GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,10), GBC.eol());
        info.add(new JLabel(tr("Homepage")), GBC.std().insets(10,0,10,0));
        info.add(new UrlLabel("http://josm.openstreetmap.de"), GBC.eol().fill(GBC.HORIZONTAL));
        info.add(new JLabel(tr("Bug Reports")), GBC.std().insets(10,0,10,0));
        info.add(new UrlLabel("http://josm.openstreetmap.de/newticket"), GBC.eol().fill(GBC.HORIZONTAL));

        about.addTab(tr("Info"), info);
        about.addTab(tr("Readme"), createScrollPane(readme));
        about.addTab(tr("Revision"), createScrollPane(revision));
        about.addTab(tr("Contribution"), createScrollPane(contribution));
        about.addTab(tr("License"), createScrollPane(license));
        about.addTab(tr("Plugins"), new JScrollPane(PluginHandler.getInfoPanel()));

        about.setPreferredSize(new Dimension(500,300));

        JOptionPane.showMessageDialog(Main.parent, about, tr("About JOSM..."),
                JOptionPane.INFORMATION_MESSAGE, ImageProvider.get("logo"));
    }

    private JScrollPane createScrollPane(JTextArea area) {
        area.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        area.setOpaque(false);
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(null);
        sp.setOpaque(false);
        return sp;
    }

    /**
     * Retrieve the latest JOSM version from the JOSM homepage.
     * @return An string with the latest version or "UNKNOWN" in case
     *      of problems (e.g. no internet connection).
     */
    public static String checkLatestVersion() {
        String latest;
        try {
            InputStream s = new URL("http://josm.openstreetmap.de/current").openStream();
            latest = new BufferedReader(new InputStreamReader(s)).readLine();
            s.close();
        } catch (IOException x) {
            x.printStackTrace();
            return tr("UNKNOWN");
        }
        return latest;
    }

    /**
     * Load the specified resource into an TextArea and return it.
     * @param resource The resource url to load
     * @return  An read-only text area with the content of "resource"
     */
    private static JTextArea loadFile(URL resource, boolean manifest) {
        JTextArea area = new JTextArea(tr("File could not be found."));
        area.setEditable(false);
        Font font = Font.getFont("monospaced");
        if (font != null) {
            area.setFont(font);
        }
        if (resource == null)
            return area;
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(resource.openStream()));
            String s = "";
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                s += line + "\n";
            }
            if (manifest) {
                s = Pattern.compile("\n ", Pattern.DOTALL).matcher(s).replaceAll("");
                s = Pattern.compile("^(SHA1-Digest|Name): .*?$", Pattern.DOTALL|Pattern.MULTILINE).matcher(s).replaceAll("");
                s = Pattern.compile("\n+$", Pattern.DOTALL).matcher(s).replaceAll("");
            }
            area.setText(s);
            area.setCaretPosition(0);
        } catch (IOException e) {
            System.err.println("Cannot load resource " + resource + ": " + e.getMessage());
            //e.printStackTrace();
        }
        return area;
    }
}
