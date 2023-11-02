// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

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
     * {@link ResponseTransformer} for use with the WireMock server.
     * Current constructors supported:
     * <ul>
     *     <li>{@code new ResponseTransformer()}</li>
     *     <li>{@code new ResponseTransformer(ExtensionContext context)}</li>
     * </ul>
     * @return The transformers to instantiate
     */
    Class<? extends ResponseTransformer>[] responseTransformers() default {};

    /**
     * Start/stop WireMock automatically, and check for missed calls.
     * @author Taylor Smock
     *
     */
    class WireMockExtension
            implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback, ParameterResolver {
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
                final List<ResponseTransformer> transformers = new ArrayList<>(annotation.responseTransformers().length);
                for (Class<? extends ResponseTransformer> responseTransformer : annotation.responseTransformers()) {
                    for (Pair<Class<?>[], Object[]> parameterMapping : Arrays.asList(
                            new Pair<>(new Class<?>[] {ExtensionContext.class }, new Object[] {context }),
                            new Pair<>(new Class<?>[0], new Object[0]))) {
                        try {
                            Constructor<? extends ResponseTransformer> constructor = responseTransformer
                                    .getConstructor(parameterMapping.a);
                            transformers.add(constructor.newInstance(parameterMapping.b));
                            break;
                        } catch (ReflectiveOperationException e) {
                            fail(e);
                        }
                    }
                }
                return new WireMockServer(
                    options().usingFilesUnderDirectory(Utils.isStripEmpty(annotation.value()) ? TestUtils.getTestDataRoot() :
                            annotation.value()).extensions(transformers.toArray(new ResponseTransformer[0])).dynamicPort());
            }, WireMockServer.class);
        }

        /**
         * Replace URL servers with wiremock
         *
         * @param wireMockServer The wiremock to point to
         * @param url            The URL to fix
         * @return A url that points at the wiremock server
         */
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
        public void afterAll(ExtensionContext context) throws Exception {
            // Run in EDT to avoid stopping wiremock server before wiremock requests finish.
            GuiHelper.runInEDTAndWait(getWiremock(context)::stop);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            List<LoggedRequest> missed = getWiremock(context).findUnmatchedRequests().getRequests();
            missed.forEach(r -> Logging.error(r.getAbsoluteUrl()));
            try {
                assertTrue(missed.isEmpty(), missed.stream().map(LoggedRequest::getUrl).collect(Collectors.joining("\n\n")));
            } finally {
                getWiremock(context).resetRequests();
                getWiremock(context).resetToDefaultMappings();
                getWiremock(context).resetScenarios();
                if (AnnotationUtils.elementIsAnnotated(context.getElement(), BasicWiremock.class)
                        || getWiremock(context) == null) {
                    this.afterAll(context);
                }
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            getWiremock(context).start();
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            if (AnnotationUtils.elementIsAnnotated(context.getElement(), BasicWiremock.class) || getWiremock(context) == null) {
                this.beforeAll(context);
            }
            if (context.getTestClass().isPresent()) {
                List<Field> wireMockFields = AnnotationSupport.findAnnotatedFields(context.getRequiredTestClass(), BasicWiremock.class);
                for (Field field : wireMockFields) {
                    if (WireMockServer.class.isAssignableFrom(field.getType())) {
                        final boolean isAccessible = field.isAccessible();
                        field.setAccessible(true);
                        try {
                            field.set(context.getTestInstance().orElse(null), getWiremock(context));
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
            return parameterContext.getParameter().getAnnotation(BasicWiremock.class) != null
                    && parameterContext.getParameter().getType() == WireMockServer.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return getWiremock(extensionContext);
        }
    }

    /**
     * A class specifically to mock OSM API calls
     */
    class OsmApiExtension extends WireMockExtension {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            try {
                super.afterAll(context);
            } finally {
                Config.getPref().put("osm-server.url", "https://invalid.url");
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            if (!AnnotationSupport.isAnnotated(context.getElement(), BasicPreferences.class)) {
                fail("OsmApiExtension requires @BasicPreferences");
            }
            super.beforeAll(context);
            Config.getPref().put("osm-server.url", getWiremock(context).baseUrl());
            OsmApi.getOsmApi().initialize(NullProgressMonitor.INSTANCE);
        }
    }
}
