// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.nmea;

/**
 * An NMEA sentence consists of a start delimiter, followed by a comma-separated sequence of fields,
 * followed by the character * (ASCII 42), the checksum and an end-of-line marker.
 * The start delimiter is normally $ (ASCII 36).<p>
 * Most GPS sensors emit only RMC, GGA, GSA, GSV, GLL, VTG, and (rarely) ZDA.
 * Newer ones conforming to NMEA 3.x may emit GBS as well.
 * Other NMEA sentences are usually only emitted by high-end maritime navigation systems.<p>
 * See <a href="https://gpsd.gitlab.io//gpsd/NMEA.html#_nmea_encoding_conventions">NMEA Encoding Conventions</a>
 * @since 12421
 */
public enum Sentence {

    /**
     * GBS - GPS Satellite Fault Detection
     * <pre>
     *            1      2   3   4   5   6   7   8   9
     *            |      |   |   |   |   |   |   |   |
     * $--GBS,hhmmss.ss,x.x,x.x,x.x,x.x,x.x,x.x,x.x*hh
     * </pre>
     * Field Number:<ol>
     * <li>UTC time of the GGA or GNS fix associated with this sentence</li>
     * <li>Expected error in latitude (meters)</li>
     * <li>Expected error in longitude (meters)</li>
     * <li>Expected error in altitude (meters)</li>
     * <li>PRN of most likely failed satellite</li>
     * <li>Probability of missed detection for most likely failed satellite</li>
     * <li>Estimate of bias in meters on most likely failed satellite</li>
     * <li>Standard deviation of bias estimate</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io//gpsd/NMEA.html#_gbs_gps_satellite_fault_detection">GBS</a>
     */
    GBS,

    /**
     * GGA - Global Positioning System Fix Data
     * <pre>
     *                                                      11
     *        1         2       3 4        5 6 7  8   9  10 |  12 13  14   15
     *        |         |       | |        | | |  |   |   | |   | |   |    |
     * $--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh
     * </pre>
     * Field Number:<ol>
     * <li>Universal Time Coordinated (UTC)</li>
     * <li>Latitude</li>
     * <li>N or S (North or South)</li>
     * <li>Longitude</li>
     * <li>E or W (East or West)</li>
     * <li>GPS Quality Indicator,<ul>
     *   <li>0 - fix not available,</li>
     *   <li>1 - GPS fix,</li>
     *   <li>2 - Differential GPS fix (values above 2 are 2.3 features)</li>
     *   <li>3 = PPS fix</li>
     *   <li>4 = Real Time Kinematic</li>
     *   <li>5 = Float RTK</li>
     *   <li>6 = estimated (dead reckoning)</li>
     *   <li>7 = Manual input mode</li>
     *   <li>8 = Simulation mode</li>
     * </ul></li>
     * <li>Number of satellites in view, 00 - 12</li>
     * <li>Horizontal Dilution of precision (meters)</li>
     * <li>Antenna Altitude above/below mean-sea-level (geoid) (in meters)</li>
     * <li>Units of antenna altitude, meters</li>
     * <li>Geoidal separation, the difference between the WGS-84 earth ellipsoid and mean-sea-level (geoid),
     *     "-" means mean-sea-level below ellipsoid</li>
     * <li>Units of geoidal separation, meters</li>
     * <li>Age of differential GPS data, time in seconds since last SC104 type 1 or 9 update, null field when DGPS is not used</li>
     * <li>Differential reference station ID, 0000-1023</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_gga_global_positioning_system_fix_data">GGA</a>
     */
    GGA,

    /**
     * GSA - GPS DOP and active satellites
     * <pre>
     *        1 2 3                        14 15  16  17  18
     *        | | |                         |  |   |   |   |
     * $--GSA,a,a,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x.x,x.x,x.x*hh
     * </pre>
     * Field Number:<ol>
     * <li>Selection mode: M=Manual, forced to operate in 2D or 3D, A=Automatic, 3D/2D</li>
     * <li>Mode (1 = no fix, 2 = 2D fix, 3 = 3D fix)</li>
     * <li>ID of 1st satellite used for fix</li>
     * <li>ID of 2nd satellite used for fix</li>
     * <li>ID of 3rd satellite used for fix</li>
     * <li>ID of 4th satellite used for fix</li>
     * <li>ID of 5th satellite used for fix</li>
     * <li>ID of 6th satellite used for fix</li>
     * <li>ID of 7th satellite used for fix</li>
     * <li>ID of 8th satellite used for fix</li>
     * <li>ID of 9th satellite used for fix</li>
     * <li>ID of 10th satellite used for fix</li>
     * <li>ID of 11th satellite used for fix</li>
     * <li>ID of 12th satellite used for fix</li>
     * <li>PDOP</li>
     * <li>HDOP</li>
     * <li>VDOP</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_gsa_gps_dop_and_active_satellites">GSA</a>
     */
    GSA,

    /**
     * GSV - Satellites in view
     * <pre>
     *        1 2 3 4 5 6 7     n
     *        | | | | | | |     |
     * $--GSV,x,x,x,x,x,x,x,...*hh
     * </pre>
     * Field Number:<ol>
     * <li>total number of GSV messages to be transmitted in this group</li>
     * <li>1-origin number of this GSV message within current group</li>
     * <li>total number of satellites in view (leading zeros sent)</li>
     * <li>satellite PRN number (leading zeros sent)</li>
     * <li>elevation in degrees (00-90) (leading zeros sent)</li>
     * <li>azimuth in degrees to true north (000-359) (leading zeros sent)</li>
     * <li>SNR in dB (00-99) (leading zeros sent) more satellite info quadruples like 4-7 n) checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_gsv_satellites_in_view">GSV</a>
     */
    GSV,

    /**
     * GLL - Geographic Position - Latitude/Longitude
     * <pre>
     *        1       2 3        4 5         6 7   8
     *        |       | |        | |         | |   |
     * $--GLL,llll.ll,a,yyyyy.yy,a,hhmmss.ss,a,m,*hh
     * </pre>
     * Field Number:<ol>
     * <li>Latitude</li>
     * <li>N or S (North or South)</li>
     * <li>Longitude</li>
     * <li>E or W (East or West)</li>
     * <li>Universal Time Coordinated (UTC)</li>
     * <li>Status A - Data Valid, V - Data Invalid</li>
     * <li>FAA mode indicator (NMEA 2.3 and later)</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_gll_geographic_position_latitude_longitude">GLL</a>
     */
    GLL,
    
    /**
     * GST - GPS Pseudorange Noise Statistics
     * <pre>
     *              1    2 3 4 5 6 7 8   9
     *              |    | | | | | | |   |
     * $ --GST,hhmmss.ss,x,x,x,x,x,x,x*hh
     * </pre>
     * Field Number:<ol>
     * <li>UTC time of associated GGA fix</li>
     * <li>Total RMS standard deviation of ranges inputs to the navigation solution</li>
     * <li>Standard deviation (meters) of semi-major axis of error ellipse</li>
     * <li>Standard deviation (meters) of semi-minor axis of error ellipse</li>
     * <li>Orientation of semi-major axis of error ellipse (true north degrees)</li>
     * <li>Standard deviation (meters) of latitude error</li>
     * <li>Standard deviation (meters) of longitude error</li>
     * <li>Standard deviation (meters) of altitude error</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_gst_gps_pseudorange_noise_statistics">GST</a>
     */
    GST,

    /**
     * RMC - Recommended Minimum Navigation Information
     * <pre>
     *                                                          12
     *        1         2 3       4 5        6  7   8   9    10 11|  13
     *        |         | |       | |        |  |   |   |    |  | |   |
     * $--RMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxx,x.x,a,m,*hh
     * </pre>
     * Field Number:<ol>
     * <li>UTC Time</li>
     * <li>Status, V=Navigation receiver warning A=Valid</li>
     * <li>Latitude</li>
     * <li>N or S</li>
     * <li>Longitude</li>
     * <li>E or W</li>
     * <li>Speed over ground, knots</li>
     * <li>Track made good, degrees true</li>
     * <li>Date, ddmmyy</li>
     * <li>Magnetic Variation, degrees</li>
     * <li>E or W</li>
     * <li>FAA mode indicator (NMEA 2.3 and later)</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_rmc_recommended_minimum_navigation_information">RMC</a>
     */
    RMC,

    /**
     * VTG - Track made good and Ground speed
     * <pre>
     *         1  2  3  4  5  6  7  8 9   10
     *         |  |  |  |  |  |  |  | |   |
     * $--VTG,x.x,T,x.x,M,x.x,N,x.x,K,m,*hh
     * </pre>
     * Field Number:<ol>
     * <li>Track Degrees</li>
     * <li>T = True</li>
     * <li>Track Degrees</li>
     * <li>M = Magnetic</li>
     * <li>Speed Knots</li>
     * <li>N = Knots</li>
     * <li>Speed Kilometers Per Hour</li>
     * <li>K = Kilometers Per Hour</li>
     * <li>FAA mode indicator (NMEA 2.3 and later)</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_vtg_track_made_good_and_ground_speed">VTG</a>
     */
    VTG,

    /**
     * ZDA - Time &amp; Date - UTC, day, month, year and local time zone
     * <pre>
     *        1         2  3  4    5  6  7
     *        |         |  |  |    |  |  |
     * $--ZDA,hhmmss.ss,xx,xx,xxxx,xx,xx*hh
     * </pre>
     * Field Number:<ol>
     * <li>UTC time (hours, minutes, seconds, may have fractional subsecond)</li>
     * <li>Day, 01 to 31</li>
     * <li>Month, 01 to 12</li>
     * <li>Year (4 digits)</li>
     * <li>Local zone description, 00 to +- 13 hours</li>
     * <li>Local zone minutes description, apply same sign as local hours</li>
     * <li>Checksum</li>
     * </ol>
     * See <a href="https://gpsd.gitlab.io/gpsd/NMEA.html#_zda_time_amp_date_utc_day_month_year_and_local_time_zone">ZDA</a>
     */
    ZDA
}
