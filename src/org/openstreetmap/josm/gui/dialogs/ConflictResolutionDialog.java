// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.pair.ConflictResolver;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This is an extended dialog for resolving conflict between {@link OsmPrimitive}s.
 *
 */
public class ConflictResolutionDialog extends JDialog implements PropertyChangeListener {
    /** the conflict resolver component */
    private ConflictResolver resolver;

    private ApplyResolutionAction applyResolutionAction;

    @Override
    public void removeNotify() {
        super.removeNotify();
        unregisterListeners();
    }

    @Override
    public void setVisible(boolean isVisible) {
        String geom = getClass().getName() + ".geometry";
        if (isVisible){
            toFront();
            new WindowGeometry(geom, WindowGeometry.centerInWindow(Main.parent,
                new Dimension(600, 400))).applySafe(this);
        } else {
            if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
                new WindowGeometry(this).remember(geom);
            }
            unregisterListeners();
        }
        super.setVisible(isVisible);
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }

    /**
     * builds the sub panel with the control buttons
     *
     * @return the panel
     */
    protected JPanel buildButtonRow() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        applyResolutionAction = new ApplyResolutionAction();
        JButton btn = new JButton(applyResolutionAction);
        btn.setName("button.apply");
        pnl.add(btn);

        btn = new JButton(new CancelAction());
        btn.setName("button.cancel");
        pnl.add(btn);

        btn = new JButton(new HelpAction());
        btn.setName("button.help");
        pnl.add(btn);

        pnl.setBorder(BorderFactory.createLoweredBevelBorder());
        return pnl;
    }

    private void registerListeners() {
        resolver.addPropertyChangeListener(applyResolutionAction);
    }

    private void unregisterListeners() {
        resolver.removePropertyChangeListener(applyResolutionAction);
        resolver.unregisterListeners();
    }

    /**
     * builds the GUI
     */
    protected void build() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        updateTitle();
        getContentPane().setLayout(new BorderLayout());

        resolver = new ConflictResolver();
        resolver.setName("panel.conflictresolver");
        getContentPane().add(resolver, BorderLayout.CENTER);
        getContentPane().add(buildButtonRow(), BorderLayout.SOUTH);

        resolver.addPropertyChangeListener(this);
        HelpUtil.setHelpContext(this.getRootPane(), ht("Dialog/Conflict"));

        registerListeners();
    }

    public ConflictResolutionDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), ModalityType.DOCUMENT_MODAL);
        build();
    }

    public ConflictResolver getConflictResolver() {
        return resolver;
    }

    /**
     * Action for canceling conflict resolution
     */
    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution and close the dialog"));
            putValue(Action.NAME, tr("Cancel"));
            putValue(Action.SMALL_ICON, ImageProvider.get("", "cancel"));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            closeDialog();
        }
    }

    /**
     * Action for canceling conflict resolution
     */
    static class HelpAction extends AbstractAction {
        public HelpAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Show help information"));
            putValue(Action.NAME, tr("Help"));
            putValue(Action.SMALL_ICON, ImageProvider.get("help"));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            HelpBrowser.setUrlForHelpTopic(ht("/Dialog/Conflict"));
        }
    }

    /**
     * Action for applying resolved differences in a conflict
     *
     */
    class ApplyResolutionAction extends AbstractAction implements PropertyChangeListener {
        public ApplyResolutionAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts and close the dialog"));
            putValue(Action.NAME, tr("Apply Resolution"));
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs", "conflict"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(resolver.isResolvedCompletely());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (! resolver.isResolvedCompletely()) {
                Object[] options = {
                        tr("Close anyway"),
                        tr("Continue resolving")};
                int ret = JOptionPane.showOptionDialog(Main.parent,
                        tr("<html>You did not finish to merge the differences in this conflict.<br>"
                                + "Conflict resolutions will not be applied unless all differences<br>"
                                + "are resolved.<br>"
                                + "Click <strong>{0}</strong> to close anyway.<strong> Already<br>"
                                + "resolved differences will not be applied.</strong><br>"
                                + "Click <strong>{1}</strong> to return to resolving conflicts.</html>"
                                , options[0].toString(), options[1].toString()
                        ),
                        tr("Conflict not resolved completely"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[1]
                );
                switch(ret) {
                case JOptionPane.YES_OPTION:
                    closeDialog();
                    break;
                default:
                    return;
                }
            }
            Command cmd = resolver.buildResolveCommand();
            Main.main.undoRedo.add(cmd);
            closeDialog();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ConflictResolver.RESOLVED_COMPLETELY_PROP)) {
                updateEnabledState();
            }
        }
    }

    protected void updateTitle() {
        updateTitle(null);
    }

    protected void updateTitle(OsmPrimitive my) {
        if (my == null) {
            setTitle(tr("Resolve conflicts"));
        } else {
            setTitle(tr("Resolve conflicts for ''{0}''", my.getDisplayName(DefaultNameFormatter.getInstance())));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConflictResolver.MY_PRIMITIVE_PROP)) {
            updateTitle((OsmPrimitive)evt.getNewValue());
        }
    }
}
