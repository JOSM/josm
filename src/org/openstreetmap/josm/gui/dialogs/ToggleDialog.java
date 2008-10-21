// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ShortCut;

/**
 * This class is a toggle dialog that can be turned on and off. It is attached
 * to a ButtonModel.
 *
 * @author imi
 */
public class ToggleDialog extends JPanel implements Helpful {

	public final class ToggleDialogAction extends JosmAction {
		public final String prefname;
		public AbstractButton button;

		private ToggleDialogAction(String name, String iconName, String tooltip, ShortCut shortCut, String prefname) {
			super(name, iconName, tooltip, shortCut, false);
			this.prefname = prefname;
		}

		public void actionPerformed(ActionEvent e) {
			if (e != null && !(e.getSource() instanceof AbstractButton))
				button.setSelected(!button.isSelected());
			setVisible(button.isSelected());
			Main.pref.put(prefname+".visible", button.isSelected());
		}
	}

	/**
	 * The action to toggle this dialog.
	 */
	public ToggleDialogAction action;
	public final String prefName;

	public JPanel parent;
	private final JPanel titleBar = new JPanel(new GridBagLayout());

	@Deprecated
	public ToggleDialog(final String name, String iconName, String tooltip, int shortCut, int preferredHeight) {
		super(new BorderLayout());
		this.prefName = iconName;
		ToggleDialogInit(name, iconName, tooltip, ShortCut.registerShortCut("auto:"+name, tooltip, shortCut, ShortCut.GROUP_LAYER), preferredHeight);
	}

	public ToggleDialog(final String name, String iconName, String tooltip, ShortCut shortCut, int preferredHeight) {
		super(new BorderLayout());
		this.prefName = iconName;
		ToggleDialogInit(name, iconName, tooltip, shortCut, preferredHeight);
	}

	private void ToggleDialogInit(final String name, String iconName, String tooltip, ShortCut shortCut, int preferredHeight) {
		setPreferredSize(new Dimension(330,preferredHeight));
		action = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, shortCut, iconName);
		String helpId = "Dialog/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
		action.putValue("help", helpId.substring(0, helpId.length()-6));
		setLayout(new BorderLayout());

		titleBar.add(new JLabel(name), GBC.std());
		titleBar.add(Box.createHorizontalGlue(),GBC.std().fill(GBC.HORIZONTAL));

		JButton sticky = new JButton(ImageProvider.get("misc", "sticky"));
		sticky.setBorder(BorderFactory.createEmptyBorder());
		final ActionListener stickyActionListener = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				final JFrame f = new JFrame(name);
				try {f.setAlwaysOnTop(true);} catch (SecurityException e1) {}
				parent.remove(ToggleDialog.this);
				f.getContentPane().add(ToggleDialog.this);
				f.addWindowListener(new WindowAdapter(){
					@Override public void windowClosing(WindowEvent e) {
						titleBar.setVisible(true);
						f.getContentPane().removeAll();
						parent.add(ToggleDialog.this);
						f.dispose();

						// doLayout() - workaround
						setVisible(false);
						setVisible(true);

						Main.pref.put(action.prefname+".docked", true);
					}
				});
				f.addComponentListener(new ComponentAdapter(){
					@Override public void componentMoved(ComponentEvent e) {
						Main.pref.put(action.prefname+".bounds", f.getX()+","+f.getY()+","+f.getWidth()+","+f.getHeight());
                    }
				});
				String bounds = Main.pref.get(action.prefname+".bounds",null);
				if (bounds != null) {
					String[] b = bounds.split(",");
					f.setBounds(Integer.parseInt(b[0]),Integer.parseInt(b[1]),Integer.parseInt(b[2]),Integer.parseInt(b[3]));
				} else
					f.pack();
				Main.pref.put(action.prefname+".docked", false);
				f.setVisible(true);
				titleBar.setVisible(false);

				// doLayout() - workaround
				parent.setVisible(false);
				parent.setVisible(true);
			}
		};
		sticky.addActionListener(stickyActionListener);

		titleBar.add(sticky);
		add(titleBar, BorderLayout.NORTH);

		setVisible(false);
		setBorder(BorderFactory.createEtchedBorder());

		if (!Main.pref.getBoolean(action.prefname+".docked", true)) {
			EventQueue.invokeLater(new Runnable(){
				public void run() {
					stickyActionListener.actionPerformed(null);
                }
			});
		}
	}

	public String helpTopic() {
		String help = getClass().getName();
		help = help.substring(help.lastIndexOf('.')+1, help.length()-6);
		return "Dialog/"+help;
	}
}
