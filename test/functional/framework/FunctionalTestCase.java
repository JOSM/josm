// License: GPL. Copyright 2007 by Immanuel Scholz and others
package framework;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import junit.extensions.jfcunit.JFCTestCase;
import junit.extensions.jfcunit.RobotTestHelper;
import junit.extensions.jfcunit.eventdata.DragEventData;
import junit.extensions.jfcunit.eventdata.KeyEventData;
import junit.extensions.jfcunit.eventdata.MouseEventData;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;

abstract public class FunctionalTestCase extends JFCTestCase {

	private KeyStroke getKey(String s) {
    	int key = 0;
    	int modifier = 0;
    	s = s.toUpperCase();
    	if (s.startsWith("CTRL")) {
    		modifier |= InputEvent.CTRL_MASK;
    		s = s.substring(4);
    	}
    	if (s.startsWith("-"))
    		s = s.substring(1);
    	if (s.startsWith("SHIFT")) {
    		modifier |= InputEvent.SHIFT_MASK;
    		s = s.substring(5);
    	}
    	if (s.startsWith("-"))
    		s = s.substring(1);
    	if (s.startsWith("ALT")) {
    		modifier |= InputEvent.ALT_MASK;
    		s = s.substring(3);
    	}
    	if (s.startsWith("-"))
    		s = s.substring(1);
    	if (s.matches("^F[1-9][012]?$"))
    		key = KeyEvent.VK_F1 + Integer.parseInt(s.substring(1)) - 1;
    	else if (s.length() == 0)
    		key = 0;
    	else if (s.length() != 1)
    		throw new RuntimeException("Illegal key description '"+s+"'.");
    	else
    		key = s.charAt(0);
    	return KeyStroke.getKeyStroke(key, modifier);
    }

	@Override protected void setUp() throws Exception {
		super.setUp();
		setHelper(new RobotTestHelper());
		
		Main.ds = new DataSet();
		Main.pref = new Preferences();
		if (Main.map != null)
			Main.main.setMapFrame(null);
		
		MainApplication.main(new String[]{});
	}
	
	@Override protected void tearDown() throws Exception {
		Main.parent.setVisible(false);
		super.tearDown();
	}

	protected Component find(Component c, String search) {
		if (c == null)
			return null;
		if (search.equals(c.getName()))
			return c;
		if (c instanceof JLabel && search.equals(((JLabel)c).getText()))
			return c;
		if (c instanceof AbstractButton && search.equals(((AbstractButton)c).getText()))
			return c;

		if (c instanceof Container) {
			Container ct = (Container)c;
			for (int i = 0; i < ct.getComponentCount(); ++i) {
				Component result = find(ct.getComponent(i), search);
				if (result != null)
					return result;
			}
		}
		if (c instanceof JMenu) {
			JMenu menu = (JMenu)c;
			for (int i = 0; i < menu.getMenuComponentCount(); ++i) {
				Component result = find(menu.getMenuComponent(i), search);
				if (result != null)
					return result;
			}
		}
		if (c instanceof JFrame) {
			Component result = find(((JFrame)c).getJMenuBar(), search);
			if (result != null)
				return result;
		}
		return null;
	}
	
	protected Component find(String s) {
		Container frame = SwingUtilities.getAncestorOfClass(Window.class, KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
		return find(frame, s);
	}

	protected void key(String... keys) {
		for (String s : keys) {
			KeyStroke k = getKey(s);
			getHelper().sendKeyAction(new KeyEventData(this, Main.contentPane, k.getKeyCode(), k.getModifiers(), 0));
		}
	}

	protected void key(int... keys) {
		for (int i : keys) {
			getHelper().sendKeyAction(new KeyEventData(this, Main.contentPane, i, 0, 0));
		}
	}
	
	/**
	 * Clicks on a spot on the main map (should be open by now)
	 */
	protected void click(int x, int y) {
		getHelper().enterClickAndLeave(new MouseEventData(this, Main.map, 1, MouseEvent.BUTTON1_MASK, false, 0, new Point(x,y)));
	}

	protected void click(int x, int y, String modifier) {
		getHelper().enterClickAndLeave(new MouseEventData(this, Main.map, 1, MouseEvent.BUTTON1_MASK + getKey(modifier).getModifiers(), false, 0, new Point(x,y)));
	}
	
	
	protected void drag(int xfrom, int yfrom, int xto, int yto) {
		getHelper().enterDragAndLeave(new DragEventData(
				this,
				new MouseEventData(this, Main.map, 1, MouseEvent.BUTTON1_MASK, false, 0, new Point(xfrom, yfrom)),
				new MouseEventData(this, Main.map, 1, MouseEvent.BUTTON1_MASK, false, 0, new Point(xto, yto))));
	}
	

	protected void assertPopup() {
		Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		Container dlg = SwingUtilities.getAncestorOfClass(JDialog.class, focus);
		assertNotNull("Popup dialog found", dlg);
		key(KeyEvent.VK_ENTER);
	}
}
