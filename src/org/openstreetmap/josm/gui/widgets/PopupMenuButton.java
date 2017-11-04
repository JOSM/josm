// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;

/**
 * Button triggering the appearance of a JPopupMenu when activated.
 * @since 12955
 */
public class PopupMenuButton extends JButton implements ActionListener {
    private JPopupMenu menu;

    /**
     * Constructs a new {@code PopupMenuButton}.
     * @see JButton#JButton()
     */
    public PopupMenuButton() {
        super();
        this.initialize();
    }

    /**
     * Constructs a new {@code PopupMenuButton}.
     * @param a the <code>Action</code> used to specify the new button
     * @see JButton#JButton(Action)
     */
    public PopupMenuButton(Action a) {
        super(a);
        this.initialize();
    }

    /**
     * Constructs a new {@code PopupMenuButton}.
     * @param i the Icon image to display on the button
     * @see JButton#JButton(Icon)
     */
    public PopupMenuButton(Icon i) {
        super(i);
        this.initialize();
    }

    /**
     * Constructs a new {@code PopupMenuButton}.
     * @param t the text of the button
     * @see JButton#JButton(String)
     */
    public PopupMenuButton(String t) {
        super(t);
        this.initialize();
    }

    /**
     * Constructs a new {@code PopupMenuButton}.
     * @param t the text of the button
     * @param i the Icon image to display on the button
     * @see JButton#JButton(String, Icon)
     */
    public PopupMenuButton(String t, Icon i) {
        super(t, i);
        this.initialize();
    }

    /**
     * Pass-through to {@link JButton#JButton()} allowing associated popup menu to be set
     * @param m the associated popup menu
     */
    public PopupMenuButton(JPopupMenu m) {
        super();
        this.initialize(m);
    }

    /**
     * Pass-through to {@link JButton#JButton(Action)} allowing associated popup menu to be set
     * @param a the <code>Action</code> used to specify the new button
     * @param m the associated popup menu
     */
    public PopupMenuButton(Action a, JPopupMenu m) {
        super(a);
        this.initialize(m);
    }

    /**
     * Pass-through to {@link JButton#JButton(Icon)} allowing associated popup menu to be set
     * @param i the Icon image to display on the button
     * @param m the associated popup menu
     */
    public PopupMenuButton(Icon i, JPopupMenu m) {
        super(i);
        this.initialize(m);
    }

    /**
     * Pass-through to {@link JButton#JButton(String)} allowing associated popup menu to be set
     * @param t the text of the button
     * @param m the associated popup menu
     */
    public PopupMenuButton(String t, JPopupMenu m) {
        super(t);
        this.initialize(m);
    }

    /**
     * Pass-through to {@link JButton#JButton(String, Icon)} allowing associated popup menu to be set
     * @param t the text of the button
     * @param i the Icon image to display on the button
     * @param m the associated popup menu
     */
    public PopupMenuButton(String t, Icon i, JPopupMenu m) {
        super(t, i);
        this.initialize(m);
    }

    private void initialize(JPopupMenu m) {
        this.menu = m;
        this.initialize();
    }

    private void initialize() {
        this.addActionListener(this);
    }

    /**
     * Get the popup menu associated with this button
     * @return the popup menu associated with this button
     */
    public JPopupMenu getPopupMenu() {
        return this.menu;
    }

    /**
     * Set the popup menu associated with this button
     * @param m Menu to show when button is triggered
     */
    public void setPopupMenu(JPopupMenu m) {
        this.menu = m;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.menu.show(this, 0, this.getHeight());
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        //
        // paint small arrow in bottom right corner
        //
        Dimension size = this.getSize();

        Path2D p = new Path2D.Float();
        p.moveTo(size.getWidth() - 7, size.getHeight() - 4);
        p.lineTo(size.getWidth() - 1, size.getHeight() - 4);
        p.lineTo(size.getWidth() - 4, size.getHeight() - 1);
        p.closePath();

        g2d.setPaint(Color.BLACK);
        g2d.fill(p);
    }
}
