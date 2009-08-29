// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This class is a toggle dialog that can be turned on and off. It is attached
 * to a ButtonModel.
 *
 * @author imi
 */
public class ToggleDialog extends JPanel implements Helpful {

    public final class ToggleDialogAction extends JosmAction {
        public final String prefname;
        public AbstractButton button;

        private ToggleDialogAction(String name, String iconName, String tooltip, Shortcut shortcut, String prefname) {
            super(name, iconName, tooltip, shortcut, false);
            this.prefname = prefname;
        }

        public void actionPerformed(ActionEvent e) {
            if (e != null && !(e.getSource() instanceof AbstractButton)) {
                button.setSelected(!button.isSelected());
            }
            Boolean selected = button.isSelected();
            setVisible(selected);
            Main.pref.put(prefname+".visible", selected);
            if(!selected && winadapter != null) {
                winadapter.windowClosing(null);
            } else if (!Main.pref.getBoolean(action.prefname+".docked", true)) {
                EventQueue.invokeLater(new Runnable(){
                    public void run() {
                        stickyActionListener.actionPerformed(null);
                    }
                });
            }
        }
    }

    /**
     * The action to toggle this dialog.
     */
    public ToggleDialogAction action;
    public final String prefName;

    public JPanel parent;
    WindowAdapter winadapter;
    private ActionListener stickyActionListener;
    private final JPanel titleBar = new JPanel(new GridBagLayout());
    public JLabel label = new JLabel();

    public ToggleDialog(final String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight) {
        super(new BorderLayout());
        this.prefName = iconName;
        ToggleDialogInit(name, iconName, tooltip, shortcut, preferredHeight);
    }

    private void ToggleDialogInit(final String name, String iconName, String tooltip, Shortcut shortcut, final int preferredHeight) {
        setPreferredSize(new Dimension(330,preferredHeight));
        action = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, shortcut, iconName);
        String helpId = "Dialog/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
        action.putValue("help", helpId.substring(0, helpId.length()-6));
        setLayout(new BorderLayout());

        // show the minimize button
        final JLabel minimize = new JLabel(ImageProvider.get("misc", "normal"));
        titleBar.add(minimize);

        // scale down the dialog icon
        ImageIcon inIcon = ImageProvider.get("dialogs", iconName);
        ImageIcon smallIcon = new ImageIcon(inIcon.getImage().getScaledInstance(16 , 16, Image.SCALE_SMOOTH));
        JLabel firstPart = new JLabel(name, smallIcon, JLabel.TRAILING);
        firstPart.setIconTextGap(8);
        titleBar.add(firstPart, GBC.std());
        titleBar.add(Box.createHorizontalGlue(),GBC.std().fill(GBC.HORIZONTAL));

        final ActionListener hideActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                boolean nowVisible = false;
                Component comps[] = getComponents();
                for(int i=0; i<comps.length; i++)
                {
                    if(comps[i] != titleBar)
                    {
                        if(comps[i].isVisible()) {
                            comps[i].setVisible(false);
                        } else {
                            comps[i].setVisible(true);
                            nowVisible = true;
                        }
                    }
                }

                Main.pref.put(action.prefname+".minimized", !nowVisible);
                if(nowVisible == true) {
                    setPreferredSize(new Dimension(330,preferredHeight));
                    setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
                    minimize.setIcon(ImageProvider.get("misc", "normal"));
                } else {
                    setPreferredSize(new Dimension(330,20));
                    setMaximumSize(new Dimension(330,20));
                    minimize.setIcon(ImageProvider.get("misc", "minimized"));
                }
                if(parent != null)
                {
                    // doLayout() - workaround
                    parent.setVisible(false);
                    parent.setVisible(true);
                }
            }
        };
        //hide.addActionListener(hideActionListener);

        final MouseListener titleMouseListener = new MouseListener(){
            public void mouseClicked(MouseEvent e) {
                hideActionListener.actionPerformed(null);
            }
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
        };
        titleBar.addMouseListener(titleMouseListener);

        // show the sticky button
        JButton sticky = new JButton(ImageProvider.get("misc", "sticky"));
        sticky.setToolTipText(tr("Undock the panel"));
        sticky.setBorder(BorderFactory.createEmptyBorder());
        stickyActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                final JDialog f = new JDialog(JOptionPane.getFrameForComponent(Main.parent),false /* not modal*/);
                if (parent != null) {
                    parent.remove(ToggleDialog.this);
                }
                f.getContentPane().add(ToggleDialog.this);
                f.addWindowListener((winadapter = new WindowAdapter(){
                    @Override public void windowClosing(WindowEvent e) {
                        f.getContentPane().removeAll();
                        f.dispose();
                        winadapter = null;

                        // doLayout() - workaround
                        setVisible(false);
                        parent.add(ToggleDialog.this);
                        if(Main.pref.getBoolean(action.prefname+".visible")) {
                            setVisible(true);
                        }
                        titleBar.setVisible(true);
                        if(e != null) {
                            Main.pref.put(action.prefname+".docked", true);
                        }
                    }
                }));
                f.addComponentListener(new ComponentAdapter(){
                    @Override public void componentMoved(ComponentEvent e) {
                        Main.pref.put(action.prefname+".bounds", f.getX()+","+f.getY()+","+f.getWidth()+","+f.getHeight());
                    }
                });
                String bounds = Main.pref.get(action.prefname+".bounds",null);
                if (bounds != null) {
                    String[] b = bounds.split(",");
                    f.setBounds(Integer.parseInt(b[0]),Integer.parseInt(b[1]),Integer.parseInt(b[2]),Integer.parseInt(b[3]));
                } else {
                    f.pack();
                }
                Main.pref.put(action.prefname+".docked", false);
                f.setVisible(true);
                titleBar.setVisible(false);

                if (parent != null) {
                    // doLayout() - workaround
                    parent.setVisible(false);
                    parent.setVisible(true);
                }
            }
        };
        sticky.addActionListener(stickyActionListener);
        titleBar.add(sticky);

        // show the close button
        JButton close = new JButton(ImageProvider.get("misc", "close"));
        close.setToolTipText(tr("Close this panel. You can reopen it with the buttons in the left toolbar."));
        close.setBorder(BorderFactory.createEmptyBorder());
        final ActionListener closeActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                // fake an event to toggle dialog
                action.actionPerformed(new ActionEvent(titleBar, 0, ""));
            }
        };
        close.addActionListener(closeActionListener);
        titleBar.add(close);

        add(titleBar, BorderLayout.NORTH);
        titleBar.setToolTipText(tr("Click to minimize/maximize the panel content"));

        setVisible(false);
        setBorder(BorderFactory.createEtchedBorder());

        if (!Main.pref.getBoolean(action.prefname+".docked", true)) {
            EventQueue.invokeLater(new Runnable(){
                public void run() {
                    stickyActionListener.actionPerformed(null);
                }
            });
        }
        if (Main.pref.getBoolean(action.prefname+".minimized", false)) {
            EventQueue.invokeLater(new Runnable(){
                public void run() {
                    titleMouseListener.mouseClicked(null);
                }
            });
        }
    }

    public void close()
    {
        if(winadapter != null) {
            winadapter.windowClosing(null);
        }
    }

    public void setTitle(String title, boolean active) {
        if(active) {
            label.setText("<html><b>" + title + "</b>");
        } else {
            label.setText(title);
        }
    }

    public String helpTopic() {
        String help = getClass().getName();
        help = help.substring(help.lastIndexOf('.')+1, help.length()-6);
        return "Dialog/"+help;
    }
}
