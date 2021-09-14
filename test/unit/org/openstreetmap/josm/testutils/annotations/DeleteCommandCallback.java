// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;

/**
 * Initialize the DeleteCommand callback with the default callback.
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(DeleteCommandCallback.DeleteCommandCallbackExtension.class)
public @interface DeleteCommandCallback {
    /**
     * Initialize and reset the DeleteCommand callback
     * @author Taylor Smock
     *
     */
    class DeleteCommandCallbackExtension implements BeforeAllCallback, AfterAllCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            AnnotationUtils.resetStaticClass(DeleteCommand.class);
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
        }
    }
}
