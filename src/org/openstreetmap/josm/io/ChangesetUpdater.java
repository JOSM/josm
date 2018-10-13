// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Checks periodically if open changesets have been closed on server side.
 * @since 14326
 */
public final class ChangesetUpdater {

    private ChangesetUpdater() {
        // Hide default constructor for utils classes
    }

    /** Property defining the update interval in minutes */
    public static final IntegerProperty PROP_INTERVAL = new IntegerProperty("changeset.updater.interval", 60);

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(Utils.newThreadFactory("changeset-updater-%d", Thread.NORM_PRIORITY));

    private static final Runnable WORKER = new Worker();

    private static volatile ScheduledFuture<?> task;

    private static class Worker implements Runnable {

        private long lastTimeInMillis;

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            // See #14671 - Make sure we don't run the API call many times after system wakeup
            if (currentTime >= lastTimeInMillis + TimeUnit.MINUTES.toMillis(PROP_INTERVAL.get())) {
                lastTimeInMillis = currentTime;
                check();
            }
        }
    }

    /**
     * Checks for open changesets that have been closed on server side, and update Changeset cache if needed.
     */
    public static void check() {
        long now = System.currentTimeMillis();
        List<Long> changesetIds = ChangesetCache.getInstance().getOpenChangesets().stream()
            .filter(x -> x.getCreatedAt() != null
                && now - x.getCreatedAt().getTime() > TimeUnit.HOURS.toMillis(1))
            .map(Changeset::getId)
            .map(Integer::longValue)
            .collect(Collectors.toList());
        if (!changesetIds.isEmpty()) {
            try {
                ChangesetCache.getInstance().update(new OsmServerChangesetReader().queryChangesets(
                        new ChangesetQuery().forChangesetIds(changesetIds), null));
            } catch (OsmTransferException e) {
                Logging.warn(e);
            }
        }
    }

    /**
     * Starts the changeset updater task if not already started
     */
    public static void start() {
        int interval = PROP_INTERVAL.get();
        if (!isRunning() && interval > 0) {
            task = EXECUTOR.scheduleAtFixedRate(WORKER, 0, interval, TimeUnit.MINUTES);
            Logging.info("Changeset updater active (checks every "+interval+" minute"+(interval > 1 ? "s" : "") +
                    " if open changesets have been closed)");
        }
    }

    /**
     * Stops the changeset updater task if started
     */
    public static void stop() {
        if (isRunning()) {
            task.cancel(false);
            Logging.info("Changeset updater inactive");
            task = null;
        }
    }

    /**
     * Determines if the changeset updater is currently running
     * @return {@code true} if the updater is running, {@code false} otherwise
     */
    public static boolean isRunning() {
        return task != null;
    }
}
