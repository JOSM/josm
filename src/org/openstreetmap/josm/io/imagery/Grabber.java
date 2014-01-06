// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.imagery.GeorefImage.State;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.WMSLayer;

abstract public class Grabber implements Runnable {
    protected final MapView mv;
    protected final WMSLayer layer;
    private final boolean localOnly;

    protected ProjectionBounds b;
    protected volatile boolean canceled;

    Grabber(MapView mv, WMSLayer layer, boolean localOnly) {
        this.mv = mv;
        this.layer = layer;
        this.localOnly = localOnly;
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
            WMSRequest request = layer.getRequest(localOnly);
            if (request == null)
                return;
            this.b = layer.getBounds(request);
            if (request.isPrecacheOnly()) {
                if (!layer.cache.hasExactMatch(Main.getProjection(), request.getPixelPerDegree(), b.minEast, b.minNorth)) {
                    attempt(request);
                }
            } else {
                if(!loadFromCache(request)){
                    attempt(request);
                }
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
                if (!request.isPrecacheOnly() && !layer.requestIsVisible(request))
                    return;
                fetch(request, i);
                break; // break out of the retry loop
            } catch (Exception e) {
                try { // sleep some time and then ask the server again
                    Thread.sleep(random(1000, 2000));
                } catch (InterruptedException e1) {
                    Main.debug("InterruptedException in "+getClass().getSimpleName()+" during WMS request");
                }
                if(i == maxTries) {
                    Main.error(e);
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
