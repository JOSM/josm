// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.gui.preferences.projection.SubPrefsOptions;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class CustomProjectionPrefGui extends CustomProjection implements ProjectionSubPrefs, SubPrefsOptions {

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        final JTextField input = new JTextField(pref == null ? "" : pref, 30);
        final HtmlPanel errorsPanel = new HtmlPanel();
        errorsPanel.setVisible(false);
        final JLabel valStatus = new JLabel();
        valStatus.setVisible(false);

        final AbstractTextComponentValidator val = new AbstractTextComponentValidator(input, false, false, false) {

            private String error;

            @Override
            public void validate() {
                if (!isValid()) {
                    feedbackInvalid(tr("Invalid projection configuration: {0}",error));
                } else {
                    feedbackValid(tr("Projection configuration is valid."));
                }
            }

            @Override
            public boolean isValid() {
                try {
                    CustomProjection test = new CustomProjection();
                    test.update(input.getText());
                } catch (ProjectionConfigurationException ex) {
                    error = ex.getMessage();
                    valStatus.setIcon(ImageProvider.get("data", "error.png"));
                    valStatus.setVisible(true);
                    errorsPanel.setText(error);
                    errorsPanel.setVisible(true);
                    return false;
                }
                errorsPanel.setVisible(false);
                valStatus.setIcon(ImageProvider.get("misc", "green_check.png"));
                valStatus.setVisible(true);
                return true;
            }

        };

        JButton btnCheck = new JButton(tr("Validate"));
        btnCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                val.validate();
            }
        });
        btnCheck.setLayout(new BorderLayout());
        btnCheck.setMargin(new Insets(-1,0,-1,0));

        p.setLayout(new GridBagLayout());
        p.add(input, GBC.std().fill(GBC.HORIZONTAL).insets(0, 0, 5, 5));
        p.add(btnCheck, GBC.eol());
        JPanel p2 = new JPanel(new GridBagLayout());
        p2.add(valStatus, GBC.std().anchor(GBC.WEST).weight(0.0001, 0));
        p2.add(errorsPanel, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(p2, GBC.eol().fill(GBC.HORIZONTAL));
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        Object prefTf = p.getComponent(0);
        if (!(prefTf instanceof JTextField))
            return null;
        String pref = ((JTextField) prefTf).getText();
        return Collections.singleton(pref);
    }

    @Override
    public void setPreferences(Collection<String> args) {
        try {
            update(args.iterator().next());
        } catch (ProjectionConfigurationException ex) {
            System.err.println("Error: Parsing of custom projection failed, falling back to Mercator. Error message is: "+ex.getMessage());
            try {
                update(null);
            } catch (ProjectionConfigurationException ex1) {
                throw new RuntimeException(ex1);
            }
        }
    }

    @Override
    public String[] allCodes() {
        return new String[0];
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        return null;
    }

    @Override
    public boolean showProjectionCode() {
        return false;
    }

}
