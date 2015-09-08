// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Basic utils, that can be useful in different parts of the program.
 */
public final class Utils {

    public static final Pattern WHITE_SPACES_PATTERN = Pattern.compile("\\s+");

    private Utils() {
        // Hide default constructor for utils classes
    }

    private static final int MILLIS_OF_SECOND = 1000;
    private static final int MILLIS_OF_MINUTE = 60000;
    private static final int MILLIS_OF_HOUR = 3600000;
    private static final int MILLIS_OF_DAY = 86400000;

    public static final String URL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=%";

    private static char[] DEFAULT_STRIP = {'\u200B', '\uFEFF'};

    /**
     * Tests whether {@code predicate} applies to at least one elements from {@code collection}.
     */
    public static <T> boolean exists(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        for (T item : collection) {
            if (predicate.evaluate(item))
                return true;
        }
        return false;
    }

    /**
     * Tests whether {@code predicate} applies to all elements from {@code collection}.
     */
    public static <T> boolean forAll(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        return !exists(collection, Predicates.not(predicate));
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

    public static <T> Collection<T> filter(Collection<? extends T> collection, Predicate<? super T> predicate) {
        return new FilteredCollection<>(collection, predicate);
    }

    /**
     * Returns the first element from {@code items} which is non-null, or null if all elements are null.
     * @param items the items to look for
     * @return first non-null item if there is one
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... items) {
        for (T i : items) {
            if (i != null) {
                return i;
            }
        }
        return null;
    }

    /**
     * Filter a collection by (sub)class.
     * This is an efficient read-only implementation.
     */
    public static <S, T extends S> SubclassFilteredCollection<S, T> filteredCollection(Collection<S> collection, final Class<T> klass) {
        return new SubclassFilteredCollection<>(collection, new Predicate<S>() {
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
     * Returns the minimum of three values.
     * @param   a   an argument.
     * @param   b   another argument.
     * @param   c   another argument.
     * @return  the smaller of {@code a}, {@code b} and {@code c}.
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

    /**
     * Returns the greater of four {@code int} values. That is, the
     * result is the argument closer to the value of
     * {@link Integer#MAX_VALUE}. If the arguments have the same value,
     * the result is that same value.
     *
     * @param   a   an argument.
     * @param   b   another argument.
     * @param   c   another argument.
     * @param   d   another argument.
     * @return  the larger of {@code a}, {@code b}, {@code c} and {@code d}.
     */
    public static int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Ensures a logical condition is met. Otherwise throws an assertion error.
     * @param condition the condition to be met
     * @param message Formatted error message to raise if condition is not met
     * @param data Message parameters, optional
     * @throws AssertionError if the condition is not met
     */
    public static void ensure(boolean condition, String message, Object...data) {
        if (!condition)
            throw new AssertionError(
                    MessageFormat.format(message, data)
            );
    }

    /**
     * return the modulus in the range [0, n)
     */
    public static int mod(int a, int n) {
        if (n <= 0)
            throw new IllegalArgumentException("n must be <= 0 but is "+n);
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
        CheckParameterUtil.ensureParameterNotNull(sep, "sep");
        if (values == null)
            return null;
        StringBuilder s = null;
        for (Object a : values) {
            if (a == null) {
                a = "";
            }
            if (s != null) {
                s.append(sep).append(a);
            } else {
                s = new StringBuilder(a.toString());
            }
        }
        return s != null ? s.toString() : "";
    }

    /**
     * Converts the given iterable collection as an unordered HTML list.
     * @param values The iterable collection
     * @return An unordered HTML list
     */
    public static String joinAsHtmlUnorderedList(Iterable<?> values) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<ul>");
        for (Object i : values) {
            sb.append("<li>").append(i).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
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
     * convert float range 0 &lt;= x &lt;= 1 to integer range 0..255
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
     * convert integer range 0..255 to float range 0 &lt;= x &lt;= 1
     * when dealing with colors and color alpha value
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

    /**
     * Copies the given array. Unlike {@link Arrays#copyOf}, this method is null-safe.
     * @param array The array to copy
     * @return A copy of the original array, or {@code null} if {@code array} is null
     * @since 6221
     */
    public static <T> T[] copyArray(T[] array) {
        if (array != null) {
            return Arrays.copyOf(array, array.length);
        }
        return null;
    }

    /**
     * Copies the given array. Unlike {@link Arrays#copyOf}, this method is null-safe.
     * @param array The array to copy
     * @return A copy of the original array, or {@code null} if {@code array} is null
     * @since 6222
     */
    public static char[] copyArray(char[] array) {
        if (array != null) {
            return Arrays.copyOf(array, array.length);
        }
        return null;
    }

    /**
     * Copies the given array. Unlike {@link Arrays#copyOf}, this method is null-safe.
     * @param array The array to copy
     * @return A copy of the original array, or {@code null} if {@code array} is null
     * @since 7436
     */
    public static int[] copyArray(int[] array) {
        if (array != null) {
            return Arrays.copyOf(array, array.length);
        }
        return null;
    }

    /**
     * Simple file copy function that will overwrite the target file.
     * @param in The source file
     * @param out The destination file
     * @return the path to the target file
     * @throws IOException if any I/O error occurs
     * @throws IllegalArgumentException if {@code in} or {@code out} is {@code null}
     * @since 7003
     */
    public static Path copyFile(File in, File out) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(in, "in");
        CheckParameterUtil.ensureParameterNotNull(out, "out");
        return Files.copy(in.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Recursive directory copy function
     * @param in The source directory
     * @param out The destination directory
     * @throws IOException if any I/O error ooccurs
     * @throws IllegalArgumentException if {@code in} or {@code out} is {@code null}
     * @since 7835
     */
    public static void copyDirectory(File in, File out) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(in, "in");
        CheckParameterUtil.ensureParameterNotNull(out, "out");
        if (!out.exists() && !out.mkdirs()) {
            Main.warn("Unable to create directory "+out.getPath());
        }
        File[] files = in.listFiles();
        if (files != null) {
            for (File f : files) {
                File target = new File(out, f.getName());
                if (f.isDirectory()) {
                    copyDirectory(f, target);
                } else {
                    copyFile(f, target);
                }
            }
        }
    }

    /**
     * Copy data from source stream to output stream.
     * @param source source stream
     * @param destination target stream
     * @return number of bytes copied
     * @throws IOException if any I/O error occurs
     */
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

    /**
     * Deletes a directory recursively.
     * @param path The directory to delete
     * @return  <code>true</code> if and only if the file or directory is
     *          successfully deleted; <code>false</code> otherwise
     */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else if (!file.delete()) {
                        Main.warn("Unable to delete file: "+file.getPath());
                    }
                }
            }
        }
        return path.delete();
    }

    /**
     * <p>Utility method for closing a {@link java.io.Closeable} object.</p>
     *
     * @param c the closeable object. May be null.
     */
    public static void close(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException e) {
            Main.warn(e);
        }
    }

    /**
     * <p>Utility method for closing a {@link java.util.zip.ZipFile}.</p>
     *
     * @param zip the zip file. May be null.
     */
    public static void close(ZipFile zip) {
        if (zip == null) return;
        try {
            zip.close();
        } catch (IOException e) {
            Main.warn(e);
        }
    }

    /**
     * Converts the given file to its URL.
     * @param f The file to get URL from
     * @return The URL of the given file, or {@code null} if not possible.
     * @since 6615
     */
    public static URL fileToURL(File f) {
        if (f != null) {
            try {
                return f.toURI().toURL();
            } catch (MalformedURLException ex) {
                Main.error("Unable to convert filename " + f.getAbsolutePath() + " to URL");
            }
        }
        return null;
    }

    private static final double EPSILON = 1e-11;

    /**
     * Determines if the two given double values are equal (their delta being smaller than a fixed epsilon)
     * @param a The first double value to compare
     * @param b The second double value to compare
     * @return {@code true} if {@code abs(a - b) <= 1e-11}, {@code false} otherwise
     */
    public static boolean equalsEpsilon(double a, double b) {
        return Math.abs(a - b) <= EPSILON;
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
                    // Do nothing
                }
            });
            return true;
        } catch (IllegalStateException ex) {
            Main.error(ex);
            return false;
        }
    }

    /**
     * Extracts clipboard content as {@code Transferable} object.
     * @param clipboard clipboard from which contents are retrieved
     * @return clipboard contents if available, {@code null} otherwise.
     * @since 8429
     */
    public static Transferable getTransferableContent(Clipboard clipboard) {
        Transferable t = null;
        for (int tries = 0; t == null && tries < 10; tries++) {
            try {
                t = clipboard.getContents(null);
            } catch (IllegalStateException e) {
                // Clipboard currently unavailable.
                // On some platforms, the system clipboard is unavailable while it is accessed by another application.
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Main.warn("InterruptedException in "+Utils.class.getSimpleName()+" while getting clipboard content");
                }
            } catch (NullPointerException e) {
                // JDK-6322854: On Linux/X11, NPE can happen for unknown reasons, on all versions of Java
                Main.error(e);
            }
        }
        return t;
    }

    /**
     * Extracts clipboard content as string.
     * @return string clipboard contents if available, {@code null} otherwise.
     */
    public static String getClipboardContent() {
        Transferable t = getTransferableContent(Toolkit.getDefaultToolkit().getSystemClipboard());
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) t.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Main.error(ex);
            return null;
        }
        return null;
    }

    /**
     * Calculate MD5 hash of a string and output in hexadecimal format.
     * @param data arbitrary String
     * @return MD5 hash of data, string of length 32 with characters in range [0-9a-f]
     */
    public static String md5Hex(String data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] byteData = data.getBytes(StandardCharsets.UTF_8);
        byte[] byteDigest = md.digest(byteData);
        return toHexString(byteDigest);
    }

    private static final char[] HEX_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Converts a byte array to a string of hexadecimal characters.
     * Preserves leading zeros, so the size of the output string is always twice
     * the number of input bytes.
     * @param bytes the byte array
     * @return hexadecimal representation
     */
    public static String toHexString(byte[] bytes) {

        if (bytes == null) {
            return "";
        }

        final int len = bytes.length;
        if (len == 0) {
            return "";
        }

        char[] hexChars = new char[len * 2];
        for (int i = 0, j = 0; i < len; i++) {
            final int v = bytes[i];
            hexChars[j++] = HEX_ARRAY[(v & 0xf0) >> 4];
            hexChars[j++] = HEX_ARRAY[v & 0xf];
        }
        return new String(hexChars);
    }

    /**
     * Topological sort.
     *
     * @param dependencies contains mappings (key -&gt; value). In the final list of sorted objects, the key will come
     * after the value. (In other words, the key depends on the value(s).)
     * There must not be cyclic dependencies.
     * @return the list of sorted objects
     */
    public static <T> List<T> topologicalSort(final MultiMap<T, T> dependencies) {
        MultiMap<T, T> deps = new MultiMap<>();
        for (T key : dependencies.keySet()) {
            deps.putVoid(key);
            for (T val : dependencies.get(key)) {
                deps.putVoid(val);
                deps.put(key, val);
            }
        }

        int size = deps.size();
        List<T> sorted = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            T parentless = null;
            for (T key : deps.keySet()) {
                if (deps.get(key).isEmpty()) {
                    parentless = key;
                    break;
                }
            }
            if (parentless == null) throw new RuntimeException();
            sorted.add(parentless);
            deps.remove(parentless);
            for (T key : deps.keySet()) {
                deps.remove(key, parentless);
            }
        }
        if (sorted.size() != size) throw new RuntimeException();
        return sorted;
    }

    /**
     * Represents a function that can be applied to objects of {@code A} and
     * returns objects of {@code B}.
     * @param <A> class of input objects
     * @param <B> class of transformed objects
     */
    public interface Function<A, B> {

        /**
         * Applies the function on {@code x}.
         * @param x an object of
         * @return the transformed object
         */
        B apply(A x);
    }

    /**
     * Transforms the collection {@code c} into an unmodifiable collection and
     * applies the {@link org.openstreetmap.josm.tools.Utils.Function} {@code f} on each element upon access.
     * @param <A> class of input collection
     * @param <B> class of transformed collection
     * @param c a collection
     * @param f a function that transforms objects of {@code A} to objects of {@code B}
     * @return the transformed unmodifiable collection
     */
    public static <A, B> Collection<B> transform(final Collection<? extends A> c, final Function<A, B> f) {
        return new AbstractCollection<B>() {

            @Override
            public int size() {
                return c.size();
            }

            @Override
            public Iterator<B> iterator() {
                return new Iterator<B>() {

                    private Iterator<? extends A> it = c.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public B next() {
                        return f.apply(it.next());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Transforms the list {@code l} into an unmodifiable list and
     * applies the {@link org.openstreetmap.josm.tools.Utils.Function} {@code f} on each element upon access.
     * @param <A> class of input collection
     * @param <B> class of transformed collection
     * @param l a collection
     * @param f a function that transforms objects of {@code A} to objects of {@code B}
     * @return the transformed unmodifiable list
     */
    public static <A, B> List<B> transform(final List<? extends A> l, final Function<A, B> f) {
        return new AbstractList<B>() {

            @Override
            public int size() {
                return l.size();
            }

            @Override
            public B get(int index) {
                return f.apply(l.get(index));
            }
        };
    }

    private static final Pattern HTTP_PREFFIX_PATTERN = Pattern.compile("https?");

    /**
     * Opens a HTTP connection to the given URL and sets the User-Agent property to JOSM's one.
     * @param httpURL The HTTP url to open (must use http:// or https://)
     * @return An open HTTP connection to the given URL
     * @throws java.io.IOException if an I/O exception occurs.
     * @since 5587
     */
    public static HttpURLConnection openHttpConnection(URL httpURL) throws IOException {
        if (httpURL == null || !HTTP_PREFFIX_PATTERN.matcher(httpURL.getProtocol()).matches()) {
            throw new IllegalArgumentException("Invalid HTTP url");
        }
        if (Main.isDebugEnabled()) {
            Main.debug("Opening HTTP connection to "+httpURL.toExternalForm());
        }
        HttpURLConnection connection = (HttpURLConnection) httpURL.openConnection();
        connection.setRequestProperty("User-Agent", Version.getInstance().getFullAgentString());
        connection.setUseCaches(false);
        return connection;
    }

    /**
     * Opens a connection to the given URL and sets the User-Agent property to JOSM's one.
     * @param url The url to open
     * @return An stream for the given URL
     * @throws java.io.IOException if an I/O exception occurs.
     * @since 5867
     */
    public static InputStream openURL(URL url) throws IOException {
        return openURLAndDecompress(url, false);
    }

    /**
     * Opens a connection to the given URL, sets the User-Agent property to JOSM's one, and decompresses stream if necessary.
     * @param url The url to open
     * @param decompress whether to wrap steam in a {@link GZIPInputStream} or {@link BZip2CompressorInputStream}
     *                   if the {@code Content-Type} header is set accordingly.
     * @return An stream for the given URL
     * @throws IOException if an I/O exception occurs.
     * @since 6421
     */
    public static InputStream openURLAndDecompress(final URL url, final boolean decompress) throws IOException {
        final URLConnection connection = setupURLConnection(url.openConnection());
        final InputStream in = connection.getInputStream();
        if (decompress) {
            switch (connection.getHeaderField("Content-Type")) {
            case "application/zip":
                return getZipInputStream(in);
            case "application/x-gzip":
                return getGZipInputStream(in);
            case "application/x-bzip2":
                return getBZip2InputStream(in);
            }
        }
        return in;
    }

    /**
     * Returns a Bzip2 input stream wrapping given input stream.
     * @param in The raw input stream
     * @return a Bzip2 input stream wrapping given input stream, or {@code null} if {@code in} is {@code null}
     * @throws IOException if the given input stream does not contain valid BZ2 header
     * @since 7867
     */
    public static BZip2CompressorInputStream getBZip2InputStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        return new BZip2CompressorInputStream(in, /* see #9537 */ true);
    }

    /**
     * Returns a Gzip input stream wrapping given input stream.
     * @param in The raw input stream
     * @return a Gzip input stream wrapping given input stream, or {@code null} if {@code in} is {@code null}
     * @throws IOException if an I/O error has occurred
     * @since 7119
     */
    public static GZIPInputStream getGZipInputStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        return new GZIPInputStream(in);
    }

    /**
     * Returns a Zip input stream wrapping given input stream.
     * @param in The raw input stream
     * @return a Zip input stream wrapping given input stream, or {@code null} if {@code in} is {@code null}
     * @throws IOException if an I/O error has occurred
     * @since 7119
     */
    public static ZipInputStream getZipInputStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8);
        // Positions the stream at the beginning of first entry
        ZipEntry ze = zis.getNextEntry();
        if (ze != null && Main.isDebugEnabled()) {
            Main.debug("Zip entry: "+ze.getName());
        }
        return zis;
    }

    /***
     * Setups the given URL connection to match JOSM needs by setting its User-Agent and timeout properties.
     * @param connection The connection to setup
     * @return {@code connection}, with updated properties
     * @since 5887
     */
    public static URLConnection setupURLConnection(URLConnection connection) {
        if (connection != null) {
            connection.setRequestProperty("User-Agent", Version.getInstance().getFullAgentString());
            connection.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect", 15)*1000);
            connection.setReadTimeout(Main.pref.getInteger("socket.timeout.read", 30)*1000);
        }
        return connection;
    }

    /**
     * Opens a connection to the given URL and sets the User-Agent property to JOSM's one.
     * @param url The url to open
     * @return An buffered stream reader for the given URL (using UTF-8)
     * @throws java.io.IOException if an I/O exception occurs.
     * @since 5868
     */
    public static BufferedReader openURLReader(URL url) throws IOException {
        return openURLReaderAndDecompress(url, false);
    }

    /**
     * Opens a connection to the given URL and sets the User-Agent property to JOSM's one.
     * @param url The url to open
     * @param decompress whether to wrap steam in a {@link GZIPInputStream} or {@link BZip2CompressorInputStream}
     *                   if the {@code Content-Type} header is set accordingly.
     * @return An buffered stream reader for the given URL (using UTF-8)
     * @throws IOException if an I/O exception occurs.
     * @since 6421
     */
    public static BufferedReader openURLReaderAndDecompress(final URL url, final boolean decompress) throws IOException {
        return new BufferedReader(new InputStreamReader(openURLAndDecompress(url, decompress), StandardCharsets.UTF_8));
    }

    /**
     * Opens a HTTP connection to the given URL, sets the User-Agent property to JOSM's one and optionnaly disables Keep-Alive.
     * @param httpURL The HTTP url to open (must use http:// or https://)
     * @param keepAlive whether not to set header {@code Connection=close}
     * @return An open HTTP connection to the given URL
     * @throws java.io.IOException if an I/O exception occurs.
     * @since 5587
     */
    public static HttpURLConnection openHttpConnection(URL httpURL, boolean keepAlive) throws IOException {
        HttpURLConnection connection = openHttpConnection(httpURL);
        if (!keepAlive) {
            connection.setRequestProperty("Connection", "close");
        }
        if (Main.isDebugEnabled()) {
            try {
                Main.debug("REQUEST: "+ connection.getRequestProperties());
            } catch (IllegalStateException e) {
                Main.warn(e);
            }
        }
        return connection;
    }

    /**
     * Opens a HTTP connection to given URL, sets the User-Agent property to JOSM's one, optionally disables Keep-Alive, and
     * optionally - follows redirects. It means, that it's not possible to send custom headers with method
     *
     * @param httpURL The HTTP url to open (must use http:// or https://)
     * @param keepAlive whether not to set header {@code Connection=close}
     * @param followRedirects wheter or not to follow HTTP(S) redirects
     * @return An open HTTP connection to the given URL
     * @throws IOException if an I/O exception occurs
     * @since 8650
     */
    public static HttpURLConnection openHttpConnection(URL httpURL, boolean keepAlive, boolean followRedirects) throws IOException {
        HttpURLConnection connection = openHttpConnection(httpURL, keepAlive);
        if (followRedirects) {
            for (int i = 0; i < 5; i++) {
                if (connection.getResponseCode() == 302) {
                    connection = openHttpConnection(new URL(connection.getHeaderField("Location")), keepAlive);
                } else {
                    break;
                }
            }
        }
        return connection;
    }

    /**
     * An alternative to {@link String#trim()} to effectively remove all leading and trailing white characters, including Unicode ones.
     * @param str The string to strip
     * @return <code>str</code>, without leading and trailing characters, according to
     *         {@link Character#isWhitespace(char)} and {@link Character#isSpaceChar(char)}.
     * @see <a href="http://closingbraces.net/2008/11/11/javastringtrim/">Java’s String.trim has a strange idea of whitespace</a>
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-4080617">JDK bug 4080617</a>
     * @since 5772
     */
    public static String strip(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return strip(str, DEFAULT_STRIP);
    }

    /**
     * An alternative to {@link String#trim()} to effectively remove all leading and trailing white characters, including Unicode ones.
     * @param str The string to strip
     * @param skipChars additional characters to skip
     * @return <code>str</code>, without leading and trailing characters, according to
     *         {@link Character#isWhitespace(char)}, {@link Character#isSpaceChar(char)} and skipChars.
     * @since 8435
     */
    public static String strip(final String str, final String skipChars) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return strip(str, stripChars(skipChars));
    }

    private static String strip(final String str, final char[] skipChars) {

        int start = 0;
        int end = str.length();
        boolean leadingSkipChar = true;
        while (leadingSkipChar && start < end) {
            char c = str.charAt(start);
            leadingSkipChar = Character.isWhitespace(c) || Character.isSpaceChar(c) || stripChar(skipChars, c);
            if (leadingSkipChar) {
                start++;
            }
        }
        boolean trailingSkipChar = true;
        while (trailingSkipChar && end > start + 1) {
            char c = str.charAt(end - 1);
            trailingSkipChar = Character.isWhitespace(c) || Character.isSpaceChar(c) || stripChar(skipChars, c);
            if (trailingSkipChar) {
                end--;
            }
        }

        return str.substring(start, end);
    }

    private static char[] stripChars(final String skipChars) {
        if (skipChars == null || skipChars.isEmpty()) {
            return DEFAULT_STRIP;
        }

        char[] chars = new char[DEFAULT_STRIP.length + skipChars.length()];
        System.arraycopy(DEFAULT_STRIP, 0, chars, 0, DEFAULT_STRIP.length);
        skipChars.getChars(0, skipChars.length(), chars, DEFAULT_STRIP.length);

        return chars;
    }

    private static boolean stripChar(final char[] strip, char c) {
        for (char s : strip) {
            if (c == s) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs an external command and returns the standard output.
     *
     * The program is expected to execute fast.
     *
     * @param command the command with arguments
     * @return the output
     * @throws IOException when there was an error, e.g. command does not exist
     */
    public static String execOutput(List<String> command) throws IOException {
        if (Main.isDebugEnabled()) {
            Main.debug(join(" ", command));
        }
        Process p = new ProcessBuilder(command).start();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder all = null;
            String line;
            while ((line = input.readLine()) != null) {
                if (all == null) {
                    all = new StringBuilder(line);
                } else {
                    all.append('\n');
                    all.append(line);
                }
            }
            return all != null ? all.toString() : null;
        }
    }

    /**
     * Returns the JOSM temp directory.
     * @return The JOSM temp directory ({@code <java.io.tmpdir>/JOSM}), or {@code null} if {@code java.io.tmpdir} is not defined
     * @since 6245
     */
    public static File getJosmTempDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir == null) {
            return null;
        }
        File josmTmpDir = new File(tmpDir, "JOSM");
        if (!josmTmpDir.exists() && !josmTmpDir.mkdirs()) {
            Main.warn("Unable to create temp directory " + josmTmpDir);
        }
        return josmTmpDir;
    }

    /**
     * Returns a simple human readable (hours, minutes, seconds) string for a given duration in milliseconds.
     * @param elapsedTime The duration in milliseconds
     * @return A human readable string for the given duration
     * @throws IllegalArgumentException if elapsedTime is &lt; 0
     * @since 6354
     */
    public static String getDurationString(long elapsedTime) {
        if (elapsedTime < 0) {
            throw new IllegalArgumentException("elapsedTime must be >= 0");
        }
        // Is it less than 1 second ?
        if (elapsedTime < MILLIS_OF_SECOND) {
            return String.format("%d %s", elapsedTime, tr("ms"));
        }
        // Is it less than 1 minute ?
        if (elapsedTime < MILLIS_OF_MINUTE) {
            return String.format("%.1f %s", elapsedTime / (double) MILLIS_OF_SECOND, tr("s"));
        }
        // Is it less than 1 hour ?
        if (elapsedTime < MILLIS_OF_HOUR) {
            final long min = elapsedTime / MILLIS_OF_MINUTE;
            return String.format("%d %s %d %s", min, tr("min"), (elapsedTime - min * MILLIS_OF_MINUTE) / MILLIS_OF_SECOND, tr("s"));
        }
        // Is it less than 1 day ?
        if (elapsedTime < MILLIS_OF_DAY) {
            final long hour = elapsedTime / MILLIS_OF_HOUR;
            return String.format("%d %s %d %s", hour, tr("h"), (elapsedTime - hour * MILLIS_OF_HOUR) / MILLIS_OF_MINUTE, tr("min"));
        }
        long days = elapsedTime / MILLIS_OF_DAY;
        return String.format("%d %s %d %s", days, trn("day", "days", days), (elapsedTime - days * MILLIS_OF_DAY) / MILLIS_OF_HOUR, tr("h"));
    }

    /**
     * Returns a human readable representation of a list of positions.
     * <p>
     * For instance, {@code [1,5,2,6,7} yields "1-2,5-7
     * @param positionList a list of positions
     * @return a human readable representation
     */
    public static String getPositionListString(List<Integer> positionList) {
        Collections.sort(positionList);
        final StringBuilder sb = new StringBuilder(32);
        sb.append(positionList.get(0));
        int cnt = 0;
        int last = positionList.get(0);
        for (int i = 1; i < positionList.size(); ++i) {
            int cur = positionList.get(i);
            if (cur == last + 1) {
                ++cnt;
            } else if (cnt == 0) {
                sb.append(',').append(cur);
            } else {
                sb.append('-').append(last);
                sb.append(',').append(cur);
                cnt = 0;
            }
            last = cur;
        }
        if (cnt >= 1) {
            sb.append('-').append(last);
        }
        return sb.toString();
    }

    /**
     * Returns a list of capture groups if {@link Matcher#matches()}, or {@code null}.
     * The first element (index 0) is the complete match.
     * Further elements correspond to the parts in parentheses of the regular expression.
     * @param m the matcher
     * @return a list of capture groups if {@link Matcher#matches()}, or {@code null}.
     */
    public static List<String> getMatches(final Matcher m) {
        if (m.matches()) {
            List<String> result = new ArrayList<>(m.groupCount() + 1);
            for (int i = 0; i <= m.groupCount(); i++) {
                result.add(m.group(i));
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Cast an object savely.
     * @param <T> the target type
     * @param o the object to cast
     * @param klass the target class (same as T)
     * @return null if <code>o</code> is null or the type <code>o</code> is not
     *  a subclass of <code>klass</code>. The casted value otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o, Class<T> klass) {
        if (klass.isInstance(o)) {
            return (T) o;
        }
        return null;
    }

    /**
     * Returns the root cause of a throwable object.
     * @param t The object to get root cause for
     * @return the root cause of {@code t}
     * @since 6639
     */
    public static Throwable getRootCause(Throwable t) {
        Throwable result = t;
        if (result != null) {
            Throwable cause = result.getCause();
            while (cause != null && !cause.equals(result)) {
                result = cause;
                cause = result.getCause();
            }
        }
        return result;
    }

    /**
     * Adds the given item at the end of a new copy of given array.
     * @param array The source array
     * @param item The item to add
     * @return An extended copy of {@code array} containing {@code item} as additional last element
     * @since 6717
     */
    public static <T> T[] addInArrayCopy(T[] array, T item) {
        T[] biggerCopy = Arrays.copyOf(array, array.length + 1);
        biggerCopy[array.length] = item;
        return biggerCopy;
    }

    /**
     * If the string {@code s} is longer than {@code maxLength}, the string is cut and "..." is appended.
     * @param s String to shorten
     * @param maxLength maximum number of characters to keep (not including the "...")
     * @return the shortened string
     */
    public static String shortenString(String s, int maxLength) {
        if (s != null && s.length() > maxLength) {
            return s.substring(0, maxLength - 3) + "...";
        } else {
            return s;
        }
    }

    /**
     * Fixes URL with illegal characters in the query (and fragment) part by
     * percent encoding those characters.
     *
     * special characters like &amp; and # are not encoded
     *
     * @param url the URL that should be fixed
     * @return the repaired URL
     */
    public static String fixURLQuery(String url) {
        if (url.indexOf('?') == -1)
            return url;

        String query = url.substring(url.indexOf('?') + 1);

        StringBuilder sb = new StringBuilder(url.substring(0, url.indexOf('?') + 1));

        for (int i = 0; i < query.length(); i++) {
            String c = query.substring(i, i + 1);
            if (URL_CHARS.contains(c)) {
                sb.append(c);
            } else {
                sb.append(encodeUrl(c));
            }
        }
        return sb.toString();
    }

    /**
     * Translates a string into <code>application/x-www-form-urlencoded</code>
     * format. This method uses UTF-8 encoding scheme to obtain the bytes for unsafe
     * characters.
     *
     * @param   s <code>String</code> to be translated.
     * @return  the translated <code>String</code>.
     * @see #decodeUrl(String)
     * @since 8304
     */
    public static String encodeUrl(String s) {
        final String enc = StandardCharsets.UTF_8.name();
        try {
            return URLEncoder.encode(s, enc);
        } catch (UnsupportedEncodingException e) {
            Main.error(e);
            return null;
        }
    }

    /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string.
     * UTF-8 encoding is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "<code>%<i>xy</i></code>".
     *
     * @param s the <code>String</code> to decode
     * @return the newly decoded <code>String</code>
     * @see #encodeUrl(String)
     * @since 8304
     */
    public static String decodeUrl(String s) {
        final String enc = StandardCharsets.UTF_8.name();
        try {
            return URLDecoder.decode(s, enc);
        } catch (UnsupportedEncodingException e) {
            Main.error(e);
            return null;
        }
    }

    /**
     * Determines if the given URL denotes a file on a local filesystem.
     * @param url The URL to test
     * @return {@code true} if the url points to a local file
     * @since 7356
     */
    public static boolean isLocalUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("resource://"))
            return false;
        return true;
    }

    /**
     * Creates a new {@link ThreadFactory} which creates threads with names according to {@code nameFormat}.
     * @param nameFormat a {@link String#format(String, Object...)} compatible name format; its first argument is a unique thread index
     * @param threadPriority the priority of the created threads, see {@link Thread#setPriority(int)}
     * @return a new {@link ThreadFactory}
     */
    public static ThreadFactory newThreadFactory(final String nameFormat, final int threadPriority) {
        final String ignore = String.format(Locale.ENGLISH, nameFormat, 0);// fail fast
        return new ThreadFactory() {
            final AtomicLong count = new AtomicLong(0);
            @Override
            public Thread newThread(final Runnable runnable) {
                final Thread thread = new Thread(runnable, String.format(Locale.ENGLISH, nameFormat, count.getAndIncrement()));
                thread.setPriority(threadPriority);
                return thread;
            }
        };
    }

    /**
     * Returns a pair containing the number of threads (n), and a thread pool (if n > 1) to perform
     * multi-thread computation in the context of the given preference key.
     * @param pref The preference key
     * @param nameFormat see {@link #newThreadFactory(String, int)}
     * @param threadPriority see {@link #newThreadFactory(String, int)}
     * @return a pair containing the number of threads (n), and a thread pool (if n > 1, null otherwise)
     * @since 7423
     */
    public static Pair<Integer, ExecutorService> newThreadPool(String pref, String nameFormat, int threadPriority) {
        int noThreads = Main.pref.getInteger(pref, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = noThreads <= 1 ? null : Executors.newFixedThreadPool(noThreads, newThreadFactory(nameFormat, threadPriority));
        return new Pair<>(noThreads, pool);
    }

    /**
     * Updates a given system property.
     * @param key The property key
     * @param value The property value
     * @return the previous value of the system property, or {@code null} if it did not have one.
     * @since 7894
     */
    public static String updateSystemProperty(String key, String value) {
        if (value != null) {
            String old = System.setProperty(key, value);
            if (!key.toLowerCase(Locale.ENGLISH).contains("password")) {
                Main.debug("System property '" + key + "' set to '" + value + "'. Old value was '" + old + "'");
            } else {
                Main.debug("System property '" + key + "' changed.");
            }
            return old;
        }
        return null;
    }

    /**
     * Returns a new secure SAX parser, supporting XML namespaces.
     * @return a new secure SAX parser, supporting XML namespaces
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws SAXException for SAX errors.
     * @since 8287
     */
    public static SAXParser newSafeSAXParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        parserFactory.setNamespaceAware(true);
        return parserFactory.newSAXParser();
    }

    /**
     * Parse the content given {@link org.xml.sax.InputSource} as XML using the specified {@link org.xml.sax.helpers.DefaultHandler}.
     * This method uses a secure SAX parser, supporting XML namespaces.
     *
     * @param is The InputSource containing the content to be parsed.
     * @param dh The SAX DefaultHandler to use.
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws SAXException for SAX errors.
     * @throws IOException if any IO errors occur.
     * @since 8347
     */
    public static void parseSafeSAX(InputSource is, DefaultHandler dh) throws ParserConfigurationException, SAXException, IOException {
        long start = System.currentTimeMillis();
        if (Main.isDebugEnabled()) {
            Main.debug("Starting SAX parsing of " + is + " using " + dh);
        }
        newSafeSAXParser().parse(is, dh);
        if (Main.isDebugEnabled()) {
            Main.debug("SAX parsing done in " + getDurationString(System.currentTimeMillis() - start));
        }
    }

    /**
     * Determines if the filename has one of the given extensions, in a robust manner.
     * The comparison is case and locale insensitive.
     * @param filename The file name
     * @param extensions The list of extensions to look for (without dot)
     * @return {@code true} if the filename has one of the given extensions
     * @since 8404
     */
    public static boolean hasExtension(String filename, String... extensions) {
        String name = filename.toLowerCase(Locale.ENGLISH);
        for (String ext : extensions) {
            if (name.endsWith("." + ext.toLowerCase(Locale.ENGLISH)))
                return true;
        }
        return false;
    }

    /**
     * Determines if the file's name has one of the given extensions, in a robust manner.
     * The comparison is case and locale insensitive.
     * @param file The file
     * @param extensions The list of extensions to look for (without dot)
     * @return {@code true} if the file's name has one of the given extensions
     * @since 8404
     */
    public static boolean hasExtension(File file, String... extensions) {
        return hasExtension(file.getName(), extensions);
    }

    /**
     * Reads the input stream and closes the stream at the end of processing (regardless if an exception was thrown)
     *
     * @param stream input stream
     * @return byte array of data in input stream
     * @throws IOException if any I/O error occurs
     */
    public static byte[] readBytesFromStream(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(stream.available());
            byte[] buffer = new byte[2048];
            boolean finished = false;
            do {
                int read = stream.read(buffer);
                if (read >= 0) {
                    bout.write(buffer, 0, read);
                } else {
                    finished = true;
                }
            } while (!finished);
            if (bout.size() == 0)
                return null;
            return bout.toByteArray();
        } finally {
            stream.close();
        }
    }

    /**
     * Returns the initial capacity to pass to the HashMap / HashSet constructor
     * when it is initialized with a known number of entries.
     *
     * When a HashMap is filled with entries, the underlying array is copied over
     * to a larger one multiple times. To avoid this process when the number of
     * entries is known in advance, the initial capacity of the array can be
     * given to the HashMap constructor. This method returns a suitable value
     * that avoids rehashing but doesn't waste memory.
     * @param nEntries the number of entries expected
     * @param loadFactor the load factor
     * @return the initial capacity for the HashMap constructor
     */
    public static int hashMapInitialCapacity(int nEntries, float loadFactor) {
        return (int) Math.ceil(nEntries / loadFactor);
    }

    /**
     * Returns the initial capacity to pass to the HashMap / HashSet constructor
     * when it is initialized with a known number of entries.
     *
     * When a HashMap is filled with entries, the underlying array is copied over
     * to a larger one multiple times. To avoid this process when the number of
     * entries is known in advance, the initial capacity of the array can be
     * given to the HashMap constructor. This method returns a suitable value
     * that avoids rehashing but doesn't waste memory.
     *
     * Assumes default load factor (0.75).
     * @param nEntries the number of entries expected
     * @return the initial capacity for the HashMap constructor
     */
    public static int hashMapInitialCapacity(int nEntries) {
        return hashMapInitialCapacity(nEntries, 0.75f);
    }
}
