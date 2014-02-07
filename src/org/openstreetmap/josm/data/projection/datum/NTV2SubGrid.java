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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Utils;

/**
 * Models the NTv2 Sub Grid within a Grid Shift File
 *
 * @author Peter Yuill
 * Modified for JOSM :
 * - removed the RandomAccessFile mode (Pieren)
 * - read grid file by single bytes. Workaround for a bug in some VM not supporting
 *   file reading by group of 4 bytes from a jar file.
 */
public class NTV2SubGrid implements Cloneable, Serializable {

    private String subGridName;
    private String parentSubGridName;
    private String created;
    private String updated;
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    private double latInterval;
    private double lonInterval;
    private int nodeCount;

    private int lonColumnCount;
    private int latRowCount;
    private float[] latShift;
    private float[] lonShift;
    private float[] latAccuracy;
    private float[] lonAccuracy;

    boolean bigEndian;
    private NTV2SubGrid[] subGrid;

    /**
     * Construct a Sub Grid from an InputStream, loading the node data into
     * arrays in this object.
     *
     * @param in GridShiftFile InputStream
     * @param bigEndian is the file bigEndian?
     * @param loadAccuracy is the node Accuracy data to be loaded?
     * @throws IOException
     */
    public NTV2SubGrid(InputStream in, boolean bigEndian, boolean loadAccuracy) throws IOException {
        byte[] b8 = new byte[8];
        byte[] b4 = new byte[4];
        byte[] b1 = new byte[1];
        in.read(b8);
        in.read(b8);
        subGridName = new String(b8).trim();
        in.read(b8);
        in.read(b8);
        parentSubGridName = new String(b8).trim();
        in.read(b8);
        in.read(b8);
        created = new String(b8);
        in.read(b8);
        in.read(b8);
        updated = new String(b8);
        in.read(b8);
        in.read(b8);
        minLat = NTV2Util.getDouble(b8, bigEndian);
        in.read(b8);
        in.read(b8);
        maxLat = NTV2Util.getDouble(b8, bigEndian);
        in.read(b8);
        in.read(b8);
        minLon = NTV2Util.getDouble(b8, bigEndian);
        in.read(b8);
        in.read(b8);
        maxLon = NTV2Util.getDouble(b8, bigEndian);
        in.read(b8);
        in.read(b8);
        latInterval = NTV2Util.getDouble(b8, bigEndian);
        in.read(b8);
        in.read(b8);
        lonInterval = NTV2Util.getDouble(b8, bigEndian);
        lonColumnCount = 1 + (int)((maxLon - minLon) / lonInterval);
        latRowCount = 1 + (int)((maxLat - minLat) / latInterval);
        in.read(b8);
        in.read(b8);
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
            // certain VM which are not able to read byte blocks when the resource file is
            // in a .jar file (Pieren)
            in.read(b1); b4[0] = b1[0];
            in.read(b1); b4[1] = b1[0];
            in.read(b1); b4[2] = b1[0];
            in.read(b1); b4[3] = b1[0];
            latShift[i] = NTV2Util.getFloat(b4, bigEndian);
            in.read(b1); b4[0] = b1[0];
            in.read(b1); b4[1] = b1[0];
            in.read(b1); b4[2] = b1[0];
            in.read(b1); b4[3] = b1[0];
            lonShift[i] = NTV2Util.getFloat(b4, bigEndian);
            in.read(b1); b4[0] = b1[0];
            in.read(b1); b4[1] = b1[0];
            in.read(b1); b4[2] = b1[0];
            in.read(b1); b4[3] = b1[0];
            if (loadAccuracy) {
                latAccuracy[i] = NTV2Util.getFloat(b4, bigEndian);
            }
            in.read(b1); b4[0] = b1[0];
            in.read(b1); b4[1] = b1[0];
            in.read(b1); b4[2] = b1[0];
            in.read(b1); b4[3] = b1[0];
            if (loadAccuracy) {
                lonAccuracy[i] = NTV2Util.getFloat(b4, bigEndian);
            }
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
        if (isCoordWithin(lon, lat)) {
            if (subGrid == null)
                return this;
            else {
                for (NTV2SubGrid aSubGrid : subGrid) {
                    if (aSubGrid.isCoordWithin(lon, lat))
                        return aSubGrid.getSubGridForCoord(lon, lat);
                }
                return this;
            }
        } else
            return null;
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
        if ((lon >= minLon) && (lon < maxLon) && (lat >= minLat) && (lat < maxLat))
            return true;
        else
            return false;
    }

    /**
     * Bi-Linear interpolation of four nearest node values as described in
     * 'GDAit Software Architecture Manual' produced by the <a
     * href='http://www.sli.unimelb.edu.au/gda94'>Geomatics
     * Department of the University of Melbourne</a>
     * @param a value at the A node
     * @param b value at the B node
     * @param c value at the C node
     * @param d value at the D node
     * @param X Longitude factor
     * @param Y Latitude factor
     * @return interpolated value
     */
    private final double interpolate(float a, float b, float c, float d, double X, double Y) {
        return a + (((double)b - (double)a) * X) + (((double)c - (double)a) * Y) +
        (((double)a + (double)d - b - c) * X * Y);
    }

    /**
     * Interpolate shift and accuracy values for a coordinate in the 'from' datum
     * of the GridShiftFile. The algorithm is described in
     * 'GDAit Software Architecture Manual' produced by the <a
     * href='http://www.sli.unimelb.edu.au/gda94'>Geomatics
     * Department of the University of Melbourne</a>
     * <p>This method is thread safe for both memory based and file based node data.
     * @param gs GridShift object containing the coordinate to shift and the shift values
     * @return the GridShift object supplied, with values updated.
     */
    public NTV2GridShift interpolateGridShift(NTV2GridShift gs) {
        int lonIndex = (int)((gs.getLonPositiveWestSeconds() - minLon) / lonInterval);
        int latIndex = (int)((gs.getLatSeconds() - minLat) / latInterval);

        double X = (gs.getLonPositiveWestSeconds() - (minLon + (lonInterval * lonIndex))) / lonInterval;
        double Y = (gs.getLatSeconds() - (minLat + (latInterval * latIndex))) / latInterval;

        // Find the nodes at the four corners of the cell

        int indexA = lonIndex + (latIndex * lonColumnCount);
        int indexB = indexA + 1;
        int indexC = indexA + lonColumnCount;
        int indexD = indexC + 1;

        gs.setLonShiftPositiveWestSeconds(interpolate(
                lonShift[indexA], lonShift[indexB], lonShift[indexC], lonShift[indexD], X, Y));

        gs.setLatShiftSeconds(interpolate(
                latShift[indexA], latShift[indexB], latShift[indexC], latShift[indexD], X, Y));

        if (lonAccuracy == null) {
            gs.setLonAccuracyAvailable(false);
        } else {
            gs.setLonAccuracyAvailable(true);
            gs.setLonAccuracySeconds(interpolate(
                    lonAccuracy[indexA], lonAccuracy[indexB], lonAccuracy[indexC], lonAccuracy[indexD], X, Y));
        }

        if (latAccuracy == null) {
            gs.setLatAccuracyAvailable(false);
        } else {
            gs.setLatAccuracyAvailable(true);
            gs.setLatAccuracySeconds(interpolate(
                    latAccuracy[indexA], latAccuracy[indexB], latAccuracy[indexC], latAccuracy[indexD], X, Y));
        }
        return gs;
    }

    public String getParentSubGridName() {
        return parentSubGridName;
    }

    public String getSubGridName() {
        return subGridName;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getSubGridCount() {
        return (subGrid == null) ? 0 : subGrid.length;
    }

    public NTV2SubGrid getSubGrid(int index) {
        return (subGrid == null) ? null : subGrid[index];
    }

    /**
     * Set an array of Sub Grids of this sub grid
     * @param subGrid
     */
    public void setSubGridArray(NTV2SubGrid[] subGrid) {
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
        StringBuilder buf = new StringBuilder("Sub Grid : ");
        buf.append(subGridName);
        buf.append("\nParent   : ");
        buf.append(parentSubGridName);
        buf.append("\nCreated  : ");
        buf.append(created);
        buf.append("\nUpdated  : ");
        buf.append(updated);
        buf.append("\nMin Lat  : ");
        buf.append(minLat);
        buf.append("\nMax Lat  : ");
        buf.append(maxLat);
        buf.append("\nMin Lon  : ");
        buf.append(minLon);
        buf.append("\nMax Lon  : ");
        buf.append(maxLon);
        buf.append("\nLat Intvl: ");
        buf.append(latInterval);
        buf.append("\nLon Intvl: ");
        buf.append(lonInterval);
        buf.append("\nNode Cnt : ");
        buf.append(nodeCount);
        return buf.toString();
    }

    /**
     * Make a deep clone of this Sub Grid
     */
    @Override
    public Object clone() {
        NTV2SubGrid clone = null;
        try {
            clone = (NTV2SubGrid)super.clone();
            // Do a deep clone of the sub grids
            if (subGrid != null) {
                clone.subGrid = new NTV2SubGrid[subGrid.length];
                for (int i = 0; i < subGrid.length; i++) {
                    clone.subGrid[i] = (NTV2SubGrid)subGrid[i].clone();
                }
            }
        } catch (CloneNotSupportedException cnse) {
            Main.warn(cnse);
        }
        return clone;
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
