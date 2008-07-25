package org.openstreetmap.josm.gui.mappaint;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class MapPaintStyles {

	public static String styleDir;
	private static HashMap<String, ElemStyle> styles = new HashMap<String, ElemStyle>();
	
	public static String getStyleDir(){
		return styleDir;
	}

	public static void readFromPreferences() {

		String styleName = Main.pref.get("mappaint.style", "standard");
		styleDir = Main.pref.getPreferencesDir()+"plugins/mappaint/"+styleName+"/"; //some day we will support different icon directories over options
		String elemStylesFile = getStyleDir()+"elemstyles.xml";

//		System.out.println("mappaint: Using style: " + styleName);
//		System.out.println("mappaint: Using style dir: " + styleDir);
//		System.out.println("mappaint: Using style file: " + elemStylesFile);

		File f = new File(elemStylesFile);
		if (f.exists())
		{
			try// reading file from file system
			{
//				System.out.println("mappaint: Using style file: \"" + f + "\"");
				XMLReader xmlReader = XMLReaderFactory.createXMLReader();
				ElemStyleHandler handler = new ElemStyleHandler();
				xmlReader.setContentHandler(handler);
				xmlReader.setErrorHandler(handler);
//				temporary only!
				xmlReader.parse(new InputSource(new FileReader(f)));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		} 
		else {// reading the builtin file from the plugin jar file
			URL elemStylesPath = Main.class.getResource("/styles/"+styleName+"/elemstyles.xml");

//			System.out.println("mappaint: Using jar's elemstyles.xml: \"" + elemStylesPath + "\"");
			if (elemStylesPath != null)
			{
				try
				{
					XMLReader xmlReader = XMLReaderFactory.createXMLReader();
					ElemStyleHandler handler = new ElemStyleHandler();
					xmlReader.setContentHandler(handler);
					xmlReader.setErrorHandler(handler);
//					temporary only!
					xmlReader.parse(new InputSource(elemStylesPath.openStream()));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			} else {
				System.out.println("mappaint: Couldn't find style: \"" + styleDir + "elemstyles.xml\"");
			}
		}
	}

//	static int nr = 0;

	public static void add (String k, String v, String b, ElemStyle style) {
		ElemStyle  old_style;
		String key;

		/* unfortunately, there don't seem to be an efficient way to */
		/* find out, if a given OsmPrimitive is an area or not, */
		/* so distinguish only between way and node here - for now */
		if (style instanceof AreaElemStyle)
			key = "way";
		else if (style instanceof LineElemStyle)
			key = "way";
		else if (style instanceof IconElemStyle)
			key = "node";
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

	public static ElemStyle getStyle (OsmPrimitive p)
	{
		if (p.keys!=null) {
			String classname;
			String kv = null;

			if (p instanceof org.openstreetmap.josm.data.osm.Node) {
				classname = "node";
			} else {
				classname = "way";
			}
			Iterator<String> iterator = p.keys.keySet().iterator();
			while (iterator.hasNext())	
			{
				String key = iterator.next();
				kv = classname + "n" + key + "=" + p.keys.get(key);
				if (styles.containsKey(kv))
					return styles.get(kv);
				kv = classname + "b" + key + "=" + OsmUtils.getNamedOsmBoolean(p.keys.get(key));
				if (styles.containsKey(kv))
					return styles.get(kv);
				kv = classname + "x" + key;
				if (styles.containsKey(kv))
					return styles.get(kv);
			}

			// not a known key/value combination
//			boolean first_line = true;

			// filter out trivial tags and show the rest
//			iterator = p.keys.keySet().iterator();
//			while (iterator.hasNext()) {
//				String key = iterator.next();
//				kv = key + "=" + p.keys.get(key);
//				if (!kv.startsWith("created_by=") &&
//						!kv.startsWith("converted_by=") &&
//						!kv.startsWith("source=") &&
//						!kv.startsWith("note=") &&
//						!kv.startsWith("layer=") &&
//						!kv.startsWith("bridge=") &&
//						!kv.startsWith("tunnel=") &&
//						!kv.startsWith("oneway=") &&
//						!kv.startsWith("speedlimit=") &&
//						!kv.startsWith("motorcar=") &&
//						!kv.startsWith("horse=") &&
//						!kv.startsWith("bicycle=") &&
//						!kv.startsWith("foot=")
//				) {

//					if (first_line) {
//						nr++;
//						System.out.println("mappaint - rule not found[" + nr + "]: " + kv + " id:" + p.id);
//					} else {
//						System.out.println("mappaint - rule not found[" + nr + "]: " + kv);
//					}
//					first_line=false;
//				}
//			}
		}

		return null;
	}

	public static boolean isArea(OsmPrimitive p)
	{
		return getStyle(p) instanceof AreaElemStyle;
	}
}
