// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.openstreetmap.josm.Main;

/**
 * Mirrors a file to a local file.
 * <p>
 * The file mirrored is only downloaded if it has been more than one day since last download
 *
 * @param url The URL of the remote file
 * @param destDir The destination dir of the mirrored file
 * @param maxTime The time interval, in seconds, to check if the file changed. If less than 0, it defaults to 1 week
 * @return The local file
 */
public class MirroredInputStream extends InputStream {
	InputStream fs = null;

	public MirroredInputStream(String name) throws IOException
	{
		this(name, null, -1L);
	}

	public MirroredInputStream(String name, long maxTime) throws IOException
	{
		this(name, null, maxTime);
	}

	public MirroredInputStream(String name, String destDir, long maxTime) throws IOException
	{
		URL url;
		File file = null;
		try
		{
			url = new URL(name);
			if(url.getProtocol().equals("file"))
			{
				file = new File(name.substring("file:/".length()));
				if(!file.exists())
					file = new File(name.substring("file://".length()));
			}
			else
				file = checkLocal(url, destDir, maxTime);
		}
		catch(java.net.MalformedURLException e)
		{
			if(name.startsWith("resource://"))
			{
				fs = getClass().getResourceAsStream(
				name.substring("resource:/".length()));
				return;
			}
			else
				file = new File(name);
		}
		if(file == null)
			throw new IOException();
		fs = new FileInputStream(file);
	}

	private File checkLocal(URL url, String destDir, long maxTime)
	{
		String localPath = Main.pref.get("mirror." + url);
		File file = null;
		if(localPath != null && localPath.length() > 0)
		{
			String[] lp = localPath.split(";");
			file = new File(lp[1]);
			if(maxTime <= 0)
				maxTime = Main.pref.getInteger("mirror.maxtime", 7*24*60*60);
			if(System.currentTimeMillis() - Long.parseLong(lp[0]) < maxTime*1000)
			{
				if(file.exists())
				{
					return file;
				}
			}
		}
		if(destDir == null)
			destDir = Main.pref.getPreferencesDir();

		File destDirFile = new File(destDir);
		if(!destDirFile.exists() )
			destDirFile.mkdirs();

		localPath = "mirror_" + new File(url.getPath()).getName();
		destDirFile = new File(destDir, localPath + ".tmp");
		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;
		try
		{
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(5000);
			bis = new BufferedInputStream(conn.getInputStream());
			bos = new BufferedOutputStream( new FileOutputStream(destDirFile));
			byte[] buffer = new byte[4096];
			int length;
			while((length = bis.read(buffer)) > -1)
				bos.write(buffer, 0, length);
		}
		catch(IOException ioe)
		{
			if(file != null)
				return file;
			else
				return null;
		}
		finally
		{
			if(bis != null)
			{
				try
				{
					bis.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			if(bos != null)
			{
				try
				{
					bos.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			file = new File(destDir, localPath);
			destDirFile.renameTo(file);
			Main.pref.put("mirror." + url, System.currentTimeMillis() + ";" + file);
		}

		return file;
	}
	public int available() throws IOException
	{ return fs.available(); }
	public void close() throws IOException
	{ fs.close(); }
	public int read() throws IOException
	{ return fs.read(); }
	public int read(byte[] b) throws IOException
	{ return fs.read(b); }
	public int read(byte[] b, int off, int len) throws IOException
	{ return fs.read(b,off, len); }
	public long skip(long n) throws IOException
	{ return fs.skip(n); }
}
