// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.runners.model.InitializationError;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.FakeOsmApi;

/**
 * Used for setting the desired OsmApi type
 */
@BasicPreferences
@ExtendWith(OsmApi.OsmApiExtension.class)
@HTTP
@LayerManager
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OsmApi {
    APIType value() default APIType.NONE;
    enum APIType {
        /** Don't use any API */
        NONE,
        /** Use the {@link org.openstreetmap.josm.testutils.FakeOsmApi} for testing. */
        FAKE,
        /** Enable the dev.openstreetmap.org API for this test. */
        DEV
    }

    class OsmApiExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            Config.getPref().put("osm-server.url", "http://invalid");
            ((Map<?, ?>) ReflectionSupport.tryToReadFieldValue(
                    org.openstreetmap.josm.io.OsmApi.class.getDeclaredField("instances"), null)
                    .get()).clear();
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            this.beforeEach(context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            final APIType useAPI = AnnotationUtils.findFirstParentAnnotation(context, OsmApi.class)
                    .map(OsmApi::value).orElse(APIType.NONE);
            // Set API
            if (useAPI == APIType.DEV) {
                Config.getPref().put("osm-server.url", "https://api06.dev.openstreetmap.org/api");
            } else if (useAPI == APIType.FAKE) {
                FakeOsmApi api = FakeOsmApi.getInstance();
                Config.getPref().put("osm-server.url", api.getServerUrl());
            } else {
                Config.getPref().put("osm-server.url", "http://invalid");
            }

            // Initialize API
            if (useAPI != APIType.NONE) {
                try {
                    org.openstreetmap.josm.io.OsmApi.getOsmApi().initialize(null);
                } catch (OsmTransferCanceledException | OsmApiInitializationException e) {
                    throw new InitializationError(e);
                }
            }
        }
    }
}
