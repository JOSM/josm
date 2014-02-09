// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Synchronizes scrollbar adjustments between a set of {@link Adjustable}s.
 * Whenever the adjustment of one of the registered Adjustables is updated
 * the adjustment of the other registered Adjustables is adjusted too.
 * @since 6147
 */
public class AdjustmentSynchronizer implements AdjustmentListener {

    private final List<Adjustable> synchronizedAdjustables;
    private final Map<Adjustable, Boolean> enabledMap;

    private final Observable observable;

    /**
     * Constructs a new {@code AdjustmentSynchronizer}
     */
    public AdjustmentSynchronizer() {
        synchronizedAdjustables = new ArrayList<Adjustable>();
        enabledMap = new HashMap<Adjustable, Boolean>();
        observable = new Observable();
    }

    /**
     * Registers an {@link Adjustable} for participation in synchronized scrolling.
     *
     * @param adjustable the adjustable
     */
    public void participateInSynchronizedScrolling(Adjustable adjustable) {
        if (adjustable == null)
            return;
        if (synchronizedAdjustables.contains(adjustable))
            return;
        synchronizedAdjustables.add(adjustable);
        setParticipatingInSynchronizedScrolling(adjustable, true);
        adjustable.addAdjustmentListener(this);
    }

    /**
     * Event handler for {@link AdjustmentEvent}s
     */
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (! enabledMap.get(e.getAdjustable()))
            return;
        for (Adjustable a : synchronizedAdjustables) {
            if (a != e.getAdjustable() && isParticipatingInSynchronizedScrolling(a)) {
                a.setValue(e.getValue());
            }
        }
    }

    /**
     * Sets whether adjustable participates in adjustment synchronization or not
     *
     * @param adjustable the adjustable
     */
    protected void setParticipatingInSynchronizedScrolling(Adjustable adjustable, boolean isParticipating) {
        CheckParameterUtil.ensureParameterNotNull(adjustable, "adjustable");
        if (! synchronizedAdjustables.contains(adjustable))
            throw new IllegalStateException(tr("Adjustable {0} not registered yet. Cannot set participation in synchronized adjustment.", adjustable));

        enabledMap.put(adjustable, isParticipating);
        observable.notifyObservers();
    }

    /**
     * Returns true if an adjustable is participating in synchronized scrolling
     *
     * @param adjustable the adjustable
     * @return true, if the adjustable is participating in synchronized scrolling, false otherwise
     * @throws IllegalStateException thrown, if adjustable is not registered for synchronized scrolling
     */
    protected boolean isParticipatingInSynchronizedScrolling(Adjustable adjustable) throws IllegalStateException {
        if (! synchronizedAdjustables.contains(adjustable))
            throw new IllegalStateException(tr("Adjustable {0} not registered yet.", adjustable));

        return enabledMap.get(adjustable);
    }

    /**
     * Wires a {@link JCheckBox} to  the adjustment synchronizer, in such a way that:
     * <ol>
     *   <li>state changes in the checkbox control whether the adjustable participates
     *      in synchronized adjustment</li>
     *   <li>state changes in this {@link AdjustmentSynchronizer} are reflected in the
     *      {@link JCheckBox}</li>
     * </ol>
     *
     * @param view  the checkbox to control whether an adjustable participates in synchronized adjustment
     * @param adjustable the adjustable
     * @exception IllegalArgumentException thrown, if view is null
     * @exception IllegalArgumentException thrown, if adjustable is null
     */
    public void adapt(final JCheckBox view, final Adjustable adjustable)  {
        CheckParameterUtil.ensureParameterNotNull(adjustable, "adjustable");
        CheckParameterUtil.ensureParameterNotNull(view, "view");

        if (! synchronizedAdjustables.contains(adjustable)) {
            participateInSynchronizedScrolling(adjustable);
        }

        // register an item lister with the check box
        //
        view.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                switch(e.getStateChange()) {
                case ItemEvent.SELECTED:
                    if (!isParticipatingInSynchronizedScrolling(adjustable)) {
                        setParticipatingInSynchronizedScrolling(adjustable, true);
                    }
                    break;
                case ItemEvent.DESELECTED:
                    if (isParticipatingInSynchronizedScrolling(adjustable)) {
                        setParticipatingInSynchronizedScrolling(adjustable, false);
                    }
                    break;
                }
            }
        });

        observable.addObserver(
                new Observer() {
                    @Override
                    public void update(Observable o, Object arg) {
                        boolean sync = isParticipatingInSynchronizedScrolling(adjustable);
                        if (view.isSelected() != sync) {
                            view.setSelected(sync);
                        }
                    }
                }
        );
        setParticipatingInSynchronizedScrolling(adjustable, true);
        view.setSelected(true);
    }
}
