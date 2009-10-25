// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.LinkedHashSet;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.tools.ExceptionUtil;


public class PostDownloadHandler implements Runnable {
    private DownloadTask task;
    private Future<?> future;

    /**
     * constructor
     * @param task the asynchronous download task
     * @param future the future on which the completion of the download task can be synchronized
     */
    public PostDownloadHandler(DownloadTask task, Future<?> future) {
        this.task = task;
        this.future = future;
    }

    public void run() {
        // wait for the download task to complete
        //
        try {
            future.get();
        } catch(Exception e) {
            e.printStackTrace();
            return;
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
            Object error = errors.iterator().next();
            if (error instanceof Exception) {
                ExceptionDialogUtil.explainException((Exception)error);
                return;
            }
            JOptionPane.showMessageDialog(
                    Main.parent,
                    error.toString(),
                    tr("Errors during Download"),
                    JOptionPane.ERROR_MESSAGE);
            return;

        }

        // multiple error object? prepare a HTML list
        //
        if (!errors.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            for (Object error:errors) {
                if (error instanceof String) {
                    sb.append("<li>").append(error).append("</li>").append("<br>");
                } else if (error instanceof Exception) {
                    sb.append("<li>").append(ExceptionUtil.explainException((Exception)error)).append("</li>").append("<br>");
                }
            }
            sb.insert(0, "<html><ul>");
            sb.append("</ul></html>");

            JOptionPane.showMessageDialog(
                    Main.parent,
                    sb.toString(),
                    tr("Errors during Download"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
    }
}