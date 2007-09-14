// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.openstreetmap.josm.gui.PleaseWaitDialog;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressInputStream extends InputStream {

	private final InputStream in;
	private int readSoFar = 0;
	private int lastDialogUpdate = 0;
	private final URLConnection connection;
	private PleaseWaitDialog pleaseWaitDlg;

	public class OsmServerException extends IOException {
		private OsmServerException(String e) {
			super(e);
		}
	}

	public ProgressInputStream(URLConnection con, PleaseWaitDialog pleaseWaitDlg) throws IOException, OsmServerException {
		this.connection = con;

		try {
			this.in = con.getInputStream();
		} catch (IOException e) {
			if (con.getHeaderField("Error") != null)
				throw new OsmServerException(con.getHeaderField("Error"));
			throw e;
		}

		int contentLength = con.getContentLength();
		this.pleaseWaitDlg = pleaseWaitDlg;
		if (pleaseWaitDlg == null)
			return;
		if (contentLength > 0)
			pleaseWaitDlg.progress.setMaximum(contentLength);
		else
			pleaseWaitDlg.progress.setMaximum(0);
		pleaseWaitDlg.progress.setValue(0);
	}

	@Override public void close() throws IOException {
		in.close();
	}

	@Override public int read(byte[] b, int off, int len) throws IOException {
		int read = in.read(b, off, len);
		if (read != -1)
			advanceTicker(read);
		return read;
	}

	@Override public int read() throws IOException {
		int read = in.read();
		if (read != -1)
			advanceTicker(1);
		return read;
	}

	/**
	 * Increase ticker (progress counter and displayed text) by the given amount.
	 * @param amount
	 */
	private void advanceTicker(int amount) {
		if (pleaseWaitDlg == null)
			return;

		if (pleaseWaitDlg.progress.getMaximum() == 0 && connection.getContentLength() != -1)
			pleaseWaitDlg.progress.setMaximum(connection.getContentLength());

		readSoFar += amount;

		if (readSoFar / 1024 != lastDialogUpdate) {
			lastDialogUpdate++;
			String progStr = " "+readSoFar/1024+"/";
			progStr += (pleaseWaitDlg.progress.getMaximum()==0) ? "??? KB" : (pleaseWaitDlg.progress.getMaximum()/1024)+" KB";
			pleaseWaitDlg.progress.setValue(readSoFar);

			String cur = pleaseWaitDlg.currentAction.getText();
			int i = cur.indexOf(' ');
			if (i != -1)
				cur = cur.substring(0, i) + progStr;
			else
				cur += progStr;
			pleaseWaitDlg.currentAction.setText(cur);
		}
	}
}
