// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                                totalDist += wps.get(j - 1).getCoor().greatCircleDistance(wps.get(j).getCoor());
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
                                    prevWp.getCoor().greatCircleDistance(curWp.getCoor()) > trkDist) {
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
                                    prevWp.getCoor().greatCircleDistance(curWp.getCoor()) > segDist) {
                                interpolate = false;
                                if (segTag) {
                                    tagTime = segTagTime;
                                }
                            }
                        }
                    }
                    WayPoint nextWp = i < size - 1 ? wps.get(i + 1) : null;
                    ret += matchPoints(images, prevWp, prevWpTime, curWp, curWpTime, offset, interpolate, tagTime, nextWp, dirpos);
                    prevWp = curWp;
                    prevWpTime = curWpTime;
                }
            }
        }
        if (trkTag && prevWp != null) {
            ret += matchPoints(images, prevWp, prevWpTime, prevWp, prevWpTime, offset, false, trkTagTime, null, dirpos);
        }
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

    private static int matchPoints(List<? extends GpxImageEntry> images, WayPoint prevWp, long prevWpTime, WayPoint curWp, long curWpTime,
            long offset, boolean interpolate, int tagTime, WayPoint nextWp, GpxImageDirectionPositionSettings dirpos) {

        int ret = 0;
        final boolean isLast = nextWp == null;

        // i is the index of the timewise last photo that has the same or earlier EXIF time
        int i;
        if (isLast) {
            i = images.size() - 1;
        } else {
            i = getLastIndexOfListBefore(images, curWpTime);
        }

        // no photos match
        if (i < 0)
            return 0;

        Double speed = null;
        Double prevElevation = null;

        if (prevWp != null && interpolate) {
            double distance = prevWp.getCoor().greatCircleDistance(curWp.getCoor());
            // This is in km/h, 3.6 * m/s
            if (curWpTime > prevWpTime) {
                speed = 3600 * distance / (curWpTime - prevWpTime);
            }
            prevElevation = getElevation(prevWp);
        }

        final Double curElevation = getElevation(curWp);

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
                    if (nextWp != null && dirpos.isSetImageDirection()) {
                        double direction = curWp.getCoor().bearing(nextWp.getCoor());
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
            while (i >= 0) {
                final GpxImageEntry curImg = images.get(i);
                final GpxImageEntry curTmp = curImg.getTmp();
                final long imgTime = curImg.getExifInstant().toEpochMilli();
                if (imgTime < prevWpTime) {
                    break;
                }
                if (!curTmp.hasNewGpsData()) {
                    // The values of timeDiff are between 0 and 1, it is not seconds but a dimensionless variable
                    final double timeDiff = (double) (imgTime - prevWpTime) / Math.abs(curWpTime - prevWpTime);
                    final boolean shiftXY = dirpos.getShiftImageX() != 0d || dirpos.getShiftImageY() != 0d;
                    final LatLon prevCoor = prevWp.getCoor();
                    final LatLon curCoor = curWp.getCoor();
                    LatLon position = prevCoor.interpolate(curCoor, timeDiff);
                    if (nextWp != null && (shiftXY || dirpos.isSetImageDirection())) {
                        double direction = curCoor.bearing(nextWp.getCoor());
                        if (dirpos.isSetImageDirection()) {
                            curTmp.setExifImgDir(computeDirection(direction, dirpos.getImageDirectionAngleOffset()));
                        }
                        if (shiftXY) {
                            final Projection proj = ProjectionRegistry.getProjection();
                            final double offsetX = dirpos.getShiftImageX();
                            final double offsetY = dirpos.getShiftImageY();
                            final double r = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
                            final double orientation = (direction + LatLon.ZERO.bearing(new LatLon(offsetX, offsetY))) % (2 * Math.PI);
                            position = proj.eastNorth2latlon(proj.latlon2eastNorth(position)
                                    .add(r * Math.sin(orientation), r * Math.cos(orientation)));
                        }
                    }
                    curTmp.setPos(position);
                    curTmp.setSpeed(speed);
                    if (curElevation != null && prevElevation != null) {
                        curTmp.setElevation(prevElevation + (curElevation - prevElevation) * timeDiff + dirpos.getElevationShift());
                    }
                    curTmp.setGpsTime(curImg.getExifInstant().minusMillis(offset));
                    curTmp.flagNewGpsData();
                    curImg.tmpUpdated();

                    ret++;
                }
                i--;
            }
        }
        return ret;
    }

    private static double computeDirection(double direction, double angleOffset) {
        return (Utils.toDegrees(direction) + angleOffset) % 360d;
    }

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
