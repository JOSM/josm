// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * Projection choice that lists all known projects by code.
 * @since 5634
 */
public class CodeProjectionChoice extends AbstractProjectionChoice implements SubPrefsOptions {

    private String code;

    /**
     * Constructs a new {@code CodeProjectionChoice}.
     */
    public CodeProjectionChoice() {
        super(tr("By Code (EPSG)"), /* NO-ICON */ "core:code");
    }

    /**
     * Comparator that compares the number part of the code numerically.
     */
    public static class CodeComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Pattern codePattern = Pattern.compile("([a-zA-Z]+):(\\d+)");

        @Override
        public int compare(String c1, String c2) {
            Matcher matcher1 = codePattern.matcher(c1);
            Matcher matcher2 = codePattern.matcher(c2);
            if (matcher1.matches()) {
                if (matcher2.matches()) {
                    int cmp1 = matcher1.group(1).compareTo(matcher2.group(1));
                    if (cmp1 != 0)
                        return cmp1;
                    int num1 = Integer.parseInt(matcher1.group(2));
                    int num2 = Integer.parseInt(matcher2.group(2));
                    return Integer.compare(num1, num2);
                } else
                    return -1;
            } else if (matcher2.matches())
                return 1;
            return c1.compareTo(c2);
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
            throw new IllegalArgumentException("Unsupported panel: "+panel);
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
