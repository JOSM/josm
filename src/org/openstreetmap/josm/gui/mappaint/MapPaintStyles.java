package org.openstreetmap.josm.gui.mappaint;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class MapPaintStyles {

	private static String styleDir;
	private static String imageDir;
	private static String internalImageDir;
	private static Boolean isInternal = false;
	private static ElemStyles styles = new ElemStyles();
	
	public static String getStyleDir(){
		return styleDir;
	}
	public static String getImageDir(){
		return imageDir;
	}
	public static String getInternalImageDir(){
		return internalImageDir;
	}
	public static Boolean isInternal(){
		return isInternal;
	}
	public static void add(String k, String v, String b, ElemStyle style)
	{
		styles.add(k, v, b, style);
	}
	public static ElemStyle getStyle(OsmPrimitive osm)
	{
		ElemStyle s = styles.get(osm, true);
		if(s != null)
		{
			ElemStyle l = styles.get(osm, false);
			if(l != null && l instanceof LineElemStyle)
			{
				s = new AreaElemStyle((AreaElemStyle)s, (LineElemStyle)l);
			}
		}
		else
			s = styles.get(osm, false);
		return s;
	}
	public static boolean isArea(OsmPrimitive osm)
	{
		return styles.isArea(osm);
	}

	public static void readFromPreferences() {

		String styleName = Main.pref.get("mappaint.style", "standard");
		// fallback to standard name for internal case, as we only have one internal style
		String internalStyleName = "standard";
		styleDir = Main.pref.get("mappaint.styledir", Main.pref.getPreferencesDir()+"plugins/mappaint/"+styleName+"/");
		String elemStylesFile = getStyleDir()+"elemstyles.xml";
		imageDir = styleDir+"icons/";
		internalImageDir = "/images/styles/"+internalStyleName+"/";

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
			URL elemStylesPath = Main.class.getResource("/styles/"+internalStyleName+"/elemstyles.xml");

//			System.out.println("mappaint: Using jar's elemstyles.xml: \"" + elemStylesPath + "\"");
			if (elemStylesPath != null)
			{
				isInternal = true;
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

}
