// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.OpenBrowser;

public class PluginListPanel extends VerticallyScrollablePanel{
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PluginListPanel.class.getName());

    private PluginPreferencesModel model;

    public PluginListPanel() {
        model = new PluginPreferencesModel();
        setLayout(new GridBagLayout());
    }

    public PluginListPanel(PluginPreferencesModel model) {
        this.model = model;
        setLayout(new GridBagLayout());
    }

    protected String formatPluginRemoteVersion(PluginInformation pi) {
        StringBuilder sb = new StringBuilder();
        if (pi.version == null || pi.version.trim().equals("")) {
            sb.append(tr("unknown"));
        } else {
            sb.append(pi.version);
            if (pi.oldmode) {
                sb.append("*");
            }
        }
        return sb.toString();
    }

    protected String formatPluginLocalVersion(PluginInformation pi) {
        if (pi == null) return tr("unknown");
        if (pi.localversion == null || pi.localversion.trim().equals(""))
            return tr("unknown");
        return pi.localversion;
    }

    protected String formatCheckboxTooltipText(PluginInformation pi) {
        if (pi == null) return "";
        if (pi.downloadlink == null)
            return tr("Plugin bundled with JOSM");
        else
            return pi.downloadlink;
    }

    public void displayEmptyPluginListInformation() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(40,0,40,0);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        HtmlPanel hint = new HtmlPanel();
        hint.setText(
                "<html>"
                + tr("Please click on <strong>Download list</strong> to download and display a list of available plugins.")
                + "</html>"
        );
        add(hint, gbc);
    }

    public void refreshView() {
        List<PluginInformation> displayedPlugins = model.getDisplayedPlugins();
        removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        if (displayedPlugins.isEmpty()) {
            displayEmptyPluginListInformation();
            return;
        }

        int row = -1;
        for (final PluginInformation pi : displayedPlugins) {
            boolean selected = model.isSelectedPlugin(pi.getName());
            String remoteversion = formatPluginRemoteVersion(pi);
            String localversion = formatPluginLocalVersion(model.getPluginInformation(pi.getName()));

            final JCheckBox cbPlugin = new JCheckBox();
            cbPlugin.setSelected(selected);
            cbPlugin.setToolTipText(formatCheckboxTooltipText(pi));
            cbPlugin.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    model.setPluginSelected(pi.getName(), cbPlugin.isSelected());
                }
            });
            JLabel lblPlugin = new JLabel(
                    tr("{0}: Version {1} (local: {2})", pi.getName(), remoteversion, localversion),
                    pi.getScaledIcon(),
                    SwingConstants.LEFT);

            gbc.gridx = 0;
            gbc.gridy = ++row;
            gbc.insets = new Insets(5,5,0,5);
            gbc.weighty = 0.0;
            gbc.weightx = 0.0;
            add(cbPlugin, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            add(lblPlugin, gbc);

            HtmlPanel description = new HtmlPanel();
            description.setText(pi.getDescriptionAsHtml());
            description.getEditorPane().addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if(e.getEventType() == EventType.ACTIVATED) {
                        OpenBrowser.displayUrl(e.getURL().toString());
                    }
                }
            });

            gbc.gridx = 1;
            gbc.gridy = ++row;
            gbc.insets = new Insets(3,25,5,5);
            gbc.weighty = 1.0;
            add(description, gbc);
        }
        revalidate();
        repaint();
    }
}
