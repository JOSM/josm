package org.openstreetmap.josm.gui.mappaint;

import java.util.HashMap;
import java.util.Iterator;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
public class ElemStyles
{
	HashMap<String, ElemStyle> styles;
	static int nr = 0;


	public ElemStyles()
	{
		styles = new HashMap<String, ElemStyle>();
	}

	public void add (String k, String v, ElemStyle style)
	{
		ElemStyle  old_style;
		String key = k + "=" + v;
		
		/* unfortunately, there don't seem to be an efficient way to */
		/* find out, if a given OsmPrimitive is an area or not, */
		/* so distinguish only between way and node here - for now */
		if(style instanceof AreaElemStyle) {
			key = key + "way";
		}
		else if(style instanceof LineElemStyle) {
			key = key + "way";
		}
		else if(style instanceof IconElemStyle) {
			key = key + "node";
		}
		/* avoid duplicates - for now */
		old_style = styles.get(key);
		if(old_style == null) {
			/* new key/value, insert */
			styles.put(key, style);
		} else {
			if(style.getMaxScale() < old_style.getMaxScale()) {
				/* existing larger scale key/value, replace */
				styles.remove(old_style);
				styles.put(key, style);
			}
		}
	}

	public ElemStyle getStyle (OsmPrimitive p)
	{
		if(p.keys!=null)
		{
			String classname;
			String kv = null;
			
			if(p instanceof org.openstreetmap.josm.data.osm.Node) {
				classname = "node";
			} else {
				classname = "way";
			}
			Iterator<String> iterator = p.keys.keySet().iterator();
			while(iterator.hasNext())	
			{
				String key = iterator.next();
				kv = key + "=" + p.keys.get(key) + classname;
				if(styles.containsKey(kv))
				{
					return styles.get(kv);
				}
			}

            // not a known key/value combination
			boolean first_line = true;

            // filter out trivial tags and show the rest
			iterator = p.keys.keySet().iterator();
			while(iterator.hasNext())	
			{
				String key = iterator.next();
				kv = key + "=" + p.keys.get(key);
				if(	!kv.startsWith("created_by=") &&
					!kv.startsWith("converted_by=") &&
					!kv.startsWith("source=") &&
					!kv.startsWith("note=") &&
					!kv.startsWith("layer=") &&
					!kv.startsWith("bridge=") &&
					!kv.startsWith("tunnel=") &&
					!kv.startsWith("oneway=") &&
					!kv.startsWith("speedlimit=") &&
					!kv.startsWith("motorcar=") &&
					!kv.startsWith("horse=") &&
					!kv.startsWith("bicycle=") &&
					!kv.startsWith("foot=")
					) {
						
					if (first_line) {
						nr++;
						//System.out.println("mappaint - rule not found[" + nr + "]: " + kv + " id:" + p.id);
					} else {
						//System.out.println("mappaint - rule not found[" + nr + "]: " + kv);
					}
					first_line=false;
				}
			}
		}
		
		return null;
	}

	public boolean isArea(OsmPrimitive p)
	{
		return getStyle(p) instanceof AreaElemStyle;
	}
}
