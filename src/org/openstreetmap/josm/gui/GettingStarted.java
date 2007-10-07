// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;

public class GettingStarted extends JPanel implements ActionListener {

	private JPanel panel;

	public class LinkLabel extends JEditorPane implements HyperlinkListener {
		private String action;
		public LinkLabel(String text, String action) {
			this.action = action;
			String normalized = text.replaceAll("\\[([^\\]]*)\\]", "$1");
			String link = "<html><h2>"+text.replaceAll("\\[([^\\]]*)\\]", "<a href='"+action+"'>$1</a>")+"</h2></html>";
			setContentType("text/html");
			setText(link);
			setToolTipText(normalized);
			setEditable(false);
			setOpaque(false);
			addHyperlinkListener(this);
        }
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				actionPerformed(new ActionEvent(e.getSource(), 0, action));
        }
    }

	public GettingStarted() {
		super(new GridBagLayout());
		
		panel = new JPanel(new GridBagLayout());
		
		panel.add(new JLabel("<html><h2>You are running the new JOSM version compatible with the 0.5 API.</h2>" +
				"<h3>You cannot load old OSM files with this version, but there are converter scripts to make your 0.4 files 0.5 compatible.</h3>"+
                "<h3>The JOSM interface hasn't changed a lot: Segments are gone, and Relations have been added.<br>You will find general information about the changes on the OSM wiki,<br>and there's a page on working with relations in the JOSM online help." +
		"</h3>"), GBC.eol());
		
		// remove these two keys from preferences if present
		boolean changePrefs = ! (
			"0.5".equals(Main.pref.get("osm-server.version", "0.5")) &&
			"0.5".equals(Main.pref.get("osm-server.additionalVersions", "0.5"))
		);
		
		if (changePrefs) {;
			Main.pref.put("osm-server.version", null);
			Main.pref.put("osm-server.additional-versions", null);
			panel.add(new JLabel("<html><h3>Your preferences have been changed by removing <b>osm-server.version</b> and/or <b>osm-server.additional-versions</b> which were still to referring 0.4.</h3></html>"), GBC.eol());
		}
		
		addLine("wiki", "Read the [Wiki page on API 0.5]");
		addGettingStarted();
		addGettingHelp();
		
		panel.add(GBC.glue(0,140), GBC.eol());
		add(panel);
    }

	public void addGettingStarted() {
		addCategory(tr("Getting Started"));
		addLine("download",tr("[Download] some data from the OSM server"));
	}
	
	public void addGettingHelp() {
	    addCategory(tr("Getting Help"));
		addLine("help",tr("Open the [online help] (english only)"));
		addLine("tutorial",tr("Watch some [tutorial videos]"));
		addLine("mailinglist",tr("Join the newbie [mailing list]"));
    }

	public void addCategory(String category) {
	    panel.add(new JLabel("<html><h1>"+category+"</h1></html>"), GBC.eop().fill(GBC.HORIZONTAL).insets(0,20,0,0));
    }

	public void addLine(String action, String text) {
	    JButton button = new JButton(ImageProvider.get("getting_started"));
        button.setBorder(null);
        button.addActionListener(this);
        button.setActionCommand(action);
		panel.add(button, GBC.std().insets(40,0,15,0));
		panel.add(new LinkLabel(text,action),GBC.eol());
    }


	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("download"))
			Main.main.menu.download.actionPerformed(e);
		else if (e.getActionCommand().equals("help"))
			Main.main.menu.help.actionPerformed(e);
		else if (e.getActionCommand().equals("wiki"))
			OpenBrowser.displayUrl("http://wiki.openstreetmap.org/index.php?title=OSM_Protocol_Version_0.5");
		else if (e.getActionCommand().equals("tutorial"))
			OpenBrowser.displayUrl("http://josm.openstreetmap.de/wiki/TutorialVideos");
		else if (e.getActionCommand().equals("mailinglist"))
			OpenBrowser.displayUrl("mailto:newbies-subscribe@openstreetmap.org?subject=subscribe");
    }
}
