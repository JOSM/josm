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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Models the NTv2 Sub Grid within a Grid Shift File.
 *
 * @author Peter Yuill
 * Modified for JOSM :
 * - removed the RandomAccessFile mode (Pieren)
 * - read grid file by single bytes. Workaround for a bug in some VM not supporting
 *   file reading by group of 4 bytes from a jar file.
 * - removed the Cloneable interface
 * @since 2507
 */
public class NTV2SubGrid implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String subGridName;
    private final String parentSubGridName;
    private final String created;
    private final String updated;
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final double latInterval;
    private final double lonInterval;
    private final int nodeCount;

    private final int lonColumnCount;
    private final int latRowCount;
    private final float[] latShift;
    private final float[] lonShift;
    private float[] latAccuracy;
    private float[] lonAccuracy;

    private NTV2SubGrid[] subGrid;

    /**
     * Construct a Sub Grid from an InputStream, loading the node data into
     * arrays in this object.
     *
     * @param in GridShiftFile InputStream
     * @param bigEndian is the file bigEndian?
     * @param loadAccuracy is the node Accuracy data to be loaded?
     * @throws IOException if any I/O error occurs
     */
    public NTV2SubGrid(InputStream in, boolean bigEndian, boolean loadAccuracy) throws IOException {
        byte[] b8 = new byte[8];
        byte[] b4 = new byte[4];
        byte[] b1 = new byte[1];
        readBytes(in, b8);
        readBytes(in, b8);
        subGridName = new String(b8, StandardCharsets.UTF_8).trim();
        readBytes(in, b8);
        readBytes(in, b8);
        parentSubGridName = new String(b8, StandardCharsets.UTF_8).trim();
        readBytes(in, b8);
        readBytes(in, b8);
        created = new String(b8, StandardCharsets.UTF_8);
        readBytes(in, b8);
        readBytes(in, b8);
        updated = new String(b8, StandardCharsets.UTF_8);
        readBytes(in, b8);
        readBytes(in, b8);
        minLat = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        maxLat = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        minLon = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        maxLon = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        latInterval = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        lonInterval = NTV2Util.getDouble(b8, bigEndian);
        lonColumnCount = 1 + (int) ((maxLon - minLon) / lonInterval);
        latRowCount = 1 + (int) ((maxLat - minLat) / latInterval);
        readBytes(in, b8);
        readBytes(in, b8);
        nodeCount = NTV2Util.getInt(b8, bigEndian);
        if (nodeCount != lonColumnCount * latRowCount)
            throw new IllegalStateException("SubGrid " + subGridName + " has inconsistent grid dimesions");
        latShift = new float[nodeCount];
        lonShift = new float[nodeCount];
        if (loadAccuracy) {
            latAccuracy = new float[nodeCount];
            lonAccuracy = new float[nodeCount];
        }

        for (int i = 0; i < nodeCount; i++) {
            // Read the grid file byte after byte. This is a workaround about a bug in
            // certain VM which are not able to read byte blocks when the resource file is in a .jar file (Pieren)
            readBytes(in, b1); b4[0] = b1[0];
            readBytes(in, b1); b4[1] = b1[0];
            readBytes(in, b1); b4[2] = b1[0];
            readBytes(in, b1); b4[3] = b1[0];
            latShift[i] = NTV2Util.getFloat(b4, bigEndian);
            readBytes(in, b1); b4[0] = b1[0];
            readBytes(in, b1); b4[1] = b1[0];
            readBytes(in, b1); b4[2] = b1[0];
            readBytes(in, b1); b4[3] = b1[0];
            lonShift[i] = NTV2Util.getFloat(b4, bigEndian);
            readBytes(in, b1); b4[0] = b1[0];
            readBytes(in, b1); b4[1] = b1[0];
            readBytes(in, b1); b4[2] = b1[0];
            readBytes(in, b1); b4[3] = b1[0];
            if (loadAccuracy) {
                latAccuracy[i] = NTV2Util.getFloat(b4, bigEndian);
            }
            readBytes(in, b1); b4[0] = b1[0];
            readBytes(in, b1); b4[1] = b1[0];
            readBytes(in, b1); b4[2] = b1[0];
            readBytes(in, b1); b4[3] = b1[0];
            if (loadAccuracy) {
                lonAccuracy[i] = NTV2Util.getFloat(b4, bigEndian);
            }
        }
    }

    private static void readBytes(InputStream in, byte[] b) throws IOException {
        if (in.read(b) < b.length) {
            Logging.error("Failed to read expected amount of bytes ("+ b.length +") from stream");
        }
    }

    /**
     * Tests if a specified coordinate is within this Sub Grid
     * or one of its Sub Grids. If the coordinate is outside
     * this Sub Grid, null is returned. If the coordinate is
     * within this Sub Grid, but not within any of its Sub Grids,
     * this Sub Grid is returned. If the coordinate is within
     * one of this Sub Grid's Sub Grids, the method is called
     * recursively on the child Sub Grid.
     *
     * @param lon Longitude in Positive West Seconds
     * @param lat Latitude in Seconds
     * @return the Sub Grid containing the Coordinate or null
     */
    public NTV2SubGrid getSubGridForCoord(double lon, double lat) {
        return !isCoordWithin(lon, lat)
                ? null
                : subGrid == null
                ? this
                : Arrays.stream(subGrid)
                .filter(aSubGrid -> aSubGrid.isCoordWithin(lon, lat))
                .map(aSubGrid -> aSubGrid.getSubGridForCoord(lon, lat))
                .filter(Objects::nonNull)
                .findFirst().orElse(this);
    }

    /**
     * Tests if a specified coordinate is within this Sub Grid.
     * A coordinate on either outer edge (maximum Latitude or
     * maximum Longitude) is deemed to be outside the grid.
     *
     * @param lon Longitude in Positive West Seconds
     * @param lat Latitude in Seconds
     * @return true or false
     */
    private boolean isCoordWithin(double lon, double lat) {
        return (lon >= minLon) && (lon < maxLon) && (lat >= minLat) && (lat < maxLat);
    }

    /**
     * Bi-Linear interpolation of four nearest node values as described in
     * 'GDAit Software Architecture Manual' produced by the <a
     * href='http://www.dtpli.vic.gov.au/property-and-land-titles/geodesy/geocentric-datum-of-australia-1994-gda94/gda94-useful-tools'>
     * Geomatics Department of the University of Melbourne</a>
     * @param a value at the A node
     * @param b value at the B node
     * @param c value at the C node
     * @param d value at the D node
     * @param x Longitude factor
     * @param y Latitude factor
     * @return interpolated value
     */
    private static double interpolate(float a, float b, float c, float d, double x, double y) {
        return a + (((double) b - (double) a) * x) + (((double) c - (double) a) * y) +
        (((double) a + (double) d - b - c) * x * y);
    }

    /**
     * Interpolate shift and accuracy values for a coordinate in the 'from' datum
     * of the GridShiftFile. The algorithm is described in
     * 'GDAit Software Architecture Manual' produced by the <a
     * href='http://www.dtpli.vic.gov.au/property-and-land-titles/geodesy/geocentric-datum-of-australia-1994-gda94/gda94-useful-tools'>
     * Geomatics Department of the University of Melbourne</a>
     * <p>This method is thread safe for both memory based and file based node data.
     * @param gs GridShift object containing the coordinate to shift and the shift values
     */
    public void interpolateGridShift(NTV2GridShift gs) {
        int lonIndex = (int) ((gs.getLonPositiveWestSeconds() - minLon) / lonInterval);
        int latIndex = (int) ((gs.getLatSeconds() - minLat) / latInterval);

        double x = (gs.getLonPositiveWestSeconds() - (minLon + (lonInterval * lonIndex))) / lonInterval;
        double y = (gs.getLatSeconds() - (minLat + (latInterval * latIndex))) / latInterval;

        // Find the nodes at the four corners of the cell

        int indexA = lonIndex + (latIndex * lonColumnCount);
        int indexB = indexA + 1;
        int indexC = indexA + lonColumnCount;
        int indexD = indexC + 1;

        gs.setLonShiftPositiveWestSeconds(interpolate(
                lonShift[indexA], lonShift[indexB], lonShift[indexC], lonShift[indexD], x, y));

        gs.setLatShiftSeconds(interpolate(
                latShift[indexA], latShift[indexB], latShift[indexC], latShift[indexD], x, y));

        if (lonAccuracy == null) {
            gs.setLonAccuracyAvailable(false);
        } else {
            gs.setLonAccuracyAvailable(true);
            gs.setLonAccuracySeconds(interpolate(
                    lonAccuracy[indexA], lonAccuracy[indexB], lonAccuracy[indexC], lonAccuracy[indexD], x, y));
        }

        if (latAccuracy == null) {
            gs.setLatAccuracyAvailable(false);
        } else {
            gs.setLatAccuracyAvailable(true);
            gs.setLatAccuracySeconds(interpolate(
                    latAccuracy[indexA], latAccuracy[indexB], latAccuracy[indexC], latAccuracy[indexD], x, y));
        }
    }

    /**
     * Returns the parent sub grid name.
     * @return the parent sub grid name
     */
    public String getParentSubGridName() {
        return parentSubGridName;
    }

    /**
     * Returns the sub grid name.
     * @return the sub grid name
     */
    public String getSubGridName() {
        return subGridName;
    }

    /**
     * Returns the node count.
     * @return the node count
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Returns the sub grid count.
     * @return the sub grid count
     */
    public int getSubGridCount() {
        return subGrid == null ? 0 : subGrid.length;
    }

    /**
     * Set an array of Sub Grids of this sub grid
     * @param subGrid subgrids
     */
    public void setSubGridArray(NTV2SubGrid... subGrid) {
        this.subGrid = Utils.copyArray(subGrid);
    }

    @Override
    public String toString() {
        return subGridName;
    }

    /**
     * Returns textual details about the sub grid.
     * @return textual details about the sub grid
     */
    public String getDetails() {
        return new StringBuilder(256)
            .append("Sub Grid : ")
            .append(subGridName)
            .append("\nParent   : ")
            .append(parentSubGridName)
            .append("\nCreated  : ")
            .append(created)
            .append("\nUpdated  : ")
            .append(updated)
            .append("\nMin Lat  : ")
            .append(minLat)
            .append("\nMax Lat  : ")
            .append(maxLat)
            .append("\nMin Lon  : ")
            .append(minLon)
            .append("\nMax Lon  : ")
            .append(maxLon)
            .append("\nLat Intvl: ")
            .append(latInterval)
            .append("\nLon Intvl: ")
            .append(lonInterval)
            .append("\nNode Cnt : ")
            .append(nodeCount)
            .toString();
    }

    /**
     * Get maximum latitude value
     * @return maximum latitude
     */
    public double getMaxLat() {
        return maxLat;
    }

    /**
     * Get maximum longitude value
     * @return maximum longitude
     */
    public double getMaxLon() {
        return maxLon;
    }

    /**
     * Get minimum latitude value
     * @return minimum latitude
     */
    public double getMinLat() {
        return minLat;
    }

    /**
     * Get minimum longitude value
     * @return minimum longitude
     */
    public double getMinLon() {
        return minLon;
    }
}
