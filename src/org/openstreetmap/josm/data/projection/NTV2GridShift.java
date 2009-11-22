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
package org.openstreetmap.josm.data.projection;

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
     * @return
     */
    public double getLatSeconds() {
        return lat;
    }

    /**
     * @return
     */
    public double getLatDegrees() {
        return lat / 3600.0;
    }

    /**
     * @return
     */
    public double getLatShiftSeconds() {
        return latShift;
    }

    /**
     * @return
     */
    public double getLatShiftDegrees() {
        return latShift / 3600.0;
    }

    /**
     * @return
     */
    public double getShiftedLatSeconds() {
        return lat + latShift;
    }

    /**
     * @return
     */
    public double getShiftedLatDegrees() {
        return (lat + latShift) / 3600.0;
    }

    /**
     * @return
     */
    public boolean isLatAccuracyAvailable() {
        return latAccuracyAvailable;
    }

    /**
     * @return
     */
    public double getLatAccuracySeconds() {
        if (!latAccuracyAvailable)
            throw new IllegalStateException("Latitude Accuracy not available");
        return latAccuracy;
    }

    /**
     * @return
     */
    public double getLatAccuracyDegrees() {
        if (!latAccuracyAvailable)
            throw new IllegalStateException("Latitude Accuracy not available");
        return latAccuracy / 3600.0;
    }

    /**
     * @return
     */
    public double getLatAccuracyMetres() {
        if (!latAccuracyAvailable)
            throw new IllegalStateException("Latitude Accuracy not available");
        return latAccuracy * METRE_PER_SECOND;
    }

    /**
     * @return
     */
    public double getLonPositiveWestSeconds() {
        return lon;
    }

    /**
     * @return
     */
    public double getLonPositiveEastDegrees() {
        return lon / -3600.0;
    }

    /**
     * @return
     */
    public double getLonShiftPositiveWestSeconds() {
        return lonShift;
    }

    /**
     * @return
     */
    public double getLonShiftPositiveEastDegrees() {
        return lonShift / -3600.0;
    }

    /**
     * @return
     */
    public double getShiftedLonPositiveWestSeconds() {
        return lon + lonShift;
    }

    /**
     * @return
     */
    public double getShiftedLonPositiveEastDegrees() {
        return (lon + lonShift) / -3600.0;
    }

    /**
     * @return
     */
    public boolean isLonAccuracyAvailable() {
        return lonAccuracyAvailable;
    }

    /**
     * @return
     */
    public double getLonAccuracySeconds() {
        if (!lonAccuracyAvailable)
            throw new IllegalStateException("Longitude Accuracy not available");
        return lonAccuracy;
    }

    /**
     * @return
     */
    public double getLonAccuracyDegrees() {
        if (!lonAccuracyAvailable)
            throw new IllegalStateException("Longitude Accuracy not available");
        return lonAccuracy / 3600.0;
    }

    /**
     * @return
     */
    public double getLonAccuracyMetres() {
        if (!lonAccuracyAvailable)
            throw new IllegalStateException("Longitude Accuracy not available");
        return lonAccuracy * METRE_PER_SECOND * Math.cos(RADIANS_PER_SECOND * lat);
    }

    /**
     * @param d
     */
    public void setLatSeconds(double d) {
        lat = d;
    }

    /**
     * @param d
     */
    public void setLatDegrees(double d) {
        lat = d * 3600.0;
    }

    /**
     * @param b
     */
    public void setLatAccuracyAvailable(boolean b) {
        latAccuracyAvailable = b;
    }

    /**
     * @param d
     */
    public void setLatAccuracySeconds(double d) {
        latAccuracy = d;
    }

    /**
     * @param d
     */
    public void setLatShiftSeconds(double d) {
        latShift = d;
    }

    /**
     * @param d
     */
    public void setLonPositiveWestSeconds(double d) {
        lon = d;
    }

    /**
     * @param d
     */
    public void setLonPositiveEastDegrees(double d) {
        lon = d * -3600.0;
    }

    /**
     * @param b
     */
    public void setLonAccuracyAvailable(boolean b) {
        lonAccuracyAvailable = b;
    }

    /**
     * @param d
     */
    public void setLonAccuracySeconds(double d) {
        lonAccuracy = d;
    }

    /**
     * @param d
     */
    public void setLonShiftPositiveWestSeconds(double d) {
        lonShift = d;
    }

    /**
     * @return
     */
    public String getSubGridName() {
        return subGridName;
    }

    /**
     * @param string
     */
    public void setSubGridName(String string) {
        subGridName = string;
    }

    /**
     * Make this object a copy of the supplied GridShift
     * @param gs
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
