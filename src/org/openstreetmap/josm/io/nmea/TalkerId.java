// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.nmea;

/**
 * Talker identifiers mnemonics are the first two characters at the beginning of each sentence.
 * This enum lists the common ones (navigation systems).<p>
 * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_talker_ids">Talker IDs</a>
 * @since 12421
 */
public enum TalkerId {
    /** BeiDou (China) */
    BD,
    /** Electronic Chart Display &amp; Information System (ECDIS) */
    EC,
    /** Galileo (Europe) */
    GA,
    /** BeiDou (China) */
    GB,
    /** GLONASS (GLObalnaya NAvigatsionnaya Sputnikovaya Sistema, Russia) */
    GL,
    /** GNSS (Global Navigation Satellite System). Generic form when multiple sources are combined. */
    GN,
    /** GPS (Global Positioning System) */
    GP,
    /** Integrated Navigation */
    IN
}
