// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Collection;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * A dialog allowing the user decide whether the tags/memberships of the existing node should afterwards be at
 * the existing node, the new nodes, or all of them.
 * @since 14320 (extracted from UnglueAction)
 */
public final class PropertiesMembershipChoiceDialog extends ExtendedDialog {

    private final transient ExistingBothNewChoice tags;
    private final transient ExistingBothNewChoice memberships;

    /**
     * Provides toggle buttons to allow the user choose the existing node, the new nodes, or all of them.
     */
    public static class ExistingBothNewChoice {
        /** The "Existing node" button */
        public final AbstractButton oldNode = new JToggleButton(tr("Existing node"), ImageProvider.get("dialogs/conflict/tagkeeptheir"));
        /** The "Both nodes" button */
        public final AbstractButton bothNodes = new JToggleButton(tr("Both nodes"), ImageProvider.get("dialogs/conflict/tagundecide"));
        /** The "New node" button */
        public final AbstractButton newNode = new JToggleButton(tr("New node"), ImageProvider.get("dialogs/conflict/tagkeepmine"));

        ExistingBothNewChoice(final boolean preselectNew) {
            final ButtonGroup tagsGroup = new ButtonGroup();
            tagsGroup.add(oldNode);
            tagsGroup.add(bothNodes);
            tagsGroup.add(newNode);
            tagsGroup.setSelected((preselectNew ? newNode : oldNode).getModel(), true);
        }
    }

    private PropertiesMembershipChoiceDialog(boolean preselectNew, boolean queryTags, boolean queryMemberships) {
        super(MainApplication.getMainFrame(), tr("Tags/Memberships"), tr("Unglue"), tr("Cancel"));
        setButtonIcons("unglueways", "cancel");

        final JPanel content = new JPanel(new GridBagLayout());

        if (queryTags) {
            content.add(new JLabel(tr("Where should the tags of the node be put?")), GBC.std(1, 1).span(3).insets(0, 20, 0, 0));
            tags = new ExistingBothNewChoice(preselectNew);
            content.add(tags.oldNode, GBC.std(1, 2));
            content.add(tags.bothNodes, GBC.std(2, 2));
            content.add(tags.newNode, GBC.std(3, 2));
        } else {
            tags = null;
        }

        if (queryMemberships) {
            content.add(new JLabel(tr("Where should the memberships of this node be put?")), GBC.std(1, 3).span(3).insets(0, 20, 0, 0));
            memberships = new ExistingBothNewChoice(preselectNew);
            content.add(memberships.oldNode, GBC.std(1, 4));
            content.add(memberships.bothNodes, GBC.std(2, 4));
            content.add(memberships.newNode, GBC.std(3, 4));
        } else {
            memberships = null;
        }

        setContent(content);
        setResizable(false);
    }

    /**
     * Returns the tags choice.
     * @return the tags choice (can be null)
     */
    public ExistingBothNewChoice getTags() {
        return tags;
    }

    /**
     * Returns the memberships choice.
     * @return the memberships choice (can be null)
     */
    public ExistingBothNewChoice getMemberships() {
        return memberships;
    }

    /**
     * Creates and shows a new {@code PropertiesMembershipChoiceDialog} if necessary. Otherwise does nothing.
     * @param selectedNodes selected nodes
     * @param preselectNew if {@code true}, pre-select "new node" as default choice
     * @return A new {@code PropertiesMembershipChoiceDialog} that has been shown to user, or {@code null}
     * @throws UserCancelException if user cancels choice
     */
    public static PropertiesMembershipChoiceDialog showIfNecessary(Collection<Node> selectedNodes, boolean preselectNew)
            throws UserCancelException {
        final boolean queryTags = isTagged(selectedNodes);
        final boolean queryMemberships = isUsedInRelations(selectedNodes);
        if (queryTags || queryMemberships) {
            final PropertiesMembershipChoiceDialog dialog = new PropertiesMembershipChoiceDialog(preselectNew, queryTags, queryMemberships);
            dialog.showDialog();
            if (dialog.getValue() != 1) {
                throw new UserCancelException();
            }
            return dialog;
        }
        return null;
    }

    private static boolean isTagged(final Collection<Node> existingNodes) {
        return existingNodes.stream().anyMatch(Node::hasKeys);
    }

    private static boolean isUsedInRelations(final Collection<Node> existingNodes) {
        return existingNodes.stream().anyMatch(
                selectedNode -> selectedNode.getReferrers().stream().anyMatch(Relation.class::isInstance));
    }
}
