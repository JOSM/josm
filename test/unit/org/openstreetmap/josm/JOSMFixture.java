// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.fail;

import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.I18n;

public class JOSMFixture {
    static private final Logger logger = Logger.getLogger(JOSMFixture.class.getName());

    static public JOSMFixture createUnitTestFixture() {
        return new JOSMFixture("test/config/unit-josm.home");
    }

    static public JOSMFixture createFunctionalTestFixture() {
        return new JOSMFixture("test/config/functional-josm.home");
    }

    static public JOSMFixture createPerformanceTestFixture() {
        return new JOSMFixture("test/config/performance-josm.home");
    }

    private final String josmHome;

    public JOSMFixture(String josmHome) {
        this.josmHome = josmHome;
    }

    public void init() {

        // check josm.home
        //
        if (josmHome == null) {
            fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
        } else {
            File f = new File(josmHome);
            if (! f.exists() || ! f.canRead()) {
                fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing or not readable.", "josm.home", josmHome));
            }
        }
        System.setProperty("josm.home", josmHome);
        Main.initApplicationPreferences();
        I18n.init();
        // initialize the plaform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we anything else
        Main.platform.preStartupHook();

        Main.pref.init(false);
        I18n.set(Main.pref.get("language", "en"));

        // init projection
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator

        // make sure we don't upload to or test against production
        //
        String url = OsmApi.getOsmApi().getBaseUrl().toLowerCase().trim();
        if (url.startsWith("http://www.openstreetmap.org") || url.startsWith("http://api.openstreetmap.org")
            || url.startsWith("https://www.openstreetmap.org") || url.startsWith("https://api.openstreetmap.org")) {
            fail(MessageFormat.format("configured server url ''{0}'' seems to be a productive url, aborting.", url));
        }
    }
}
