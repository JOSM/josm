// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
		
		panel.add(new JLabel(tr("<html><h2>You are running the latest JOSM version with some geometry extensions.</h2>" +
                "<h3>New elements in the status bar will inform you about the orientation and size<br>" +
                "of the object being drawn. There is a new \"extrude\" mode that you can use to create<br>" +
                "rectangular shapes (see below for a short video on this).</h3>" +
                "<h3>There is also a new option in the tools menu that will make existing shapes into proper<br>" +
                "rectangles. Note that all this is dependend on the projection you're using; you must use<br>"+
                "a projection in which rectangles look rectangular and not skewed. Try it out.</h3>"+
                "<h3>If you dislike the helper line dangling from the mouse cursor, set the \"draw.helper-line\"<br>"+
                "preference to \"false\"."+
		"</h3>")), GBC.eol());

        /*
		boolean changePrefs = ! (
			"0.5".equals(Main.pref.get("osm-server.version", "0.5")) &&
			"0.5".equals(Main.pref.get("osm-server.additionalVersions", "0.5"))
		);
		
		if (changePrefs) {
			Main.pref.put("osm-server.version", null);
			Main.pref.put("osm-server.additional-versions", null);
			panel.add(new JLabel(tr("<html><h3>Your preferences have been changed by removing <b>osm-server.version</b> and/or <b>osm-server.additional-versions</b> which were still referring to 0.4.</h3></html>")), GBC.eol());
		}
        */
		
		addLine("wiki", tr("Read the [Wiki page on API 0.5]"));
		addLine("extrudevideo", tr("Short (sound-less) [video] demonstrating the new \"extrude\" feature"));

		addLine("audio", tr("This version also has built-in support for [Audio Mapping] with continuously recorded sound tracks."));

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
		else if (e.getActionCommand().equals("audio"))
			OpenBrowser.displayUrl("http://josm.openstreetmap.de/wiki/Help/HowTo/AudioMapping");
		else if (e.getActionCommand().equals("wiki"))
			OpenBrowser.displayUrl("http://wiki.openstreetmap.org/index.php?title=OSM_Protocol_Version_0.5");
		else if (e.getActionCommand().equals("tutorial"))
			OpenBrowser.displayUrl("http://josm.openstreetmap.de/wiki/TutorialVideos");
		else if (e.getActionCommand().equals("mailinglist"))
			OpenBrowser.displayUrl("mailto:newbies-subscribe@openstreetmap.org?subject=subscribe");
		else if (e.getActionCommand().equals("extrudevideo"))
			OpenBrowser.displayUrl("http://josm.openstreetmap.de/download/tutorials/josm-extrude-feature.mpeg");
    }
}
