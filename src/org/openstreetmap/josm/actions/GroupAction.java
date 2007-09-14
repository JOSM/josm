// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ShortCutLabel;
import org.openstreetmap.josm.tools.ImageProvider.OverlayPosition;


public class GroupAction extends JosmAction {

	protected final List<Action> actions = new ArrayList<Action>();
	private int current = -1;
	private String shortCutName = "";

	private PropertyChangeListener forwardActiveListener = new PropertyChangeListener(){
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("active"))
				putValue("active", evt.getNewValue());
		}
	};

	protected void setCurrent(int current) {
		if (this.current != -1)
			actions.get(this.current).removePropertyChangeListener(forwardActiveListener);
		actions.get(current).addPropertyChangeListener(forwardActiveListener);

		this.current = current;
		putValue(SMALL_ICON, ImageProvider.overlay((Icon)actions.get(current).getValue(SMALL_ICON), "overlay/right", OverlayPosition.SOUTHEAST));
		Object tooltip = actions.get(current).getValue(SHORT_DESCRIPTION);
		putValue(SHORT_DESCRIPTION, "<html>"+tooltip+" <font size='-2'>"+shortCutName+"</font>&nbsp;</html>");
	}

	public GroupAction(int shortCut, int modifiers) {
		String idName = getClass().getName();
		Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(shortCut, modifiers), idName);
		Main.contentPane.getActionMap().put(idName, this);
		shortCutName = ShortCutLabel.name(shortCut, modifiers);
		Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(shortCut, KeyEvent.SHIFT_DOWN_MASK), idName+".cycle");
		Main.contentPane.getActionMap().put(idName+".cycle", new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				setCurrent((current+1)%actions.size());
				actions.get(current).actionPerformed(e);
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof IconToggleButton && ((IconToggleButton)e.getSource()).groupbutton) {
			IconToggleButton b = (IconToggleButton)e.getSource();
			b.setSelected(!b.isSelected());
			openPopup(b);
		} else
			actions.get(current).actionPerformed(e);
	}

	private void openPopup(IconToggleButton b) {
		JPopupMenu popup = new JPopupMenu();
		for (int i = 0; i < actions.size(); ++i) {
			final int j = i;
			JMenuItem item = new JMenuItem(actions.get(i));
			item.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					setCurrent(j);
				}
			});
			popup.add(item);
		}
		popup.show(b, b.getWidth(), 0);
	}
}
