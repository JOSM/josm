// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;


/**
 * This class read encapsulate one tagging preset. A class method can
 * read in all predefined presets, either shipped with JOSM or that are
 * in the config directory.
 * 
 * It is also able to construct dialogs out of preset definitions.
 */
public class TaggingPreset extends AbstractAction {

	public static abstract class Item {
		public boolean focus = false;
		abstract void addToPanel(JPanel p);
		abstract void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds);
		boolean requestFocusInWindow() {return false;}
	}

	public static class Text extends Item {
		public String key;
		public String text;
		public String default_;
		public boolean delete_if_empty = false;

		private JTextField value = new JTextField();

		@Override public void addToPanel(JPanel p) {
			value.setText(default_ == null ? "" : default_);
			p.add(new JLabel(text), GBC.std().insets(0,0,10,0));
			p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
		}
		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			String v = value.getText();
			if (delete_if_empty && v.length() == 0)
				v = null;
			cmds.add(new ChangePropertyCommand(sel, key, v));
		}
		@Override boolean requestFocusInWindow() {return value.requestFocusInWindow();}
	}

	public static class Check extends Item {
		public String key;
		public String text;
		public boolean default_ = false;

		private JCheckBox check = new JCheckBox();

		@Override public void addToPanel(JPanel p) {
			check.setSelected(default_);
			check.setText(text);
			p.add(check, GBC.eol().fill(GBC.HORIZONTAL));
		}
		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			cmds.add(new ChangePropertyCommand(sel, key, check.isSelected() ? "true" : null));
		}
		@Override boolean requestFocusInWindow() {return check.requestFocusInWindow();}
	}

	public static class Combo extends Item {
		public String key;
		public String text;
		public String values;
		public String display_values;
		public String default_;
		public boolean delete_if_empty = false;
		public boolean editable = true;

		private JComboBox combo;

		@Override public void addToPanel(JPanel p) {
			combo = new JComboBox((display_values != null ? display_values : values).split(","));
			combo.setEditable(editable);
			combo.setSelectedItem(default_);
			p.add(new JLabel(text), GBC.std().insets(0,0,10,0));
			p.add(combo, GBC.eol().fill(GBC.HORIZONTAL));
		}
		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			String v = combo.getSelectedIndex() == -1 ? null : values.split(",")[combo.getSelectedIndex()];
			String str = combo.isEditable()?combo.getEditor().getItem().toString() : v;
			if (delete_if_empty && str != null && str.length() == 0)
				str = null;
			cmds.add(new ChangePropertyCommand(sel, key, str));
		}
		@Override boolean requestFocusInWindow() {return combo.requestFocusInWindow();}
	}

	public static class Label extends Item {
		public String text;

		@Override public void addToPanel(JPanel p) {
			p.add(new JLabel(text), GBC.eol());
		}
		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
	}

	public static class Key extends Item {
		public String key;
		public String value;

		@Override public void addToPanel(JPanel p) {}
		@Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			cmds.add(new ChangePropertyCommand(sel, key, value != null && !value.equals("") ? value : null));
		}
	}

	/**
	 * The types as preparsed collection.
	 */
	public Collection<Class<?>> types;
	private List<Item> data = new LinkedList<Item>();

	/**
	 * Create an empty tagging preset. This will not have any items and
	 * will be an empty string as text. createPanel will return null.
	 * Use this as default item for "do not select anything".
	 */
	public TaggingPreset() {}

	/**
	 * Called from the XML parser to set the name of the tagging preset
	 */
	public void setName(String name) {
		putValue(Action.NAME, name);
		putValue("toolbar", "tagging_"+name);
	}

	/**
	 * Called from the XML parser to set the icon
	 * 
	 * FIXME for Java 1.6 - use 24x24 icons for LARGE_ICON_KEY (button bar)
	 * and the 16x16 icons for SMALL_ICON.
	 */
	public void setIcon(String iconName) {
		ImageIcon icon = ImageProvider.getIfAvailable(null, iconName);
		if (icon == null)
			icon = new ImageIcon(iconName);
		if (Math.max(icon.getIconHeight(), icon.getIconWidth()) != 16)
			icon = new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		putValue(Action.SMALL_ICON, icon);
	}

	/**
	 * Called from the XML parser to set the types, this preset affects
	 */
	public void setType(String types) throws SAXException {
		try {
			for (String type : types.split(",")) {
				type = Character.toUpperCase(type.charAt(0))+type.substring(1);
				if (this.types == null)
					this.types = new LinkedList<Class<?>>();
				this.types.add(Class.forName("org.openstreetmap.josm.data.osm."+type));
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new SAXException(tr("Unknown type"));
		}
	}

	public static List<TaggingPreset> readAll(InputStream inStream) throws SAXException {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			in = new BufferedReader(new InputStreamReader(inStream));
		}
		XmlObjectParser parser = new XmlObjectParser();
		parser.mapOnStart("item", TaggingPreset.class);
		parser.map("text", Text.class);
		parser.map("check", Check.class);
		parser.map("combo", Combo.class);
		parser.map("label", Label.class);
		parser.map("key", Key.class);
		LinkedList<TaggingPreset> all = new LinkedList<TaggingPreset>();
		parser.start(in);
		while(parser.hasNext()) {
			Object o = parser.next();
			if (o instanceof TaggingPreset) {
				all.add((TaggingPreset)o);
				Main.toolbar.register((TaggingPreset)o);
			} else
				all.getLast().data.add((Item)o);
		}
		return all;
	}

	public static Collection<TaggingPreset> readFromPreferences() {
		LinkedList<TaggingPreset> allPresets = new LinkedList<TaggingPreset>();
		String allTaggingPresets = Main.pref.get("taggingpreset.sources");
		
		if (Main.pref.getBoolean("taggingpreset.enable-defaults", true)) {
			InputStream in = Main.class.getResourceAsStream("/presets/presets.xml");
			try {
				allPresets.addAll(TaggingPreset.readAll(in));
			} catch (SAXException x) {
				JOptionPane.showMessageDialog(Main.parent, tr("Error parsing presets.xml: ")+x.getMessage());
			}
		}
		
		StringTokenizer st = new StringTokenizer(allTaggingPresets, ";");
		while (st.hasMoreTokens()) {
			InputStream in = null;
			String source = st.nextToken();
			try {
				if (source.startsWith("http") || source.startsWith("ftp") || source.startsWith("file"))
					in = new URL(source).openStream();
				else if (source.startsWith("resource://")) 
					in = Main.class.getResourceAsStream(source.substring("resource:/".length()));
				else
					in = new FileInputStream(source);
				allPresets.addAll(TaggingPreset.readAll(in));
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Main.parent, tr("Could not read tagging preset source: {0}",source));
			} catch (SAXException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Main.parent, tr("Error parsing {0}: ", source)+e.getMessage());
			}
		}
		return allPresets;
	}

	public JPanel createPanel() {
		if (data == null)
			return null;
		JPanel p = new JPanel(new GridBagLayout());
		for (Item i : data)
			i.addToPanel(p);
		return p;
	}

	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		JPanel p = createPanel();
		if (p == null)
			return;
		int answer = JOptionPane.OK_OPTION;
		if (p.getComponentCount() != 0) {
			final JOptionPane optionPane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
				@Override public void selectInitialValue() {
					for (Item i : data) {
						if (i.focus) {
							i.requestFocusInWindow();
							return;
						}
					}
				}
			};
			optionPane.createDialog(Main.parent, trn("Change {0} object", "Change {0} objects", sel.size(), sel.size())).setVisible(true);
			Object answerObj = optionPane.getValue();
			if (answerObj == null || answerObj == JOptionPane.UNINITIALIZED_VALUE ||
					(answerObj instanceof Integer && (Integer)answerObj != JOptionPane.OK_OPTION))
				answer = JOptionPane.CANCEL_OPTION;
		}
		if (answer == JOptionPane.OK_OPTION) {
			Command cmd = createCommand(Main.ds.getSelected());
			if (cmd != null)
				Main.main.undoRedo.add(cmd);
		}
		Main.ds.setSelected(Main.ds.getSelected()); // force update
	}

	private Command createCommand(Collection<OsmPrimitive> participants) {
		Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
		for (OsmPrimitive osm : participants)
			if (types == null || types.contains(osm.getClass()))
				sel.add(osm);
		if (sel.isEmpty())
			return null;

		List<Command> cmds = new LinkedList<Command>();
		for (Item i : data)
			i.addCommands(sel, cmds);
		if (cmds.size() == 0)
			return null;
		else if (cmds.size() == 1)
			return cmds.get(0);
		else
			return new SequenceCommand(tr("Change Properties"), cmds);
	}
}
