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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.tools.Logging;

/**
 * Models the NTv2 format Grid Shift File and exposes methods to shift
 * coordinate values using the Sub Grids contained in the file.
 * <p>The principal reference for the alogrithms used is the
 * 'GDAit Software Architecture Manual' produced by the <a
 * href='http://www.sli.unimelb.edu.au/gda94'>Geomatics
 * Department of the University of Melbourne</a>
 * <p>This library reads binary NTv2 Grid Shift files in Big Endian
 * (Canadian standard) or Little Endian (Australian Standard) format.
 * The older 'Australian' binary format is not supported, only the
 * official Canadian format, which is now also used for the national
 * Australian Grid.
 * <p>Grid Shift files can be read as InputStreams or RandomAccessFiles.
 * Loading an InputStream places all the required node information
 * (accuracy data is optional) into heap based Java arrays. This is the
 * highest perfomance option, and is useful for large volume transformations.
 * Non-file data sources (eg using an SQL Blob) are also supported through
 * InputStream. The RandonAccessFile option has a much smaller memory
 * footprint as only the Sub Grid headers are stored in memory, but
 * transformation is slower because the file must be read a number of
 * times for each transformation.
 * <p>Coordinates may be shifted Forward (ie from and to the Datums specified
 * in the Grid Shift File header) or Reverse. The reverse transformation
 * uses an iterative approach to approximate the Grid Shift, as the
 * precise transformation is based on 'from' datum coordinates.
 * <p>Coordinates may be specified
 * either in Seconds using Positive West Longitude (the original NTv2
 * arrangement) or in decimal Degrees using Positive East Longitude.
 *
 * @author Peter Yuill
 * Modified for JOSM :
 * - removed the RandomAccessFile mode (Pieren)
 */
public class NTV2GridShiftFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private int overviewHeaderCount;
    private int subGridHeaderCount;
    private int subGridCount;
    private String shiftType;
    private String version;
    private String fromEllipsoid = "";
    private String toEllipsoid = "";
    private double fromSemiMajorAxis;
    private double fromSemiMinorAxis;
    private double toSemiMajorAxis;
    private double toSemiMinorAxis;

    private NTV2SubGrid[] topLevelSubGrid;
    private NTV2SubGrid lastSubGrid;

    private static void readBytes(InputStream in, byte[] b) throws IOException {
        if (in.read(b) < b.length) {
            Logging.error("Failed to read expected amount of bytes ("+ b.length +") from stream");
        }
    }

    /**
     * Load a Grid Shift File from an InputStream. The Grid Shift node
     * data is stored in Java arrays, which will occupy about the same memory
     * as the original file with accuracy data included, and about half that
     * with accuracy data excluded. The size of the Australian national file
     * is 4.5MB, and the Canadian national file is 13.5MB
     * <p>The InputStream is closed by this method.
     *
     * @param in Grid Shift File InputStream
     * @param loadAccuracy is Accuracy data to be loaded as well as shift data?
     * @throws IOException if any I/O error occurs
     */
    public void loadGridShiftFile(InputStream in, boolean loadAccuracy) throws IOException {
        byte[] b8 = new byte[8];
        fromEllipsoid = "";
        toEllipsoid = "";
        topLevelSubGrid = null;
        readBytes(in, b8);
        String overviewHeaderCountId = new String(b8, StandardCharsets.UTF_8);
        if (!"NUM_OREC".equals(overviewHeaderCountId))
            throw new IllegalArgumentException("Input file is not an NTv2 grid shift file");
        boolean bigEndian;
        readBytes(in, b8);
        overviewHeaderCount = NTV2Util.getIntBE(b8, 0);
        if (overviewHeaderCount == 11) {
            bigEndian = true;
        } else {
            overviewHeaderCount = NTV2Util.getIntLE(b8, 0);
            if (overviewHeaderCount == 11) {
                bigEndian = false;
            } else
                throw new IllegalArgumentException("Input file is not an NTv2 grid shift file");
        }
        readBytes(in, b8);
        readBytes(in, b8);
        subGridHeaderCount = NTV2Util.getInt(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        subGridCount = NTV2Util.getInt(b8, bigEndian);
        NTV2SubGrid[] subGrid = new NTV2SubGrid[subGridCount];
        readBytes(in, b8);
        readBytes(in, b8);
        shiftType = new String(b8, StandardCharsets.UTF_8);
        readBytes(in, b8);
        readBytes(in, b8);
        version = new String(b8, StandardCharsets.UTF_8);
        readBytes(in, b8);
        readBytes(in, b8);
        fromEllipsoid = new String(b8, StandardCharsets.UTF_8);
        readBytes(in, b8);
        readBytes(in, b8);
        toEllipsoid = new String(b8, StandardCharsets.UTF_8);
        readBytes(in, b8);
        readBytes(in, b8);
        fromSemiMajorAxis = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        fromSemiMinorAxis = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        toSemiMajorAxis = NTV2Util.getDouble(b8, bigEndian);
        readBytes(in, b8);
        readBytes(in, b8);
        toSemiMinorAxis = NTV2Util.getDouble(b8, bigEndian);

        for (int i = 0; i < subGridCount; i++) {
            subGrid[i] = new NTV2SubGrid(in, bigEndian, loadAccuracy);
        }
        topLevelSubGrid = createSubGridTree(subGrid);
        lastSubGrid = topLevelSubGrid[0];
    }

    /**
     * Create a tree of Sub Grids by adding each Sub Grid to its parent (where
     * it has one), and returning an array of the top level Sub Grids
     * @param subGrid an array of all Sub Grids
     * @return an array of top level Sub Grids with lower level Sub Grids set.
     */
    private static NTV2SubGrid[] createSubGridTree(NTV2SubGrid... subGrid) {
        int topLevelCount = 0;
        Map<String, List<NTV2SubGrid>> subGridMap = new HashMap<>();
        for (int i = 0; i < subGrid.length; i++) {
            if ("NONE".equalsIgnoreCase(subGrid[i].getParentSubGridName())) {
                topLevelCount++;
            }
            subGridMap.put(subGrid[i].getSubGridName(), new ArrayList<NTV2SubGrid>());
        }
        NTV2SubGrid[] topLevelSubGrid = new NTV2SubGrid[topLevelCount];
        topLevelCount = 0;
        for (int i = 0; i < subGrid.length; i++) {
            if ("NONE".equalsIgnoreCase(subGrid[i].getParentSubGridName())) {
                topLevelSubGrid[topLevelCount++] = subGrid[i];
            } else {
                List<NTV2SubGrid> parent = subGridMap.get(subGrid[i].getParentSubGridName());
                parent.add(subGrid[i]);
            }
        }
        NTV2SubGrid[] nullArray = new NTV2SubGrid[0];
        for (int i = 0; i < subGrid.length; i++) {
            List<NTV2SubGrid> subSubGrids = subGridMap.get(subGrid[i].getSubGridName());
            if (!subSubGrids.isEmpty()) {
                NTV2SubGrid[] subGridArray = subSubGrids.toArray(nullArray);
                subGrid[i].setSubGridArray(subGridArray);
            }
        }
        return topLevelSubGrid;
    }

    /**
     * Shift a coordinate in the Forward direction of the Grid Shift File.
     *
     * @param gs A GridShift object containing the coordinate to shift
     * @return True if the coordinate is within a Sub Grid, false if not
     */
    public boolean gridShiftForward(NTV2GridShift gs) {
        NTV2SubGrid subGrid = null;
        if (lastSubGrid != null) {
            // Try the last sub grid first, big chance the coord is still within it
            subGrid = lastSubGrid.getSubGridForCoord(gs.getLonPositiveWestSeconds(), gs.getLatSeconds());
        }
        if (subGrid == null) {
            subGrid = getSubGrid(topLevelSubGrid, gs.getLonPositiveWestSeconds(), gs.getLatSeconds());
        }
        if (subGrid == null) {
            return false;
        } else {
            subGrid.interpolateGridShift(gs);
            gs.setSubGridName(subGrid.getSubGridName());
            lastSubGrid = subGrid;
            return true;
        }
    }

    /**
     * Shift a coordinate in the Reverse direction of the Grid Shift File.
     *
     * @param gs A GridShift object containing the coordinate to shift
     * @return True if the coordinate is within a Sub Grid, false if not
     */
    public boolean gridShiftReverse(NTV2GridShift gs) {
        // set up the first estimate
        NTV2GridShift forwardGs = new NTV2GridShift();
        forwardGs.setLonPositiveWestSeconds(gs.getLonPositiveWestSeconds());
        forwardGs.setLatSeconds(gs.getLatSeconds());
        for (int i = 0; i < 4; i++) {
            if (!gridShiftForward(forwardGs))
                return false;
            forwardGs.setLonPositiveWestSeconds(
                    gs.getLonPositiveWestSeconds() - forwardGs.getLonShiftPositiveWestSeconds());
            forwardGs.setLatSeconds(gs.getLatSeconds() - forwardGs.getLatShiftSeconds());
        }
        gs.setLonShiftPositiveWestSeconds(-forwardGs.getLonShiftPositiveWestSeconds());
        gs.setLatShiftSeconds(-forwardGs.getLatShiftSeconds());
        gs.setLonAccuracyAvailable(forwardGs.isLonAccuracyAvailable());
        if (forwardGs.isLonAccuracyAvailable()) {
            gs.setLonAccuracySeconds(forwardGs.getLonAccuracySeconds());
        }
        gs.setLatAccuracyAvailable(forwardGs.isLatAccuracyAvailable());
        if (forwardGs.isLatAccuracyAvailable()) {
            gs.setLatAccuracySeconds(forwardGs.getLatAccuracySeconds());
        }
        return true;
    }

    /**
     * Find the finest SubGrid containing the coordinate, specified in Positive West Seconds
     * @param topLevelSubGrid top level subgrid
     * @param lon Longitude in Positive West Seconds
     * @param lat Latitude in Seconds
     * @return The SubGrid found or null
     */
    private static NTV2SubGrid getSubGrid(NTV2SubGrid[] topLevelSubGrid, double lon, double lat) {
        NTV2SubGrid sub = null;
        for (int i = 0; i < topLevelSubGrid.length; i++) {
            sub = topLevelSubGrid[i].getSubGridForCoord(lon, lat);
            if (sub != null) {
                break;
            }
        }
        return sub;
    }

    @Override
    public String toString() {
        return new StringBuilder(256)
            .append("Headers  : ")
            .append(overviewHeaderCount)
            .append("\nSub Hdrs : ")
            .append(subGridHeaderCount)
            .append("\nSub Grids: ")
            .append(subGridCount)
            .append("\nType     : ")
            .append(shiftType)
            .append("\nVersion  : ")
            .append(version)
            .append("\nFr Ellpsd: ")
            .append(fromEllipsoid)
            .append("\nTo Ellpsd: ")
            .append(toEllipsoid)
            .append("\nFr Maj Ax: ")
            .append(fromSemiMajorAxis)
            .append("\nFr Min Ax: ")
            .append(fromSemiMinorAxis)
            .append("\nTo Maj Ax: ")
            .append(toSemiMajorAxis)
            .append("\nTo Min Ax: ")
            .append(toSemiMinorAxis)
            .toString();
    }

    public String getFromEllipsoid() {
        return fromEllipsoid;
    }

    public String getToEllipsoid() {
        return toEllipsoid;
    }
}
