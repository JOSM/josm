// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel.Action;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This class is a toggle dialog that can be turned on and off.
 *
 *
 */
public class ToggleDialog extends JPanel implements Helpful {
    /** The action to toggle this dialog */
    private ToggleDialogAction toggleAction;
    private String preferencePrefix;

    /** DialogsPanel that manages all ToggleDialogs */
    private DialogsPanel dialogsPanel;

    private TitleBar titleBar;

    /**
     * Indicates whether the dialog is showing or not.
     */
    private boolean isShowing;
    /**
     * If isShowing is true, indicates whether the dialog is docked or not, e. g.
     * shown as part of the main window or as a seperate dialog window.
     */
    private boolean isDocked;
    /**
     * If isShowing and isDocked are true, indicates whether the dialog is
     * currently minimized or not.
     */
    boolean isCollapsed;

    /** the preferred height if the toggle dialog is expanded */
    private int preferredHeight;

    /** the label in the title bar which shows whether the toggle dialog is expanded or collapsed */
    private JLabel lblMinimized;

    /** the JDialog displaying the toggle dialog as undocked dialog */
    private JDialog detachedDialog;

    /**
     * Constructor
     * (see below)
     */
    public ToggleDialog(String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight) {
        this(name, iconName, tooltip, shortcut, preferredHeight, false);
    }
    /**
     * Constructor
     *
     * @param name  the name of the dialog
     * @param iconName the name of the icon to be displayed
     * @param tooltip  the tool tip
     * @param shortcut  the shortcut
     * @param preferredHeight the preferred height for the dialog
     * @param defShow if the dialog should be shown by default, if there is no preference
     */
    public ToggleDialog(String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight, boolean defShow) {
        super(new BorderLayout());
        this.preferencePrefix = iconName;

        /** Use the full width of the parent element */
        setPreferredSize(new Dimension(0, preferredHeight));
        /** Override any minimum sizes of child elements so the user can resize freely */
        setMinimumSize(new Dimension(0,0));
        this.preferredHeight = preferredHeight;
        toggleAction = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, shortcut, iconName);
        String helpId = "Dialog/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
        toggleAction.putValue("help", helpId.substring(0, helpId.length()-6));

        setLayout(new BorderLayout());

        /** show the minimize button */
        lblMinimized = new JLabel(ImageProvider.get("misc", "normal"));
        titleBar = new TitleBar(name, iconName);
        add(titleBar, BorderLayout.NORTH);

        setBorder(BorderFactory.createEtchedBorder());

        isShowing = Main.pref.getBoolean(preferencePrefix+".visible", defShow);
        isDocked = Main.pref.getBoolean(preferencePrefix+".docked", true);
        isCollapsed = Main.pref.getBoolean(preferencePrefix+".minimized", false);
        //System.err.println(name+": showing="+isShowing+" docked="+isDocked+" collapsed="+isCollapsed);
    }

    /**
     * The action to toggle the visibility state of this toggle dialog.
     *
     * Emits {@see PropertyChangeEvent}s for the property <tt>selected</tt>:
     * <ul>
     *   <li>true, if the dialog is currently visible</li>
     *   <li>false, if the dialog is currently invisible</li>
     * </ul>
     *
     */
    public final class ToggleDialogAction extends JosmAction {
        private ToggleDialogAction(String name, String iconName, String tooltip, Shortcut shortcut, String prefname) {
            super(name, iconName, tooltip, shortcut, false);
        }

        public void actionPerformed(ActionEvent e) {
            if (isShowing) {
                hideDialog();
                dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
            } else {
                showDialog();
                if (isDocked && isCollapsed) {
                    expand();
                }
                if (isDocked) {
                    dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, ToggleDialog.this);
                }
            }
        }
    }

    /**
     * Shows the dialog
     */
    public void showDialog() {
        setIsShowing(true);
        if (!isDocked) {
            detach();
        } else {
            dock();
            this.setVisible(true);
        }
        // toggling the selected value in order to enforce PropertyChangeEvents
        setIsShowing(true);
        toggleAction.putValue("selected", false);
        toggleAction.putValue("selected", true);
        showNotify();
    }

    /**
     * Hides the dialog
     */
    public void hideDialog() {
        closeDetachedDialog();
        this.setVisible(false);
        setIsShowing(false);
        toggleAction.putValue("selected", false);
        hideNotify();
    }

    /**
     * Displays the toggle dialog in the toggle dialog view on the right
     * of the main map window.
     *
     */
    protected void dock() {
        detachedDialog = null;
        titleBar.setVisible(true);
        setIsDocked(true);
    }

    /**
     * Display the dialog in a detached window.
     *
     */
    protected void detach() {
        setContentVisible(true);
        this.setVisible(true);
        titleBar.setVisible(false);
        detachedDialog = new DetachedDialog();
        detachedDialog.setVisible(true);
        setIsDocked(false);
    }

    /**
     * Collapses the toggle dialog to the title bar only
     *
     */
    public void collapse() {
//        if (isShowing && isDocked && !isCollapsed) {
            setContentVisible(false);
            setIsCollapsed(true);
            setPreferredSize(new Dimension(0,20));
            setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
            setMinimumSize(new Dimension(Integer.MAX_VALUE,20));
            lblMinimized.setIcon(ImageProvider.get("misc", "minimized"));
            hideNotify();
//        }
//        else throw ...
    }

    /**
     * Expands the toggle dialog
     */
    protected void expand() {
//        if (isShowing && isDocked && isCollapsed) {
            setContentVisible(true);
            setIsCollapsed(false);
            setPreferredSize(new Dimension(0,preferredHeight));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            lblMinimized.setIcon(ImageProvider.get("misc", "normal"));
            showNotify();
//        }
//        else throw ...
    }

    /**
     * Sets the visibility of all components in this toggle dialog, except the title bar
     *
     * @param visible true, if the components should be visible; false otherwise
     */
    protected void setContentVisible(boolean visible) {
        Component comps[] = getComponents();
        for(int i=0; i<comps.length; i++) {
            if(comps[i] != titleBar) {
                comps[i].setVisible(visible);
            }
        }
    }

    public void destroy() {
        closeDetachedDialog();
        hideNotify();
    }

    /**
     * Closes the the detached dialog if this toggle dialog is currently displayed
     * in a detached dialog.
     *
     */
    public void closeDetachedDialog() {
        if (detachedDialog != null) {
            detachedDialog.setVisible(false);
            detachedDialog.getContentPane().removeAll();
            detachedDialog.dispose();
        }
    }

    /**
     * Called when toggle dialog is shown (after it was created or expanded). Descendants may overwrite this
     * method, it's a good place to register listeners needed to keep dialog updated
     */
    public void showNotify() {

    }

    /**
     * Called when toggle dialog is hidden (collapsed, removed, MapFrame is removed, ...). Good place to unregister
     * listeners
     */
    public void hideNotify() {

    }

    /**
     * The title bar displayed in docked mode
     *
     */
    private class TitleBar extends JPanel {
        final private JLabel lblTitle;
        final private JComponent lblTitle_weak;

        public TitleBar(String toggleDialogName, String iconName) {
            setLayout(new GridBagLayout());
            lblMinimized = new JLabel(ImageProvider.get("misc", "normal"));
            add(lblMinimized);

            // scale down the dialog icon
            ImageIcon inIcon = ImageProvider.get("dialogs", iconName);
            ImageIcon smallIcon = new ImageIcon(inIcon.getImage().getScaledInstance(16 , 16, Image.SCALE_SMOOTH));
            lblTitle = new JLabel("",smallIcon, JLabel.TRAILING);
            lblTitle.setIconTextGap(8);

            JPanel conceal = new JPanel();
            conceal.add(lblTitle);
            conceal.setVisible(false);
            add(conceal, GBC.std());

            // Cannot add the label directly since it would displace other elements on resize
            lblTitle_weak = new JComponent() {
                @Override
                public void paintComponent(Graphics g) {
                    lblTitle.paint(g);
                }
            };
            lblTitle_weak.setPreferredSize(new Dimension(Integer.MAX_VALUE,20));
            lblTitle_weak.setMinimumSize(new Dimension(0,20));
            add(lblTitle_weak, GBC.std().fill(GBC.HORIZONTAL));

            addMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            // toggleExpandedState();
                            if (isCollapsed) {
                                expand();
                                dialogsPanel.reconstruct(Action.COLLAPSED_TO_DEFAULT, ToggleDialog.this);
                            } else {
                                collapse();
                                dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                            }
                        }
                    }
            );

            // show the sticky button
            JButton sticky = new JButton(ImageProvider.get("misc", "sticky"));
            sticky.setToolTipText(tr("Undock the panel"));
            sticky.setBorder(BorderFactory.createEmptyBorder());
            sticky.addActionListener(
                    new ActionListener(){
                        public void actionPerformed(ActionEvent e) {
                            detach();
                            dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                        }
                    }
            );
            add(sticky);

            // show the close button
            JButton close = new JButton(ImageProvider.get("misc", "close"));
            close.setToolTipText(tr("Close this panel. You can reopen it with the buttons in the left toolbar."));
            close.setBorder(BorderFactory.createEmptyBorder());
            close.addActionListener(
                    new ActionListener(){
                        public void actionPerformed(ActionEvent e) {
                            hideDialog();
                            dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                        }
                    }
            );
            add(close);
            setToolTipText(tr("Click to minimize/maximize the panel content"));
            setTitle(toggleDialogName);
        }

        public void setTitle(String title) {
            lblTitle.setText(title);
            lblTitle_weak.repaint();
        }

        public String getTitle() {
            return lblTitle.getText();
        }
    }

    /**
     * The dialog class used to display toggle dialogs in a detached window.
     *
     */
    private class DetachedDialog extends JDialog{
        public DetachedDialog() {
            super(JOptionPane.getFrameForComponent(Main.parent));
            getContentPane().add(ToggleDialog.this);
            addWindowListener(new WindowAdapter(){
                @Override public void windowClosing(WindowEvent e) {
                    rememberGeometry();
                    getContentPane().removeAll();
                    dispose();
                    dock();
                    expand();
                    dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, ToggleDialog.this);
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override public void componentMoved(ComponentEvent e) {
                    rememberGeometry();
                }
                @Override public void componentResized(ComponentEvent e) {
                    rememberGeometry();
                }
            });

            String bounds = Main.pref.get(preferencePrefix+".bounds",null);
            if (bounds != null) {
                String[] b = bounds.split(",");
                setBounds(getDetachedGeometry(new Rectangle(
                        Integer.parseInt(b[0]),Integer.parseInt(b[1]),Integer.parseInt(b[2]),Integer.parseInt(b[3]))));
            } else {
                ToggleDialog.this.setPreferredSize(ToggleDialog.this.getDefaultDetachedSize());
                pack();
                setLocationRelativeTo(Main.parent);
            }
            setTitle(titleBar.getTitle());
            HelpUtil.setHelpContext(getRootPane(), helpTopic());
        }

        protected void rememberGeometry() {
            Main.pref.put(preferencePrefix+".bounds", detachedDialog.getX()+","+detachedDialog.getY()+","+detachedDialog.getWidth()+","+detachedDialog.getHeight());
        }
    }

    /**
     * Change the Geometry of the detached dialog to better fit the content.
     * Overrride this to make it useful.
     */
    protected Rectangle getDetachedGeometry(Rectangle last) {
        return last;
    }

    /**
     * Default size of the detached dialog.
     * Override this method to customize the initial dialog size.
     */
    protected Dimension getDefaultDetachedSize() {
        return new Dimension(Main.map.DEF_TOGGLE_DLG_WIDTH, preferredHeight);
    }

    /**
     * Replies the action to toggle the visible state of this toggle dialog
     *
     * @return the action to toggle the visible state of this toggle dialog
     */
    public AbstractAction getToggleAction() {
        return toggleAction;
    }

    /**
     * Replies the prefix for the preference settings of this dialog.
     *
     * @return the prefix for the preference settings of this dialog.
     */
    public String getPreferencePrefix() {
        return preferencePrefix;
    }

    /**
     * Sets the dialogsPanel managing all toggle dialogs
     */
    public void setDialogsPanel(DialogsPanel dialogsPanel) {
        this.dialogsPanel = dialogsPanel;
    }

    /**
     * Replies the name of this toggle dialog
     */
    @Override
    public String getName() {
        return "toggleDialog." + preferencePrefix;
    }

    /**
     * Sets the title
     */
    public void setTitle(String title) {
        titleBar.setTitle(title);
    }

    private void setIsShowing(boolean val) {
        isShowing = val;
        Main.pref.put(preferencePrefix+".visible", val);
    }

    private void setIsDocked(boolean val) {
        isDocked = val;
        Main.pref.put(preferencePrefix+".docked", val);
    }

    private void setIsCollapsed(boolean val) {
        isCollapsed = val;
        Main.pref.put(preferencePrefix+".minimized", val);
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    /**
     * Replies true if this dialog is showing either as docked or as detached dialog
     */
    public boolean isDialogShowing() {
        return isShowing;
    }

    /**
     * Replies true if this dialog is docked and expanded
     */
    public boolean isDialogInDefaultView() {
        return isShowing && isDocked && (! isCollapsed);
    }

    /**
     * Replies true if this dialog is docked and collapsed
     */
    public boolean isDialogInCollapsedView() {
        return isShowing && isDocked && isCollapsed;
    }

    public String helpTopic() {
        String help = getClass().getName();
        help = help.substring(help.lastIndexOf('.')+1, help.length()-6);
        return "Dialog/"+help;
    }
}
