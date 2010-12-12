package org.openstreetmap.josm.io.imagery;

import java.awt.image.BufferedImage;

import org.openstreetmap.josm.data.imagery.GeorefImage;
import org.openstreetmap.josm.data.imagery.GeorefImage.State;

public class WMSRequest implements Comparable<WMSRequest> {
    private final int xIndex;
    private final int yIndex;
    private final double pixelPerDegree;
    private final boolean real; // Download even if autodownloading is disabled
    private int priority;
    // Result
    private State state;
    private BufferedImage image;

    public WMSRequest(int xIndex, int yIndex, double pixelPerDegree, boolean real) {
        this.xIndex = xIndex;
        this.yIndex = yIndex;
        this.pixelPerDegree = pixelPerDegree;
        this.real = real;
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
}
