// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Bidi;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import com.kitfox.svg.xml.XMLParseUtil;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Basic utils, that can be useful in different parts of the program.
 */
public final class Utils {

    /** Pattern matching white spaces */
    public static final Pattern WHITE_SPACES_PATTERN = Pattern.compile("\\s+");

    private static final long MILLIS_OF_SECOND = TimeUnit.SECONDS.toMillis(1);
    private static final long MILLIS_OF_MINUTE = TimeUnit.MINUTES.toMillis(1);
    private static final long MILLIS_OF_HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long MILLIS_OF_DAY = TimeUnit.DAYS.toMillis(1);

    /**
     * A list of all characters allowed in URLs
     */
    public static final String URL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=%";

    private static final Pattern REMOVE_DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final String DEFAULT_STRIP = "\uFEFF\u200B";

    private static final String[] SIZE_UNITS = {"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    // Constants backported from Java 9, see https://bugs.openjdk.java.net/browse/JDK-4477961
    private static final double TO_DEGREES = 180.0 / Math.PI;
    private static final double TO_RADIANS = Math.PI / 180.0;

    private Utils() {
        // Hide default constructor for utils classes
    }

    /**
     * Checks if an item that is an instance of clazz exists in the collection
     * @param <T> The collection type.
     * @param collection The collection
     * @param clazz The class to search for.
     * @return <code>true</code> if that item exists in the collection.
     * @deprecated use {@link Stream#anyMatch}
     */
    @Deprecated
    public static <T> boolean exists(Iterable<T> collection, Class<? extends T> clazz) {
        CheckParameterUtil.ensureParameterNotNull(clazz, "clazz");
        return StreamUtils.toStream(collection).anyMatch(clazz::isInstance);
    }

    /**
     * Finds the first item in the iterable for which the predicate matches.
     * @param <T> The iterable type.
     * @param collection The iterable to search in.
     * @param predicate The predicate to match
     * @return the item or <code>null</code> if there was not match.
     * @deprecated use {@link Stream#filter} and {@link Stream#findFirst}
     */
    @Deprecated
    public static <T> T find(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        for (T item : collection) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Finds the first item in the iterable which is of the given type.
     * @param <T> The iterable type.
     * @param collection The iterable to search in.
     * @param clazz The class to search for.
     * @return the item or <code>null</code> if there was not match.
     * @deprecated use {@link Stream#filter} and {@link Stream#findFirst}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T find(Iterable<? extends Object> collection, Class<? extends T> clazz) {
        CheckParameterUtil.ensureParameterNotNull(clazz, "clazz");
        return (T) find(collection, clazz::isInstance);
    }

    /**
     * Returns the first element from {@code items} which is non-null, or null if all elements are null.
     * @param <T> type of items
     * @param items the items to look for
     * @return first non-null item if there is one
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... items) {
        return Arrays.stream(items).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    /**
     * Filter a collection by (sub)class.
     * This is an efficient read-only implementation.
     * @param <S> Super type of items
     * @param <T> type of items
     * @param collection the collection
     * @param clazz the (sub)class
     * @return a read-only filtered collection
     */
    public static <S, T extends S> SubclassFilteredCollection<S, T> filteredCollection(Collection<S> collection, final Class<T> clazz) {
        CheckParameterUtil.ensureParameterNotNull(clazz, "clazz");
        return new SubclassFilteredCollection<>(collection, clazz::isInstance);
    }

    /**
     * Find the index of the first item that matches the predicate.
     * @param <T> The iterable type
     * @param collection The iterable to iterate over.
     * @param predicate The predicate to search for.
     * @return The index of the first item or -1 if none was found.
     */
    public static <T> int indexOf(Iterable<? extends T> collection, Predicate<? super T> predicate) {
        int i = 0;
        for (T item : collection) {
            if (predicate.test(item))
                return i;
            i++;
        }
        return -1;
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
     * Return the modulus in the range [0, n)
     * @param a dividend
     * @param n divisor
     * @return modulo (remainder of the Euclidian division of a by n)
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
     * @deprecated use {@link String#join} or {@link Collectors#joining}
     */
    @Deprecated
    public static String join(String sep, Collection<?> values) {
        CheckParameterUtil.ensureParameterNotNull(sep, "sep");
        if (values == null)
            return null;
        return values.stream()
                .map(v -> v != null ? v.toString() : "")
                .collect(Collectors.joining(sep));
    }

    /**
     * Converts the given iterable collection as an unordered HTML list.
     * @param values The iterable collection
     * @return An unordered HTML list
     */
    public static String joinAsHtmlUnorderedList(Iterable<?> values) {
        return StreamUtils.toStream(values).map(Object::toString).collect(StreamUtils.toHtmlList());
    }

    /**
     * Copies the given array. Unlike {@link Arrays#copyOf}, this method is null-safe.
     * @param <T> type of items
     * @param array The array to copy
     * @return A copy of the original array, or {@code null} if {@code array} is null
     * @since 6221
     */
    public static <T> T[] copyArray(T[] array) {
        if (array != null) {
            return Arrays.copyOf(array, array.length);
        }
        return array;
    }

    /**
     * Copies the given array. Unlike {@link Arrays#copyOf}, this method is null-safe.
     * @param array The array to copy
     * @return A copy of the original array, or {@code null} if {@code array} is null
     * @since 6222
     */
    public static char[] copyArray(char... array) {
        if (array != null) {
            return Arrays.copyOf(array, array.length);
        }
        return array;
    }

    /**
     * Copies the given array. Unlike {@link Arrays#copyOf}, this method is null-safe.
     * @param array The array to copy
     * @return A copy of the original array, or {@code null} if {@code array} is null
     * @since 7436
     */
    public static int[] copyArray(int... array) {
        if (array != null) {
            return Arrays.copyOf(array, array.length);
        }
        return array;
    }

    /**
     * Copies the given array. Unlike {@link Arrays#copyOf}, this method is null-safe.
     * @param array The array to copy
     * @return A copy of the original array, or {@code null} if {@code array} is null
     * @since 11879
     */
    public static byte[] copyArray(byte... array) {
        if (array != null) {
            return Arrays.copyOf(array, array.length);
        }
        return array;
    }

    /**
     * Simple file copy function that will overwrite the target file.
     * @param in The source file
     * @param out The destination file
     * @return the path to the target file
     * @throws IOException if any I/O error occurs
     * @throws IllegalArgumentException if {@code in} or {@code out} is {@code null}
     * @throws InvalidPathException if a Path object cannot be constructed from the abstract path
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
            Logging.warn("Unable to create directory "+out.getPath());
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
                    } else {
                        deleteFile(file);
                    }
                }
            }
        }
        return path.delete();
    }

    /**
     * Deletes a file and log a default warning if the file exists but the deletion fails.
     * @param file file to delete
     * @return {@code true} if and only if the file does not exist or is successfully deleted; {@code false} otherwise
     * @since 10569
     */
    public static boolean deleteFileIfExists(File file) {
        if (file.exists()) {
            return deleteFile(file);
        } else {
            return true;
        }
    }

    /**
     * Deletes a file and log a default warning if the deletion fails.
     * @param file file to delete
     * @return {@code true} if and only if the file is successfully deleted; {@code false} otherwise
     * @since 9296
     */
    public static boolean deleteFile(File file) {
        return deleteFile(file, marktr("Unable to delete file {0}"));
    }

    /**
     * Deletes a file and log a configurable warning if the deletion fails.
     * @param file file to delete
     * @param warnMsg warning message. It will be translated with {@code tr()}
     * and must contain a single parameter <code>{0}</code> for the file path
     * @return {@code true} if and only if the file is successfully deleted; {@code false} otherwise
     * @since 9296
     */
    public static boolean deleteFile(File file, String warnMsg) {
        boolean result = file.delete();
        if (!result) {
            Logging.warn(tr(warnMsg, file.getPath()));
        }
        return result;
    }

    /**
     * Creates a directory and log a default warning if the creation fails.
     * @param dir directory to create
     * @return {@code true} if and only if the directory is successfully created; {@code false} otherwise
     * @since 9645
     */
    public static boolean mkDirs(File dir) {
        return mkDirs(dir, marktr("Unable to create directory {0}"));
    }

    /**
     * Creates a directory and log a configurable warning if the creation fails.
     * @param dir directory to create
     * @param warnMsg warning message. It will be translated with {@code tr()}
     * and must contain a single parameter <code>{0}</code> for the directory path
     * @return {@code true} if and only if the directory is successfully created; {@code false} otherwise
     * @since 9645
     */
    public static boolean mkDirs(File dir, String warnMsg) {
        boolean result = dir.mkdirs();
        if (!result) {
            Logging.warn(tr(warnMsg, dir.getPath()));
        }
        return result;
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
            Logging.warn(e);
        }
    }

    /**
     * <p>Utility method for closing a {@link java.util.zip.ZipFile}.</p>
     *
     * @param zip the zip file. May be null.
     */
    public static void close(ZipFile zip) {
        close((Closeable) zip);
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
                Logging.error("Unable to convert filename " + f.getAbsolutePath() + " to URL");
            }
        }
        return null;
    }

    /**
     * Converts the given URL to its URI.
     * @param url the URL to get URI from
     * @return the URI of given URL
     * @throws URISyntaxException if the URL cannot be converted to an URI
     * @throws MalformedURLException if no protocol is specified, or an unknown protocol is found, or {@code spec} is {@code null}.
     * @since 15543
     */
    public static URI urlToURI(String url) throws URISyntaxException, MalformedURLException {
        return urlToURI(new URL(url));
    }

    /**
     * Converts the given URL to its URI.
     * @param url the URL to get URI from
     * @return the URI of given URL
     * @throws URISyntaxException if the URL cannot be converted to an URI
     * @since 15543
     */
    public static URI urlToURI(URL url) throws URISyntaxException {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            Logging.trace(e);
            return new URI(
                    url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        }
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
     * Calculate MD5 hash of a string and output in hexadecimal format.
     * @param data arbitrary String
     * @return MD5 hash of data, string of length 32 with characters in range [0-9a-f]
     */
    public static String md5Hex(String data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new JosmRuntimeException(e);
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
     * @param <T> type of items
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
            T parentless = deps.keySet().stream()
                    .filter(key -> deps.get(key).isEmpty())
                    .findFirst().orElse(null);
            if (parentless == null) throw new JosmRuntimeException("parentless");
            sorted.add(parentless);
            deps.remove(parentless);
            for (T key : deps.keySet()) {
                deps.remove(key, parentless);
            }
        }
        if (sorted.size() != size) throw new JosmRuntimeException("Wrong size");
        return sorted;
    }

    /**
     * Replaces some HTML reserved characters (&lt;, &gt; and &amp;) by their equivalent entity (&amp;lt;, &amp;gt; and &amp;amp;);
     * @param s The unescaped string
     * @return The escaped string
     */
    public static String escapeReservedCharactersHTML(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Transforms the collection {@code c} into an unmodifiable collection and
     * applies the {@link Function} {@code f} on each element upon access.
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

                    private final Iterator<? extends A> it = c.iterator();

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
     * applies the {@link Function} {@code f} on each element upon access.
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

    /**
     * Returns an unmodifiable list for the given collection.
     * Makes use of {@link Collections#emptySet()} and {@link Collections#singleton} and {@link Arrays#asList} to save memory.
     * @param  collection the collection for which an unmodifiable collection is to be returned
     * @param <T> the class of the objects in the array
     * @return an unmodifiable list
     * @see <a href="https://dzone.com/articles/preventing-your-java-collections-from-wasting-memo">
     *     How to Prevent Your Java Collections From Wasting Memory</a>
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> toUnmodifiableList(Collection<T> collection) {
        // Java 9: use List.of(...)
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyList();
        } else if (collection.size() == 1) {
            return Collections.singletonList(collection.iterator().next());
        } else {
            return (List<T>) Arrays.asList(collection.toArray());
        }
    }

    /**
     * Returns an unmodifiable map for the given map.
     * Makes use of {@link Collections#emptyMap} and {@link Collections#singletonMap} and {@code Map#ofEntries} to save memory.
     *
     * @param map the map for which an unmodifiable map is to be returned
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @return an unmodifiable map
     * @see <a href="https://dzone.com/articles/preventing-your-java-collections-from-wasting-memo">
     *     How to Prevent Your Java Collections From Wasting Memory</a>
     */
    public static <K, V> Map<K, V> toUnmodifiableMap(Map<K, V> map) {
        return XMLParseUtil.toUnmodifiableMap(map);
    }

    /**
     * Returns the first not empty string in the given candidates, otherwise the default string.
     * @param defaultString default string returned if all candidates would be empty if stripped
     * @param candidates string candidates to consider
     * @return the first not empty string in the given candidates, otherwise the default string
     * @since 15646
     */
    public static String firstNotEmptyString(String defaultString, String... candidates) {
        return Arrays.stream(candidates)
                .filter(candidate -> !Utils.isStripEmpty(candidate))
                .findFirst().orElse(defaultString);
    }

    /**
     * Determines if the given String would be empty if stripped.
     * This is an efficient alternative to {@code strip(s).isEmpty()} that avoids to create useless String object.
     * @param str The string to test
     * @return {@code true} if the stripped version of {@code s} would be empty.
     * @since 11435
     */
    public static boolean isStripEmpty(String str) {
        return str == null || IntStream.range(0, str.length()).allMatch(i -> isStrippedChar(str.charAt(i), null));
    }

    /**
     * An alternative to {@link String#trim()} to effectively remove all leading
     * and trailing white characters, including Unicode ones.
     * @param str The string to strip
     * @return <code>str</code>, without leading and trailing characters, according to
     *         {@link Character#isWhitespace(char)} and {@link Character#isSpaceChar(char)}.
     * @see <a href="http://closingbraces.net/2008/11/11/javastringtrim/">Java String.trim has a strange idea of whitespace</a>
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-4080617">JDK bug 4080617</a>
     * @see <a href="https://bugs.openjdk.java.net/browse/JDK-7190385">JDK bug 7190385</a>
     * @since 5772
     */
    public static String strip(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return strip(str, DEFAULT_STRIP);
    }

    /**
     * An alternative to {@link String#trim()} to effectively remove all leading
     * and trailing white characters, including Unicode ones.
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

        int start = 0;
        int end = str.length();
        boolean leadingSkipChar = true;
        while (leadingSkipChar && start < end) {
            leadingSkipChar = isStrippedChar(str.charAt(start), skipChars);
            if (leadingSkipChar) {
                start++;
            }
        }
        boolean trailingSkipChar = true;
        while (trailingSkipChar && end > start) {
            trailingSkipChar = isStrippedChar(str.charAt(end - 1), skipChars);
            if (trailingSkipChar) {
                end--;
            }
        }

        return str.substring(start, end);
    }

    private static boolean isStrippedChar(char c, final String skipChars) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c)
                || DEFAULT_STRIP.indexOf(c) >= 0
                || (skipChars != null && skipChars.indexOf(c) >= 0);
    }

    /**
     * Removes leading, trailing, and multiple inner whitespaces from the given string, to be used as a key or value.
     * @param s The string
     * @return The string without leading, trailing or multiple inner whitespaces
     * @since 13597
     */
    public static String removeWhiteSpaces(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return strip(s).replaceAll("\\s+", " ");
    }

    /**
     * Runs an external command and returns the standard output.
     *
     * The program is expected to execute fast, as this call waits 10 seconds at most.
     *
     * @param command the command with arguments
     * @return the output
     * @throws IOException when there was an error, e.g. command does not exist
     * @throws ExecutionException when the return code is != 0. The output is can be retrieved in the exception message
     * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another thread while waiting
     */
    public static String execOutput(List<String> command) throws IOException, ExecutionException, InterruptedException {
        return execOutput(command, 10, TimeUnit.SECONDS);
    }

    /**
     * Runs an external command and returns the standard output. Waits at most the specified time.
     *
     * @param command the command with arguments
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument. Must not be null
     * @return the output
     * @throws IOException when there was an error, e.g. command does not exist
     * @throws ExecutionException when the return code is != 0. The output is can be retrieved in the exception message
     * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another thread while waiting
     * @since 13467
     */
    public static String execOutput(List<String> command, long timeout, TimeUnit unit)
            throws IOException, ExecutionException, InterruptedException {
        if (Logging.isDebugEnabled()) {
            Logging.debug(String.join(" ", command));
        }
        Path out = Files.createTempFile("josm_exec_" + command.get(0) + "_", ".txt");
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(out.toFile()).start();
            if (!p.waitFor(timeout, unit) || p.exitValue() != 0) {
                throw new ExecutionException(command.toString(), null);
            }
            return String.join("\n", Files.readAllLines(out)).trim();
        } finally {
            try {
                Files.delete(out);
            } catch (IOException e) {
                Logging.warn(e);
            }
        }
    }

    /**
     * Returns the JOSM temp directory.
     * @return The JOSM temp directory ({@code <java.io.tmpdir>/JOSM}), or {@code null} if {@code java.io.tmpdir} is not defined
     * @since 6245
     */
    public static File getJosmTempDir() {
        String tmpDir = getSystemProperty("java.io.tmpdir");
        if (tmpDir == null) {
            return null;
        }
        File josmTmpDir = new File(tmpDir, "JOSM");
        if (!josmTmpDir.exists() && !josmTmpDir.mkdirs()) {
            Logging.warn("Unable to create temp directory " + josmTmpDir);
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
     * Returns a human readable representation (B, kB, MB, ...) for the given number of byes.
     * @param bytes the number of bytes
     * @param locale the locale used for formatting
     * @return a human readable representation
     * @since 9274
     */
    public static String getSizeString(long bytes, Locale locale) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must be >= 0");
        }
        int unitIndex = 0;
        double value = bytes;
        while (value >= 1024 && unitIndex < SIZE_UNITS.length) {
            value /= 1024;
            unitIndex++;
        }
        if (value > 100 || unitIndex == 0) {
            return String.format(locale, "%.0f %s", value, SIZE_UNITS[unitIndex]);
        } else if (value > 10) {
            return String.format(locale, "%.1f %s", value, SIZE_UNITS[unitIndex]);
        } else {
            return String.format(locale, "%.2f %s", value, SIZE_UNITS[unitIndex]);
        }
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
            return IntStream.rangeClosed(0, m.groupCount())
                    .mapToObj(m::group)
                    .collect(Collectors.toList());
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
     * @param <T> type of items
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
     * @throws IllegalArgumentException if maxLength is less than the length of "..."
     */
    public static String shortenString(String s, int maxLength) {
        final String ellipses = "...";
        CheckParameterUtil.ensureThat(maxLength >= ellipses.length(), "maxLength is shorter than " + ellipses.length());
        if (s != null && s.length() > maxLength) {
            return s.substring(0, maxLength - ellipses.length()) + ellipses;
        } else {
            return s;
        }
    }

    /**
     * If the string {@code s} is longer than {@code maxLines} lines, the string is cut and a "..." line is appended.
     * @param s String to shorten
     * @param maxLines maximum number of lines to keep (including including the "..." line)
     * @return the shortened string
     */
    public static String restrictStringLines(String s, int maxLines) {
        if (s == null) {
            return null;
        } else {
            return String.join("\n", limit(Arrays.asList(s.split("\\n")), maxLines, "..."));
        }
    }

    /**
     * If the collection {@code elements} is larger than {@code maxElements} elements,
     * the collection is shortened and the {@code overflowIndicator} is appended.
     * @param <T> type of elements
     * @param elements collection to shorten
     * @param maxElements maximum number of elements to keep (including including the {@code overflowIndicator})
     * @param overflowIndicator the element used to indicate that the collection has been shortened
     * @return the shortened collection
     */
    public static <T> Collection<T> limit(Collection<T> elements, int maxElements, T overflowIndicator) {
        if (elements == null) {
            return null;
        } else {
            if (elements.size() > maxElements) {
                final Collection<T> r = new ArrayList<>(maxElements);
                final Iterator<T> it = elements.iterator();
                while (r.size() < maxElements - 1) {
                    r.add(it.next());
                }
                r.add(overflowIndicator);
                return r;
            } else {
                return elements;
            }
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
        if (url == null || url.indexOf('?') == -1)
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
            throw new IllegalStateException(e);
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
            throw new IllegalStateException(e);
        }
    }

    /**
     * Determines if the given URL denotes a file on a local filesystem.
     * @param url The URL to test
     * @return {@code true} if the url points to a local file
     * @since 7356
     */
    public static boolean isLocalUrl(String url) {
        return url != null && !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("resource://");
    }

    /**
     * Determines if the given URL is valid.
     * @param url The URL to test
     * @return {@code true} if the url is valid
     * @since 10294
     */
    public static boolean isValidUrl(String url) {
        if (url != null) {
            try {
                new URL(url);
                return true;
            } catch (MalformedURLException e) {
                Logging.trace(e);
            }
        }
        return false;
    }

    /**
     * Creates a new {@link ThreadFactory} which creates threads with names according to {@code nameFormat}.
     * @param nameFormat a {@link String#format(String, Object...)} compatible name format; its first argument is a unique thread index
     * @param threadPriority the priority of the created threads, see {@link Thread#setPriority(int)}
     * @return a new {@link ThreadFactory}
     */
    public static ThreadFactory newThreadFactory(final String nameFormat, final int threadPriority) {
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
     * Compute <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein distance</a>
     *
     * @param s First word
     * @param t Second word
     * @return The distance between words
     * @since 14371
     */
    public static int getLevenshteinDistance(String s, String t) {
        int[][] d; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char si; // ith character of s
        char tj; // jth character of t
        int cost; // cost

        // Step 1
        n = s.length();
        m = t.length();
        if (n == 0)
            return m;
        if (m == 0)
            return n;
        d = new int[n+1][m+1];

        // Step 2
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3
        for (i = 1; i <= n; i++) {

            si = s.charAt(i - 1);

            // Step 4
            for (j = 1; j <= m; j++) {

                tj = t.charAt(j - 1);

                // Step 5
                if (si == tj) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                // Step 6
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }

        // Step 7
        return d[n][m];
    }

    /**
     * Check if two strings are similar, but not identical, i.e., have a Levenshtein distance of 1 or 2.
     * @param string1 first string to compare
     * @param string2 second string to compare
     * @return true if the normalized strings are different but only a "little bit"
     * @see #getLevenshteinDistance
     * @since 14371
     */
    public static boolean isSimilar(String string1, String string2) {
        // check plain strings
        int distance = getLevenshteinDistance(string1, string2);

        // check if only the case differs, so we don't consider large distance as different strings
        if (distance > 2 && string1.length() == string2.length()) {
            return deAccent(string1).equalsIgnoreCase(deAccent(string2));
        } else {
            return distance > 0 && distance <= 2;
        }
    }

    /**
     * A ForkJoinWorkerThread that will always inherit caller permissions,
     * unlike JDK's InnocuousForkJoinWorkerThread, used if a security manager exists.
     */
    static final class JosmForkJoinWorkerThread extends ForkJoinWorkerThread {
        JosmForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }
    }

    /**
     * Returns a {@link ForkJoinPool} with the parallelism given by the preference key.
     * @param pref The preference key to determine parallelism
     * @param nameFormat see {@link #newThreadFactory(String, int)}
     * @param threadPriority see {@link #newThreadFactory(String, int)}
     * @return a {@link ForkJoinPool}
     */
    public static ForkJoinPool newForkJoinPool(String pref, final String nameFormat, final int threadPriority) {
        int noThreads = Config.getPref().getInt(pref, Runtime.getRuntime().availableProcessors());
        return new ForkJoinPool(noThreads, new ForkJoinPool.ForkJoinWorkerThreadFactory() {
            final AtomicLong count = new AtomicLong(0);
            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                // Do not use JDK default thread factory !
                // If JOSM is started with Java Web Start, a security manager is installed and the factory
                // creates threads without any permission, forbidding them to load a class instantiating
                // another ForkJoinPool such as MultipolygonBuilder (see bug #15722)
                final ForkJoinWorkerThread thread = new JosmForkJoinWorkerThread(pool);
                thread.setName(String.format(Locale.ENGLISH, nameFormat, count.getAndIncrement()));
                thread.setPriority(threadPriority);
                return thread;
            }
        }, null, true);
    }

    /**
     * Returns an executor which executes commands in the calling thread
     * @return an executor
     */
    public static Executor newDirectExecutor() {
        return Runnable::run;
    }

    /**
     * Gets the value of the specified environment variable.
     * An environment variable is a system-dependent external named value.
     * @param name name the name of the environment variable
     * @return the string value of the variable;
     *         {@code null} if the variable is not defined in the system environment or if a security exception occurs.
     * @see System#getenv(String)
     * @since 13647
     */
    public static String getSystemEnv(String name) {
        try {
            return System.getenv(name);
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get system env", e);
            return null;
        }
    }

    /**
     * Gets the system property indicated by the specified key.
     * @param key the name of the system property.
     * @return the string value of the system property;
     *         {@code null} if there is no property with that key or if a security exception occurs.
     * @see System#getProperty(String)
     * @since 13647
     */
    public static String getSystemProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to get system property", e);
            return null;
        }
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
            try {
                String old = System.setProperty(key, value);
                if (Logging.isDebugEnabled() && !value.equals(old)) {
                    if (!key.toLowerCase(Locale.ENGLISH).contains("password")) {
                        Logging.debug("System property '" + key + "' set to '" + value + "'. Old value was '" + old + '\'');
                    } else {
                        Logging.debug("System property '" + key + "' changed.");
                    }
                }
                return old;
            } catch (SecurityException e) {
                // Don't call Logging class, it may not be fully initialized yet
                System.err.println("Unable to update system property: " + e.getMessage());
            }
        }
        return null;
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
        String name = filename.toLowerCase(Locale.ENGLISH).replace("?format=raw", "");
        return Arrays.stream(extensions)
                .anyMatch(ext -> name.endsWith('.' + ext.toLowerCase(Locale.ENGLISH)));
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
     * @return byte array of data in input stream (empty if stream is null)
     * @throws IOException if any I/O error occurs
     */
    public static byte[] readBytesFromStream(InputStream stream) throws IOException {
        // TODO: remove this method when switching to Java 11 and use InputStream.readAllBytes
        if (stream == null) {
            return new byte[0];
        }
        try { // NOPMD
            ByteArrayOutputStream bout = new ByteArrayOutputStream(stream.available());
            byte[] buffer = new byte[8192];
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
                return new byte[0];
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
    public static int hashMapInitialCapacity(int nEntries, double loadFactor) {
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
        return hashMapInitialCapacity(nEntries, 0.75d);
    }

    /**
     * Utility class to save a string along with its rendering direction
     * (left-to-right or right-to-left).
     */
    private static class DirectionString {
        public final int direction;
        public final String str;

        DirectionString(int direction, String str) {
            this.direction = direction;
            this.str = str;
        }
    }

    /**
     * Convert a string to a list of {@link GlyphVector}s. The string may contain
     * bi-directional text. The result will be in correct visual order.
     * Each element of the resulting list corresponds to one section of the
     * string with consistent writing direction (left-to-right or right-to-left).
     *
     * @param string the string to render
     * @param font the font
     * @param frc a FontRenderContext object
     * @return a list of GlyphVectors
     */
    public static List<GlyphVector> getGlyphVectorsBidi(String string, Font font, FontRenderContext frc) {
        List<GlyphVector> gvs = new ArrayList<>();
        Bidi bidi = new Bidi(string, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        byte[] levels = new byte[bidi.getRunCount()];
        DirectionString[] dirStrings = new DirectionString[levels.length];
        for (int i = 0; i < levels.length; ++i) {
            levels[i] = (byte) bidi.getRunLevel(i);
            String substr = string.substring(bidi.getRunStart(i), bidi.getRunLimit(i));
            int dir = levels[i] % 2 == 0 ? Bidi.DIRECTION_LEFT_TO_RIGHT : Bidi.DIRECTION_RIGHT_TO_LEFT;
            dirStrings[i] = new DirectionString(dir, substr);
        }
        Bidi.reorderVisually(levels, 0, dirStrings, 0, levels.length);
        for (int i = 0; i < dirStrings.length; ++i) {
            char[] chars = dirStrings[i].str.toCharArray();
            gvs.add(font.layoutGlyphVector(frc, chars, 0, chars.length, dirStrings[i].direction));
        }
        return gvs;
    }

    /**
     * Removes diacritics (accents) from string.
     * @param str string
     * @return {@code str} without any diacritic (accent)
     * @since 13836 (moved from SimilarNamedWays)
     */
    public static String deAccent(String str) {
        // https://stackoverflow.com/a/1215117/2257172
        return REMOVE_DIACRITICS.matcher(Normalizer.normalize(str, Normalizer.Form.NFD)).replaceAll("");
    }

    /**
     * Clamp a value to the given range
     * @param val The value
     * @param min minimum value
     * @param max maximum value
     * @return the value
     * @throws IllegalArgumentException if {@code min > max}
     * @since 10805
     */
    public static double clamp(double val, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException(MessageFormat.format("Parameter min ({0}) cannot be greater than max ({1})", min, max));
        } else if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    /**
     * Clamp a integer value to the given range
     * @param val The value
     * @param min minimum value
     * @param max maximum value
     * @return the value
     * @throws IllegalArgumentException if {@code min > max}
     * @since 11055
     */
    public static int clamp(int val, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(MessageFormat.format("Parameter min ({0}) cannot be greater than max ({1})", min, max));
        } else if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    /**
     * Convert angle from radians to degrees.
     *
     * Replacement for {@link Math#toDegrees(double)} to match the Java 9
     * version of that method. (Can be removed when JOSM support for Java 8 ends.)
     * Only relevant in relation to ProjectionRegressionTest.
     * @param angleRad an angle in radians
     * @return the same angle in degrees
     * @see <a href="https://josm.openstreetmap.de/ticket/11889">#11889</a>
     * @since 12013
     */
    public static double toDegrees(double angleRad) {
        return angleRad * TO_DEGREES;
    }

    /**
     * Convert angle from degrees to radians.
     *
     * Replacement for {@link Math#toRadians(double)} to match the Java 9
     * version of that method. (Can be removed when JOSM support for Java 8 ends.)
     * Only relevant in relation to ProjectionRegressionTest.
     * @param angleDeg an angle in degrees
     * @return the same angle in radians
     * @see <a href="https://josm.openstreetmap.de/ticket/11889">#11889</a>
     * @since 12013
     */
    public static double toRadians(double angleDeg) {
        return angleDeg * TO_RADIANS;
    }

    /**
     * Returns the Java version as an int value.
     * @return the Java version as an int value (8, 9, 10, etc.)
     * @since 12130
     */
    public static int getJavaVersion() {
        String version = getSystemProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        // Allow these formats:
        // 1.8.0_72-ea
        // 9-ea
        // 9
        // 9.0.1
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : version.length()));
    }

    /**
     * Returns the Java update as an int value.
     * @return the Java update as an int value (121, 131, etc.)
     * @since 12217
     */
    public static int getJavaUpdate() {
        String version = getSystemProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        // Allow these formats:
        // 1.8.0_72-ea
        // 9-ea
        // 9
        // 9.0.1
        int undePos = version.indexOf('_');
        int dashPos = version.indexOf('-');
        if (undePos > -1) {
            return Integer.parseInt(version.substring(undePos + 1,
                    dashPos > -1 ? dashPos : version.length()));
        }
        int firstDotPos = version.indexOf('.');
        int lastDotPos = version.lastIndexOf('.');
        if (firstDotPos == lastDotPos) {
            return 0;
        }
        return firstDotPos > -1 ? Integer.parseInt(version.substring(firstDotPos + 1,
                lastDotPos > -1 ? lastDotPos : version.length())) : 0;
    }

    /**
     * Returns the Java build number as an int value.
     * @return the Java build number as an int value (0, 1, etc.)
     * @since 12217
     */
    public static int getJavaBuild() {
        String version = getSystemProperty("java.runtime.version");
        int bPos = version.indexOf('b');
        int pPos = version.indexOf('+');
        try {
            return Integer.parseInt(version.substring(bPos > -1 ? bPos + 1 : pPos + 1, version.length()));
        } catch (NumberFormatException e) {
            Logging.trace(e);
            return 0;
        }
    }

    /**
     * Returns the JRE expiration date.
     * @return the JRE expiration date, or null
     * @since 12219
     */
    public static Date getJavaExpirationDate() {
        try {
            Object value = null;
            Class<?> c = Class.forName("com.sun.deploy.config.BuiltInProperties");
            try {
                value = c.getDeclaredField("JRE_EXPIRATION_DATE").get(null);
            } catch (NoSuchFieldException e) {
                // Field is gone with Java 9, there's a method instead
                Logging.trace(e);
                value = c.getDeclaredMethod("getProperty", String.class).invoke(null, "JRE_EXPIRATION_DATE");
            }
            if (value instanceof String) {
                return DateFormat.getDateInstance(3, Locale.US).parse((String) value);
            }
        } catch (IllegalArgumentException | ReflectiveOperationException | SecurityException | ParseException e) {
            Logging.debug(e);
        }
        return null;
    }

    /**
     * Returns the latest version of Java, from Oracle website.
     * @return the latest version of Java, from Oracle website
     * @since 12219
     */
    public static String getJavaLatestVersion() {
        try {
            String[] versions = HttpClient.create(
                    new URL(Config.getPref().get(
                            "java.baseline.version.url",
                            Config.getUrls().getJOSMWebsite() + "/remote/oracle-java-update-baseline.version")))
                    .connect().fetchContent().split("\n");
            if (getJavaVersion() <= 8) {
                for (String version : versions) {
                    if (version.startsWith("1.8")) {
                        return version;
                    }
                }
            }
            return versions[0];
        } catch (IOException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Determines whether JOSM has been started via Java Web Start.
     * @return true if JOSM has been started via Java Web Start
     * @since 15740
     */
    public static boolean isRunningJavaWebStart() {
        try {
            // See http://stackoverflow.com/a/16200769/2257172
            return Class.forName("javax.jnlp.ServiceManager") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Get a function that converts an object to a singleton stream of a certain
     * class (or null if the object cannot be cast to that class).
     *
     * Can be useful in relation with streams, but be aware of the performance
     * implications of creating a stream for each element.
     * @param <T> type of the objects to convert
     * @param <U> type of the elements in the resulting stream
     * @param klass the class U
     * @return function converting an object to a singleton stream or null
     * @since 12594
     */
    public static <T, U> Function<T, Stream<U>> castToStream(Class<U> klass) {
        return x -> klass.isInstance(x) ? Stream.of(klass.cast(x)) : null;
    }

    /**
     * Helper method to replace the "<code>instanceof</code>-check and cast" pattern.
     * Checks if an object is instance of class T and performs an action if that
     * is the case.
     * Syntactic sugar to avoid typing the class name two times, when one time
     * would suffice.
     * @param <T> the type for the instanceof check and cast
     * @param o the object to check and cast
     * @param klass the class T
     * @param consumer action to take when o is and instance of T
     * @since 12604
     */
    @SuppressWarnings("unchecked")
    public static <T> void instanceOfThen(Object o, Class<T> klass, Consumer<? super T> consumer) {
        if (klass.isInstance(o)) {
            consumer.accept((T) o);
        }
    }

    /**
     * Helper method to replace the "<code>instanceof</code>-check and cast" pattern.
     *
     * @param <T> the type for the instanceof check and cast
     * @param o the object to check and cast
     * @param klass the class T
     * @return {@link Optional} containing the result of the cast, if it is possible, an empty
     * Optional otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> instanceOfAndCast(Object o, Class<T> klass) {
        if (klass.isInstance(o))
            return Optional.of((T) o);
        return Optional.empty();
    }

    /**
     * Convenient method to open an URL stream, using JOSM HTTP client if neeeded.
     * @param url URL for reading from
     * @return an input stream for reading from the URL
     * @throws IOException if any I/O error occurs
     * @since 13356
     */
    public static InputStream openStream(URL url) throws IOException {
        switch (url.getProtocol()) {
            case "http":
            case "https":
                return HttpClient.create(url).connect().getContent();
            case "jar":
                try {
                    return url.openStream();
                } catch (FileNotFoundException | InvalidPathException e) {
                    URL betterUrl = betterJarUrl(url);
                    if (betterUrl != null) {
                        try {
                            return betterUrl.openStream();
                        } catch (RuntimeException | IOException ex) {
                            Logging.warn(ex);
                        }
                    }
                    throw e;
                }
            case "file":
            default:
                return url.openStream();
        }
    }

    /**
     * Tries to build a better JAR URL if we find it concerned by a JDK bug.
     * @param jarUrl jar URL to test
     * @return potentially a better URL that won't provoke a JDK bug, or null
     * @throws IOException if an I/O error occurs
     * @since 14404
     */
    public static URL betterJarUrl(URL jarUrl) throws IOException {
        return betterJarUrl(jarUrl, null);
    }

    /**
     * Tries to build a better JAR URL if we find it concerned by a JDK bug.
     * @param jarUrl jar URL to test
     * @param defaultUrl default URL to return
     * @return potentially a better URL that won't provoke a JDK bug, or {@code defaultUrl}
     * @throws IOException if an I/O error occurs
     * @since 14480
     */
    public static URL betterJarUrl(URL jarUrl, URL defaultUrl) throws IOException {
        // Workaround to https://bugs.openjdk.java.net/browse/JDK-4523159
        String urlPath = jarUrl.getPath().replace("%20", " ");
        if (urlPath.startsWith("file:/") && urlPath.split("!").length > 2) {
            // Locate jar file
            int index = urlPath.lastIndexOf("!/");
            Path jarFile = Paths.get(urlPath.substring("file:/".length(), index));
            Path filename = jarFile.getFileName();
            FileTime jarTime = Files.readAttributes(jarFile, BasicFileAttributes.class).lastModifiedTime();
            // Copy it to temp directory (hopefully free of exclamation mark) if needed (missing or older jar)
            Path jarCopy = Paths.get(getSystemProperty("java.io.tmpdir")).resolve(filename);
            if (!jarCopy.toFile().exists() ||
                    Files.readAttributes(jarCopy, BasicFileAttributes.class).lastModifiedTime().compareTo(jarTime) < 0) {
                Files.copy(jarFile, jarCopy, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
            // Return URL using the copy
            return new URL(jarUrl.getProtocol() + ':' + jarCopy.toUri().toURL().toExternalForm() + urlPath.substring(index));
        }
        return defaultUrl;
    }

    /**
     * Finds a resource with a given name, with robustness to known JDK bugs.
     * @param klass class on which {@link ClassLoader#getResourceAsStream} will be called
     * @param path name of the desired resource
     * @return  A {@link java.io.InputStream} object or {@code null} if no resource with this name is found
     * @since 14480
     */
    public static InputStream getResourceAsStream(Class<?> klass, String path) {
        return getResourceAsStream(klass.getClassLoader(), path);
    }

    /**
     * Finds a resource with a given name, with robustness to known JDK bugs.
     * @param cl classloader on which {@link ClassLoader#getResourceAsStream} will be called
     * @param path name of the desired resource
     * @return  A {@link java.io.InputStream} object or {@code null} if no resource with this name is found
     * @since 15416
     */
    public static InputStream getResourceAsStream(ClassLoader cl, String path) {
        try {
            if (path != null && path.startsWith("/")) {
                path = path.substring(1); // See Class#resolveName
            }
            return cl.getResourceAsStream(path);
        } catch (InvalidPathException e) {
            Logging.error("Cannot open {0}: {1}", path, e.getMessage());
            Logging.trace(e);
            try {
                URL betterUrl = betterJarUrl(cl.getResource(path));
                if (betterUrl != null) {
                    return betterUrl.openStream();
                }
            } catch (IOException ex) {
                Logging.error(ex);
            }
            return null;
        }
    }

    /**
     * Strips all HTML characters and return the result.
     *
     * @param rawString The raw HTML string
     * @return the plain text from the HTML string
     * @since 15760
     */
    public static String stripHtml(String rawString) {
        // remove HTML tags
        rawString = rawString.replaceAll("<.*?>", " ");
        // consolidate multiple spaces between a word to a single space
        rawString = rawString.replaceAll("\\b\\s{2,}\\b", " ");
        // remove extra whitespaces
        return rawString.trim();
    }

    /**
     * Intern a string
     * @param string The string to intern
     * @return The interned string
     * @since 16545
     */
    public static String intern(String string) {
        return string == null ? null : string.intern();
    }
}
