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

import java.util.Arrays;
import java.util.Locale;

/**
 * <p><b>Domain name</b> validation routines.</p>
 *
 * <p>
 * This validator provides methods for validating Internet domain names
 * and top-level domains.
 * </p>
 *
 * <p>Domain names are evaluated according
 * to the standards <a href="http://www.ietf.org/rfc/rfc1034.txt">RFC1034</a>,
 * section 3, and <a href="http://www.ietf.org/rfc/rfc1123.txt">RFC1123</a>,
 * section 2.1. No accomodation is provided for the specialized needs of
 * other applications; if the domain name has been URL-encoded, for example,
 * validation will fail even though the equivalent plaintext version of the
 * same name would have passed.
 * </p>
 *
 * <p>
 * Validation is also provided for top-level domains (TLDs) as defined and
 * maintained by the Internet Assigned Numbers Authority (IANA):
 * </p>
 *
 *   <ul>
 *     <li>{@link #isValidInfrastructureTld} - validates infrastructure TLDs
 *         (<code>.arpa</code>, etc.)</li>
 *     <li>{@link #isValidGenericTld} - validates generic TLDs
 *         (<code>.com, .org</code>, etc.)</li>
 *     <li>{@link #isValidIdnTld} - validates IDN TLDs
 *         (<code>.xn--*</code>, etc.)</li>
 *     <li>{@link #isValidCountryCodeTld} - validates country code TLDs
 *         (<code>.us, .uk, .cn</code>, etc.)</li>
 *   </ul>
 *
 * <p>
 * (<b>NOTE</b>: This class does not provide IP address lookup for domain names or
 * methods to ensure that a given domain name matches a specific IP; see
 * {@link java.net.InetAddress} for that functionality.)
 * </p>
 *
 * @version $Revision: 1640271 $ $Date: 2014-11-18 02:32:15 2014 UTC (Tue, 18 Nov 2014) $
 * @since Validator 1.4
 */
public final class DomainValidator extends AbstractValidator {

    // Regular expression strings for hostnames (derived from RFC2396 and RFC 1123)
    private static final String DOMAIN_LABEL_REGEX = "\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*";
    private static final String TOP_LABEL_REGEX = "\\p{Alpha}{2,}";
    // JOSM PATCH BEGIN
    // See #10862 - IDN TLDs in ASCII form
    private static final String TOP_LABEL_IDN_REGEX = "(?:xn|XN)--\\p{Alnum}{2,}(?:-\\p{Alpha}{2,})?";
    private static final String DOMAIN_NAME_REGEX =
            "^(?:" + DOMAIN_LABEL_REGEX + "\\.)+" + "(" + TOP_LABEL_REGEX + "|" + TOP_LABEL_IDN_REGEX + ")$";
    // JOSM PATCH END

    private final boolean allowLocal;

    /**
     * Singleton instance of this validator, which
     *  doesn't consider local addresses as valid.
     */
    private static final DomainValidator DOMAIN_VALIDATOR = new DomainValidator(false);

    /**
     * Singleton instance of this validator, which does
     *  consider local addresses valid.
     */
    private static final DomainValidator DOMAIN_VALIDATOR_WITH_LOCAL = new DomainValidator(true);

    /**
     * RegexValidator for matching domains.
     */
    private final RegexValidator domainRegex =
            new RegexValidator(DOMAIN_NAME_REGEX);
    /**
     * RegexValidator for matching the a local hostname
     */
    private final RegexValidator hostnameRegex =
            new RegexValidator(DOMAIN_LABEL_REGEX);

    /**
     * Returns the singleton instance of this validator. It
     *  will not consider local addresses as valid.
     * @return the singleton instance of this validator
     */
    public static DomainValidator getInstance() {
        return DOMAIN_VALIDATOR;
    }

    /**
     * Returns the singleton instance of this validator,
     *  with local validation as required.
     * @param allowLocal Should local addresses be considered valid?
     * @return the singleton instance of this validator
     */
    public static DomainValidator getInstance(boolean allowLocal) {
       if (allowLocal) {
          return DOMAIN_VALIDATOR_WITH_LOCAL;
       }
       return DOMAIN_VALIDATOR;
    }

    /** Private constructor. */
    private DomainValidator(boolean allowLocal) {
       this.allowLocal = allowLocal;
    }

    /**
     * Returns true if the specified <code>String</code> parses
     * as a valid domain name with a recognized top-level domain.
     * The parsing is case-sensitive.
     * @param domain the parameter to check for domain name syntax
     * @return true if the parameter is a valid domain name
     */
    @Override
    public boolean isValid(String domain) {
        String[] groups = domainRegex.match(domain);
        if (groups != null && groups.length > 0) {
            return isValidTld(groups[0]);
        } else if (allowLocal) {
            if (hostnameRegex.isValid(domain)) {
               return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined top-level domain. Leading dots are ignored if present.
     * The search is case-sensitive.
     * @param tld the parameter to check for TLD status
     * @return true if the parameter is a TLD
     */
    public boolean isValidTld(String tld) {
        if (allowLocal && isValidLocalTld(tld)) {
           return true;
        }
        return isValidInfrastructureTld(tld)
                || isValidGenericTld(tld)
                || isValidIdnTld(tld)
                || isValidCountryCodeTld(tld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined infrastructure top-level domain. Leading dots are
     * ignored if present. The search is case-sensitive.
     * @param iTld the parameter to check for infrastructure TLD status
     * @return true if the parameter is an infrastructure TLD
     */
    public boolean isValidInfrastructureTld(String iTld) {
        return Arrays.binarySearch(INFRASTRUCTURE_TLDS, chompLeadingDot(iTld.toLowerCase(Locale.ENGLISH))) >= 0;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined generic top-level domain. Leading dots are ignored
     * if present. The search is case-sensitive.
     * @param gTld the parameter to check for generic TLD status
     * @return true if the parameter is a generic TLD
     */
    public boolean isValidGenericTld(String gTld) {
        return Arrays.binarySearch(GENERIC_TLDS, chompLeadingDot(gTld.toLowerCase(Locale.ENGLISH))) >= 0;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined IDN top-level domain. Leading dots are ignored
     * if present. The search is case-sensitive.
     * @param iTld the parameter to check for IDN TLD status
     * @return true if the parameter is an IDN TLD
     */
    public boolean isValidIdnTld(String iTld) {
        return Arrays.binarySearch(IDN_TLDS, chompLeadingDot(iTld.toUpperCase(Locale.ENGLISH))) >= 0;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined country code top-level domain. Leading dots are
     * ignored if present. The search is case-sensitive.
     * @param ccTld the parameter to check for country code TLD status
     * @return true if the parameter is a country code TLD
     */
    public boolean isValidCountryCodeTld(String ccTld) {
        return Arrays.binarySearch(COUNTRY_CODE_TLDS, chompLeadingDot(ccTld.toLowerCase(Locale.ENGLISH))) >= 0;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * widely used "local" domains (localhost or localdomain). Leading dots are
     *  ignored if present. The search is case-sensitive.
     * @param iTld the parameter to check for local TLD status
     * @return true if the parameter is an local TLD
     */
    public boolean isValidLocalTld(String iTld) {
        return Arrays.binarySearch(LOCAL_TLDS, chompLeadingDot(iTld.toLowerCase(Locale.ENGLISH))) >= 0;
    }

    private static String chompLeadingDot(String str) {
        if (str.startsWith(".")) {
            return str.substring(1);
        } else {
            return str;
        }
    }

    // ---------------------------------------------
    // ----- TLDs defined by IANA
    // ----- Authoritative and comprehensive list at:
    // ----- http://data.iana.org/TLD/tlds-alpha-by-domain.txt

    private static final String[] INFRASTRUCTURE_TLDS = new String[] {
        "arpa",               // internet infrastructure
        "root"                // diagnostic marker for non-truncated root zone
    };

    private static final String[] GENERIC_TLDS = new String[] {
        "abogado",
        "academy",
        "accountants",
        "active",
        "actor",
        "aero",
        "agency",
        "airforce",
        "allfinanz",
        "alsace",
        "archi",
        "army",
        "arpa",
        "asia",
        "associates",
        "attorney",
        "auction",
        "audio",
        "autos",
        "axa",
        "band",
        "bar",
        "bargains",
        "bayern",
        "beer",
        "berlin",
        "best",
        "bid",
        "bike",
        "bio",
        "biz",
        "black",
        "blackfriday",
        "blue",
        "bmw",
        "bnpparibas",
        "boo",
        "boutique",
        "brussels",
        "budapest",
        "build",
        "builders",
        "business",
        "buzz",
        "bzh",
        "cab",
        "cal",
        "camera",
        "camp",
        "cancerresearch",
        "capetown",
        "capital",
        "caravan",
        "cards",
        "care",
        "career",
        "careers",
        "casa",
        "cash",
        "cat",
        "catering",
        "center",
        "ceo",
        "cern",
        "channel",
        "cheap",
        "christmas",
        "chrome",
        "church",
        "citic",
        "city",
        "claims",
        "cleaning",
        "click",
        "clinic",
        "clothing",
        "club",
        "codes",
        "coffee",
        "college",
        "cologne",
        "com",
        "community",
        "company",
        "computer",
        "condos",
        "construction",
        "consulting",
        "contractors",
        "cooking",
        "cool",
        "coop",
        "country",
        "credit",
        "creditcard",
        "crs",
        "cruises",
        "cuisinella",
        "cymru",
        "dad",
        "dance",
        "dating",
        "day",
        "deals",
        "degree",
        "democrat",
        "dental",
        "dentist",
        "desi",
        "diamonds",
        "diet",
        "digital",
        "direct",
        "directory",
        "discount",
        "dnp",
        "domains",
        "durban",
        "dvag",
        "eat",
        "edu",
        "education",
        "email",
        "engineer",
        "engineering",
        "enterprises",
        "equipment",
        "esq",
        "estate",
        "eus",
        "events",
        "exchange",
        "expert",
        "exposed",
        "fail",
        "farm",
        "feedback",
        "finance",
        "financial",
        "fish",
        "fishing",
        "fitness",
        "flights",
        "florist",
        "flsmidth",
        "fly",
        "foo",
        "forsale",
        "foundation",
        "frl",
        "frogans",
        "fund",
        "furniture",
        "futbol",
        "gal",
        "gallery",
        "gbiz",
        "gent",
        "gift",
        "gifts",
        "gives",
        "glass",
        "gle",
        "global",
        "globo",
        "gmail",
        "gmo",
        "gmx",
        "google",
        "gop",
        "gov",
        "graphics",
        "gratis",
        "green",
        "gripe",
        "guide",
        "guitars",
        "guru",
        "hamburg",
        "haus",
        "healthcare",
        "help",
        "here",
        "hiphop",
        "hiv",
        "holdings",
        "holiday",
        "homes",
        "horse",
        "host",
        "hosting",
        "house",
        "how",
        "ibm",
        "immo",
        "immobilien",
        "industries",
        "info",
        "ing",
        "ink",
        "institute",
        "insure",
        "int",
        "international",
        "investments",
        "jetzt",
        "jobs",
        "joburg",
        "juegos",
        "kaufen",
        "kim",
        "kitchen",
        "kiwi",
        "koeln",
        "krd",
        "kred",
        "lacaixa",
        "land",
        "lawyer",
        "lease",
        "lgbt",
        "life",
        "lighting",
        "limited",
        "limo",
        "link",
        "loans",
        "london",
        "lotto",
        "ltda",
        "luxe",
        "luxury",
        "maison",
        "management",
        "mango",
        "market",
        "marketing",
        "media",
        "meet",
        "melbourne",
        "meme",
        "menu",
        "miami",
        "mil",
        "mini",
        "mobi",
        "moda",
        "moe",
        "monash",
        "mortgage",
        "moscow",
        "motorcycles",
        "mov",
        "museum",
        "nagoya",
        "name",
        "navy",
        "net",
        "network",
        "neustar",
        "new",
        "nexus",
        "ngo",
        "nhk",
        "ninja",
        "nra",
        "nrw",
        "nyc",
        "okinawa",
        "ong",
        "onl",
        "ooo",
        "org",
        "organic",
        "otsuka",
        "ovh",
        "paris",
        "partners",
        "parts",
        "pharmacy",
        "photo",
        "photography",
        "photos",
        "physio",
        "pics",
        "pictures",
        "pink",
        "pizza",
        "place",
        "plumbing",
        "pohl",
        "poker",
        "post",
        "praxi",
        "press",
        "pro",
        "prod",
        "productions",
        "prof",
        "properties",
        "property",
        "pub",
        "qpon",
        "quebec",
        "realtor",
        "recipes",
        "red",
        "rehab",
        "reise",
        "reisen",
        "ren",
        "rentals",
        "repair",
        "report",
        "republican",
        "rest",
        "restaurant",
        "reviews",
        "rich",
        "rio",
        "rip",
        "rocks",
        "rodeo",
        "rsvp",
        "ruhr",
        "ryukyu",
        "saarland",
        "sarl",
        "sca",
        "scb",
        "schmidt",
        "schule",
        "scot",
        "services",
        "sexy",
        "shiksha",
        "shoes",
        "singles",
        "social",
        "software",
        "sohu",
        "solar",
        "solutions",
        "soy",
        "space",
        "spiegel",
        "supplies",
        "supply",
        "support",
        "surf",
        "surgery",
        "suzuki",
        "systems",
        "tatar",
        "tattoo",
        "tax",
        "technology",
        "tel",
        "tienda",
        "tips",
        "tirol",
        "today",
        "tokyo",
        "tools",
        "top",
        "town",
        "toys",
        "trade",
        "training",
        "travel",
        "tui",
        "university",
        "uno",
        "uol",
        "vacations",
        "vegas",
        "ventures",
        "versicherung",
        "vet",
        "viajes",
        "villas",
        "vision",
        "vlaanderen",
        "vodka",
        "vote",
        "voting",
        "voto",
        "voyage",
        "wales",
        "wang",
        "watch",
        "webcam",
        "website",
        "wed",
        "wedding",
        "whoswho",
        "wien",
        "wiki",
        "williamhill",
        "wme",
        "work",
        "works",
        "world",
        "wtc",
        "wtf",
        "xxx",
        "xyz",
        "yachts",
        "yandex",
        "yoga",
        "yokohama",
        "youtube",
        "zip",
        "zone",
    };

    // JOSM PATCH BEGIN
    // see #10862 - list of IDN TLDs taken from IANA on 2014-12-18
    private static final String[] IDN_TLDS = new String[] {
        "XN--1QQW23A",
        "XN--3BST00M",
        "XN--3DS443G",
        "XN--3E0B707E",
        "XN--45BRJ9C",
        "XN--45Q11C",
        "XN--4GBRIM",
        "XN--55QW42G",
        "XN--55QX5D",
        "XN--6FRZ82G",
        "XN--6QQ986B3XL",
        "XN--80ADXHKS",
        "XN--80AO21A",
        "XN--80ASEHDB",
        "XN--80ASWG",
        "XN--90A3AC",
        "XN--C1AVG",
        "XN--CG4BKI",
        "XN--CLCHC0EA0B2G2A9GCD",
        "XN--CZR694B",
        "XN--CZRS0T",
        "XN--CZRU2D",
        "XN--D1ACJ3B",
        "XN--D1ALF",
        "XN--FIQ228C5HS",
        "XN--FIQ64B",
        "XN--FIQS8S",
        "XN--FIQZ9S",
        "XN--FLW351E",
        "XN--FPCRJ9C3D",
        "XN--FZC2C9E2C",
        "XN--GECRJ9C",
        "XN--H2BRJ9C",
        "XN--HXT814E",
        "XN--I1B6B1A6A2E",
        "XN--IO0A7I",
        "XN--J1AMH",
        "XN--J6W193G",
        "XN--KPRW13D",
        "XN--KPRY57D",
        "XN--KPUT3I",
        "XN--L1ACC",
        "XN--LGBBAT1AD8J",
        "XN--MGB9AWBF",
        "XN--MGBA3A4F16A",
        "XN--MGBAAM7A8H",
        "XN--MGBAB2BD",
        "XN--MGBAYH7GPA",
        "XN--MGBBH1A71E",
        "XN--MGBC0A9AZCG",
        "XN--MGBERP4A5D4AR",
        "XN--MGBX4CD0AB",
        "XN--NGBC5AZD",
        "XN--NODE",
        "XN--NQV7F",
        "XN--NQV7FS00EMA",
        "XN--O3CW4H",
        "XN--OGBPF8FL",
        "XN--P1ACF",
        "XN--P1AI",
        "XN--PGBS0DH",
        "XN--Q9JYB4C",
        "XN--QCKA1PMC",
        "XN--RHQV96G",
        "XN--S9BRJ9C",
        "XN--SES554G",
        "XN--UNUP4Y",
        "XN--VERMGENSBERATER-CTB",
        "XN--VERMGENSBERATUNG-PWB",
        "XN--VHQUV",
        "XN--WGBH1C",
        "XN--WGBL6A",
        "XN--XHQ521B",
        "XN--XKC2AL3HYE2A",
        "XN--XKC2DL3A5EE0H",
        "XN--YFRO4I67O",
        "XN--YGBI2AMMX",
        "XN--ZFR164B",
    };
    // END JOSM PATCH

    private static final String[] COUNTRY_CODE_TLDS = new String[] {
        "ac",                 // Ascension Island
        "ad",                 // Andorra
        "ae",                 // United Arab Emirates
        "af",                 // Afghanistan
        "ag",                 // Antigua and Barbuda
        "ai",                 // Anguilla
        "al",                 // Albania
        "am",                 // Armenia
        "an",                 // Netherlands Antilles
        "ao",                 // Angola
        "aq",                 // Antarctica
        "ar",                 // Argentina
        "as",                 // American Samoa
        "at",                 // Austria
        "au",                 // Australia (includes Ashmore and Cartier Islands and Coral Sea Islands)
        "aw",                 // Aruba
        "ax",                 // Åland
        "az",                 // Azerbaijan
        "ba",                 // Bosnia and Herzegovina
        "bb",                 // Barbados
        "bd",                 // Bangladesh
        "be",                 // Belgium
        "bf",                 // Burkina Faso
        "bg",                 // Bulgaria
        "bh",                 // Bahrain
        "bi",                 // Burundi
        "bj",                 // Benin
        "bm",                 // Bermuda
        "bn",                 // Brunei Darussalam
        "bo",                 // Bolivia
        "br",                 // Brazil
        "bs",                 // Bahamas
        "bt",                 // Bhutan
        "bv",                 // Bouvet Island
        "bw",                 // Botswana
        "by",                 // Belarus
        "bz",                 // Belize
        "ca",                 // Canada
        "cc",                 // Cocos (Keeling) Islands
        "cd",                 // Democratic Republic of the Congo (formerly Zaire)
        "cf",                 // Central African Republic
        "cg",                 // Republic of the Congo
        "ch",                 // Switzerland
        "ci",                 // Côte d'Ivoire
        "ck",                 // Cook Islands
        "cl",                 // Chile
        "cm",                 // Cameroon
        "cn",                 // China, mainland
        "co",                 // Colombia
        "cr",                 // Costa Rica
        "cu",                 // Cuba
        "cv",                 // Cape Verde
        "cw",                 // Curaçao
        "cx",                 // Christmas Island
        "cy",                 // Cyprus
        "cz",                 // Czech Republic
        "de",                 // Germany
        "dj",                 // Djibouti
        "dk",                 // Denmark
        "dm",                 // Dominica
        "do",                 // Dominican Republic
        "dz",                 // Algeria
        "ec",                 // Ecuador
        "ee",                 // Estonia
        "eg",                 // Egypt
        "er",                 // Eritrea
        "es",                 // Spain
        "et",                 // Ethiopia
        "eu",                 // European Union
        "fi",                 // Finland
        "fj",                 // Fiji
        "fk",                 // Falkland Islands
        "fm",                 // Federated States of Micronesia
        "fo",                 // Faroe Islands
        "fr",                 // France
        "ga",                 // Gabon
        "gb",                 // Great Britain (United Kingdom)
        "gd",                 // Grenada
        "ge",                 // Georgia
        "gf",                 // French Guiana
        "gg",                 // Guernsey
        "gh",                 // Ghana
        "gi",                 // Gibraltar
        "gl",                 // Greenland
        "gm",                 // The Gambia
        "gn",                 // Guinea
        "gp",                 // Guadeloupe
        "gq",                 // Equatorial Guinea
        "gr",                 // Greece
        "gs",                 // South Georgia and the South Sandwich Islands
        "gt",                 // Guatemala
        "gu",                 // Guam
        "gw",                 // Guinea-Bissau
        "gy",                 // Guyana
        "hk",                 // Hong Kong
        "hm",                 // Heard Island and McDonald Islands
        "hn",                 // Honduras
        "hr",                 // Croatia (Hrvatska)
        "ht",                 // Haiti
        "hu",                 // Hungary
        "id",                 // Indonesia
        "ie",                 // Ireland (Éire)
        "il",                 // Israel
        "im",                 // Isle of Man
        "in",                 // India
        "io",                 // British Indian Ocean Territory
        "iq",                 // Iraq
        "ir",                 // Iran
        "is",                 // Iceland
        "it",                 // Italy
        "je",                 // Jersey
        "jm",                 // Jamaica
        "jo",                 // Jordan
        "jp",                 // Japan
        "ke",                 // Kenya
        "kg",                 // Kyrgyzstan
        "kh",                 // Cambodia (Khmer)
        "ki",                 // Kiribati
        "km",                 // Comoros
        "kn",                 // Saint Kitts and Nevis
        "kp",                 // North Korea
        "kr",                 // South Korea
        "kw",                 // Kuwait
        "ky",                 // Cayman Islands
        "kz",                 // Kazakhstan
        "la",                 // Laos (currently being marketed as the official domain for Los Angeles)
        "lb",                 // Lebanon
        "lc",                 // Saint Lucia
        "li",                 // Liechtenstein
        "lk",                 // Sri Lanka
        "lr",                 // Liberia
        "ls",                 // Lesotho
        "lt",                 // Lithuania
        "lu",                 // Luxembourg
        "lv",                 // Latvia
        "ly",                 // Libya
        "ma",                 // Morocco
        "mc",                 // Monaco
        "md",                 // Moldova
        "me",                 // Montenegro
        "mg",                 // Madagascar
        "mh",                 // Marshall Islands
        "mk",                 // Republic of Macedonia
        "ml",                 // Mali
        "mm",                 // Myanmar
        "mn",                 // Mongolia
        "mo",                 // Macau
        "mp",                 // Northern Mariana Islands
        "mq",                 // Martinique
        "mr",                 // Mauritania
        "ms",                 // Montserrat
        "mt",                 // Malta
        "mu",                 // Mauritius
        "mv",                 // Maldives
        "mw",                 // Malawi
        "mx",                 // Mexico
        "my",                 // Malaysia
        "mz",                 // Mozambique
        "na",                 // Namibia
        "nc",                 // New Caledonia
        "ne",                 // Niger
        "nf",                 // Norfolk Island
        "ng",                 // Nigeria
        "ni",                 // Nicaragua
        "nl",                 // Netherlands
        "no",                 // Norway
        "np",                 // Nepal
        "nr",                 // Nauru
        "nu",                 // Niue
        "nz",                 // New Zealand
        "om",                 // Oman
        "pa",                 // Panama
        "pe",                 // Peru
        "pf",                 // French Polynesia With Clipperton Island
        "pg",                 // Papua New Guinea
        "ph",                 // Philippines
        "pk",                 // Pakistan
        "pl",                 // Poland
        "pm",                 // Saint-Pierre and Miquelon
        "pn",                 // Pitcairn Islands
        "pr",                 // Puerto Rico
        "ps",                 // Palestinian territories (PA-controlled West Bank and Gaza Strip)
        "pt",                 // Portugal
        "pw",                 // Palau
        "py",                 // Paraguay
        "qa",                 // Qatar
        "re",                 // Réunion
        "ro",                 // Romania
        "rs",                 // Serbia
        "ru",                 // Russia
        "rw",                 // Rwanda
        "sa",                 // Saudi Arabia
        "sb",                 // Solomon Islands
        "sc",                 // Seychelles
        "sd",                 // Sudan
        "se",                 // Sweden
        "sg",                 // Singapore
        "sh",                 // Saint Helena
        "si",                 // Slovenia
        "sj",                 // Svalbard and Jan Mayen Islands Not in use (Norwegian dependencies; see .no)
        "sk",                 // Slovakia
        "sl",                 // Sierra Leone
        "sm",                 // San Marino
        "sn",                 // Senegal
        "so",                 // Somalia
        "sr",                 // Suriname
        "st",                 // São Tomé and Príncipe
        "su",                 // Soviet Union (deprecated)
        "sv",                 // El Salvador
        "sx",                 // Sint Maarten
        "sy",                 // Syria
        "sz",                 // Swaziland
        "tc",                 // Turks and Caicos Islands
        "td",                 // Chad
        "tf",                 // French Southern and Antarctic Lands
        "tg",                 // Togo
        "th",                 // Thailand
        "tj",                 // Tajikistan
        "tk",                 // Tokelau
        "tl",                 // East Timor (deprecated old code)
        "tm",                 // Turkmenistan
        "tn",                 // Tunisia
        "to",                 // Tonga
        "tp",                 // East Timor
        "tr",                 // Turkey
        "tt",                 // Trinidad and Tobago
        "tv",                 // Tuvalu
        "tw",                 // Taiwan, Republic of China
        "tz",                 // Tanzania
        "ua",                 // Ukraine
        "ug",                 // Uganda
        "uk",                 // United Kingdom
        "um",                 // United States Minor Outlying Islands
        "us",                 // United States of America
        "uy",                 // Uruguay
        "uz",                 // Uzbekistan
        "va",                 // Vatican City State
        "vc",                 // Saint Vincent and the Grenadines
        "ve",                 // Venezuela
        "vg",                 // British Virgin Islands
        "vi",                 // U.S. Virgin Islands
        "vn",                 // Vietnam
        "vu",                 // Vanuatu
        "wf",                 // Wallis and Futuna
        "ws",                 // Samoa (formerly Western Samoa)
        "ye",                 // Yemen
        "yt",                 // Mayotte
        "yu",                 // Serbia and Montenegro (originally Yugoslavia)
        "za",                 // South Africa
        "zm",                 // Zambia
        "zw",                 // Zimbabwe
    };

    private static final String[] LOCAL_TLDS = new String[] {
       "localhost",           // RFC2606 defined
       "localdomain"          // Also widely used as localhost.localdomain
   };

    static {
        Arrays.sort(INFRASTRUCTURE_TLDS);
        Arrays.sort(COUNTRY_CODE_TLDS);
        Arrays.sort(GENERIC_TLDS);
        Arrays.sort(IDN_TLDS);
        Arrays.sort(LOCAL_TLDS);
    }
}
