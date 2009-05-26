// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.gui.conflict.ConflictResolver;

public class ConflictResolutionDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ConflictResolutionDialog.class.getName());
    public final static Dimension DEFAULT_SIZE = new Dimension(600,400);

    private ConflictResolver resolver;
        
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
    
    protected void rememberPositionAndDimension() {
        Point p = getLocation();
        Main.pref.put("conflictresolutiondialog.x", Integer.toString(p.x));
        Main.pref.put("conflictresolutiondialog.y", Integer.toString(p.y));
        
        Dimension d = getSize();
        Main.pref.put("conflictresolutiondialog.width", Integer.toString(d.width));
        Main.pref.put("conflictresolutiondialog.height", Integer.toString(d.height));
    }
    
    public void setVisible(boolean isVisible) {
        if (isVisible){
            restorePositionAndDimension();
        } else {
            rememberPositionAndDimension();
        }
        super.setVisible(isVisible);
    }
    
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
    
    protected void build() {
       setTitle(tr("Resolve conflicts"));
       getContentPane().setLayout(new BorderLayout());     
       
       resolver = new ConflictResolver();
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
    
    protected ImageIcon getIcon(String iconPath) {
        URL imageURL   = this.getClass().getResource(iconPath);            
        if (imageURL == null) {
            System.out.println(tr("WARNING: failed to load resource {0}", iconPath));
            return null;
        }
        return new ImageIcon(imageURL);
    }

    
    class CancelAction extends AbstractAction {
        
        public CancelAction() {            
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution and close the dialog"));
            putValue(Action.NAME, tr("Cancel"));
            putValue(Action.SMALL_ICON, getIcon("/images/cancel.png"));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }        
    }   
    
    class ApplyResolutionAction extends AbstractAction {        
        public ApplyResolutionAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts and close the dialog"));
            putValue(Action.NAME, tr("Apply Resolution"));
            putValue(Action.SMALL_ICON, getIcon("/images/dialogs/conflict.png"));
            setEnabled(true);            
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Command cmd = resolver.buildResolveCommand();
            Main.main.undoRedo.add(cmd);
            setVisible(false);
        }        
    }
}
