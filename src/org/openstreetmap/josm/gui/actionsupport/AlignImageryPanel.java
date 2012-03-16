package org.openstreetmap.josm.gui.actionsupport;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.JSplitPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.openstreetmap.josm.Main;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;


/**
 * The panel to nag a user ONCE that he/she has to align imagery.
 * 
 * @author zverik
 */
public class AlignImageryPanel extends JPanel {
    private static final String PREF = "imagery.offsetnagging";

    public AlignImageryPanel() {
        super();
        
        Font font = getFont().deriveFont(Font.PLAIN, 14.0f);
        JLabel nagLabel = new JLabel(tr("Aerial imagery might be misaligned. Please check its offset using GPS tracks!"));
        UrlLabel detailsList = new UrlLabel(tr("http://wiki.openstreetmap.org/wiki/Using_Imagery"), tr("Details..."));
        nagLabel.setFont(font);
        detailsList.setFont(font);
        
        JButton closeButton = new JButton(ImageProvider.get("misc", "black_x"));
        closeButton.setContentAreaFilled(false);
        closeButton.setRolloverEnabled(true);
        closeButton.setBorderPainted(false);
        closeButton.setToolTipText(tr("Hide this message and never show it again"));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                if (Main.map!=null) {
                    Main.map.removeTopPanel(AlignImageryPanel.class);
                    Main.pref.put(PREF, false);
                }
            }
        });
        
        BoxLayout box = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(box);
        add(nagLabel);
        add(Box.createHorizontalStrut(12));
        add(detailsList);
        add(Box.createHorizontalGlue());
        add(closeButton);
//        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(12, 12, 12, 12)));
        setBackground(new Color(224, 236, 249));
    }

    public static void addNagPanelIfNeeded() {
        if( Main.map != null && !Main.pref.getBoolean("expert") && Main.pref.getBoolean(PREF, true) ) {
            if (Main.map.getTopPanel(AlignImageryPanel.class) == null) {
                AlignImageryPanel p = new AlignImageryPanel();
                Main.map.addTopPanel(p);
            }
        }
    }
    
}
