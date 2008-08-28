// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.GBC;

public class SearchAction extends JosmAction {
	public static enum SearchMode {replace, add, remove}

    private String lastSearch = "";

    public SearchAction() {
    	super(tr("Search ..."), "dialogs/search", tr("Search for objects."), KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK, true);
    }

    public void actionPerformed(ActionEvent e) {
    	if (Main.map == null) {
    		JOptionPane.showMessageDialog(Main.parent, tr("No data loaded."));
    		return;
    	}
    	JLabel label = new JLabel(tr("Please enter a search string."));
    	final JTextField input = new JTextField(lastSearch);
    	input.setToolTipText(tr("<html>Fulltext search:<ul>" +
    			"<li><b>Baker Street</b> - 'Baker' and 'Street' in any key or name.</li>" +
    			"<li><b>\"Baker Street\"</b> - 'Baker Street' in any key or name.</li>" +
    			"<li><b>name:Bak</b> - 'Bak' anywhere in the name.</li>" +
    			"<li><b>-name:Bak</b> - not 'Bak' in the name.</li>" +
    			"<li><b>foot:</b> - key=foot set to any value.</li>" +
    			"<li>Special targets:</li>" +
    			"<li><b>type:</b> - type of the object (<b>node</b>, <b>way</b>, <b>relation</b>)</li>" +
    			"<li><b>user:</b>... - all objects changed by user</li>" +
    			"<li><b>id:</b>... - object with given ID</li>" +
    			"<li><b>nodes:</b>... - object with given number of nodes</li>" +
    			"<li><b>modified</b> - all changed objects</li>" +
    			"<li><b>incomplete</b> - all incomplete objects</li>" +
    			"<li>Use <b>|</b> or <b>OR</b> to combine with logical or</li>" +
    			"<li>Use <b>\"</b> to quote operators (e.g. if key contains :)</li>" +
    	"</ul></html>"));
    
    	JRadioButton replace = new JRadioButton(tr("replace selection"), true);
    	JRadioButton add = new JRadioButton(tr("add to selection"), false);
    	JRadioButton remove = new JRadioButton(tr("remove from selection"), false);
    	ButtonGroup bg = new ButtonGroup();
    	bg.add(replace);
    	bg.add(add);
    	bg.add(remove);
    	
    	JCheckBox caseSensitive = new JCheckBox(tr("case sensitive"), false);
    
    	JPanel p = new JPanel(new GridBagLayout());
    	p.add(label, GBC.eop());
    	p.add(input, GBC.eop().fill(GBC.HORIZONTAL));
    	p.add(replace, GBC.eol());
    	p.add(add, GBC.eol());
    	p.add(remove, GBC.eop());
    	p.add(caseSensitive, GBC.eol());
    	JOptionPane pane = new JOptionPane(p, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null){
    		@Override public void selectInitialValue() {
    			input.requestFocusInWindow();
    			input.selectAll();
    		}
    	};
    	pane.createDialog(Main.parent,tr("Search")).setVisible(true);
    	if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
    		return;
    	lastSearch = input.getText();
    	SearchAction.SearchMode mode = replace.isSelected() ? SearchAction.SearchMode.replace : (add.isSelected() ? SearchAction.SearchMode.add : SearchAction.SearchMode.remove);
    	search(lastSearch, mode, caseSensitive.isSelected());
    }

	public static void search(String search, SearchMode mode, boolean caseSensitive) {
    	if (search.startsWith("http://") || search.startsWith("ftp://") || search.startsWith("https://") || search.startsWith("file:/")) {
    		SelectionWebsiteLoader loader = new SelectionWebsiteLoader(search, mode);
    		if (loader.url != null) {
    			Main.worker.execute(loader);
    			return;
    		}
    	}
		try {
			Collection<OsmPrimitive> sel = Main.ds.getSelected();
			SearchCompiler.Match matcher = SearchCompiler.compile(search, caseSensitive);
			for (OsmPrimitive osm : Main.ds.allNonDeletedCompletePrimitives()) {
				if (mode == SearchMode.replace) {
					if (matcher.match(osm))
						sel.add(osm);
					else
						sel.remove(osm);
				} else if (mode == SearchMode.add && !osm.selected && matcher.match(osm))
					sel.add(osm);
				else if (mode == SearchMode.remove && osm.selected && matcher.match(osm))
					sel.remove(osm);
			}
			Main.ds.setSelected(sel);
		} catch (SearchCompiler.ParseError e) {
			JOptionPane.showMessageDialog(Main.parent, e.getMessage());
		}
    }
}
