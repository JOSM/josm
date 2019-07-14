// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Constants for GPX handling.
 */
public interface GpxConstants {

    /** GPS name of the element. This field will be transferred to and from the GPS.
     *  GPX does not place restrictions on the length of this field or the characters contained in it.
     *  It is up to the receiving application to validate the field before sending it to the GPS. */
    String GPX_NAME = "name";

    /** GPS element comment. Sent to GPS as comment. */
    String GPX_CMT = "cmt";

    /** Text description of the element. Holds additional information about the element intended for the user, not the GPS. */
    String GPX_DESC = "desc";

    /** Source of data. Included to give user some idea of reliability and accuracy of data. */
    String GPX_SRC = "src";

    /**
     * Prefix used for all meta values.
     */
    String META_PREFIX = "meta.";
    /**
     * A constant for the metadata hash map: the author name of the file
     * @see GpxData#get(String)
     */
    String META_AUTHOR_NAME = META_PREFIX + "author.name";
    /**
     * A constant for the metadata hash map: the author email of the file
     * @see GpxData#get(String)
     */
    String META_AUTHOR_EMAIL = META_PREFIX + "author.email";
    /**
     * A constant for the metadata hash map: a link to a page about the author
     * @see GpxData#get(String)
     */
    String META_AUTHOR_LINK = META_PREFIX + "author.link";
    /**
     * A constant for the metadata hash map: the author field for the copyright information in the gpx file
     * @see GpxData#get(String)
     */
    String META_COPYRIGHT_AUTHOR = META_PREFIX + "copyright.author";
    /**
     * A constant for the metadata hash map: the license of the file
     * @see GpxData#get(String)
     */
    String META_COPYRIGHT_LICENSE = META_PREFIX + "copyright.license";
    /**
     * A constant for the metadata hash map: the year of the license for the file
     * @see GpxData#get(String)
     */
    String META_COPYRIGHT_YEAR = META_PREFIX + "copyright.year";
    /**
     * A constant for the metadata hash map: a description of the file
     * @see GpxData#get(String)
     */
    String META_DESC = META_PREFIX + "desc";
    /**
     * A constant for the metadata hash map: the keywords of the file
     * @see GpxData#get(String)
     */
    String META_KEYWORDS = META_PREFIX + "keywords";
    /**
     * A constant for the metadata hash map: the links. They are stored as list of {@link GpxLink} objects
     * @see GpxData#get(String)
     */
    String META_LINKS = META_PREFIX + "links";
    /**
     * A constant for the metadata hash map: the name of the file (stored in the file, not the one on the disk)
     * @see GpxData#get(String)
     */
    String META_NAME = META_PREFIX + "name";
    /**
     * A constant for the metadata hash map: the time as string
     * @see GpxData#get(String)
     */
    String META_TIME = META_PREFIX + "time";
    /**
     * A constant for the metadata hash map: the bounding box. This is a {@link Bounds} object
     * @see GpxData#getMetaBounds()
     */
    String META_BOUNDS = META_PREFIX + "bounds";
    /**
     * A constant for the metadata hash map: the extension data. This is a {@link Extensions} object
     * @see GpxData#addExtension(String, String)
     * @see GpxData#get(String)
     */
    String META_EXTENSIONS = META_PREFIX + "extensions";

    /**
     * A namespace for josm GPX extensions
     */
    String JOSM_EXTENSIONS_NAMESPACE_URI = Config.getUrls().getXMLBase() + "/gpx-extensions-1.0";

    /** Elevation (in meters) of the point. */
    String PT_ELE = "ele";

    /** Creation/modification timestamp for the point.
     *  Date and time in are in Univeral Coordinated Time (UTC), not local time!
     *  Conforms to ISO 8601 specification for date/time representation.
     *  Fractional seconds are allowed for millisecond timing in tracklogs. */
    String PT_TIME = "time";

    /** Magnetic variation (in degrees) at the point. 0.0 &lt;= value &lt; 360.0 */
    String PT_MAGVAR = "magvar";

    /** Height, in meters, of geoid (mean sea level) above WGS-84 earth ellipsoid. (NMEA GGA message) */
    String PT_GEOIDHEIGHT = "geoidheight";

    /** Text of GPS symbol name. For interchange with other programs, use the exact spelling of the symbol on the GPS, if known. */
    String PT_SYM = "sym";

    /** Type (textual classification) of element. */
    String PT_TYPE = "type";

    /** Type of GPS fix. none means GPS had no fix. Value comes from list: {'none'|'2d'|'3d'|'dgps'|'pps'} */
    String PT_FIX = "fix";

    /** Number of satellites used to calculate the GPS fix. (not number of satellites in view). */
    String PT_SAT = "sat";

    /** Horizontal dilution of precision. */
    String PT_HDOP = "hdop";

    /** Vertical dilution of precision. */
    String PT_VDOP = "vdop";

    /** Position dilution of precision. */
    String PT_PDOP = "pdop";

    /** Number of seconds since last DGPS update. */
    String PT_AGEOFDGPSDATA = "ageofdgpsdata";

    /** Represents a differential GPS station. 0 &lt;= value &lt;= 1023 */
    String PT_DGPSID = "dgpsid";

    /**
     * Ordered list of all possible waypoint keys.
     */
    List<String> WPT_KEYS = Collections.unmodifiableList(Arrays.asList(PT_ELE, PT_TIME, PT_MAGVAR, PT_GEOIDHEIGHT,
            GPX_NAME, GPX_CMT, GPX_DESC, GPX_SRC, META_LINKS, PT_SYM, PT_TYPE,
            PT_FIX, PT_SAT, PT_HDOP, PT_VDOP, PT_PDOP, PT_AGEOFDGPSDATA, PT_DGPSID, META_EXTENSIONS));

    /**
     * Ordered list of all possible route and track keys.
     */
    List<String> RTE_TRK_KEYS = Collections.unmodifiableList(Arrays.asList(
            GPX_NAME, GPX_CMT, GPX_DESC, GPX_SRC, META_LINKS, "number", PT_TYPE, META_EXTENSIONS));

    /**
     * Possible fix values. NMEA 0183 Version 4.00
     */
    Collection<String> FIX_VALUES = Collections.unmodifiableList(
            Arrays.asList("none", "2d", "3d", "dgps", "pps", "rtk", "float rtk", "estimated", "manual", "simulated"));

    /**
     * The flag which indicates the solution quality.<ul>
     * <li>1 : Fixed, solution by carrier‐based relative positioning and the integer ambiguity is properly resolved.</li>
     * <li>2 : Float, solution by carrier‐based relative positioning but the integer ambiguity is not resolved.</li>
     * <li>3 : Reserved</li>
     * <li>4 : DGPS, solution by code‐based DGPS solutions or single point positioning with SBAS corrections</li>
     * <li>5 : Single, solution by single point positioning</li></ul>
     * @since 15247
     */
    String RTKLIB_Q = "Q";
    /** N (north) component of the standard deviations in m. */
    String RTKLIB_SDN = "sdn";
    /** E (east) component of the standard deviations in m. */
    String RTKLIB_SDE = "sde";
    /** U (up) component of the standard deviations in m. */
    String RTKLIB_SDU = "sdu";
    /**
     * The absolute value of sdne means square root of the absolute value of NE component of the estimated covariance matrix.
     * The sign represents the sign of the covariance. */
    String RTKLIB_SDNE = "sdne";
    /**
     * The absolute value of sdeu means square root of the absolute value of EU component of the estimated covariance matrix.
     * The sign represents the sign of the covariance. */
    String RTKLIB_SDEU = "sdeu";
    /**
     * The absolute value of sdun means square root of the absolute value of UN component of the estimated covariance matrix.
     * The sign represents the sign of the covariance. */
    String RTKLIB_SDUN = "sdun";
    /** The time difference between the observation data epochs of the rover receiver and the base station in second. */
    String RTKLIB_AGE = "age";
    /**
     * The ratio factor of ʺratio‐testʺ for standard integer ambiguity validation strategy.
     * The value means the ratio of the squared sum of the residuals with the second best integer vector to with the best integer vector. */
    String RTKLIB_RATIO = "ration";
}
