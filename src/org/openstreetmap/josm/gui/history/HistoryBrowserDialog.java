// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryDataSetListener;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

/**
 * This is non-modal dialog, always showing on top, which displays history information
 * about a given {@see OsmPrimitive}.
 * 
 */
public class HistoryBrowserDialog extends JDialog implements HistoryDataSetListener{

    /** the embedded browser */
    private HistoryBrowser browser;

    /**
     * displays the title for this dialog
     * 
     * @param h the current history
     */
    protected void renderTitle(History h) {
        String title = "";
        switch(h.getEarliest().getType()) {
            case NODE:  title = marktr("History for node {0}"); break;
            case WAY: title = marktr("History for way {0}"); break;
            case RELATION:  title = marktr("History for relation {0}"); break;
        }
        setTitle(tr(
                title,
                Long.toString(h.getId())
        ));
    }

    /**
     * builds the GUI
     * 
     */
    protected void build() {
        setLayout(new BorderLayout());
        browser = new HistoryBrowser();
        add(browser, BorderLayout.CENTER);

        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        SideButton btn = new SideButton(new ReloadAction());
        btn.setName("btn.reload");
        pnl.add(btn);

        btn = new SideButton(new CloseAction());
        btn.setName("btn.close");
        pnl.add(btn);

        btn = new SideButton(new ContextSensitiveHelpAction(ht("/Dialog/History")));
        btn.setName("btn.help");
        pnl.add(btn);
        add(pnl, BorderLayout.SOUTH);

        HelpUtil.setHelpContext(getRootPane(), ht("/Dialog/History"));

        setSize(800, 500);
    }

    /**
     * constructor
     * 
     * @param history  the history to be displayed
     */
    public HistoryBrowserDialog(History history) {
        super(JOptionPane.getFrameForComponent(Main.parent), false);
        build();
        setHistory(history);
        renderTitle(history);
        HistoryDataSet.getInstance().addHistoryDataSetListener(this);
        addWindowListener(new WindowClosingAdapter());
    }

    /**
     * sets the current history
     * @param history
     */
    protected void setHistory(History history) {
        browser.populate(history);
    }

    public void historyUpdated(HistoryDataSet source, long primitiveId) {
        if (primitiveId == browser.getHistory().getId()) {
            browser.populate(source.getHistory(primitiveId));
        } else if (primitiveId == 0) {
            browser.populate(source.getHistory(browser.getHistory().getId()));
        }
    }

    class CloseAction extends AbstractAction {
        public CloseAction() {
            putValue(NAME, tr("Close"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
        }

        public void actionPerformed(ActionEvent e) {
            HistoryDataSet.getInstance().removeHistoryDataSetListener(HistoryBrowserDialog.this);
            HistoryBrowserDialogManager.getInstance().hide(HistoryBrowserDialog.this);
        }
    }

    class ReloadAction extends AbstractAction {
        public ReloadAction() {
            putValue(NAME, tr("Reload"));
            putValue(SHORT_DESCRIPTION, tr("Reload the history from the server"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
        }

        public void actionPerformed(ActionEvent e) {
            HistoryLoadTask task = new HistoryLoadTask();
            task.add(browser.getHistory());
            Main.worker.submit(task);
        }
    }

    class WindowClosingAdapter extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            HistoryDataSet.getInstance().removeHistoryDataSetListener(HistoryBrowserDialog.this);
            HistoryBrowserDialogManager.getInstance().hide(HistoryBrowserDialog.this);
        }
    }

    public HistoryBrowser getHistoryBrowser() {
        return browser;
    }
}
