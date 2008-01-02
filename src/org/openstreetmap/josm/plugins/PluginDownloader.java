//License: GPL. Copyright 2007 by Immanuel Scholz and others
/**
 * 
 */
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.preferences.PluginPreference.PluginDescription;
import org.xml.sax.SAXException;

public class PluginDownloader {

	private static final class UpdateTask extends PleaseWaitRunnable {
		private final Collection<PluginDescription> toUpdate;
		private String errors = "";
		private int count = 0;

		private UpdateTask(Collection<PluginDescription> toUpdate) {
			super(tr("Update Plugins"));
			this.toUpdate = toUpdate;
		}

		@Override protected void cancel() {
			finish();
		}

		@Override protected void finish() {
			if (errors.length() > 0)
				JOptionPane.showMessageDialog(Main.parent, tr("There were problems with the following plugins:\n\n {0}",errors));
			else
				JOptionPane.showMessageDialog(Main.parent, trn("{0} Plugin successfully updated. Please restart JOSM.", "{0} Plugins successfully updated. Please restart JOSM.", count, count));
		}

		@Override protected void realRun() throws SAXException, IOException {
			for (PluginDescription d : toUpdate) {
				File tempFile = new File(Main.pref.getPreferencesDir()+"temp.jar");
				if (download(d.resource, tempFile)) {
					tempFile.renameTo(new File(Main.pref.getPreferencesDir()+"plugins/"+d.name+".jar"));
					count++;
				} else
					errors += d.name + "\n";
			}
		}
	}

	private static final Pattern wiki = Pattern.compile("^</td></tr><tr><td><a class=\"ext-link\" href=\"([^\"]*)\"><span class=\"icon\">([^<]*)</span></a></td><td>([^<]*)</td><td>([^<].*)</td><td>(.*)");

	public static int downloadDescription() {
		int count = 0;
		for (String site : getSites()) {
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(new URL(site).openStream()));
				CharSequence txt;
				if (site.toLowerCase().endsWith(".xml"))
					txt = readXml(r);
				else
					txt = readWiki(r);
				r.close();
				new File(Main.pref.getPreferencesDir()+"plugins").mkdir();
				FileWriter out = new FileWriter(Main.pref.getPreferencesDir()+"plugins/"+count+"-site-"+site.replaceAll("[/:\\\\ <>|]", "_")+".xml");
				out.append(txt);
				out.close();
				count++;
			} catch (IOException x) {
			}
		}
		return count;
	}

	public static String[] getSites() {
	    return Main.pref.get("pluginmanager.sites", "http://josm.openstreetmap.de/wiki/Plugins").split(" ");
    }

	private static CharSequence readXml(BufferedReader r) throws IOException {
		StringBuilder b = new StringBuilder();
		for (String line = r.readLine(); line != null; line = r.readLine())
			b.append(line+"\n");
		return b;
	}

	private static CharSequence readWiki(BufferedReader r) throws IOException {
		StringBuilder b = new StringBuilder("<plugins>\n");
		for (String line = r.readLine(); line != null; line = r.readLine()) {
			Matcher m = wiki.matcher(line);
			if (!m.matches())
				continue;
			b.append("  <plugin>\n");
			b.append("    <name>"+escape(m.group(2))+"</name>\n");
			b.append("    <resource>"+escape(m.group(1))+"</resource>\n");
			b.append("    <author>"+escape(m.group(3))+"</author>\n");
			b.append("    <description>"+escape(m.group(4))+"</description>\n");
			b.append("    <version>"+escape(m.group(5))+"</version>\n");
			b.append("  </plugin>\n");
		}
		b.append("</plugins>\n");
		return b;
	}

	private static String escape(String s) {
		return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	public static boolean downloadPlugin(PluginDescription pd) {
		File file = new File(Main.pref.getPreferencesDir()+"plugins/"+pd.name+".jar");
		if (!download(pd.resource, file)) {
			JOptionPane.showMessageDialog(Main.parent, tr("Could not download plugin: {0} from {1}", pd.name, pd.resource));
		} else {
			try {
				PluginInformation.findPlugin(pd.name);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Main.parent, tr("The plugin {0} seem to be broken or could not be downloaded automatically.", pd.name));
			}
		}
		if (file.exists())
			file.delete();
		return false;
	}

	private static boolean download(URL url, File file) {
		try {
			InputStream in = url.openStream();
			OutputStream out = new FileOutputStream(file);
			byte[] buffer = new byte[8192];
			for (int read = in.read(buffer); read != -1; read = in.read(buffer))
				out.write(buffer, 0, read);
			out.close();
			in.close();
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void update(Collection<PluginDescription> update) {
		Main.worker.execute(new UpdateTask(update));
	}
}
