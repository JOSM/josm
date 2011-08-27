// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class SideButton extends JButton {
    public SideButton(Action action)
    {
        super(action);
        fixIcon();
        doStyle();
    }

    public SideButton(Action action, boolean usename)
    {
        super(action);
        if(!usename)
            setText(null);
        fixIcon();
        doStyle();
    }

    public SideButton(Action action, String imagename)
    {
        super(action);
        setIcon(makeIcon(imagename));
        doStyle();
    }

    void fixIcon() {
        Icon i = getIcon();
        if(i != null && i instanceof ImageIcon)
        {
            Image im = ((ImageIcon) i).getImage();
            setIcon(new ImageIcon(im.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
        }
    }

    public static ImageIcon makeIcon(String imagename) {
        Image im = ImageProvider.get("dialogs", imagename).getImage();
        return new ImageIcon(im.getScaledInstance(20, 20, Image.SCALE_SMOOTH));
    }

    public SideButton(String imagename, String property, String tooltip, ActionListener actionListener)
    {
        super(makeIcon(imagename));
        doStyle();
        setActionCommand(imagename);
        addActionListener(actionListener);
        setToolTipText(tooltip);
    }
    public SideButton(String name, String imagename, String property, String tooltip, Shortcut shortcut, ActionListener actionListener)
    {
        super(tr(name), makeIcon(imagename));
        if(shortcut != null)
        {
            shortcut.setMnemonic(this);
            if(tooltip != null) {
                tooltip = Main.platform.makeTooltip(tooltip, shortcut);
            }
        }
        setup(name, property, tooltip, actionListener);
    }
    public SideButton(String name, String imagename, String property, String tooltip, ActionListener actionListener)
    {
        super(tr(name), makeIcon(imagename));
        setup(name, property, tooltip, actionListener);
    }
    private void setup(String name, String property, String tooltip, ActionListener actionListener)
    {
        doStyle();
        setActionCommand(name);
        addActionListener(actionListener);
        setToolTipText(tooltip);
        putClientProperty("help", "Dialog/"+property+"/"+name);
    }
    private void doStyle()
    {
        setLayout(new BorderLayout());
        setIconTextGap(2);
        setMargin(new Insets(-1,0,-1,0));
    }

    public void createArrow(ActionListener listener) {
        setMargin(new Insets(0,0,0,0));
        BasicArrowButton arrowButton = new BasicArrowButton(SwingConstants.SOUTH, null, null, Color.BLACK, null);
        arrowButton.setBorder(BorderFactory.createEmptyBorder());
        add(arrowButton, BorderLayout.EAST);
        arrowButton.addActionListener(listener);
    }
}
