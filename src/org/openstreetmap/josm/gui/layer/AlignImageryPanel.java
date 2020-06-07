// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.TextUtils;

/**
 * The panel to nag a user ONCE that he/she has to align imagery.
 *
 * @author zverik
 */
public class AlignImageryPanel extends JPanel {

    /**
     * @param oneLine if true, show the nagging message in one line, otherwise - in two lines
     * @param showAgain show again property
     * @param infoToAdd imagery info for which the nagging message is shown
     */
    public AlignImageryPanel(boolean oneLine, final BooleanProperty showAgain, ImageryInfo infoToAdd) {
        Font font = getFont().deriveFont(Font.PLAIN, 14.0f);
        JMultilineLabel nagLabel = new JMultilineLabel(
                tr("Aerial imagery \"{0}\" might be misaligned. Please check its offset using GPS tracks!",
                        TextUtils.wrapLongUrl(infoToAdd.getName())));
        UrlLabel detailsList = new UrlLabel(tr("http://wiki.openstreetmap.org/wiki/Using_Imagery"), tr("Details..."));
        nagLabel.setFont(font);
        nagLabel.setForeground(Color.BLACK);
        detailsList.setFont(font);
        final JCheckBox doNotShowAgain = new JCheckBox(tr("Do not show this message again"));
        doNotShowAgain.setOpaque(false);
        doNotShowAgain.setForeground(Color.BLACK);

        JButton closeButton = new JButton(ImageProvider.get("misc", "black_x"));
        closeButton.setContentAreaFilled(false);
        closeButton.setRolloverEnabled(true);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setToolTipText(tr("Hide this message"));
        closeButton.addActionListener(e -> {
            if (MainApplication.isDisplayingMapView()) {
                MainApplication.getMap().removeTopPanel(AlignImageryPanel.class);
                if (doNotShowAgain.isSelected()) {
                    showAgain.put(Boolean.FALSE);
                }
            }
        });

        setLayout(new GridBagLayout());
        if (!oneLine) { // tune for small screens
            add(nagLabel, GBC.std(1, 1).fill());
            add(detailsList, GBC.std(1, 2).fill());
            add(doNotShowAgain, GBC.std(1, 3).fill());
            add(closeButton, GBC.std(2, 1).span(1, 2).anchor(GBC.EAST));
        } else {
            add(nagLabel, GBC.std(1, 1).fill());
            add(detailsList, GBC.std(2, 1).fill());
            add(doNotShowAgain, GBC.std(1, 2).fill());
            add(closeButton, GBC.std(3, 1).anchor(GBC.EAST));
        }
        setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(12, 12, 12, 12)));
        setBackground(new Color(224, 236, 249));
    }

    /**
     * Adds a nag panel for a given imagery info.
     * @param infoToAdd ImageryInfo for which the nag panel should be created
     */
    public static void addNagPanelIfNeeded(ImageryInfo infoToAdd) {
        BooleanProperty showAgain = new BooleanProperty("message.imagery.nagPanel." + infoToAdd.getUrl(), true);
        MapFrame map = MainApplication.getMap();
        if (MainApplication.isDisplayingMapView() && showAgain.get() && !infoToAdd.isGeoreferenceValid()
                && map.getTopPanel(AlignImageryPanel.class) == null) {
            double w = GuiHelper.getScreenSize().getWidth();
            map.addTopPanel(new AlignImageryPanel(w > 1300, showAgain, infoToAdd));
        }
    }
}
