// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;

/**
 * Create a basic wiremock environment. If you need the actual WireMockServer, annotate a field or parameter
 * with {@code @BasicWiremock}.
 *
 * @author Taylor Smock
 * @see OsmApiExtension (this sets the Osm Api to the wiremock URL)
 * @since 18106
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@ExtendWith(BasicWiremock.WireMockExtension.class)
public @interface BasicWiremock {
    /**
     * Set the path for the data. Default is {@link TestUtils#getTestDataRoot()}.
     * @return The path ({@code ""} for the default)
     */
    String value() default "";

    /**
     * {@link ResponseTransformerV2} for use with the WireMock server.
     * Current constructors supported:
     * <ul>
     *     <li>{@code new ResponseTransformer()}</li>
     *     <li>{@code new ResponseTransformer(ExtensionContext context)}</li>
     * </ul>
     * @return The transformers to instantiate
     */
    Class<? extends ResponseTransformerV2>[] responseTransformers() default {};

    /**
     * Start/stop WireMock automatically, and check for missed calls.
     * @author Taylor Smock
     *
     */
    class WireMockExtension extends com.github.tomakehurst.wiremock.junit5.WireMockExtension {
        protected WireMockExtension() {
            this(defaultOptions());
        }

        /**
         * Create a new extension with options
         *
         * @param builder a {@link Builder}
         *                instance holding the initialisation parameters for the extension.
         */
        protected WireMockExtension(Builder builder) {
            super(builder);
        }

        /**
         * Get the default wiremock server
         * @param context The context to search
         * @return The wiremock server
         */
        static WireMockServer getWiremock(ExtensionContext context) {
            ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(BasicWiremock.class);
            BasicWiremock annotation = AnnotationUtils.findFirstParentAnnotation(context, BasicWiremock.class)
                    .orElseThrow(() -> new IllegalArgumentException("There must be a @BasicWiremock annotation"));
            return context.getStore(namespace).getOrComputeIfAbsent(WireMockServer.class, clazz -> {
                final List<ResponseTransformerV2> transformers = new ArrayList<>(annotation.responseTransformers().length);
                for (Class<? extends ResponseTransformerV2> responseTransformer : annotation.responseTransformers()) {
                    for (Pair<Class<?>[], Object[]> parameterMapping : Arrays.asList(
                            new Pair<>(new Class<?>[] {ExtensionContext.class }, new Object[] {context }),
                            new Pair<>(new Class<?>[0], new Object[0]))) {
                        Constructor<? extends ResponseTransformerV2> constructor = assertDoesNotThrow(() ->
                                responseTransformer.getConstructor(parameterMapping.a));
                        ResponseTransformerV2 transformerV2 = assertDoesNotThrow(() -> constructor.newInstance(parameterMapping.b));
                        transformers.add(transformerV2);
                        break;
                    }
                }
                return new WireMockServer(
                    options().usingFilesUnderDirectory(Utils.isStripEmpty(annotation.value()) ? TestUtils.getTestDataRoot() :
                            annotation.value()).extensions(transformers.toArray(new ResponseTransformerV2[0])).dynamicPort());
            }, WireMockServer.class);
        }

        static Builder defaultOptions() {
            WireMockConfiguration options = WireMockConfiguration.options()
                    .usingFilesUnderDirectory(TestUtils.getTestDataRoot())
                    .dynamicPort();
            return extensionOptions().options(options);
        }

        /**
         * Replace URL servers with wiremock
         *
         * @param wireMockServer The wiremock to point to
         * @param url            The URL to fix
         * @return A url that points at the wiremock server
         * @deprecated since 19152 (not used in core; no known users)
         */
        @Deprecated(forRemoval = true, since = "19152")
        public static String replaceUrl(WireMockServer wireMockServer, String url) {
            try {
                URL temp = new URL(url);
                return wireMockServer.baseUrl() + temp.getFile();
            } catch (MalformedURLException error) {
                Logging.error(error);
            }
            return null;
        }

        @Override
        protected void onBeforeAll(ExtensionContext extensionContext, WireMockRuntimeInfo wireMockRuntimeInfo) {
            extensionContext.getStore(ExtensionContext.Namespace.create(BasicWiremock.WireMockExtension.class))
                    .put(BasicWiremock.WireMockExtension.class, this);
        }

        @Override
        protected void onAfterAll(ExtensionContext extensionContext, WireMockRuntimeInfo wireMockRuntimeInfo) {
            // Sync threads to ensure that no further wiremock requests will be made
            final ThreadSync.ThreadSyncExtension threadSyncExtension = new ThreadSync.ThreadSyncExtension();
            assertDoesNotThrow(() -> threadSyncExtension.afterEach(extensionContext));
        }

        @Override
        protected void onBeforeEach(ExtensionContext context, WireMockRuntimeInfo wireMockRuntimeInfo) {
            if (context.getTestClass().isPresent()) {
                List<Field> wireMockFields = AnnotationSupport.findAnnotatedFields(context.getRequiredTestClass(), BasicWiremock.class);
                for (Field field : wireMockFields) {
                    if (WireMockServer.class.isAssignableFrom(field.getType())) {
                        final boolean isAccessible = field.canAccess(context.getRequiredTestInstance());
                        field.setAccessible(true);
                        try {
                            field.set(context.getTestInstance().orElse(null), getWiremock(context));
                        } catch (IllegalAccessException e) {
                            fail(e);
                        } finally {
                            field.setAccessible(isAccessible);
                        }
                    } else {
                        throw new IllegalArgumentException("@BasicWiremock: cannot set field of type " + field.getType().getName());
                    }
                }
            }
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            if (super.supportsParameter(parameterContext, extensionContext)) {
                return true;
            }
            if (WireMockRuntimeInfo.class.isAssignableFrom(parameterContext.getParameter().getType())) {
                return true;
            }
            return parameterContext.getParameter().getAnnotation(BasicWiremock.class) != null
                    && parameterContext.getParameter().getType() == WireMockServer.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            if (super.supportsParameter(parameterContext, extensionContext)) {
                return super.resolveParameter(parameterContext, extensionContext);
            }
            if (WireMockRuntimeInfo.class.isAssignableFrom(parameterContext.getParameter().getType())) {
                return getRuntimeInfo();
            }
            return getWiremock(extensionContext);
        }
    }

    /**
     * A class specifically to mock OSM API calls
     */
    class OsmApiExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {
        @Override
        public void afterAll(ExtensionContext context) {
            Config.getPref().put("osm-server.url", "https://invalid.url");
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            if (!AnnotationSupport.isAnnotated(context.getElement(), BasicPreferences.class)) {
                fail("OsmApiExtension requires @BasicPreferences");
            }
            this.beforeEach(context);
        }

        @Override
        public void beforeEach(ExtensionContext extensionContext) throws Exception {
            BasicWiremock.WireMockExtension extension =
                    extensionContext.getStore(ExtensionContext.Namespace.create(BasicWiremock.WireMockExtension.class))
                    .get(BasicWiremock.WireMockExtension.class, BasicWiremock.WireMockExtension.class);
            WireMockRuntimeInfo wireMockRuntimeInfo = extension.getRuntimeInfo();
            Config.getPref().put("osm-server.url", wireMockRuntimeInfo.getHttpBaseUrl() + "/api");
            wireMockRuntimeInfo.getWireMock().register(WireMock.get("/api/0.6/capabilities")
                    .willReturn(WireMock.aResponse().withBodyFile("api/0.6/capabilities")));
            wireMockRuntimeInfo.getWireMock().register(WireMock.get("/api/capabilities")
                    .willReturn(WireMock.aResponse().withBodyFile("api/capabilities")));
            assertDoesNotThrow(() -> OsmApi.getOsmApi().initialize(NullProgressMonitor.INSTANCE));
        }
    }
}
