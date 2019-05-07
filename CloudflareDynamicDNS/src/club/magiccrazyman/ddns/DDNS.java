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
package club.magiccrazyman.ddns;

import club.magiccrazyman.ddns.Configuration.Account.Domain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
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

    private final Configuration CONFIG;
    private final HashMap<String, Thread> UPDATE_THREADS = new HashMap<>();
    private final HashMap<String, Runnable> UPDATE_RUNNABLE = new HashMap<>();

    /**
     * Create a new <code>DDNS</code> instance by given parameters
     *
     * @param config An Configuration instance
     */
    public DDNS(Configuration config) {
        CONFIG = config;
    }

    /**
     * Start service
     */
    public void startDDNS() {
        if (CONFIG != null) {
            LOGGER_DDNS.info("DDNS 正在初始化...");
            if (CONFIG.isBaidu) {
                LOGGER_DDNS.info("检测到你正在使用百度获取网络IP地址，建议使用可信任的主机自行搭建IP查询网页");
            }
            this.CONFIG.accounts.forEach((a) -> {
                a.domains.forEach((d) -> {
                    //启动域名更新线程
                    Runnable r = new UpdateRunnable(a.email, a.key, d);
                    Thread t = new Thread(r);
                    UPDATE_THREADS.put(d.nickname, t);
                    UPDATE_RUNNABLE.put(d.nickname, r);
                    t.setName(d.nickname);
                    t.start();
                });
            });
            //启动控制台命令线程
            Thread t = new Thread(new CommandRunnable());
            t.setName("Command");
            t.start();
        }
    }

    class CommandRunnable implements Runnable {

        private final HashMap<String, String> commands = new HashMap<>();

        public CommandRunnable() {
            commands.put("help", "查看所有命令");
            commands.put("list", "查询所有正在运行的域名更新线程");
            commands.put("reload", "重新读取配置文件并重启");
            commands.put("update", "<域名昵称> 强制更新域名");
            commands.put("stop", "强制关闭程序");
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                listen();
            }
        }

        private void listen() {
            Scanner scanner = new Scanner(System.in);
            String[] input = scanner.nextLine().trim().replaceAll("\\s+", " ").split(" ");
            String command = input[0];
            String[] args = Arrays.copyOfRange(input, 1, input.length);

            if (!command.equals("")) {
                LOGGER_DDNS.info(String.format("检测到命令：%s", command));
                switch (command) {
                    case "help":
                        helpCommand();
                        break;
                    case "stop":
                        stopCommand();
                        break;
                    case "list":
                        listCommand();
                        break;
                    case "reload":
                        reloadCommand();
                        break;
                    case "update":
                        updateCommand(args);
                        break;
                    default:
                        LOGGER_DDNS.info(String.format("未知命令 %s ,输入 help 以查看更多命令", command));
                        break;
                }
            }
        }

        private void helpCommand() {
            StringBuilder sb = new StringBuilder();
            ArrayList<String> keys = new ArrayList(commands.keySet());
            for (int i = 0; i < keys.size(); i++) {
                sb.append(String.format("%-8s %s", keys.get(i), commands.get(keys.get(i))));
                if (i < keys.size() - 1) {
                    sb.append(System.lineSeparator());
                }
            }
            System.out.println(sb);
        }

        private void stopCommand() {

            UPDATE_THREADS.values().forEach((t) -> {
                LOGGER_DDNS.info(String.format("正在中止线程 %s ...", t.getName()));
                t.interrupt();
            });
            LOGGER_DDNS.info("程序中止");
            System.exit(0);
        }

        private void listCommand() {

            StringBuilder str = new StringBuilder("以下线程正在运行：");
            UPDATE_THREADS.keySet().forEach((k) -> {
                str.append(k).append(" ");
            });
            LOGGER_DDNS.info(str);
        }

        private void reloadCommand() {

            LOGGER_DDNS.info("正在重新读取配置并重启...");
            UPDATE_THREADS.values().forEach((t) -> {
                t.interrupt();
            });
            Main.main(Main.inputArgs);
            Thread.currentThread().interrupt();
        }

        private void updateCommand(String[] targetAll) {

            for (String target : targetAll) {
                if (UPDATE_RUNNABLE.containsKey(target)) {
                    LOGGER_DDNS.info(String.format("正在强制更新域名线程 %s", target));
                    Thread t = new Thread(() -> {
                        ((UpdateRunnable) UPDATE_RUNNABLE.get(target)).updateDNS();
                    });
                    t.setName(String.format("temp_%s", target));
                    t.start();
                } else {
                    LOGGER_DDNS.error(String.format("无效线程 %s", target));
                }
            }
        }
    }

    //A domain update thread
    class UpdateRunnable implements Runnable {

        private final Domain DOMAIN;
        private final HashMap<String, String> HEADERS = new HashMap<>();

        public UpdateRunnable(String email, String key, Domain domain) {
            this.DOMAIN = domain;

            this.HEADERS.put("X-Auth-Email", email);
            this.HEADERS.put("X-Auth-Key", key);
            this.HEADERS.put("Content-Type", "application/json");
        }

        @Override
        public void run() {
            LOGGER_DDNS.info(String.format("域名 %s 的DDNS服务已启动", DOMAIN.domain));

            while (!Thread.currentThread().isInterrupted()) {
                updateDNS();
            }
        }

        private void updateDNS() {
            try {
                String newIP = getLocalIP();
                String oldIP = getDomainIP();

                if (newIP != null && oldIP != null) {
                    if (!newIP.equals(oldIP)) {
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
                            LOGGER_DDNS.info(String.format("域名 %s 已更新为 %s ,将睡眠 %s 秒", DOMAIN.domain, newIP, DOMAIN.ttl == 1 ? CONFIG.defaultSleepSconds : DOMAIN.ttl * 4));
                            sleep(DOMAIN.ttl == 1 ? CONFIG.defaultSleepSconds * 1000 : DOMAIN.ttl * 4 * 1000);
                        } else {
                            LOGGER_DDNS.error(String.format("域名 %s 更新失败,将睡眠 %s 秒后重试" + System.lineSeparator()
                                    + "失败原因：" + System.lineSeparator()
                                    + formatErrorMessages(responseJson.errors),
                                    DOMAIN.domain, CONFIG.failedSleepSeconds));
                            sleep(CONFIG.failedSleepSeconds);
                        }
                    } else {
                        LOGGER_DDNS.info(String.format("域名 %s 未发生变化, 将睡眠 %s 秒", DOMAIN.domain, CONFIG.defaultSleepSconds));
                        sleep(CONFIG.defaultSleepSconds * 1000);
                    }
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
                } catch (NullPointerException | IOException ex) {
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

        private void sleep(long time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
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
