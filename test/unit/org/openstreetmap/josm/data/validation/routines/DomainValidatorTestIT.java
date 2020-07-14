/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openstreetmap.josm.data.validation.routines;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests for the DomainValidator.
 *
 * @version $Revision: 1723861 $
 */
public class DomainValidatorTestIT {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https();

    /**
     * Download and process local copy of http://data.iana.org/TLD/tlds-alpha-by-domain.txt
     * Check if the internal TLD table is up to date
     * Check if the internal TLD tables have any spurious entries
     * @throws Exception if an error occurs
     */
    @Test
    public void testIanaTldList() throws Exception {
        // Check the arrays first as this affects later checks
        // Doing this here makes it easier when updating the lists
        boolean OK = true;
        for (String list : new String[]{"INFRASTRUCTURE_TLDS", "COUNTRY_CODE_TLDS", "GENERIC_TLDS", "LOCAL_TLDS"}) {
            OK &= isSortedLowerCase(list);
        }
        if (!OK) {
            System.out.println("Fix arrays before retrying; cannot continue");
            return;
        }
        Set<String> ianaTlds = new HashSet<>(); // keep for comparison with array contents
        DomainValidator dv = DomainValidator.getInstance();
        File txtFile = new File(System.getProperty("java.io.tmpdir"), "tlds-alpha-by-domain.txt");
        long timestamp;
        try {
            timestamp = download(txtFile, "http://data.iana.org/TLD/tlds-alpha-by-domain.txt", 0L);
        } catch (ConnectException e) {
            Logging.error(e);
            // Try again one more time in case of random network issue
            timestamp = download(txtFile, "http://data.iana.org/TLD/tlds-alpha-by-domain.txt", 0L);
        }
        final File htmlFile = new File(System.getProperty("java.io.tmpdir"), "tlds-alpha-by-domain.html");
        // N.B. sometimes the html file may be updated a day or so after the txt file
        // if the txt file contains entries not found in the html file, try again in a day or two
        download(htmlFile, "http://www.iana.org/domains/root/db", timestamp);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtFile), StandardCharsets.UTF_8))) {
            String line;
            final String header;
            line = br.readLine(); // header
            if (line != null && line.startsWith("# Version ")) {
                header = line.substring(2);
            } else {
                throw new IOException("File does not have expected Version header");
            }
            final boolean generateUnicodeTlds = false; // Change this to generate Unicode TLDs as well

            // Parse html page to get entries
            Map<String, String[]> htmlInfo = getHtmlInfo(htmlFile);
            Map<String, String> missingTLD = new TreeMap<>(); // stores entry and comments as String[]
            Map<String, String> missingCC = new TreeMap<>();
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    final String unicodeTld; // only different from asciiTld if that was punycode
                    final String asciiTld = line.toLowerCase(Locale.ENGLISH);
                    if (line.startsWith("XN--")) {
                        unicodeTld = IDN.toUnicode(line);
                    } else {
                        unicodeTld = asciiTld;
                    }
                    if (!dv.isValidTld(asciiTld)) {
                        String[] info = htmlInfo.get(asciiTld);
                        if (info != null) {
                            String type = info[0];
                            String comment = info[1];
                            if ("country-code".equals(type)) { // Which list to use?
                                missingCC.put(asciiTld, unicodeTld + " " + comment);
                                if (generateUnicodeTlds) {
                                    missingCC.put(unicodeTld, asciiTld + " " + comment);
                                }
                            } else {
                                missingTLD.put(asciiTld, unicodeTld + " " + comment);
                                if (generateUnicodeTlds) {
                                    missingTLD.put(unicodeTld, asciiTld + " " + comment);
                                }
                            }
                        } else {
                            Logging.error("Expected to find HTML info for "+ asciiTld);
                        }
                    }
                    ianaTlds.add(asciiTld);
                    // Don't merge these conditions; generateUnicodeTlds is final so needs to be separate to avoid a warning
                    if (generateUnicodeTlds) {
                        if (!unicodeTld.equals(asciiTld)) {
                            ianaTlds.add(unicodeTld);
                        }
                    }
                }
            }
            // List html entries not in TLD text list
            for (String key : (new TreeMap<>(htmlInfo)).keySet()) {
                if (!ianaTlds.contains(key)) {
                    if (isNotInRootZone(key)) {
                        Logging.info("HTML entry not yet in root zone: "+key);
                    } else {
                        Logging.warn("Expected to find text entry for html: "+key);
                    }
                }
            }
            if (!missingTLD.isEmpty()) {
                printMap(header, missingTLD, "TLD");
                fail("missing TLD");
            }
            if (!missingCC.isEmpty()) {
                printMap(header, missingCC, "CC");
                fail("missing CC");
            }
        }
        // Check if internal tables contain any additional entries
        assertTrue(isInIanaList("INFRASTRUCTURE_TLDS", ianaTlds), String.join(System.lineSeparator(), Logging.getLastErrorAndWarnings()));
        assertTrue(isInIanaList("COUNTRY_CODE_TLDS", ianaTlds), String.join(System.lineSeparator(), Logging.getLastErrorAndWarnings()));
        assertTrue(isInIanaList("GENERIC_TLDS", ianaTlds), String.join(System.lineSeparator(), Logging.getLastErrorAndWarnings()));
        // Don't check local TLDS assertTrue(isInIanaList("LOCAL_TLDS", ianaTlds));
    }

    private static void printMap(final String header, Map<String, String> map, String string) {
        Logging.warn("Entries missing from "+ string +" List\n");
        if (header != null) {
            Logging.warn("        // Taken from " + header);
        }
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> me = it.next();
            Logging.warn("        \"" + me.getKey() + "\", // " + me.getValue());
        }
        Logging.warn(System.lineSeparator() + "Done");
    }

    @SuppressFBWarnings(value = "PERFORMANCE")
    private static Map<String, String[]> getHtmlInfo(final File f) throws IOException {
        final Map<String, String[]> info = new HashMap<>();

        final Pattern domain = Pattern.compile(".*<a href=\"/domains/root/db/([^.]+)\\.html");
        final Pattern type = Pattern.compile("\\s+<td>([^<]+)</td>");
        final Pattern comment = Pattern.compile("\\s+<td>([^<]+)</td>");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = domain.matcher(line);
                if (m.lookingAt()) {
                    String dom = m.group(1);
                    String typ = "??";
                    String com = "??";
                    line = br.readLine();
                    while (line != null && line.matches("^\\s*$")) { // extra blank lines introduced
                        line = br.readLine();
                    }
                    Matcher t = type.matcher(line);
                    if (t.lookingAt()) {
                        typ = t.group(1);
                        line = br.readLine();
                        if (line != null && line.matches("\\s+<!--.*")) {
                            while (line != null && !line.matches(".*-->.*")) {
                                line = br.readLine();
                            }
                            line = br.readLine();
                        }
                        // Should have comment; is it wrapped?
                        while (line != null && !line.matches(".*</td>.*")) {
                            line += " " +br.readLine();
                        }
                        Matcher n = comment.matcher(line);
                        if (n.lookingAt()) {
                            com = n.group(1);
                        }
                        // Don't save unused entries
                        if (!com.contains("Not assigned") && !com.contains("Retired") && !typ.equals("test")) {
                            info.put(dom.toLowerCase(Locale.ENGLISH), new String[]{typ, com});
                        }
                    } else {
                        Logging.error("Unexpected type: " + line);
                    }
                }
            }
        }
        return info;
    }

    /*
     * Download a file if it is more recent than our cached copy.
     * Unfortunately the server does not seem to honour If-Modified-Since for the
     * Html page, so we check if it is newer than the txt file and skip download if so
     */
    private static long download(File f, String tldurl, long timestamp) throws IOException {
        final int HOUR = 60*60*1000; // an hour in ms
        final long modTime;
        // For testing purposes, don't download files more than once an hour
        if (f.canRead()) {
            modTime = f.lastModified();
            if (modTime > System.currentTimeMillis()-HOUR) {
                Logging.debug("Skipping download - found recent " + f);
                return modTime;
            }
        } else {
            modTime = 0;
        }
        HttpURLConnection hc = (HttpURLConnection) new URL(tldurl).openConnection();
        if (modTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"); //Sun, 06 Nov 1994 08:49:37 GMT
            String since = sdf.format(new Date(modTime));
            hc.addRequestProperty("If-Modified-Since", since);
            Logging.debug("Found " + f + " with date " + since);
        }
        if (hc.getResponseCode() == 304) {
            Logging.debug("Already have most recent " + tldurl);
        } else {
            Logging.debug("Downloading " + tldurl);
            byte[] buff = new byte[1024];
            try (InputStream is = hc.getInputStream();
                 FileOutputStream fos = new FileOutputStream(f)) {
                int len;
                while ((len = is.read(buff)) != -1) {
                    fos.write(buff, 0, len);
                }
            }
            Logging.debug("Done");
        }
        return f.lastModified();
    }

    /**
     * Check whether the domain is in the root zone currently.
     * Reads the URL http://www.iana.org/domains/root/db/*domain*.html
     * (using a local disk cache)
     * and checks for the string "This domain is not present in the root zone at this time."
     * @param domain the domain to check
     * @return true if the string is found
     */
    private static boolean isNotInRootZone(String domain) {
        String tldurl = "http://www.iana.org/domains/root/db/" + domain + ".html";
        BufferedReader in = null;
        try {
            File rootCheck = new File(System.getProperty("java.io.tmpdir"), "tld_" + domain + ".html");
            download(rootCheck, tldurl, 0L);
            in = new BufferedReader(new InputStreamReader(new FileInputStream(rootCheck), StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("This domain is not present in the root zone at this time.")) {
                    return true;
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(in);
        }
        return false;
    }

    private static void closeQuietly(Closeable in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // isInIanaList and isSorted are split into two methods.
    // If/when access to the arrays is possible without reflection, the intermediate
    // methods can be dropped
    private static boolean isInIanaList(String arrayName, Set<String> ianaTlds) throws Exception {
        Field f = DomainValidator.class.getDeclaredField(arrayName);
        final boolean isPrivate = Modifier.isPrivate(f.getModifiers());
        if (isPrivate) {
            f.setAccessible(true);
        }
        String[] array = (String[]) f.get(null);
        try {
            return isInIanaList(arrayName, array, ianaTlds);
        } finally {
            if (isPrivate) {
                f.setAccessible(false);
            }
        }
    }

    private static boolean isInIanaList(String name, String[] array, Set<String> ianaTlds) {
        boolean ok = true;
        for (int i = 0; i < array.length; i++) {
            if (!ianaTlds.contains(array[i])) {
                Logging.error(name + " contains unexpected value: " + array[i]);
                ok = false;
            }
        }
        return ok;
    }

    private static boolean isSortedLowerCase(String arrayName) throws Exception {
        Field f = DomainValidator.class.getDeclaredField(arrayName);
        final boolean isPrivate = Modifier.isPrivate(f.getModifiers());
        if (isPrivate) {
            f.setAccessible(true);
        }
        String[] array = (String[]) f.get(null);
        try {
            return isSortedLowerCase(arrayName, array);
        } finally {
            if (isPrivate) {
                f.setAccessible(false);
            }
        }
    }

    private static boolean isLowerCase(String string) {
        return string.equals(string.toLowerCase(Locale.ENGLISH));
    }

    // Check if an array is strictly sorted - and lowerCase
    private static boolean isSortedLowerCase(String name, String[] array) {
        boolean sorted = true;
        boolean strictlySorted = true;
        final int length = array.length;
        boolean lowerCase = isLowerCase(array[length-1]); // Check the last entry
        for (int i = 0; i < length-1; i++) { // compare all but last entry with next
            final String entry = array[i];
            final String nextEntry = array[i+1];
            final int cmp = entry.compareTo(nextEntry);
            if (cmp > 0) { // out of order
                Logging.error("Out of order entry: " + entry + " < " + nextEntry + " in " + name);
                sorted = false;
            } else if (cmp == 0) {
                strictlySorted = false;
                Logging.error("Duplicated entry: " + entry + " in " + name);
            }
            if (!isLowerCase(entry)) {
                Logging.error("Non lowerCase entry: " + entry + " in " + name);
                lowerCase = false;
            }
        }
        return sorted && strictlySorted && lowerCase;
    }
}
