/*
 * Copyright (c) 2003 Objectix Pty Ltd  All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL OBJECTIX PTY LTD BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.data.projection.datum;

import java.io.Serializable;

import org.openstreetmap.josm.data.coor.LatLon;

/**
 * A value object for storing Longitude and Latitude of a point, the
 * Lon and Lat shift values to get from one datum to another, and the
 * Lon and Lat accuracy of the shift values.
 * <p>All values are stored as Positive West Seconds, but accessors
 * are also provided for Positive East Degrees.
 *
 * @author Peter Yuill
 * Modifified for JOSM :
 * - add a constructor for JOSM LatLon (Pieren)
 */
public class NTV2GridShift implements Serializable {

    private static final double METRE_PER_SECOND = 2.0 * Math.PI * 6378137.0 / 3600.0 / 360.0;
    private static final double RADIANS_PER_SECOND = 2.0 * Math.PI / 3600.0 / 360.0;
    private double lon;
    private double lat;
    private double lonShift;
    private double latShift;
    private double lonAccuracy;
    private double latAccuracy;
    boolean latAccuracyAvailable;
    boolean lonAccuracyAvailable;
    private String subGridName;

    public NTV2GridShift() {
    }

    public NTV2GridShift(LatLon p) {
        setLatDegrees(p.lat());
        setLonPositiveEastDegrees(p.lon());
    }

    /**
     * Data access function for latitude value
     * @return latitude in seconds
     */
    public double getLatSeconds() {
        return lat;
    }

    /**
     * Data access function for latitude value
     * @return latitude in degree
     */
    public double getLatDegrees() {
        return lat / 3600.0;
    }

    /**
     * Data access function for latitude shift value
     * @return latitude shift in seconds
     */
    public double getLatShiftSeconds() {
        return latShift;
    }

    /**
     * Data access function for latitude shift value
     * @return latitude shift in degree
     */
    public double getLatShiftDegrees() {
        return latShift / 3600.0;
    }

    /**
     * Data access function for already shifted latitude value
     * @return shifted latitude in seconds
     */
    public double getShiftedLatSeconds() {
        return lat + latShift;
    }

    /**
     * Data access function for already shifted latitude value
     * @return shifted latitude in degree
     */
    public double getShiftedLatDegrees() {
        return (lat + latShift) / 3600.0;
    }

    /**
     * Checks whether latitude accuracy is available or not
     * @return <code>true</code> if latitude accuracy is available
     */
    public boolean isLatAccuracyAvailable() {
        return latAccuracyAvailable;
    }

    /**
     * Data access function for latitude accuracy
     * @return latitude accuracy in seconds
     */
    public double getLatAccuracySeconds() {
        if (!latAccuracyAvailable)
            throw new IllegalStateException("Latitude Accuracy not available");
        return latAccuracy;
    }

    /**
     * Data access function for latitude accuracy
     * @return latitude accuracy in degree
     */
    public double getLatAccuracyDegrees() {
        if (!latAccuracyAvailable)
            throw new IllegalStateException("Latitude Accuracy not available");
        return latAccuracy / 3600.0;
    }

    /**
     * Data access function for latitude accuracy
     * @return latitude accuracy in meter
     */
    public double getLatAccuracyMetres() {
        if (!latAccuracyAvailable)
            throw new IllegalStateException("Latitude Accuracy not available");
        return latAccuracy * METRE_PER_SECOND;
    }

    /**
     * Data access function for longitude value, positive values in west direction
     * @return longitude in seconds
     */
    public double getLonPositiveWestSeconds() {
        return lon;
    }

    /**
     * Data access function for longitude value, positive values in east direction
     * @return longitude in degree
     */
    public double getLonPositiveEastDegrees() {
        return lon / -3600.0;
    }

    /**
     * Data access function for longitude shift value, positive values in west direction
     * @return longitude shift in seconds
     */
    public double getLonShiftPositiveWestSeconds() {
        return lonShift;
    }

    /**
     * Data access function for longitude shift value, positive values in east direction
     * @return longitude shift in degree
     */
    public double getLonShiftPositiveEastDegrees() {
        return lonShift / -3600.0;
    }

    /**
     * Data access function for shifted longitude value, positive values in west direction
     * @return shifted longitude in seconds
     */
    public double getShiftedLonPositiveWestSeconds() {
        return lon + lonShift;
    }

    /**
     * Data access function for shifted longitude value, positive values in east direction
     * @return shifted longitude in degree
     */
    public double getShiftedLonPositiveEastDegrees() {
        return (lon + lonShift) / -3600.0;
    }

    /**
     * Checks whether longitude accuracy is available or not
     * @return <code>true</code> if longitude accuracy is available
     */
    public boolean isLonAccuracyAvailable() {
        return lonAccuracyAvailable;
    }

    /**
     * Data access function for longitude accuracy
     * @return longitude accuracy in seconds
     */
    public double getLonAccuracySeconds() {
        if (!lonAccuracyAvailable)
            throw new IllegalStateException("Longitude Accuracy not available");
        return lonAccuracy;
    }

    /**
     * Data access function for longitude accuracy
     * @return longitude accuracy in degree
     */
    public double getLonAccuracyDegrees() {
        if (!lonAccuracyAvailable)
            throw new IllegalStateException("Longitude Accuracy not available");
        return lonAccuracy / 3600.0;
    }

    /**
     * Data access function for longitude accuracy
     * @return longitude accuracy in meter
     */
    public double getLonAccuracyMetres() {
        if (!lonAccuracyAvailable)
            throw new IllegalStateException("Longitude Accuracy not available");
        return lonAccuracy * METRE_PER_SECOND * Math.cos(RADIANS_PER_SECOND * lat);
    }

    /**
     * Data store function for latitude
     * @param d latitude value in seconds
     */
    public void setLatSeconds(double d) {
        lat = d;
    }

    /**
     * Data store function for latitude
     * @param d latitude value in degree
     */
    public void setLatDegrees(double d) {
        lat = d * 3600.0;
    }

    /**
     * Data store function for latitude accuracy availability
     * @param b availability of latitude accuracy
     */
    public void setLatAccuracyAvailable(boolean b) {
        latAccuracyAvailable = b;
    }

    /**
     * Data store function for latitude accuracy
     * @param d latitude accuracy in seconds
     */
    public void setLatAccuracySeconds(double d) {
        latAccuracy = d;
    }

    /**
     * Data store function for latitude shift
     * @param d latitude shift in seconds
     */
    public void setLatShiftSeconds(double d) {
        latShift = d;
    }

    /**
     * Data store function for longitude
     * @param d latitude value in seconds, west direction is positive
     */
    public void setLonPositiveWestSeconds(double d) {
        lon = d;
    }

    /**
     * Data store function for longitude
     * @param d latitude value in degree, est direction is positive
     */
    public void setLonPositiveEastDegrees(double d) {
        lon = d * -3600.0;
    }

    /**
     * Data store function for longitude accuracy availability
     * @param b availability of longitude accuracy
     */
    public void setLonAccuracyAvailable(boolean b) {
        lonAccuracyAvailable = b;
    }

    /**
     * Data store function for longitude accuracy
     * @param d longitude accuracy in seconds
     */
    public void setLonAccuracySeconds(double d) {
        lonAccuracy = d;
    }

    /**
     * Data store function for longitude shift value
     * @param d longitude shift in seconds, west direction is positive
     */
    public void setLonShiftPositiveWestSeconds(double d) {
        lonShift = d;
    }

    /**
     * Get the name of the sub grid
     * @return name of the sub grid
     */
    public String getSubGridName() {
        return subGridName;
    }

    /**
     * Set the name of the sub grid
     * @param string name of the sub grid
     */
    public void setSubGridName(String string) {
        subGridName = string;
    }

    /**
     * Make this object a copy of the supplied GridShift
     * @param gs grid to copy data from
     */
    public void copy(NTV2GridShift gs) {
        this.lon = gs.lon;
        this.lat = gs.lat;
        this.lonShift = gs.lonShift;
        this.latShift = gs.latShift;
        this.lonAccuracy = gs.lonAccuracy;
        this.latAccuracy = gs.latAccuracy;
        this.latAccuracyAvailable = gs.latAccuracyAvailable;
        this.lonAccuracyAvailable = gs.lonAccuracyAvailable;
        this.subGridName = gs.subGridName;
    }

}
