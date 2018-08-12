// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Holder for current platform hook.
 * @since 14138
 */
public final class PlatformManager {

    /**
     * Platform specific code goes in here.
     */
    private static final PlatformHook PLATFORM = Platform.determinePlatform().accept(PlatformHook.CONSTRUCT_FROM_PLATFORM);

    private PlatformManager() {
        // Hide constructor
    }

    /**
     * Returns the current platform hook.
     * @return the current platform hook
     */
    public static PlatformHook getPlatform() {
        return PLATFORM;
    }

    /**
     * Determines if we are currently running on macOS.
     * @return {@code true} if we are currently running on macOS
     */
    public static boolean isPlatformOsx() {
        return PLATFORM instanceof PlatformHookOsx;
    }

    /**
     * Determines if we are currently running on an Unix system.
     * @return {@code true} if we are currently running on an Unix system
     */
    public static boolean isPlatformUnixoid() {
        return PLATFORM instanceof PlatformHookUnixoid;
    }

    /**
     * Determines if we are currently running on Windows.
     * @return {@code true} if we are currently running on Windows
     */
    public static boolean isPlatformWindows() {
        return PLATFORM instanceof PlatformHookWindows;
    }
}
