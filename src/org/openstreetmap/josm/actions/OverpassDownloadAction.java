// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
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
                null, true, "overpassdownload/download", true);
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
        protected JTextArea overpassQuery;
        private static OverpassDownloadDialog instance;
        static final StringProperty OVERPASS_SERVER = new StringProperty("download.overpass.server", "http://overpass-api.de/api/");
        static final CollectionProperty OVERPASS_SERVER_HISTORY = new CollectionProperty("download.overpass.servers",
                Arrays.asList("http://overpass-api.de/api/", "http://overpass.osm.rambler.ru/cgi/"));
        static final CollectionProperty OVERPASS_WIZARD_HISTORY = new CollectionProperty("download.overpass.wizard", new ArrayList<String>());

        private OverpassDownloadDialog(Component parent) {
            super(parent);
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

            overpassQuery = new JTextArea("[timeout:15];", 8, 80);
            overpassQuery.setFont(GuiHelper.getMonospacedFont(overpassQuery));
            JScrollPane scrollPane = new JScrollPane(overpassQuery);
            pnl.add(new JLabel(tr("Overpass query: ")), GBC.std().insets(5, 5, 5, 5));
            GBC gbc = GBC.eol().fill(GBC.HORIZONTAL);
            gbc.ipady = 200;
            pnl.add(scrollPane, gbc);

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
        }

    }

    static class OverpassDownloadReader extends BoundingBoxDownloader {

        final String overpassServer;
        final String overpassQuery;

        public OverpassDownloadReader(Bounds downloadArea, String overpassServer, String overpassQuery) {
            super(downloadArea);
            this.overpassServer = overpassServer;
            this.overpassQuery = overpassQuery.trim();
        }

        @Override
        protected String getBaseUrl() {
            return overpassServer;
        }

        @Override
        protected String getRequestForBbox(double lon1, double lat1, double lon2, double lat2) {
            if (overpassQuery.isEmpty())
                return super.getRequestForBbox(lon1, lat1, lon2, lat2);
            else {
                String realQuery = completeOverpassQuery(overpassQuery);
                try {
                    return "interpreter?data=" + URLEncoder.encode(realQuery, "UTF-8") + "&bbox=" + lon1 + "," + lat1 + "," + lon2 + "," + lat2;
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException();
                }
            }
        }

        private String completeOverpassQuery(String query) {
            int firstColon = query.indexOf(";");
            if (firstColon == -1) {
                return "[bbox];" + query;
            }
            int bboxPos = query.indexOf("[bbox");
            if (bboxPos > -1 && bboxPos < firstColon) {
                return query;
            }

            int bracketCount = 0;
            int pos = 0;
            for (; pos < firstColon; ++pos) {
                if (query.charAt(pos) == '[')
                    ++bracketCount;
                else if (query.charAt(pos) == '[')
                    --bracketCount;
                else if (bracketCount == 0) {
                    if (!Character.isWhitespace(query.charAt(pos)))
                        break;
                }
            }

            if (pos < firstColon) {
                // We start with a statement, not with declarations
                return "[bbox];" + query;
            }

            // We start with declarations. Add just one more declaration in this case.
            return "[bbox]" + query;
        }

        @Override
        public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {

            DataSet ds = super.parseOsm(progressMonitor);

            // add bounds if necessary (note that Overpass API does not return bounds in the response XML)
            if (ds != null && ds.dataSources.isEmpty()) {
                if (crosses180th) {
                    Bounds bounds = new Bounds(lat1, lon1, lat2, 180.0);
                    DataSource src = new DataSource(bounds, getBaseUrl());
                    ds.dataSources.add(src);

                    bounds = new Bounds(lat1, -180.0, lat2, lon2);
                    src = new DataSource(bounds, getBaseUrl());
                    ds.dataSources.add(src);
                } else {
                    Bounds bounds = new Bounds(lat1, lon1, lat2, lon2);
                    DataSource src = new DataSource(bounds, getBaseUrl());
                    ds.dataSources.add(src);
                }
            }

            return ds;
        }
    }
}
