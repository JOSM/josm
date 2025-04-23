// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Correlation logic for {@code CorrelateGpxWithImages}.
 * @since 14205
 */
public final class GpxImageCorrelation {

    private GpxImageCorrelation() {
        // Hide public constructor
    }

    /**
     * Match a list of photos to a gpx track with given settings.
     * All images need a exifTime attribute and the List must be sorted according to these times.
     * @param images images to match
     * @param selectedGpx selected GPX data
     * @param settings correlation settings
     * @return number of matched points
     */
    public static int matchGpxTrack(List<? extends GpxImageEntry> images, GpxData selectedGpx, GpxImageCorrelationSettings settings) {
        int ret = 0;

        if (Logging.isDebugEnabled()) {
            Logging.debug("Correlating {0} images to {1} GPX track segments using {2}",
                    images.size(), selectedGpx.getTrackSegsCount(), settings);
        }

        boolean trkInt, trkTag, segInt, segTag;
        int trkTime, trkDist, trkTagTime, segTime, segDist, segTagTime;

        if (settings.isForceTags()) {
            // temporary option to override advanced settings and activate all possible interpolations / tagging methods
            trkInt = trkTag = segInt = segTag = true;
            trkTime = trkDist = trkTagTime = segTime = segDist = segTagTime = Integer.MAX_VALUE;
        } else {
            // Load the settings
            trkInt = Config.getPref().getBoolean("geoimage.trk.int", false);
            trkTime = Config.getPref().getBoolean("geoimage.trk.int.time", false) ?
                    Config.getPref().getInt("geoimage.trk.int.time.val", 60) : Integer.MAX_VALUE;
            trkDist = Config.getPref().getBoolean("geoimage.trk.int.dist", false) ?
                    Config.getPref().getInt("geoimage.trk.int.dist.val", 50) : Integer.MAX_VALUE;

            trkTag = Config.getPref().getBoolean("geoimage.trk.tag", true);
            trkTagTime = Config.getPref().getBoolean("geoimage.trk.tag.time", true) ?
                    Config.getPref().getInt("geoimage.trk.tag.time.val", 2) : Integer.MAX_VALUE;

            segInt = Config.getPref().getBoolean("geoimage.seg.int", true);
            segTime = Config.getPref().getBoolean("geoimage.seg.int.time", true) ?
                    Config.getPref().getInt("geoimage.seg.int.time.val", 60) : Integer.MAX_VALUE;
            segDist = Config.getPref().getBoolean("geoimage.seg.int.dist", true) ?
                    Config.getPref().getInt("geoimage.seg.int.dist.val", 50) : Integer.MAX_VALUE;

            segTag = Config.getPref().getBoolean("geoimage.seg.tag", true);
            segTagTime = Config.getPref().getBoolean("geoimage.seg.tag.time", true) ?
                    Config.getPref().getInt("geoimage.seg.tag.time.val", 2) : Integer.MAX_VALUE;
        }

        final GpxImageDirectionPositionSettings dirpos = settings.getDirectionPositionSettings();
        final GpxImageDatumSettings datumSettings = settings.getDatumSettings();
        final long offset = settings.getOffset();

        boolean isFirst = true;
        long prevWpTime = 0;
        WayPoint prevWp = null;

        for (List<List<WayPoint>> segs : loadTracks(selectedGpx.getTracks())) {
            boolean firstSegment = true;
            for (List<WayPoint> wps : segs) {
                int size = wps.size();
                for (int i = 0; i < size; i++) {
                    final WayPoint curWp = wps.get(i);
                    // Interpolate timestamps in the segment, if one or more waypoints miss them
                    if (!curWp.hasDate()) {
                        //check if any of the following waypoints has a timestamp...
                        if (i > 0 && wps.get(i - 1).hasDate()) {
                            long prevWpTimeNoOffset = wps.get(i - 1).getTimeInMillis();
                            double totalDist = 0;
                            List<Pair<Double, WayPoint>> nextWps = new ArrayList<>();
                            for (int j = i; j < size; j++) {
                                totalDist += wps.get(j - 1).greatCircleDistance(wps.get(j));
                                nextWps.add(new Pair<>(totalDist, wps.get(j)));
                                if (wps.get(j).hasDate()) {
                                    // ...if yes, interpolate everything in between
                                    long timeDiff = wps.get(j).getTimeInMillis() - prevWpTimeNoOffset;
                                    for (Pair<Double, WayPoint> pair : nextWps) {
                                        pair.b.setTimeInMillis((long) (prevWpTimeNoOffset + (timeDiff * (pair.a / totalDist))));
                                    }
                                    break;
                                }
                            }
                            if (!curWp.hasDate()) {
                                break; //It's pointless to continue with this segment, because none of the following waypoints had a timestamp
                            }
                        } else {
                            // Timestamps on waypoints without preceding timestamps in the same segment can not be interpolated, so try next one
                            continue;
                        }
                    }

                    final long curWpTime = curWp.getTimeInMillis() + offset;
                    boolean interpolate = true;
                    int tagTime = 0;
                    if (i == 0) {
                        if (firstSegment) {
                            // First segment of the track, so apply settings for tracks
                            firstSegment = false;
                            if (!trkInt || isFirst || prevWp == null ||
                                    Math.abs(curWpTime - prevWpTime) > TimeUnit.MINUTES.toMillis(trkTime) ||
                                    prevWp.greatCircleDistance(curWp) > trkDist) {
                                isFirst = false;
                                interpolate = false;
                                if (trkTag) {
                                    tagTime = trkTagTime;
                                }
                            }
                        } else {
                            // Apply settings for segments
                            if (!segInt || prevWp == null ||
                                    Math.abs(curWpTime - prevWpTime) > TimeUnit.MINUTES.toMillis(segTime) ||
                                    prevWp.greatCircleDistance(curWp) > segDist) {
                                interpolate = false;
                                if (segTag) {
                                    tagTime = segTagTime;
                                }
                            }
                        }
                    }
                    WayPoint nextWp = i < size - 1 ? wps.get(i + 1) : null;
                    ret += matchPoints(images, prevWp, prevWpTime, curWp, curWpTime, offset,
                                       interpolate, tagTime, nextWp, dirpos, datumSettings);
                    prevWp = curWp;
                    prevWpTime = curWpTime;
                }
            }
        }
        if (trkTag && prevWp != null) {
            ret += matchPoints(images, prevWp, prevWpTime, prevWp, prevWpTime, offset,
                               false, trkTagTime, null, dirpos, datumSettings);
        }
        Logging.debug("Correlated {0} total points", ret);
        return ret;
    }

    static List<List<List<WayPoint>>> loadTracks(Collection<IGpxTrack> tracks) {
        List<List<List<WayPoint>>> trks = new ArrayList<>();
        for (IGpxTrack trk : tracks) {
            List<List<WayPoint>> segs = new ArrayList<>();
            for (IGpxTrackSegment seg : trk.getSegments()) {
                List<WayPoint> wps = new ArrayList<>(seg.getWayPoints());
                if (!wps.isEmpty()) {
                    //remove waypoints at the beginning of the track/segment without timestamps
                    int wp;
                    for (wp = 0; wp < wps.size(); wp++) {
                        if (wps.get(wp).hasDate()) {
                            break;
                        }
                    }
                    if (wp == 0) {
                        segs.add(wps);
                    } else if (wp < wps.size()) {
                        segs.add(wps.subList(wp, wps.size()));
                    }
                }
            }
            //sort segments by first waypoint
            if (!segs.isEmpty()) {
                segs.sort((o1, o2) -> {
                    if (o1.isEmpty() || o2.isEmpty())
                        return 0;
                    return o1.get(0).compareTo(o2.get(0));
                });
                trks.add(segs);
            }
        }
        //sort tracks by first waypoint of first segment
        trks.sort((o1, o2) -> {
            if (o1.isEmpty() || o1.get(0).isEmpty()
             || o2.isEmpty() || o2.get(0).isEmpty())
                return 0;
            return o1.get(0).get(0).compareTo(o2.get(0).get(0));
        });
        return trks;
    }

    /**
     * Gets the elevation value from a WayPoint's attributes.
     * @param wp the WayPoint
     * @return the WayPoint's elevation (in meters)
     */
    static Double getElevation(WayPoint wp) {
        if (wp != null) {
            String value = wp.getString(GpxConstants.PT_ELE);
            if (!Utils.isEmpty(value)) {
                try {
                    return Double.valueOf(value);
                } catch (NumberFormatException e) {
                    Logging.warn(e);
                }
            }
        }
        return null;
    }

    /**
     * Gets the horizontal positioning estimated error value from a WayPoint's attributes.
     * @param wp the WayPoint
     * @return the WayPoint's horizontal positioning error (in meters)
     * @since 19387
     */
    static Double getHPosErr(WayPoint wp) {
        if (wp != null) {
            if (wp.attr.get(GpxConstants.PT_STD_HDEV) instanceof Float) {
                Float hposerr = (Float) wp.attr.get(GpxConstants.PT_STD_HDEV);
                if (hposerr != null) {
                    return hposerr.doubleValue();
                }
            } else if (wp.attr.get(GpxConstants.PT_STD_HDEV) instanceof Double) {               
                Double hposerr = (Double) wp.attr.get(GpxConstants.PT_STD_HDEV);
                if (hposerr != null) {
                    return hposerr;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves GPS Dilution of Precision (DOP) from a WayPoint's attributes.
     * Checks for Position DOP (PDOP) first, then falls back to Horizontal DOP (HDOP) if PDOP isn't available.
     * Converts Float values to Double for consistent return type.
     * @param wp the WayPoint
     * @return the WayPoint's DOP value as Double (PDOP preferred, HDOP fallback)
     * @since 19387
     */
    static Double getGpsDop(WayPoint wp) {
        if (wp != null) {
            if (wp.attr.get(GpxConstants.PT_PDOP) != null) {
                if (wp.attr.get(GpxConstants.PT_PDOP) instanceof Float) {
                    Float pdopvalue = (Float) wp.attr.get(GpxConstants.PT_PDOP);
                    return pdopvalue.doubleValue();
                } else if (wp.attr.get(GpxConstants.PT_PDOP) instanceof Double) {
                    return (Double) wp.attr.get(GpxConstants.PT_PDOP);
                }
            } else if (wp.attr.get(GpxConstants.PT_HDOP) != null) {
                if (wp.attr.get(GpxConstants.PT_HDOP) instanceof Float) {
                    Float hdopvalue = (Float) wp.attr.get(GpxConstants.PT_HDOP);
                    return hdopvalue.doubleValue();
                } else if (wp.attr.get(GpxConstants.PT_HDOP) instanceof Double) {
                    return (Double) wp.attr.get(GpxConstants.PT_HDOP);
                }
            }
        }
        return null;
    }

    /**
     * Gets the track direction angle value from a waypoint in a GNSS track.
     * This angle is the GNSS receiver movement direction.
     * @param wp the waypoint
     * @return the waypoint direction (in degrees)
     * @since 19387
     */
    static Double getGpsTrack(WayPoint wp) {
        if (wp != null) {
            String trackvalue = wp.getString(GpxConstants.PT_COURSE);
            Logging.debug("track angle value: {0}", trackvalue);
            if (!Utils.isEmpty(trackvalue)) {
                try {
                    return Double.valueOf(trackvalue);
                } catch (NumberFormatException e) {
                    Logging.warn(e);
                }
            }
        }
        return null;
    }

    /**
     * Determines the GPS processing method based on previous and current GNSS fix modes.
     * This method compares the positioning modes of the previous and current GNSS fixes,
     * selects the most basic processing method (lowest index in positioningModes list),
     * and formats the output string according to predefined conventions.
     * Because the returned processing method depends on a time correlation between an image
     * and a waypoint timestamp, the term 'CORRELATION' is added. 
     * @param prevGpsFixMode the previous GNSS fix mode (e.g., "SINGLE", "DGNSS", "RTK_FIX")
     * @param curGpsFixMode the current GNSS fix mode
     * @param positioningModes list of positioning modes ordered by accuracy
     * @return formatted processing method string
     * @since 19387
     */
    static String getGpsProcMethod(String prevGpsFixMode, final String curGpsFixMode,
                                    final List<String> positioningModes) {
        String gpsProcMethod = null;
        Integer lowestProcIndex = null;
        int lowestGnssModeIdx = 3; // 2d or higher index in positioningModes list are Gnss methods
        try {
            lowestProcIndex = Math.min(positioningModes.indexOf(prevGpsFixMode), positioningModes.indexOf(curGpsFixMode));
            if (lowestProcIndex < 0) {
                return null;
            }
            gpsProcMethod = "GNSS" + " " + positioningModes.get(lowestProcIndex).toUpperCase(Locale.ENGLISH) + " " + "CORRELATION";
            if (lowestProcIndex < lowestGnssModeIdx) {
                gpsProcMethod = positioningModes.get(lowestProcIndex).toUpperCase(Locale.ENGLISH) + " " + "CORRELATION";
            } else {
                gpsProcMethod = "GNSS" + " " + positioningModes.get(lowestProcIndex).toUpperCase(Locale.ENGLISH) + " " + "CORRELATION";
            }
            gpsProcMethod = gpsProcMethod.replace("FLOAT RTK", "RTK_FLOAT");
            gpsProcMethod = gpsProcMethod.replace(" RTK ", " RTK_FIX ");
        } catch (ArrayIndexOutOfBoundsException ex) {
            Logging.warn(ex);
        }
        return gpsProcMethod;
    }

    /**
     * Determines if the GNSS mode is 2d or 3d, based on previous and current GNSS fix modes.
     * This method compares the positioning modes of the previous and current GNSS fixes,
     * selects the most basic processing method (lowest index in positioningModes list),
     * and return the lowest value between 2d or 3d mode, or null if it's not a gnss mode (e.g. estimated, manual).
     * @param prevGpsFixMode the previous GNSS mode
     * @param curGpsFixMode the current GNSS mode
     * @param positioningModes list of positioning modes ordered by accuracy
     * @return 2 for 2d, 3 for 3d, or null
     * @since 19387
     */
    static Integer getGps2d3dMode(String prevGpsFixMode, final String curGpsFixMode,
                                final List<String> positioningModes) {
        Integer lowestMode = null;
        lowestMode = Math.min(positioningModes.indexOf(prevGpsFixMode), positioningModes.indexOf(curGpsFixMode));
        if (lowestMode > 3) {
            return 3;
        }
        if (lowestMode > 2) {
            return 2;
        }
        return null;
    }

    // CHECKSTYLE.OFF: ParameterNumber
    private static int matchPoints(List<? extends GpxImageEntry>
                                        images,
                                        WayPoint prevWp,
                                        long prevWpTime,
                                        WayPoint curWp,
                                        long curWpTime,
                                        long offset,
                                        boolean interpolate,
                                        int tagTime,
                                        WayPoint nextWp,
                                        GpxImageDirectionPositionSettings dirpos,
                                        GpxImageDatumSettings datumSettings) {

        final boolean isLast = nextWp == null;

        // i is the index of the timewise last photo that has the same or earlier EXIF time
        int i;
        if (isLast) {
            i = images.size() - 1;
        } else {
            i = getLastIndexOfListBefore(images, curWpTime);
        }

        if (Logging.isDebugEnabled()) {
            Logging.debug("Correlating images for i={1} - curWp={2}/{3} - prevWp={4}/{5} - nextWp={6} - tagTime={7} - interpolate={8}",
                    i, curWp, curWpTime, prevWp, prevWpTime, nextWp, tagTime, interpolate);
        }

        // no photos match
        if (i < 0) {
            Logging.debug("Correlated nothing, no photos match");
            return 0;
        }

        int ret = 0;
        Double speed = null;
        Double prevElevation = null;
        Double prevHPosErr = null;
        Double prevGpsDop = null;
        Double prevGpsTrack = null;
        String prevGpsFixMode = null;
        //list of differential GPS mode
        //TODO move these lists in Gpx.Constants?
        final List<String> diffMode = Arrays.asList("dgps", "float rtk", "rtk");
        final List<String> positioningModes = Arrays.asList("none", "manual", "estimated", "2d", "3d", "dgps", "float rtk", "rtk");


        if (prevWp != null && interpolate) {
            double distance = prevWp.greatCircleDistance(curWp);
            // This is in km/h, 3.6 * m/s
            if (curWpTime > prevWpTime) {
                speed = 3600 * distance / (curWpTime - prevWpTime);
            }
            prevElevation = getElevation(prevWp);
            prevHPosErr = getHPosErr(prevWp);
            prevGpsDop = getGpsDop(prevWp);
            prevGpsTrack = getGpsTrack(prevWp);
            prevGpsFixMode = prevWp.getString(GpxConstants.PT_FIX);
        }

        final Double curElevation = getElevation(curWp);
        final Double curHPosErr = getHPosErr(curWp);
        final Double curGpsDop = getGpsDop(curWp);
        final Double curGpsTrack = getGpsTrack(curWp);
        final String curGpsFixMode = curWp.getString(GpxConstants.PT_FIX);

        if (!interpolate || isLast) {
            final long half = Math.abs(curWpTime - prevWpTime) / 2;
            while (i >= 0) {
                final GpxImageEntry curImg = images.get(i);
                final GpxImageEntry curTmp = curImg.getTmp();
                final long time = curImg.getExifInstant().toEpochMilli();
                if ((!isLast && time > curWpTime) || time < prevWpTime) {
                    break;
                }
                long tagms = TimeUnit.MINUTES.toMillis(tagTime);
                if (!curTmp.hasNewGpsData() &&
                        (Math.abs(time - curWpTime) <= tagms
                        || Math.abs(prevWpTime - time) <= tagms)) {
                    if (prevWp != null && time < curWpTime - half) {
                        curTmp.setPos(prevWp.getCoor());
                    } else {
                        curTmp.setPos(curWp.getCoor());
                    }
                    //TODO fix this, nextWp doesn't exist here
                    if (nextWp != null && dirpos.isSetImageDirection()) {
                        double direction = curWp.bearing(nextWp);
                        curTmp.setExifImgDir(computeDirection(direction, dirpos.getImageDirectionAngleOffset()));
                    }
                    curTmp.setGpsTime(curImg.getExifInstant().minusMillis(offset));
                    curTmp.flagNewGpsData();
                    curImg.tmpUpdated();

                    ret++;
                }
                i--;
            }
        } else if (prevWp != null) {
            // This code gives a simple linear interpolation of the coordinates between current and
            // previous track point assuming a constant speed in between
            @SuppressWarnings("null")
            LatLon nextCoorForDirection = nextWp.getCoor();
            while (i >= 0) {
                final GpxImageEntry curImg = images.get(i);
                final long imgTime = curImg.getExifInstant().toEpochMilli();
                if (imgTime < prevWpTime) {
                    break;
                }
                final GpxImageEntry curTmp = curImg.getTmp();
                if (!curTmp.hasNewGpsData()) {
                    // The values of timeDiff are between 0 and 1, it is not seconds but a dimensionless variable
                    final double timeDiff = (double) (imgTime - prevWpTime) / Math.abs(curWpTime - prevWpTime);
                    final boolean shiftXY = dirpos.getShiftImageX() != 0d || dirpos.getShiftImageY() != 0d;
                    final LatLon prevCoor = prevWp.getCoor();
                    final LatLon curCoor = curWp.getCoor();
                    LatLon position = prevCoor.interpolate(curCoor, timeDiff);
                    if (nextCoorForDirection != null && (shiftXY || dirpos.isSetImageDirection())) {
                        double direction = position.bearing((ILatLon) nextCoorForDirection);
                        if (dirpos.isSetImageDirection()) {
                            curTmp.setExifImgDir(computeDirection(direction, dirpos.getImageDirectionAngleOffset()));
                        }
                        if (shiftXY) {
                            final Projection proj = ProjectionRegistry.getProjection();
                            final double offsetX = dirpos.getShiftImageX();
                            final double offsetY = dirpos.getShiftImageY();
                            final double r = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
                            final double orientation = (direction + LatLon.ZERO.bearing((ILatLon) new LatLon(offsetX, offsetY))) % (2 * Math.PI);
                            position = proj.eastNorth2latlon(proj.latlon2eastNorth(position)
                                    .add(r * Math.sin(orientation), r * Math.cos(orientation)));
                        }
                    }
                    curTmp.setPos(position);
                    curTmp.setSpeed(speed);
                    if (curElevation != null && prevElevation != null) {
                        curTmp.setElevation(prevElevation + (curElevation - prevElevation) * timeDiff + dirpos.getElevationShift());
                    }

                    // Add exif GpsHPositioningerror interpolated value
                    if (curHPosErr != null && prevHPosErr != null) {
                        Double interpolatedValue = prevHPosErr + (curHPosErr - prevHPosErr) * timeDiff;
                        curTmp.setExifHPosErr(Math.round(interpolatedValue*10000)/10000.0);
                    }

                    // Add exif GpsDifferentialMode
                    // Get previous and current waypoint differential. As no interpolation is possible,
                    // set differential mode to 0 if any waypoint isn't in differential mode.
                    if (prevGpsFixMode != null) {
                        if (diffMode.contains(prevGpsFixMode) && diffMode.contains(curGpsFixMode)) {
                            curTmp.setGpsDiffMode(1);
                        } else {
                            curTmp.setGpsDiffMode(0);
                        }
                    }

                    // Add exif GpsMeasureMode
                    if (prevGpsFixMode != null && curGpsFixMode != null) {
                        Integer gps2d3dMode = getGps2d3dMode(prevGpsFixMode, curGpsFixMode, positioningModes);
                        if (gps2d3dMode != null) {
                            curTmp.setGps2d3dMode(gps2d3dMode);
                        }
                    }
                    
                    // Add exif GpsProcessingMethod. As no interpolation is possible,
                    // set processing method to the "lowest" previous and current processing method value.
                    if (prevGpsFixMode != null && curGpsFixMode != null) {
                        String gpsProcMethod = getGpsProcMethod(prevGpsFixMode, curGpsFixMode, positioningModes);                       
                        if (gpsProcMethod != null) {
                            curTmp.setExifGpsProcMethod(gpsProcMethod);
                        }
                    }

                    // Add Exif GpsDop with interpolated GPS DOP value
                    if (curGpsDop != null && prevGpsDop != null) {
                        Double interpolatedValue = prevGpsDop + (curGpsDop - prevGpsDop) * timeDiff;
                        curTmp.setExifGpsDop(Math.round(interpolatedValue*100)/100.0);
                    }

                    // Add Exif GpsTrack tag
                    if (dirpos.isSetGpxTrackDirection()) {
                        if (curGpsTrack != null && prevGpsTrack != null) {
                            curTmp.setExifGpsTrack(prevGpsTrack + (curGpsTrack - prevGpsTrack) * timeDiff);
                        }
                    }
                    
                    // Add GpsDatum tag
                    if (datumSettings.isSetImageGpsDatum()) {
                        if (diffMode.contains(prevGpsFixMode) && diffMode.contains(curGpsFixMode)) {
                            curTmp.setExifGpsDatum(datumSettings.getImageGpsDatum());
                        } else //without differential mode, datum is WGS-84
                            curTmp.setExifGpsDatum("WGS-84");
                    }

                    curTmp.setGpsTime(curImg.getExifInstant().minusMillis(offset));
                    curTmp.flagNewGpsData();
                    curImg.tmpUpdated();

                    nextCoorForDirection = curCoor;
                    ret++;
                }
                i--;
            }
        }
        Logging.debug("Correlated {0} image(s)", ret);
        return ret;
    }
    // CHECKSTYLE.ON: ParameterNumber

    /**
     * Computes an adjusted direction by applying an angular offset and normalizing the result between 0° and 360°.
     * @param direction initial direction angle (in radians)
     * @param angleOffset angular offset to apply (in degrees)
     * @return resulting direction normalized to the range [0, 360) degrees
     */
    private static double computeDirection(double direction, double angleOffset) {
        return (Utils.toDegrees(direction) + angleOffset) % 360d;
    }

    /**
     * Finds the last image in the sorted image list which is before the given time
     * @param images list of images
     * @param searchTime time to search
     * @return index of last image before given time
     */
    private static int getLastIndexOfListBefore(List<? extends GpxImageEntry> images, long searchedTime) {
        int lstSize = images.size();

        // No photos or the first photo taken is later than the search period
        if (lstSize == 0 || searchedTime < images.get(0).getExifInstant().toEpochMilli())
            return -1;

        // The search period is later than the last photo
        if (searchedTime > images.get(lstSize - 1).getExifInstant().toEpochMilli())
            return lstSize-1;

        // The searched index is somewhere in the middle, do a binary search from the beginning
        int curIndex;
        int startIndex = 0;
        int endIndex = lstSize-1;
        while (endIndex - startIndex > 1) {
            curIndex = (endIndex + startIndex) / 2;
            if (searchedTime > images.get(curIndex).getExifInstant().toEpochMilli()) {
                startIndex = curIndex;
            } else {
                endIndex = curIndex;
            }
        }
        if (searchedTime < images.get(endIndex).getExifInstant().toEpochMilli())
            return startIndex;

        // This final loop is to check if photos with the exact same EXIF time follows
        while ((endIndex < (lstSize - 1)) && (images.get(endIndex).getExifInstant().toEpochMilli()
                == images.get(endIndex + 1).getExifInstant().toEpochMilli())) {
            endIndex++;
        }
        return endIndex;
    }
}
