// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * A panel displaying the list of known plugins.
 */
public class PluginListPanel extends VerticallyScrollablePanel {
    static final class PluginCheckBoxMouseAdapter extends MouseAdapter {
        private final PluginCheckBox cbPlugin;

        PluginCheckBoxMouseAdapter(PluginCheckBox cbPlugin) {
            this.cbPlugin = cbPlugin;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            cbPlugin.doClick();
        }
    }

    private final transient PluginPreferencesModel model;

    /** Whether the plugin list has been built up already in the UI. */
    private boolean pluginListInitialized;

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

    protected static String formatPluginRemoteVersion(PluginInformation pi) {
        StringBuilder sb = new StringBuilder();
        if (pi.version == null || pi.version.trim().isEmpty()) {
            sb.append(tr("unknown"));
        } else {
            sb.append(pi.version);
            if (pi.oldmode) {
                sb.append('*');
            }
        }
        return sb.toString();
    }

    protected static String formatPluginLocalVersion(PluginInformation pi) {
        if (pi == null)
            return tr("unknown");
        if (pi.localversion == null || pi.localversion.trim().isEmpty())
            return tr("unknown");
        return pi.localversion;
    }

    protected static String formatCheckboxTooltipText(PluginInformation pi) {
        if (pi == null)
            return "";
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
        gbc.insets = new Insets(40, 0, 40, 0);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        HtmlPanel hint = new HtmlPanel();
        hint.setText(
                "<html>"
                + (model.getAvailablePlugins().isEmpty() ?
                        tr("Please click on <strong>Download list</strong> to download and display a list of available plugins.") :
                        tr("The filter returned no results."))
                + "</html>"
        );
        hint.putClientProperty("plugin", "empty");
        hint.setVisible(false);
        add(hint, gbc);
    }

    /**
     * Displays a list of plugins.
     * @param displayedPlugins list of plugins
     * @since 13799
     */
    public void displayPluginList(List<PluginInformation> displayedPlugins) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = -1;
        for (final PluginInformation pi : displayedPlugins) {
            boolean selected = model.isSelectedPlugin(pi.getName());
            String remoteversion = formatPluginRemoteVersion(pi);
            String localversion = formatPluginLocalVersion(model.getPluginInformation(pi.getName()));

            final PluginCheckBox cbPlugin = new PluginCheckBox(pi, selected, this, model);
            String pluginText = tr("{0}: Version {1} (local: {2})", pi.getName(), remoteversion, localversion);
            if (pi.requires != null && !pi.requires.isEmpty()) {
                pluginText += tr(" (requires: {0})", pi.requires);
            }
            JLabel lblPlugin = new JLabel(
                    pluginText,
                    pi.getScaledIcon(),
                    SwingConstants.LEADING);
            lblPlugin.addMouseListener(new PluginCheckBoxMouseAdapter(cbPlugin));

            gbc.gridx = 0;
            gbc.gridy = ++row;
            gbc.insets = new Insets(5, 5, 0, 5);
            gbc.weighty = 0.0;
            gbc.weightx = 0.0;
            cbPlugin.putClientProperty("plugin", pi);
            add(cbPlugin, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            lblPlugin.putClientProperty("plugin", pi);
            add(lblPlugin, gbc);

            HtmlPanel description = new HtmlPanel();
            description.setText(pi.getDescriptionAsHtml());
            description.enableClickableHyperlinks();
            lblPlugin.setLabelFor(description);

            gbc.gridx = 1;
            gbc.gridy = ++row;
            gbc.insets = new Insets(3, 25, 5, 5);
            gbc.weighty = 1.0;
            description.putClientProperty("plugin", pi);
            add(description, gbc);
        }
        pluginListInitialized = true;
    }

    /**
     * Refreshes the list.
     *
     * If the list has been changed completely (i.e. not just filtered),
     * call {@link #resetDisplayedComponents()} prior to calling this method.
     */
    public void refreshView() {
        final Rectangle visibleRect = getVisibleRect();
        if (!pluginListInitialized) {
            removeAll();
            displayEmptyPluginListInformation();
            displayPluginList(model.getAvailablePlugins());
        } else {
            hidePluginsNotInList(new HashSet<>(model.getDisplayedPlugins()));
        }
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> scrollRectToVisible(visibleRect));
    }

    /**
     * Hides components in the list for plugins that are currently filtered away.
     *
     * Since those components are relatively heavyweight rebuilding them every time
     * when the filter changes is fairly slow, so we build them once and just hide
     * those that shouldn't be visible.
     *
     * @param displayedPlugins A set of plugins that are currently visible.
     */
    private void hidePluginsNotInList(Set<PluginInformation> displayedPlugins) {
        synchronized (getTreeLock()) {
            for (int i = 0; i < getComponentCount(); i++) {
                JComponent component = (JComponent) getComponent(i);
                Object plugin = component.getClientProperty("plugin");
                if ("empty".equals(plugin)) {
                    // Hide the empty plugin list warning if it's there
                    component.setVisible(displayedPlugins.isEmpty());
                } else {
                    component.setVisible(displayedPlugins.contains(plugin));
                }
            }
        }
    }

    /**
     * Causes the components for the list items to be rebuilt from scratch.
     *
     * Should be called before calling {@link #refreshView()} whenever the
     * underlying list changes to display a completely different set of
     * plugins instead of merely hiding plugins by a filter.
     */
    public void resetDisplayedComponents() {
        pluginListInitialized = false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height / 4;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }
}
