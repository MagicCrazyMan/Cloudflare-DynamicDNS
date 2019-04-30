package club.magiccrazyman.ddns;

import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This is a Json-Like Class for Gson
 *
 * @author Magic Crazy Man
 */
public class ConfigurationJson {

    /**
     * Contains all valid dns ttl values which can define in
     * Cloudflare
     */
    public static final ArrayList<Integer> VALID_TTL = new ArrayList<>();

    /**
     * Contains all valid dns type values which can define in
     * Cloudflare
     */
    public static final ArrayList<String> VALID_TYPE = new ArrayList<>();

    /**
     * Cotains valid file permissions
     * <p>
     * Only work on Linux OS
     */
    public static final HashSet<PosixFilePermission> VALID_PERMISSIONS = new HashSet<>();

    /**
     * Contains all invalid file permissions
     * <p>
     * Only work on Linux OS
     */
    public static final HashSet<PosixFilePermission> INVALID_PERMISSIONS = new HashSet<>();

    static {
        VALID_TTL.add(300);
        VALID_TTL.add(900);
        VALID_TTL.add(1800);
        VALID_TTL.add(1800);
        VALID_TTL.add(2700);
        VALID_TTL.add(3600);
        VALID_TTL.add(7200);
        VALID_TTL.add(10800);
        VALID_TTL.add(14400);
        VALID_TTL.add(28800);
        VALID_TTL.add(57600);
        VALID_TTL.add(86400);
        VALID_TTL.add(604800);
        VALID_TTL.add(2592000);
        VALID_TTL.add(31536000);
        VALID_TTL.add(1);

        VALID_TYPE.add("A");
        VALID_TYPE.add("AAAA");
        VALID_TYPE.add("CNAME");
        VALID_TYPE.add("TXT");
        VALID_TYPE.add("SRV");
        VALID_TYPE.add("LOC");
        VALID_TYPE.add("MX");
        VALID_TYPE.add("NS");
        VALID_TYPE.add("SPF");
        VALID_TYPE.add("CERT");
        VALID_TYPE.add("DNSKEY");
        VALID_TYPE.add("DS");
        VALID_TYPE.add("NAPTR");
        VALID_TYPE.add("SMIMEA");
        VALID_TYPE.add("SSHFP");
        VALID_TYPE.add("TLSA");
        VALID_TYPE.add("URI");

        VALID_PERMISSIONS.add(PosixFilePermission.OWNER_READ);
        VALID_PERMISSIONS.add(PosixFilePermission.OWNER_WRITE);

        INVALID_PERMISSIONS.add(PosixFilePermission.OWNER_EXECUTE);
        INVALID_PERMISSIONS.add(PosixFilePermission.OTHERS_WRITE);
        INVALID_PERMISSIONS.add(PosixFilePermission.OTHERS_READ);
        INVALID_PERMISSIONS.add(PosixFilePermission.OTHERS_EXECUTE);
        INVALID_PERMISSIONS.add(PosixFilePermission.GROUP_WRITE);
        INVALID_PERMISSIONS.add(PosixFilePermission.GROUP_READ);
        INVALID_PERMISSIONS.add(PosixFilePermission.GROUP_EXECUTE);
    }

    /**
     * An URL where can get your Intenet IP Address
     * <p>
     * This URL should return a json string like: {"ip":"0.0.0.0"}
     *
     */
    public String whereGetYourIP;

    /**
     * Sleep time in seconds after checking a DNS record
     * <p>
     * When a DNS record's TTl value is 1, this may work as sleep time
     */
    public int defaultSleepSconds;

    /**
     * Sleep time in seconds when failed to check a DNS record or connect to
     * remote server
     */
    public int failedSleepSeconds;

    /**
     * Contains all different Cloudflare accounts
     * <p>
     * Each account has different Email and Global API Key
     */
    public ArrayList<Account> accounts = new ArrayList<>();

    /**
     * An Cloudflare account 
     */
    public static class Account {

        /**
         * Cloudflare account's email
         */
        public String email;

        /**
         * Account's Global API Key
         */
        public String key;

        /**
         * Catains all different domains of an account
         * <p>
         * Each domain will be kept checking in ONE thread
         */
        public ArrayList<Domain> domains = new ArrayList<>();

        /**
         * A domain of an account
         */
        public static class Domain {

            /**
             * Defines thread name
             */
            public String nickname;

            /**
             * Full domain 
             */
            public String domain;

            /**
             * Zone ID of root domain
             */
            public String zone;

            /**
             * Identifier of this domain
             */
            public String identifier;

            /**
             * Record type of this domain 
             */
            public String type;

            /**
             * Record TTL of this domain
             */
            public int ttl;

            /**
             * whether or not enable the Cloudflare CDN proxy
             */
            public boolean proixed;
        }
    }
}
