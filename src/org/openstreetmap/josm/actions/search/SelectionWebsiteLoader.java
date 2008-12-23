// License: GPL. Copyright 2007 by Immanuel Scholz and others
/**
 *
 */
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmIdReader;
import org.openstreetmap.josm.io.ProgressInputStream;
import org.xml.sax.SAXException;

public class SelectionWebsiteLoader extends PleaseWaitRunnable {
    public final URL url;
    public Collection<OsmPrimitive> sel;
    private final SearchAction.SearchMode mode;
    private OsmIdReader idReader = new OsmIdReader();
    public SelectionWebsiteLoader(String urlStr, SearchAction.SearchMode mode) {
        super(tr("Load Selection"));
        this.mode = mode;
        URL u = null;
        try {u = new URL(urlStr);} catch (MalformedURLException e) {}
        this.url = u;
    }
    @Override protected void realRun() {
        Main.pleaseWaitDlg.currentAction.setText(tr("Contact {0}...", url.getHost()));
        sel = mode != SearchAction.SearchMode.remove ? new LinkedList<OsmPrimitive>() : Main.ds.allNonDeletedPrimitives();
        try {
            URLConnection con = url.openConnection();
            InputStream in = new ProgressInputStream(con, Main.pleaseWaitDlg);
            Main.pleaseWaitDlg.currentAction.setText(tr("Downloading..."));
            Map<Long, String> ids = idReader.parseIds(in);
            for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives()) {
                if (ids.containsKey(osm.id) && osm.getClass().getName().toLowerCase().endsWith(ids.get(osm.id))) {
                    if (mode == SearchAction.SearchMode.remove)
                        sel.remove(osm);
                    else
                        sel.add(osm);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(Main.parent, tr("Could not read from url: \"{0}\"",url));
        } catch (SAXException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(Main.parent,tr("Parsing error in url: \"{0}\"",url));
        }
    }
    @Override protected void cancel() {
        sel = null;
        idReader.cancel();
    }
    @Override protected void finish() {
        if (sel != null)
            Main.ds.setSelected(sel);
    }
}