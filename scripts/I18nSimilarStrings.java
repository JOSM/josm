import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.gui.MainApplicationTest;
import org.openstreetmap.josm.plugins.PluginHandlerTestIT;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Utils;

// License: GPL. For details, see LICENSE file.

/**
 * Finds similar strings in lang files to find potential duplicates in order to reduce translation workload.
 */
public final class I18nSimilarStrings {

    /**
     * Main.
     * @param args not used
     */
    public static void main(String[] args) {
        I18n.init();
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
        Config.setUrlsProvider(JosmUrls.getInstance());
        Preferences pref = new Preferences();
        Config.setPreferencesInstance(pref);
        pref.init(false);
        MainApplicationTest.initContentPane();
        MainApplicationTest.initToolbar();
        MainApplicationTest.initMainMenu();
        PluginHandlerTestIT.loadAllPlugins();
        List<String> strings = new ArrayList<>();
        strings.addAll(I18n.getSingularTranslations().keySet());
        strings.addAll(I18n.getPluralTranslations().keySet());
        System.out.println("Loaded " + strings.size() + " strings");
        strings.removeIf(s -> s.length() <= 5);
        System.out.println("Kept " + strings.size() + " strings longer than 5 characters");
        Collections.sort(strings);
        for (int i = 0; i < strings.size(); i++) {
            for (int j = i+1; j < strings.size(); j++) {
                String a = strings.get(i);
                String b = strings.get(j);
                int d = Utils.getLevenshteinDistance(a, b);
                if (d <= 2) {
                    System.err.println(": " + a + " <--> " + b);
                }
            }
        }
        System.out.println("Done!");
    }
}
