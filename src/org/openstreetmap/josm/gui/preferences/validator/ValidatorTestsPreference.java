// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;

/**
 * The general validator preferences, allowing to enable/disable tests.
 * @since 6666
 */
public class ValidatorTestsPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code ValidatorTestsPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ValidatorTestsPreference();
        }
    }

    private JCheckBox prefUseIgnore;
    private JCheckBox prefUseLayer;
    private JCheckBox prefOtherUpload;
    private JCheckBox prefOther;

    /** The list of all tests */
    private Collection<Test> allTests;

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel testPanel = new VerticallyScrollablePanel(new GridBagLayout());
        testPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        prefUseIgnore = new JCheckBox(tr("Use ignore list."), ValidatorPrefHelper.PREF_USE_IGNORE.get());
        prefUseIgnore.setToolTipText(tr("Use the ignore list to suppress warnings."));
        testPanel.add(prefUseIgnore, GBC.eol());

        prefUseLayer = new JCheckBox(tr("Use error layer."), ValidatorPrefHelper.PREF_LAYER.get());
        prefUseLayer.setToolTipText(tr("Use the error layer to display problematic elements."));
        testPanel.add(prefUseLayer, GBC.eol());

        prefOther = new JCheckBox(tr("Show informational level."), ValidatorPrefHelper.PREF_OTHER.get());
        prefOther.setToolTipText(tr("Show the informational tests."));
        testPanel.add(prefOther, GBC.eol());

        prefOtherUpload = new JCheckBox(tr("Show informational level on upload."),
                ValidatorPrefHelper.PREF_OTHER_UPLOAD.get());
        prefOtherUpload.setToolTipText(tr("Show the informational tests in the upload check windows."));
        testPanel.add(prefOtherUpload, GBC.eol());

        ActionListener otherUploadEnabled = e -> prefOtherUpload.setEnabled(prefOther.isSelected());
        prefOther.addActionListener(otherUploadEnabled);
        otherUploadEnabled.actionPerformed(null);

        GBC a = GBC.eol().insets(-5, 0, 0, 0);
        a.anchor = GBC.EAST;
        testPanel.add(new JLabel(tr("On demand")), GBC.std());
        testPanel.add(new JLabel(tr("On upload")), a);

        allTests = OsmValidator.getTests();
        for (Test test: allTests) {
            test.addGui(testPanel);
        }

        gui.getValidatorPreference().addSubTab(this, tr("Tests"),
                GuiHelper.embedInVerticalScrollPane(testPanel),
                tr("Choose tests to enable"));
    }

    @Override
    public boolean ok() {
        Collection<String> tests = new LinkedList<>();
        Collection<String> testsBeforeUpload = new LinkedList<>();

        for (Test test : allTests) {
            test.ok();
            String name = test.getClass().getName();
            if (!test.enabled)
                tests.add(name);
            if (!test.testBeforeUpload)
                testsBeforeUpload.add(name);
        }

        // Initializes all tests but MapCSSTagChecker because it is initialized
        // later in ValidatorTagCheckerRulesPreference.ok(),
        // after its list of rules has been saved to preferences
        List<Test> testsToInitialize = new ArrayList<>(allTests);
        testsToInitialize.remove(OsmValidator.getTest(MapCSSTagChecker.class));
        OsmValidator.initializeTests(testsToInitialize);

        Main.pref.putCollection(ValidatorPrefHelper.PREF_SKIP_TESTS, tests);
        Main.pref.putCollection(ValidatorPrefHelper.PREF_SKIP_TESTS_BEFORE_UPLOAD, testsBeforeUpload);
        ValidatorPrefHelper.PREF_USE_IGNORE.put(prefUseIgnore.isSelected());
        ValidatorPrefHelper.PREF_OTHER.put(prefOther.isSelected());
        ValidatorPrefHelper.PREF_OTHER_UPLOAD.put(prefOtherUpload.isSelected());
        ValidatorPrefHelper.PREF_LAYER.put(prefUseLayer.isSelected());
        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getValidatorPreference();
    }
}
