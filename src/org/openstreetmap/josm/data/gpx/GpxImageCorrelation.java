// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * Correlation logic for {@code CorrelateGpxWithImages}.
 * @since 14205
 */
public final class GpxImageCorrelation {

    private GpxImageCorrelation() {
        // Hide public constructor
    }

    /**
     * Match a list of photos to a gpx track with a given offset.
     * All images need a exifTime attribute and the List must be sorted according to these times.
     * @param images images to match
     * @param selectedGpx selected GPX data
     * @param offset offset
     * @param forceTags force tagging of all photos, otherwise prefs are used
     * @return number of matched points
     */
    public static int matchGpxTrack(List<? extends GpxImageEntry> images, GpxData selectedGpx, long offset, boolean forceTags) {
        int ret = 0;

        long prevWpTime = 0;
        WayPoint prevWp = null;

        List<List<List<WayPoint>>> trks = new ArrayList<>();

        for (GpxTrack trk : selectedGpx.tracks) {
            List<List<WayPoint>> segs = new ArrayList<>();
            for (GpxTrackSegment seg : trk.getSegments()) {
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

        boolean trkInt, trkTag, segInt, segTag;
        int trkTime, trkDist, trkTagTime, segTime, segDist, segTagTime;

        if (forceTags) { //temporary option to override advanced settings and activate all possible interpolations / tagging methods
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
        boolean isFirst = true;

        for (int t = 0; t < trks.size(); t++) {
            List<List<WayPoint>> segs = trks.get(t);
            for (int s = 0; s < segs.size(); s++) {
                List<WayPoint> wps = segs.get(s);
                for (int i = 0; i < wps.size(); i++) {
                    WayPoint curWp = wps.get(i);
                    // Interpolate timestamps in the segment, if one or more waypoints miss them
                    if (!curWp.hasDate()) {
                        //check if any of the following waypoints has a timestamp...
                        if (i > 0 && wps.get(i - 1).hasDate()) {
                            long prevWpTimeNoOffset = wps.get(i - 1).getTimeInMillis();
                            double totalDist = 0;
                            List<Pair<Double, WayPoint>> nextWps = new ArrayList<>();
                            for (int j = i; j < wps.size(); j++) {
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
                        if (s == 0) { //First segment of the track, so apply settings for tracks
                            if (!trkInt || isFirst || prevWp == null ||
                                    Math.abs(curWpTime - prevWpTime) > TimeUnit.MINUTES.toMillis(trkTime) ||
                                    prevWp.getCoor().greatCircleDistance(curWp.getCoor()) > trkDist) {
                                isFirst = false;
                                interpolate = false;
                                if (trkTag) {
                                    tagTime = trkTagTime;
                                }
                            }
                        } else { //Apply settings for segments
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
                    ret += matchPoints(images, prevWp, prevWpTime, curWp, curWpTime, offset, interpolate, tagTime, false);
                    prevWp = curWp;
                    prevWpTime = curWpTime;
                }
            }
        }
        if (trkTag) {
            ret += matchPoints(images, prevWp, prevWpTime, prevWp, prevWpTime, offset, false, trkTagTime, true);
        }
        return ret;
    }

    static Double getElevation(WayPoint wp) {
        if (wp != null) {
            String value = wp.getString(GpxConstants.PT_ELE);
            if (value != null && !value.isEmpty()) {
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
            long offset, boolean interpolate, int tagTime, boolean isLast) {

        int ret = 0;

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

        Double curElevation = getElevation(curWp);

        if (!interpolate || isLast) {
            final long half = Math.abs(curWpTime - prevWpTime) / 2;
            while (i >= 0) {
                final GpxImageEntry curImg = images.get(i);
                final GpxImageEntry curTmp = curImg.getTmp();
                final long time = curImg.getExifTime().getTime();
                if ((!isLast && time > curWpTime) || time < prevWpTime) {
                    break;
                }
                long tagms = TimeUnit.MINUTES.toMillis(tagTime);
                if (curTmp.getPos() == null &&
                        (Math.abs(time - curWpTime) <= tagms
                        || Math.abs(prevWpTime - time) <= tagms)) {
                    if (prevWp != null && time < curWpTime - half) {
                        curTmp.setPos(prevWp.getCoor());
                    } else {
                        curTmp.setPos(curWp.getCoor());
                    }
                    curTmp.setGpsTime(new Date(curImg.getExifTime().getTime() - offset));
                    curTmp.flagNewGpsData();
                    ret++;
                }
                i--;
            }
        } else if (prevWp != null) {
            // This code gives a simple linear interpolation of the coordinates between current and
            // previous track point assuming a constant speed in between
            while (i >= 0) {
                GpxImageEntry curImg = images.get(i);
                GpxImageEntry curTmp = curImg.getTmp();
                final long imgTime = curImg.getExifTime().getTime();
                if (imgTime < prevWpTime) {
                    break;
                }
                if (curTmp.getPos() == null) {
                    // The values of timeDiff are between 0 and 1, it is not seconds but a dimensionless variable
                    double timeDiff = (double) (imgTime - prevWpTime) / Math.abs(curWpTime - prevWpTime);
                    curTmp.setPos(prevWp.getCoor().interpolate(curWp.getCoor(), timeDiff));
                    curTmp.setSpeed(speed);
                    if (curElevation != null && prevElevation != null) {
                        curTmp.setElevation(prevElevation + (curElevation - prevElevation) * timeDiff);
                    }
                    curTmp.setGpsTime(new Date(curImg.getExifTime().getTime() - offset));
                    curTmp.flagNewGpsData();

                    ret++;
                }
                i--;
            }
        }
        return ret;
    }

    private static int getLastIndexOfListBefore(List<? extends GpxImageEntry> images, long searchedTime) {
        int lstSize = images.size();

        // No photos or the first photo taken is later than the search period
        if (lstSize == 0 || searchedTime < images.get(0).getExifTime().getTime())
            return -1;

        // The search period is later than the last photo
        if (searchedTime > images.get(lstSize - 1).getExifTime().getTime())
            return lstSize-1;

        // The searched index is somewhere in the middle, do a binary search from the beginning
        int curIndex;
        int startIndex = 0;
        int endIndex = lstSize-1;
        while (endIndex - startIndex > 1) {
            curIndex = (endIndex + startIndex) / 2;
            if (searchedTime > images.get(curIndex).getExifTime().getTime()) {
                startIndex = curIndex;
            } else {
                endIndex = curIndex;
            }
        }
        if (searchedTime < images.get(endIndex).getExifTime().getTime())
            return startIndex;

        // This final loop is to check if photos with the exact same EXIF time follows
        while ((endIndex < (lstSize - 1)) && (images.get(endIndex).getExifTime().getTime()
                == images.get(endIndex + 1).getExifTime().getTime())) {
            endIndex++;
        }
        return endIndex;
    }
}
