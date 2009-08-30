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
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public abstract class TagCorrector<P extends OsmPrimitive> {

    public abstract Collection<Command> execute(P primitive, P oldprimitive)
    throws UserCancelException;

    private String[] applicationOptions = new String[] {
            tr("Apply selected changes"),
            tr("Don't apply changes"),
            tr("Cancel")
    };

    protected Collection<Command> applyCorrections(
            Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap,
            Map<OsmPrimitive, List<RoleCorrection>> roleCorrectionMap,
            String description) throws UserCancelException {

        boolean hasCorrections = false;
        for (List<TagCorrection> tagCorrectionList : tagCorrectionsMap.values()) {
            if (!tagCorrectionList.isEmpty()) {
                hasCorrections = true;
                break;
            }
        }

        if (!hasCorrections) {
            for (List<RoleCorrection> roleCorrectionList : roleCorrectionMap
                    .values()) {
                if (!roleCorrectionList.isEmpty()) {
                    hasCorrections = true;
                    break;
                }
            }
        }

        if (hasCorrections) {
            Collection<Command> commands = new ArrayList<Command>();
            Map<OsmPrimitive, TagCorrectionTable> tagTableMap =
                new HashMap<OsmPrimitive, TagCorrectionTable>();
            Map<OsmPrimitive, RoleCorrectionTable> roleTableMap =
                new HashMap<OsmPrimitive, RoleCorrectionTable>();

            //NameVisitor nameVisitor = new NameVisitor();

            final JPanel p = new JPanel(new GridBagLayout());

            final JMultilineLabel label1 = new JMultilineLabel(description);
            label1.setMaxWidth(600);
            p.add(label1, GBC.eop().anchor(GBC.CENTER));

            final JMultilineLabel label2 = new JMultilineLabel(
                    tr("Please select which property changes you want to apply."));
            label2.setMaxWidth(600);
            p.add(label2, GBC.eop().anchor(GBC.CENTER));

            for (OsmPrimitive primitive : tagCorrectionsMap.keySet()) {
                final List<TagCorrection> tagCorrections = tagCorrectionsMap
                .get(primitive);

                if (tagCorrections.isEmpty()) {
                    continue;
                }

                final JLabel propertiesLabel = new JLabel(tr("Properties of "));
                p.add(propertiesLabel, GBC.std());

                final JLabel primitiveLabel = new JLabel(
                        primitive.getDisplayName(DefaultNameFormatter.getInstance()) + ":",
                        ImageProvider.get(OsmPrimitiveType.from(primitive)),
                        JLabel.LEFT
                );
                p.add(primitiveLabel, GBC.eol());

                final TagCorrectionTable table = new TagCorrectionTable(
                        tagCorrections);
                final JScrollPane scrollPane = new JScrollPane(table);
                p.add(scrollPane, GBC.eop().fill(GBC.HORIZONTAL));

                tagTableMap.put(primitive, table);
            }

            for (OsmPrimitive primitive : roleCorrectionMap.keySet()) {
                final List<RoleCorrection> roleCorrections = roleCorrectionMap
                .get(primitive);
                if (roleCorrections.isEmpty()) {
                    continue;
                }

                final JLabel rolesLabel = new JLabel(
                        tr("Roles in relations referring to"));
                p.add(rolesLabel, GBC.std());

                final JLabel primitiveLabel = new JLabel(
                        primitive.getDisplayName(DefaultNameFormatter.getInstance()),
                        ImageProvider.get(OsmPrimitiveType.from(primitive)),
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
                for (OsmPrimitive primitive : tagCorrectionsMap.keySet()) {
                    List<TagCorrection> tagCorrections =
                        tagCorrectionsMap.get(primitive);

                    // create the clone
                    OsmPrimitive clone = null;
                    if (primitive instanceof Way) {
                        clone = new Way((Way)primitive);
                    } else if (primitive instanceof Node) {
                        clone = new Node((Node)primitive);
                    } else if (primitive instanceof Relation) {
                        clone = new Relation((Relation)primitive);
                    }

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
                for (OsmPrimitive primitive : roleCorrectionMap.keySet()) {
                    List<RoleCorrection> roleCorrections = roleCorrectionMap
                    .get(primitive);

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
