package org.openstreetmap.josm.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;


public class ExtendedDialog extends JDialog {
    private int result = 0;
    private Component parent;
    private final String[] bTexts;

    // For easy access when inherited
    protected Object contentConstraints = GBC.eol().anchor(GBC.CENTER).fill(GBC.HORIZONTAL).insets(5,10,5,0);
    protected ArrayList<JButton> buttons = new ArrayList<JButton>();

    /**
     * Sets up the dialog. The first button is always the default.
     * @param parent The parent element that will be used for position and maximum size
     * @param title The text that will be shown in the window titlebar
     * @param content Any component that should be show above the buttons (e.g. JLabel)
     * @param buttonTexts The labels that will be displayed on the buttons
     * @param buttonIcons The path to the icons that will be displayed on the buttons. Path is relative to JOSM's image directory. File extensions need to be included. If a button should not have an icon pass null.
     */
    public ExtendedDialog(Component parent, String title, Component content, String[] buttonTexts, String[] buttonIcons) {
        super(JOptionPane.getFrameForComponent(parent), title, true /* modal */);
        this.parent = parent;
        bTexts = buttonTexts;
        setupDialog(content, buttonIcons);
        setVisible(true);
    }

    public ExtendedDialog(Component parent, String title, Component content, String[] buttonTexts) {
        this(parent, title, content, buttonTexts, null);
    }

    /**
     * Sets up the dialog and displays the given message in a breakable label
     */
    public ExtendedDialog(Component parent, String title, String message, String[] buttonTexts, String[] buttonIcons) {
        super(JOptionPane.getFrameForComponent(parent), title, true);

        JMultilineLabel lbl = new JMultilineLabel(message);
        // Make it not wider than 2/3 of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        lbl.setMaxWidth(Math.round(screenSize.width*2/3));

        this.parent = parent;
        bTexts = buttonTexts;
        setupDialog(lbl, buttonIcons);
        setVisible(true);
    }

    public ExtendedDialog(Component parent, String title, String message, String[] buttonTexts) {
        this(parent, title, message, buttonTexts, null);
    }

    /**
     * Constructor that doesn't make the dialog visible immediately. Intended for when inheriting.
     */
    public ExtendedDialog(Component parent, String title, String[] buttonTexts, boolean modal) {
        super(JOptionPane.getFrameForComponent(parent), title, modal);
        this.parent = parent;
        bTexts = buttonTexts;
    }

    protected void setupDialog(Component content, String[] buttonIcons) {
        setupEscListener();

        JButton button;
        JPanel buttonsPanel = new JPanel(new GridBagLayout());

        for(int i=0; i < bTexts.length; i++) {
            Action action = new AbstractAction(bTexts[i]) {
                public void actionPerformed(ActionEvent evt) {
                    buttonAction(evt);
                }
            };

            button = new JButton(action);
            if(buttonIcons != null && buttonIcons[i] != null) {
                button.setIcon(ImageProvider.get(buttonIcons[i]));
            }

            if(i == 0) {
                rootPane.setDefaultButton(button);
            }
            buttonsPanel.add(button, GBC.std().insets(2,2,2,2));
            buttons.add(button);
        }

        JPanel cp = new JPanel(new GridBagLayout());
        cp.add(content, contentConstraints);
        cp.add(buttonsPanel, GBC.eol().anchor(GBC.CENTER).insets(5,5,5,5));

        JScrollPane pane = new JScrollPane(cp);
        pane.setBorder(null);
        setContentPane(pane);

        pack();

        // Try to make it not larger than the parent window or at least not larger than 2/3 of the screen
        Dimension d = getSize();
        Dimension x = findMaxDialogSize();

        boolean limitedInWidth = d.width > x.width;
        boolean limitedInHeight = d.height > x.height;

        if(x.width  > 0 && d.width  > x.width) {
            d.width  = x.width;
        }
        if(x.height > 0 && d.height > x.height) {
            d.height = x.height;
        }

        // We have a vertical scrollbar and enough space to prevent a horizontal one
        if(!limitedInWidth && limitedInHeight) {
            d.width += new JScrollBar().getPreferredSize().width;
        }

        setSize(d);
        setLocationRelativeTo(parent);
    }

    /**
     * @return int The selected button. The count starts with 1.
     *             A return value of 0 means the dialog has been closed otherwise.
     */
    public int getValue() {
        return result;
    }

    /**
     * This gets performed whenever a button is clicked or activated
     * @param evt the button event
     */
    protected void buttonAction(ActionEvent evt) {
        String a = evt.getActionCommand();
        for(int i=0; i < bTexts.length; i++)
            if(bTexts[i].equals(a)) {
                result = i+1;
                break;
            }

        setVisible(false);
    }

    /**
     * Tries to find a good value of how large the dialog should be
     * @return Dimension Size of the parent Component or 2/3 of screen size if not available
     */
    protected Dimension findMaxDialogSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension x = new Dimension(Math.round(screenSize.width*2/3),
                Math.round(screenSize.height*2/3));
        try {
            if(parent != null) {
                x = JOptionPane.getFrameForComponent(parent).getSize();
            }
        } catch(NullPointerException e) { }
        return x;
    }

    /**
     * Makes the dialog listen to ESC keypressed
     */
    private void setupEscListener() {
        Action actionListener = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                // 0 means that the dialog has been closed otherwise.
                // We need to set it to zero again, in case the dialog has been re-used
                // and the result differs from its default value
                result = 0;
                setVisible(false);
            }
        };

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", actionListener);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            toFront();
        }
    }
}