// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.User;

/**
 * Clear users between tests
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@ExtendWith(Users.UserExtension.class)
public @interface Users {
    class UserExtension implements AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            User.clearUserMap();
            UserIdentityManager.getInstance().setAnonymous();
        }
    }
}
