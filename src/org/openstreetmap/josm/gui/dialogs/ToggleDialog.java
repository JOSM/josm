// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.ParametrizedEnumProperty;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.ShowHideButtonListener;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel.Action;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.WindowGeometry;
import org.openstreetmap.josm.tools.WindowGeometry.WindowGeometryException;

/**
 * This class is a toggle dialog that can be turned on and off.
 *
 */
public class ToggleDialog extends JPanel implements ShowHideButtonListener, Helpful, AWTEventListener, Destroyable, PreferenceChangedListener {

    /**
     * The button-hiding strategy in toggler dialogs.
     */
    public enum ButtonHidingType {
        /** Buttons are always shown (default) **/
        ALWAYS_SHOWN,
        /** Buttons are always hidden **/
        ALWAYS_HIDDEN,
        /** Buttons are dynamically hidden, i.e. only shown when mouse cursor is in dialog */
        DYNAMIC
    }

    /**
     * Property to enable dyanmic buttons globally.
     * @since 6752
     */
    public static final BooleanProperty PROP_DYNAMIC_BUTTONS = new BooleanProperty("dialog.dynamic.buttons", false);

    private final ParametrizedEnumProperty<ButtonHidingType> PROP_BUTTON_HIDING = new ParametrizedEnumProperty<ToggleDialog.ButtonHidingType>(
            ButtonHidingType.class, ButtonHidingType.DYNAMIC) {
        @Override
        protected String getKey(String... params) {
            return preferencePrefix + ".buttonhiding";
        }
        @Override
        protected ButtonHidingType parse(String s) {
            try {
                return super.parse(s);
            } catch (IllegalArgumentException e) {
                // Legacy settings
                return Boolean.parseBoolean(s)?ButtonHidingType.DYNAMIC:ButtonHidingType.ALWAYS_SHOWN;
            }
        }
    };

    /** The action to toggle this dialog */
    protected final ToggleDialogAction toggleAction;
    protected String preferencePrefix;
    final protected String name;

    /** DialogsPanel that manages all ToggleDialogs */
    protected DialogsPanel dialogsPanel;

    protected TitleBar titleBar;

    /**
     * Indicates whether the dialog is showing or not.
     */
    protected boolean isShowing;

    /**
     * If isShowing is true, indicates whether the dialog is docked or not, e. g.
     * shown as part of the main window or as a separate dialog window.
     */
    protected boolean isDocked;

    /**
     * If isShowing and isDocked are true, indicates whether the dialog is
     * currently minimized or not.
     */
    protected boolean isCollapsed;

    /**
     * Indicates whether dynamic button hiding is active or not.
     */
    protected ButtonHidingType buttonHiding;

    /** the preferred height if the toggle dialog is expanded */
    private int preferredHeight;

    /** the JDialog displaying the toggle dialog as undocked dialog */
    protected JDialog detachedDialog;

    protected JToggleButton button;
    private JPanel buttonsPanel;
    private List<javax.swing.Action> buttonActions = new ArrayList<javax.swing.Action>();

    /** holds the menu entry in the windows menu. Required to properly
     * toggle the checkbox on show/hide
     */
    protected JCheckBoxMenuItem windowMenuItem;

    /**
     * The linked preferences class (optional). If set, accessible from the title bar with a dedicated button
     */
    protected Class<? extends PreferenceSetting> preferenceClass;

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
        this(name, iconName, tooltip, shortcut, preferredHeight, false);
    }

    /**
     * Constructor

     * @param name  the name of the dialog
     * @param iconName the name of the icon to be displayed
     * @param tooltip  the tool tip
     * @param shortcut  the shortcut
     * @param preferredHeight the preferred height for the dialog
     * @param defShow if the dialog should be shown by default, if there is no preference
     */
    public ToggleDialog(String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight, boolean defShow) {
        this(name, iconName, tooltip, shortcut, preferredHeight, defShow, null);
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
     * @param prefClass the preferences settings class, or null if not applicable
     */
    public ToggleDialog(String name, String iconName, String tooltip, Shortcut shortcut, int preferredHeight, boolean defShow, Class<? extends PreferenceSetting> prefClass) {
        super(new BorderLayout());
        this.preferencePrefix = iconName;
        this.name = name;
        this.preferenceClass = prefClass;

        /** Use the full width of the parent element */
        setPreferredSize(new Dimension(0, preferredHeight));
        /** Override any minimum sizes of child elements so the user can resize freely */
        setMinimumSize(new Dimension(0,0));
        this.preferredHeight = preferredHeight;
        toggleAction = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, shortcut);
        String helpId = "Dialog/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
        toggleAction.putValue("help", helpId.substring(0, helpId.length()-6));

        isShowing = Main.pref.getBoolean(preferencePrefix+".visible", defShow);
        isDocked = Main.pref.getBoolean(preferencePrefix+".docked", true);
        isCollapsed = Main.pref.getBoolean(preferencePrefix+".minimized", false);
        buttonHiding = PROP_BUTTON_HIDING.get();

        /** show the minimize button */
        titleBar = new TitleBar(name, iconName);
        add(titleBar, BorderLayout.NORTH);

        setBorder(BorderFactory.createEtchedBorder());

        Main.redirectToMainContentPane(this);
        Main.pref.addPreferenceChangeListener(this);

        windowMenuItem = MainMenu.addWithCheckbox(Main.main.menu.windowMenu,
                (JosmAction) getToggleAction(),
                MainMenu.WINDOW_MENU_GROUP.TOGGLE_DIALOG);
    }

    /**
     * The action to toggle the visibility state of this toggle dialog.
     *
     * Emits {@link PropertyChangeEvent}s for the property <tt>selected</tt>:
     * <ul>
     *   <li>true, if the dialog is currently visible</li>
     *   <li>false, if the dialog is currently invisible</li>
     * </ul>
     *
     */
    public final class ToggleDialogAction extends JosmAction {

        private ToggleDialogAction(String name, String iconName, String tooltip, Shortcut shortcut) {
            super(name, iconName, tooltip, shortcut, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toggleButtonHook();
            if (getValue("toolbarbutton") instanceof JButton) {
                ((JButton) getValue("toolbarbutton")).setSelected(!isShowing);
            }
            if (isShowing) {
                hideDialog();
                if (dialogsPanel != null) {
                    dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                }
                hideNotify();
            } else {
                showDialog();
                if (isDocked && isCollapsed) {
                    expand();
                }
                if (isDocked && dialogsPanel != null) {
                    dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, ToggleDialog.this);
                }
                showNotify();
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
        windowMenuItem.setState(true);
        toggleAction.putValue("selected", false);
        toggleAction.putValue("selected", true);
    }

    /**
     * Changes the state of the dialog such that the user can see the content.
     * (takes care of the panel reconstruction)
     */
    public void unfurlDialog() {
        if (isDialogInDefaultView())
            return;
        if (isDialogInCollapsedView()) {
            expand();
            dialogsPanel.reconstruct(Action.COLLAPSED_TO_DEFAULT, this);
        } else if (!isDialogShowing()) {
            showDialog();
            if (isDocked && isCollapsed) {
                expand();
            }
            if (isDocked) {
                dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, this);
            }
            showNotify();
        }
    }

    @Override
    public void buttonHidden() {
        if ((Boolean) toggleAction.getValue("selected")) {
            toggleAction.actionPerformed(null);
        }
    }

    @Override
    public void buttonShown() {
        unfurlDialog();
    }


    /**
     * Hides the dialog
     */
    public void hideDialog() {
        closeDetachedDialog();
        this.setVisible(false);
        windowMenuItem.setState(false);
        setIsShowing(false);
        toggleAction.putValue("selected", false);
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
        setIsShowing(true);
        setIsDocked(false);
    }

    /**
     * Collapses the toggle dialog to the title bar only
     *
     */
    public void collapse() {
        if (isDialogInDefaultView()) {
            setContentVisible(false);
            setIsCollapsed(true);
            setPreferredSize(new Dimension(0,20));
            setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
            setMinimumSize(new Dimension(Integer.MAX_VALUE,20));
            titleBar.lblMinimized.setIcon(ImageProvider.get("misc", "minimized"));
        }
        else throw new IllegalStateException();
    }

    /**
     * Expands the toggle dialog
     */
    protected void expand() {
        if (isDialogInCollapsedView()) {
            setContentVisible(true);
            setIsCollapsed(false);
            setPreferredSize(new Dimension(0,preferredHeight));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            titleBar.lblMinimized.setIcon(ImageProvider.get("misc", "normal"));
        }
        else throw new IllegalStateException();
    }

    /**
     * Sets the visibility of all components in this toggle dialog, except the title bar
     *
     * @param visible true, if the components should be visible; false otherwise
     */
    protected void setContentVisible(boolean visible) {
        Component[] comps = getComponents();
        for (Component comp : comps) {
            if (comp != titleBar && (!visible || comp != buttonsPanel || buttonHiding != ButtonHidingType.ALWAYS_HIDDEN)) {
                comp.setVisible(visible);
            }
        }
    }

    @Override
    public void destroy() {
        closeDetachedDialog();
        hideNotify();
        Main.main.menu.windowMenu.remove(windowMenuItem);
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        Main.pref.removePreferenceChangeListener(this);
        destroyComponents(this, false);
    }

    private void destroyComponents(Component component, boolean destroyItself) {
        if (component instanceof Container) {
            for (Component c: ((Container)component).getComponents()) {
                destroyComponents(c, true);
            }
        }
        if (destroyItself && component instanceof Destroyable) {
            ((Destroyable) component).destroy();
        }
    }

    /**
     * Closes the detached dialog if this toggle dialog is currently displayed
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
    protected class TitleBar extends JPanel {
        /** the label which shows whether the toggle dialog is expanded or collapsed */
        private final JLabel lblMinimized;
        /** the label which displays the dialog's title **/
        private final JLabel lblTitle;
        private final JComponent lblTitle_weak;
        private final DialogPopupMenu popupMenu = new DialogPopupMenu();
        /** the button which shows whether buttons are dynamic or not */
        private final JButton buttonsHide;

        public TitleBar(String toggleDialogName, String iconName) {
            setLayout(new GridBagLayout());

            lblMinimized = new JLabel(ImageProvider.get("misc", "normal"));
            add(lblMinimized);

            // scale down the dialog icon
            lblTitle = new JLabel("", new ImageProvider("dialogs", iconName).setWidth(16).get(), JLabel.TRAILING);
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

            buttonsHide = new JButton(ImageProvider.get("misc", buttonHiding != ButtonHidingType.ALWAYS_SHOWN ? "buttonhide" : "buttonshow"));
            buttonsHide.setToolTipText(tr("Toggle dynamic buttons"));
            buttonsHide.setBorder(BorderFactory.createEmptyBorder());
            buttonsHide.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JRadioButtonMenuItem item = (buttonHiding == ButtonHidingType.DYNAMIC) ? popupMenu.alwaysShown : popupMenu.dynamic;
                            item.setSelected(true);
                            item.getAction().actionPerformed(null);
                        }
                    }
                    );
            add(buttonsHide);

            // show the pref button if applicable
            if (preferenceClass != null) {
                JButton pref = new JButton(new ImageProvider("preference").setWidth(16).get());
                pref.setToolTipText(tr("Open preferences for this panel"));
                pref.setBorder(BorderFactory.createEmptyBorder());
                pref.addActionListener(
                        new ActionListener(){
                            @Override
                            @SuppressWarnings("unchecked")
                            public void actionPerformed(ActionEvent e) {
                                final PreferenceDialog p = new PreferenceDialog(Main.parent);
                                if (TabPreferenceSetting.class.isAssignableFrom(preferenceClass)) {
                                    p.selectPreferencesTabByClass((Class<? extends TabPreferenceSetting>) preferenceClass);
                                } else if (SubPreferenceSetting.class.isAssignableFrom(preferenceClass)) {
                                    p.selectSubPreferencesTabByClass((Class<? extends SubPreferenceSetting>) preferenceClass);
                                }
                                p.setVisible(true);
                            }
                        }
                        );
                add(pref);
            }

            // show the sticky button
            JButton sticky = new JButton(ImageProvider.get("misc", "sticky"));
            sticky.setToolTipText(tr("Undock the panel"));
            sticky.setBorder(BorderFactory.createEmptyBorder());
            sticky.addActionListener(
                    new ActionListener(){
                        @Override
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
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            hideDialog();
                            dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                            hideNotify();
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

        public class DialogPopupMenu extends JPopupMenu {
            private final ButtonGroup buttonHidingGroup = new ButtonGroup();
            private final JMenu buttonHidingMenu = new JMenu(tr("Side buttons"));

            public final JRadioButtonMenuItem alwaysShown  = new JRadioButtonMenuItem(new AbstractAction(tr("Always shown")) {
                @Override public void actionPerformed(ActionEvent e) {
                    setIsButtonHiding(ButtonHidingType.ALWAYS_SHOWN);
                }
            });

            public final JRadioButtonMenuItem dynamic      = new JRadioButtonMenuItem(new AbstractAction(tr("Dynamic")) {
                @Override public void actionPerformed(ActionEvent e) {
                    setIsButtonHiding(ButtonHidingType.DYNAMIC);
                }
            });

            public final JRadioButtonMenuItem alwaysHidden = new JRadioButtonMenuItem(new AbstractAction(tr("Always hidden")) {
                @Override public void actionPerformed(ActionEvent e) {
                    setIsButtonHiding(ButtonHidingType.ALWAYS_HIDDEN);
                }
            });

            public DialogPopupMenu() {
                alwaysShown.setSelected(buttonHiding == ButtonHidingType.ALWAYS_SHOWN);
                dynamic.setSelected(buttonHiding == ButtonHidingType.DYNAMIC);
                alwaysHidden.setSelected(buttonHiding == ButtonHidingType.ALWAYS_HIDDEN);
                for (JRadioButtonMenuItem rb : new JRadioButtonMenuItem[]{alwaysShown, dynamic, alwaysHidden}) {
                    buttonHidingGroup.add(rb);
                    buttonHidingMenu.add(rb);
                }
                add(buttonHidingMenu);
                for (javax.swing.Action action: buttonActions) {
                    add(action);
                }
            }
        }

        public final void registerMouseListener() {
            addMouseListener(new MouseEventHandler());
        }

        class MouseEventHandler extends PopupMenuLauncher {
            public MouseEventHandler() {
                super(popupMenu);
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (isCollapsed) {
                        expand();
                        dialogsPanel.reconstruct(Action.COLLAPSED_TO_DEFAULT, ToggleDialog.this);
                    } else {
                        collapse();
                        dialogsPanel.reconstruct(Action.ELEMENT_SHRINKS, null);
                    }
                }
            }
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
                    if (dockWhenClosingDetachedDlg()) {
                        dock();
                        if (isDialogInCollapsedView()) {
                            expand();
                        }
                        dialogsPanel.reconstruct(Action.INVISIBLE_TO_DEFAULT, ToggleDialog.this);
                    } else {
                        hideDialog();
                        hideNotify();
                    }
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

            try {
                new WindowGeometry(preferencePrefix+".geometry").applySafe(this);
            } catch (WindowGeometryException e) {
                ToggleDialog.this.setPreferredSize(ToggleDialog.this.getDefaultDetachedSize());
                pack();
                setLocationRelativeTo(Main.parent);
            }
            setTitle(titleBar.getTitle());
            HelpUtil.setHelpContext(getRootPane(), helpTopic());
        }

        protected void rememberGeometry() {
            if (detachedDialog != null) {
                new WindowGeometry(detachedDialog).remember(preferencePrefix+".geometry");
            }
        }
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
     * Sets the title.
     * @param title The dialog's title
     */
    public void setTitle(String title) {
        titleBar.setTitle(title);
        if (detachedDialog != null) {
            detachedDialog.setTitle(title);
        }
    }

    protected void setIsShowing(boolean val) {
        isShowing = val;
        Main.pref.put(preferencePrefix+".visible", val);
        stateChanged();
    }

    protected void setIsDocked(boolean val) {
        if (buttonsPanel != null) {
            buttonsPanel.setVisible(val ? buttonHiding != ButtonHidingType.ALWAYS_HIDDEN : true);
        }
        isDocked = val;
        Main.pref.put(preferencePrefix+".docked", val);
        stateChanged();
    }

    protected void setIsCollapsed(boolean val) {
        isCollapsed = val;
        Main.pref.put(preferencePrefix+".minimized", val);
        stateChanged();
    }

    protected void setIsButtonHiding(ButtonHidingType val) {
        buttonHiding = val;
        PROP_BUTTON_HIDING.put(val);
        refreshHidingButtons();
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    @Override
    public String helpTopic() {
        String help = getClass().getName();
        help = help.substring(help.lastIndexOf('.')+1, help.length()-6);
        return "Dialog/"+help;
    }

    @Override
    public String toString() {
        return name;
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

    public void setButton(JToggleButton button) {
        this.button = button;
    }

    public JToggleButton getButton() {
        return button;
    }

    /***
     * The following methods are intended to be overridden, in order to customize
     * the toggle dialog behavior.
     **/

    /**
     * Change the Geometry of the detached dialog to better fit the content.
     */
    protected Rectangle getDetachedGeometry(Rectangle last) {
        return last;
    }

    /**
     * Default size of the detached dialog.
     * Override this method to customize the initial dialog size.
     */
    protected Dimension getDefaultDetachedSize() {
        return new Dimension(dialogsPanel.getWidth(), preferredHeight);
    }

    /**
     * Do something when the toggleButton is pressed.
     */
    protected void toggleButtonHook() {
    }

    protected boolean dockWhenClosingDetachedDlg() {
        return true;
    }

    /**
     * primitive stateChangedListener for subclasses
     */
    protected void stateChanged() {
    }

    protected Component createLayout(Component data, boolean scroll, Collection<SideButton> buttons) {
        return createLayout(data, scroll, buttons, (Collection<SideButton>[]) null);
    }

    protected Component createLayout(Component data, boolean scroll, Collection<SideButton> firstButtons, Collection<SideButton>... nextButtons) {
        if (scroll) {
            data = new JScrollPane(data);
        }
        LinkedList<Collection<SideButton>> buttons = new LinkedList<Collection<SideButton>>();
        buttons.addFirst(firstButtons);
        if (nextButtons != null) {
            buttons.addAll(Arrays.asList(nextButtons));
        }
        add(data, BorderLayout.CENTER);
        if (!buttons.isEmpty() && buttons.get(0) != null && !buttons.get(0).isEmpty()) {
            buttonsPanel = new JPanel(new GridLayout(buttons.size(), 1));
            for (Collection<SideButton> buttonRow : buttons) {
                if (buttonRow == null) {
                    continue;
                }
                final JPanel buttonRowPanel = new JPanel(Main.pref.getBoolean("dialog.align.left", false)
                        ? new FlowLayout(FlowLayout.LEFT) : new GridLayout(1, buttonRow.size()));
                buttonsPanel.add(buttonRowPanel);
                for (SideButton button : buttonRow) {
                    buttonRowPanel.add(button);
                    javax.swing.Action action = button.getAction();
                    if (action != null) {
                        buttonActions.add(action);
                    } else {
                        Main.warn("Button " + button + " doesn't have action defined");
                        Main.error(new Exception());
                    }
                }
            }
            add(buttonsPanel, BorderLayout.SOUTH);
            dynamicButtonsPropertyChanged();
        } else {
            titleBar.buttonsHide.setVisible(false);
        }

        // Register title bar mouse listener only after buttonActions has been initialized to have a complete popup menu
        titleBar.registerMouseListener();

        return data;
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if(isShowing() && !isCollapsed && isDocked && buttonHiding == ButtonHidingType.DYNAMIC) {
            if (buttonsPanel != null) {
                Rectangle b = this.getBounds();
                b.setLocation(getLocationOnScreen());
                if (b.contains(((MouseEvent)event).getLocationOnScreen())) {
                    if(!buttonsPanel.isVisible()) {
                        buttonsPanel.setVisible(true);
                    }
                } else if (buttonsPanel.isVisible()) {
                    buttonsPanel.setVisible(false);
                }
            }
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey().equals(PROP_DYNAMIC_BUTTONS.getKey())) {
            dynamicButtonsPropertyChanged();
        }
    }

    private void dynamicButtonsPropertyChanged() {
        boolean propEnabled = PROP_DYNAMIC_BUTTONS.get();
        if (propEnabled) {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_MOTION_EVENT_MASK);
        } else {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        }
        titleBar.buttonsHide.setVisible(propEnabled);
        refreshHidingButtons();
    }

    private void refreshHidingButtons() {
        titleBar.buttonsHide.setIcon(ImageProvider.get("misc", buttonHiding != ButtonHidingType.ALWAYS_SHOWN ? "buttonhide" : "buttonshow"));
        titleBar.buttonsHide.setEnabled(buttonHiding != ButtonHidingType.ALWAYS_HIDDEN);
        if (buttonsPanel != null) {
            buttonsPanel.setVisible(buttonHiding != ButtonHidingType.ALWAYS_HIDDEN || !isDocked);
        }
        stateChanged();
    }
}
