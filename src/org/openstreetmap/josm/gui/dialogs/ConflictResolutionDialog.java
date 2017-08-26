// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.conflict.pair.ConflictResolver;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is an extended dialog for resolving conflict between {@link OsmPrimitive}s.
 * @since 1622
 */
public class ConflictResolutionDialog extends ExtendedDialog implements PropertyChangeListener {
    /** the conflict resolver component */
    private final ConflictResolver resolver = new ConflictResolver();
    private final JLabel titleLabel = new JLabel("", null, JLabel.CENTER);

    private final ApplyResolutionAction applyResolutionAction = new ApplyResolutionAction();

    private boolean isRegistered;

    /**
     * Constructs a new {@code ConflictResolutionDialog}.
     * @param parent parent component
     */
    public ConflictResolutionDialog(Component parent) {
        // We define our own actions, but need to give a hint about number of buttons
        super(parent, tr("Resolve conflicts"), null, null, null);
        setDefaultButton(1);
        setCancelButton(2);
        build();
        pack();
        if (getInsets().top > 0) {
            titleLabel.setVisible(false);
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        unregisterListeners();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        registerListeners();
    }

    private synchronized void registerListeners() {
        if (!isRegistered) {
            resolver.addPropertyChangeListener(applyResolutionAction);
            resolver.registerListeners();
            isRegistered = true;
        }
    }

    private synchronized void unregisterListeners() {
        // See #13479 - See https://bugs.openjdk.java.net/browse/JDK-4387314
        // Owner window keep a list of owned windows, and does not remove the references when the child is disposed.
        // There's no easy way to remove ourselves from this list, so we must keep track of register state
        if (isRegistered) {
            resolver.removePropertyChangeListener(applyResolutionAction);
            resolver.unregisterListeners();
            isRegistered = false;
        }
    }

    /**
     * builds the GUI
     */
    protected void build() {
        JPanel p = new JPanel(new BorderLayout());

        p.add(titleLabel, BorderLayout.NORTH);

        updateTitle();

        resolver.setName("panel.conflictresolver");
        p.add(resolver, BorderLayout.CENTER);

        resolver.addPropertyChangeListener(this);
        HelpUtil.setHelpContext(this.getRootPane(), ht("Dialog/Conflict"));

        setContent(p, false);
    }

    @Override
    protected Action createButtonAction(int i) {
        switch (i) {
            case 0: return applyResolutionAction;
            case 1: return new CancelAction();
            case 2: return new HelpAction();
            default: return super.createButtonAction(i);
        }
    }

    /**
     * Replies the conflict resolver component.
     * @return the conflict resolver component
     */
    public ConflictResolver getConflictResolver() {
        return resolver;
    }

    /**
     * Action for canceling conflict resolution
     */
    class CancelAction extends AbstractAction {
        CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution and close the dialog"));
            putValue(Action.NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            buttonAction(2, evt);
        }
    }

    /**
     * Action for canceling conflict resolution
     */
    static class HelpAction extends AbstractAction {
        HelpAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Show help information"));
            putValue(Action.NAME, tr("Help"));
            new ImageProvider("help").getResource().attachImageIcon(this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            HelpBrowser.setUrlForHelpTopic(ht("/Dialog/Conflict"));
        }
    }

    /**
     * Action for applying resolved differences in a conflict
     *
     */
    class ApplyResolutionAction extends AbstractAction implements PropertyChangeListener {
        ApplyResolutionAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts and close the dialog"));
            putValue(Action.NAME, tr("Apply Resolution"));
            new ImageProvider("dialogs", "conflict").getResource().attachImageIcon(this);
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(resolver.isResolvedCompletely());
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (!resolver.isResolvedCompletely()) {
                Object[] options = {
                        tr("Close anyway"),
                        tr("Continue resolving")};
                int ret = JOptionPane.showOptionDialog(Main.parent,
                        tr("<html>You did not finish to merge the differences in this conflict.<br>"
                                + "Conflict resolutions will not be applied unless all differences<br>"
                                + "are resolved.<br>"
                                + "Click <strong>{0}</strong> to close anyway.<strong> Already<br>"
                                + "resolved differences will not be applied.</strong><br>"
                                + "Click <strong>{1}</strong> to return to resolving conflicts.</html>",
                                options[0].toString(), options[1].toString()
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
                    buttonAction(1, evt);
                    break;
                default:
                    return;
                }
            }
            MainApplication.undoRedo.add(resolver.buildResolveCommand());
            buttonAction(1, evt);
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
    public void setTitle(String title) {
        super.setTitle(title);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConflictResolver.MY_PRIMITIVE_PROP)) {
            updateTitle((OsmPrimitive) evt.getNewValue());
        }
    }
}
