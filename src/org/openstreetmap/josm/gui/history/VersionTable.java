// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * VersionTable shows a list of version in a {@link org.openstreetmap.josm.data.osm.history.History}
 * of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive}.
 * @since 1709
 */
public class VersionTable extends JTable implements ChangeListener, Destroyable {
    private VersionTablePopupMenu popupMenu;
    private final transient HistoryBrowserModel model;

    /**
     * Constructs a new {@code VersionTable}.
     * @param model model used by the history browser
     */
    public VersionTable(HistoryBrowserModel model) {
        super(model.getVersionTableModel(), new VersionTableColumnModel());
        model.addChangeListener(this);
        build();
        this.model = model;
    }

    /**
     * Builds the table.
     */
    protected void build() {
        getTableHeader().setFont(getTableHeader().getFont().deriveFont(9f));
        setRowSelectionAllowed(false);
        setShowGrid(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableHelper.setFont(this, getClass());
        GuiHelper.setBackgroundReadable(this, UIManager.getColor("Button.background"));
        setIntercellSpacing(new Dimension(6, 0));
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        popupMenu = new VersionTablePopupMenu();
        addMouseListener(new MouseListener());
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // navigate history down/up using the corresponding arrow keys.
                long ref = model.getReferencePointInTime().getVersion();
                long cur = model.getCurrentPointInTime().getVersion();
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    History refNext = model.getHistory().from(ref);
                    History curNext = model.getHistory().from(cur);
                    if (refNext.getNumVersions() > 1 && curNext.getNumVersions() > 1) {
                        model.setReferencePointInTime(refNext.sortAscending().get(1));
                        model.setCurrentPointInTime(curNext.sortAscending().get(1));
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    History refNext = model.getHistory().until(ref);
                    History curNext = model.getHistory().until(cur);
                    if (refNext.getNumVersions() > 1 && curNext.getNumVersions() > 1) {
                        model.setReferencePointInTime(refNext.sortDescending().get(1));
                        model.setCurrentPointInTime(curNext.sortDescending().get(1));
                    }
                }
            }
        });
        getModel().addTableModelListener(e ->
                IntStream.range(0, model.getHistory().getNumVersions()).filter(model::isCurrentPointInTime).findFirst().ifPresent(row ->
                        scrollRectToVisible(getCellRect(row, 0, true))));
        getModel().addTableModelListener(e -> {
            adjustColumnWidth(this, 0, 0);
            adjustColumnWidth(this, 1, -8);
            adjustColumnWidth(this, 2, -8);
            adjustColumnWidth(this, 3, 0);
            adjustColumnWidth(this, 4, 0);
            adjustColumnWidth(this, 5, 0);
        });
    }

    @Override
    public void destroy() {
        popupMenu.destroy();
    }

    // some kind of hack to prevent the table from scrolling to the
    // right when clicking on the cells
    @Override
    public void scrollRectToVisible(Rectangle aRect) {
        super.scrollRectToVisible(new Rectangle(0, aRect.y, aRect.width, aRect.height));
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        repaint();
    }

    final class MouseListener extends PopupMenuLauncher {
        private MouseListener() {
            super(Objects.requireNonNull(popupMenu));
        }

        @Override
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            if (!e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON1) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row >= 0 && (col == VersionTableColumnModel.COL_DATE || col == VersionTableColumnModel.COL_USER)) {
                    model.setCurrentPointInTime(row);
                    model.setReferencePointInTime(Math.max(0, row - 1));
                }
            }
        }

        @Override
        protected int checkTableSelection(JTable table, Point p) {
            int row = rowAtPoint(p);
            if (row > -1 && !model.isLatest(row)) {
                popupMenu.prepare(model.getPrimitive(row));
            }
            return row;
        }
    }

    static class ChangesetInfoAction extends AbstractInfoAction {
        private transient HistoryOsmPrimitive primitive;

        /**
         * Constructs a new {@code ChangesetInfoAction}.
         */
        ChangesetInfoAction() {
            super(true);
            putValue(NAME, tr("Changeset info"));
            putValue(SHORT_DESCRIPTION, tr("Launch browser with information about the changeset"));
            new ImageProvider("help/internet").getResource().attachImageIcon(this, true);
        }

        @Override
        protected String createInfoUrl(Object infoObject) {
            if (infoObject instanceof HistoryOsmPrimitive) {
                HistoryOsmPrimitive prim = (HistoryOsmPrimitive) infoObject;
                return Config.getUrls().getBaseBrowseUrl() + "/changeset/" + prim.getChangesetId();
            } else {
                return null;
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            String url = createInfoUrl(primitive);
            OpenBrowser.displayUrl(url);
        }

        public void prepare(HistoryOsmPrimitive primitive) {
            putValue(NAME, tr("Show changeset {0}", primitive.getChangesetId()));
            this.primitive = primitive;
        }
    }

    static class UserInfoAction extends AbstractInfoAction {
        private transient HistoryOsmPrimitive primitive;

        /**
         * Constructs a new {@code UserInfoAction}.
         */
        UserInfoAction() {
            super(true);
            putValue(NAME, tr("User info"));
            putValue(SHORT_DESCRIPTION, tr("Launch browser with information about the user"));
            new ImageProvider("data/user").getResource().attachImageIcon(this, true);
        }

        @Override
        protected String createInfoUrl(Object infoObject) {
            if (infoObject instanceof HistoryOsmPrimitive) {
                HistoryOsmPrimitive hp = (HistoryOsmPrimitive) infoObject;
                return hp.getUser() == null ? null : Config.getUrls().getBaseUserUrl() + '/' + hp.getUser().getName();
            } else {
                return null;
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            String url = createInfoUrl(primitive);
            OpenBrowser.displayUrl(url);
        }

        public void prepare(HistoryOsmPrimitive primitive) {
            final User user = primitive.getUser();
            putValue(NAME, "<html>" + tr("Show user {0}", user == null ? "?" :
                    XmlWriter.encode(user.getName(), true) + " <font color=gray>(" + user.getId() + ")</font>") + "</html>");
            this.primitive = primitive;
        }
    }

    static class VersionTablePopupMenu extends JPopupMenu implements Destroyable {

        private ChangesetInfoAction changesetInfoAction;
        private UserInfoAction userInfoAction;

        /**
         * Constructs a new {@code VersionTablePopupMenu}.
         */
        VersionTablePopupMenu() {
            super();
            build();
        }

        protected void build() {
            changesetInfoAction = new ChangesetInfoAction();
            add(changesetInfoAction);
            userInfoAction = new UserInfoAction();
            add(userInfoAction);
        }

        public void prepare(HistoryOsmPrimitive primitive) {
            changesetInfoAction.prepare(primitive);
            userInfoAction.prepare(primitive);
            invalidate();
        }

        @Override
        public void destroy() {
            if (changesetInfoAction != null) {
                changesetInfoAction.destroy();
                changesetInfoAction = null;
            }
            if (userInfoAction != null) {
                userInfoAction.destroy();
                userInfoAction = null;
            }
        }
    }

    /**
     * Renderer for history radio buttons in columns A and B.
     */
    public static class RadioButtonRenderer extends JRadioButton implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setSelected(value != null && (Boolean) value);
            setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    /**
     * Editor for history radio buttons in columns A and B.
     */
    public static class RadioButtonEditor extends DefaultCellEditor implements ItemListener {

        private final JRadioButton btn;

        /**
         * Constructs a new {@code RadioButtonEditor}.
         */
        public RadioButtonEditor() {
            super(new JCheckBox());
            btn = new JRadioButton();
            btn.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value == null)
                return null;
            boolean val = (Boolean) value;
            btn.setSelected(val);
            btn.addItemListener(this);
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            btn.removeItemListener(this);
            return btn.isSelected();
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            fireEditingStopped();
        }
    }

    /**
     * Renderer for history version labels, allowing to define horizontal alignment.
     */
    public static class AlignedRenderer extends JLabel implements TableCellRenderer {

        /**
         * Constructs a new {@code AlignedRenderer}.
         * @param hAlignment Horizontal alignement. One of the following constants defined in SwingConstants:
         *        LEFT, CENTER (the default for image-only labels), RIGHT, LEADING (the default for text-only labels) or TRAILING
         */
        public AlignedRenderer(int hAlignment) {
            setHorizontalAlignment(hAlignment);
        }

        AlignedRenderer() {
            this(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            String v = "";
            if (value != null) {
                v = value.toString();
            }
            setText(v);
            return this;
        }
    }

    private static void adjustColumnWidth(JTable tbl, int col, int cellInset) {
        int maxwidth = 0;

        for (int row = 0; row < tbl.getRowCount(); row++) {
            TableCellRenderer tcr = tbl.getCellRenderer(row, col);
            Object val = tbl.getValueAt(row, col);
            Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, row, col);
            maxwidth = Math.max(comp.getPreferredSize().width + cellInset, maxwidth);
        }
        TableCellRenderer tcr = tbl.getTableHeader().getDefaultRenderer();
        Object val = tbl.getColumnModel().getColumn(col).getHeaderValue();
        Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, -1, col);
        maxwidth = Math.max(comp.getPreferredSize().width + Config.getPref().getInt("table.header-inset", 0), maxwidth);

        int spacing = tbl.getIntercellSpacing().width;
        tbl.getColumnModel().getColumn(col).setPreferredWidth(maxwidth + spacing);
    }
}
