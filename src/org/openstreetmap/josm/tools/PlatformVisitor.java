// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Visitor, to be used with {@link Platform}.
 * @param <T> type that will be the result of the visiting operation
 * @since 12776
 */
public interface PlatformVisitor<T> {
    /**
     * Visit {@link Platform#UNIXOID}
     * @return result of the operation
     */
    T visitUnixoid();

    /**
     * Visit {@link Platform#WINDOWS}
     * @return result of the operation
     */
    T visitWindows();

    /**
     * Visit {@link Platform#OSX}
     * @return result of the operation
     */
    T visitOsx();
}
