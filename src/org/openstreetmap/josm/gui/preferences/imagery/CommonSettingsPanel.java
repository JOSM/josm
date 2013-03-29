// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;

/**
 * {@code JPanel} giving access to common imagery settings.
 * @since 5465
 */
public class CommonSettingsPanel extends JPanel {

    // Common Settings
    private final JButton btnFadeColor;
    private final JSlider fadeAmount = new JSlider(0, 100);
    private final JosmComboBox sharpen;

    /**
     * Constructs a new {@code CommonSettingsPanel}.
     */
    public CommonSettingsPanel() {
        super(new GridBagLayout());
        
        this.btnFadeColor = new JButton();

        this.btnFadeColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JColorChooser chooser = new JColorChooser(btnFadeColor.getBackground());
                int answer = JOptionPane.showConfirmDialog(
                        CommonSettingsPanel.this, chooser,
                        tr("Choose a color for {0}", tr("imagery fade")),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    Color colFadeColor = chooser.getColor();
                    btnFadeColor.setBackground(colFadeColor);
                    btnFadeColor.setText(ColorHelper.color2html(colFadeColor));
                }
            }
        });

        add(new JLabel(tr("Fade Color: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        add(this.btnFadeColor, GBC.eol().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("Fade amount: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        add(this.fadeAmount, GBC.eol().fill(GBC.HORIZONTAL));

        this.sharpen = new JosmComboBox(new String[] {
                tr("None"),
                tr("Soft"),
                tr("Strong")});
        add(new JLabel(tr("Sharpen (requires layer re-add): ")));
        add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        add(this.sharpen, GBC.eol().fill(GBC.HORIZONTAL));
    }
    
    /**
     * Loads the common settings.
     */
    public void loadSettings() {
        Color colFadeColor = ImageryLayer.PROP_FADE_COLOR.get();
        this.btnFadeColor.setBackground(colFadeColor);
        this.btnFadeColor.setText(ColorHelper.color2html(colFadeColor));
        this.fadeAmount.setValue(ImageryLayer.PROP_FADE_AMOUNT.get());
        this.sharpen.setSelectedIndex(Math.max(0, Math.min(2, ImageryLayer.PROP_SHARPEN_LEVEL.get())));
    }
    
    /**
     * Saves the common settings.
     * @return true when restart is required
     */
    public boolean saveSettings() {
        ImageryLayer.PROP_FADE_AMOUNT.put(this.fadeAmount.getValue());
        ImageryLayer.PROP_FADE_COLOR.put(this.btnFadeColor.getBackground());
        ImageryLayer.PROP_SHARPEN_LEVEL.put(sharpen.getSelectedIndex());
        return false;
    }
}
