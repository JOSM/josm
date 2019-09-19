// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * The post-download handler notifies user of potential errors that occurred.
 * @since 2322
 */
public class PostDownloadHandler implements Runnable {
    private final DownloadTask task;
    private final Future<?> future;
    private Consumer<Collection<Object>> errorReporter;

    private static final Set<String> NO_DATA_ERROR_MESSAGES = new HashSet<>();

    /**
     * Creates a new {@link PostDownloadHandler}
     * @param task the asynchronous download task
     * @param future the future on which the completion of the download task can be synchronized
     */
    public PostDownloadHandler(DownloadTask task, Future<?> future) {
        this.task = task;
        this.future = future;
    }

    /**
     * Creates a new {@link PostDownloadHandler} using a custom error reporter
     * @param task the asynchronous download task
     * @param future the future on which the completion of the download task can be synchronized
     * @param errorReporter a callback to inform about the number errors happened during the download
     *                      task
     */
    public PostDownloadHandler(DownloadTask task, Future<?> future, Consumer<Collection<Object>> errorReporter) {
        this(task, future);
        this.errorReporter = errorReporter;
    }

    /**
     * Adds a new translated error message indicating that no data has been downloaded.
     * @param message new translated error message indicating that no data has been downloaded.
     * @return {@code true} if the message was not already known
     * @since 15358
     */
    public static boolean addNoDataErrorMessage(String message) {
        return NO_DATA_ERROR_MESSAGES.add(message);
    }

    /**
     * Determines if a translated error message indicates that no data has been downloaded.
     * @param message translated error message to check
     * @return {@code true} if the message indicates that no data has been downloaded
     * @since 15358
     */
    public static boolean isNoDataErrorMessage(Object message) {
        return NO_DATA_ERROR_MESSAGES.contains(message);
    }

    @Override
    public void run() {
        // wait for downloads task to finish (by waiting for the future to return a value)
        //
        try {
            future.get();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            Logging.error(e);
            return;
        }

        // make sure errors are reported only once
        //
        Set<Object> errors = new LinkedHashSet<>(task.getErrorObjects());

        if (this.errorReporter != null) {
            GuiHelper.runInEDT(() -> errorReporter.accept(errors));
        }

        if (errors.isEmpty()) {
            return;
        }

        // just one error object?
        //
        if (errors.size() == 1) {
            final Object error = errors.iterator().next();
            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> {
                    if (error instanceof Exception) {
                        ExceptionDialogUtil.explainException((Exception) error);
                    } else if (isNoDataErrorMessage(error)) {
                        new Notification(error.toString()).setIcon(JOptionPane.WARNING_MESSAGE).show();
                    } else {
                        JOptionPane.showMessageDialog(
                                MainApplication.getMainFrame(),
                                error.toString(),
                                tr("Error during download"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            return;
        }

        // multiple error object? prepare a HTML list
        //
        if (!errors.isEmpty()) {
            final Collection<String> items = new ArrayList<>();
            for (Object error : errors) {
                if (error instanceof String) {
                    items.add((String) error);
                } else if (error instanceof Exception) {
                    items.add(ExceptionUtil.explainException((Exception) error));
                }
            }

            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        "<html>"+Utils.joinAsHtmlUnorderedList(items)+"</html>",
                        tr("Errors during download"),
                        JOptionPane.ERROR_MESSAGE));
            }
        }
    }
}
