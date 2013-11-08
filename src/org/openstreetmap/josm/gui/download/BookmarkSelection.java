// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.download.BookmarkList.Bookmark;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * DownloadAreaSelector which manages a list of "bookmarks", i.e. a list of
 * name download areas.
 *
 */
public class BookmarkSelection implements DownloadSelection {

    /** the currently selected download area. One can add bookmarks for this
     * area, if not null
     */
    private Bounds currentArea;
    /** the list of bookmarks */
    private BookmarkList bookmarks;

    /** the parent download GUI */
    private DownloadDialog parent;

    /** displays information about the current download area */
    private JMultilineLabel lblCurrentDownloadArea;
    final private JosmTextArea bboxDisplay = new JosmTextArea();
    /** the add action */
    private AddAction actAdd;

    /**
     * Creates the panel with the action buttons on the left
     *
     * @return the panel with the action buttons on the left
     */
    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        RemoveAction removeAction = new RemoveAction();
        bookmarks.addListSelectionListener(removeAction);
        pnl.add(new JButton(removeAction), gc);

        gc.gridy = 1;
        RenameAction renameAction = new RenameAction();
        bookmarks.addListSelectionListener(renameAction);
        pnl.add(new JButton(renameAction), gc);

        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.gridy = 3;
        pnl.add(new JPanel(), gc); // just a filler
        return pnl;
    }

    protected JPanel buildDownloadAreaAddPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());

        GridBagConstraints  gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(5,5,5,5);
        pnl.add(lblCurrentDownloadArea = new JMultilineLabel(""), gc);

        gc.weightx = 1.0;
        gc.weighty = 1.0;
        bboxDisplay.setEditable(false);
        bboxDisplay.setBackground(pnl.getBackground());
        bboxDisplay.addFocusListener(new BoundingBoxSelection.SelectAllOnFocusHandler(bboxDisplay));
        pnl.add(bboxDisplay, gc);

        gc.anchor = GridBagConstraints.NORTHEAST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(5,5,5,5);
        pnl.add(new JButton(actAdd = new AddAction()), gc);
        return pnl;
    }

    @Override
    public void addGui(final DownloadDialog gui) {
        JPanel dlg = new JPanel(new GridBagLayout());
        gui.addDownloadAreaSelector(dlg, tr("Bookmarks"));
        GridBagConstraints gc = new GridBagConstraints();

        bookmarks = new BookmarkList();
        bookmarks.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Bookmark b = (Bookmark)bookmarks.getSelectedValue();
                if (b != null) {
                    gui.boundingBoxChanged(b.getArea(),BookmarkSelection.this);
                }
            }
        });
        bookmarks.addMouseListener(new DoubleClickAdapter());

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 2;
        dlg.add(buildDownloadAreaAddPanel(),gc);

        gc.gridwidth = 1;
        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.VERTICAL;
        gc.weightx = 0.0;
        gc.weighty = 1.0;
        dlg.add(buildButtonPanel(),gc);

        gc.gridwidth = 1;
        gc.gridx = 1;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.gridx = 1;
        dlg.add(new JScrollPane(bookmarks), gc);

        this.parent = gui;
    }

    protected void updateDownloadAreaLabel() {
        if (currentArea == null) {
            lblCurrentDownloadArea.setText(tr("<html>There is currently no download area selected.</html>"));
        } else {
            lblCurrentDownloadArea.setText(tr("<html><strong>Current download area</strong> (minlon, minlat, maxlon, maxlat): </html>"));
            bboxDisplay.setText(currentArea.toBBox().toStringCSV(","));
        }
    }

    /**
     * Sets the current download area
     *
     * @param area the download area.
     */
    @Override
    public void setDownloadArea(Bounds area) {
        if (area == null) return;
        this.currentArea = area;
        bookmarks.clearSelection();
        updateDownloadAreaLabel();
        actAdd.setEnabled(true);
    }

    /**
     * The action to add a new bookmark for the current download area.
     *
     */
    class AddAction extends AbstractAction {
        public AddAction() {
            putValue(NAME, tr("Create bookmark"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "bookmark-new"));
            putValue(SHORT_DESCRIPTION, tr("Add a bookmark for the currently selected download area"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentArea == null) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Currently, there is no download area selected. Please select an area first."),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            Bookmark b = new Bookmark();
            b.setName(
                    JOptionPane.showInputDialog(
                            Main.parent,tr("Please enter a name for the bookmarked download area."),
                            tr("Name of location"),
                            JOptionPane.QUESTION_MESSAGE)
            );
            b.setArea(currentArea);
            if (b.getName() != null && !b.getName().isEmpty()) {
                ((DefaultListModel)bookmarks.getModel()).addElement(b);
                bookmarks.save();
            }
        }
    }

    class RemoveAction extends AbstractAction implements ListSelectionListener{
        public RemoveAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Remove the currently selected bookmarks"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object[] sels = bookmarks.getSelectedValues();
            if (sels == null || sels.length == 0)
                return;
            for (Object sel: sels) {
                ((DefaultListModel)bookmarks.getModel()).removeElement(sel);
            }
            bookmarks.save();
        }
        protected void updateEnabledState() {
            setEnabled(bookmarks.getSelectedIndices().length > 0);
        }
        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class RenameAction extends AbstractAction implements ListSelectionListener{
        public RenameAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            putValue(SHORT_DESCRIPTION, tr("Rename the currently selected bookmark"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object[] sels = bookmarks.getSelectedValues();
            if (sels == null || sels.length != 1)
                return;
            Bookmark b = (Bookmark)sels[0];
            Object value =
                JOptionPane.showInputDialog(
                        Main.parent,tr("Please enter a name for the bookmarked download area."),
                        tr("Name of location"),
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        b.getName()
                );
            if (value != null) {
                b.setName(value.toString());
                bookmarks.save();
                bookmarks.repaint();
            }
        }
        protected void updateEnabledState() {
            setEnabled(bookmarks.getSelectedIndices().length == 1);
        }
        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class DoubleClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2))
                return;
            int idx = bookmarks.locationToIndex(e.getPoint());
            if (idx < 0 || idx >= bookmarks.getModel().getSize())
                return;
            Bookmark b = (Bookmark)bookmarks.getModel().getElementAt(idx);
            parent.startDownload(b.getArea());
        }
    }
}
