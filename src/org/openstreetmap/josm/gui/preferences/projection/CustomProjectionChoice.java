// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

public class CustomProjectionChoice extends AbstractProjectionChoice implements SubPrefsOptions {

    private String pref;

    /**
     * Constructs a new {@code CustomProjectionChoice}.
     */
    public CustomProjectionChoice() {
        super(tr("Custom Projection"), "core:custom");
    }

    private static class PreferencePanel extends JPanel {

        public JosmTextField input;
        private HistoryComboBox cbInput;

        public PreferencePanel(String initialText, ActionListener listener) {
            build(initialText, listener);
        }

        private void build(String initialText, final ActionListener listener) {
            input = new JosmTextField(30);
            cbInput = new HistoryComboBox();
            cbInput.setPrototypeDisplayValue(new AutoCompletionListItem("xxxx"));
            cbInput.setEditor(new BasicComboBoxEditor() {
                @Override
                protected JosmTextField createEditorComponent() {
                    return input;
                }
            });
            Collection<String> samples = Arrays.asList(
                    "+proj=lonlat +ellps=WGS84 +datum=WGS84 +bounds=-180,-90,180,90",
                    "+proj=tmerc +lat_0=0 +lon_0=9 +k_0=1 +x_0=3500000 +y_0=0 +ellps=bessel +nadgrids=BETA2007.gsb");
            List<String> inputHistory = new LinkedList<String>(Main.pref.getCollection("projection.custom.value.history", samples));
            Collections.reverse(inputHistory);
            cbInput.setPossibleItems(inputHistory);
            cbInput.setText(initialText == null ? "" : initialText);

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
                    listener.actionPerformed(null);
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
                    CustomProjectionChoice.ParameterInfoDialog dlg = new CustomProjectionChoice.ParameterInfoDialog();
                    dlg.showDialog();
                    dlg.toFront();
                }
            });

            this.setLayout(new GridBagLayout());
            JPanel p2 = new JPanel(new GridBagLayout());
            p2.add(cbInput, GBC.std().fill(GBC.HORIZONTAL).insets(0, 20, 5, 5));
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

        public void rememberHistory() {
            cbInput.addCurrentItemToHistory();
            Main.pref.putCollection("projection.custom.value.history", cbInput.getHistory());
        }
    }

    public static class ParameterInfoDialog extends ExtendedDialog {

        /**
         * Constructs a new {@code ParameterInfoDialog}.
         */
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
            s.append("&nbsp;&nbsp;&nbsp;&nbsp;"+tr("Built-in:")+" ");
            s.append(listKeys(Projections.nadgrids)+"<br>");
            s.append("<b>+bounds=</b>minlon,minlat,maxlon,maxlat - <i>"+tr("Projection bounds (in degrees)")+"</i><br>");

            return new HtmlPanel(s.toString());
        }

        private String listKeys(Map<String, ?> map) {
            List<String> keys = new ArrayList<String>(map.keySet());
            Collections.sort(keys);
            return Utils.join(", ", keys);
        }
    }

    @Override
    public void setPreferences(Collection<String> args) {
        if (args != null && !args.isEmpty()) {
            pref = args.iterator().next();
        }
    }

    @Override
    public Projection getProjection() {
        return new CustomProjection(pref);
    }

    @Override
    public String getCurrentCode() {
        // not needed - getProjection() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectionName() {
        // not needed - getProjection() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new PreferencePanel(pref, listener);
    }

    @Override
    public Collection<String> getPreferences(JPanel panel) {
        if (!(panel instanceof PreferencePanel)) {
            throw new IllegalArgumentException();
        }
        PreferencePanel prefPanel = (PreferencePanel) panel;
        String pref = prefPanel.input.getText();
        prefPanel.rememberHistory();
        return Collections.singleton(pref);
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

    @Override
    public boolean showProjectionName() {
        return false;
    }
}
