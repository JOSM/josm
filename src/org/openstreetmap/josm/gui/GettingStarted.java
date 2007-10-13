// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
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
			String link = "<html><h3>"+text.replaceAll("\\[([^\\]]*)\\]", "<a href='"+action+"'>$1</a>")+"</h3></html>";
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
		
		panel.add(new JLabel("<html><h2>You are running the latest \"modeless\" JOSM version.</h2>" +
				"<h3>This version (almost) does away with the old edit modes, like \"add node and connect\"<br>" +
				"etc.; instead, there are only four modes: zoom, select, edit, and delete." +
				"<br>The edit mode will do what you want in most cases (also see the mini help about<br>" +
				"modifier keys at the bottom of the screen).</h3>" +
                "<h3>If this is the first time you use JOSM since 08 October, you will also find that with the<br>" +
                "0.5 API, segments have gone and relations have been added. You will find general<br>" +
                "information about the changes on the OSM wiki, and there's a page on using relations<br>"+
                "in the JOSM online help." +
		"</h3>"), GBC.eol());
		
		// remove these two keys from preferences if present
		boolean changePrefs = ! (
			"0.5".equals(Main.pref.get("osm-server.version", "0.5")) &&
			"0.5".equals(Main.pref.get("osm-server.additionalVersions", "0.5"))
		);
		
		if (changePrefs) {
			Main.pref.put("osm-server.version", null);
			Main.pref.put("osm-server.additional-versions", null);
			panel.add(new JLabel("<html><h3>Your preferences have been changed by removing <b>osm-server.version</b> and/or <b>osm-server.additional-versions</b> which were still referring to 0.4.</h3></html>"), GBC.eol());
		}
		
		addLine("wiki", "Read the [Wiki page on API 0.5]");
		addGettingStarted();
		addGettingHelp();
		
		panel.add(GBC.glue(0,70), GBC.eol());
		//panel.setMinimumSize(new Dimension(400, 600));
		add(panel);
    }

	public void addGettingStarted() {
		addCategory(tr("Getting Started"));
		addLine("download",tr("[Download] some data from the OSM server"));
	}
	
	public void addGettingHelp() {
	    addCategory(tr("Getting Help"));
		addLine("help",tr("Open the [online help] (english only)"));
		addLine("mailinglist",tr("Join the newbie [mailing list]"));
    }

	public void addCategory(String category) {
	    panel.add(new JLabel("<html><h2>"+category+"</h2></html>"), GBC.eol().fill(GBC.HORIZONTAL).insets(0,20,0,0));
    }

	public void addLine(String action, String text) {
	    JButton button = new JButton(ImageProvider.get("getting_started"));
        button.setBorder(null);
        button.addActionListener(this);
        button.setActionCommand(action);
		panel.add(button, GBC.std().insets(20,0,5,0));
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
