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

import club.magiccrazyman.ddns.components.ComponentAbstract;
import club.magiccrazyman.ddns.core.Configuration.Account.Domain;
import club.magiccrazyman.ddns.components.command.Command;
import club.magiccrazyman.ddns.components.passiveupdate.PassiveUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.reflections.Reflections;

/**
 *
 * @author Magic Crazy Man
 */
public class DDNS {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private final Configuration CONFIG;
    private final HashMap<String, Thread> UPDATE_THREADS = new HashMap<>();
    private final HashMap<String, ComponentAbstract> COMPONENTS = new HashMap<>();

    private boolean isInit = false;

    /**
     * Create a new <code>DDNS</code> instance by given parameters
     *
     * @param config An Configuration instance
     */
    public DDNS(Configuration config) {
        CONFIG = config;

        initInternalComponents();
        initExternalComponents();
    }

    /**
     * Start service
     */
    public void startDDNS() {
        if (CONFIG != null && isInit) {
            //启动组件
            startComponents();
            startUpdateThreads();
        }
    }

    private void startUpdateThreads() {
        LOGGER_DDNS.info("DDNS 正在初始化...");
        this.CONFIG.accounts.forEach((a) -> a.domains.forEach((d) -> {
            //启动域名更新线程
            Runnable r = new UpdateRunnable(a.email, a.key, d);
            Thread t = new Thread(r);
            UPDATE_THREADS.put(d.nickname, t);
            t.setName(d.nickname);
            t.start();
        }));
    }

    //初始化内置组件
    private void initInternalComponents() {
        Reflections reflections = new Reflections("club.magiccrazyman.ddns.components"); //使用反射搜索组件
        Set<Class<? extends ComponentAbstract>> clazz = reflections.getSubTypesOf(ComponentAbstract.class);
        ArrayList<Class<? extends ComponentAbstract>> arrs = new ArrayList<>(clazz);
        arrs.forEach((arr) -> {
            try {
                ComponentAbstract component =  arr.newInstance();
                COMPONENTS.put(component.name(), component);
            } catch (InstantiationException | IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(Command.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        COMPONENTS.values().forEach((component) -> component.register(this));
        isInit = true;
    }

    //初始化外部组件
    private void initExternalComponents() {

    }

    private void startComponents() {

        COMPONENTS.values().forEach((component) -> {
            Thread t = new Thread(component);
            t.setName(component.name());
            t.start();
        });
    }

    //A domain update thread
    public class UpdateRunnable implements Runnable {

        private final Domain DOMAIN;
        private final String DOMAIN_NAME;
        private final HashMap<String, String> HEADERS = new HashMap<>();
        private final Object LOCK;

        private int defaultSleepSconds = 0;
        private int failedSleepSeconds = 0;
        private String domainIP = null;
        private String localIP = null;
        private String sourceType = null;

        public UpdateRunnable(String email, String key, Domain domain) {
            this.DOMAIN = domain;

            this.HEADERS.put("X-Auth-Email", email);
            this.HEADERS.put("X-Auth-Key", key);
            this.HEADERS.put("Content-Type", "application/json");

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            HttpConnection conn = (HttpConnection) Jsoup.connect(String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s", DOMAIN.zone, DOMAIN.identifier));
            conn.headers(HEADERS);
            conn.ignoreContentType(true);
            conn.method(Connection.Method.GET);

            CloudflareResponseJson json = null;
            try {
                json = gson.fromJson(conn.execute().body(), CloudflareResponseJson.class);
            } catch (IOException ex) {
                LOGGER_DDNS.error("无法从Cloudflare获取信息,请确认输入信息正确并且网络已连接，应用已关闭");
                LOGGER_EX.error("发生错误",ex);
                System.exit(1);
            }
            DOMAIN_NAME = json.result.name;
            domainIP = json.result.content;

            setSourceType();

            failedSleepSeconds = CONFIG.failedSleepSeconds;
            if (DOMAIN.passiveUpdate) {
                PassiveUpdate com = (PassiveUpdate) COMPONENTS.get("passiveUpdate");
                LOCK = com.getLOCK(DOMAIN.passiveUpdateID);
            } else {
                defaultSleepSconds = CONFIG.defaultSleepSconds;
                LOCK = null;
            }
        }

        @Override
        public void run() {
            LOGGER_DDNS.info(String.format("域名 %s 的DDNS服务已启动", DOMAIN_NAME));

            while (!Thread.currentThread().isInterrupted()) {
                if (DOMAIN.passiveUpdate) {
                    LOGGER_DDNS.info("等待被动更新");
                    synchronized (LOCK) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException ex) {
                            java.util.logging.Logger.getLogger(DDNS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                updateDNS();
            }
        }

        private void updateDNS() {
            try {
                setLocalIP();

                if (localIP != null && domainIP != null) {
                    if (!localIP.equals(domainIP)) {
                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        UpdateJson updateJson = new UpdateJson();
                        updateJson.type = DOMAIN.type;
                        updateJson.name = DOMAIN_NAME;
                        updateJson.content = localIP;
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
                            LOGGER_DDNS.info(String.format("域名 %s 已更新为 %s ,将睡眠 %s 秒", DOMAIN_NAME, localIP, defaultSleepSconds * 10));
                            domainIP = localIP;
                            sleep(defaultSleepSconds * 10 * 1000);
                        } else {
                            LOGGER_DDNS.error(String.format("域名 %s 更新失败,将睡眠 %s 秒后重试" + System.lineSeparator()
                                    + "失败原因：" + System.lineSeparator()
                                    + formatErrorMessages(responseJson.errors),
                                    DOMAIN_NAME, failedSleepSeconds));
                            sleep(failedSleepSeconds);
                        }
                    } else {
                        LOGGER_DDNS.info(String.format("域名 %s 未发生变化, 将睡眠 %s 秒", DOMAIN_NAME, defaultSleepSconds));
                        sleep(defaultSleepSconds * 1000);
                    }
                } else {
                    LOGGER_DDNS.info(String.format("域名 %s 未能获取新IP地址, 将睡眠 %s 秒", DOMAIN_NAME, failedSleepSeconds));
                    sleep(failedSleepSeconds * 1000);
                }
            } catch (IOException ex) {
                LOGGER_DDNS.error(String.format("进程发生致命故障，但未中断，将在 %s 秒后重试", failedSleepSeconds));
                LOGGER_EX.error("发生错误",ex);
                sleep(failedSleepSeconds * 1000);
            }
        }

        private void setLocalIP() {

            switch (sourceType) {
                case "baidu":
                    getIPviaBaidu();
                    break;
                case "passive":
                    getIPviaPassive();
                    break;
                case "http":
                    getIPviaHtttp();
                    break;
                case "js":
                    getIPviaJS();
                    break;
                default:
                    LOGGER_DDNS.error(String.format("未知IP来源 %s", CONFIG.whereGetYourIP));
                    LOGGER_EX.error(String.format("未知IP来源 %s", CONFIG.whereGetYourIP));
                    System.exit(1);
            }
        }

        private void setSourceType() {
            if (CONFIG.isBaidu) {
                sourceType = "baidu";
            } else if (DOMAIN.passiveUpdate) {
                sourceType = "passive";
            } else if (CONFIG.whereGetYourIP.startsWith("http://") || CONFIG.whereGetYourIP.startsWith("https://")) {
                sourceType = "http";
            } else if (CONFIG.whereGetYourIP.endsWith(".js")) {
                sourceType = "js";
            } else {
                sourceType = "unknown";
            }
        }

        private void getIPviaBaidu() {
            try {
                HttpConnection conn = (HttpConnection) Jsoup.connect("https://www.baidu.com/s?wd=ip");
                Document doc = conn.execute().parse();
                localIP = doc.getElementById("1").attr("fk");
            } catch (NullPointerException | IOException ex) {
                LOGGER_DDNS.error(String.format("无法连接至百度，将在 %s 秒后重试", failedSleepSeconds));
                LOGGER_EX.error("发生错误",ex);
                localIP = null;
                sleep(failedSleepSeconds * 1000);
            }
        }

        private void getIPviaHtttp() {
            try {
                HttpConnection conn = (HttpConnection) Jsoup.connect(CONFIG.whereGetYourIP);
                conn.ignoreContentType(true);
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .create();
                LocalIPJson addr = gson.fromJson(conn.execute().body(), LocalIPJson.class);
                localIP = addr.ip;
            } catch (IOException ex) {
                LOGGER_DDNS.error(String.format("服务器 %s 故障，将在 %s 秒后重试", CONFIG.whereGetYourIP, failedSleepSeconds));
                LOGGER_EX.error("发生错误",ex);
                localIP = null;
                sleep(failedSleepSeconds * 1000);
            } catch (IllegalArgumentException ex) {
                LOGGER_DDNS.fatal(String.format("无效HTTP地址 %s", CONFIG.whereGetYourIP));
                LOGGER_EX.fatal("发生错误",ex);
                System.exit(1);
            }
        }

        private void getIPviaJS() {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("nashorn");
            try {
                engine.eval(new FileReader(CONFIG.whereGetYourIP));
                Invocable invo = (Invocable) engine;
                localIP = (String) invo.invokeFunction("getIP", "");
            } catch (FileNotFoundException | NoSuchMethodException | ScriptException ex) {
                LOGGER_DDNS.error(String.format("无效JavaScript文件 %s", CONFIG.whereGetYourIP));
                LOGGER_EX.error("发生错误",ex);
                localIP = null;
                System.exit(1);
            }
        }

        private void getIPviaPassive() {
            PassiveUpdate com = (PassiveUpdate) COMPONENTS.get("passiveUpdate");
            localIP = com.getIP(DOMAIN.passiveUpdateID);
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

        class LocalIPJson {

            String ip;
        }
    }

    public Thread getUpdateThread(String threadName) {
        return UPDATE_THREADS.getOrDefault(threadName, null);
    }

    public HashMap<String, Thread> getUpdateThreads() {
        return UPDATE_THREADS;
    }

    public ComponentAbstract getComponent(String name) {
        return COMPONENTS.getOrDefault(name, null);
    }

    public HashMap<String, ComponentAbstract> getComponents() {
        return COMPONENTS;
    }

    public Configuration getCONFIG() {
        return CONFIG;
    }

}
