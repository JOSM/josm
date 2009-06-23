// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.gui.conflict.ConflictResolver;
import org.openstreetmap.josm.gui.conflict.properties.OperationCancelledException;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is an extended dialog for resolving conflict between {@see OsmPrimitive}.
 *
 *
 */
public class ConflictResolutionDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ConflictResolutionDialog.class.getName());
    public final static Dimension DEFAULT_SIZE = new Dimension(600,400);

    /** the conflict resolver component */
    private ConflictResolver resolver;

    /**
     * restore position and size on screen from preference settings
     *
     */
    protected void restorePositionAndDimension() {
        Point p = new Point();
        Dimension d = new Dimension();
        try {
            p.x = Integer.parseInt(Main.pref.get("conflictresolutiondialog.x", "0"));
            p.x = Math.max(0,p.x);
        } catch(Exception e) {
            logger.warning("unexpected value for preference conflictresolutiondialog.x, assuming 0");
            p.x = 0;
        }
        try {
            p.y = Integer.parseInt(Main.pref.get("conflictresolutiondialog.y", "0"));
            p.y = Math.max(0,p.y);
        } catch(Exception e) {
            logger.warning("unexpected value for preference conflictresolutiondialog.x, assuming 0");
            p.y = 0;
        }
        try {
            d.width = Integer.parseInt(Main.pref.get("conflictresolutiondialog.width", Integer.toString(DEFAULT_SIZE.width)));
            d.width = Math.max(0,d.width);
        } catch(Exception e) {
            logger.warning("unexpected value for preference conflictresolutiondialog.width, assuming " + DEFAULT_SIZE.width);
            p.y = 0;
        }
        try {
            d.height = Integer.parseInt(Main.pref.get("conflictresolutiondialog.height", Integer.toString(DEFAULT_SIZE.height)));
            d.height = Math.max(0,d.height);
        } catch(Exception e) {
            logger.warning("unexpected value for preference conflictresolutiondialog.height, assuming " +  + DEFAULT_SIZE.height);
            p.y = 0;
        }

        setLocation(p);
        setSize(d);
    }

    /**
     * remember position and size on screen in the preferences
     *
     */
    protected void rememberPositionAndDimension() {
        Point p = getLocation();
        Main.pref.put("conflictresolutiondialog.x", Integer.toString(p.x));
        Main.pref.put("conflictresolutiondialog.y", Integer.toString(p.y));

        Dimension d = getSize();
        Main.pref.put("conflictresolutiondialog.width", Integer.toString(d.width));
        Main.pref.put("conflictresolutiondialog.height", Integer.toString(d.height));
    }


    @Override
    public void setVisible(boolean isVisible) {
        if (isVisible){
            restorePositionAndDimension();
        } else {
            rememberPositionAndDimension();
        }
        super.setVisible(isVisible);
    }

    /**
     * builds the sub panel with the control buttons
     *
     * @return the panel
     */
    protected JPanel buildButtonRow() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton btn = new JButton(new CancelAction());
        btn.setName("button.cancel");
        pnl.add(btn);

        btn = new JButton(new ApplyResolutionAction());
        btn.setName("button.apply");
        pnl.add(btn);

        pnl.setBorder(BorderFactory.createLoweredBevelBorder());
        return pnl;
    }

    /**
     * builds the GUI
     */
    protected void build() {
        setTitle(tr("Resolve conflicts"));
        getContentPane().setLayout(new BorderLayout());

        resolver = new ConflictResolver();
        resolver.setName("panel.conflictresolver");
        getContentPane().add(resolver, BorderLayout.CENTER);
        getContentPane().add(buildButtonRow(), BorderLayout.SOUTH);
    }


    public ConflictResolutionDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent), true /* modal */);
        build();
    }

    public ConflictResolver getConflictResolver() {
        return resolver;
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution and close the dialog"));
            putValue(Action.NAME, tr("Cancel"));
            putValue(Action.SMALL_ICON, ImageProvider.get("", "cancel"));
            setEnabled(true);
        }


        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }
    }

    class ApplyResolutionAction extends AbstractAction {
        public ApplyResolutionAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts and close the dialog"));
            putValue(Action.NAME, tr("Apply Resolution"));
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs", "conflict"));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent arg0) {
            if (! resolver.isResolvedCompletely()) {
                Object[] options = {
                        tr("Apply partial resolutions"),
                        tr("Continue resolving")};
                int n = JOptionPane.showOptionDialog(null,
                        tr("<html>You didn''t finish to resolve all conflicts.<br>"
                                + "Click <strong>{0}</strong> to apply already resolved conflicts anyway.<br>"
                                + "You can resolve the remaining conflicts later.<br>"
                                + "Click <strong>{1}</strong> to return to resolving conflicts.</html>"
                                , options[0].toString(), options[1].toString()
                        ),
                        tr("Warning"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[1]
                );
                if (n == JOptionPane.NO_OPTION || n == JOptionPane.CLOSED_OPTION)
                    return;
            }
            try {
                Command cmd = resolver.buildResolveCommand();
                Main.main.undoRedo.add(cmd);
            } catch(OperationCancelledException e) {
                // do nothing. Exception already reported
            }
            setVisible(false);
        }
    }
}
