// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;

abstract class AbstractExtendedSourceEntryTestCase {

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource://(.+)");
    private static final Pattern JOSM_WIKI_PATTERN = Pattern.compile("https://josm.openstreetmap.de/josmfile\\?page=(.+)&zip=1");
    private static final Pattern GITHUB_PATTERN = Pattern.compile("https://raw.githubusercontent.com/([^/]+)/([^/]+)/([^/]+)/(.+)");

    protected static final List<String> errorsToIgnore = new ArrayList<>();

    /** Entry to test */
    protected final ExtendedSourceEntry source;
    protected final List<String> ignoredErrors = new ArrayList<>();

    protected AbstractExtendedSourceEntryTestCase(ExtendedSourceEntry source) {
        this.source = source;
    }

    protected static List<Object[]> getTestParameters(Collection<ExtendedSourceEntry> entries) throws Exception {
        return entries.stream().map(x -> new Object[] {x.getDisplayName(), cleanUrl(x.url), x}).collect(Collectors.toList());
    }

    private static String cleanUrl(String url) {
        Matcher wiki = JOSM_WIKI_PATTERN.matcher(url);
        if (wiki.matches()) {
            return "https://josm.openstreetmap.de/wiki/" + wiki.group(1);
        }
        Matcher github = GITHUB_PATTERN.matcher(url);
        if (github.matches()) {
            return String.format("https://github.com/%s/%s/blob/%s/%s", github.group(1), github.group(2), github.group(3), github.group(4));
        }
        Matcher resource = RESOURCE_PATTERN.matcher(url);
        if (resource.matches()) {
            return "https://josm.openstreetmap.de/browser/trunk/" + resource.group(1);
        }
        return url;
    }

    protected final void handleException(Exception e, Set<String> errors) {
        e.printStackTrace();
        String s = source.url + " => " + e.toString();
        if (isIgnoredSubstring(s)) {
            ignoredErrors.add(s);
        } else {
            errors.add(s);
        }
    }

    protected static boolean isIgnoredSubstring(String substring) {
        return errorsToIgnore.parallelStream().anyMatch(x -> substring.contains(x));
    }
}
