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

import java.net.IDN;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

import org.openstreetmap.josm.tools.Logging;

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
 * section 2.1. No accommodation is provided for the specialized needs of
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
 * @version $Revision: 1740822 $
 * @since Validator 1.4
 */
public final class DomainValidator extends AbstractValidator {

    private static final int MAX_DOMAIN_LENGTH = 253;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    // Regular expression strings for hostnames (derived from RFC2396 and RFC 1123)

    // RFC2396: domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
    // Max 63 characters
    private static final String DOMAIN_LABEL_REGEX = "\\p{Alnum}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    // RFC2396 toplabel = alpha | alpha *( alphanum | "-" ) alphanum
    // Max 63 characters
    private static final String TOP_LABEL_REGEX = "\\p{Alpha}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    // RFC2396 hostname = *( domainlabel "." ) toplabel [ "." ]
    // Note that the regex currently requires both a domain label and a top level label, whereas
    // the RFC does not. This is because the regex is used to detect if a TLD is present.
    // If the match fails, input is checked against DOMAIN_LABEL_REGEX (hostnameRegex)
    // RFC1123 sec 2.1 allows hostnames to start with a digit
    private static final String DOMAIN_NAME_REGEX =
            "^(?:" + DOMAIN_LABEL_REGEX + "\\.)+" + "(" + TOP_LABEL_REGEX + ")\\.?$";

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
     * RegexValidator for matching a local hostname
     */
    // RFC1123 sec 2.1 allows hostnames to start with a digit
    private final RegexValidator hostnameRegex =
            new RegexValidator(DOMAIN_LABEL_REGEX);

    /**
     * Returns the singleton instance of this validator. It
     *  will not consider local addresses as valid.
     * @return the singleton instance of this validator
     */
    public static synchronized DomainValidator getInstance() {
        inUse = true;
        return DOMAIN_VALIDATOR;
    }

    /**
     * Returns the singleton instance of this validator,
     *  with local validation as required.
     * @param allowLocal Should local addresses be considered valid?
     * @return the singleton instance of this validator
     */
    public static synchronized DomainValidator getInstance(boolean allowLocal) {
        inUse = true;
        if (allowLocal) {
            return DOMAIN_VALIDATOR_WITH_LOCAL;
        }
        return DOMAIN_VALIDATOR;
    }

    /**
     * Private constructor.
     * @param allowLocal whether to allow local domains
     */
    private DomainValidator(boolean allowLocal) {
        this.allowLocal = allowLocal;
    }

    /**
     * Returns true if the specified <code>String</code> parses
     * as a valid domain name with a recognized top-level domain.
     * The parsing is case-insensitive.
     * @param domain the parameter to check for domain name syntax
     * @return true if the parameter is a valid domain name
     */
    @Override
    public boolean isValid(String domain) {
        if (domain == null) {
            return false;
        }
        String asciiDomain = unicodeToASCII(domain);
        // hosts must be equally reachable via punycode and Unicode
        // Unicode is never shorter than punycode, so check punycode
        // if domain did not convert, then it will be caught by ASCII
        // checks in the regexes below
        if (asciiDomain.length() > MAX_DOMAIN_LENGTH) {
            return false;
        }
        String[] groups = domainRegex.match(asciiDomain);
        if (groups != null && groups.length > 0) {
            return isValidTld(groups[0]);
        }
        return allowLocal && hostnameRegex.isValid(asciiDomain);
    }

    @Override
    public String getValidatorName() {
        return null;
    }

    // package protected for unit test access
    // must agree with isValid() above
    boolean isValidDomainSyntax(String domain) {
        if (domain == null) {
            return false;
        }
        String asciiDomain = unicodeToASCII(domain);
        // hosts must be equally reachable via punycode and Unicode
        // Unicode is never shorter than punycode, so check punycode
        // if domain did not convert, then it will be caught by ASCII
        // checks in the regexes below
        if (asciiDomain.length() > MAX_DOMAIN_LENGTH) {
            return false;
        }
        String[] groups = domainRegex.match(asciiDomain);
        return (groups != null && groups.length > 0)
                || hostnameRegex.isValid(asciiDomain);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined top-level domain. Leading dots are ignored if present.
     * The search is case-insensitive.
     * @param tld the parameter to check for TLD status, not null
     * @return true if the parameter is a TLD
     */
    public boolean isValidTld(String tld) {
        String asciiTld = unicodeToASCII(tld);
        if (allowLocal && isValidLocalTld(asciiTld)) {
            return true;
        }
        return isValidInfrastructureTld(asciiTld)
                || isValidGenericTld(asciiTld)
                || isValidCountryCodeTld(asciiTld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined infrastructure top-level domain. Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param iTld the parameter to check for infrastructure TLD status, not null
     * @return true if the parameter is an infrastructure TLD
     */
    public boolean isValidInfrastructureTld(String iTld) {
        if (iTld == null) return false;
        final String key = chompLeadingDot(unicodeToASCII(iTld).toLowerCase(Locale.ENGLISH));
        return arrayContains(INFRASTRUCTURE_TLDS, key);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined generic top-level domain. Leading dots are ignored
     * if present. The search is case-insensitive.
     * @param gTld the parameter to check for generic TLD status, not null
     * @return true if the parameter is a generic TLD
     */
    public boolean isValidGenericTld(String gTld) {
        if (gTld == null) return false;
        final String key = chompLeadingDot(unicodeToASCII(gTld).toLowerCase(Locale.ENGLISH));
        return (arrayContains(GENERIC_TLDS, key) || arrayContains(genericTLDsPlus, key))
                && !arrayContains(genericTLDsMinus, key);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined country code top-level domain. Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param ccTld the parameter to check for country code TLD status, not null
     * @return true if the parameter is a country code TLD
     */
    public boolean isValidCountryCodeTld(String ccTld) {
        if (ccTld == null) return false;
        final String key = chompLeadingDot(unicodeToASCII(ccTld).toLowerCase(Locale.ENGLISH));
        return (arrayContains(COUNTRY_CODE_TLDS, key) || arrayContains(countryCodeTLDsPlus, key))
                && !arrayContains(countryCodeTLDsMinus, key);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * widely used "local" domains (localhost or localdomain). Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param lTld the parameter to check for local TLD status, not null
     * @return true if the parameter is an local TLD
     */
    public boolean isValidLocalTld(String lTld) {
        if (lTld == null) return false;
        final String key = chompLeadingDot(unicodeToASCII(lTld).toLowerCase(Locale.ENGLISH));
        return arrayContains(LOCAL_TLDS, key);
    }

    private static String chompLeadingDot(String str) {
        if (str.startsWith(".")) {
            return str.substring(1);
        }
        return str;
    }

    // ---------------------------------------------
    // ----- TLDs defined by IANA
    // ----- Authoritative and comprehensive list at:
    // ----- http://data.iana.org/TLD/tlds-alpha-by-domain.txt

    // Note that the above list is in UPPER case.
    // The code currently converts strings to lower case (as per the tables below)

    // IANA also provide an HTML list at http://www.iana.org/domains/root/db
    // Note that this contains several country code entries which are NOT in
    // the text file. These all have the "Not assigned" in the "Sponsoring Organisation" column
    // For example (as of 2015-01-02):
    // .bl  country-code    Not assigned
    // .um  country-code    Not assigned

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] INFRASTRUCTURE_TLDS = {
        "arpa",               // internet infrastructure
    };

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] GENERIC_TLDS = {
        // Taken from Version 2026111200, Last Updated Thu Nov 12 07:07:01 2026 UTC
        "aaa", // aaa American Automobile Association, Inc.
        "aarp", // aarp AARP
        "abb", // abb ABB Ltd
        "abbott", // abbott Abbott Laboratories, Inc.
        "abbvie", // abbvie AbbVie Inc.
        "abc", // abc Disney Enterprises, Inc.
        "able", // able Able Inc.
        "abogado", // abogado Registry Services, LLC
        "abudhabi", // abudhabi Abu Dhabi Systems and Information Centre
        "academy", // academy Binky Moon, LLC
        "accenture", // accenture Accenture plc
        "accountant", // accountant dot Accountant Limited
        "accountants", // accountants Binky Moon, LLC
        "aco", // aco ACO Severin Ahlmann GmbH & Co. KG
        "actor", // actor Dog Beach, LLC
        "ads", // ads Charleston Road Registry Inc.
        "adult", // adult ICM Registry AD LLC
        "aeg", // aeg Aktiebolaget Electrolux
        "aero", // aero Societe Internationale de Telecommunications Aeronautique (SITA INC USA)
        "aetna", // aetna Aetna Life Insurance Company
        "afl", // afl Australian Football League
        "africa", // africa ZA Central Registry NPC trading as Registry.Africa
        "agakhan", // agakhan Fondation Aga Khan (Aga Khan Foundation)
        "agency", // agency Binky Moon, LLC
        "aig", // aig American International Group, Inc.
        "airbus", // airbus Airbus S.A.S.
        "airforce", // airforce Dog Beach, LLC
        "airtel", // airtel Bharti Airtel Limited
        "akdn", // akdn Fondation Aga Khan (Aga Khan Foundation)
        "alibaba", // alibaba Alibaba Group Holding Limited
        "alipay", // alipay Alibaba Group Holding Limited
        "allfinanz", // allfinanz Allfinanz Deutsche Vermögensberatung Aktiengesellschaft
        "allstate", // allstate Allstate Fire and Casualty Insurance Company
        "ally", // ally Ally Financial Inc.
        "alsace", // alsace REGION GRAND EST
        "alstom", // alstom ALSTOM
        "amazon", // amazon Amazon Registry Services, Inc.
        "americanexpress", // americanexpress American Express Travel Related Services Company, Inc.
        "americanfamily", // americanfamily AmFam, Inc.
        "amex", // amex American Express Travel Related Services Company, Inc.
        "amfam", // amfam AmFam, Inc.
        "amica", // amica Amica Mutual Insurance Company
        "amsterdam", // amsterdam Gemeente Amsterdam
        "analytics", // analytics Campus IP LLC
        "android", // android Charleston Road Registry Inc.
        "anquan", // anquan Beijing Qihu Keji Co., Ltd.
        "anz", // anz Australia and New Zealand Banking Group Limited
        "aol", // aol AOL Media LLC
        "apartments", // apartments Binky Moon, LLC
        "app", // app Charleston Road Registry Inc.
        "apple", // apple Apple Inc.
        "aquarelle", // aquarelle Aquarelle.com
        "arab", // arab League of Arab States
        "aramco", // aramco Aramco Services Company
        "archi", // archi Identity Digital Limited
        "army", // army Dog Beach, LLC
        "art", // art UK Creative Ideas Limited
        "arte", // arte Association Relative à la Télévision Européenne G.E.I.E.
        "asda", // asda Asda Stores Limited
        "asia", // asia DotAsia Organisation Ltd.
        "associates", // associates Binky Moon, LLC
        "athleta", // athleta The Gap, Inc.
        "attorney", // attorney Dog Beach, LLC
        "auction", // auction Dog Beach, LLC
        "audi", // audi AUDI Aktiengesellschaft
        "audible", // audible Amazon Registry Services, Inc.
        "audio", // audio XYZ.COM LLC
        "auspost", // auspost Australian Postal Corporation
        "author", // author Amazon Registry Services, Inc.
        "auto", // auto XYZ.COM LLC
        "autos", // autos XYZ.COM LLC
        "aws", // aws AWS Registry LLC
        "axa", // axa AXA Group Operations SAS
        "azure", // azure Microsoft Corporation
        "baby", // baby XYZ.COM LLC
        "baidu", // baidu Baidu, Inc.
        "banamex", // banamex Citigroup Inc.
        "band", // band Dog Beach, LLC
        "bank", // bank fTLD Registry Services, LLC
        "bar", // bar Punto 2012 Sociedad Anonima Promotora de Inversion de Capital Variable
        "barcelona", // barcelona Municipi de Barcelona
        "barclaycard", // barclaycard Barclays Bank PLC
        "barclays", // barclays Barclays Bank PLC
        "barefoot", // barefoot Gallo Vineyards, Inc.
        "bargains", // bargains Binky Moon, LLC
        "baseball", // baseball MLB Advanced Media DH, LLC
        "basketball", // basketball Fédération Internationale de Basketball (FIBA)
        "bauhaus", // bauhaus Werkhaus GmbH
        "bayern", // bayern Bayern Connect GmbH
        "bbc", // bbc British Broadcasting Corporation
        "bbt", // bbt BB&T Corporation
        "bbva", // bbva BANCO BILBAO VIZCAYA ARGENTARIA, S.A.
        "bcg", // bcg The Boston Consulting Group, Inc.
        "bcn", // bcn Municipi de Barcelona
        "beats", // beats Beats Electronics, LLC
        "beauty", // beauty XYZ.COM LLC
        "beer", // beer Registry Services, LLC
        "berlin", // berlin dotBERLIN GmbH & Co. KG
        "best", // best BestTLD Pty Ltd
        "bestbuy", // bestbuy BBY Solutions, Inc.
        "bet", // bet Identity Digital Limited
        "bharti", // bharti Bharti Enterprises (Holding) Private Limited
        "bible", // bible American Bible Society
        "bid", // bid dot Bid Limited
        "bike", // bike Binky Moon, LLC
        "bing", // bing Microsoft Corporation
        "bingo", // bingo Binky Moon, LLC
        "bio", // bio Identity Digital Limited
        "biz", // biz Registry Services, LLC
        "black", // black Identity Digital Limited
        "blackfriday", // blackfriday Registry Services, LLC
        "blockbuster", // blockbuster Dish DBS Corporation
        "blog", // blog Knock Knock WHOIS There, LLC
        "bloomberg", // bloomberg Bloomberg IP Holdings LLC
        "blue", // blue Identity Digital Limited
        "bms", // bms Bristol-Myers Squibb Company
        "bmw", // bmw Bayerische Motoren Werke Aktiengesellschaft
        "bnpparibas", // bnpparibas BNP Paribas
        "boats", // boats XYZ.COM LLC
        "boehringer", // boehringer Boehringer Ingelheim International GmbH
        "bofa", // bofa Bank of America Corporation
        "bom", // bom Núcleo de Informação e Coordenação do Ponto BR - NIC.br
        "bond", // bond Shortdot SA
        "boo", // boo Charleston Road Registry Inc.
        "book", // book Amazon Registry Services, Inc.
        "booking", // booking Booking.com B.V.
        "bosch", // bosch Robert Bosch GMBH
        "bostik", // bostik Bostik SA
        "boston", // boston Registry Services, LLC
        "bot", // bot Amazon Registry Services, Inc.
        "boutique", // boutique Binky Moon, LLC
        "box", // box Intercap Registry Inc.
        "bradesco", // bradesco Banco Bradesco S.A.
        "bridgestone", // bridgestone Bridgestone Corporation
        "broadway", // broadway Celebrate Broadway, Inc.
        "broker", // broker Dog Beach, LLC
        "brother", // brother Brother Industries, Ltd.
        "brussels", // brussels DNS.be vzw
        "build", // build Plan Bee LLC
        "builders", // builders Binky Moon, LLC
        "business", // business Binky Moon, LLC
        "buy", // buy Amazon Registry Services, INC
        "buzz", // buzz DOTSTRATEGY CO.
        "bzh", // bzh Association www.bzh
        "cab", // cab Binky Moon, LLC
        "cafe", // cafe Binky Moon, LLC
        "cal", // cal Charleston Road Registry Inc.
        "call", // call Amazon Registry Services, Inc.
        "calvinklein", // calvinklein PVH gTLD Holdings LLC
        "cam", // cam CAM Connecting SarL
        "camera", // camera Binky Moon, LLC
        "camp", // camp Binky Moon, LLC
        "canon", // canon Canon Inc.
        "capetown", // capetown ZA Central Registry NPC trading as ZA Central Registry
        "capital", // capital Binky Moon, LLC
        "capitalone", // capitalone Capital One Financial Corporation
        "car", // car XYZ.COM LLC
        "caravan", // caravan Caravan International, Inc.
        "cards", // cards Binky Moon, LLC
        "care", // care Binky Moon, LLC
        "career", // career dotCareer LLC
        "careers", // careers Binky Moon, LLC
        "cars", // cars XYZ.COM LLC
        "casa", // casa Registry Services, LLC
        "case", // case Digity, LLC
        "cash", // cash Binky Moon, LLC
        "casino", // casino Binky Moon, LLC
        "cat", // cat Fundacio puntCAT
        "catering", // catering Binky Moon, LLC
        "catholic", // catholic Pontificium Consilium de Comunicationibus Socialibus (PCCS) (Pontifical Council for Social Communication)
        "cba", // cba COMMONWEALTH BANK OF AUSTRALIA
        "cbn", // cbn The Christian Broadcasting Network, Inc.
        "cbre", // cbre CBRE, Inc.
        "center", // center Binky Moon, LLC
        "ceo", // ceo XYZ.COM LLC
        "cern", // cern European Organization for Nuclear Research ("CERN")
        "cfa", // cfa CFA Institute
        "cfd", // cfd Shortdot SA
        "chanel", // chanel Chanel International B.V.
        "channel", // channel Charleston Road Registry Inc.
        "charity", // charity Public Interest Registry (PIR)
        "chase", // chase JPMorgan Chase Bank, National Association
        "chat", // chat Binky Moon, LLC
        "cheap", // cheap Binky Moon, LLC
        "chintai", // chintai CHINTAI Corporation
        "christmas", // christmas XYZ.COM LLC
        "chrome", // chrome Charleston Road Registry Inc.
        "church", // church Binky Moon, LLC
        "cipriani", // cipriani Hotel Cipriani Srl
        "circle", // circle Jolly Host, LLC
        "cisco", // cisco Cisco Technology, Inc.
        "citadel", // citadel Citadel Domain LLC
        "citi", // citi Citigroup Inc.
        "citic", // citic CITIC Group Corporation
        "city", // city Binky Moon, LLC
        "claims", // claims Binky Moon, LLC
        "cleaning", // cleaning Binky Moon, LLC
        "click", // click Internet Naming Co.
        "clinic", // clinic Binky Moon, LLC
        "clinique", // clinique The Estée Lauder Companies Inc.
        "clothing", // clothing Binky Moon, LLC
        "cloud", // cloud ARUBA PEC S.p.A.
        "club", // club Registry Services, LLC
        "clubmed", // clubmed Club Méditerranée S.A.
        "coach", // coach Binky Moon, LLC
        "codes", // codes Binky Moon, LLC
        "coffee", // coffee Binky Moon, LLC
        "college", // college XYZ.COM LLC
        "cologne", // cologne domainworx Service & Management GmbH
        "com", // com VeriSign Global Registry Services
        "commbank", // commbank COMMONWEALTH BANK OF AUSTRALIA
        "community", // community Binky Moon, LLC
        "company", // company Binky Moon, LLC
        "compare", // compare Registry Services, LLC
        "computer", // computer Binky Moon, LLC
        "comsec", // comsec VeriSign, Inc.
        "condos", // condos Binky Moon, LLC
        "construction", // construction Binky Moon, LLC
        "consulting", // consulting Dog Beach, LLC
        "contact", // contact Dog Beach, LLC
        "contractors", // contractors Binky Moon, LLC
        "cooking", // cooking Registry Services, LLC
        "cool", // cool Binky Moon, LLC
        "coop", // coop DotCooperation LLC
        "corsica", // corsica Collectivité de Corse
        "country", // country Internet Naming Co.
        "coupon", // coupon Amazon Registry Services, Inc.
        "coupons", // coupons Binky Moon, LLC
        "courses", // courses Registry Services, LLC
        "cpa", // cpa American Institute of Certified Public Accountants
        "credit", // credit Binky Moon, LLC
        "creditcard", // creditcard Binky Moon, LLC
        "creditunion", // creditunion DotCooperation, LLC
        "cricket", // cricket dot Cricket Limited
        "crown", // crown Crown Equipment Corporation
        "crs", // crs Federated Co-operatives Limited
        "cruise", // cruise Viking River Cruises (Bermuda) Ltd.
        "cruises", // cruises Binky Moon, LLC
        "cuisinella", // cuisinella SCHMIDT GROUPE S.A.S.
        "cymru", // cymru Nominet UK
        "cyou", // cyou Shortdot SA
        "dad", // dad Charleston Road Registry Inc.
        "dance", // dance Dog Beach, LLC
        "data", // data Dish DBS Corporation
        "date", // date dot Date Limited
        "dating", // dating Binky Moon, LLC
        "datsun", // datsun NISSAN MOTOR CO., LTD.
        "day", // day Charleston Road Registry Inc.
        "dclk", // dclk Charleston Road Registry Inc.
        "dds", // dds Registry Services, LLC
        "deal", // deal Amazon Registry Services, Inc.
        "dealer", // dealer Intercap Registry Inc.
        "deals", // deals Binky Moon, LLC
        "degree", // degree Dog Beach, LLC
        "delivery", // delivery Binky Moon, LLC
        "dell", // dell Dell Inc.
        "deloitte", // deloitte Deloitte Touche Tohmatsu
        "delta", // delta Delta Air Lines, Inc.
        "democrat", // democrat Dog Beach, LLC
        "dental", // dental Binky Moon, LLC
        "dentist", // dentist Dog Beach, LLC
        "desi", // desi Emergency Back-End Registry Operator Program - ICANN
        "design", // design Registry Services, LLC
        "dev", // dev Charleston Road Registry Inc.
        "dhl", // dhl Deutsche Post AG
        "diamonds", // diamonds Binky Moon, LLC
        "diet", // diet XYZ.COM LLC
        "digital", // digital Binky Moon, LLC
        "direct", // direct Binky Moon, LLC
        "directory", // directory Binky Moon, LLC
        "discount", // discount Binky Moon, LLC
        "discover", // discover Discover Financial Services
        "dish", // dish Dish DBS Corporation
        "diy", // diy Internet Naming Co.
        "dnp", // dnp Dai Nippon Printing Co., Ltd.
        "docs", // docs Charleston Road Registry Inc.
        "doctor", // doctor Binky Moon, LLC
        "dog", // dog Binky Moon, LLC
        "domains", // domains Binky Moon, LLC
        "dot", // dot Dish DBS Corporation
        "download", // download dot Support Limited
        "drive", // drive Charleston Road Registry Inc.
        "dtv", // dtv Dish DBS Corporation
        "dubai", // dubai Dubai Smart Government Department
        "dupont", // dupont DuPont Specialty Products USA, LLC
        "durban", // durban ZA Central Registry NPC trading as ZA Central Registry
        "dvag", // dvag Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "dvr", // dvr DISH Technologies L.L.C.
        "earth", // earth Interlink Systems Innovation Institute K.K.
        "eat", // eat Charleston Road Registry Inc.
        "eco", // eco Big Room Inc.
        "edeka", // edeka EDEKA Verband kaufmännischer Genossenschaften e.V.
        "edu", // edu EDUCAUSE
        "education", // education Binky Moon, LLC
        "email", // email Binky Moon, LLC
        "emerck", // emerck Merck KGaA
        "energy", // energy Binky Moon, LLC
        "engineer", // engineer Dog Beach, LLC
        "engineering", // engineering Binky Moon, LLC
        "enterprises", // enterprises Binky Moon, LLC
        "epson", // epson Seiko Epson Corporation
        "equipment", // equipment Binky Moon, LLC
        "ericsson", // ericsson Telefonaktiebolaget L M Ericsson
        "erni", // erni ERNI Group Holding AG
        "esq", // esq Charleston Road Registry Inc.
        "estate", // estate Binky Moon, LLC
        "eurovision", // eurovision European Broadcasting Union (EBU)
        "eus", // eus Puntueus Fundazioa
        "events", // events Binky Moon, LLC
        "exchange", // exchange Binky Moon, LLC
        "expert", // expert Binky Moon, LLC
        "exposed", // exposed Binky Moon, LLC
        "express", // express Binky Moon, LLC
        "extraspace", // extraspace Extra Space Storage LLC
        "fage", // fage Fage International S.A.
        "fail", // fail Binky Moon, LLC
        "fairwinds", // fairwinds FairWinds Partners, LLC
        "faith", // faith dot Faith Limited
        "family", // family Dog Beach, LLC
        "fan", // fan Dog Beach, LLC
        "fans", // fans ZDNS International Limited
        "farm", // farm Binky Moon, LLC
        "farmers", // farmers Farmers Insurance Exchange
        "fashion", // fashion Registry Services, LLC
        "fast", // fast Amazon Registry Services, Inc.
        "fedex", // fedex Federal Express Corporation
        "feedback", // feedback Top Level Spectrum, Inc.
        "ferrari", // ferrari Fiat Chrysler Automobiles N.V.
        "ferrero", // ferrero Ferrero Trading Lux S.A.
        "fidelity", // fidelity Fidelity Brokerage Services LLC
        "fido", // fido Rogers Communications Canada Inc.
        "film", // film Motion Picture Domain Registry Pty Ltd
        "final", // final Núcleo de Informação e Coordenação do Ponto BR - NIC.br
        "finance", // finance Binky Moon, LLC
        "financial", // financial Binky Moon, LLC
        "fire", // fire Amazon Registry Services, Inc.
        "firestone", // firestone Bridgestone Licensing Services, Inc.
        "firmdale", // firmdale Firmdale Holdings Limited
        "fish", // fish Binky Moon, LLC
        "fishing", // fishing Registry Services, LLC
        "fit", // fit Registry Services, LLC
        "fitness", // fitness Binky Moon, LLC
        "flickr", // flickr Flickr, Inc.
        "flights", // flights Binky Moon, LLC
        "flir", // flir FLIR Systems, Inc.
        "florist", // florist Binky Moon, LLC
        "flowers", // flowers XYZ.COM LLC
        "fly", // fly Charleston Road Registry Inc.
        "foo", // foo Charleston Road Registry Inc.
        "food", // food Internet Naming Co.
        "football", // football Binky Moon, LLC
        "ford", // ford Ford Motor Company
        "forex", // forex Dog Beach, LLC
        "forsale", // forsale Dog Beach, LLC
        "forum", // forum Fegistry, LLC
        "foundation", // foundation Public Interest Registry (PIR)
        "fox", // fox FOX Registry, LLC
        "free", // free Amazon Registry Services, Inc.
        "fresenius", // fresenius Fresenius Immobilien-Verwaltungs-GmbH
        "frl", // frl FRLregistry B.V.
        "frogans", // frogans OP3FT
        "frontier", // frontier Frontier Communications Corporation
        "ftr", // ftr Frontier Communications Corporation
        "fujitsu", // fujitsu Fujitsu Limited
        "fun", // fun Radix Technologies Inc.
        "fund", // fund Binky Moon, LLC
        "furniture", // furniture Binky Moon, LLC
        "futbol", // futbol Dog Beach, LLC
        "fyi", // fyi Binky Moon, LLC
        "gal", // gal Asociación puntoGAL
        "gallery", // gallery Binky Moon, LLC
        "gallo", // gallo Gallo Vineyards, Inc.
        "gallup", // gallup Gallup, Inc.
        "game", // game XYZ.COM LLC
        "games", // games Dog Beach, LLC
        "gap", // gap The Gap, Inc.
        "garden", // garden Registry Services, LLC
        "gay", // gay Registry Services, LLC
        "gbiz", // gbiz Charleston Road Registry Inc.
        "gdn", // gdn Joint Stock Company "Navigation-information systems"
        "gea", // gea GEA Group Aktiengesellschaft
        "gent", // gent Easyhost bv
        "genting", // genting Resorts World Inc. Pte. Ltd.
        "george", // george Wal-Mart Stores, Inc.
        "ggee", // ggee GMO Internet, Inc.
        "gift", // gift Uniregistry, Corp.
        "gifts", // gifts Binky Moon, LLC
        "gives", // gives Public Interest Registry (PIR)
        "giving", // giving Public Interest Registry (PIR)
        "glass", // glass Binky Moon, LLC
        "gle", // gle Charleston Road Registry Inc.
        "global", // global Identity Digital Limited
        "globo", // globo Globo Comunicação e Participações S.A
        "gmail", // gmail Charleston Road Registry Inc.
        "gmbh", // gmbh Binky Moon, LLC
        "gmo", // gmo GMO Internet, Inc.
        "gmx", // gmx 1&1 Mail & Media GmbH
        "godaddy", // godaddy Go Daddy East, LLC
        "gold", // gold Binky Moon, LLC
        "goldpoint", // goldpoint YODOBASHI CAMERA CO.,LTD.
        "golf", // golf Binky Moon, LLC
        "goodyear", // goodyear The Goodyear Tire & Rubber Company
        "goog", // goog Charleston Road Registry Inc.
        "google", // google Charleston Road Registry Inc.
        "gop", // gop Republican State Leadership Committee, Inc.
        "got", // got Jolly Host, LLC
        "gov", // gov Cybersecurity and Infrastructure Security Agency
        "grainger", // grainger Grainger Registry Services, LLC
        "graphics", // graphics Binky Moon, LLC
        "gratis", // gratis Binky Moon, LLC
        "green", // green Identity Digital Limited
        "gripe", // gripe Binky Moon, LLC
        "grocery", // grocery Wal-Mart Stores, Inc.
        "group", // group Binky Moon, LLC
        "gucci", // gucci Guccio Gucci S.p.a.
        "guge", // guge Charleston Road Registry Inc.
        "guide", // guide Binky Moon, LLC
        "guitars", // guitars XYZ.COM LLC
        "guru", // guru Binky Moon, LLC
        "hair", // hair XYZ.COM LLC
        "hamburg", // hamburg Hamburg Top-Level-Domain GmbH
        "hangout", // hangout Charleston Road Registry Inc.
        "haus", // haus Dog Beach, LLC
        "hbo", // hbo HBO Registry Services, Inc.
        "hdfc", // hdfc HOUSING DEVELOPMENT FINANCE CORPORATION LIMITED
        "hdfcbank", // hdfcbank HDFC Bank Limited
        "health", // health Registry Services, LLC
        "healthcare", // healthcare Binky Moon, LLC
        "help", // help Innovation Service Ltd
        "here", // here Charleston Road Registry Inc.
        "hermes", // hermes Hermes International
        "hiphop", // hiphop Dot Hip Hop, LLC
        "hisamitsu", // hisamitsu Hisamitsu Pharmaceutical Co.,Inc.
        "hitachi", // hitachi Hitachi, Ltd.
        "hiv", // hiv Internet Naming Co.
        "hkt", // hkt PCCW-HKT DataCom Services Limited
        "hockey", // hockey Binky Moon, LLC
        "holdings", // holdings Binky Moon, LLC
        "holiday", // holiday Binky Moon, LLC
        "homedepot", // homedepot Home Depot Product Authority, LLC
        "homegoods", // homegoods The TJX Companies, Inc.
        "homes", // homes XYZ.COM LLC
        "homesense", // homesense The TJX Companies, Inc.
        "honda", // honda Honda Motor Co., Ltd.
        "horse", // horse Registry Services, LLC
        "hospital", // hospital Binky Moon, LLC
        "host", // host Radix Technologies Inc.
        "hosting", // hosting XYZ.COM LLC
        "hot", // hot Amazon Registry Services, Inc.
        "hotels", // hotels Booking.com B.V.
        "hotmail", // hotmail Microsoft Corporation
        "house", // house Binky Moon, LLC
        "how", // how Charleston Road Registry Inc.
        "hsbc", // hsbc HSBC Global Services (UK) Limited
        "hughes", // hughes Hughes Satellite Systems Corporation
        "hyatt", // hyatt Hyatt GTLD, L.L.C.
        "hyundai", // hyundai Hyundai Motor Company
        "ibm", // ibm International Business Machines Corporation
        "icbc", // icbc Industrial and Commercial Bank of China Limited
        "ice", // ice IntercontinentalExchange, Inc.
        "icu", // icu Shortdot SA
        "ieee", // ieee IEEE Global LLC
        "ifm", // ifm ifm electronic gmbh
        "ikano", // ikano Ikano S.A.
        "imamat", // imamat Fondation Aga Khan (Aga Khan Foundation)
        "imdb", // imdb Amazon Registry Services, Inc.
        "immo", // immo Binky Moon, LLC
        "immobilien", // immobilien Dog Beach, LLC
        "inc", // inc Intercap Registry Inc.
        "industries", // industries Binky Moon, LLC
        "infiniti", // infiniti NISSAN MOTOR CO., LTD.
        "info", // info Identity Digital Limited
        "ing", // ing Charleston Road Registry Inc.
        "ink", // ink Registry Services, LLC
        "institute", // institute Binky Moon, LLC
        "insurance", // insurance fTLD Registry Services LLC
        "insure", // insure Binky Moon, LLC
        "int", // int Internet Assigned Numbers Authority
        "international", // international Binky Moon, LLC
        "intuit", // intuit Intuit Administrative Services, Inc.
        "investments", // investments Binky Moon, LLC
        "ipiranga", // ipiranga Ipiranga Produtos de Petroleo S.A.
        "irish", // irish Binky Moon, LLC
        "ismaili", // ismaili Fondation Aga Khan (Aga Khan Foundation)
        "ist", // ist Istanbul Metropolitan Municipality
        "istanbul", // istanbul Istanbul Metropolitan Municipality
        "itau", // itau Itau Unibanco Holding S.A.
        "itv", // itv ITV Services Limited
        "jaguar", // jaguar Jaguar Land Rover Ltd
        "java", // java Oracle Corporation
        "jcb", // jcb JCB Co., Ltd.
        "jeep", // jeep FCA US LLC.
        "jetzt", // jetzt Binky Moon, LLC
        "jewelry", // jewelry Binky Moon, LLC
        "jio", // jio Reliance Industries Limited
        "jll", // jll Jones Lang LaSalle Incorporated
        "jmp", // jmp Matrix IP LLC
        "jnj", // jnj Johnson & Johnson Services, Inc.
        "jobs", // jobs Employ Media LLC
        "joburg", // joburg ZA Central Registry NPC trading as ZA Central Registry
        "jot", // jot Jolly Host, LLC
        "joy", // joy Amazon Registry Services, Inc.
        "jpmorgan", // jpmorgan JPMorgan Chase Bank, National Association
        "jprs", // jprs Japan Registry Services Co., Ltd.
        "juegos", // juegos Dog Beach, LLC
        "kaufen", // kaufen Dog Beach, LLC
        "kddi", // kddi KDDI CORPORATION
        "kerryhotels", // kerryhotels Kerry Trading Co. Limited
        "kerryproperties", // kerryproperties Kerry Trading Co. Limited
        "kfh", // kfh Kuwait Finance House
        "kia", // kia KIA MOTORS CORPORATION
        "kids", // kids DotKids Foundation Limited
        "kim", // kim Identity Digital Limited
        "kindle", // kindle Amazon Registry Services, Inc.
        "kitchen", // kitchen Binky Moon, LLC
        "kiwi", // kiwi DOT KIWI LIMITED
        "koeln", // koeln domainworx Service & Management GmbH
        "komatsu", // komatsu Komatsu Ltd.
        "kosher", // kosher Kosher Marketing Assets LLC
        "kpmg", // kpmg KPMG International Cooperative (KPMG International Genossenschaft)
        "kpn", // kpn Koninklijke KPN N.V.
        "krd", // krd KRG Department of Information Technology
        "kred", // kred KredTLD Pty Ltd
        "kuokgroup", // kuokgroup Kerry Trading Co. Limited
        "kyoto", // kyoto Academic Institution: Kyoto Jyoho Gakuen
        "lacaixa", // lacaixa Fundación Bancaria Caixa d'Estalvis i Pensions de Barcelona, "la Caixa"
        "lamborghini", // lamborghini Automobili Lamborghini S.p.A.
        "lamer", // lamer The Estée Lauder Companies Inc.
        "land", // land Binky Moon, LLC
        "landrover", // landrover Jaguar Land Rover Ltd
        "lanxess", // lanxess LANXESS Corporation
        "lasalle", // lasalle Jones Lang LaSalle Incorporated
        "lat", // lat XYZ.COM LLC
        "latino", // latino Dish DBS Corporation
        "latrobe", // latrobe La Trobe University
        "law", // law Registry Services, LLC
        "lawyer", // lawyer Dog Beach, LLC
        "lds", // lds IRI Domain Management, LLC
        "lease", // lease Binky Moon, LLC
        "leclerc", // leclerc A.C.D. LEC Association des Centres Distributeurs Edouard Leclerc
        "lefrak", // lefrak LeFrak Organization, Inc.
        "legal", // legal Binky Moon, LLC
        "lego", // lego LEGO Juris A/S
        "lexus", // lexus TOYOTA MOTOR CORPORATION
        "lgbt", // lgbt Identity Digital Limited
        "lidl", // lidl Schwarz Domains und Services GmbH & Co. KG
        "life", // life Binky Moon, LLC
        "lifeinsurance", // lifeinsurance American Council of Life Insurers
        "lifestyle", // lifestyle Internet Naming Co.
        "lighting", // lighting Binky Moon, LLC
        "like", // like Amazon Registry Services, Inc.
        "lilly", // lilly Eli Lilly and Company
        "limited", // limited Binky Moon, LLC
        "limo", // limo Binky Moon, LLC
        "lincoln", // lincoln Ford Motor Company
        "link", // link Nova Registry Ltd.
        "live", // live Dog Beach, LLC
        "living", // living Internet Naming Co.
        "llc", // llc Identity Digital Limited
        "llp", // llp Intercap Registry Inc.
        "loan", // loan dot Loan Limited
        "loans", // loans Binky Moon, LLC
        "locker", // locker Orange Domains LLC
        "locus", // locus Locus Analytics LLC
        "lol", // lol XYZ.COM LLC
        "london", // london Dot London Domains Limited
        "lotte", // lotte Lotte Holdings Co., Ltd.
        "lotto", // lotto Identity Digital Limited
        "love", // love Waterford Limited
        "lpl", // lpl LPL Holdings, Inc.
        "lplfinancial", // lplfinancial LPL Holdings, Inc.
        "ltd", // ltd Binky Moon, LLC
        "ltda", // ltda InterNetX Corp.
        "lundbeck", // lundbeck H. Lundbeck A/S
        "luxe", // luxe Registry Services, LLC
        "luxury", // luxury Luxury Partners LLC
        "madrid", // madrid Comunidad de Madrid
        "maif", // maif Mutuelle Assurance Instituteur France (MAIF)
        "maison", // maison Binky Moon, LLC
        "makeup", // makeup XYZ.COM LLC
        "man", // man MAN Truck & Bus SE
        "management", // management Binky Moon, LLC
        "mango", // mango PUNTO FA S.L.
        "map", // map Charleston Road Registry Inc.
        "market", // market Dog Beach, LLC
        "marketing", // marketing Binky Moon, LLC
        "markets", // markets Dog Beach, LLC
        "marriott", // marriott Marriott Worldwide Corporation
        "marshalls", // marshalls The TJX Companies, Inc.
        "mattel", // mattel Mattel IT Services, Inc.
        "mba", // mba Binky Moon, LLC
        "mckinsey", // mckinsey McKinsey Holdings, Inc.
        "med", // med Medistry LLC
        "media", // media Binky Moon, LLC
        "meet", // meet Charleston Road Registry Inc.
        "melbourne", // melbourne The Crown in right of the State of Victoria, Department of State Development, Business and Innovation
        "meme", // meme Charleston Road Registry Inc.
        "memorial", // memorial Dog Beach, LLC
        "men", // men Exclusive Registry Limited
        "menu", // menu Dot Menu Registry LLC
        "merck", // merck Merck Registry Holdings, Inc.
        "merckmsd", // merckmsd MSD Registry Holdings, Inc.
        "miami", // miami Registry Services, LLC
        "microsoft", // microsoft Microsoft Corporation
        "mil", // mil DoD Network Information Center
        "mini", // mini Bayerische Motoren Werke Aktiengesellschaft
        "mint", // mint Intuit Administrative Services, Inc.
        "mit", // mit Massachusetts Institute of Technology
        "mitsubishi", // mitsubishi Mitsubishi Corporation
        "mlb", // mlb MLB Advanced Media DH, LLC
        "mls", // mls The Canadian Real Estate Association
        "mma", // mma MMA IARD
        "mobi", // mobi Identity Digital Limited
        "mobile", // mobile Dish DBS Corporation
        "moda", // moda Dog Beach, LLC
        "moe", // moe Interlink Systems Innovation Institute K.K.
        "moi", // moi Amazon Registry Services, Inc.
        "mom", // mom XYZ.COM LLC
        "monash", // monash Monash University
        "money", // money Binky Moon, LLC
        "monster", // monster XYZ.COM LLC
        "mormon", // mormon IRI Domain Management, LLC ("Applicant")
        "mortgage", // mortgage Dog Beach, LLC
        "moscow", // moscow Foundation for Assistance for Internet Technologies and Infrastructure Development (FAITID)
        "moto", // moto Motorola Trademark Holdings, LLC
        "motorcycles", // motorcycles XYZ.COM LLC
        "mov", // mov Charleston Road Registry Inc.
        "movie", // movie Binky Moon, LLC
        "msd", // msd MSD Registry Holdings, Inc.
        "mtn", // mtn MTN Dubai Limited
        "mtr", // mtr MTR Corporation Limited
        "museum", // museum Museum Domain Management Association
        "music", // music DotMusic Limited
        "nab", // nab National Australia Bank Limited
        "nagoya", // nagoya GMO Registry, Inc.
        "name", // name VeriSign Information Services, Inc.
        "navy", // navy Dog Beach, LLC
        "nba", // nba NBA REGISTRY, LLC
        "nec", // nec NEC Corporation
        "net", // net VeriSign Global Registry Services
        "netbank", // netbank COMMONWEALTH BANK OF AUSTRALIA
        "netflix", // netflix Netflix, Inc.
        "network", // network Binky Moon, LLC
        "neustar", // neustar NeuStar, Inc.
        "new", // new Charleston Road Registry Inc.
        "news", // news Dog Beach, LLC
        "next", // next Next plc
        "nextdirect", // nextdirect Next plc
        "nexus", // nexus Charleston Road Registry Inc.
        "nfl", // nfl NFL Reg Ops LLC
        "ngo", // ngo Public Interest Registry
        "nhk", // nhk Japan Broadcasting Corporation (NHK)
        "nico", // nico DWANGO Co., Ltd.
        "nike", // nike NIKE, Inc.
        "nikon", // nikon NIKON CORPORATION
        "ninja", // ninja Dog Beach, LLC
        "nissan", // nissan NISSAN MOTOR CO., LTD.
        "nissay", // nissay Nippon Life Insurance Company
        "nokia", // nokia Nokia Corporation
        "norton", // norton Gen Digital Inc.
        "now", // now Amazon Registry Services, Inc.
        "nowruz", // nowruz Emergency Back-End Registry Operator Program - ICANN
        "nowtv", // nowtv Starbucks (HK) Limited
        "nra", // nra National Rifle Association of America
        "nrw", // nrw Minds + Machines GmbH
        "ntt", // ntt NIPPON TELEGRAPH AND TELEPHONE CORPORATION
        "nyc", // nyc The City of New York by and through the New York City Department of Information Technology & Telecommunications
        "obi", // obi OBI Group Holding SE & Co. KGaA
        "observer", // observer Fegistry, LLC
        "office", // office Microsoft Corporation
        "okinawa", // okinawa BRregistry, Inc.
        "olayan", // olayan Competrol (Luxembourg) Sarl
        "olayangroup", // olayangroup Competrol (Luxembourg) Sarl
        "ollo", // ollo Dish DBS Corporation
        "omega", // omega The Swatch Group Ltd
        "one", // one One.com A/S
        "ong", // ong Public Interest Registry
        "onl", // onl Jolly Host, LLC
        "online", // online Radix Technologies Inc. SEZC
        "ooo", // ooo INFIBEAM AVENUES LIMITED
        "open", // open American Express Travel Related Services Company, Inc.
        "oracle", // oracle Oracle Corporation
        "orange", // orange Orange Brand Services Limited
        "org", // org Public Interest Registry (PIR)
        "organic", // organic Identity Digital Limited
        "origins", // origins The Estée Lauder Companies Inc.
        "osaka", // osaka Osaka Registry Co., Ltd.
        "otsuka", // otsuka Otsuka Holdings Co., Ltd.
        "ott", // ott Dish DBS Corporation
        "ovh", // ovh OVH SAS
        "page", // page Charleston Road Registry Inc.
        "panasonic", // panasonic Panasonic Corporation
        "paris", // paris City of Paris
        "pars", // pars Emergency Back-End Registry Operator Program - ICANN
        "partners", // partners Binky Moon, LLC
        "parts", // parts Binky Moon, LLC
        "party", // party Blue Sky Registry Limited
        "pay", // pay Amazon Registry Services, Inc.
        "pccw", // pccw PCCW Enterprises Limited
        "pet", // pet Identity Digital Limited
        "pfizer", // pfizer Pfizer Inc.
        "pharmacy", // pharmacy National Association of Boards of Pharmacy
        "phd", // phd Charleston Road Registry Inc.
        "philips", // philips Koninklijke Philips N.V.
        "phone", // phone Dish DBS Corporation
        "photo", // photo Registry Services, LLC
        "photography", // photography Binky Moon, LLC
        "photos", // photos Binky Moon, LLC
        "physio", // physio PhysBiz Pty Ltd
        "pics", // pics XYZ.COM LLC
        "pictet", // pictet Banque Pictet & Cie SA
        "pictures", // pictures Binky Moon, LLC
        "pid", // pid Top Level Spectrum, Inc.
        "pin", // pin Amazon Registry Services, Inc.
        "ping", // ping Ping Registry Provider, Inc.
        "pink", // pink Identity Digital Limited
        "pioneer", // pioneer Pioneer Corporation
        "pizza", // pizza Binky Moon, LLC
        "place", // place Binky Moon, LLC
        "play", // play Charleston Road Registry Inc.
        "playstation", // playstation Sony Computer Entertainment Inc.
        "plumbing", // plumbing Binky Moon, LLC
        "plus", // plus Binky Moon, LLC
        "pnc", // pnc PNC Domain Co., LLC
        "pohl", // pohl Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "poker", // poker Identity Digital Limited
        "politie", // politie Politie Nederland
        "porn", // porn ICM Registry PN LLC
        "post", // post Universal Postal Union
        "praxi", // praxi Praxi S.p.A.
        "press", // press Radix Technologies Inc.
        "prime", // prime Amazon Registry Services, Inc.
        "pro", // pro Identity Digital Limited
        "prod", // prod Charleston Road Registry Inc.
        "productions", // productions Binky Moon, LLC
        "prof", // prof Charleston Road Registry Inc.
        "progressive", // progressive Progressive Casualty Insurance Company
        "promo", // promo Identity Digital Limited
        "properties", // properties Binky Moon, LLC
        "property", // property Digital Property Infrastructure Limited
        "protection", // protection XYZ.COM LLC
        "pru", // pru Prudential Financial, Inc.
        "prudential", // prudential Prudential Financial, Inc.
        "pub", // pub Dog Beach, LLC
        "pwc", // pwc PricewaterhouseCoopers LLP
        "qpon", // qpon DOTQPON LLC.
        "quebec", // quebec PointQuébec Inc
        "quest", // quest XYZ.COM LLC
        "racing", // racing Premier Registry Limited
        "radio", // radio Digity, LLC
        "read", // read Amazon Registry Services, Inc.
        "realestate", // realestate dotRealEstate LLC
        "realtor", // realtor Real Estate Domains LLC
        "realty", // realty Internet Naming Co.
        "recipes", // recipes Binky Moon, LLC
        "red", // red Identity Digital Limited
        "redumbrella", // redumbrella Travelers TLD, LLC
        "rehab", // rehab Dog Beach, LLC
        "reise", // reise Binky Moon, LLC
        "reisen", // reisen Binky Moon, LLC
        "reit", // reit National Association of Real Estate Investment Trusts, Inc.
        "reliance", // reliance Reliance Industries Limited
        "ren", // ren ZDNS International Limited
        "rent", // rent XYZ.COM LLC
        "rentals", // rentals Binky Moon, LLC
        "repair", // repair Binky Moon, LLC
        "report", // report Binky Moon, LLC
        "republican", // republican Dog Beach, LLC
        "rest", // rest Punto 2012 Sociedad Anonima Promotora de Inversion de Capital Variable
        "restaurant", // restaurant Binky Moon, LLC
        "review", // review dot Review Limited
        "reviews", // reviews Dog Beach, LLC
        "rexroth", // rexroth Robert Bosch GMBH
        "rich", // rich iRegistry GmbH
        "richardli", // richardli Pacific Century Asset Management (HK) Limited
        "ricoh", // ricoh Ricoh Company, Ltd.
        "ril", // ril Reliance Industries Limited
        "rio", // rio Empresa Municipal de Informática SA - IPLANRIO
        "rip", // rip Dog Beach, LLC
        "rocks", // rocks Dog Beach, LLC
        "rodeo", // rodeo Registry Services, LLC
        "rogers", // rogers Rogers Communications Canada Inc.
        "room", // room Amazon Registry Services, Inc.
        "rsvp", // rsvp Charleston Road Registry Inc.
        "rugby", // rugby World Rugby Strategic Developments Limited
        "ruhr", // ruhr dotSaarland GmbH
        "run", // run Binky Moon, LLC
        "rwe", // rwe RWE AG
        "ryukyu", // ryukyu BRregistry, Inc.
        "saarland", // saarland dotSaarland GmbH
        "safe", // safe Amazon Registry Services, Inc.
        "safety", // safety Jolly Host, LLC
        "sakura", // sakura SAKURA internet Inc.
        "sale", // sale Dog Beach, LLC
        "salon", // salon Binky Moon, LLC
        "samsclub", // samsclub Wal-Mart Stores, Inc.
        "samsung", // samsung SAMSUNG SDS CO., LTD
        "sandvik", // sandvik Sandvik AB
        "sandvikcoromant", // sandvikcoromant Sandvik AB
        "sanofi", // sanofi Sanofi
        "sap", // sap SAP AG
        "sarl", // sarl Binky Moon, LLC
        "sas", // sas Research IP LLC
        "save", // save Amazon Registry Services, Inc.
        "saxo", // saxo Saxo Bank A/S
        "sbi", // sbi STATE BANK OF INDIA
        "sbs", // sbs Shortdot SA
        "scb", // scb The Siam Commercial Bank Public Company Limited ("SCB")
        "schaeffler", // schaeffler Schaeffler Technologies AG & Co. KG
        "schmidt", // schmidt SCHMIDT GROUPE S.A.S.
        "scholarships", // scholarships Scholarships.com, LLC
        "school", // school Binky Moon, LLC
        "schule", // schule Binky Moon, LLC
        "schwarz", // schwarz Schwarz Domains und Services GmbH & Co. KG
        "science", // science dot Science Limited
        "scot", // scot Dot Scot Registry Limited
        "search", // search Charleston Road Registry Inc.
        "seat", // seat SEAT, S.A. (Sociedad Unipersonal)
        "secure", // secure Amazon Registry Services, Inc.
        "security", // security XYZ.COM LLC
        "seek", // seek Seek Limited
        "select", // select Registry Services, LLC
        "sener", // sener Sener Ingeniería y Sistemas, S.A.
        "services", // services Binky Moon, LLC
        "seven", // seven Seven West Media Ltd
        "sew", // sew SEW-EURODRIVE GmbH & Co KG
        "sex", // sex ICM Registry SX LLC
        "sexy", // sexy Internet Naming Co.
        "sfr", // sfr Societe Francaise du Radiotelephone - SFR
        "shangrila", // shangrila Shangri-La International Hotel Management Limited
        "sharp", // sharp Sharp Corporation
        "shell", // shell Shell Information Technology International Inc
        "shia", // shia Emergency Back-End Registry Operator Program - ICANN
        "shiksha", // shiksha Identity Digital Limited
        "shoes", // shoes Binky Moon, LLC
        "shop", // shop GMO Registry, Inc.
        "shopping", // shopping Binky Moon, LLC
        "shouji", // shouji Beijing Qihu Keji Co., Ltd.
        "show", // show Binky Moon, LLC
        "silk", // silk Amazon Registry Services, Inc.
        "sina", // sina Sina Corporation
        "singles", // singles Binky Moon, LLC
        "site", // site Radix Technologies Inc. SEZC
        "ski", // ski Identity Digital Limited
        "skin", // skin XYZ.COM LLC
        "sky", // sky Sky UK Limited
        "skype", // skype Microsoft Corporation
        "sling", // sling DISH Technologies L.L.C.
        "smart", // smart Smart Communications, Inc. (SMART)
        "smile", // smile Amazon Registry Services, Inc.
        "sncf", // sncf Société Nationale SNCF
        "soccer", // soccer Binky Moon, LLC
        "social", // social Dog Beach, LLC
        "softbank", // softbank SoftBank Group Corp.
        "software", // software Dog Beach, LLC
        "sohu", // sohu Sohu.com Limited
        "solar", // solar Binky Moon, LLC
        "solutions", // solutions Binky Moon, LLC
        "song", // song Amazon Registry Services, Inc.
        "sony", // sony Sony Corporation
        "soy", // soy Charleston Road Registry Inc.
        "spa", // spa Asia Spa and Wellness Promotion Council Limited
        "space", // space Radix Technologies Inc.
        "sport", // sport SportAccord
        "spot", // spot Amazon Registry Services, Inc.
        "srl", // srl InterNetX Corp.
        "stada", // stada STADA Arzneimittel AG
        "staples", // staples Staples, Inc.
        "star", // star Star India Private Limited
        "statebank", // statebank STATE BANK OF INDIA
        "statefarm", // statefarm State Farm Mutual Automobile Insurance Company
        "stc", // stc Saudi Telecom Company
        "stcgroup", // stcgroup Saudi Telecom Company
        "stockholm", // stockholm Stockholms kommun
        "storage", // storage XYZ.COM LLC
        "store", // store Radix Technologies Inc.
        "stream", // stream dot Stream Limited
        "studio", // studio Dog Beach, LLC
        "study", // study Registry Services, LLC
        "style", // style Binky Moon, LLC
        "sucks", // sucks Vox Populi Registry Ltd.
        "supplies", // supplies Binky Moon, LLC
        "supply", // supply Binky Moon, LLC
        "support", // support Binky Moon, LLC
        "surf", // surf Registry Services, LLC
        "surgery", // surgery Binky Moon, LLC
        "suzuki", // suzuki SUZUKI MOTOR CORPORATION
        "swatch", // swatch The Swatch Group Ltd
        "swiss", // swiss Swiss Confederation
        "sydney", // sydney State of New South Wales, Department of Premier and Cabinet
        "systems", // systems Binky Moon, LLC
        "tab", // tab Tabcorp Holdings Limited
        "taipei", // taipei Taipei City Government
        "talk", // talk Amazon Registry Services, Inc.
        "taobao", // taobao Alibaba Group Holding Limited
        "target", // target Target Domain Holdings, LLC
        "tatamotors", // tatamotors Tata Motors Ltd
        "tatar", // tatar Limited Liability Company "Coordination Center of Regional Domain of Tatarstan Republic"
        "tattoo", // tattoo Registry Services, LLC
        "tax", // tax Binky Moon, LLC
        "taxi", // taxi Binky Moon, LLC
        "tci", // tci Emergency Back-End Registry Operator Program - ICANN
        "tdk", // tdk TDK Corporation
        "team", // team Binky Moon, LLC
        "tech", // tech Radix Technologies Inc.
        "technology", // technology Binky Moon, LLC
        "tel", // tel Telnames Ltd.
        "temasek", // temasek Temasek Holdings (Private) Limited
        "tennis", // tennis Binky Moon, LLC
        "teva", // teva Teva Pharmaceutical Industries Limited
        "thd", // thd Home Depot Product Authority, LLC
        "theater", // theater Binky Moon, LLC
        "theatre", // theatre XYZ.COM LLC
        "tiaa", // tiaa Teachers Insurance and Annuity Association of America
        "tickets", // tickets XYZ.COM LLC
        "tienda", // tienda Binky Moon, LLC
        "tips", // tips Binky Moon, LLC
        "tires", // tires Binky Moon, LLC
        "tirol", // tirol punkt Tirol GmbH
        "tjmaxx", // tjmaxx The TJX Companies, Inc.
        "tjx", // tjx The TJX Companies, Inc.
        "tkmaxx", // tkmaxx The TJX Companies, Inc.
        "tmall", // tmall Alibaba Group Holding Limited
        "today", // today Binky Moon, LLC
        "tokyo", // tokyo GMO Registry, Inc.
        "tools", // tools Binky Moon, LLC
        "top", // top Hong Kong Zhongze International Limited
        "toray", // toray Toray Industries, Inc.
        "toshiba", // toshiba TOSHIBA Corporation
        "total", // total TotalEnergies SE
        "tours", // tours Binky Moon, LLC
        "town", // town Binky Moon, LLC
        "toyota", // toyota TOYOTA MOTOR CORPORATION
        "toys", // toys Binky Moon, LLC
        "trade", // trade Elite Registry Limited
        "trading", // trading Dog Beach, LLC
        "training", // training Binky Moon, LLC
        "travel", // travel Dog Beach, LLC
        "travelers", // travelers Travelers TLD, LLC
        "travelersinsurance", // travelersinsurance Travelers TLD, LLC
        "trust", // trust Internet Naming Co.
        "trv", // trv Travelers TLD, LLC
        "tube", // tube Latin American Telecom LLC
        "tui", // tui TUI AG
        "tunes", // tunes Amazon Registry Services, Inc.
        "tushu", // tushu Amazon Registry Services, Inc.
        "tvs", // tvs T V SUNDRAM IYENGAR & SONS PRIVATE LIMITED
        "ubank", // ubank National Australia Bank Limited
        "ubs", // ubs UBS AG
        "unicom", // unicom China United Network Communications Corporation Limited
        "university", // university Binky Moon, LLC
        "uno", // uno Radix Technologies Inc.
        "uol", // uol UBN INTERNET LTDA.
        "ups", // ups UPS Market Driver, Inc.
        "vacations", // vacations Binky Moon, LLC
        "vana", // vana D3 Registry LLC
        "vanguard", // vanguard The Vanguard Group, Inc.
        "vegas", // vegas Dot Vegas, Inc.
        "ventures", // ventures Binky Moon, LLC
        "verisign", // verisign VeriSign, Inc.
        "versicherung", // versicherung tldbox GmbH
        "vet", // vet Dog Beach, LLC
        "viajes", // viajes Binky Moon, LLC
        "video", // video Dog Beach, LLC
        "vig", // vig VIENNA INSURANCE GROUP AG Wiener Versicherung Gruppe
        "viking", // viking Viking River Cruises (Bermuda) Ltd.
        "villas", // villas Binky Moon, LLC
        "vin", // vin Binky Moon, LLC
        "vip", // vip Registry Services, LLC
        "virgin", // virgin Virgin Enterprises Limited
        "visa", // visa Visa Worldwide Pte. Limited
        "vision", // vision Binky Moon, LLC
        "viva", // viva Saudi Telecom Company
        "vivo", // vivo Telefonica Brasil S.A.
        "vlaanderen", // vlaanderen DNS.be vzw
        "vodka", // vodka Registry Services, LLC
        "volvo", // volvo Volvo Holding Sverige Aktiebolag
        "vote", // vote Monolith Registry LLC
        "voting", // voting Valuetainment Corp.
        "voto", // voto Monolith Registry LLC
        "voyage", // voyage Binky Moon, LLC
        "wales", // wales Nominet UK
        "walmart", // walmart Wal-Mart Stores, Inc.
        "walter", // walter Sandvik AB
        "wang", // wang Zodiac Wang Limited
        "wanggou", // wanggou Amazon Registry Services, Inc.
        "watch", // watch Binky Moon, LLC
        "watches", // watches Identity Digital Limited
        "weather", // weather The Weather Company, LLC
        "weatherchannel", // weatherchannel The Weather Company, LLC
        "web", // web VeriSign, Inc.
        "webcam", // webcam dot Webcam Limited
        "weber", // weber Saint-Gobain Weber SA
        "website", // website Radix Technologies Inc.
        "wed", // wed Emergency Back-End Registry Operator Program - ICANN
        "wedding", // wedding Registry Services, LLC
        "weibo", // weibo Sina Corporation
        "weir", // weir Weir Group IP Limited
        "whoswho", // whoswho Who's Who Registry
        "wien", // wien punkt.wien GmbH
        "wiki", // wiki Registry Services, LLC
        "williamhill", // williamhill William Hill Organization Limited
        "win", // win First Registry Limited
        "windows", // windows Microsoft Corporation
        "wine", // wine Binky Moon, LLC
        "winners", // winners The TJX Companies, Inc.
        "wme", // wme William Morris Endeavor Entertainment, LLC
        "woodside", // woodside Woodside Petroleum Limited
        "work", // work Registry Services, LLC
        "works", // works Binky Moon, LLC
        "world", // world Binky Moon, LLC
        "wow", // wow Amazon Registry Services, Inc.
        "wtc", // wtc World Trade Centers Association, Inc.
        "wtf", // wtf Binky Moon, LLC
        "xbox", // xbox Microsoft Corporation
        "xerox", // xerox Xerox DNHC LLC
        "xihuan", // xihuan Beijing Qihu Keji Co., Ltd.
        "xin", // xin Elegant Leader Limited
        "xn--11b4c3d", // कॉम VeriSign Sarl
        "xn--1ck2e1b", // セール Amazon Registry Services, Inc.
        "xn--1qqw23a", // 佛山 Guangzhou YU Wei Information Technology Co., Ltd.
        "xn--30rr7y", // 慈善 Excellent First Limited
        "xn--3bst00m", // 集团 Eagle Horizon Limited
        "xn--3ds443g", // 在线 Beijing TLD Registry Technology Limited
        "xn--3pxu8k", // 点看 VeriSign Sarl
        "xn--42c2d9a", // คอม VeriSign Sarl
        "xn--45q11c", // 八卦 Zodiac Gemini Ltd
        "xn--4gbrim", // موقع Helium TLDs Ltd
        "xn--55qw42g", // 公益 China Organizational Name Administration Center
        "xn--55qx5d", // 公司 China Internet Network Information Center (CNNIC)
        "xn--5su34j936bgsg", // 香格里拉 Shangri-La International Hotel Management Limited
        "xn--5tzm5g", // 网站 Jolly Host, LLC
        "xn--6frz82g", // 移动 Identity Digital Limited
        "xn--6qq986b3xl", // 我爱你 Tycoon Treasure Limited
        "xn--80adxhks", // москва Foundation for Assistance for Internet Technologies and Infrastructure Development (FAITID)
        "xn--80aqecdr1a", // католик Pontificium Consilium de Comunicationibus Socialibus (PCCS) (Pontifical Council for Social Communication)
        "xn--80asehdb", // онлайн CORE Association
        "xn--80aswg", // сайт CORE Association
        "xn--8y0a063a", // 联通 China United Network Communications Corporation Limited
        "xn--9dbq2a", // קום VeriSign Sarl
        "xn--9et52u", // 时尚 RISE VICTORY LIMITED
        "xn--9krt00a", // 微博 Sina Corporation
        "xn--b4w605ferd", // 淡马锡 Temasek Holdings (Private) Limited
        "xn--bck1b9a5dre4c", // ファッション Amazon Registry Services, Inc.
        "xn--c1avg", // орг Public Interest Registry
        "xn--c2br7g", // नेट VeriSign Sarl
        "xn--cck2b3b", // ストア Amazon Registry Services, Inc.
        "xn--cckwcxetd", // アマゾン Amazon Registry Services, Inc.
        "xn--cg4bki", // 삼성 SAMSUNG SDS CO., LTD
        "xn--czr694b", // 商标 Internet DotTrademark Organisation Limited
        "xn--czrs0t", // 商店 Binky Moon, LLC
        "xn--czru2d", // 商城 Zodiac Aquarius Limited
        "xn--d1acj3b", // дети The Foundation for Network Initiatives “The Smart Internet”
        "xn--eckvdtc9d", // ポイント Amazon Registry Services, Inc.
        "xn--efvy88h", // 新闻 Guangzhou YU Wei Information and Technology Co.,Ltd
        "xn--fct429k", // 家電 Amazon Registry Services, Inc.
        "xn--fhbei", // كوم VeriSign Sarl
        "xn--fiq228c5hs", // 中文网 Beijing TLD Registry Technology Limited
        "xn--fiq64b", // 中信 CITIC Group Corporation
        "xn--fjq720a", // 娱乐 Binky Moon, LLC
        "xn--flw351e", // 谷歌 Charleston Road Registry Inc.
        "xn--fzys8d69uvgm", // 電訊盈科 PCCW Enterprises Limited
        "xn--g2xx48c", // 购物 Nawang Heli(Xiamen) Network Service Co., LTD.
        "xn--gckr3f0f", // クラウド Amazon Registry Services, Inc.
        "xn--gk3at1e", // 通販 Amazon Registry Services, Inc.
        "xn--hxt814e", // 网店 Zodiac Taurus Ltd.
        "xn--i1b6b1a6a2e", // संगठन Public Interest Registry
        "xn--imr513n", // 餐厅 Internet DotTrademark Organisation Limited
        "xn--io0a7i", // 网络 China Internet Network Information Center (CNNIC)
        "xn--j1aef", // ком VeriSign Sarl
        "xn--jlq480n2rg", // 亚马逊 Amazon Registry Services, Inc.
        "xn--jvr189m", // 食品 Amazon Registry Services, Inc.
        "xn--kcrx77d1x4a", // 飞利浦 Koninklijke Philips N.V.
        "xn--kput3i", // 手机 Beijing RITT-Net Technology Development Co., Ltd
        "xn--mgba3a3ejt", // ارامكو Aramco Services Company
        "xn--mgba7c0bbn0a", // العليان Competrol (Luxembourg) Sarl
        "xn--mgbab2bd", // بازار CORE Association
        "xn--mgbca7dzdo", // ابوظبي Abu Dhabi Systems and Information Centre
        "xn--mgbi4ecexp", // كاثوليك Pontificium Consilium de Comunicationibus Socialibus (PCCS) (Pontifical Council for Social Communication)
        "xn--mgbt3dhd", // همراه Emergency Back-End Registry Operator Program - ICANN
        "xn--mk1bu44c", // 닷컴 VeriSign Sarl
        "xn--mxtq1m", // 政府 Net-Chinese Co., Ltd.
        "xn--ngbc5azd", // شبكة International Domain Registry Pty. Ltd.
        "xn--ngbe9e0a", // بيتك Kuwait Finance House
        "xn--ngbrx", // عرب League of Arab States
        "xn--nqv7f", // 机构 Public Interest Registry
        "xn--nqv7fs00ema", // 组织机构 Public Interest Registry
        "xn--nyqy26a", // 健康 Stable Tone Limited
        "xn--otu796d", // 招聘 Jiang Yu Liang Cai Technology Company Limited
        "xn--p1acf", // рус Rusnames Limited
        "xn--pssy2u", // 大拿 VeriSign Sarl
        "xn--q9jyb4c", // みんな Charleston Road Registry Inc.
        "xn--qcka1pmc", // グーグル Charleston Road Registry Inc.
        "xn--rhqv96g", // 世界 Stable Tone Limited
        "xn--rovu88b", // 書籍 Amazon Registry Services, Inc.
        "xn--ses554g", // 网址 KNET Co., Ltd
        "xn--t60b56a", // 닷넷 VeriSign Sarl
        "xn--tckwe", // コム VeriSign Sarl
        "xn--tiq49xqyj", // 天主教 Pontificium Consilium de Comunicationibus Socialibus (PCCS) (Pontifical Council for Social Communication)
        "xn--unup4y", // 游戏 Binky Moon, LLC
        "xn--vermgensberater-ctb", // VERMöGENSBERATER Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "xn--vermgensberatung-pwb", // VERMöGENSBERATUNG Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "xn--vhquv", // 企业 Binky Moon, LLC
        "xn--vuq861b", // 信息 Beijing Tele-info Technology Co., Ltd.
        "xn--w4r85el8fhu5dnra", // 嘉里大酒店 Kerry Trading Co. Limited
        "xn--w4rs40l", // 嘉里 Kerry Trading Co. Limited
        "xn--xhq521b", // 广东 Guangzhou YU Wei Information Technology Co., Ltd.
        "xn--zfr164b", // 政务 China Organizational Name Administration Center
        "xxx", // xxx ICM Registry LLC
        "xyz", // xyz XYZ.COM LLC
        "yachts", // yachts XYZ.COM LLC
        "yahoo", // yahoo Yahoo Inc.
        "yamaxun", // yamaxun Amazon Registry Services, Inc.
        "yandex", // yandex YANDEX LLC
        "yodobashi", // yodobashi YODOBASHI CAMERA CO.,LTD.
        "yoga", // yoga Registry Services, LLC
        "yokohama", // yokohama GMO Registry, Inc.
        "you", // you Amazon Registry Services, Inc.
        "youtube", // youtube Charleston Road Registry Inc.
        "yun", // yun Beijing Qihu Keji Co., Ltd.
        "zappos", // zappos Amazon Registry Services, Inc.
        "zara", // zara Industria de Diseño Textil, S.A. (INDITEX, S.A.)
        "zero", // zero Amazon Registry Services, Inc.
        "zip", // zip Charleston Road Registry Inc.
        "zone", // zone Binky Moon, LLC
        "zuerich", // zuerich Kanton Zürich (Canton of Zurich)
    };

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] COUNTRY_CODE_TLDS = {
        // Taken from Version 2026111200, Last Updated Thu Nov 12 07:07:01 2026 UTC
        "ac", // ac Internet Computer Bureau Limited
        "ad", // ad Andorra Telecom
        "ae", // ae Telecommunications and Digital Government Regulatory Authority (TDRA)
        "af", // af Ministry of Communications and IT
        "ag", // ag UHSA School of Medicine
        "ai", // ai Government of Anguilla
        "al", // al Electronic and Postal Communications Authority - AKEP
        "am", // am "Internet Society" Non-governmental Organization
        "ao", // ao Ministry of Telecommunications and Information Technologies (MTTI)
        "aq", // aq Antarctica Network Information Centre Limited
        "ar", // ar Presidencia de la Nación , Secretaría Legal y Técnica
        "as", // as AS Domain Registry
        "at", // at nic.at GmbH
        "au", // au .au Domain Administration (auDA)
        "aw", // aw SETAR
        "ax", // ax Ålands landskapsregering
        "az", // az IntraNS
        "ba", // ba University Tele-Informatic Center (UTIC)
        "bb", // bb Ministry of Innovation, Science and Smart Technology
        "bd", // bd Posts and Telecommunications Division
        "be", // be DNS Belgium vzw/asbl
        "bf", // bf Autorité de Régulation des Communications Electroniques et des Postes (ARCEP)
        "bg", // bg Register.BG
        "bh", // bh Telecommunications Regulatory Authority (TRA)
        "bi", // bi Centre National de l'Informatique
        "bj", // bj Autorité de Régulation des Communications Electroniques et de la Poste du Bénin (ARCEP BENIN)
        "bm", // bm Registry General Department, Ministry of Home Affairs
        "bn", // bn Authority for Info-communications Technology Industry of Brunei Darussalam (AITI)
        "bo", // bo Agencia de Gobierno Electrónico y Tecnologías de Información y Comunicación
        "br", // br Comite Gestor da Internet no Brasil
        "bs", // bs University of The Bahamas
        "bt", // bt Ministry of Information and Communications
        "bv", // bv Norid A/S
        "bw", // bw Botswana Communications Regulatory Authority (BOCRA)
        "by", // by Belarusian Cloud Technologies LLC
        "bz", // bz University of Belize
        "ca", // ca Canadian Internet Registration Authority (CIRA) Autorité Canadienne pour les enregistrements Internet (ACEI)
        "cc", // cc eNIC Cocos (Keeling) Islands Pty. Ltd. d/b/a Island Internet Services
        "cd", // cd Office Congolais des Postes et Télécommunications - OCPT
        "cf", // cf Societe Centrafricaine de Telecommunications (SOCATEL)
        "cg", // cg Interpoint Switzerland
        "ch", // ch SWITCH The Swiss Education & Research Network
        "ci", // ci Autorité de Régulation des Télécommunications/TIC de Côte d’lvoire (ARTCI)
        "ck", // ck Telecom Cook Islands Ltd.
        "cl", // cl NIC Chile (University of Chile)
        "cm", // cm Agence Nationale des Technologies de l'Information et de la Communication (ANTIC)
        "cn", // cn China Internet Network Information Center (CNNIC)
        "co", // co Ministry of Information and Communications Technologies (MinTIC)
        "cr", // cr National Academy of Sciences (Academia Nacional de Ciencias)
        "cu", // cu CENIAInternet
        "cv", // cv Agência Reguladora Multissectorial da Economia (ARME)
        "cw", // cw University of Curacao
        "cx", // cx Christmas Island Domain Administration Limited
        "cy", // cy University of Cyprus
        "cz", // cz CZ.NIC, z.s.p.o
        "de", // de DENIC eG
        "dj", // dj Djibouti Telecom S.A
        "dk", // dk Dansk Internet Forum
        "dm", // dm DotDM Corporation
        "do", // do Pontificia Universidad Catolica Madre y Maestra, Recinto Santo Tomas de Aquino
        "dz", // dz CERIST
        "ec", // ec ECUADORDOMAIN S.A.
        "ee", // ee Eesti Interneti Sihtasutus (EIS)
        "eg", // eg Egyptian Universities Network (EUN), Supreme Council of Universities
        "er", // er Eritrea Telecommunication Services Corporation (EriTel)
        "es", // es Red.es
        "et", // et Ethio telecom
        "eu", // eu EURid vzw
        "fi", // fi Finnish Transport and Communications Agency Traficom
        "fj", // fj The University of the South Pacific IT Services
        "fk", // fk Falkland Islands Government
        "fm", // fm FSM Telecommunications Corporation
        "fo", // fo FO Council
        "fr", // fr Association Française pour le Nommage Internet en Coopération (A.F.N.I.C.)
        "ga", // ga Agence Nationale des Infrastructures Numériques et des Fréquences (ANINF)
        "gb", // gb Reserved Domain - IANA
        "gd", // gd The National Telecommunications Regulatory Commission (NTRC)
        "ge", // ge Caucasus Online LLC
        "gf", // gf CANAL+ TELECOM
        "gg", // gg Island Networks Ltd.
        "gh", // gh Network Computer Systems Limited
        "gi", // gi Sapphire Networks
        "gl", // gl TELE Greenland A/S
        "gm", // gm GM-NIC
        "gn", // gn Centre National des Sciences Halieutiques de Boussoura
        "gp", // gp Networking Technologies Group
        "gq", // gq GETESA
        "gr", // gr ICS-FORTH GR
        "gs", // gs Government of South Georgia and South Sandwich Islands (GSGSSI)
        "gt", // gt Universidad del Valle de Guatemala
        "gu", // gu University of Guam
        "gw", // gw Autoridade Reguladora Nacional - Tecnologias de Informação e Comunicação da Guiné-Bissau
        "gy", // gy University of Guyana
        "hk", // hk Hong Kong Internet Registration Corporation Ltd.
        "hm", // hm HM Domain Registry
        "hn", // hn Red de Desarrollo Sostenible Honduras
        "hr", // hr CARNet - Croatian Academic and Research Network
        "ht", // ht Consortium FDS/RDDH
        "hu", // hu Council of Hungarian Internet Providers (CHIP)
        "id", // id Perkumpulan Pengelola Nama Domain Internet Indonesia (PANDI)
        "ie", // ie IE Domain Registry CLG
        "il", // il The Israel Internet Association (RA)
        "im", // im Isle of Man Government
        "in", // in National Internet Exchange of India
        "io", // io Internet Computer Bureau Limited
        "iq", // iq Communications and Media Commission (CMC)
        "ir", // ir Institute for Research in Fundamental Sciences
        "is", // is ISNIC - Internet á Íslandi hf.
        "it", // it IIT - CNR
        "je", // je Island Networks (Jersey) Ltd.
        "jm", // jm University of West Indies
        "jo", // jo Ministry of Digital Economy and Entrepreneurship (MoDEE)
        "jp", // jp Japan Registry Services Co., Ltd.
        "ke", // ke Kenya Network Information Center (KeNIC)
        "kg", // kg AsiaInfo Telecommunication Enterprise
        "kh", // kh Telecommunication Regulator of Cambodia (TRC)
        "ki", // ki Ministry of Information, Communications and Transport (MICT)
        "km", // km Comores Telecom
        "kn", // kn Ministry of Finance, Sustainable Development Information & Technology
        "kp", // kp Star Joint Venture Company
        "kr", // kr Korea Internet & Security Agency (KISA)
        "kw", // kw Communications and Information Technology Regulatory Authority
        "ky", // ky Utility Regulation and Competition Office (OfReg)
        "kz", // kz Association of IT Companies of Kazakhstan
        "la", // la Lao National Internet Center (LANIC), Ministry of Technology and Communications
        "lb", // lb Internet Society Lebanon
        "lc", // lc University of Puerto Rico
        "li", // li SWITCH The Swiss Education & Research Network
        "lk", // lk Council for Information Technology LK Domain Registrar
        "lr", // lr Data Technology Solutions, Inc.
        "ls", // ls Lesotho Network Information Centre Proprietary (LSNIC)
        "lt", // lt Kaunas University of Technology
        "lu", // lu RESTENA
        "lv", // lv University of Latvia, Institute of Mathematics and Computer Science, Department of Network Solutions (DNS)
        "ly", // ly General Authority of Communications and Informatics (GACI)
        "ma", // ma Agence Nationale de Réglementation des Télécommunications (ANRT)
        "mc", // mc Direction des Plateformes et des Ressources Numériques
        "md", // md IP Serviciul Tehnologia Informatiei si Securitate Cibernetica
        "me", // me Government of Montenegro
        "mg", // mg NIC-MG (Network Information Center Madagascar)
        "mh", // mh Office of the Cabinet
        "mk", // mk Macedonian Academic Research Network Skopje
        "ml", // ml Agence des Technologies de l’Information et de la Communication
        "mm", // mm Ministry of Transport and Communications
        "mn", // mn Datacom Co., Ltd.
        "mo", // mo Macao Post and Telecommunications Bureau (CTT)
        "mp", // mp Saipan Datacom, Inc.
        "mq", // mq CANAL+ TELECOM
        "mr", // mr Université de Nouakchott Al Aasriya
        "ms", // ms MNI Networks Ltd.
        "mt", // mt NIC (Malta)
        "mu", // mu Internet Direct Ltd
        "mv", // mv Dhivehi Raajjeyge Gulhun PLC
        "mw", // mw Malawi Sustainable Development Network Programme (Malawi SDNP)
        "mx", // mx NIC-Mexico ITESM - Campus Monterrey
        "my", // my MYNIC Berhad
        "mz", // mz Centro de Informatica de Universidade Eduardo Mondlane
        "na", // na Namibian Network Information Center
        "nc", // nc Office des Postes et Telecommunications
        "ne", // ne Niger Télécoms SA (NIGERTELECOMS)
        "nf", // nf Norfolk Island Data Services
        "ng", // ng Nigeria Internet Registration Association
        "ni", // ni Universidad Nacional del Ingernieria. Division de Tecnologias de la Informacion.
        "nl", // nl SIDN (Stichting Internet Domeinregistratie Nederland)
        "no", // no Norid A/S
        "np", // np Mercantile Communications Pvt. Ltd.
        "nr", // nr CENPAC NET
        "nu", // nu The IUSN Foundation
        "nz", // nz InternetNZ
        "om", // om Telecommunications Regulatory Authority (TRA)
        "pa", // pa Universidad Tecnologica de Panama
        "pe", // pe Red Cientifica Peruana
        "pf", // pf Gouvernement de la Polynésie française
        "pg", // pg PNG DNS Administration, Vice Chancellors Office, The Papua New Guinea University of Technology
        "ph", // ph PH Domain Foundation
        "pk", // pk PKNIC
        "pl", // pl Research and Academic Computer Network
        "pm", // pm Association Française pour le Nommage Internet en Coopération (A.F.N.I.C.)
        "pn", // pn Pitcairn Island Administration
        "pr", // pr Gauss Research Laboratory Inc.
        "ps", // ps Ministry of Telecommunications & Information Technology, Government Computer Center
        "pt", // pt Associação DNS.PT
        "pw", // pw Micronesia Investment and Development Corporation
        "py", // py NIC-PY
        "qa", // qa Communications Regulatory Authority
        "re", // re Association Française pour le Nommage Internet en Coopération (A.F.N.I.C.)
        "ro", // ro National Institute for R&D in Informatics
        "rs", // rs Serbian National Internet Domain Registry (RNIDS)
        "ru", // ru Coordination Center for TLD RU
        "rw", // rw Rwanda Internet Community and Technology Alliance (RICTA) Ltd
        "sa", // sa Communications, Space and Technology Commission
        "sb", // sb Solomon Telekom Company Limited
        "sc", // sc VCS Pty Ltd
        "sd", // sd Sudan Internet Society
        "se", // se The Internet Infrastructure Foundation
        "sg", // sg Singapore Network Information Centre (SGNIC) Pte Ltd
        "sh", // sh Government of St. Helena
        "si", // si Academic and Research Network of Slovenia (ARNES)
        "sj", // sj Norid A/S
        "sk", // sk SK-NIC, a.s.
        "sl", // sl Sierratel
        "sm", // sm Telecom Italia San Marino S.p.A.
        "sn", // sn Universite Cheikh Anta Diop
        "so", // so Ministry of Post and Telecommunications
        "sr", // sr Telesur
        "ss", // ss National Communication Authority (NCA)
        "st", // st Tecnisys
        "su", // su Russian Institute for Development of Public Networks (ROSNIIROS)
        "sv", // sv SVNet
        "sx", // sx SX Registry SA B.V.
        "sy", // sy National Authority for Information Technology Services (NAITS)
        "sz", // sz University of Swaziland Department of Computer Science
        "tc", // tc Melrex TC
        "td", // td l'Agence de Développement des Technologies de l'Information et de la Communication (ADETIC)
        "tf", // tf Association Française pour le Nommage Internet en Coopération (A.F.N.I.C.)
        "tg", // tg Autorité de Régulation des Communications Electroniques et des Postes (ARCEP)
        "th", // th Thai Network Information Center Foundation
        "tj", // tj Information Technology Center
        "tk", // tk Telecommunication Tokelau Corporation (Teletok)
        "tl", // tl Autoridade Nacional de Comunicações
        "tm", // tm TM Domain Registry Ltd
        "tn", // tn Agence Tunisienne d'Internet
        "to", // to Government of the Kingdom of Tonga, H.R.H. Crown Prince Tupouto'a, c/o Consulate of Tonga
        "tr", // tr Bilgi Teknolojileri ve İletişim Kurumu (BTK)
        "tt", // tt University of the West Indies, Faculty of Engineering
        "tv", // tv Ministry of Transport, Energy, Communications and Innovations
        "tw", // tw Taiwan Network Information Center (TWNIC)
        "tz", // tz Tanzania Communications Regulatory Authority
        "ua", // ua Hostmaster Ltd.
        "ug", // ug Uganda Online Ltd.
        "uk", // uk Nominet UK
        "us", // us Registry Services, LLC
        "uy", // uy SeCIU - Universidad de la Republica
        "uz", // uz Single Integrator for Creation and Support of State Information Systems UZINFOCOM
        "va", // va Holy See - Vatican City State
        "vc", // vc Ministry of Telecommunications, Science, Technology and Industry
        "ve", // ve Comisión Nacional de Telecomunicaciones (CONATEL)
        "vg", // vg Telecommunications Regulatory Commission of the Virgin Islands
        "vi", // vi Virgin Islands Public Telecommunications System, Inc.
        "vn", // vn Viet Nam Internet Network Information Center (VNNIC)
        "vu", // vu Telecommunications Radiocommunications and Broadcasting Regulator (TRBR)
        "wf", // wf Association Française pour le Nommage Internet en Coopération (A.F.N.I.C.)
        "ws", // ws Government of Samoa Ministry of Foreign Affairs & Trade
        "xn--2scrj9c", // ಭಾರತ National Internet eXchange of India
        "xn--3e0b707e", // 한국 KISA (Korea Internet & Security Agency)
        "xn--3hcrj9c", // ଭାରତ National Internet eXchange of India
        "xn--45br5cyl", // ভাৰত National Internet eXchange of India
        "xn--45brj9c", // ভারত National Internet Exchange of India
        "xn--4dbrk0ce", // ישראל The Israel Internet Association (RA)
        "xn--54b7fta0cc", // বাংলা Posts and Telecommunications Division
        "xn--80ao21a", // қаз Association of IT Companies of Kazakhstan
        "xn--90a3ac", // срб Serbian National Internet Domain Registry (RNIDS)
        "xn--90ae", // бг Imena.BG AD
        "xn--90ais", // бел Belarusian Cloud Technologies LLC
        "xn--clchc0ea0b2g2a9gcd", // சிங்கப்பூர் Singapore Network Information Centre (SGNIC) Pte Ltd
        "xn--d1alf", // мкд Macedonian Academic Research Network Skopje
        "xn--e1a4c", // ею EURid vzw
        "xn--fiqs8s", // 中国 China Internet Network Information Center (CNNIC)
        "xn--fiqz9s", // 中國 China Internet Network Information Center (CNNIC)
        "xn--fpcrj9c3d", // భారత్ National Internet Exchange of India
        "xn--fzc2c9e2c", // ලංකා LK Domain Registry
        "xn--gecrj9c", // ભારત National Internet Exchange of India
        "xn--h2breg3eve", // भारतम् National Internet eXchange of India
        "xn--h2brj9c", // भारत National Internet Exchange of India
        "xn--h2brj9c8c", // भारोत National Internet eXchange of India
        "xn--j1amh", // укр Ukrainian Network Information Centre (UANIC), Inc.
        "xn--j6w193g", // 香港 Hong Kong Internet Registration Corporation Ltd.
        "xn--kprw13d", // 台湾 Taiwan Network Information Center (TWNIC)
        "xn--kpry57d", // 台灣 Taiwan Network Information Center (TWNIC)
        "xn--l1acc", // мон Datacom Co.,Ltd
        "xn--lgbbat1ad8j", // الجزائر CERIST
        "xn--mgb9awbf", // عمان Telecommunications Regulatory Authority (TRA)
        "xn--mgba3a4f16a", // ایران Institute for Research in Fundamental Sciences (IPM)
        "xn--mgbaam7a8h", // امارات Telecommunications and Digital Government Regulatory Authority (TDRA)
        "xn--mgbah1a3hjkrd", // موريتانيا Université de Nouakchott Al Aasriya
        "xn--mgbai9azgqp6j", // پاکستان National Telecommunication Corporation
        "xn--mgbayh7gpa", // الاردن Ministry of Digital Economy and Entrepreneurship (MoDEE)
        "xn--mgbbh1a", // بارت National Internet eXchange of India
        "xn--mgbbh1a71e", // بھارت National Internet Exchange of India
        "xn--mgbc0a9azcg", // المغرب Agence Nationale de Réglementation des Télécommunications (ANRT)
        "xn--mgbcpq6gpa1a", // البحرين Telecommunications Regulatory Authority (TRA)
        "xn--mgberp4a5d4ar", // السعودية Communications, Space and Technology Commission
        "xn--mgbgu82a", // ڀارت National Internet eXchange of India
        "xn--mgbpl2fh", // سودان Sudan Internet Society
        "xn--mgbtx2b", // عراق Communications and Media Commission (CMC)
        "xn--mgbx4cd0ab", // مليسيا MYNIC Berhad
        "xn--mix891f", // 澳門 Macao Post and Telecommunications Bureau (CTT)
        "xn--node", // გე Information Technologies Development Center (ITDC)
        "xn--o3cw4h", // ไทย Thai Network Information Center Foundation
        "xn--ogbpf8fl", // سورية National Authority for Information Technology Services (NAITS)
        "xn--p1ai", // рф Coordination Center for TLD RU
        "xn--pgbs0dh", // تونس Agence Tunisienne d'Internet
        "xn--q7ce6a", // ລາວ Lao National Internet Center (LANIC), Ministry of Technology and Communications
        "xn--qxa6a", // ευ EURid vzw
        "xn--qxam", // ελ ICS-FORTH GR
        "xn--rvc1e0am3e", // ഭാരതം National Internet eXchange of India
        "xn--s9brj9c", // ਭਾਰਤ National Internet Exchange of India
        "xn--wgbh1c", // مصر National Telecommunication Regulatory Authority - NTRA
        "xn--wgbl6a", // قطر Communications Regulatory Authority
        "xn--xkc2al3hye2a", // இலங்கை LK Domain Registry
        "xn--xkc2dl3a5ee0h", // இந்தியா National Internet Exchange of India
        "xn--y9a3aq", // հայ "Internet Society" Non-governmental Organization
        "xn--yfro4i67o", // 新加坡 Singapore Network Information Centre (SGNIC) Pte Ltd
        "xn--ygbi2ammx", // فلسطين Ministry of Telecom & Information Technology (MTIT)
        "ye", // ye TeleYemen
        "yt", // yt Association Française pour le Nommage Internet en Coopération (A.F.N.I.C.)
        "za", // za ZA Domain Name Authority
        "zm", // zm Zambia Information and Communications Technology Authority (ZICTA)
        "zw", // zw Postal and Telecommunications Regulatory Authority of Zimbabwe (POTRAZ)
    };

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static final String[] LOCAL_TLDS = {
       "localdomain",         // Also widely used as localhost.localdomain
       "localhost",           // RFC2606 defined
    };

    // Additional arrays to supplement or override the built in ones.
    // The PLUS arrays are valid keys, the MINUS arrays are invalid keys

    /*
     * This field is used to detect whether the getInstance has been called.
     * After this, the method updateTLDOverride is not allowed to be called.
     * This field does not need to be volatile since it is only accessed from
     * synchronized methods.
     */
    private static boolean inUse;

    /*
     * These arrays are mutable, but they don't need to be volatile.
     * They can only be updated by the updateTLDOverride method, and any readers must get an instance
     * using the getInstance methods which are all (now) synchronised.
     */
    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] countryCodeTLDsPlus = EMPTY_STRING_ARRAY;

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] genericTLDsPlus = EMPTY_STRING_ARRAY;

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] countryCodeTLDsMinus = EMPTY_STRING_ARRAY;

    // WARNING: this array MUST be sorted, otherwise it cannot be searched reliably using binary search
    private static volatile String[] genericTLDsMinus = EMPTY_STRING_ARRAY;

    /**
     * enum used by {@link DomainValidator#updateTLDOverride(ArrayType, String[])}
     * to determine which override array to update / fetch
     * @since 1.5.0
     * @since 1.5.1 made public and added read-only array references
     */
    public enum ArrayType {
        /** Update (or get a copy of) the GENERIC_TLDS_PLUS table containing additional generic TLDs */
        GENERIC_PLUS,
        /** Update (or get a copy of) the GENERIC_TLDS_MINUS table containing deleted generic TLDs */
        GENERIC_MINUS,
        /** Update (or get a copy of) the COUNTRY_CODE_TLDS_PLUS table containing additional country code TLDs */
        COUNTRY_CODE_PLUS,
        /** Update (or get a copy of) the COUNTRY_CODE_TLDS_MINUS table containing deleted country code TLDs */
        COUNTRY_CODE_MINUS,
        /** Get a copy of the generic TLDS table */
        GENERIC_RO,
        /** Get a copy of the country code table */
        COUNTRY_CODE_RO,
        /** Get a copy of the infrastructure table */
        INFRASTRUCTURE_RO,
        /** Get a copy of the local table */
        LOCAL_RO
    }

    // For use by unit test code only
    static synchronized void clearTLDOverrides() {
        inUse = false;
        countryCodeTLDsPlus = EMPTY_STRING_ARRAY;
        countryCodeTLDsMinus = EMPTY_STRING_ARRAY;
        genericTLDsPlus = EMPTY_STRING_ARRAY;
        genericTLDsMinus = EMPTY_STRING_ARRAY;
    }

    /**
     * Update one of the TLD override arrays.
     * This must only be done at program startup, before any instances are accessed using getInstance.
     * <p>
     * For example:
     * <p>
     * <code>DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, new String[]{"apache"})}</code>
     * <p>
     * To clear an override array, provide an empty array.
     *
     * @param table the table to update, see {@link DomainValidator.ArrayType}
     * Must be one of the following
     * <ul>
     * <li>COUNTRY_CODE_MINUS</li>
     * <li>COUNTRY_CODE_PLUS</li>
     * <li>GENERIC_MINUS</li>
     * <li>GENERIC_PLUS</li>
     * </ul>
     * @param tlds the array of TLDs, must not be null
     * @throws IllegalStateException if the method is called after getInstance
     * @throws IllegalArgumentException if one of the read-only tables is requested
     * @since 1.5.0
     */
    public static synchronized void updateTLDOverride(ArrayType table, String... tlds) {
        if (inUse) {
            throw new IllegalStateException("Can only invoke this method before calling getInstance");
        }
        // Comparisons are always done with lower-case entries
        String[] copy = Arrays.stream(tlds)
                .map(tld -> tld.toLowerCase(Locale.ENGLISH))
                .toArray(String[]::new);
        Arrays.sort(copy);
        switch (table) {
        case COUNTRY_CODE_MINUS:
            countryCodeTLDsMinus = copy;
            break;
        case COUNTRY_CODE_PLUS:
            countryCodeTLDsPlus = copy;
            break;
        case GENERIC_MINUS:
            genericTLDsMinus = copy;
            break;
        case GENERIC_PLUS:
            genericTLDsPlus = copy;
            break;
        case COUNTRY_CODE_RO:
        case GENERIC_RO:
        case INFRASTRUCTURE_RO:
        case LOCAL_RO:
            throw new IllegalArgumentException("Cannot update the table: " + table);
        }
    }

    /**
     * Get a copy of the internal array.
     * @param table the array type (any of the enum values)
     * @return a copy of the array
     * @throws IllegalArgumentException if the table type is unexpected (should not happen)
     * @since 1.5.1
     */
    public static String[] getTLDEntries(ArrayType table) {
        String[] array = null;
        switch (table) {
        case COUNTRY_CODE_MINUS:
            array = countryCodeTLDsMinus;
            break;
        case COUNTRY_CODE_PLUS:
            array = countryCodeTLDsPlus;
            break;
        case GENERIC_MINUS:
            array = genericTLDsMinus;
            break;
        case GENERIC_PLUS:
            array = genericTLDsPlus;
            break;
        case GENERIC_RO:
            array = GENERIC_TLDS;
            break;
        case COUNTRY_CODE_RO:
            array = COUNTRY_CODE_TLDS;
            break;
        case INFRASTRUCTURE_RO:
            array = INFRASTRUCTURE_TLDS;
            break;
        case LOCAL_RO:
            array = LOCAL_TLDS;
            break;
        }
        if (array == null) {
            throw new IllegalArgumentException("Unexpected enum value: " + table);
        }
        return Arrays.copyOf(array, array.length); // clone the array
    }

    /**
     * Converts potentially Unicode input to punycode.
     * If conversion fails, returns the original input.
     *
     * @param input the string to convert, not null
     * @return converted input, or original input if conversion fails
     */
    // Needed by UrlValidator
    public static String unicodeToASCII(String input) {
        if (isOnlyASCII(input)) { // skip possibly expensive processing
            return input;
        }
        try {
            final String ascii = IDN.toASCII(input);
            if (IdnBugHolder.IDN_TOASCII_PRESERVES_TRAILING_DOTS) {
                return ascii;
            }
            final int length = input.length();
            if (length == 0) { // check there is a last character
                return input;
            }
            // RFC3490 3.1. 1)
            //            Whenever dots are used as label separators, the following
            //            characters MUST be recognized as dots: U+002E (full stop), U+3002
            //            (ideographic full stop), U+FF0E (fullwidth full stop), U+FF61
            //            (halfwidth ideographic full stop).
            char lastChar = input.charAt(length-1); // fetch original last char
            switch (lastChar) {
                case '.':      // "." full stop, AKA U+002E
                case '\u3002': // ideographic full stop
                case '\uFF0E': // fullwidth full stop
                case '\uFF61': // halfwidth ideographic full stop
                    return ascii + '.'; // restore the missing stop
                default:
                    return ascii;
            }
        } catch (IllegalArgumentException e) { // input is not valid
            Logging.trace(e);
            return input;
        }
    }

    private static final class IdnBugHolder {
        private static boolean keepsTrailingDot() {
            final String input = "a."; // must be a valid name
            return input.equals(IDN.toASCII(input));
        }

        private static final boolean IDN_TOASCII_PRESERVES_TRAILING_DOTS = keepsTrailingDot();
    }

    /*
     * Check if input contains only ASCII
     * Treats null as all ASCII
     */
    private static boolean isOnlyASCII(String input) {
        if (input == null) {
            return true;
        }
        return IntStream.range(0, input.length()).noneMatch(i -> input.charAt(i) > 0x7F); // CHECKSTYLE IGNORE MagicNumber
    }

    /**
     * Check if a sorted array contains the specified key
     *
     * @param sortedArray the array to search
     * @param key the key to find
     * @return {@code true} if the array contains the key
     */
    private static boolean arrayContains(String[] sortedArray, String key) {
        return Arrays.binarySearch(sortedArray, key) >= 0;
    }
}
