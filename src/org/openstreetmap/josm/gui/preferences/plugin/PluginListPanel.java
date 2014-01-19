// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

/**
 * A panel displaying the list of known plugins.
 */
public class PluginListPanel extends VerticallyScrollablePanel {
    private PluginPreferencesModel model;

    /**
     * Constructs a new {@code PluginListPanel} with a default model.
     */
    public PluginListPanel() {
        this(new PluginPreferencesModel());
    }

    /**
     * Constructs a new {@code PluginListPanel} with a given model.
     * @param model The plugin model
     */
    public PluginListPanel(PluginPreferencesModel model) {
        this.model = model;
        setLayout(new GridBagLayout());
    }

    protected String formatPluginRemoteVersion(PluginInformation pi) {
        StringBuilder sb = new StringBuilder();
        if (pi.version == null || pi.version.trim().isEmpty()) {
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
        if (pi.localversion == null || pi.localversion.trim().isEmpty())
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

    /**
     * Displays a message when the plugin list is empty.
     */
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

    /**
     * A plugin checkbox.
     *
     */
    private class JPluginCheckBox extends JCheckBox {
        public final PluginInformation pi;
        public JPluginCheckBox(final PluginInformation pi, boolean selected) {
            this.pi = pi;
            setSelected(selected);
            setToolTipText(formatCheckboxTooltipText(pi));
            addActionListener(new PluginCbActionListener(this));
        }
    }

    /**
     * Listener called when the user selects/unselects a plugin checkbox.
     *
     */
    private class PluginCbActionListener implements ActionListener {
        private final JPluginCheckBox cb;
        public PluginCbActionListener(JPluginCheckBox cb) {
            this.cb = cb;
        }
        protected void selectRequiredPlugins(PluginInformation info) {
            if (info != null && info.requires != null) {
                for (String s : info.getRequiredPlugins()) {
                    if (!model.isSelectedPlugin(s)) {
                        model.setPluginSelected(s, true);
                        selectRequiredPlugins(model.getPluginInformation(s));
                    }
                }
            }
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            // Select/unselect corresponding plugin in the model
            model.setPluginSelected(cb.pi.getName(), cb.isSelected());
            // Does the newly selected plugin require other plugins ?
            if (cb.isSelected() && cb.pi.requires != null) {
                // Select required plugins
                selectRequiredPlugins(cb.pi);
                // Alert user if plugin requirements are not met
                PluginHandler.checkRequiredPluginsPreconditions(PluginListPanel.this, model.getAvailablePlugins(), cb.pi, false);
            }
            // If the plugin has been unselected, was it required by other plugins still selected ?
            else if (!cb.isSelected()) {
                Set<String> otherPlugins = new HashSet<String>();
                for (PluginInformation pi : model.getAvailablePlugins()) {
                    if (!pi.equals(cb.pi) && pi.requires != null && model.isSelectedPlugin(pi.getName())) {
                        for (String s : pi.getRequiredPlugins()) {
                            if (s.equals(cb.pi.getName())) {
                                otherPlugins.add(pi.getName());
                                break;
                            }
                        }
                    }
                }
                if (!otherPlugins.isEmpty()) {
                    alertPluginStillRequired(PluginListPanel.this, cb.pi.getName(), otherPlugins);
                }
            }
        }
    }


    /**
     * Alerts the user if an unselected plugin is still required by another plugins
     *
     * @param parent The parent Component used to display error popup
     * @param plugin the plugin
     * @param otherPlugins the other plugins
     */
    private static void alertPluginStillRequired(Component parent, String plugin, Set<String> otherPlugins) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(trn("Plugin {0} is still required by this plugin:",
                "Plugin {0} is still required by these {1} plugins:",
                otherPlugins.size(),
                plugin,
                otherPlugins.size()
        ));
        sb.append(Utils.joinAsHtmlUnorderedList(otherPlugins));
        sb.append("</html>");
        JOptionPane.showMessageDialog(
                parent,
                sb.toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Refreshes the list.
     */
    public void refreshView() {
        final Rectangle visibleRect = getVisibleRect();
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

            final JPluginCheckBox cbPlugin = new JPluginCheckBox(pi, selected);
            String pluginText = tr("{0}: Version {1} (local: {2})", pi.getName(), remoteversion, localversion);
            if (pi.requires != null && !pi.requires.isEmpty()) {
                pluginText += tr(" (requires: {0})", pi.requires);
            }
            JLabel lblPlugin = new JLabel(
                    pluginText,
                    pi.getScaledIcon(),
                    SwingConstants.LEFT);
            lblPlugin.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    cbPlugin.setSelected(!cbPlugin.isSelected());
                }
            });

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
                @Override
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
        if (visibleRect != null && visibleRect.width > 0 && visibleRect.height > 0) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    scrollRectToVisible(visibleRect);
                }
            });
        }
    }
}
