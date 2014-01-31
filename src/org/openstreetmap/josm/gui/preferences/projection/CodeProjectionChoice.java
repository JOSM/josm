// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;

/**
 * Projection choice that lists all known projects by code.
 */
public class CodeProjectionChoice extends AbstractProjectionChoice implements SubPrefsOptions {

    String code;

    /**
     * Constructs a new {@code CodeProjectionChoice}.
     */
    public CodeProjectionChoice() {
        super(tr("By Code (EPSG)"), "core:code");
    }

    private static class CodeSelectionPanel extends JPanel implements ListSelectionListener, DocumentListener {

        public JosmTextField filter;
        private ProjectionCodeListModel model;
        public JList selectionList;
        List<String> data;
        List<String> filteredData;
        static final String DEFAULT_CODE = "EPSG:3857";
        String lastCode = DEFAULT_CODE;
        ActionListener listener;

        public CodeSelectionPanel(String initialCode, ActionListener listener) {
            this.listener = listener;
            data = new ArrayList<String>(Projections.getAllProjectionCodes());
            Collections.sort(data, new CodeComparator());
            filteredData = new ArrayList<String>(data);
            build();
            setCode(initialCode != null ? initialCode : DEFAULT_CODE);
            selectionList.addListSelectionListener(this);
        }

        /**
         * Comparator that compares the number part of the code numerically.
         */
        private class CodeComparator implements Comparator<String> {
            final Pattern codePattern = Pattern.compile("([a-zA-Z]+):(\\d+)");
            @Override
            public int compare(String c1, String c2) {
                Matcher matcher1 = codePattern.matcher(c1);
                Matcher matcher2 = codePattern.matcher(c2);
                if (matcher1.matches()) {
                    if (matcher2.matches()) {
                        int cmp1 = matcher1.group(1).compareTo(matcher2.group(1));
                        if (cmp1 != 0) return cmp1;
                        int num1 = Integer.parseInt(matcher1.group(2));
                        int num2 = Integer.parseInt(matcher2.group(2));
                        return Integer.valueOf(num1).compareTo(num2);
                    } else
                        return -1;
                } else if (matcher2.matches())
                    return 1;
                return c1.compareTo(c2);
            }
        }

        /**
         * List model for the filtered view on the list of all codes.
         */
        private class ProjectionCodeListModel extends AbstractListModel {
            @Override
            public int getSize() {
                return filteredData.size();
            }

            @Override
            public Object getElementAt(int index) {
                if (index >= 0 && index < filteredData.size())
                    return filteredData.get(index);
                else
                    return null;
            }

            public void fireContentsChanged() {
                fireContentsChanged(this, 0, this.getSize()-1);
            }
        }

        private void build() {
            filter = new JosmTextField(30);
            filter.setColumns(10);
            filter.getDocument().addDocumentListener(this);

            selectionList = new JList(data.toArray());
            selectionList.setModel(model = new ProjectionCodeListModel());
            JScrollPane scroll = new JScrollPane(selectionList);
            scroll.setPreferredSize(new Dimension(200, 214));

            this.setLayout(new GridBagLayout());
            this.add(filter, GBC.eol().weight(1.0, 0.0));
            this.add(scroll, GBC.eol());
        }

        public String getCode() {
            int idx = selectionList.getSelectedIndex();
            if (idx == -1) return lastCode;
            return filteredData.get(selectionList.getSelectedIndex());
        }

        public void setCode(String code) {
            int idx = filteredData.indexOf(code);
            if (idx != -1) {
                selectionList.setSelectedIndex(idx);
                selectionList.ensureIndexIsVisible(idx);
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            listener.actionPerformed(null);
            lastCode = getCode();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateFilter();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateFilter();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateFilter();
        }

        private void updateFilter() {
            filteredData.clear();
            String filterTxt = filter.getText().trim().toLowerCase();
            for (String code : data) {
                if (code.toLowerCase().contains(filterTxt)) {
                    filteredData.add(code);
                }
            }
            model.fireContentsChanged();
            int idx =  filteredData.indexOf(lastCode);
            if (idx == -1) {
                selectionList.clearSelection();
                if (selectionList.getModel().getSize() > 0) {
                    selectionList.ensureIndexIsVisible(0);
                }
            } else {
                selectionList.setSelectedIndex(idx);
                selectionList.ensureIndexIsVisible(idx);
            }
        }
    }

    @Override
    public Projection getProjection() {
        return Projections.getProjectionByCode(code);
    }


    @Override
    public String getCurrentCode() {
        // not needed - getProjection() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectionName() {
        // not needed - getProjection() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPreferences(Collection<String> args) {
        if (args != null && !args.isEmpty()) {
            code = args.iterator().next();
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new CodeSelectionPanel(code, listener);
    }

    @Override
    public Collection<String> getPreferences(JPanel panel) {
        if (!(panel instanceof CodeSelectionPanel)) {
            throw new IllegalArgumentException();
        }
        CodeSelectionPanel csPanel = (CodeSelectionPanel) panel;
        return Collections.singleton(csPanel.getCode());
    }

    /* don't return all possible codes - this projection choice it too generic */
    @Override
    public String[] allCodes() {
        return new String[0];
    }

    /* not needed since allCodes() returns empty array */
    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        return null;
    }

    @Override
    public boolean showProjectionCode() {
        return true;
    }

    @Override
    public boolean showProjectionName() {
        return true;
    }

}
