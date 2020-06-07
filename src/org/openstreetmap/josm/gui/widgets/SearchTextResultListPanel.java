// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;

/**
 * A panel containing a search text field and a list of results for that search text.
 * @param <T> The class of the things that are searched
 */
public abstract class SearchTextResultListPanel<T> extends JPanel {

    protected final JosmTextField edSearchText;
    protected final JList<T> lsResult;
    protected final ResultListModel<T> lsResultModel = new ResultListModel<>();

    protected final transient List<ListSelectionListener> listSelectionListeners = new ArrayList<>();

    private transient ActionListener dblClickListener;
    private transient ActionListener clickListener;

    protected abstract void filterItems();

    /**
     * Constructs a new {@code SearchTextResultListPanel}.
     */
    protected SearchTextResultListPanel() {
        super(new BorderLayout());

        edSearchText = new JosmTextField();
        edSearchText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterItems();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterItems();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterItems();
            }
        });
        edSearchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        selectItem(lsResult.getSelectedIndex() + 1);
                        break;
                    case KeyEvent.VK_UP:
                        selectItem(lsResult.getSelectedIndex() - 1);
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        selectItem(lsResult.getSelectedIndex() + 10);
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        selectItem(lsResult.getSelectedIndex() - 10);
                        break;
                    case KeyEvent.VK_HOME:
                        selectItem(0);
                        break;
                    case KeyEvent.VK_END:
                        selectItem(lsResultModel.getSize());
                        break;
                    default: // Do nothing
                }
            }
        });
        add(edSearchText, BorderLayout.NORTH);

        lsResult = new JList<>(lsResultModel);
        lsResult.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lsResult.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    if (dblClickListener != null)
                        dblClickListener.actionPerformed(null);
                } else {
                    if (clickListener != null)
                        clickListener.actionPerformed(null);
                }
            }
        });
        add(new JScrollPane(lsResult), BorderLayout.CENTER);
    }

    protected static class ResultListModel<T> extends AbstractListModel<T> {

        private transient List<T> items = new ArrayList<>();

        public synchronized void setItems(List<T> items) {
            this.items = items;
            fireContentsChanged(this, 0, Integer.MAX_VALUE);
        }

        @Override
        public synchronized T getElementAt(int index) {
            return items.get(index);
        }

        @Override
        public synchronized int getSize() {
            return items.size();
        }

        public synchronized boolean isEmpty() {
            return items.isEmpty();
        }
    }

    /**
     * Initializes and clears the panel.
     */
    public synchronized void init() {
        listSelectionListeners.clear();
        edSearchText.setText("");
        filterItems();
    }

    private synchronized void selectItem(int newIndex) {
        if (newIndex < 0) {
            newIndex = 0;
        }
        if (newIndex > lsResultModel.getSize() - 1) {
            newIndex = lsResultModel.getSize() - 1;
        }
        lsResult.setSelectedIndex(newIndex);
        lsResult.ensureIndexIsVisible(newIndex);
    }

    /**
     * Clear the selected result
     */
    public synchronized void clearSelection() {
        lsResult.clearSelection();
    }

    /**
     * Get the number of items available
     * @return The number of search result items available
     */
    public synchronized int getItemCount() {
        return lsResultModel.getSize();
    }

    /**
     * Returns the search text entered by user.
     * @return the search text entered by user
     * @since 14975
     */
    public String getSearchText() {
        return edSearchText.getText();
    }

    /**
     * Sets a listener to be invoked on double click
     * @param dblClickListener The double click listener
     */
    public void setDblClickListener(ActionListener dblClickListener) {
        this.dblClickListener = dblClickListener;
    }

    /**
     * Sets a listener to be invoked on ssingle click
     * @param clickListener The click listener
     */
    public void setClickListener(ActionListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * Adds a selection listener to the presets list.
     *
     * @param selectListener The list selection listener
     * @since 7412
     */
    public synchronized void addSelectionListener(ListSelectionListener selectListener) {
        lsResult.getSelectionModel().addListSelectionListener(selectListener);
        listSelectionListeners.add(selectListener);
    }

    /**
     * Removes a selection listener from the presets list.
     *
     * @param selectListener The list selection listener
     * @since 7412
     */
    public synchronized void removeSelectionListener(ListSelectionListener selectListener) {
        listSelectionListeners.remove(selectListener);
        lsResult.getSelectionModel().removeListSelectionListener(selectListener);
    }
}
