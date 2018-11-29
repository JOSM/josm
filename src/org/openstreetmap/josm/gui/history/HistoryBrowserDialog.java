// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryDataSetListener;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

/**
 * This is non-modal dialog, always showing on top, which displays history information
 * about a given {@link org.openstreetmap.josm.data.osm.OsmPrimitive}.
 * @since 1709
 */
public class HistoryBrowserDialog extends JDialog implements HistoryDataSetListener {

    /** the embedded browser */
    private final HistoryBrowser browser = new HistoryBrowser();
    private final CloseAction closeAction = new CloseAction();
    private final JLabel titleLabel = new JLabel("", JLabel.CENTER);

    /**
     * Constructs a new {@code HistoryBrowserDialog}.
     *
     * @param history the history to be displayed
     */
    public HistoryBrowserDialog(History history) {
        super(GuiHelper.getFrameForComponent(MainApplication.getMainFrame()), false);
        build();
        setHistory(history);
        setTitle(buildTitle(history));
        pack();
        if (getInsets().top > 0) {
            titleLabel.setVisible(false);
        }
        HistoryDataSet.getInstance().addHistoryDataSetListener(this);
        addWindowListener(new WindowClosingAdapter());
    }

    /**
     * Constructs the title for this dialog
     *
     * @param h the current history
     * @return the title for this dialog
     */
    static String buildTitle(History h) {
        String title;
        switch (h.getEarliest().getType()) {
        case NODE: title = marktr("History for node {0}");
            break;
        case WAY: title = marktr("History for way {0}");
            break;
        case RELATION: title = marktr("History for relation {0}");
            break;
        default: title = "";
        }
        return tr(title, Long.toString(h.getId()));
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    /**
     * builds the GUI
     */
    protected void build() {
        setLayout(new BorderLayout());

        add(titleLabel, BorderLayout.NORTH);

        add(browser, BorderLayout.CENTER);

        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton btn = new JButton(new ReloadAction());
        btn.setName("btn.reload");
        pnl.add(btn);

        btn = new JButton(closeAction);
        btn.setName("btn.close");
        pnl.add(btn);
        InputMapUtils.addEscapeAction(getRootPane(), closeAction);

        btn = new JButton(new ContextSensitiveHelpAction(ht("/Action/ObjectHistory")));
        btn.setName("btn.help");
        pnl.add(btn);
        add(pnl, BorderLayout.SOUTH);

        HelpUtil.setHelpContext(getRootPane(), ht("/Action/ObjectHistory"));
    }

    /**
     * Sets the current history.
     * @param history current history
     */
    protected void setHistory(History history) {
        browser.populate(history);
    }

    /**
     * Removes this history browser model as listener for data change and layer change events.
     * @deprecated not needeed anymore, job is done in {@link #dispose}
     */
    @Deprecated
    public void unlinkAsListener() {
        getHistoryBrowser().getModel().unlinkAsListener();
    }

    /* ---------------------------------------------------------------------------------- */
    /* interface HistoryDataSetListener                                                   */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void historyUpdated(HistoryDataSet source, PrimitiveId primitiveId) {
        if (primitiveId == null || primitiveId.equals(browser.getHistory().getPrimitiveId())) {
            History history = source.getHistory(browser.getHistory().getPrimitiveId());
            if (history != null) {
                browser.populate(history);
            }
        }
    }

    @Override
    public void historyDataSetCleared(HistoryDataSet source) {
        if (isVisible()) {
            closeAction.run();
        }
    }

    class CloseAction extends AbstractAction {
        CloseAction() {
            putValue(NAME, tr("Close"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog"));
            new ImageProvider("ok").getResource().attachImageIcon(this);
        }

        void run() {
            HistoryBrowserDialogManager.getInstance().hide(HistoryBrowserDialog.this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            run();
        }
    }

    class ReloadAction extends AbstractAction {
        ReloadAction() {
            putValue(NAME, tr("Reload"));
            putValue(SHORT_DESCRIPTION, tr("Reload the history from the server"));
            new ImageProvider("dialogs", "refresh").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            HistoryLoadTask task = new HistoryLoadTask();
            task.add(browser.getHistory());
            MainApplication.worker.submit(task);
        }
    }

    class WindowClosingAdapter extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            if (isVisible()) {
                closeAction.run();
            }
        }
    }

    /**
     * Replies the history browser.
     * @return the history browser
     */
    public HistoryBrowser getHistoryBrowser() {
        return browser;
    }

    @Override
    public void dispose() {
        HistoryDataSet.getInstance().removeHistoryDataSetListener(this);
        GuiHelper.destroyComponents(this, false);
        super.dispose();
    }
}
