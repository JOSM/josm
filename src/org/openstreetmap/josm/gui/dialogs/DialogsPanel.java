// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openstreetmap.josm.gui.widgets.MultiSplitPane;
import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Divider;
import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Leaf;
import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Node;
import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Split;
import org.openstreetmap.josm.tools.Destroyable;

public class DialogsPanel extends JPanel implements Destroyable {
    protected List<ToggleDialog> allDialogs = new ArrayList<ToggleDialog>();
    protected MultiSplitPane mSpltPane = new MultiSplitPane();
    protected static final int DIVIDER_SIZE = 5;

    /**
     * Panels that are added to the multisplitpane.
     */
    private List<JPanel> panels = new ArrayList<JPanel>();

    final private JSplitPane parent;
    public DialogsPanel(JSplitPane parent) {
        this.parent = parent;
    }

    public boolean initialized = false; // read only from outside

    public void initialize(List<ToggleDialog> pAllDialogs) {
        if (initialized)
            throw new IllegalStateException();
        initialized = true;
        allDialogs = new ArrayList<ToggleDialog>();

        for (ToggleDialog dialog: pAllDialogs) {
            add(dialog, false);
        }

        this.add(mSpltPane);
        reconstruct(Action.ELEMENT_SHRINKS, null);
    }

    public void add(ToggleDialog dlg) {
        add(dlg, true);
    }

    public void add(ToggleDialog dlg, boolean doReconstruct) {
        allDialogs.add(dlg);
        int i = allDialogs.size() - 1;
        dlg.setDialogsPanel(this);
        dlg.setVisible(false);
        final JPanel p = new JPanel() {
            /**
             * Honoured by the MultiSplitPaneLayout when the
             * entire Window is resized.
             */
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 40);
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setVisible(false);

        mSpltPane.add(p, "L"+i);
        panels.add(p);

        if (dlg.isDialogShowing()) {
            dlg.showDialog();
            if (dlg.isDialogInCollapsedView()) {
                dlg.isCollapsed = false;    // pretend to be in Default view, this will be set back by collapse()
                dlg.collapse();
            }
            if (doReconstruct) {
                reconstruct(Action.INVISIBLE_TO_DEFAULT, dlg);
            }
            dlg.showNotify();
        } else {
            dlg.hideDialog();
        }
    }

    /**
     * What action was performed to trigger the reconstruction
     */
    public enum Action {
        INVISIBLE_TO_DEFAULT,
        COLLAPSED_TO_DEFAULT,
        /*  INVISIBLE_TO_COLLAPSED,    does not happen */
        ELEMENT_SHRINKS         /* else. (Remaining elements have more space.) */
    }
    /**
     * Reconstruct the view, if the configurations of dialogs has changed.
     * @param action what happened, so the reconstruction is necessary
     * @param triggeredBy the dialog that caused the reconstruction
     */
    public void reconstruct(Action action, ToggleDialog triggeredBy) {

        final int N = allDialogs.size();

        /**
         * reset the panels
         */
        for (JPanel p: panels) {
            p.removeAll();
            p.setVisible(false);
        }

        /**
         * Add the elements to their respective panel.
         *
         * Each panel contains one dialog in default view and zero or more
         * collapsed dialogs on top of it. The last panel is an exception
         * as it can have collapsed dialogs at the bottom as well.
         * If there are no dialogs in default view, show the collapsed ones
         * in the last panel anyway.
         */
        JPanel p = panels.get(N-1); // current Panel (start with last one)
        int k = -1;                 // indicates that the current Panel index is N-1, but no default-view-Dialog has been added to this Panel yet.
        for (int i=N-1; i >= 0 ; --i) {
            final ToggleDialog dlg = allDialogs.get(i);
            if (dlg.isDialogInDefaultView()) {
                if (k == -1) {
                    k = N-1;
                } else {
                    --k;
                    p = panels.get(k);
                }
                p.add(dlg, 0);
                p.setVisible(true);
            }
            else if (dlg.isDialogInCollapsedView()) {
                p.add(dlg, 0);
                p.setVisible(true);
            }
        }

        if (k == -1) {
            k = N-1;
        }
        final int numPanels = N - k;

        /**
         * Determine the panel geometry
         */
        if (action == Action.ELEMENT_SHRINKS) {
            for (int i=0; i<N; ++i) {
                final ToggleDialog dlg = allDialogs.get(i);
                if (dlg.isDialogInDefaultView()) {
                    final int ph = dlg.getPreferredHeight();
                    final int ah = dlg.getSize().height;
                    dlg.setPreferredSize(new Dimension(Integer.MAX_VALUE, (ah < 20 ? ph : ah)));
                }
            }
        } else {
            if (triggeredBy == null)
                throw new IllegalArgumentException();

            int sumP = 0;   // sum of preferred heights of dialogs in default view (without the triggering dialog)
            int sumA = 0;   // sum of actual heights of dialogs in default view (without the triggering dialog)
            int sumC = 0;   // sum of heights of all collapsed dialogs (triggering dialog is never collapsed)

            for (ToggleDialog dlg: allDialogs) {
                if (dlg.isDialogInDefaultView()) {
                    if (dlg != triggeredBy) {
                        sumP += dlg.getPreferredHeight();
                        sumA += dlg.getHeight();
                    }
                } else if (dlg.isDialogInCollapsedView()) {
                    sumC += dlg.getHeight();
                }
            }

            /**
             * If we add additional dialogs on startup (e.g. geoimage), they may
             * not have an actual height yet.
             * In this case we simply reset everything to it's preferred size.
             */
            if (sumA == 0) {
                reconstruct(Action.ELEMENT_SHRINKS, null);
                return;
            }

            /** total Height */
            final int H = mSpltPane.getMultiSplitLayout().getModel().getBounds().getSize().height;

            /** space, that is available for dialogs in default view (after the reconfiguration) */
            final int s2 = H - (numPanels - 1) * DIVIDER_SIZE - sumC;

            final int hp_trig = triggeredBy.getPreferredHeight();
            if (hp_trig <= 0) throw new IllegalStateException(); // Must be positive

            /** The new dialog gets a fair share */
            final int hn_trig = hp_trig * s2 / (hp_trig + sumP);
            triggeredBy.setPreferredSize(new Dimension(Integer.MAX_VALUE, hn_trig));

            /** This is remainig for the other default view dialogs */
            final int R = s2 - hn_trig;

            /**
             * Take space only from dialogs that are relatively large
             */
            int D_m = 0;        // additional space needed by the small dialogs
            int D_p = 0;        // available space from the large dialogs
            for (int i=0; i<N; ++i) {
                final ToggleDialog dlg = allDialogs.get(i);
                if (dlg.isDialogInDefaultView() && dlg != triggeredBy) {
                    final int ha = dlg.getSize().height;                              // current
                    final int h0 = ha * R / sumA;                                     // proportional shrinking
                    final int he = dlg.getPreferredHeight() * s2 / (sumP + hp_trig);  // fair share
                    if (h0 < he) {                  // dialog is relatively small
                        int hn = Math.min(ha, he);  // shrink less, but do not grow
                        D_m += hn - h0;
                    } else {                        // dialog is relatively large
                        D_p += h0 - he;
                    }
                }
            }
            /** adjust, without changing the sum */
            for (int i=0; i<N; ++i) {
                final ToggleDialog dlg = allDialogs.get(i);
                if (dlg.isDialogInDefaultView() && dlg != triggeredBy) {
                    final int ha = dlg.getHeight();
                    final int h0 = ha * R / sumA;
                    final int he = dlg.getPreferredHeight() * s2 / (sumP + hp_trig);
                    if (h0 < he) {
                        int hn = Math.min(ha, he);
                        dlg.setPreferredSize(new Dimension(Integer.MAX_VALUE, hn));
                    } else {
                        int d;
                        try {
                            d = (h0-he) * D_m / D_p;
                        } catch (ArithmeticException e) { /* D_p may be zero - nothing wrong with that. */
                            d = 0;
                        }
                        dlg.setPreferredSize(new Dimension(Integer.MAX_VALUE, h0 - d));
                    }
                }
            }
        }

        /**
         * create Layout
         */
        final List<Node> ch = new ArrayList<Node>();

        for (int i = k; i <= N-1; ++i) {
            if (i != k) {
                ch.add(new Divider());
            }
            Leaf l = new Leaf("L"+i);
            l.setWeight(1.0 / numPanels);
            ch.add(l);
        }

        if (numPanels == 1) {
            Node model = ch.get(0);
            mSpltPane.getMultiSplitLayout().setModel(model);
        } else {
            Split model = new Split();
            model.setRowLayout(false);
            model.setChildren(ch);
            mSpltPane.getMultiSplitLayout().setModel(model);
        }

        mSpltPane.getMultiSplitLayout().setDividerSize(DIVIDER_SIZE);
        mSpltPane.getMultiSplitLayout().setFloatingDividers(true);
        mSpltPane.revalidate();

        /**
         * Hide the Panel, if there is nothing to show
         */
        if (numPanels == 1 && panels.get(N-1).getComponents().length == 0)
        {
            parent.setDividerSize(0);
            this.setVisible(false);
        } else {
            if (this.getWidth() != 0) { // only if josm started with hidden panel
                this.setPreferredSize(new Dimension(this.getWidth(), 0));
            }
            this.setVisible(true);
            parent.setDividerSize(5);
            parent.resetToPreferredSizes();
        }
    }

    @Override
    public void destroy() {
        for (ToggleDialog t : allDialogs) {
            t.destroy();
        }
    }

    /**
     * Replies the instance of a toggle dialog of type <code>type</code> managed by this
     * map frame
     *
     * @param <T>
     * @param type the class of the toggle dialog, i.e. UserListDialog.class
     * @return the instance of a toggle dialog of type <code>type</code> managed by this
     * map frame; null, if no such dialog exists
     *
     */
    public <T> T getToggleDialog(Class<T> type) {
        for (ToggleDialog td : allDialogs) {
            if (type.isInstance(td))
                return type.cast(td);
        }
        return null;
    }
}
