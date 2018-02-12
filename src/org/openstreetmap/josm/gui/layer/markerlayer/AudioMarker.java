// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.audio.AudioPlayer;
import org.openstreetmap.josm.io.audio.AudioUtil;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

/**
 * Marker class with audio playback capability.
 *
 * @author Frederik Ramm
 *
 */
public class AudioMarker extends ButtonMarker {

    private final URL audioUrl;
    private static volatile AudioMarker recentlyPlayedMarker;
    public double syncOffset;
    public boolean timeFromAudio; // as opposed to from the GPX track

    public AudioMarker(LatLon ll, TemplateEngineDataProvider dataProvider, URL audioUrl, MarkerLayer parentLayer, double time, double offset) {
        super(ll, dataProvider, /* ICON(markers/) */ "speech", parentLayer, time, offset);
        this.audioUrl = audioUrl;
        this.syncOffset = 0.0;
        this.timeFromAudio = false;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        play();
    }

    /**
     * Returns the marker played the most recently, if any.
     * @return the marker played the most recently, or {@code null}
     */
    public static AudioMarker recentlyPlayedMarker() {
        return recentlyPlayedMarker;
    }

    /**
     * Forgets the marker played the most recently, if any.
     */
    static void resetRecentlyPlayedMarker() {
        recentlyPlayedMarker = null;
    }

    public URL url() {
        return audioUrl;
    }

    /**
     * Starts playing the audio associated with the marker offset by the given amount
     * @param after : seconds after marker where playing should start
     */
    public void play(double after) {
        try {
            // first enable tracing the audio along the track
            MainApplication.getMap().mapView.playHeadMarker.animate();

            AudioPlayer.play(audioUrl, offset + syncOffset + after);
            recentlyPlayedMarker = this;
        } catch (IOException | InterruptedException e) {
            AudioUtil.audioMalfunction(e);
        }
    }

    /**
     * Starts playing the audio associated with the marker: used in response to pressing
     * the marker as well as indirectly
     *
     */
    public void play() {
        play(0.0);
    }

    public void adjustOffset(double adjustment) {
        syncOffset = adjustment; // added to offset may turn out negative, but that's ok
    }

    public double syncOffset() {
        return syncOffset;
    }

    @Override
    protected TemplateEntryProperty getTextTemplate() {
        return TemplateEntryProperty.forAudioMarker(parentLayer.getName());
    }

    @Override
    public WayPoint convertToWayPoint() {
        WayPoint wpt = super.convertToWayPoint();
        GpxLink link = new GpxLink(audioUrl.toString());
        link.type = "audio";
        wpt.put(GpxConstants.META_LINKS, Collections.singleton(link));
        wpt.addExtension("offset", Double.toString(offset));
        wpt.addExtension("sync-offset", Double.toString(syncOffset));
        return wpt;
    }
}
