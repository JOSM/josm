// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.preferences.StringProperty;

public class OsmosnimkiOffsetServer implements OffsetServer {
    public static StringProperty PROP_SERVER_URL = new StringProperty("imagery.offsetserver.url","http://offset.osmosnimki.ru/offset/v0?");
    private String url;

    public OsmosnimkiOffsetServer(String url) {
        this.url = url;
    }

    @Override
    public boolean isLayerSupported(ImageryInfo info) {
        try {
            URL url = new URL(this.url + "action=CheckAvailability&id=" + URLEncoder.encode(info.getFullUrl(), "UTF-8"));
            System.out.println(tr("Querying offset availability: {0}", url));
            final BufferedReader rdr = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), "UTF-8"));
            String response = rdr.readLine();
            System.out.println(tr("Offset server response: {0}", response));
            if (response.contains("\"offsets_available\": true")) return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public EastNorth getOffset(ImageryInfo info, EastNorth en) {
        LatLon ll = Main.getProjection().eastNorth2latlon(en);
        try {
            URL url = new URL(this.url + "action=GetOffsetForPoint&lat=" + ll.lat() + "&lon=" + ll.lon() + "&id=" + URLEncoder.encode(info.getFullUrl(), "UTF-8"));
            System.out.println(tr("Querying offset: {0}", url.toString()));
            final BufferedReader rdr = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream(), "UTF-8"));
            String s = rdr.readLine();
            int i = s.indexOf(',');
            if (i == -1) return null;
            String sLon = s.substring(1,i);
            String sLat = s.substring(i+1,s.length()-1);
            return Main.getProjection().latlon2eastNorth(new LatLon(Double.valueOf(sLat),Double.valueOf(sLon))).sub(en);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
