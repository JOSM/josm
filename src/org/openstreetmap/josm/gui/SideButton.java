package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.event.ActionListener;
import java.awt.Insets;
import javax.swing.Action;
import javax.swing.JButton;

import org.openstreetmap.josm.tools.ImageProvider;

public class SideButton extends JButton {
	public SideButton(Action action)
	{
		super(action);
		doStyle();
		setText(null);
	}
	public SideButton(String imagename, String property, String tooltip, ActionListener actionListener)
	{
		super(ImageProvider.get("dialogs", imagename));
		doStyle();
		setActionCommand(imagename);
		addActionListener(actionListener);
		setToolTipText(tooltip);
	}
	public SideButton(String name, String imagename, String property, String tooltip, int mnemonic, ActionListener actionListener)
	{
		super(tr(name), ImageProvider.get("dialogs", imagename));
		setMnemonic(mnemonic);
		setup(name, property, tooltip, actionListener);
	}
	public SideButton(String name, String imagename, String property, String tooltip, ActionListener actionListener)
	{
		super(tr(name), ImageProvider.get("dialogs", imagename));
		setup(name, property, tooltip, actionListener);
	}
	private void setup(String name, String property, String tooltip, ActionListener actionListener)
	{
		doStyle();
		setActionCommand(name);
		addActionListener(actionListener);
		setToolTipText(tooltip);
		putClientProperty("help", "Dialog/"+property+"/"+name);
	}
	private void doStyle()
	{
		setMargin(new Insets(1,1,1,1));
		setIconTextGap(2);
	}
}
