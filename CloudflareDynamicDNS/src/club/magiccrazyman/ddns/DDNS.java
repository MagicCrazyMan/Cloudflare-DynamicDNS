package club.magiccrazyman.ddns;

import club.magiccrazyman.ddns.ConfigurationJson.Account.Domain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;

/**
 *
 * @author Magic Crazy Man
 */
public class DDNS {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private final ConfigurationJson CONFIG;

    /**
     * Create a new <code>DDNS</code> instance by given parameters
     *
     * @param config An ConfigurationJson instance
     */
    public DDNS(ConfigurationJson config) {
        CONFIG = config;
    }

    /**
     * Start service
     */
    public void startDDNS() {
        if (CONFIG != null) {
            LOGGER_DDNS.info("DDNS 正在初始化");
            if (CONFIG.isBaidu) {
                LOGGER_DDNS.info("检测到你正在使用百度获取网络IP地址，建议使用可信任的主机自行搭建IP查询网页");
            }
            this.CONFIG.accounts.forEach((a) -> {
                a.domains.forEach((d) -> {
                    Thread t = new Thread(new UpdateThread(a.email, a.key, d));
                    t.setName(d.nickname);
                    t.start();
                    LOGGER_DDNS.info(String.format("域名 %s 的DDNS服务已启动", d.domain));
                });
            });
        }
    }

    //A domain update thread
    class UpdateThread implements Runnable {

        private final Domain DOMAIN;
        private final HashMap<String, String> HEADERS = new HashMap<>();

        public UpdateThread(String email, String key, Domain domain) {
            this.DOMAIN = domain;

            this.HEADERS.put("X-Auth-Email", email);
            this.HEADERS.put("X-Auth-Key", key);
            this.HEADERS.put("Content-Type", "application/json");
        }

        @Override
        public void run() {
            while (true) {
                updateDNS();
            }
        }

        private void updateDNS() {
            try {
                String newIP = getLocalIP();
                String oldIP = getDomainIP();

                if (newIP != null && oldIP != null && !newIP.equals(oldIP)) {
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    UpdateJson updateJson = new UpdateJson();
                    updateJson.type = DOMAIN.type;
                    updateJson.name = DOMAIN.domain;
                    updateJson.content = newIP;
                    updateJson.ttl = DOMAIN.ttl;
                    updateJson.proxied = DOMAIN.proixed;

                    String url = String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s", DOMAIN.zone, DOMAIN.identifier);
                    HttpConnection conn2 = (HttpConnection) Jsoup.connect(url);
                    conn2.headers(HEADERS);
                    conn2.requestBody(gson.toJson(updateJson));
                    conn2.ignoreContentType(true);
                    conn2.method(Connection.Method.PUT);
                    CloudflareResponseJson responseJson = gson.fromJson(conn2.execute().body(), CloudflareResponseJson.class);

                    if (responseJson.success) {
                        LOGGER_DDNS.info(String.format("域名 %s 已更新为 %s ,将睡眠 %s 秒以等待上游DNS服务器更新", DOMAIN.domain, newIP, DOMAIN.ttl == 1 ? CONFIG.defaultSleepSconds : DOMAIN.ttl * 4));
                        sleep(DOMAIN.ttl == 1 ? CONFIG.defaultSleepSconds * 1000 : DOMAIN.ttl * 4 * 1000);
                    } else {
                        LOGGER_DDNS.error(String.format("域名 %s 更新失败,将睡眠 %s 秒后重试" + System.lineSeparator()
                                + "失败原因：" + System.lineSeparator()
                                + formatErrorMessages(responseJson.errors),
                                DOMAIN.domain, CONFIG.failedSleepSeconds));
                        sleep(CONFIG.failedSleepSeconds);
                    }
                } else {
                    LOGGER_DDNS.info(String.format("域名 %s 未发生变化, 将睡眠 %s 秒后进行下次检测", DOMAIN.domain, CONFIG.defaultSleepSconds));
                    sleep(CONFIG.defaultSleepSconds * 1000);
                }
            } catch (IOException ex) {
                LOGGER_DDNS.error(String.format("进程发生致命故障，但未中断，将在 %s 秒后重试", CONFIG.failedSleepSeconds));
                LOGGER_EX.error(ex);
                sleep(CONFIG.failedSleepSeconds * 1000);
            }
        }

        private String getLocalIP() {
            if (CONFIG.isBaidu) {
                try {
                    HttpConnection conn = (HttpConnection) Jsoup.connect("https://www.baidu.com/s?wd=ip");
                    Document doc = conn.execute().parse();
                    String ip = doc.getElementById("1").attr("fk");
                    return ip;
                } catch (IOException ex) {
                    LOGGER_DDNS.error(String.format("无法连接至百度，将在 %s 秒后重试", CONFIG.failedSleepSeconds));
                    LOGGER_EX.error(ex);
                    sleep(CONFIG.failedSleepSeconds * 1000);
                }
            } else {
                try {
                    HttpConnection conn = (HttpConnection) Jsoup.connect(CONFIG.whereGetYourIP);
                    conn.ignoreContentType(true);
                    Gson gson = new GsonBuilder()
                            .disableHtmlEscaping()
                            .setPrettyPrinting()
                            .create();
                    LocalAddrJson addr = gson.fromJson(conn.execute().body(), LocalAddrJson.class);
                    return addr.ip;
                } catch (IOException ex) {
                    LOGGER_DDNS.error(String.format("远程服务器故障，将在 %s 秒后重试", CONFIG.failedSleepSeconds));
                    LOGGER_EX.error(ex);
                    sleep(CONFIG.failedSleepSeconds * 1000);
                } catch (IllegalArgumentException ex) {
                    LOGGER_DDNS.fatal("请确定输入的URL正确，进程已中止");
                    LOGGER_EX.fatal(ex);
                    System.exit(1);
                }
            }
            return null;
        }

        //ping the ip address of this domain
        private String getDomainIP() {
            try {
                String command = String.format("ping %s -c 1", DOMAIN.domain);
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader bis = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String str = bis.readLine().split(" ")[2];
                String ip = str.substring(1, str.length() - 1);
                return ip;
            } catch (IOException ex) {
                LOGGER_DDNS.error(String.format("获取原有IP失败，将在 %s 秒后重试", CONFIG.failedSleepSeconds));
                LOGGER_EX.error(ex);
                sleep(CONFIG.failedSleepSeconds * 1000);
            }
            return null;
        }

        private void sleep(int time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                LOGGER_DDNS.error("进程发生致命故障,进程已终止");
                LOGGER_EX.error(ex);
                System.exit(1);
            }
        }

        //Print formatter of the Cloudflare error messages
        private String formatErrorMessages(String[] errors) {
            int count = 0;
            StringBuilder builder = new StringBuilder();
            for (String e : errors) {
                builder.append(String.format("%s.%s", count, e)).append(System.lineSeparator());
                count++;
            }
            return builder.toString();
        }

        class UpdateJson {

            String type;
            String name;
            String content;
            int ttl;
            boolean proxied;
        }

        class CloudflareResponseJson {

            boolean success;
            String[] errors;
            String[] messages;
            Result result;

            class Result {

                String id;
                String type;
                String name;
                String content;
                String proxiable;
                boolean proxied;
                int ttl;
                boolean locked;
                String zone_id;
                String zone_name;
                String created_on;
                String modified_on;
                Meta meta;

                class Meta {

                    boolean auto_added;
                    boolean managed_by_apps;
                    boolean managed_by_argo_tunnel;
                }
            }
        }

        class LocalAddrJson {

            String ip;
        }
    }
}
