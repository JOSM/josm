// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.FakeOsmApi;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Specify the OSM API to use for the test. {@link APIType#NONE} has no effect.
 *
 * @author Taylor Smock
 * @see JOSMTestRules#devAPI()
 * @see JOSMTestRules#fakeAPI()
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@FullPreferences
@HTTP
@ExtendWith(OsmApiType.OsmApiTypeExtension.class)
public @interface OsmApiType {
    /**
     * The API type to use
     * @return The API type to use (default NONE)
     */
    APIType value() default APIType.NONE;

    /**
     * API types to initialize
     * @author Taylor Smock
     *
     */
    enum APIType {
        NONE, FAKE, DEV
    }

    /**
     * Initialize the OSM api
     * @author Taylor Smock
     *
     */
    class OsmApiTypeExtension implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            afterEach(context);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            // Reset to none
            Config.getPref().put("osm-server.url", null);
            AnnotationUtils.resetStaticClass(OsmApi.class);
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            beforeEach(context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            Optional<OsmApiType> annotation = AnnotationUtils.findFirstParentAnnotation(context, OsmApiType.class);
            APIType useAPI = APIType.NONE;
            if (annotation.isPresent()) {
                useAPI = annotation.get().value();
            }
            // Set API
            if (useAPI == APIType.DEV) {
                Config.getPref().put("osm-server.url", "https://api06.dev.openstreetmap.org/api");
            } else if (useAPI == APIType.FAKE) {
                FakeOsmApi api = FakeOsmApi.getInstance();
                Config.getPref().put("osm-server.url", api.getServerUrl());
            }

            // Initialize API
            if (useAPI != APIType.NONE) {
                OsmApi.getOsmApi().initialize(null);
            }
        }
    }
}
