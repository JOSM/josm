package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JToolBar;

import org.openstreetmap.josm.actions.ExpertToggleAction;

/**
 * An action group for the relation editor, to be used in one of the tool bars.
 * 
 * @author Michael Zangl
 * @since 14027
 */
public interface IRelationEditorActionGroup {

	/**
	 * Get the position at which the action group should be added.
	 * 
	 * @return The order index, default is to add at the end.
	 */
	default int order() {
		return 100;
	}

	/**
	 * Get the actions in this action group.
	 * 
	 * @param editorAccess
	 *            Methods to access the relation editor.
	 * @return The actions
	 */
	List<AbstractRelationEditorAction> getActions(IRelationEditorActionAccess editorAccess);

	static void fillToolbar(JToolBar toolbar, List<IRelationEditorActionGroup> groups,
			IRelationEditorActionAccess editorAccess) {
		groups.stream().sorted(Comparator.comparingInt(IRelationEditorActionGroup::order)).forEach(group -> {
			if (toolbar.getComponentCount() > 0) {
				toolbar.addSeparator();
			}

			for (AbstractRelationEditorAction action : group.getActions(editorAccess)) {
				JButton button = toolbar.add(action);
				if (action.isExpertOnly()) {
					ExpertToggleAction.addVisibilitySwitcher(button);
				}
			}
		});
	}
}
