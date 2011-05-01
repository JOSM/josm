// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.GeorefImage.State;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.WMSLayer;

abstract public class Grabber implements Runnable {
    protected final MapView mv;
    protected final WMSLayer layer;

    protected ProjectionBounds b;
    protected Projection proj;
    protected double pixelPerDegree;
    protected WMSRequest request;
    protected volatile boolean canceled;

    Grabber(MapView mv, WMSLayer layer) {
        this.mv = mv;
        this.layer = layer;
    }

    private void updateState(WMSRequest request) {
        b = new ProjectionBounds(
                layer.getEastNorth(request.getXIndex(), request.getYIndex()),
                layer.getEastNorth(request.getXIndex() + 1, request.getYIndex() + 1));
        if (WMSLayer.PROP_OVERLAP.get()) {
            double eastSize =  b.maxEast - b.minEast;
            double northSize =  b.maxNorth - b.minNorth;

            double eastCoef = WMSLayer.PROP_OVERLAP_EAST.get() / 100.0;
            double northCoef = WMSLayer.PROP_OVERLAP_NORTH.get() / 100.0;

            this.b = new ProjectionBounds(b.getMin(),
                    new EastNorth(b.maxEast + eastCoef * eastSize,
                            b.maxNorth + northCoef * northSize));
        }

        this.proj = Main.proj;
        this.pixelPerDegree = request.getPixelPerDegree();
        this.request = request;
    }

    abstract void fetch(WMSRequest request, int attempt) throws Exception; // the image fetch code

    int width(){
        return layer.getBaseImageWidth();
    }
    int height(){
        return layer.getBaseImageHeight();
    }

    @Override
    public void run() {
        while (true) {
            if (canceled)
                return;
            WMSRequest request = layer.getRequest();
            if (request == null)
                return;
            updateState(request);
            if(!loadFromCache(request)){
                attempt(request);
            }
            layer.finishRequest(request);
        }
    }

    protected void attempt(WMSRequest request){ // try to fetch the image
        int maxTries = 5; // n tries for every image
        for (int i = 1; i <= maxTries; i++) {
            if (canceled)
                return;
            try {
                if (!layer.requestIsValid(request))
                    return;
                fetch(request, i);
                break; // break out of the retry loop
            } catch (Exception e) {
                try { // sleep some time and then ask the server again
                    Thread.sleep(random(1000, 2000));
                } catch (InterruptedException e1) {}

                if(i == maxTries) {
                    e.printStackTrace();
                    request.finish(State.FAILED, null);
                }
            }
        }
    }

    public static int random(int min, int max) {
        return (int)(Math.random() * ((max+1)-min) ) + min;
    }

    abstract public boolean loadFromCache(WMSRequest request);

    public void cancel() {
        canceled = true;
    }

}
