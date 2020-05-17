// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.loader;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * This class loads the map paint styles
 * @since 12651 (extracted from {@link MapPaintStyles}).
 */
public class MapPaintStyleLoader extends PleaseWaitRunnable {
    private boolean canceled;
    private final Collection<StyleSource> sources;

    /**
     * Create a new {@link MapPaintStyleLoader}
     * @param sources The styles to load
     */
    public MapPaintStyleLoader(Collection<StyleSource> sources) {
        super(tr("Reloading style sources"));
        this.sources = sources;
    }

    @Override
    protected void cancel() {
        canceled = true;
    }

    @Override
    protected void finish() {
        MapPaintStyles.fireMapPaintSylesUpdated();
    }

    @Override
    protected void realRun() {
        ProgressMonitor monitor = getProgressMonitor();
        monitor.setTicksCount(sources.size());
        for (StyleSource s : sources) {
            if (canceled)
                return;
            monitor.subTask(tr("loading style ''{0}''...", s.getDisplayString()));
            s.loadStyleSource();
            monitor.worked(1);
        }
    }

    /**
     * Reload styles
     * preferences are the same, but the file source may have changed
     * @param sel the indices of styles to reload
     */
    public static void reloadStyles(final int... sel) {
        List<StyleSource> data = MapPaintStyles.getStyles().getStyleSources();
        List<StyleSource> toReload = Arrays.stream(sel).mapToObj(data::get).collect(Collectors.toList());
        MainApplication.worker.submit(new MapPaintStyleLoader(toReload));
    }

    /**
     * Reload style.
     * @param style {@link StyleSource} to reload
     * @throws IllegalArgumentException if {@code style} is not a {@code StyleSource} instance
     * @since 12825
     */
    public static void reloadStyle(SourceEntry style) {
        if (style instanceof StyleSource) {
            MainApplication.worker.submit(new MapPaintStyleLoader(Collections.singleton((StyleSource) style)));
        } else {
            throw new IllegalArgumentException(style + " is not a StyleSource");
        }
    }
}
