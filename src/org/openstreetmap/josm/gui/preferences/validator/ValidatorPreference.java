// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Preference settings for the validator
 *
 * @author frsantos
 */
public final class ValidatorPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code ValidatorPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ValidatorPreference();
        }
    }

    private ValidatorPreference() {
        super("validator", tr("Data validator"),
                tr("An OSM data validator that checks for common errors made by users and editor programs."));
    }

    /** The preferences prefix */
    public static final String PREFIX = "validator";

    /** The preferences key for error layer */
    public static final String PREF_LAYER = PREFIX + ".layer";

    /** The preferences key for enabled tests */
    public static final String PREF_SKIP_TESTS = PREFIX + ".skip";

    /** The preferences key for enabled tests */
    public static final String PREF_USE_IGNORE = PREFIX + ".ignore";

    /** The preferences key for enabled tests before upload*/
    public static final String PREF_SKIP_TESTS_BEFORE_UPLOAD = PREFIX + ".skipBeforeUpload";

    /** The preferences key for ignored severity other on upload */
    public static final String PREF_OTHER_UPLOAD = PREFIX + ".otherUpload";

    /** The preferences key for ignored severity other */
    public static final String PREF_OTHER = PREFIX + ".other";

    /**
     * The preferences key for enabling the permanent filtering
     * of the displayed errors in the tree regarding the current selection
     */
    public static final String PREF_FILTER_BY_SELECTION = PREFIX + ".selectionFilter";

    private JCheckBox prefUseIgnore;
    private JCheckBox prefUseLayer;
    private JCheckBox prefOtherUpload;
    private JCheckBox prefOther;

    /** The list of all tests */
    private Collection<Test> allTests;

    @Override
    public void addGui(PreferenceTabbedPane gui)
    {
        JPanel testPanel = new JPanel(new GridBagLayout());
        testPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        prefUseIgnore = new JCheckBox(tr("Use ignore list."), Main.pref.getBoolean(PREF_USE_IGNORE, true));
        prefUseIgnore.setToolTipText(tr("Use the ignore list to suppress warnings."));
        testPanel.add(prefUseIgnore, GBC.eol());

        prefUseLayer = new JCheckBox(tr("Use error layer."), Main.pref.getBoolean(PREF_LAYER, true));
        prefUseLayer.setToolTipText(tr("Use the error layer to display problematic elements."));
        testPanel.add(prefUseLayer, GBC.eol());

        prefOther = new JCheckBox(tr("Show informational level."), Main.pref.getBoolean(PREF_OTHER, false));
        prefOther.setToolTipText(tr("Show the informational tests."));
        testPanel.add(prefOther, GBC.eol());

        prefOtherUpload = new JCheckBox(tr("Show informational level on upload."), Main.pref.getBoolean(PREF_OTHER_UPLOAD, false));
        prefOtherUpload.setToolTipText(tr("Show the informational tests in the upload check windows."));
        testPanel.add(prefOtherUpload, GBC.eol());

        ActionListener otherUploadEnabled = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prefOtherUpload.setEnabled(prefOther.isSelected());
            }
        };
        prefOther.addActionListener(otherUploadEnabled);
        otherUploadEnabled.actionPerformed(null);

        GBC a = GBC.eol().insets(-5,0,0,0);
        a.anchor = GBC.EAST;
        testPanel.add( new JLabel(tr("On demand")), GBC.std() );
        testPanel.add( new JLabel(tr("On upload")), a );

        allTests = OsmValidator.getTests();
        for (Test test: allTests) {
            test.addGui(testPanel);
        }

        createPreferenceTabWithScrollPane(gui, testPanel);
    }

    @Override
    public boolean ok() {
        Collection<String> tests = new LinkedList<String>();
        Collection<String> testsBeforeUpload = new LinkedList<String>();

        for (Test test : allTests) {
            test.ok();
            String name = test.getClass().getSimpleName();
            if(!test.enabled)
                tests.add(name);
            if(!test.testBeforeUpload)
                testsBeforeUpload.add(name);
        }
        OsmValidator.initializeTests(allTests);

        Main.pref.putCollection(PREF_SKIP_TESTS, tests);
        Main.pref.putCollection(PREF_SKIP_TESTS_BEFORE_UPLOAD, testsBeforeUpload);
        Main.pref.put(PREF_USE_IGNORE, prefUseIgnore.isSelected());
        Main.pref.put(PREF_OTHER, prefOther.isSelected());
        Main.pref.put(PREF_OTHER_UPLOAD, prefOtherUpload.isSelected());
        Main.pref.put(PREF_LAYER, prefUseLayer.isSelected());
        return false;
    }
}
