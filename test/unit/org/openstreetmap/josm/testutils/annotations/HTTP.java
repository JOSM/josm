// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.openstreetmap.josm.tools.Http1Client;
import org.openstreetmap.josm.tools.HttpClient;

/**
 * Set up the HttpClient factory
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(HTTP.HTTPExtension.class)
public @interface HTTP {
    /**
     * Set the HttpClient type
     * Note that {@link HttpClient#HttpClient(URL, String)} must be accessible.
     *
     * @return The client type to create
     */
    Class<? extends HttpClient> value() default Http1Client.class;

    /**
     * Initialize and reset HttpClient
     * @author Taylor Smock
     *
     */
    class HTTPExtension implements BeforeAllCallback, AfterAllCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            AnnotationUtils.resetStaticClass(HttpClient.class);
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            final Class<? extends HttpClient> clientFactory;
            final Optional<HTTP> annotation = AnnotationSupport.findAnnotation(context.getElement(), HTTP.class);
            if (annotation.isPresent()) {
                clientFactory = annotation.get().value();
            } else {
                clientFactory = Http1Client.class;
            }
            HttpClient.setFactory((url, method) -> ReflectionSupport.newInstance(clientFactory, url, method));
        }
    }
}
