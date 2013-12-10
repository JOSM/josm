// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.fixtures;

import static org.junit.Assert.fail;

import java.io.File;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.I18n;

public class JOSMFixture {
    static private final Logger logger = Logger.getLogger(JOSMFixture.class.getName());

    static public JOSMFixture createUnitTestFixture() {
        return new JOSMFixture("/test-unit-env.properties");
    }

    static public JOSMFixture createFunctionalTestFixture() {
        return new JOSMFixture("/test-functional-env.properties");
    }

    private Properties testProperties;
    private String testPropertiesResourceName;

    public JOSMFixture(String testPropertiesResourceName) {
        this.testPropertiesResourceName = testPropertiesResourceName;
    }

    public void init() {
        testProperties = new Properties();

        // load properties
        //
        try {
            testProperties.load(JOSMFixture.class.getResourceAsStream(testPropertiesResourceName));
        } catch(Exception e){
            logger.log(Level.SEVERE, MessageFormat.format("failed to load property file ''{0}''", testPropertiesResourceName));
            fail(MessageFormat.format("failed to load property file ''{0}''. \nMake sure the path ''$project_root/test/config'' is on the classpath.", testPropertiesResourceName));
        }

        // check josm.home
        //
        String josmHome = testProperties.getProperty("josm.home");
        if (josmHome == null) {
            fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
        } else {
            File f = new File(josmHome);
            if (! f.exists() || ! f.canRead()) {
                fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing or not readable.\nEdit ''{2}'' and update the value ''josm.home''. ", "josm.home", josmHome,testPropertiesResourceName ));
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

        // init projection
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator

        // make sure we don't upload to or test against production
        //
        String url = OsmApi.getOsmApi().getBaseUrl().toLowerCase().trim();
        if (url.startsWith("http://www.openstreetmap.org")
                || url.startsWith("http://api.openstreetmap.org")) {
            fail(MessageFormat.format("configured server url ''{0}'' seems to be a productive url, aborting.", url));
        }
    }
}
