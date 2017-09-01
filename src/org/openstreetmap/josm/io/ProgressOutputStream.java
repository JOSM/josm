// License: GPL. For details, see LICENSE file.
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
    private final boolean finishOnClose;

    /**
     * Constructs a new {@code ProgressOutputStream}.
     *
     * @param out the stream to monitor
     * @param size the total size which will be sent
     * @param progressMonitor the monitor to report to
     * @param finishOnClose whether to call {@link ProgressMonitor#finishTask} when this stream is closed
     * @since 10302
     */
    public ProgressOutputStream(OutputStream out, long size, ProgressMonitor progressMonitor, boolean finishOnClose) {
        this(out, size, progressMonitor, tr("Uploading data ..."), finishOnClose);
    }

    /**
     * Constructs a new {@code ProgressOutputStream}.
     *
     * @param out the stream to monitor
     * @param size the total size which will be sent
     * @param progressMonitor the monitor to report to
     * @param message the message that will be displayed by the inner {@link StreamProgressUpdater}
     * @param finishOnClose whether to call {@link ProgressMonitor#finishTask} when this stream is closed
     * @since 12711
     */
    public ProgressOutputStream(OutputStream out, long size, ProgressMonitor progressMonitor, String message, boolean finishOnClose) {
        this.updater = new StreamProgressUpdater(size,
                progressMonitor != null ? progressMonitor : NullProgressMonitor.INSTANCE, message);
        this.out = out;
        this.finishOnClose = finishOnClose;
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
            if (finishOnClose) {
                updater.finishTask();
            }
        }
    }
}
