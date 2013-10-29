// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.actionsupport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The panel to nag a user ONCE that he/she has to align imagery.
 *
 * @author zverik
 */
public class AlignImageryPanel extends JPanel {
    private static final String PREF = "imagery.offsetnagging";

    public AlignImageryPanel(boolean oneLine) {
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
                if (Main.isDisplayingMapView()) {
                    Main.map.removeTopPanel(AlignImageryPanel.class);
                    Main.pref.put(PREF, false);
                }
            }
        });

        setLayout(new GridBagLayout());
        if (!oneLine) { // tune for small screens
            add(nagLabel, GBC.std(1, 1).fill());
            add(detailsList, GBC.std(1, 2).fill());
            add(closeButton, GBC.std(2, 1).span(1,2).anchor(GBC.EAST));
        } else {
            add(nagLabel, GBC.std(1,1).fill());
            add(detailsList, GBC.std(2,1).fill());
            add(closeButton, GBC.std(3,1).anchor(GBC.EAST));
        }
        setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(12, 12, 12, 12)));
        setBackground(new Color(224, 236, 249));
    }

    public static void addNagPanelIfNeeded() {
        if (Main.isDisplayingMapView() && !Main.pref.getBoolean("expert") && Main.pref.getBoolean(PREF, true) ) {
            if (Main.map.getTopPanel(AlignImageryPanel.class) == null) {
                double w = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
                AlignImageryPanel p = new AlignImageryPanel(w>1300);
                Main.map.addTopPanel(p);
            }
        }
    }
}
