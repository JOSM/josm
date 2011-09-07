// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Collection;

/**
 * Basic utils, that can be useful in different parts of the program.
 */
public class Utils {

    public static <T> boolean exists(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        for (T item : collection) {
            if (predicate.evaluate(item))
                return true;
        }
        return false;
    }

    public static <T> boolean exists(Iterable<T> collection, Class<? extends T> klass) {
        for (Object item : collection) {
            if (klass.isInstance(item))
                return true;
        }
        return false;
    }

    public static <T> T find(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        for (T item : collection) {
            if (predicate.evaluate(item))
                return item;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T find(Iterable<? super T> collection, Class<? extends T> klass) {
        for (Object item : collection) {
            if (klass.isInstance(item))
                return (T) item;
        }
        return null;
    }
    
    /**
     * Filter a collection by (sub)class.
     * This is an efficient read-only implementation.
     */
    public static <S, T extends S> SubclassFilteredCollection<S, T> filteredCollection(Collection<S> collection, final Class<T> klass) {
        return new SubclassFilteredCollection<S, T>(collection, new Predicate<S>() {
            @Override
            public boolean evaluate(S o) {
                return klass.isInstance(o);
            }
        });
    }

    public static <T> int indexOf(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        int i = 0;
        for (T item : collection) {
            if (predicate.evaluate(item))
                return i;
            i++;
        }
        return -1;
    }

    /**
     * Get minimum of 3 values
     */
    public static int min(int a, int b, int c) {
        if (b < c) {
            if (a < b)
                return a;
            return b;
        } else {
            if (a < c)
                return a;
            return c;
        }
    }

    public static int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * for convenience: test whether 2 objects are either both null or a.equals(b)
     */
    public static <T> boolean equal(T a, T b) {
        if (a == b)
            return true;
        return (a != null && a.equals(b));
    }

    public static void ensure(boolean condition, String message, Object...data) {
        if (!condition)
            throw new AssertionError(
                    MessageFormat.format(message,data)
            );
    }

    /**
     * return the modulus in the range [0, n)
     */
    public static int mod(int a, int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        int res = a % n;
        if (res < 0) {
            res += n;
        }
        return res;
    }

    /**
     * Joins a list of strings (or objects that can be converted to string via
     * Object.toString()) into a single string with fields separated by sep.
     * @param sep the separator
     * @param values collection of objects, null is converted to the
     *  empty string
     * @return null if values is null. The joined string otherwise.
     */
    public static String join(String sep, Collection<?> values) {
        if (sep == null)
            throw new IllegalArgumentException();
        if (values == null)
            return null;
        if (values.isEmpty())
            return "";
        StringBuilder s = null;
        for (Object a : values) {
            if (a == null) {
                a = "";
            }
            if (s != null) {
                s.append(sep).append(a.toString());
            } else {
                s = new StringBuilder(a.toString());
            }
        }
        return s.toString();
    }

    /**
     * convert Color to String
     * (Color.toString() omits alpha value)
     */
    public static String toString(Color c) {
        if (c == null)
            return "null";
        if (c.getAlpha() == 255)
            return String.format("#%06x", c.getRGB() & 0x00ffffff);
        else
            return String.format("#%06x(alpha=%d)", c.getRGB() & 0x00ffffff, c.getAlpha());
    }

    /**
     * convert float range 0 <= x <= 1 to integer range 0..255
     * when dealing with colors and color alpha value
     * @return null if val is null, the corresponding int if val is in the
     *         range 0...1. If val is outside that range, return 255
     */
    public static Integer color_float2int(Float val) {
        if (val == null)
            return null;
        if (val < 0 || val > 1)
            return 255;
        return (int) (255f * val + 0.5f);
    }

    /**
     * convert back
     */
    public static Float color_int2float(Integer val) {
        if (val == null)
            return null;
        if (val < 0 || val > 255)
            return 1f;
        return ((float) val) / 255f;
    }

    public static Color complement(Color clr) {
        return new Color(255 - clr.getRed(), 255 - clr.getGreen(), 255 - clr.getBlue(), clr.getAlpha());
    }

    public static int copyStream(InputStream source, OutputStream destination) throws IOException {
        int count = 0;
        byte[] b = new byte[512];
        int read;
        while ((read = source.read(b)) != -1) {
            count += read;
            destination.write(b, 0, read);
        }
        return count;
    }

    public static boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return( path.delete() );
    }

    /**
     * <p>Utility method for closing an input stream.</p>
     * 
     * @param is the input stream. May be null.
     */
    public static void close(InputStream is){
        if (is == null) return;
        try {
            is.close();
        } catch(IOException e){
            // ignore
        }
    }

    /**
     * <p>Utility method for closing an output stream.</p>
     * 
     * @param os the output stream. May be null.
     */
    public static void close(OutputStream os){
        if (os == null) return;
        try {
            os.close();
        } catch(IOException e){
            // ignore
        }
    }

    /**
     * <p>Utility method for closing a reader.</p>
     * 
     * @param reader the reader. May be null.
     */
    public static void close(Reader reader){
        if (reader == null) return;
        try {
            reader.close();
        } catch(IOException e){
            // ignore
        }
    }

    private final static double EPSILION = 1e-11;

    public static boolean equalsEpsilon(double a, double b) {
        return Math.abs(a - b) <= EPSILION;
    }

    /**
     * Copies the string {@code s} to system clipboard.
     * @param s string to be copied to clipboard.
     * @return true if succeeded, false otherwise.
     */
    public static boolean copyToClipboard(String s) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), new ClipboardOwner() {

                @Override
                public void lostOwnership(Clipboard clpbrd, Transferable t) {
                }
            });
            return true;
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts clipboard content as string.
     * @return string clipboard contents if available, {@code null} otherwise.
     */
    public static String getClipboardContent() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                return text;
            }
        } catch (UnsupportedFlavorException ex) {
            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Calculate MD5 hash of a string and output in hexadecimal format.
     * Output has length 32 with characters in range [0-9a-f]
     */
    public static String md5Hex(String data) {
        byte[] byteData = null;
        try {
            byteData = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }
        byte[] byteDigest = md.digest(byteData);
        return toHexString(byteDigest);
    }

    /**
     * Converts a byte array to a string of hexadecimal characters. Preserves leading zeros, so the
     * size of the output string is always twice the number of input bytes.
     */
    public static String toHexString(byte[] bytes) {
        char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        char[] hexChars = new char[bytes.length * 2];
        for (int j=0; j<bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v/16];
            hexChars[j*2 + 1] = hexArray[v%16];
        }
        return new String(hexChars);
    }
}
