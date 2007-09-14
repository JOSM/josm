// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Helper class to use for xml outputting classes.
 * 
 * @author imi
 */
public class XmlWriter {

	/**
	 * The interface to write the data into an Osm stream
	 * @author immanuel.scholz
	 */
	public static interface OsmWriterInterface {
		void header(PrintWriter out);
		void write(PrintWriter out);
		void footer(PrintWriter out);
	}


	protected XmlWriter(PrintWriter out) {
		this.out = out;
	}

	/**
	 * Encode the given string in XML1.0 format.
	 * Optimized to fast pass strings that don't need encoding (normal case).
	 */
	public static String encode(String unencoded) {
		StringBuilder buffer = null;
		for (int i = 0; i < unencoded.length(); ++i) {
			String encS = XmlWriter.encoding.get(unencoded.charAt(i));
			if (encS != null) {
				if (buffer == null)
					buffer = new StringBuilder(unencoded.substring(0,i));
				buffer.append(encS);
			} else if (buffer != null)
				buffer.append(unencoded.charAt(i));
		}
		return (buffer == null) ? unencoded : buffer.toString();
	}

	/**
	 * Write the header and start tag, then call the runnable to add all real tags and finally
	 * "closes" the xml by writing the footer.
	 */
	public static void output(OutputStream outStream, OsmWriterInterface outputWriter) {
		PrintWriter out;
		try {
			out = new PrintWriter(new OutputStreamWriter(outStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		out.println("<?xml version='1.0' encoding='UTF-8'?>");
		outputWriter.header(out);
		outputWriter.write(out);
		outputWriter.footer(out);
		out.flush();
		out.close();
	}



	/**
	 * The output writer to save the values to.
	 */
	protected final PrintWriter out;
	final private static HashMap<Character, String> encoding = new HashMap<Character, String>();
	static {
		encoding.put('<', "&lt;");
		encoding.put('>', "&gt;");
		encoding.put('"', "&quot;");
		encoding.put('\'', "&apos;");
		encoding.put('&', "&amp;");
		encoding.put('\n', "&#xA;");
		encoding.put('\r', "&#xD;");
		encoding.put('\t', "&#x9;");
	}
}
