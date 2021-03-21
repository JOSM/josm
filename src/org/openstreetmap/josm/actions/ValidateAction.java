// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.ValidationTask;
import org.openstreetmap.josm.data.validation.util.AggregatePrimitivesVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The action that does the validate thing.
 * <p>
 * This action iterates through all active tests and give them the data, so that
 * each one can test it.
 *
 * @author frsantos
 */
public class ValidateAction extends JosmAction {

    /** Last selection used to validate */
    private transient Collection<OsmPrimitive> lastSelection;

    /**
     * Constructor
     */
    public ValidateAction() {
        super(tr("Validation"), "dialogs/validator", tr("Performs the data validation"),
                Shortcut.registerShortcut("tools:validate", tr("Validation"),
                        KeyEvent.VK_V, Shortcut.SHIFT), true);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        doValidate(true);
    }

    /**
     * Does the validation.
     * <p>
     * If getSelectedItems is true, the selected items (or all items, if no one
     * is selected) are validated. If it is false, last selected items are revalidated
     *
     * @param getSelectedItems If selected or last selected items must be validated
     */
    public void doValidate(boolean getSelectedItems) {
        MapFrame map = MainApplication.getMap();
        if (map == null || !map.isVisible())
            return;

        OsmValidator.initializeTests();

        Collection<Test> tests = OsmValidator.getEnabledTests(false);
        if (tests.isEmpty())
            return;

        Collection<OsmPrimitive> selection;
        if (getSelectedItems) {
            selection = getLayerManager().getActiveDataSet().getAllSelected();
            if (selection.isEmpty()) {
                selection = getLayerManager().getActiveDataSet().allNonDeletedPrimitives();
                lastSelection = null;
            } else {
                AggregatePrimitivesVisitor v = new AggregatePrimitivesVisitor();
                selection = v.visit(selection);
                lastSelection = selection;
            }
        } else {
            selection = Optional.ofNullable(lastSelection).orElseGet(
                    () -> getLayerManager().getActiveDataSet().allNonDeletedPrimitives());
        }

        MainApplication.worker.submit(new ValidationTask(tests, selection, lastSelection));
    }

    @Override
    public void updateEnabledState() {
        setEnabled(getLayerManager().getActiveDataSet() != null);
    }

    @Override
    public void destroy() {
        // Hack - this action should stay forever because it could be added to toolbar
        // Do not call super.destroy() here
        lastSelection = null;
    }

}
