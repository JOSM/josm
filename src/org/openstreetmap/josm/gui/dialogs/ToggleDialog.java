package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This class is a toggle dialog that can be turned on and off.
 * 
 *
 */
public class ToggleDialog extends JPanel implements Helpful {
//    private static final Logger logger = Logger.getLogger(ToggleDialog.class.getName());

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
            toggleVisibility();
        }

        public void toggleVisibility() {
            if (isShowing) {
                hideDialog();
            } else {
                showDialog();
            }
        }
    }

    /**
     * The action to toggle this dialog.
     */
    private ToggleDialogAction toggleAction;
    private String preferencePrefix;

    private JPanel parent;
    private  TitleBar titleBar;
    private String title;

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
    private boolean isCollapsed;

    /** the preferred height if the toggle dialog is expanded */
    private int preferredHeight;
    /** the label in the title bar which shows whether the toggle dialog is expanded or collapsed */
    private JLabel lblMinimized;
    /** the JDialog displaying the toggle dialog as undocked dialog */
    private JDialog detachedDialog;

    /**
     * Constructor
     * 
     * @param name  the name of the dialog
     * @param iconName the name of the icon to be displayed
     * @param tooltip  the tool tip
     * @param shortcut  the shortcut
     * @param preferredHeight the preferred height for the dialog
     */
    public ToggleDialog(String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight) {
        super(new BorderLayout());
        this.preferencePrefix = iconName;
        init(name, iconName, tooltip, shortcut, preferredHeight);
    }

    /**
     * Initializes the toggle dialog
     * 
     * @param name
     * @param iconName
     * @param tooltip
     * @param shortcut
     * @param preferredHeight
     */
    private void init(String name, String iconName, String tooltip, Shortcut shortcut, final int preferredHeight) {
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

        setVisible(false);
        setBorder(BorderFactory.createEtchedBorder());

        isDocked = Main.pref.getBoolean(preferencePrefix+".docked", true);
        isCollapsed = Main.pref.getBoolean(preferencePrefix+".minimized", false);
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

    /**
     * Toggles between collapsed and expanded state
     * 
     */
    protected void toggleExpandedState() {
        if (isCollapsed) {
            expand();
        } else {
            collapse();
        }
    }

    /**
     * Collapses the toggle dialog to the title bar only
     * 
     */
    protected void collapse() {
        setContentVisible(false);
        isCollapsed = true;
        Main.pref.put(preferencePrefix+".minimized", true);
        setPreferredSize(new Dimension(0,20));
        setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
        lblMinimized.setIcon(ImageProvider.get("misc", "minimized"));
        refreshToggleDialogsView();
    }

    /**
     * Expands the toggle dialog
     */
    protected void expand() {
        setContentVisible(true);
        isCollapsed = false;
        Main.pref.put(preferencePrefix+".minimized", false);
        setPreferredSize(new Dimension(0,preferredHeight));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        lblMinimized.setIcon(ImageProvider.get("misc", "normal"));
        refreshToggleDialogsView();
    }

    /**
     * Replies the index of this toggle dialog in the view of
     * toggle dialog.
     * 
     * @return
     */
    protected int getDialogPosition() {
        if (parent == null) return -1;
        for (int i=0; i< parent.getComponentCount(); i++) {
            String name = parent.getComponent(i).getName();
            if (name != null && name.equals(this.getName()))
                return i;
        }
        return -1;
    }

    /**
     * Displays the toggle dialog in the toggle dialog view on the right
     * of the main map window.
     * 
     */
    protected void dock() {
        detachedDialog = null;
        if (parent == null) return;

        // check whether the toggle dialog view contains a placeholder
        // for this toggle dialog. If so, replace it with this dialog.
        //
        int idx = getDialogPosition();
        if (idx > -1) {
            parent.remove(idx);
            if (idx >= parent.getComponentCount()) {
                parent.add(ToggleDialog.this);
            } else {
                parent.add(ToggleDialog.this,idx);
            }
        } else {
            parent.add(ToggleDialog.this);
        }
        parent.validate();

        if(Main.pref.getBoolean(preferencePrefix+".visible")) {
            setVisible(true);
        } else {
            setVisible(false);
        }
        titleBar.setVisible(true);
        isCollapsed = Main.pref.getBoolean(preferencePrefix+".minimized", false);
        if (isCollapsed) {
            collapse();
        } else {
            expand();
        }
        isDocked = true;
        Main.pref.put(preferencePrefix+".docked", isDocked);
    }

    /**
     * Display the dialog in a detached window.
     * 
     */
    protected void detach() {
        setContentVisible(true);
        setVisible(true);
        // replace the toggle dialog by an invisible place holder. Makes sure
        // we can place the toggle dialog where it was when it becomes docked
        // again.
        //
        if (parent != null) {
            int idx = getDialogPosition();
            if (idx > -1) {
                JPanel placeHolder = new JPanel();
                placeHolder.setName(this.getName());
                placeHolder.setVisible(false);
                parent.add(placeHolder,idx);
            }
            parent.remove(ToggleDialog.this);
        }
        

        titleBar.setVisible(false);
        detachedDialog = new DetachedDialog();
        detachedDialog.setVisible(true);
        refreshToggleDialogsView();
        isDocked = false;
        Main.pref.put(preferencePrefix+".docked", isDocked);
    }

    /**
     * Hides the dialog
     */
    public void hideDialog() {
        closeDetachedDialog();
        setVisible(false);
        isShowing = false;
        Main.pref.put(preferencePrefix+".visible", false);
        refreshToggleDialogsView();
        toggleAction.putValue("selected", false);
    }

    /**
     * Replies true if this dialog is showing either as docked or as detached dialog
     */
    public boolean isDialogShowing() {
        return this.isShowing;
    }

    /**
     * Shows the dialog
     */
    public void showDialog() {
        if (!isDocked) {
            detach();
        } else {
            dock();
            if (!isCollapsed) {
                expand();
                setVisible(true);
                refreshToggleDialogsView();
            } else {
                setVisible(true);
                refreshToggleDialogsView();
            }
        }
        isShowing = true;
        // toggling the selected value in order to enforce PropertyChangeEvents
        toggleAction.putValue("selected", false);
        toggleAction.putValue("selected", true);
        Main.pref.put(preferencePrefix+".visible", true);
    }

    /**
     * Refreshes the layout of the parent toggle dialog view
     * 
     */
    protected void refreshToggleDialogsView() {
        if(parent != null){
            parent.validate();
        }
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

    public String helpTopic() {
        String help = getClass().getName();
        help = help.substring(help.lastIndexOf('.')+1, help.length()-6);
        return "Dialog/"+help;
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
     * Sets the parent displaying all toggle dialogs
     * 
     * @param parent the parent
     */
    public void setParent(JPanel parent) {
        this.parent = parent;
    }

    /**
     * Replies the name of this toggle dialog
     *
     */
    @Override
    public String getName() {
        return "toggleDialog." + preferencePrefix;
    }

    /**
     * Sets the title
     * 
     * @param title the title
     */
    public void setTitle(String title) {
        titleBar.setTitle(title);
    }

    /**
     * The title bar displayed in docked mode
     * 
     */
    private class TitleBar extends JPanel {
        private JLabel leftPart;

        public TitleBar(String toggleDialogName, String iconName) {
            setLayout(new GridBagLayout());
            lblMinimized = new JLabel(ImageProvider.get("misc", "normal"));
            add(lblMinimized);

            // scale down the dialog icon
            ImageIcon inIcon = ImageProvider.get("dialogs", iconName);
            ImageIcon smallIcon = new ImageIcon(inIcon.getImage().getScaledInstance(16 , 16, Image.SCALE_SMOOTH));
            leftPart = new JLabel("",smallIcon, JLabel.TRAILING);
            leftPart.setIconTextGap(8);
            add(leftPart, GBC.std());
            add(Box.createHorizontalGlue(),GBC.std().fill(GBC.HORIZONTAL));
            addMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            toggleExpandedState();
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
                        }
                    }
            );
            add(close);
            setToolTipText(tr("Click to minimize/maximize the panel content"));
            setTitle(toggleDialogName);
        }

        public void setTitle(String title) {
            leftPart.setText(title);
        }

        public String getTitle() {
            return leftPart.getText();
        }
    }

    /**
     * The dialog class used to display toggle dialogs in a detached window.
     * 
     */
    private class DetachedDialog extends JDialog {
        public DetachedDialog() {
            super(JOptionPane.getFrameForComponent(Main.parent));
            getContentPane().add(ToggleDialog.this);
            addWindowListener(new WindowAdapter(){
                @Override public void windowClosing(WindowEvent e) {
                    rememberGeometry();
                    getContentPane().removeAll();
                    dispose();
                    dock();
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
}
