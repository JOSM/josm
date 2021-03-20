// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangeRelationMemberRoleCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.correction.RoleCorrection;
import org.openstreetmap.josm.data.correction.TagCorrection;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.correction.RoleCorrectionTable;
import org.openstreetmap.josm.gui.correction.TagCorrectionTable;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Abstract base class for automatic tag corrections.
 *
 * Subclasses call applyCorrections() with maps of the requested
 * corrections and a dialog is presented to the user to
 * confirm these changes.
 * @param <P> The type of OSM primitive to correct
 */
public abstract class TagCorrector<P extends OsmPrimitive> {

    /**
     * Executes the tag correction.
     * @param oldprimitive old primitive
     * @param primitive new primitive
     * @return A list of commands
     * @throws UserCancelException If the user canceled
     * @see #applyCorrections(DataSet, Map, Map, String)
     */
    public abstract Collection<Command> execute(P oldprimitive, P primitive) throws UserCancelException;

    private static final String[] APPLICATION_OPTIONS = {
            tr("Apply selected changes"),
            tr("Do not apply changes"),
            tr("Cancel")
    };

    /**
     * Creates the commands to correct the tags. Asks the users about it.
     * @param dataSet The data set the primitives will be in once the commands are executed
     * @param tagCorrectionsMap The possible tag corrections
     * @param roleCorrectionMap The possible role corrections
     * @param description A description to add to the dialog.
     * @return A list of commands
     * @throws UserCancelException If the user canceled
     */
    protected Collection<Command> applyCorrections(
            DataSet dataSet,
            Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap,
            Map<OsmPrimitive, List<RoleCorrection>> roleCorrectionMap,
            String description) throws UserCancelException {

        if (!tagCorrectionsMap.isEmpty() || !roleCorrectionMap.isEmpty()) {
            Collection<Command> commands = new ArrayList<>();
            Map<OsmPrimitive, TagCorrectionTable> tagTableMap = new HashMap<>();
            Map<OsmPrimitive, RoleCorrectionTable> roleTableMap = new HashMap<>();

            final JPanel p = new JPanel(new GridBagLayout());

            final JMultilineLabel label1 = new JMultilineLabel(description);
            label1.setMaxWidth(600);
            p.add(label1, GBC.eop().anchor(GBC.CENTER).fill(GBC.HORIZONTAL));

            final JMultilineLabel label2 = new JMultilineLabel(
                    tr("Please select which changes you want to apply."));
            label2.setMaxWidth(600);
            p.add(label2, GBC.eop().anchor(GBC.CENTER).fill(GBC.HORIZONTAL));

            for (Entry<OsmPrimitive, List<TagCorrection>> entry : tagCorrectionsMap.entrySet()) {
                final OsmPrimitive primitive = entry.getKey();
                final List<TagCorrection> tagCorrections = entry.getValue();

                if (tagCorrections.isEmpty()) {
                    continue;
                }

                final JLabel propertiesLabel = new JLabel(tr("Tags of "));
                p.add(propertiesLabel, GBC.std());

                final JLabel primitiveLabel = new JLabel(
                        primitive.getDisplayName(DefaultNameFormatter.getInstance()) + ':',
                        ImageProvider.get(primitive.getDisplayType()),
                        JLabel.LEFT
                );
                p.add(primitiveLabel, GBC.eol());

                final TagCorrectionTable table = new TagCorrectionTable(
                        tagCorrections);
                final JScrollPane scrollPane = new JScrollPane(table);
                p.add(scrollPane, GBC.eop().fill(GBC.BOTH));

                tagTableMap.put(primitive, table);
            }

            for (Entry<OsmPrimitive, List<RoleCorrection>> entry : roleCorrectionMap.entrySet()) {
                final OsmPrimitive primitive = entry.getKey();
                final List<RoleCorrection> roleCorrections = entry.getValue();

                if (roleCorrections.isEmpty()) {
                    continue;
                }

                final JLabel rolesLabel = new JLabel(tr("Roles in relations referring to"));
                p.add(rolesLabel, GBC.std());

                final JLabel primitiveLabel = new JLabel(
                        primitive.getDisplayName(DefaultNameFormatter.getInstance()),
                        ImageProvider.get(primitive.getDisplayType()),
                        JLabel.LEFT
                );
                p.add(primitiveLabel, GBC.eol());
                rolesLabel.setLabelFor(primitiveLabel);

                final RoleCorrectionTable table = new RoleCorrectionTable(roleCorrections);
                final JScrollPane scrollPane = new JScrollPane(table);
                p.add(scrollPane, GBC.eop().fill(GBC.BOTH));
                primitiveLabel.setLabelFor(table);

                roleTableMap.put(primitive, table);
            }

            ExtendedDialog dialog = new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    tr("Automatic tag correction"),
                    APPLICATION_OPTIONS
            );
            dialog.setContent(p, false);
            dialog.setButtonIcons("dialogs/edit", "dialogs/next", "cancel");
            dialog.showDialog();
            int answer = dialog.getValue();

            if (answer == JOptionPane.YES_OPTION) {
                for (Entry<OsmPrimitive, List<TagCorrection>> entry : tagCorrectionsMap.entrySet()) {
                    OsmPrimitive primitive = entry.getKey();

                    // use this structure to remember keys that have been set already so that
                    // they're not dropped by a later step
                    Set<String> keysChanged = new HashSet<>();

                    // the map for the change command
                    Map<String, String> tagMap = new HashMap<>();

                    List<TagCorrection> tagCorrections = entry.getValue();
                    for (int i = 0; i < tagCorrections.size(); i++) {
                        if (tagTableMap.get(primitive).getCorrectionTableModel().getApply(i)) {
                            TagCorrection tagCorrection = tagCorrections.get(i);
                            if (tagCorrection.isKeyChanged() && !keysChanged.contains(tagCorrection.oldKey)) {
                                tagMap.put(tagCorrection.oldKey, null);
                            }
                            tagMap.put(tagCorrection.newKey, tagCorrection.newValue);
                            keysChanged.add(tagCorrection.newKey);
                        }
                    }

                    if (!keysChanged.isEmpty()) {
                        // change the properties of this object
                        commands.add(new ChangePropertyCommand(dataSet, Collections.singleton(primitive),
                                Utils.toUnmodifiableMap(tagMap)));
                    }
                }
                for (Entry<OsmPrimitive, List<RoleCorrection>> entry : roleCorrectionMap.entrySet()) {
                    OsmPrimitive primitive = entry.getKey();
                    List<RoleCorrection> roleCorrections = entry.getValue();

                    for (int i = 0; i < roleCorrections.size(); i++) {
                        RoleCorrection roleCorrection = roleCorrections.get(i);
                        if (roleTableMap.get(primitive).getCorrectionTableModel().getApply(i)) {
                            commands.add(new ChangeRelationMemberRoleCommand(dataSet,
                                    roleCorrection.relation, roleCorrection.position, roleCorrection.newRole));
                        }
                    }
                }
            } else if (answer != JOptionPane.NO_OPTION)
                throw new UserCancelException();
            return Collections.unmodifiableCollection(commands);
        }

        return Collections.emptyList();
    }
}
