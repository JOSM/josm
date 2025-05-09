// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GeoJSONServerReader;
import org.openstreetmap.josm.io.UrlPatterns.GeoJsonUrlPattern;
import org.openstreetmap.josm.tools.Utils;

/**
 * GeoJson download task.
 * @author Omar Vega Ramos &lt;ovruni@riseup.net&gt;
 * @since 15424
 */
public class DownloadGeoJsonTask extends DownloadOsmTask {

    @Override
    public String[] getPatterns() {
        return patterns(GeoJsonUrlPattern.class);
    }

    @Override
    public String getTitle() {
        return tr("Download GeoJSON");
    }

    @Override
    public Future<?> download(DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return null;
    }

    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        downloadTask = new InternalDownloadTask(settings, url, progressMonitor);
        return MainApplication.worker.submit(downloadTask);
    }

    class InternalDownloadTask extends DownloadTask {

        private final String url;

        InternalDownloadTask(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
            super(settings, new GeoJSONServerReader(url), progressMonitor, true, Compression.byExtension(url));
            this.url = url;
        }

        @Override
        protected String generateLayerName() {
            return Optional.of(url.substring(url.lastIndexOf('/')+1))
                .filter(Predicate.not(Utils::isStripEmpty))
                .orElse(super.generateLayerName());
        }

        @Override
        protected OsmDataLayer createNewLayer(final DataSet dataSet, final Optional<String> layerName) {
            if (layerName.filter(Utils::isStripEmpty).isPresent()) {
                throw new IllegalArgumentException("Blank layer name!");
            }
            return new OsmDataLayer(dataSet, layerName.orElseGet(this::generateLayerName), null);
        }
    }
}
