// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangeRelationMemberRoleCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Abstract base class for automatic tag corrections.
 *
 * Subclasses call applyCorrections() with maps of the requested
 * corrections and a dialog is pesented to the user to
 * confirm these changes.
 */

public abstract class TagCorrector<P extends OsmPrimitive> {

    public abstract Collection<Command> execute(P primitive, P oldprimitive)
    throws UserCancelException;

    private String[] applicationOptions = new String[] {
            tr("Apply selected changes"),
            tr("Do not apply changes"),
            tr("Cancel")
    };

    protected Collection<Command> applyCorrections(
            Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap,
            Map<OsmPrimitive, List<RoleCorrection>> roleCorrectionMap,
            String description) throws UserCancelException {

        if (!tagCorrectionsMap.isEmpty() || !roleCorrectionMap.isEmpty()) {
            Collection<Command> commands = new ArrayList<Command>();
            Map<OsmPrimitive, TagCorrectionTable> tagTableMap =
                new HashMap<OsmPrimitive, TagCorrectionTable>();
            Map<OsmPrimitive, RoleCorrectionTable> roleTableMap =
                new HashMap<OsmPrimitive, RoleCorrectionTable>();

            final JPanel p = new JPanel(new GridBagLayout());

            final JMultilineLabel label1 = new JMultilineLabel(description);
            label1.setMaxWidth(600);
            p.add(label1, GBC.eop().anchor(GBC.CENTER));

            final JMultilineLabel label2 = new JMultilineLabel(
                    tr("Please select which changes you want to apply."));
            label2.setMaxWidth(600);
            p.add(label2, GBC.eop().anchor(GBC.CENTER));

            for (Entry<OsmPrimitive, List<TagCorrection>> entry : tagCorrectionsMap.entrySet()) {
                final OsmPrimitive primitive = entry.getKey();
                final List<TagCorrection> tagCorrections = entry.getValue();

                if (tagCorrections.isEmpty()) {
                    continue;
                }

                final JLabel propertiesLabel = new JLabel(tr("Tags of "));
                p.add(propertiesLabel, GBC.std());

                final JLabel primitiveLabel = new JLabel(
                        primitive.getDisplayName(DefaultNameFormatter.getInstance()) + ":",
                        ImageProvider.get(primitive.getDisplayType()),
                        JLabel.LEFT
                );
                p.add(primitiveLabel, GBC.eol());

                final TagCorrectionTable table = new TagCorrectionTable(
                        tagCorrections);
                final JScrollPane scrollPane = new JScrollPane(table);
                p.add(scrollPane, GBC.eop().fill(GBC.HORIZONTAL));

                tagTableMap.put(primitive, table);
            }

            for (Entry<OsmPrimitive, List<RoleCorrection>> entry : roleCorrectionMap.entrySet()) {
                final OsmPrimitive primitive = entry.getKey();
                final List<RoleCorrection> roleCorrections = entry.getValue();

                if (roleCorrections.isEmpty()) {
                    continue;
                }

                final JLabel rolesLabel = new JLabel(
                        tr("Roles in relations referring to"));
                p.add(rolesLabel, GBC.std());

                final JLabel primitiveLabel = new JLabel(
                        primitive.getDisplayName(DefaultNameFormatter.getInstance()),
                        ImageProvider.get(primitive.getDisplayType()),
                        JLabel.LEFT
                );
                p.add(primitiveLabel, GBC.eol());

                final RoleCorrectionTable table = new RoleCorrectionTable(
                        roleCorrections);
                final JScrollPane scrollPane = new JScrollPane(table);
                p.add(scrollPane, GBC.eop().fill(GBC.HORIZONTAL));

                roleTableMap.put(primitive, table);
            }

            int answer = JOptionPane.showOptionDialog(
                    Main.parent,
                    p,
                    tr("Automatic tag correction"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    applicationOptions,
                    applicationOptions[0]
            );

            if (answer == JOptionPane.YES_OPTION) {
                for (Entry<OsmPrimitive, List<TagCorrection>> entry : tagCorrectionsMap.entrySet()) {
                    List<TagCorrection> tagCorrections = entry.getValue();
                    OsmPrimitive primitive = entry.getKey();

                    // create the clone
                    OsmPrimitive clone = null;
                    if (primitive instanceof Way) {
                        clone = new Way((Way)primitive);
                    } else if (primitive instanceof Node) {
                        clone = new Node((Node)primitive);
                    } else if (primitive instanceof Relation) {
                        clone = new Relation((Relation)primitive);
                    } else
                        throw new AssertionError();

                    // use this structure to remember keys that have been set already so that
                    // they're not dropped by a later step
                    Set<String> keysChanged = new HashSet<String>();

                    // apply all changes to this clone
                    for (int i = 0; i < tagCorrections.size(); i++) {
                        if (tagTableMap.get(primitive).getCorrectionTableModel().getApply(i)) {
                            TagCorrection tagCorrection = tagCorrections.get(i);
                            if (tagCorrection.isKeyChanged() && !keysChanged.contains(tagCorrection.oldKey)) {
                                clone.remove(tagCorrection.oldKey);
                            }
                            clone.put(tagCorrection.newKey, tagCorrection.newValue);
                            keysChanged.add(tagCorrection.newKey);
                        }
                    }

                    // save the clone
                    if (!keysChanged.isEmpty()) {
                        commands.add(new ChangeCommand(primitive, clone));
                    }
                }
                for (Entry<OsmPrimitive, List<RoleCorrection>> entry : roleCorrectionMap.entrySet()) {
                    OsmPrimitive primitive = entry.getKey();
                    List<RoleCorrection> roleCorrections = entry.getValue();

                    for (int i = 0; i < roleCorrections.size(); i++) {
                        RoleCorrection roleCorrection = roleCorrections.get(i);
                        if (roleTableMap.get(primitive).getCorrectionTableModel().getApply(i)) {
                            commands.add(new ChangeRelationMemberRoleCommand(roleCorrection.relation, roleCorrection.position, roleCorrection.newRole));
                        }
                    }
                }
            } else if (answer != JOptionPane.NO_OPTION)
                throw new UserCancelException();
            return commands;
        }

        return Collections.emptyList();
    }
}
