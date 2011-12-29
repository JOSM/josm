// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.awt.image.BufferedImage;

import org.openstreetmap.josm.data.imagery.GeorefImage.State;
import org.openstreetmap.josm.gui.layer.WMSLayer.PrecacheTask;

public class WMSRequest implements Comparable<WMSRequest> {
    private final int xIndex;
    private final int yIndex;
    private final double pixelPerDegree;
    private final boolean real; // Download even if autodownloading is disabled
    private final PrecacheTask precacheTask; // Download even when wms tile is not currently visible (precache)
    private final boolean allowPartialCacheMatch;
    private int priority;
    private boolean hasExactMatch;
    // Result
    private State state;
    private BufferedImage image;

    public WMSRequest(int xIndex, int yIndex, double pixelPerDegree, boolean real, boolean allowPartialCacheMatch) {
        this(xIndex, yIndex, pixelPerDegree, real, allowPartialCacheMatch, null);
    }

    public WMSRequest(int xIndex, int yIndex, double pixelPerDegree, boolean real, boolean allowPartialCacheMatch, PrecacheTask precacheTask) {
        this.xIndex = xIndex;
        this.yIndex = yIndex;
        this.pixelPerDegree = pixelPerDegree;
        this.real = real;
        this.precacheTask = precacheTask;
        this.allowPartialCacheMatch = allowPartialCacheMatch;
    }


    public void finish(State state, BufferedImage image) {
        this.state = state;
        this.image = image;
    }

    public int getXIndex() {
        return xIndex;
    }

    public int getYIndex() {
        return yIndex;
    }

    public double getPixelPerDegree() {
        return pixelPerDegree;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(pixelPerDegree);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + xIndex;
        result = prime * result + yIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WMSRequest other = (WMSRequest) obj;
        if (Double.doubleToLongBits(pixelPerDegree) != Double
                .doubleToLongBits(other.pixelPerDegree))
            return false;
        if (xIndex != other.xIndex)
            return false;
        if (yIndex != other.yIndex)
            return false;
        if (allowPartialCacheMatch != other.allowPartialCacheMatch)
            return false;
        return true;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(WMSRequest o) {
        return priority - o.priority;
    }

    public State getState() {
        return state;
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "WMSRequest [xIndex=" + xIndex + ", yIndex=" + yIndex
                + ", pixelPerDegree=" + pixelPerDegree + "]";
    }

    public boolean isReal() {
        return real;
    }

    public boolean isPrecacheOnly() {
        return precacheTask != null;
    }

    public PrecacheTask getPrecacheTask() {
        return precacheTask;
    }

    public boolean isAllowPartialCacheMatch() {
        return allowPartialCacheMatch;
    }

    public boolean hasExactMatch() {
        return hasExactMatch;
    }

    public void setHasExactMatch(boolean hasExactMatch) {
        this.hasExactMatch = hasExactMatch;
    }
}
