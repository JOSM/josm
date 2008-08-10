// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;

public abstract class TagCorrector<P extends OsmPrimitive> {

	public abstract Collection<ChangePropertyCommand> execute(P primitive);

	protected Collection<ChangePropertyCommand> applyCorrections(
	        Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap,
	        String description) {

		boolean hasCorrections = false;
		for (List<TagCorrection> tagCorrectionList : tagCorrectionsMap.values()) {
			if (!tagCorrectionList.isEmpty()) {
				hasCorrections = true;
				break;
			}
		}

		if (hasCorrections) {
			Collection<ChangePropertyCommand> changePropertyCommands = new ArrayList<ChangePropertyCommand>();
			Map<OsmPrimitive, TagCorrectionTable> tableMap = new HashMap<OsmPrimitive, TagCorrectionTable>();
			NameVisitor nameVisitor = new NameVisitor();

			final JPanel p = new JPanel(new GridBagLayout());

			final JMultilineLabel label1 = new JMultilineLabel(description);
			label1.setMaxWidth(400);
			p.add(label1, GBC.eop());

			final JMultilineLabel label2 = new JMultilineLabel(
			        tr("Please select which property changes you want to apply."));
			label2.setMaxWidth(400);
			p.add(label2, GBC.eop());

			for (OsmPrimitive primitive : tagCorrectionsMap.keySet()) {
				final List<TagCorrection> tagCorrections = tagCorrectionsMap
				        .get(primitive);

				if (tagCorrections.isEmpty())
					continue;

				final TagCorrectionTable table = TagCorrectionTable
				        .create(tagCorrections);
				final JScrollPane scrollPane = new JScrollPane(table);
				tableMap.put(primitive, table);

				primitive.visit(nameVisitor);

				final JLabel label3 = new JLabel(nameVisitor.name + ":",
				        nameVisitor.icon, JLabel.LEFT);

				p.add(label3, GBC.eol());
				p.add(scrollPane, GBC.eop());
			}

			int answer = JOptionPane.showConfirmDialog(Main.parent, p,
			        tr("Automatic tag correction"),
			        JOptionPane.OK_CANCEL_OPTION);

			if (answer == JOptionPane.OK_OPTION) {
				for (OsmPrimitive primitive : tagCorrectionsMap.keySet()) {
					List<TagCorrection> tagCorrections = tagCorrectionsMap
					        .get(primitive);
					for (int i = 0; i < tagCorrections.size(); i++) {
						if (tableMap.get(primitive)
						        .getTagCorrectionTableModel().getApply(i)) {
							TagCorrection tagCorrection = tagCorrections.get(i);
							if (tagCorrection.isKeyChanged())
								changePropertyCommands
								        .add(new ChangePropertyCommand(
								                primitive,
								                tagCorrection.oldKey, null));
							changePropertyCommands
							        .add(new ChangePropertyCommand(primitive,
							                tagCorrection.newKey,
							                tagCorrection.newValue));
						}
					}
				}
			}
			return changePropertyCommands;
		}

		return Collections.emptyList();
	}
}
