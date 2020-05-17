// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Utils;

/**
 * A plugin checkbox.
 * @since 10228
 */
public class PluginCheckBox extends JCheckBox implements ActionListener {
    private final transient PluginInformation pi;
    private final PluginListPanel panel;
    private final transient PluginPreferencesModel ppModel;

    PluginCheckBox(PluginInformation pi, boolean selected, PluginListPanel panel, PluginPreferencesModel ppModel) {
        this.pi = pi;
        this.panel = panel;
        this.ppModel = ppModel;
        setSelected(selected);
        setToolTipText(PluginListPanel.formatCheckboxTooltipText(pi));
        addActionListener(this);
    }

    protected void selectRequiredPlugins(PluginInformation info) {
        if (info != null && info.requires != null) {
            for (String s : info.getRequiredPlugins()) {
                if (!ppModel.isSelectedPlugin(s)) {
                    ppModel.setPluginSelected(s, true);
                    selectRequiredPlugins(ppModel.getPluginInformation(s));
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Select/unselect corresponding plugin in the model
        ppModel.setPluginSelected(pi.getName(), isSelected());
        // Does the newly selected plugin require other plugins ?
        if (isSelected() && pi.requires != null) {
            // Select required plugins
            selectRequiredPlugins(pi);
            // Alert user if plugin requirements are not met
            PluginHandler.checkRequiredPluginsPreconditions(panel, ppModel.getAvailablePlugins(), pi, false);
        } else if (!isSelected()) {
            // If the plugin has been unselected, was it required by other plugins still selected ?
            Set<String> otherPlugins = ppModel.getAvailablePlugins().stream()
                    .filter(p -> !p.equals(pi) && p.requires != null && ppModel.isSelectedPlugin(p.getName()))
                    .filter(p -> p.getRequiredPlugins().stream().anyMatch(s -> s.equals(pi.getName()) || s.equals(pi.provides)))
                    .map(PluginInformation::getName)
                    .collect(Collectors.toSet());
            if (!otherPlugins.isEmpty()) {
                alertPluginStillRequired(panel, pi.getName(), otherPlugins);
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
        StringBuilder sb = new StringBuilder("<html>")
          .append(trn("Plugin {0} is still required by this plugin:",
                "Plugin {0} is still required by these {1} plugins:",
                otherPlugins.size(),
                Utils.escapeReservedCharactersHTML(plugin),
                otherPlugins.size()))
          .append(Utils.joinAsHtmlUnorderedList(otherPlugins))
          .append("</html>");
        JOptionPane.showMessageDialog(
                parent,
                sb.toString(),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE
        );
    }
}
