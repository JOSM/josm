// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.Authenticator.RequestorType;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsAgentResponse;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.JosmPreferencesCredentialAgent;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Notifies user periodically of new received (unread) messages
 * @since 6349
 */
public final class MessageNotifier {

    private MessageNotifier() {
        // Hide default constructor for utils classes
    }

    /**
     * Called when new new messages are detected.
     * @since 12766
     */
    @FunctionalInterface
    public interface NotifierCallback {
        /**
         * Perform the actual notification of new messages.
         * @param userInfo the new user information, that includes the number of unread messages
         */
        void notifyNewMessages(UserInfo userInfo);
    }

    private static volatile NotifierCallback callback;

    /**
     * Sets the {@link NotifierCallback} responsible of notifying the user when new messages are received.
     * @param notifierCallback the new {@code NotifierCallback}
     */
    public static void setNotifierCallback(NotifierCallback notifierCallback) {
        callback = notifierCallback;
    }

    /** Property defining if this task is enabled or not */
    public static final BooleanProperty PROP_NOTIFIER_ENABLED = new BooleanProperty("message.notifier.enabled", true);
    /** Property defining the update interval in minutes */
    public static final IntegerProperty PROP_INTERVAL = new IntegerProperty("message.notifier.interval", 5);

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(Utils.newThreadFactory("message-notifier-%d", Thread.NORM_PRIORITY));

    private static final Runnable WORKER = new Worker();

    private static volatile ScheduledFuture<?> task;

    private static class Worker implements Runnable {

        private int lastUnreadCount;
        private long lastTimeInMillis;

        @Override
        public void run() {
            try {
                long currentTime = System.currentTimeMillis();
                // See #14671 - Make sure we don't run the API call many times after system wakeup
                if (currentTime >= lastTimeInMillis + TimeUnit.MINUTES.toMillis(PROP_INTERVAL.get())) {
                    lastTimeInMillis = currentTime;
                    final UserInfo userInfo = new OsmServerUserInfoReader().fetchUserInfo(NullProgressMonitor.INSTANCE,
                            tr("get number of unread messages"));
                    final int unread = userInfo.getUnreadMessages();
                    if (unread > 0 && unread != lastUnreadCount) {
                        callback.notifyNewMessages(userInfo);
                        lastUnreadCount = unread;
                    }
                }
            } catch (OsmApiException e) {
                // We want to explicitely display message to user in some cases like when he has been blocked (#17722)
                ExceptionDialogUtil.explainOsmTransferException(e);
            } catch (OsmTransferException e) {
                // But not message for random network or API issues (like in #17929)
                Logging.warn(e);
            }
        }
    }

    /**
     * Starts the message notifier task if not already started and if user is fully identified
     */
    public static void start() {
        int interval = PROP_INTERVAL.get();
        if (NetworkManager.isOffline(OnlineResource.OSM_API)) {
            Logging.info(tr("{0} not available (offline mode)", tr("Message notifier")));
        } else if (!isRunning() && interval > 0 && isUserEnoughIdentified()) {
            task = EXECUTOR.scheduleAtFixedRate(WORKER, 0, interval, TimeUnit.MINUTES);
            Logging.info("Message notifier active (checks every "+interval+" minute"+(interval > 1 ? "s" : "")+')');
        }
    }

    /**
     * Stops the message notifier task if started
     */
    public static void stop() {
        if (isRunning()) {
            task.cancel(false);
            Logging.info("Message notifier inactive");
            task = null;
        }
    }

    /**
     * Determines if the message notifier is currently running
     * @return {@code true} if the notifier is running, {@code false} otherwise
     */
    public static boolean isRunning() {
        return task != null;
    }

    /**
     * Determines if user set enough information in JOSM preferences to make the request to OSM API without
     * prompting him for a password.
     * @return {@code true} if user chose an OAuth token or supplied both its username and password, {@code false otherwise}
     */
    public static boolean isUserEnoughIdentified() {
        UserIdentityManager identManager = UserIdentityManager.getInstance();
        if (identManager.isFullyIdentified()) {
            return true;
        } else {
            CredentialsManager credManager = CredentialsManager.getInstance();
            try {
                if (JosmPreferencesCredentialAgent.class.equals(credManager.getCredentialsAgentClass())) {
                    if (OsmApi.isUsingOAuth()) {
                        return credManager.lookupOAuthAccessToken() != null;
                    } else {
                        String username = Config.getPref().get("osm-server.username", null);
                        String password = Config.getPref().get("osm-server.password", null);
                        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
                    }
                } else {
                    CredentialsAgentResponse credentials = credManager.getCredentials(
                            RequestorType.SERVER, OsmApi.getOsmApi().getHost(), false);
                    if (credentials != null) {
                        String username = credentials.getUsername();
                        char[] password = credentials.getPassword();
                        return username != null && !username.isEmpty() && password != null && password.length > 0;
                    }
                }
            } catch (CredentialsAgentException e) {
                Logging.log(Logging.LEVEL_WARN, "Unable to get credentials:", e);
            }
        }
        return false;
    }
}
