// License: GPL. Copyright 2009 by Dave Hansen, others
package org.openstreetmap.josm.data.coor;


public class QuadTiling
{
    public static int NR_LEVELS = 24;
    public static double WORLD_PARTS = (1 << NR_LEVELS);

    public static int MAX_OBJECTS_PER_LEVEL = 16;
    // has to be a power of 2
    public static int TILES_PER_LEVEL_SHIFT = 2;
    public static int TILES_PER_LEVEL = 1<<TILES_PER_LEVEL_SHIFT;
    static public int X_PARTS = 360;
    static public int X_BIAS = -180;

    static public int Y_PARTS = 180;
    static public int Y_BIAS = -90;

    public static LatLon tile2LatLon(long quad)
    {
        // The world is divided up into X_PARTS,Y_PARTS.
        // The question is how far we move for each bit
        // being set.  In the case of the top level, we
        // move half of the world.
        double x_unit = X_PARTS/2;
        double y_unit = Y_PARTS/2;
        long shift = (NR_LEVELS*2)-2;

        //if (debug)
        //    out("tile2xy(0x"+Long.toHexString(quad)+")");
        double x = 0;
        double y = 0;
        for (int i = 0; i < NR_LEVELS; i++) {
            long bits = (quad >> shift) & 0x3;
            //if (debug)
            //    out("shift: " + shift + " bits: " + bits);
            // remember x is the MSB
            if ((bits & 0x2) != 0) {
                x += x_unit;
            }
            if ((bits & 0x1) != 0) {
                y += y_unit;
            }
            x_unit /= 2;
            y_unit /= 2;
            shift -= 2;
        }
        x += X_BIAS;
        y += Y_BIAS;
        return new LatLon(y, x);
    }
    static long xy2tile(long x, long y)
    {
        long tile = 0;
        int i;
        for (i = NR_LEVELS-1; i >= 0; i--)
        {
            long xbit = ((x >> i) & 1);
            long ybit = ((y >> i) & 1);
            tile <<= 2;
            // Note that x is the MSB
            tile |= (xbit<<1) | ybit;
        }
        return tile;
    }
    static long coorToTile(LatLon coor)
    {
        return quadTile(coor);
    }
    static long lon2x(double lon)
    {
        //return Math.round((lon + 180.0) * QuadBuckets.WORLD_PARTS / 360.0)-1;
        long ret = (long)Math.floor((lon + 180.0) * WORLD_PARTS / 360.0);
        if (ret == WORLD_PARTS) {
            ret--;
        }
        return ret;
    }
    static long lat2y(double lat)
    {
        //return Math.round((lat + 90.0) * QuadBuckets.WORLD_PARTS / 180.0)-1;
        long ret = (long)Math.floor((lat + 90.0) * WORLD_PARTS / 180.0);
        if (ret == WORLD_PARTS) {
            ret--;
        }
        return ret;
    }
    static public long quadTile(LatLon coor)
    {
        return xy2tile(lon2x(coor.lon()),
                lat2y(coor.lat()));
    }
    static public int index(int level, long quad)
    {
        long mask = 0x00000003;
        int total_shift = TILES_PER_LEVEL_SHIFT*(NR_LEVELS-level-1);
        return (int)(mask & (quad >> total_shift));
    }
    static public int index(LatLon coor, int level)
    {
        // The nodes that don't return coordinates will all get
        // stuck in a single tile.  Hopefully there are not too
        // many of them
        if (coor == null)
            return 0;
        long quad = coorToTile(coor);
        return index(level, quad);
    }
}
