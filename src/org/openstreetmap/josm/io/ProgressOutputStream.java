package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.OutputStream;

import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * An {@link OutputStream} which reports progress to the {@link ProgressMonitor}.
 *
 * @since 9185
 */
public class ProgressOutputStream extends OutputStream {

    private final StreamProgressUpdater updater;
    private final OutputStream out;

    /**
     * Constructs a new {@code ProgressOutputStream}.
     *
     * @param out the stream to monitor
     * @param size the total size which will be sent
     * @param progressMonitor the monitor to report to
     */
    public ProgressOutputStream(OutputStream out, long size, ProgressMonitor progressMonitor) {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        this.updater = new StreamProgressUpdater(size, progressMonitor, tr("Uploading data ..."));
        this.out = out;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        updater.advanceTicker(len);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        updater.advanceTicker(1);
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
        } finally {
            updater.finishTask();
        }
    }
}
