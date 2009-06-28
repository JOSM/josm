// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.gui.dialogs.HistoryDialog;

/**
 * This is non-modal dialog, always showing on top, which displays history information
 * about a given {@see OsmPrimitive}.
 * 
 */
public class HistoryBrowserDialog extends JDialog {

    /** the embedded browser */
    private HistoryBrowser browser;

    /**
     * displays the title for this dialog
     * 
     * @param h the current history
     */
    protected void renderTitle(History h) {
        String title = tr(
                "History for {0} {1}",
                h.getEarliest().getType().getLocalizedDisplayNameSingular(),
                Long.toString(h.getId())
        );
        setTitle(title);
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
        pnl.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton btn = new JButton(new CloseAction());
        btn.setName("btn.close");
        pnl.add(btn);
        add(pnl, BorderLayout.SOUTH);

        setSize(800, 500);
    }

    /**
     * constructor
     * 
     * @param history  the history to be displayed
     */
    public HistoryBrowserDialog(History history) {
        super(JOptionPane.getFrameForComponent(Main.parent), false);
        setAlwaysOnTop(true);
        build();
        setHistory(history);
        renderTitle(history);
    }

    /**
     * sets the current history
     * @param history
     */
    protected void setHistory(History history) {
        browser.populate(history);
    }

    /**
     * registers this dialog with the registry of history dialogs
     * 
     * @see HistoryDialog#registerHistoryBrowserDialog(long, HistoryBrowserDialog)
     */
    protected void register() {
        HistoryDialog.registerHistoryBrowserDialog(browser.getHistory().getId(), this);
    }

    /**
     * unregisters this dialog from the registry of history dialogs
     * 
     * @see HistoryDialog#unregisterHistoryBrowserDialog(long)
     */
    protected void unregister() {
        HistoryDialog.unregisterHistoryBrowserDialog(browser.getHistory().getId());
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            register();
            toFront();
        } else {
            unregister();
        }
        super.setVisible(visible);
    }

    class CloseAction extends AbstractAction {
        public CloseAction() {
            putValue(NAME, tr("Close"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog"));
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }

    public HistoryBrowser getHistoryBrowser() {
        return browser;
    }
}
