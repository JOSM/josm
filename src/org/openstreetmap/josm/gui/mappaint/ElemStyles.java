package org.openstreetmap.josm.gui.mappaint;

import java.util.HashMap;
import java.util.Iterator;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;

public class ElemStyles
{
	private HashMap<String, ElemStyle> styles;

	public ElemStyles()
	{
		styles = new HashMap<String, ElemStyle>();
	}

	public void add(String k, String v, String b, ElemStyle style)
	{
		ElemStyle  old_style;
		String key;

		/* unfortunately, there don't seem to be an efficient way to */
		/* find out, if a given OsmPrimitive is an area or not, */
		/* so distinguish only between way and node here - for now */
		if (style instanceof AreaElemStyle)
			key = "w";
		else if (style instanceof LineElemStyle)
			key = "w";
		else if (style instanceof IconElemStyle)
			key = "n";
		else
			key = "";

		if(v != null)
			key += "n" + k + "=" + v;
		else if(b != null)
			key += "b" + k  + "=" + OsmUtils.getNamedOsmBoolean(b);
		else
			key += "x" + k;

		/* avoid duplicates - for now */
		old_style = styles.get(key);
		if (old_style == null) {
			/* new key/value, insert */
			styles.put(key, style);
		} else {
			if (style.getMaxScale() < old_style.getMaxScale()) {
				/* existing larger scale key/value, replace */
				styles.remove(old_style);
				styles.put(key, style);
			}
		}
	}

	public ElemStyle get(OsmPrimitive p, Boolean area)
	{
		if (p.keys!=null) {
			String classname;
			String kv = null;

			if (p instanceof org.openstreetmap.josm.data.osm.Node) {
				if(area)
					return null;
				classname = "n";
			} else {
				classname = "w";
			}
			Iterator<String> iterator = p.keys.keySet().iterator();
			while (iterator.hasNext())
			{
				String key = iterator.next();
				ElemStyle style = null;
				kv = classname + "n" + key + "=" + p.keys.get(key);
				if (styles.containsKey(kv))
				{
					style = styles.get(kv);
					if(area == style instanceof AreaElemStyle)
						return style;
				}
				kv = classname + "b" + key + "=" + OsmUtils.getNamedOsmBoolean(p.keys.get(key));
				if (styles.containsKey(kv))
				{
					style = styles.get(kv);
					if(area == style instanceof AreaElemStyle)
						return style;
				}
				kv = classname + "x" + key;
				if (styles.containsKey(kv))
				{
					style = styles.get(kv);
					if(area == style instanceof AreaElemStyle)
						return style;
				}
			}
		}

		return null;
	}

	public boolean isArea(OsmPrimitive p)
	{
		return get(p, true) instanceof AreaElemStyle;
	}
}
