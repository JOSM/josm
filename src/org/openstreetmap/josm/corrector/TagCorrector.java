// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;

public abstract class TagCorrector<P extends OsmPrimitive> {

	public abstract boolean execute(P primitive);

	protected boolean applyCorrections(List<TagCorrection> tagCorrections,
	        P primitive, String description) {

		boolean updated = false;

		if (tagCorrections != null && tagCorrections.size() > 0) {

			final TagCorrectionTable table = TagCorrectionTable
			        .create(tagCorrections);
			final JScrollPane scrollPane = new JScrollPane(table);

	    	final JPanel p = new JPanel(new GridBagLayout());

			final JMultilineLabel label1 = new JMultilineLabel(description);
			label1.setMaxWidth(400);
			p.add(label1, GBC.eop());
			
			final JMultilineLabel label2 = new JMultilineLabel(tr("Please select which property changes you want to apply."));
			label2.setMaxWidth(400);
			p.add(label2, GBC.eop());
	    	p.add(scrollPane, GBC.eol());
			
			int answer = JOptionPane.showConfirmDialog(Main.parent, p,
			        tr("Automatic tag correction"),
			        JOptionPane.OK_CANCEL_OPTION);

			if (answer == JOptionPane.OK_OPTION) {
				for (int i = 0; i < tagCorrections.size(); i++) {
					if (table.getTagCorrectionTableModel().getApply(i)) {
						TagCorrection tagCorrection = tagCorrections.get(i);
						if (tagCorrection.isKeyChanged())
							primitive.remove(tagCorrection.oldKey);
						primitive.put(tagCorrection.newKey, tagCorrection
						        .newValue);
						updated = true;
					}
				}
			}
		}

		return updated;
	}

}
