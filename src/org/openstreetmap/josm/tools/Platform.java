// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Locale;

/**
 * Enum listing the supported platforms (operating system families).
 * @since 12776
 */
public enum Platform {

    /**
     * Unik-like platform. This is the default when the platform cannot be identified.
     */
    UNIXOID {
        @Override
        public <T> T accept(PlatformVisitor<T> visitor) {
            return visitor.visitUnixoid();
        }
    },
    /**
     * Windows platform.
     */
    WINDOWS {
        @Override
        public <T> T accept(PlatformVisitor<T> visitor) {
            return visitor.visitWindows();
        }
    },
    /**
     * macOS (previously OS X) platform.
     */
    OSX {
        @Override
        public <T> T accept(PlatformVisitor<T> visitor) {
            return visitor.visitOsx();
        }
    };

    private static volatile Platform platform;

    /**
     * Support for the visitor pattern.
     * @param <T> type that will be the result of the visiting operation
     * @param visitor the visitor
     * @return result of the operation
     */
    public abstract <T> T accept(PlatformVisitor<T> visitor);

    /**
     * Identifies the current operating system family.
     * @return the the current operating system family
     */
    public static Platform determinePlatform() {
        if (platform == null) {
            String os = Utils.getSystemProperty("os.name");
            if (os == null) {
                Logging.warn("Your operating system has no name, so I'm guessing its some kind of *nix.");
                platform = Platform.UNIXOID;
            } else if (os.toLowerCase(Locale.ENGLISH).startsWith("windows")) {
                platform = Platform.WINDOWS;
            } else if ("Linux".equals(os) || "Solaris".equals(os) ||
                    "SunOS".equals(os) || "AIX".equals(os) ||
                    "FreeBSD".equals(os) || "NetBSD".equals(os) || "OpenBSD".equals(os)) {
                platform = Platform.UNIXOID;
            } else if (os.toLowerCase(Locale.ENGLISH).startsWith("mac os x")) {
                platform = Platform.OSX;
            } else {
                Logging.warn("I don't know your operating system '"+os+"', so I'm guessing its some kind of *nix.");
                platform = Platform.UNIXOID;
            }
        }
        return platform;
    }

}
