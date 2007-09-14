// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;


import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ShortCutLabel;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 * 
 * destroy() from interface Destroyable is called e.g. for MapModes, when the last layer has
 * been removed and so the mapframe will be destroyed. For other JosmActions, destroy() may never
 * be called (currently).
 * 
 * @author imi
 */
abstract public class JosmAction extends AbstractAction implements Destroyable {

	private KeyStroke shortCut;

	public JosmAction(String name, String iconName, String tooltip, int shortCut, int modifier, boolean register) {
		super(name, ImageProvider.get(iconName));
		setHelpId();
		String scl = ShortCutLabel.name(shortCut, modifier);
		putValue(SHORT_DESCRIPTION, "<html>"+tooltip+" <font size='-2'>"+scl+"</font>"+(scl.equals("")?"":"&nbsp;")+"</html>");
		if (shortCut != 0) {
			this.shortCut = KeyStroke.getKeyStroke(shortCut, modifier);
			Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(this.shortCut, name);
			Main.contentPane.getActionMap().put(name, this);
		}
        putValue("toolbar", iconName);
        if (register)
        	Main.toolbar.register(this);
	}

	public void destroy() {
		if (shortCut != null) {
			Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(shortCut);
			Main.contentPane.getActionMap().remove(shortCut);
		}
	}
	
	public JosmAction() {
		setHelpId();
	}


	private void setHelpId() {
		String helpId = "Action/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
		if (helpId.endsWith("Action"))
			helpId = helpId.substring(0, helpId.length()-6);
		putValue("help", helpId);
	}
}
