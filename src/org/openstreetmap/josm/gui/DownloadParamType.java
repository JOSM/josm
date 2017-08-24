// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.OpenLocationAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * The type of a command line parameter, to be used in switch statements.
 * @since 12633 (extracted from {@code Main})
 */
public enum DownloadParamType {
    /** http(s):// URL */
    httpUrl {
        @Override
        public List<Future<?>> download(String s, Collection<File> fileList) {
            return new OpenLocationAction().openUrl(false, s);
        }

        @Override
        public List<Future<?>> downloadGps(String s) {
            final Bounds b = OsmUrlToBounds.parse(s);
            if (b == null) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Ignoring malformed URL: \"{0}\"", s),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return Collections.emptyList();
            }
            return MainApplication.downloadFromParamBounds(true, b);
        }
    },
    /** file:// URL */
    fileUrl {
        @Override
        public List<Future<?>> download(String s, Collection<File> fileList) {
            File f = null;
            try {
                f = new File(new URI(s));
            } catch (URISyntaxException e) {
                Logging.warn(e);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Ignoring malformed file URL: \"{0}\"", s),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
            }
            if (f != null) {
                fileList.add(f);
            }
            return Collections.emptyList();
        }
    },
    /** geographic area */
    bounds {

        /**
         * Download area specified on the command line as bounds string.
         * @param rawGps Flag to download raw GPS tracks
         * @param s The bounds parameter
         * @return the complete download task (including post-download handler), or {@code null}
         */
        private List<Future<?>> downloadFromParamBounds(final boolean rawGps, String s) {
            final StringTokenizer st = new StringTokenizer(s, ",");
            if (st.countTokens() == 4) {
                return MainApplication.downloadFromParamBounds(rawGps, new Bounds(
                        new LatLon(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken())),
                        new LatLon(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()))
                ));
            }
            return Collections.emptyList();
        }

        @Override
        public List<Future<?>> download(String param, Collection<File> fileList) {
            return downloadFromParamBounds(false, param);
        }

        @Override
        public List<Future<?>> downloadGps(String param) {
            return downloadFromParamBounds(true, param);
        }
    },
    /** local file name */
    fileName {
        @Override
        public List<Future<?>> download(String s, Collection<File> fileList) {
            fileList.add(new File(s));
            return Collections.emptyList();
        }
    };

    /**
     * Performs the download
     * @param param represents the object to be downloaded
     * @param fileList files which shall be opened, should be added to this collection
     * @return the download task, or {@code null}
     */
    public abstract List<Future<?>> download(String param, Collection<File> fileList);

    /**
     * Performs the GPS download
     * @param param represents the object to be downloaded
     * @return the download task, or {@code null}
     */
    public List<Future<?>> downloadGps(String param) {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Parameter \"downloadgps\" does not accept file names or file URLs"),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
        }
        return Collections.emptyList();
    }

    /**
     * Guess the type of a parameter string specified on the command line with --download= or --downloadgps.
     *
     * @param s A parameter string
     * @return The guessed parameter type
     */
    public static DownloadParamType paramType(String s) {
        if (s.startsWith("http:") || s.startsWith("https:")) return DownloadParamType.httpUrl;
        if (s.startsWith("file:")) return DownloadParamType.fileUrl;
        String coorPattern = "\\s*[+-]?[0-9]+(\\.[0-9]+)?\\s*";
        if (s.matches(coorPattern + "(," + coorPattern + "){3}")) return DownloadParamType.bounds;
        // everything else must be a file name
        return DownloadParamType.fileName;
    }
}
