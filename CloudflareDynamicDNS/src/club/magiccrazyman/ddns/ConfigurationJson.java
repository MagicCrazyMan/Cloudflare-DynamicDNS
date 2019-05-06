package club.magiccrazyman.ddns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This is a Json-Like Class for Gson
 *
 * @author Magic Crazy Man
 */
public class ConfigurationJson {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    /**
     * Contains all valid dns ttl values which can define in Cloudflare
     */
    public static final ArrayList<Integer> VALID_TTL = new ArrayList<>();

    /**
     * Contains all valid dns type values which can define in Cloudflare
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
     * inits the configurations and create an ConfigurationJson instance
     *
     * @param config path of the config file
     * @param isBaidu whether use baidu for getting IP or not
     * @return if config exists, return an ConfigurationJson instance, otherwise
     * return null and create a template.json file
     */
    public static ConfigurationJson initConfiguration(String config, boolean isBaidu) {
        File configFile = new File(config);

        if (configFile.exists()) {
            return readConfiguraation(configFile, isBaidu);
        } else {
            createTemplate();
            return null;
        }
    }

    private static ConfigurationJson readConfiguraation(File configFile, boolean isBaidu) {
        Gson gson = new GsonBuilder().create();
        ConfigurationJson configJson = null;

        FileReader fr;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            switch (os) {
                case "linux":
                    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(configFile.toPath(), LinkOption.NOFOLLOW_LINKS);
                    for (PosixFilePermission s : permissions) {
                        if (ConfigurationJson.INVALID_PERMISSIONS.contains(s)) {
                            LOGGER_EX.error("配置文件权限不正确！请确认配置文件权限为600");
                            System.exit(0);
                        }
                    }
                    break;
            }
            fr = new FileReader(configFile);
            configJson = gson.fromJson(fr, ConfigurationJson.class);
            configJson.isBaidu = isBaidu;

            changeLoggerHome(configJson, (LoggerContext) LogManager.getContext());

        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(DDNS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DDNS.class.getName()).log(Level.SEVERE, null, ex);
        }
        return configJson;
    }

    private static void changeLoggerHome(ConfigurationJson config, LoggerContext ctx) {

        try {
            InputStream is = ConfigurationJson.class.getResourceAsStream("/log4j2.xml");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(is));
            doc.getElementsByTagName("property").item(0).setTextContent(config.logFileHome);
            
            for (int i = 0; i < doc.getElementsByTagName("RollingFile").getLength(); i++) {
                Node node =doc.getElementsByTagName("RollingFile").item(i);
                node.getAttributes().removeNamedItem("createOnDemand");
                doc.renameNode(node, null, "RollingRandomAccessFile");
            }

            StringWriter writer = new StringWriter();
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.transform(new DOMSource(doc), new StreamResult(writer));
            ByteArrayInputStream bais = new ByteArrayInputStream(writer.toString().getBytes());

            XmlConfiguration configuration = new XmlConfiguration(ctx, new ConfigurationSource(bais));
            ctx.setConfiguration(configuration);
        } catch (IOException | TransformerException | SAXException | ParserConfigurationException ex) {
            java.util.logging.Logger.getLogger(ConfigurationJson.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void createTemplate() {
        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        ConfigurationJson templateJson = new ConfigurationJson();
        templateJson.whereGetYourIP = "url";
        templateJson.logFileHome = ".";
        templateJson.defaultSleepSconds = 900;
        templateJson.failedSleepSeconds = 300;

        Account A1 = new Account();
        A1.email = "example1@example1.com";
        A1.key = "Cloudflare SecretKey";
        Account.Domain domain1 = new Account.Domain();
        domain1.nickname = "nickname 1";
        domain1.domain = "example1.example1.example1";
        domain1.zone = "Cloudflare DNS Zone";
        domain1.identifier = "Cloudflare DNS domain identifier";
        domain1.proixed = false;
        domain1.ttl = 1;
        domain1.type = "A";
        A1.domains.add(domain1);

        Account A2 = new Account();
        A2.email = "example2@example2.com";
        A2.key = "Cloudflare SecretKey";
        Account.Domain domain2 = new Account.Domain();
        domain2.nickname = "nickname 2";
        domain2.domain = "example2.example2.example2";
        domain2.zone = "Cloudflare DNS Zone";
        domain2.identifier = "Cloudflare DNS domain identifier";
        domain2.proixed = false;
        domain2.ttl = 1;
        domain2.type = "A";
        A2.domains.add(domain1);
        A2.domains.add(domain2);

        ArrayList<Account> accounts = new ArrayList<>();
        accounts.add(A1);
        accounts.add(A2);
        templateJson.accounts = accounts;
        String json = gson.toJson(templateJson);
        try {
            File f = new File("template.json");
            f.createNewFile();
            try (FileWriter fw = new FileWriter(f)) {
                String os = System.getProperty("os.name").toLowerCase();
                switch (os) {
                    case "linux":
                        Files.setPosixFilePermissions(f.toPath(), ConfigurationJson.VALID_PERMISSIONS);
                        break;
                }
                fw.write(json);
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DDNS.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOGGER_EX.error("未找到配置文件，已在当前目录创建模板配置文件 template.json");
        LOGGER_EX.error("可以使用自带的工具创建配置文件");
    }

    @Expose
    boolean isBaidu;
    /**
     * An URL where can get your Intenet IP Address
     * <p>
     * This URL should return a json string like: {"ip":"0.0.0.0"}
     *
     */
    public String whereGetYourIP;

    /**
     * path of the log files
     * <p>
     * log files saves in current folder in default
     */
    public String logFileHome;

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
