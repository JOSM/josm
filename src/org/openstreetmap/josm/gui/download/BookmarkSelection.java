// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.Bookmark;
import org.openstreetmap.josm.gui.BookmarkList;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Bookmark selector.
 *
 * Provides selection, creation and deletion of bookmarks.
 * Extracted from old DownloadAction.
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class BookmarkSelection implements DownloadSelection {

    private Bounds currentArea;
    private BookmarkList bookmarks;
    
    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(new JButton(new AddAction()), gc);

        gc.gridy = 1;
        RemoveAction removeAction = new RemoveAction();
        bookmarks.addListSelectionListener(removeAction);
        pnl.add(new JButton(removeAction), gc);

        gc.gridy = 2;
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
 
    public void addGui(final DownloadDialog gui) {
        JPanel dlg = new JPanel(new GridBagLayout());
        gui.addDownloadAreaSelector(dlg, tr("Bookmarks"));
        GridBagConstraints gc = new GridBagConstraints();
     
        
        bookmarks = new BookmarkList();
        bookmarks.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Preferences.Bookmark b = (Preferences.Bookmark)bookmarks.getSelectedValue();
                if (b != null) {
                    gui.boundingBoxChanged(b.getArea(),BookmarkSelection.this);
                }
            }
        });
                
        gc.fill = GridBagConstraints.VERTICAL;
        gc.weightx = 0.0;
        gc.weighty = 1.0;        
        dlg.add(buildButtonPanel(),gc);
        
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;   
        gc.gridx = 1;
        dlg.add(new JScrollPane(bookmarks), gc);        
    }
    
    public void setDownloadArea(Bounds area) {
        if (area == null) return;
        this.currentArea = area;
        bookmarks.clearSelection();
    }
    
    class AddAction extends AbstractAction {
        public AddAction() {
            //putValue(NAME, tr("Add"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
            putValue(SHORT_DESCRIPTION, tr("Add a bookmark for the currently selected download area"));
        }
        
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
            if (b.getName() != null && !b.getName().equals("")) {
                ((DefaultListModel)bookmarks.getModel()).addElement(b);
                bookmarks.save();
            }            
        }
    }
    
    class RemoveAction extends AbstractAction implements ListSelectionListener{
        public RemoveAction() {
           //putValue(NAME, tr("Remove"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Remove the currently selected bookmarks"));
            updateEnabledState();
        }
        
        public void actionPerformed(ActionEvent e) {
            Object[] sels = bookmarks.getSelectedValues();
            if (sels == null || sels.length == 0) {
                return;
            }
            for (Object sel: sels) {
                ((DefaultListModel)bookmarks.getModel()).removeElement(sel);
            }
            bookmarks.save();
        }
        protected void updateEnabledState() {
            setEnabled(bookmarks.getSelectedIndices().length > 0);
        }
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }       
    }
    
    class RenameAction extends AbstractAction implements ListSelectionListener{
        public RenameAction() {
           //putValue(NAME, tr("Remove"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            putValue(SHORT_DESCRIPTION, tr("Rename the currently selected bookmark"));
            updateEnabledState();
        }
        
        public void actionPerformed(ActionEvent e) {
            Object[] sels = bookmarks.getSelectedValues();
            if (sels == null || sels.length != 1) {
                return;
            }
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
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }       
    }
   
}
