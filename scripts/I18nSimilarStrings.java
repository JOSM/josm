import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays;
import org.openstreetmap.josm.tools.I18n;

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
        List<String> strings = new ArrayList<>();
        strings.addAll(I18n.getSingularTranslations().keySet());
        strings.addAll(I18n.getPluralTranslations().keySet());
        System.out.println("Loaded " + strings.size() + " core strings");
        strings.removeIf(s -> s.length() <= 5);
        System.out.println("Kept " + strings.size() + " core strings longer than 5 characters");
        Collections.sort(strings);
        for (int i = 0; i < strings.size(); i++) {
            for (int j = i+1; j < strings.size(); j++) {
                String a = strings.get(i);
                String b = strings.get(j);
                int d = SimilarNamedWays.getLevenshteinDistance(a, b);
                if (d <= 2) {
                    System.err.println(": " + a + " <--> " + b);
                }
            }
        }
        System.out.println("Done!");
    }
}
