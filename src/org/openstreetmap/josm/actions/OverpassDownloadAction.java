// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OverpassTurboQueryWizard;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Download map data from Overpass API server.
 * @since 8684
 */
public class OverpassDownloadAction extends JosmAction {

    /**
     * Constructs a new {@code OverpassDownloadAction}.
     */
    public OverpassDownloadAction() {
        super(tr("Download from Overpass API ..."), "download-overpass", tr("Download map data from Overpass API server."),
                // CHECKSTYLE.OFF: LineLength
                Shortcut.registerShortcut("file:download-overpass", tr("File: {0}", tr("Download from Overpass API ...")), KeyEvent.VK_DOWN, Shortcut.ALT_SHIFT),
                // CHECKSTYLE.ON: LineLength
                true, "overpassdownload/download", true);
        putValue("help", ht("/Action/OverpassDownload"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OverpassDownloadDialog dialog = OverpassDownloadDialog.getInstance();
        dialog.restoreSettings();
        dialog.setVisible(true);
        if (!dialog.isCanceled()) {
            dialog.rememberSettings();
            Bounds area = dialog.getSelectedDownloadArea();
            DownloadOsmTask task = new DownloadOsmTask();
            Future<?> future = task.download(
                    new OverpassDownloadReader(area, dialog.getOverpassServer(), dialog.getOverpassQuery()),
                    dialog.isNewLayerRequired(), area, null);
            Main.worker.submit(new PostDownloadHandler(task, future));
        }
    }

    static final class OverpassDownloadDialog extends DownloadDialog {

        protected HistoryComboBox overpassServer;
        protected HistoryComboBox overpassWizard;
        protected JosmTextArea overpassQuery;
        private static OverpassDownloadDialog instance;
        static final StringProperty OVERPASS_SERVER = new StringProperty("download.overpass.server", "http://overpass-api.de/api/");
        static final CollectionProperty OVERPASS_SERVER_HISTORY = new CollectionProperty("download.overpass.servers",
                Arrays.asList("http://overpass-api.de/api/", "http://overpass.osm.rambler.ru/cgi/"));
        static final CollectionProperty OVERPASS_WIZARD_HISTORY = new CollectionProperty("download.overpass.wizard", new ArrayList<String>());

        private OverpassDownloadDialog(Component parent) {
            super(parent, ht("/Action/OverpassDownload"));
            cbDownloadOsmData.setEnabled(false);
            cbDownloadOsmData.setSelected(false);
            cbDownloadGpxData.setVisible(false);
            cbDownloadNotes.setVisible(false);
            cbStartup.setVisible(false);
        }

        public static OverpassDownloadDialog getInstance() {
            if (instance == null) {
                instance = new OverpassDownloadDialog(Main.parent);
            }
            return instance;
        }

        @Override
        protected void buildMainPanelAboveDownloadSelections(JPanel pnl) {

            pnl.add(new JLabel(), GBC.eol()); // needed for the invisible checkboxes cbDownloadGpxData, cbDownloadNotes

            final String tooltip = tr("Builds an Overpass query using the Overpass Turbo query wizard");
            overpassWizard = new HistoryComboBox();
            overpassWizard.setToolTipText(tooltip);
            final JButton buildQuery = new JButton(tr("Build query"));
            buildQuery.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String overpassWizardText = overpassWizard.getText();
                    try {
                        overpassQuery.setText(OverpassTurboQueryWizard.getInstance().constructQuery(overpassWizardText));
                    } catch (OverpassTurboQueryWizard.ParseException ex) {
                        HelpAwareOptionPane.showOptionDialog(
                                Main.parent,
                                tr("<html>The Overpass wizard could not parse the following query:"
                                        + Utils.joinAsHtmlUnorderedList(Collections.singleton(overpassWizardText))),
                                tr("Parse error"),
                                JOptionPane.ERROR_MESSAGE,
                                null
                        );
                    }
                }
            });
            buildQuery.setToolTipText(tooltip);
            pnl.add(buildQuery, GBC.std().insets(5, 5, 5, 5));
            pnl.add(overpassWizard, GBC.eol().fill(GBC.HORIZONTAL));

            overpassQuery = new JosmTextArea("", 8, 80);
            overpassQuery.setFont(GuiHelper.getMonospacedFont(overpassQuery));
            JScrollPane scrollPane = new JScrollPane(overpassQuery);
            final JPanel pane = new JPanel(new BorderLayout());
            final BasicArrowButton arrowButton = new BasicArrowButton(BasicArrowButton.SOUTH);
            arrowButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    OverpassQueryHistoryPopup.show(arrowButton, OverpassDownloadDialog.this);
                }
            });
            pane.add(scrollPane, BorderLayout.CENTER);
            pane.add(arrowButton, BorderLayout.EAST);
            pnl.add(new JLabel(tr("Overpass query: ")), GBC.std().insets(5, 5, 5, 5));
            GBC gbc = GBC.eol().fill(GBC.HORIZONTAL);
            gbc.ipady = 200;
            pnl.add(pane, gbc);

            overpassServer = new HistoryComboBox();
            pnl.add(new JLabel(tr("Overpass server: ")), GBC.std().insets(5, 5, 5, 5));
            pnl.add(overpassServer, GBC.eol().fill(GBC.HORIZONTAL));

        }

        public String getOverpassServer() {
            return overpassServer.getText();
        }

        public String getOverpassQuery() {
            return overpassQuery.getText();
        }

        public void setOverpassQuery(String text) {
            overpassQuery.setText(text);
        }

        @Override
        public void restoreSettings() {
            super.restoreSettings();
            overpassServer.setPossibleItems(OVERPASS_SERVER_HISTORY.get());
            overpassServer.setText(OVERPASS_SERVER.get());
            overpassWizard.setPossibleItems(OVERPASS_WIZARD_HISTORY.get());
        }

        @Override
        public void rememberSettings() {
            super.rememberSettings();
            overpassWizard.addCurrentItemToHistory();
            OVERPASS_SERVER.put(getOverpassServer());
            OVERPASS_SERVER_HISTORY.put(overpassServer.getHistory());
            OVERPASS_WIZARD_HISTORY.put(overpassWizard.getHistory());
            OverpassQueryHistoryPopup.addToHistory(getOverpassQuery());
        }

    }

    static class OverpassQueryHistoryPopup extends JPopupMenu {

        static final CollectionProperty OVERPASS_QUERY_HISTORY = new CollectionProperty("download.overpass.query", new ArrayList<String>());
        static final IntegerProperty OVERPASS_QUERY_HISTORY_SIZE = new IntegerProperty("download.overpass.query.size", 12);

        OverpassQueryHistoryPopup(final OverpassDownloadDialog dialog) {
            final Collection<String> history = OVERPASS_QUERY_HISTORY.get();
            setLayout(new GridLayout((int) Math.ceil(history.size() / 2.), 2));
            for (final String i : history) {
                add(new OverpassQueryHistoryItem(i, dialog));
            }
        }

        static void show(final JComponent parent, final OverpassDownloadDialog dialog) {
            final OverpassQueryHistoryPopup menu = new OverpassQueryHistoryPopup(dialog);
            final Rectangle r = parent.getBounds();
            menu.show(parent.getParent(), r.x + r.width - (int) menu.getPreferredSize().getWidth(), r.y + r.height);
        }

        static void addToHistory(final String query) {
            final Deque<String> history = new LinkedList<>(OVERPASS_QUERY_HISTORY.get());
            if (!history.contains(query)) {
                history.add(query);
            }
            while (history.size() > OVERPASS_QUERY_HISTORY_SIZE.get()) {
                history.removeFirst();
            }
            OVERPASS_QUERY_HISTORY.put(history);
        }
    }

    static class OverpassQueryHistoryItem extends JMenuItem implements ActionListener {

        final String query;
        final OverpassDownloadDialog dialog;

        OverpassQueryHistoryItem(final String query, final OverpassDownloadDialog dialog) {
            this.query = query;
            this.dialog = dialog;
            setText("<html><pre style='width:300px;'>" +
                    Utils.escapeReservedCharactersHTML(Utils.restrictStringLines(query, 7)));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.setOverpassQuery(query);
        }
    }

}
