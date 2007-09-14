// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class ToolbarPreferences implements PreferenceSetting {

	private final class Move implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("<<")) {
				while (unselected.size() > 1) {
					selected.addElement(unselected.get(0));
					unselected.remove(0);
				}
			} else if (e.getActionCommand().equals("<") && unselectedList.getSelectedIndex() != -1) {
				while (unselectedList.getSelectedIndex() != -1 && unselectedList.getSelectedIndex() != unselected.size()-1) {
					selected.addElement(unselectedList.getSelectedValue());
					unselected.remove(unselectedList.getSelectedIndex());
				}
				if (unselectedList.getSelectedIndex() == unselected.size()-1)
					selected.addElement(null);
			} else if (e.getActionCommand().equals(">") && selectedList.getSelectedIndex() != -1) {
				while (selectedList.getSelectedIndex() != -1) {
					if (selectedList.getSelectedValue() != null)
						unselected.add(unselected.size()-1, selectedList.getSelectedValue());
					selected.remove(selectedList.getSelectedIndex());
				}
			} else if (e.getActionCommand().equals(">>")) {
				while (selected.size() > 0) {
					if (selected.get(0) != null)
						unselected.add(unselected.size()-1, selected.get(0));
					selected.remove(0);
				}
			} else if (e.getActionCommand().equals("up")) {
				int i = selectedList.getSelectedIndex();
				Object o = selected.get(i);
				if (i != 0) {
					selected.remove(i);
					selected.add(i-1, o);
					selectedList.setSelectedIndex(i-1);
				}
			} else if (e.getActionCommand().equals("down")) {
				int i = selectedList.getSelectedIndex();
				Object o = selected.get(i);
				if (i != selected.size()-1) {
					selected.remove(i);
					selected.add(i+1, o);
					selectedList.setSelectedIndex(i+1);
				}
			}
		}
	}
	private Move moveAction = new Move();

	/**
	 * Key: Registered name (property "toolbar" of action).
	 * Value: The action to execute.
	 */
	private Map<String, Action> actions = new HashMap<String, Action>();

	private DefaultListModel selected = new DefaultListModel();
	private DefaultListModel unselected = new DefaultListModel();
	private JList selectedList = new JList(selected);
	private JList unselectedList = new JList(unselected);

	public JToolBar control = new JToolBar();

	private JButton upButton;
	private JButton downButton;

	public ToolbarPreferences() {
		control.setFloatable(false);

		final ListCellRenderer oldRenderer = selectedList.getCellRenderer();
		ListCellRenderer renderer = new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				String s = tr("Separator");
				Icon i = ImageProvider.get("preferences/separator");
				if (value != null) {
					s = (String)((Action)value).getValue(Action.NAME);
					i = (Icon)((Action)value).getValue(Action.SMALL_ICON);
				}
				JLabel l = (JLabel)oldRenderer.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
				l.setIcon(i);
				return l;
			}
		};
		selectedList.setCellRenderer(renderer);
		unselectedList.setCellRenderer(renderer);

		unselectedList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if ((unselectedList.getSelectedIndex() != -1))
					selectedList.clearSelection();
				upButton.setEnabled(selectedList.getSelectedIndex() != -1);
				downButton.setEnabled(selectedList.getSelectedIndex() != -1);
			}
		});
		selectedList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				boolean sel = selectedList.getSelectedIndex() != -1;
				if (sel)
					unselectedList.clearSelection();
				upButton.setEnabled(sel);
				downButton.setEnabled(sel);
			}
		});
	}

	public void addGui(PreferenceDialog gui) {
		selected.removeAllElements();
		unselected.removeAllElements();
		for (Action a : actions.values())
			unselected.addElement(a);
		unselected.addElement(null);

		final JPanel left = new JPanel(new GridBagLayout());
		left.add(new JLabel("Toolbar"), GBC.eol());
		left.add(new JScrollPane(selectedList), GBC.std().fill(GBC.BOTH));

		final JPanel right = new JPanel(new GridBagLayout());
		right.add(new JLabel("Available"), GBC.eol());
		right.add(new JScrollPane(unselectedList), GBC.eol().fill(GBC.BOTH));

		final JPanel buttons = new JPanel(new GridLayout(6,1));
		buttons.add(upButton = createButton("up"));
		buttons.add(createButton("<<"));
		buttons.add(createButton("<"));
		buttons.add(createButton(">"));
		buttons.add(createButton(">>"));
		buttons.add(downButton = createButton("down"));
		upButton.setEnabled(false);
		downButton.setEnabled(false);
		
		final JPanel p = new JPanel();
		p.setLayout(new LayoutManager(){
			public void addLayoutComponent(String name, Component comp) {}
			public void removeLayoutComponent(Component comp) {}
			public Dimension minimumLayoutSize(Container parent) {
				Dimension l = left.getMinimumSize();
				Dimension r = right.getMinimumSize();
				Dimension b = buttons.getMinimumSize();
				return new Dimension(l.width+b.width+10+r.width,l.height+b.height+10+r.height);
			}
			public Dimension preferredLayoutSize(Container parent) {
				Dimension l = left.getPreferredSize();
				Dimension r = right.getPreferredSize();
				return new Dimension(l.width+r.width+10+buttons.getPreferredSize().width,Math.max(l.height, r.height));
			}
			public void layoutContainer(Container parent) {
				Dimension d = p.getSize();
				Dimension b = buttons.getPreferredSize();
				int width = d.width/2-10-b.width;
				left.setBounds(new Rectangle(0,0,width,d.height));
				right.setBounds(new Rectangle(width+10+b.width,0,width,d.height));
				buttons.setBounds(new Rectangle(width+5, d.height/2-b.height/2, b.width, b.height));
			}
		});
		p.add(left);
		p.add(buttons);
		p.add(right);

		JPanel panel = gui.createPreferenceTab("toolbar", "Toolbar customization", "Customize the elements on the toolbar.");
		panel.add(p, GBC.eol().fill(GBC.BOTH));

		for (String s : getToolString()) {
			if (s.equals("|"))
				selected.addElement(null);
			else {
				Action a = actions.get(s);
				if (a != null) {
					selected.addElement(a);
					unselected.removeElement(a);
				}
			}
		}
	}

	private String[] getToolString() {
		String s = Main.pref.get("toolbar", "download;upload;|;new;open;save;exportgpx;|;undo;redo;|;preference");
		if (s == null || s.equals("null") || s.equals(""))
			return new String[0];
		return s.split(";");
	}

	private JButton createButton(String name) {
		JButton b = new JButton();
		if (name.equals("up"))
			b.setIcon(ImageProvider.get("dialogs", "up"));
		else if (name.equals("down"))
			b.setIcon(ImageProvider.get("dialogs", "down"));
		else
			b.setText(name);
		b.addActionListener(moveAction);
		b.setActionCommand(name);
		return b;
	}

	public void ok() {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < selected.size(); ++i) {
			if (selected.get(i) == null)
				b.append("|");
			else
				b.append(((Action)selected.get(i)).getValue("toolbar"));
			b.append(";");
		}
		String s = b.toString();
		if (s.length() > 0)
			s = s.substring(0, s.length()-1);
		else
			s = "null";
		Main.pref.put("toolbar", s);
		refreshToolbarControl();
	}

	/**
	 * @return The parameter (for better chaining)
	 */
	public Action register(Action action) {
		actions.put((String)action.getValue("toolbar"), action);
		return action;
	}

	/**
	 * Parse the toolbar preference setting and construct the toolbar GUI control.
	 * 
	 * Call this, if anything has changed in the toolbar settings and you want to refresh 
	 * the toolbar content (e.g. after registering actions in a plugin)
	 */
	public void refreshToolbarControl() {
		control.removeAll();
		for (String s : getToolString()) {
			if (s.equals("|"))
				control.addSeparator();
			else
				control.add(actions.get(s));
		}
		control.setVisible(control.getComponentCount() != 0);
	}
}
