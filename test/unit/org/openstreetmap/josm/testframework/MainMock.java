// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.testframework;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.BeforeClass;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.gui.PleaseWaitDialog;

public class MainMock {

	private static JDialog lastPopup;

	@Before public void clearFoundPopup() {
		lastPopup = null;
	}
	
	@BeforeClass public static void mockMain() throws Exception {
		Main.pref = new Preferences(){
			@Override protected void save() {}
			@Override public void load() throws IOException {}
			@Override public Collection<Bookmark> loadBookmarks() throws IOException {return Collections.emptyList();}
			@Override public void saveBookmarks(Collection<Bookmark> bookmarks) throws IOException {}
		};
		Main.parent = new JFrame();
		Main.proj = new Epsg4326();
		Main.pleaseWaitDlg = new PleaseWaitDialog(Main.parent);
		Main.main = new Main(){};
	}

	@BeforeClass public static void startPopupKiller() {
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener(){
			public void eventDispatched(AWTEvent event) {
				if (event.getSource() instanceof JButton) {
					JButton b = (JButton)event.getSource();
					if (b.getParent().getParent() instanceof JOptionPane) {
						lastPopup = (JDialog)SwingUtilities.getRoot(b);
						b.doClick();
					}
				}
            }
		}, AWTEvent.FOCUS_EVENT_MASK);
    }

	public void assertPopup() {
		waitForPopup();
		lastPopup = null;
	}

	public JDialog waitForPopup() {
	    for (int i = 0; i < 100; ++i) {
			if (lastPopup != null)
				return lastPopup;
			try {Thread.sleep(10);} catch (InterruptedException e) {}
		}
		throw new AssertionError("Expected Popup dialog");
    }
}
