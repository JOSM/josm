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
import java.net.URL;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;
import org.openstreetmap.josm.tools.ShortCut;

/**
 * Nice about screen. I guess every application need one these days.. *sigh*
 *
 * The REVISION resource is read and if present, it shows the revision
 * information of the jar-file.
 *
 * @author imi
 */
public class AboutAction extends JosmAction {

	public static final String version;

	private final static JTextArea revision;
	private static String time;

	static {
		revision = loadFile(Main.class.getResource("/REVISION"));

		Pattern versionPattern = Pattern.compile(".*?Revision: ([0-9]*).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		Matcher match = versionPattern.matcher(revision.getText());
		version = match.matches() ? match.group(1) : tr("UNKNOWN");

		Pattern timePattern = Pattern.compile(".*?Last Changed Date: ([^\n]*).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		match = timePattern.matcher(revision.getText());
		time = match.matches() ? match.group(1) : tr("UNKNOWN");
	}

	static public String getVersion() {
		return version;
	}

	public AboutAction() {
		super(tr("About"), "about", tr("Display the about screen."), ShortCut.registerShortCut("system:about", tr("About"), KeyEvent.VK_F1, ShortCut.GROUP_DIRECT, ShortCut.SHIFT_DEFAULT), true);
	}

	public void actionPerformed(ActionEvent e) {
		JTabbedPane about = new JTabbedPane();

		JTextArea readme = loadFile(Main.class.getResource("/README"));
		JTextArea contribution = loadFile(Main.class.getResource("/CONTRIBUTION"));

		JPanel info = new JPanel(new GridBagLayout());
		info.add(new JLabel(tr("Java OpenStreetMap Editor Version {0}",version)), GBC.eol().fill(GBC.HORIZONTAL));
		info.add(new JLabel(tr("last change at {0}",time)), GBC.eol().fill(GBC.HORIZONTAL));
		info.add(new JLabel(tr("Java Version {0}",System.getProperty("java.version"))), GBC.eol().fill(GBC.HORIZONTAL));
		info.add(GBC.glue(0,10), GBC.eol());
		info.add(new JLabel(tr("Homepage")), GBC.std().insets(0,0,10,0));
		info.add(new UrlLabel("http://josm.openstreetmap.de"), GBC.eol().fill(GBC.HORIZONTAL));
		info.add(new JLabel(tr("Bug Reports")), GBC.std().insets(0,0,10,0));
		info.add(new UrlLabel("http://josm.openstreetmap.de/newticket"), GBC.eol().fill(GBC.HORIZONTAL));
		info.add(new JLabel(tr("News about JOSM")), GBC.std().insets(0,0,10,0));
		info.add(new UrlLabel("http://www.opengeodata.org/?cat=17"), GBC.eol().fill(GBC.HORIZONTAL));

		about.addTab(tr("Info"), info);
		about.addTab(tr("Readme"), createScrollPane(readme));
		about.addTab(tr("Revision"), createScrollPane(revision));
		about.addTab(tr("Contribution"), createScrollPane(contribution));

		JPanel pluginTab = new JPanel(new GridBagLayout());
		for (final PluginProxy p : Main.plugins) {
			String name = p.info.name + (p.info.version != null && !p.info.version.equals("") ? " Version: "+p.info.version : "");
			pluginTab.add(new JLabel(name), GBC.std());
			pluginTab.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
			pluginTab.add(new JButton(new AbstractAction(tr("Information")){
				public void actionPerformed(ActionEvent event) {
					StringBuilder b = new StringBuilder();
					for (Entry<String,String> e : p.info.attr.entrySet()) {
						b.append(e.getKey());
						b.append(": ");
						b.append(e.getValue());
						b.append("\n");
					}
					JTextArea a = new JTextArea(10,40);
					a.setEditable(false);
					a.setText(b.toString());
					JOptionPane.showMessageDialog(Main.parent, new JScrollPane(a));
				}
			}), GBC.eol());
			JLabel label = new JLabel("<html><i>"+(p.info.description==null?tr("no description available"):p.info.description)+"</i></html>");
			label.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
			label.setMaximumSize(new Dimension(450,1000));
			pluginTab.add(label, GBC.eop().fill(GBC.HORIZONTAL));
		}
		about.addTab(tr("Plugins"), new JScrollPane(pluginTab));

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
	 * 		of problems (e.g. no internet connection).
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
	 * @return	An read-only text area with the content of "resource"
	 */
	private static JTextArea loadFile(URL resource) {
		JTextArea area = new JTextArea(tr("File could not be found."));
		area.setEditable(false);
		Font font = Font.getFont("monospaced");
		if (font != null)
			area.setFont(font);
		if (resource == null)
			return area;
		BufferedReader in;
		try {
			in = new BufferedReader(new InputStreamReader(resource.openStream()));
			StringBuilder sb = new StringBuilder();
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				sb.append(line);
				sb.append('\n');
			}
			area.setText(sb.toString());
			area.setCaretPosition(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return area;
	}
}
