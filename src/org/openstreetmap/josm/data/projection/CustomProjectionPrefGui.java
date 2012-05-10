// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.projection.SubPrefsOptions;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

public class CustomProjectionPrefGui extends CustomProjection implements ProjectionSubPrefs, SubPrefsOptions {

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        JPanel inner = new PreferencePanel();
        p.setLayout(new GridBagLayout());
        p.add(inner, GBC.std().fill(GBC.HORIZONTAL));
    }

    private class PreferencePanel extends JPanel {

        public final JTextField input;

        public PreferencePanel() {
            input = new JTextField(pref == null ? "" : pref, 30);
            build();
        }

        private void build() {
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

            JButton btnInfo = new JButton(tr("Parameter information..."));
            btnInfo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ParameterInfoDialog dlg = new ParameterInfoDialog();
                    dlg.showDialog();
                    dlg.toFront();
                }
            });

            this.setLayout(new GridBagLayout());
            JPanel p2 = new JPanel(new GridBagLayout());
            p2.add(input, GBC.std().fill(GBC.HORIZONTAL).insets(0, 20, 5, 5));
            p2.add(btnCheck, GBC.eol().insets(0, 20, 0, 5));
            this.add(p2, GBC.eol().fill(GBC.HORIZONTAL));
            p2 = new JPanel(new GridBagLayout());
            p2.add(valStatus, GBC.std().anchor(GBC.WEST).weight(0.0001, 0));
            p2.add(errorsPanel, GBC.eol().fill(GBC.HORIZONTAL));
            this.add(p2, GBC.eol().fill(GBC.HORIZONTAL));
            p2 = new JPanel(new GridBagLayout());
            p2.add(btnInfo, GBC.std().insets(0, 20, 0, 0));
            p2.add(GBC.glue(1, 0), GBC.eol().fill(GBC.HORIZONTAL));
            this.add(p2, GBC.eol().fill(GBC.HORIZONTAL));
        }
    }

    public class ParameterInfoDialog extends ExtendedDialog {

        public ParameterInfoDialog() {
            super(null, tr("Parameter information"), new String[] { tr("Close") }, false);
            setContent(build());
        }

        private JComponent build() {
            StringBuilder s = new StringBuilder();
            s.append("<b>+proj=...</b> - <i>"+tr("Projection name")+"</i><br>");
            s.append("&nbsp;&nbsp;&nbsp;&nbsp;"+tr("Supported values:")+" ");
            s.append(listKeys(Projections.projs)+"<br>");
            s.append("<b>+lat_0=..., +lat_1=..., +lat_2=...</b> - <i>"+tr("Projection parameters")+"</i><br>");
            s.append("<b>+x_0=..., +y_0=...</b> - <i>"+tr("False easting and false northing")+"</i><br>");
            s.append("<b>+lon_0=...</b> - <i>"+tr("Central meridian")+"</i><br>");
            s.append("<b>+k_0=...</b> - <i>"+tr("Scaling factor")+"</i><br>");
            s.append("<b>+ellps=...</b> - <i>"+tr("Ellipsoid name")+"</i><br>");
            s.append("&nbsp;&nbsp;&nbsp;&nbsp;"+tr("Supported values:")+" ");
            s.append(listKeys(Projections.ellipsoids)+"<br>");
            s.append("<b>+a=..., +b=..., +rf=..., +f=..., +es=...</b> - <i>"+tr("Ellipsoid parameters")+"</i><br>");
            s.append("<b>+datum=...</b> - <i>"+tr("Datum name")+"</i><br>");
            s.append("&nbsp;&nbsp;&nbsp;&nbsp;"+tr("Supported values:")+" ");
            s.append(listKeys(Projections.datums)+"<br>");
            s.append("<b>+towgs84=...</b> - <i>"+tr("3 or 7 term datum transform parameters")+"</i><br>");
            s.append("<b>+nadgrids=...</b> - <i>"+tr("NTv2 grid file")+"</i><br>");
            s.append("&nbsp;&nbsp;&nbsp;&nbsp;"+tr("Build-in:")+" ");
            s.append(listKeys(Projections.nadgrids)+"<br>");

            HtmlPanel info = new HtmlPanel(s.toString());
            return info;
        }

        private String listKeys(Map<String, ?> map) {
            List<String> keys = new ArrayList<String>(map.keySet());
            Collections.sort(keys);
            return Utils.join(", ", keys);
        }
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        PreferencePanel prefPanel = (PreferencePanel) p.getComponent(0);
        String pref = prefPanel.input.getText();
        return Collections.singleton(pref);
    }

    @Override
    public void setPreferences(Collection<String> args) {
        try {
            if (args == null || args.isEmpty()) throw new ProjectionConfigurationException();
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
