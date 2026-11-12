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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;
import org.openstreetmap.josm.tools.Logging;

/**
 * Integration tests for the DomainValidator.
 *
 * @version $Revision: 1723861 $
 */
@HTTPS
@IntegrationTest
class DomainValidatorTestIT {
    /**
     * Download and process local copy of http://data.iana.org/TLD/tlds-alpha-by-domain.txt
     * Check if the internal TLD table is up to date
     * Check if the internal TLD tables have any spurious entries
     * @throws Exception if an error occurs
     */
    @Test
    void testIanaTldList() throws Exception {
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
        String tlds = "https://data.iana.org/TLD/tlds-alpha-by-domain.txt";
        try {
            timestamp = download(txtFile, tlds, 0L);
        } catch (ConnectException e) {
            Logging.error(e);
            // Try again one more time in case of random network issue
            timestamp = download(txtFile, tlds, 0L);
        }
        final File jsonFile = new File(System.getProperty("java.io.tmpdir"), "tlds-alpha-by-domain.json");
        // N.B. sometimes the JSON data may be updated a day or so after the txt file
        // if the txt file contains entries not found in the JSON file, try again in a day or two
        download(jsonFile, "https://data.iana.org/TLD/v1/tlds.json", timestamp);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(txtFile.toPath()), StandardCharsets.UTF_8))) {
            String line;
            final String header;
            line = br.readLine(); // header
            if (line != null && line.startsWith("# Version ")) {
                header = line.substring(2);
            } else {
                throw new IOException("File does not have expected Version header");
            }
            final boolean generateUnicodeTlds = false; // Change this to generate Unicode TLDs as well

            // Parse JOSN to get entries
            Map<String, String[]> jsonInfo = getJSONInfo(jsonFile);
            Map<String, String> missingTLD = new TreeMap<>(); // stores entry and comments as String[]
            Map<String, String> missingCC = new TreeMap<>();
            Map<String, String> allTLD = new TreeMap<>(); // stores entry and comments as String[]
            Map<String, String> allCC = new TreeMap<>(); // stores entry and comments as String[]
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    final String unicodeTld; // only different from asciiTld if that was punycode
                    final String asciiTld = line.toLowerCase(Locale.ENGLISH);
                    if (line.startsWith("XN--")) {
                        unicodeTld = IDN.toUnicode(line);
                    } else {
                        unicodeTld = asciiTld;
                    }
                    String[] info = jsonInfo.get(asciiTld);
                    if (info != null) {
                        String type = info[0];
                        String comment = info[1];
                        if ("country-code".equals(type)) { // Which list to use?
                            if (!dv.isValidInfrastructureTld(asciiTld)) {
                                allCC.put(asciiTld, unicodeTld + " " + comment);
                                if (generateUnicodeTlds) {
                                    allCC.put(unicodeTld, asciiTld + " " + comment);
                                }
                            }
                        } else {
                            allTLD.put(asciiTld, unicodeTld + " " + comment);
                            if (generateUnicodeTlds) {
                                allTLD.put(unicodeTld, asciiTld + " " + comment);
                            }
                        }
                        if (!dv.isValidTld(asciiTld)) {
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
                        }
                    } else {
                        Logging.error("Expected to find JSON information for "+ asciiTld);
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
            // List JSON entries not in TLD text list
            for (String key : (new TreeMap<>(jsonInfo)).keySet()) {
                if (!ianaTlds.contains(key)) {
                    Logging.warn("Expected to find text entry for JSON: "+key);
                }
            }
            allTLD.remove("arpa");
            String s = allTLD.get("melbourne");
            if (s != null) /* text too long, shorten a bit */
                allTLD.replace("melbourne", s.replace("represented by its ", ""));
            printMap(header, allTLD, "allTLD", "in");
            printMap(header, allCC, "allCC", "in");
            if (!missingTLD.isEmpty()) {
                printMap(header, missingTLD, "TLD", "missing from");
                fail("missing TLD");
            }
            if (!missingCC.isEmpty()) {
                printMap(header, missingCC, "CC", "missing from");
                fail("missing CC");
            }
        }
        // Check if internal tables contain any additional entries
        assertTrue(isInIanaList("INFRASTRUCTURE_TLDS", ianaTlds), String.join(System.lineSeparator(), Logging.getLastErrorAndWarnings()));
        assertTrue(isInIanaList("COUNTRY_CODE_TLDS", ianaTlds), String.join(System.lineSeparator(), Logging.getLastErrorAndWarnings()));
        assertTrue(isInIanaList("GENERIC_TLDS", ianaTlds), String.join(System.lineSeparator(), Logging.getLastErrorAndWarnings()));
        // Don't check local TLDS assertTrue(isInIanaList("LOCAL_TLDS", ianaTlds));
    }

    private static void printMap(final String header, Map<String, String> map, String string, String mode) {
        Logging.warn("Entries " + mode + " "+ string +" List\n");
        if (header != null) {
            Logging.warn("        // Taken from " + header);
        }
        for (Map.Entry<String, String> me : map.entrySet()) {
            Logging.warn("        \"" + me.getKey() + "\", // " + me.getValue());
        }
        Logging.warn(System.lineSeparator() + "Done");
    }

    private static Map<String, String[]> getJSONInfo(final File f) throws IOException {
        final Map<String, String[]> info = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8));
            JsonReader reader = Json.createReader(br)) {
            
            JsonObject root = reader.readObject();
            JsonArray domains = root.getJsonArray("domains");

            if (domains != null) {
                for (JsonValue val : domains) {
                    JsonObject domainNode = val.asJsonObject();
                    
                    String dom = domainNode.getString("name", "");
                    String typ = domainNode.getString("type", "??");
                    String status = domainNode.getString("status", "");

                    String com = "??";
                    if (domainNode.containsKey("manager") && !domainNode.isNull("manager")) {
                        JsonObject managerNode = domainNode.getJsonObject("manager");
                        if (managerNode != null && managerNode.containsKey("organization") && !managerNode.isNull("organization")) {
                            com = managerNode.getString("organization", "??");
                        }
                    }

                    boolean isRetired = "retired".equalsIgnoreCase(status);
                    boolean isTest = "test".equalsIgnoreCase(typ);
                    boolean isNotAssigned = com.contains("Not assigned") || com.equals("??");

                    if (!isRetired && !isTest && !isNotAssigned) {
                        info.put(dom.toLowerCase(Locale.ENGLISH), new String[]{typ, com});
                    }
                }
            }
        }
        return info;
    }

    /*
     * Download a file if it is more recent than our cached copy.
     * Unfortunately the server does not seem to honour If-Modified-Since for the
     * page, so we check if it is newer than the txt file and skip download if so
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
        HttpURLConnection hc = (HttpURLConnection) URI.create(tldurl).toURL().openConnection();
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
        for (String element : array) {
            if (!ianaTlds.contains(element)) {
                Logging.error(name + " contains unexpected value: " + element);
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
