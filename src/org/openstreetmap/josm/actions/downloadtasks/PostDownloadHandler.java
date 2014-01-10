// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.Utils;

public class PostDownloadHandler implements Runnable {
    private DownloadTask task;
    private List<Future<?>> futures;

    /**
     * constructor
     * @param task the asynchronous download task
     * @param future the future on which the completion of the download task can be synchronized
     */
    public PostDownloadHandler(DownloadTask task, Future<?> future) {
        this.task = task;
        this.futures = new ArrayList<Future<?>>();
        if (future != null) {
            this.futures.add(future);
        }
    }

    /**
     * constructor
     * @param task the asynchronous download task
     * @param futures the futures on which the completion of the download task can be synchronized
     */
    public PostDownloadHandler(DownloadTask task, Future<?> ... futures) {
        this.task = task;
        this.futures = new ArrayList<Future<?>>();
        if (futures == null) return;
        for (Future<?> future: futures) {
            this.futures.add(future);
        }
    }

    /**
     * constructor
     * @param task the asynchronous download task
     * @param futures the futures on which the completion of the download task can be synchronized
     */
    public PostDownloadHandler(DownloadTask task, List<Future<?>> futures) {
        this.task = task;
        this.futures = new ArrayList<Future<?>>();
        if (futures == null) return;
        this.futures.addAll(futures);
    }

    @Override
    public void run() {
        // wait for all downloads task to finish (by waiting for the futures
        // to return a value)
        //
        for (Future<?> future: futures) {
            try {
                future.get();
            } catch(Exception e) {
                Main.error(e);
                return;
            }
        }

        // make sure errors are reported only once
        //
        LinkedHashSet<Object> errors = new LinkedHashSet<Object>();
        errors.addAll(task.getErrorObjects());
        if (errors.isEmpty())
            return;

        // just one error object?
        //
        if (errors.size() == 1) {
            final Object error = errors.iterator().next();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (error instanceof Exception) {
                        ExceptionDialogUtil.explainException((Exception)error);
                    } else {
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                error.toString(),
                                tr("Error during download"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            return;
        }

        // multiple error object? prepare a HTML list
        //
        if (!errors.isEmpty()) {
            final Collection<String> items = new ArrayList<String>();
            for (Object error:errors) {
                if (error instanceof String) {
                    items.add((String) error);
                } else if (error instanceof Exception) {
                    items.add(ExceptionUtil.explainException((Exception)error));
                }
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            "<html>"+Utils.joinAsHtmlUnorderedList(items)+"</html>",
                            tr("Errors during download"),
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            return;
        }
    }
}
