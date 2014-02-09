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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.io.FileImporter;

/**
 * Basic utils, that can be useful in different parts of the program.
 */
public final class Utils {

    public static final Pattern WHITE_SPACES_PATTERN = Pattern.compile("\\s+");

    private Utils() {
        // Hide default constructor for utils classes
    }

    /**
     * UTF-8 (UCS Transformation Format—8-bit).
     *
     * <p>Every implementation of the Java platform is required to support UTF-8 (see {@link Charset}).</p>
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final int MILLIS_OF_SECOND = 1000;
    private static final int MILLIS_OF_MINUTE = 60000;
    private static final int MILLIS_OF_HOUR = 3600000;
    private static final int MILLIS_OF_DAY = 86400000;

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
        return new FilteredCollection<T>(collection, predicate);
    }

    /**
     * Returns the first element from {@code items} which is non-null, or null if all elements are null.
     */
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
     * Simple file copy function that will overwrite the target file.<br>
     * Taken from <a href="http://www.rgagnon.com/javadetails/java-0064.html">this article</a> (CC-NC-BY-SA)
     * @param in The source file
     * @param out The destination file
     * @throws java.io.IOException If any I/O error occurs
     */
    public static void copyFile(File in, File out) throws IOException  {
        // TODO: remove this function when we move to Java 7 (use Files.copy instead)
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = new FileInputStream(in);
            outStream = new FileOutputStream(out);
            FileChannel inChannel = inStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outStream.getChannel());
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            close(outStream);
            close(inStream);
        }
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
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return( path.delete() );
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

    private final static double EPSILON = 1e-11;

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
                }
            });
            return true;
        } catch (IllegalStateException ex) {
            Main.error(ex);
            return false;
        }
    }

    /**
     * Extracts clipboard content as string.
     * @return string clipboard contents if available, {@code null} otherwise.
     */
    public static String getClipboardContent() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = null;
        for (int tries = 0; t == null && tries < 10; tries++) {
            try {
                t = clipboard.getContents(null);
            } catch (IllegalStateException e) {
                // Clipboard currently unavailable. On some platforms, the system clipboard is unavailable while it is accessed by another application.
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Main.warn("InterruptedException in "+Utils.class.getSimpleName()+" while getting clipboard content");
                }
            }
        }
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) t.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException ex) {
            Main.error(ex);
            return null;
        } catch (IOException ex) {
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
        byte[] byteData = data.getBytes(UTF_8);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] byteDigest = md.digest(byteData);
        return toHexString(byteDigest);
    }

    private static final char[] HEX_ARRAY = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

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
    public static <T> List<T> topologicalSort(final MultiMap<T,T> dependencies) {
        MultiMap<T,T> deps = new MultiMap<T,T>();
        for (T key : dependencies.keySet()) {
            deps.putVoid(key);
            for (T val : dependencies.get(key)) {
                deps.putVoid(val);
                deps.put(key, val);
            }
        }

        int size = deps.size();
        List<T> sorted = new ArrayList<T>();
        for (int i=0; i<size; ++i) {
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
    public static interface Function<A, B> {

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
     * @param decompress whether to wrap steam in a {@link GZIPInputStream} or {@link CBZip2InputStream}
     *                   if the {@code Content-Type} header is set accordingly.
     * @return An stream for the given URL
     * @throws IOException if an I/O exception occurs.
     * @since 6421
     */
    public static InputStream openURLAndDecompress(final URL url, final boolean decompress) throws IOException {
        final URLConnection connection = setupURLConnection(url.openConnection());
        if (decompress && "application/x-gzip".equals(connection.getHeaderField("Content-Type"))) {
            return new GZIPInputStream(connection.getInputStream());
        } else if (decompress && "application/x-bzip2".equals(connection.getHeaderField("Content-Type"))) {
            return FileImporter.getBZip2InputStream(new BufferedInputStream(connection.getInputStream()));
        } else {
            return connection.getInputStream();
        }
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
            connection.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);
            connection.setReadTimeout(Main.pref.getInteger("socket.timeout.read",30)*1000);
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
     * @param decompress whether to wrap steam in a {@link GZIPInputStream} or {@link CBZip2InputStream}
     *                   if the {@code Content-Type} header is set accordingly.
     * @return An buffered stream reader for the given URL (using UTF-8)
     * @throws IOException if an I/O exception occurs.
     * @since 6421
     */
    public static BufferedReader openURLReaderAndDecompress(final URL url, final boolean decompress) throws IOException {
        return new BufferedReader(new InputStreamReader(openURLAndDecompress(url, decompress), UTF_8));
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
        return connection;
    }

    /**
     * An alternative to {@link String#trim()} to effectively remove all leading and trailing white characters, including Unicode ones.
     * @see <a href="http://closingbraces.net/2008/11/11/javastringtrim/">Java’s String.trim has a strange idea of whitespace</a>
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-4080617">JDK bug 4080617</a>
     * @param str The string to strip
     * @return <code>str</code>, without leading and trailing characters, according to
     *         {@link Character#isWhitespace(char)} and {@link Character#isSpaceChar(char)}.
     * @since 5772
     */
    public static String strip(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        int start = 0, end = str.length();
        boolean leadingWhite = true;
        while (leadingWhite && start < end) {
            char c = str.charAt(start);
            // '\u200B' (ZERO WIDTH SPACE character) needs to be handled manually because of change in Unicode 6.0 (Java 7, see #8918)
            // same for '\uFEFF' (ZERO WIDTH NO-BREAK SPACE)
            leadingWhite = (Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\u200B' || c == '\uFEFF');
            if (leadingWhite) {
                start++;
            }
        }
        boolean trailingWhite = true;
        while (trailingWhite && end > start+1) {
            char c = str.charAt(end-1);
            trailingWhite = (Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\u200B' || c == '\uFEFF');
            if (trailingWhite) {
                end--;
            }
        }
        return str.substring(start, end);
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
        Process p = new ProcessBuilder(command).start();
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder all = null;
        String line;
        while ((line = input.readLine()) != null) {
            if (all == null) {
                all = new StringBuilder(line);
            } else {
                all.append("\n");
                all.append(line);
            }
        }
        Utils.close(input);
        return all.toString();
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
        if (!josmTmpDir.exists()) {
            if (!josmTmpDir.mkdirs()) {
                Main.warn("Unable to create temp directory "+josmTmpDir);
            }
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
    public static String getDurationString(long elapsedTime) throws IllegalArgumentException {
        if (elapsedTime < 0) {
            throw new IllegalArgumentException("elapsedTime must be > 0");
        }
        // Is it less than 1 second ?
        if (elapsedTime < MILLIS_OF_SECOND) {
            return String.format("%d %s", elapsedTime, tr("ms"));
        }
        // Is it less than 1 minute ?
        if (elapsedTime < MILLIS_OF_MINUTE) {
            return String.format("%.1f %s", elapsedTime / (float) MILLIS_OF_SECOND, tr("s"));
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
    public static String getPositionListString(List<Integer> positionList)  {
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
                sb.append(",").append(cur);
            } else {
                sb.append("-").append(last);
                sb.append(",").append(cur);
                cnt = 0;
            }
            last = cur;
        }
        if (cnt >= 1) {
            sb.append("-").append(last);
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
            List<String> result = new ArrayList<String>(m.groupCount() + 1);
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
            while (cause != null && cause != result) {
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
     */
    public static String shortenString(String s, int maxLength) {
        if (s != null && s.length() > maxLength) {
            return s.substring(0, maxLength - 3) + "...";
        } else {
            return s;
        }
    }
}
