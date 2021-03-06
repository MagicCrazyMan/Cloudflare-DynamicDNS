/*
 * Copyright (C) 2019 Magic Crazy Man
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package club.magiccrazyman.ddns.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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
public class Configuration {

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
     * inits the configurations and create an Configuration instance
     *
     * @param configLocation path of the config file
     * @param isBaidu whether use baidu for getting IP or not
     * @return if config exists, return an Configuration instance, otherwise
     * return null and create a template.json file at current folder
     */
    public static Configuration initConfiguration(String configLocation, boolean isBaidu) {
        File configFile = new File(configLocation);

        if (configFile.exists()) {
            return readConfiguration(configFile, isBaidu);
        } else {
            createTemplate();
            return null;
        }
    }

    private static Configuration readConfiguration(File configFile, boolean isBaidu) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        Configuration configJson = null;

        FileReader fr;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            switch (os) {
                case "linux":
                    Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(configFile.toPath(), LinkOption.NOFOLLOW_LINKS);
                    for (PosixFilePermission s : permissions) {
                        if (Configuration.INVALID_PERMISSIONS.contains(s)) {
                            LOGGER_EX.error("配置文件权限不正确！请确认配置文件权限为600");
                            System.exit(0);
                        }
                    }
                    break;
            }
            fr = new FileReader(configFile);
            configJson = gson.fromJson(fr, Configuration.class);
            configJson.isBaidu = isBaidu;

            changeLoggerHome(configJson, (LoggerContext) LogManager.getContext());
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(DDNS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DDNS.class.getName()).log(Level.SEVERE, null, ex);
        }
        return configJson;
    }

    private static void changeLoggerHome(Configuration config, LoggerContext ctx) {

        try {
            InputStream is = Configuration.class.getResourceAsStream("/log4j2.xml");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(is));
            doc.getElementsByTagName("property").item(0).setTextContent(config.logFileHome);

            for (int i = 0; i < doc.getElementsByTagName("RollingFile").getLength(); i++) {
                Node node = doc.getElementsByTagName("RollingFile").item(i);
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
            java.util.logging.Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void createTemplate() {
        Gson gson = new GsonBuilder().serializeNulls().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        Configuration templateJson = new Configuration();
        templateJson.whereGetYourIP = "url";
        templateJson.logFileHome = ".";
        templateJson.defaultSleepSconds = 900;
        templateJson.failedSleepSeconds = 300;

        Account A1 = new Account();
        A1.email = "example1@example1.com";
        A1.key = "Cloudflare SecretKey";
        Account.Domain domain1 = new Account.Domain();
        domain1.passiveUpdate = false;
        domain1.nickname = "nickname 1";
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
        domain2.passiveUpdate = false;
        domain2.nickname = "nickname 2";
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
                        Files.setPosixFilePermissions(f.toPath(), Configuration.VALID_PERMISSIONS);
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

    public boolean isBaidu;
    /**
     * An URL where can get your Intenet IP Address
     * <p>
     * This URL should return a json string like: {"ip":"0.0.0.0"}
     *
     */
    @Expose
    public String whereGetYourIP;

    @Expose
    public boolean enablePassiveUpdateModule;

    @Expose
    public int passiveUpdatePort;
    /**
     * path of the log files
     * <p>
     * log files saves in current folder in default
     */
    @Expose
    public String logFileHome;

    /**
     * Sleep time in seconds after checking a DNS record
     * <p>
     * When a DNS record's TTl value is 1, this may work as sleep time
     */
    @Expose
    public int defaultSleepSconds;

    /**
     * Sleep time in seconds when failed to check a DNS record or connect to
     * remote server
     */
    @Expose
    public int failedSleepSeconds;

    /**
     * Contains all different Cloudflare accounts
     * <p>
     * Each account has different Email and Global API Key
     */
    @Expose
    public ArrayList<Account> accounts = new ArrayList<>();

    /**
     * An Cloudflare account
     */
    public static class Account {

        /**
         * Cloudflare account's email
         */
        @Expose
        public String email;

        /**
         * Account's Global API Key
         */
        @Expose
        public String key;

        /**
         * Catains all different domains of an account
         * <p>
         * Each domain will be kept checking in ONE thread
         */
        @Expose
        public ArrayList<Domain> domains = new ArrayList<>();

        /**
         * A domain of an account
         */
        public static class Domain {

            /**
             * Defines thread name
             */
            @Expose
            public String nickname;

            @Expose
            public boolean passiveUpdate;

            @Expose
            public String passiveUpdateID;
            /**
             * Zone ID of root domain
             */
            @Expose
            public String zone;

            /**
             * Identifier of this domain
             */
            @Expose
            public String identifier;

            /**
             * Record type of this domain
             */
            @Expose
            public String type;

            /**
             * Record TTL of this domain
             */
            @Expose
            public int ttl;

            /**
             * whether or not enable the Cloudflare CDN proxy
             */
            @Expose
            public boolean proixed;
        }
    }
}
