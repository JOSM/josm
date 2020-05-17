// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Divider;
import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Leaf;
import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Node;
import org.openstreetmap.josm.gui.widgets.MultiSplitLayout.Split;
import org.openstreetmap.josm.gui.widgets.MultiSplitPane;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This is the panel displayed on the right side of JOSM. It displays a list of panels.
 */
public class DialogsPanel extends JPanel implements Destroyable {
    private final List<ToggleDialog> allDialogs = new ArrayList<>();
    private final MultiSplitPane mSpltPane = new MultiSplitPane();
    private static final int DIVIDER_SIZE = 5;

    /**
     * Panels that are added to the multisplitpane.
     */
    private final List<JPanel> panels = new ArrayList<>();

    /**
     * If {@link #initialize(List)} was called. read only from outside
     */
    public boolean initialized;

    private final JSplitPane myParent;

    /**
     * Creates a new {@link DialogsPanel}.
     * @param parent The parent split pane that allows this panel to change it's size.
     */
    public DialogsPanel(JSplitPane parent) {
        this.myParent = parent;
    }

    /**
     * Initializes this panel
     * @param pAllDialogs The list of dialogs this panel should contain on start.
     */
    public void initialize(List<ToggleDialog> pAllDialogs) {
        if (initialized) {
            throw new IllegalStateException("Panel can only be initialized once.");
        }
        initialized = true;
        allDialogs.clear();

        for (ToggleDialog dialog: pAllDialogs) {
            add(dialog, false);
        }

        this.add(mSpltPane);
        reconstruct(Action.RESTORE_SAVED, null);
    }

    /**
     * Add a new {@link ToggleDialog} to the list of known dialogs and trigger reconstruct.
     * @param dlg The dialog to add
     */
    public void add(ToggleDialog dlg) {
        add(dlg, true);
    }

    /**
     * Add a new {@link ToggleDialog} to the list of known dialogs.
     * @param dlg The dialog to add
     * @param doReconstruct <code>true</code> if reconstruction should be triggered.
     */
    public void add(ToggleDialog dlg, boolean doReconstruct) {
        allDialogs.add(dlg);
        dlg.setDialogsPanel(this);
        dlg.setVisible(false);
        final JPanel p = new MinSizePanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setVisible(false);

        int dialogIndex = allDialogs.size() - 1;
        mSpltPane.add(p, 'L'+Integer.toString(dialogIndex));
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

    static final class MinSizePanel extends JPanel {
        @Override
        public Dimension getMinimumSize() {
            // Honoured by the MultiSplitPaneLayout when the entire Window is resized
            return new Dimension(0, 40);
        }
    }

    /**
     * What action was performed to trigger the reconstruction
     */
    public enum Action {
        /**
         * The panel was invisible previously
         */
        INVISIBLE_TO_DEFAULT,
        /**
         * The panel was collapsed by the user.
         */
        COLLAPSED_TO_DEFAULT,
        /**
         * Restore saved heights.
         * @since 14425
         */
        RESTORE_SAVED,
        /*  INVISIBLE_TO_COLLAPSED,    does not happen */
        /**
         * else. (Remaining elements have more space.)
         */
        ELEMENT_SHRINKS
    }

    /**
     * Reconstruct the view, if the configurations of dialogs has changed.
     * @param action what happened, so the reconstruction is necessary
     * @param triggeredBy the dialog that caused the reconstruction
     */
    public void reconstruct(Action action, ToggleDialog triggeredBy) {

        final int n = allDialogs.size();

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
        JPanel p = panels.get(n-1); // current Panel (start with last one)
        int k = -1;                 // indicates that current Panel index is N-1, but no default-view-Dialog has been added to this Panel yet.
        for (int i = n-1; i >= 0; --i) {
            final ToggleDialog dlg = allDialogs.get(i);
            if (dlg.isDialogInDefaultView()) {
                if (k == -1) {
                    k = n-1;
                } else {
                    --k;
                    p = panels.get(k);
                }
                p.add(dlg, 0);
                p.setVisible(true);
            } else if (dlg.isDialogInCollapsedView()) {
                p.add(dlg, 0);
                p.setVisible(true);
            }
        }

        if (k == -1) {
            k = n-1;
        }
        final int numPanels = n - k;

        /**
         * Determine the panel geometry
         */
        if (action == Action.RESTORE_SAVED || action == Action.ELEMENT_SHRINKS) {
            for (int i = 0; i < n; ++i) {
                final ToggleDialog dlg = allDialogs.get(i);
                if (dlg.isDialogInDefaultView()) {
                    final int ph = action == Action.RESTORE_SAVED ? dlg.getLastHeight() : dlg.getPreferredHeight();
                    final int ah = dlg.getSize().height;
                    dlg.setPreferredSize(new Dimension(Integer.MAX_VALUE, ah < 20 ? ph : ah));
                }
            }
        } else {
            CheckParameterUtil.ensureParameterNotNull(triggeredBy, "triggeredBy");

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
            final int h = mSpltPane.getMultiSplitLayout().getModel().getBounds().getSize().height;

            /** space, that is available for dialogs in default view (after the reconfiguration) */
            final int s2 = h - (numPanels - 1) * DIVIDER_SIZE - sumC;

            final int hpTrig = triggeredBy.getPreferredHeight();
            if (hpTrig <= 0) throw new IllegalStateException(); // Must be positive

            /** The new dialog gets a fair share */
            final int hnTrig = hpTrig * s2 / (hpTrig + sumP);
            triggeredBy.setPreferredSize(new Dimension(Integer.MAX_VALUE, hnTrig));

            /** This is remaining for the other default view dialogs */
            final int r = s2 - hnTrig;

            /**
             * Take space only from dialogs that are relatively large
             */
            int dm = 0;        // additional space needed by the small dialogs
            int dp = 0;        // available space from the large dialogs
            for (int i = 0; i < n; ++i) {
                final ToggleDialog dlg = allDialogs.get(i);
                if (dlg != triggeredBy && dlg.isDialogInDefaultView()) {
                    final int ha = dlg.getSize().height;                              // current
                    final int h0 = ha * r / sumA;                                     // proportional shrinking
                    final int he = dlg.getPreferredHeight() * s2 / (sumP + hpTrig);  // fair share
                    if (h0 < he) {                  // dialog is relatively small
                        int hn = Math.min(ha, he);  // shrink less, but do not grow
                        dm += hn - h0;
                    } else {                        // dialog is relatively large
                        dp += h0 - he;
                    }
                }
            }
            /** adjust, without changing the sum */
            for (int i = 0; i < n; ++i) {
                final ToggleDialog dlg = allDialogs.get(i);
                if (dlg != triggeredBy && dlg.isDialogInDefaultView()) {
                    final int ha = dlg.getHeight();
                    final int h0 = ha * r / sumA;
                    final int he = dlg.getPreferredHeight() * s2 / (sumP + hpTrig);
                    if (h0 < he) {
                        int hn = Math.min(ha, he);
                        dlg.setPreferredSize(new Dimension(Integer.MAX_VALUE, hn));
                    } else {
                        int d = dp == 0 ? 0 : ((h0-he) * dm / dp);
                        dlg.setPreferredSize(new Dimension(Integer.MAX_VALUE, h0 - d));
                    }
                }
            }
        }

        /**
         * create Layout
         */
        final List<Node> ch = new ArrayList<>();

        for (int i = k; i <= n-1; ++i) {
            if (i != k) {
                ch.add(new Divider());
            }
            Leaf l = new Leaf('L'+Integer.toString(i));
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
        if (numPanels == 1 && panels.get(n-1).getComponents().length == 0) {
            myParent.setDividerSize(0);
            this.setVisible(false);
        } else {
            if (this.getWidth() != 0) { // only if josm started with hidden panel
                this.setPreferredSize(new Dimension(this.getWidth(), 0));
            }
            this.setVisible(true);
            myParent.setDividerSize(5);
            myParent.resetToPreferredSizes();
        }
    }

    @Override
    public void destroy() {
        for (ToggleDialog t : allDialogs) {
            try {
                t.destroy();
            } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                throw BugReport.intercept(e).put("dialog", t).put("dialog-class", t.getClass());
            }
        }
        mSpltPane.removeAll();
        allDialogs.clear();
        panels.clear();
    }

    /**
     * Replies the instance of a toggle dialog of type <code>type</code> managed by this
     * map frame
     *
     * @param <T> toggle dialog type
     * @param type the class of the toggle dialog, i.e. UserListDialog.class
     * @return the instance of a toggle dialog of type <code>type</code> managed by this
     * map frame; null, if no such dialog exists
     *
     */
    public <T extends ToggleDialog> T getToggleDialog(Class<T> type) {
        return Utils.filteredCollection(allDialogs, type).stream()
                .findFirst().orElse(null);
    }
}
