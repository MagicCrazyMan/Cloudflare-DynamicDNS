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
package club.magiccrazyman.ddns.tools;

import club.magiccrazyman.ddns.core.Configuration;
import club.magiccrazyman.ddns.core.Configuration.Account;
import club.magiccrazyman.ddns.core.Configuration.Account.Domain;
import static club.magiccrazyman.ddns.tools.BuildConfigurationJson.readInt;
import club.magiccrazyman.ddns.tools.ListDNSRecordDetails.CloudflareResponseJson.Result;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;

/**
 *
 * @author Magic Crazy Man
 */
public class ListDNSRecordDetails {

    /**
     * Tools development version
     */
    public static final String VERSION = "1.0.3";

    private static final Console CONSOLE = System.console();

    /**
     * Start tools
     */
    public static void start() {
        System.out.println(String.format("正在使用DNS查询工具(版本 %s )", VERSION));
        System.out.println("");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String email = CONSOLE.readLine("请输入邮箱:");
        String password = String.valueOf(CONSOLE.readPassword("请输入Cloudflare Global API Key(密钥输入过程不可见):"));
        String zoneID = CONSOLE.readLine("请输入Zone ID:");
        System.out.println();

        HashMap<String, String> header = new HashMap<>();
        header.put("X-Auth-Email", email);
        header.put("X-Auth-Key", password);
        header.put("Content-Type", "application/json");
        HttpConnection conn = (HttpConnection) Jsoup.connect(String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records", zoneID));
        conn.headers(header);
        conn.ignoreContentType(true);
        try {
            String str = conn.execute().body();
            CloudflareResponseJson json = gson.fromJson(str, CloudflareResponseJson.class);

            System.out.println("查询到以下域名");
            for (int count = 0; count < json.result.size(); count++) {
                Result result = json.result.get(count);
                System.out.println(String.format("%s.(%s) %s", count, result.type, result.name));
            }
            System.out.println();

            System.out.println("输入 exit 退出");
            System.out.println("输入 build 进入配置文件创建模式");
            System.out.println("输入 序号 查看域名信息");
            System.out.println();
            boolean exit = false;
            while (!exit) {

                String in = CONSOLE.readLine("请输入一个命令或序号:");
                switch (in) {
                    case "":
                        break;
                    case "exit":
                        exit = true;
                        break;
                    case "build":
                        System.out.println();
                        System.out.println("进入配置文件创建模式");
                        buildMode(json, email, password, zoneID);
                        break;
                    default:
                        int id = BuildConfigurationJson.readInt(in);
                        if (id < json.result.size()) {
                            Result result = json.result.get(id);
                            System.out.println(String.format("域名:%s", result.name));
                            System.out.println(String.format("域名identifier:%s", result.id));
                            System.out.println(String.format("域名类型:%s", result.type));
                            System.out.println(String.format("域名记录值:%s", result.content));
                            System.out.println(String.format("域名TTL:%s", result.ttl));
                            System.out.println(String.format("域名是否被CDN代理:%s", result.proxied));
                            System.out.println();
                        } else {
                            System.out.println("超出范围!");
                            System.out.println();
                        }
                        break;
                }
            }
        } catch (IOException ex) {
            System.out.println("网络连接出错,请检查网络连接状态");
        }
    }

    private static void buildMode(CloudflareResponseJson responseJson, String email, String key, String zoneID) {
        Configuration configJson = new Configuration();

        System.out.println("开始设定全局变量");
        configJson.whereGetYourIP = CONSOLE.readLine("指定获取本机IP的URL(若无并使用百度作为搜索来源请留空)：");
        configJson.enablePassiveUpdateModule = CONSOLE.readLine("是否使用被动更新模式模块?(y/N)").toLowerCase().equals("y");
        configJson.passiveUpdatePort = configJson.enablePassiveUpdateModule ? readInt(CONSOLE.readLine("被动模式下启用HTTP服务器的端口：")) : 0;
        configJson.logFileHome = CONSOLE.readLine("指定日志文件储存位置：");
        configJson.defaultSleepSconds = Integer.parseInt(CONSOLE.readLine("指定成功更新后等待的时间(秒)："));
        configJson.failedSleepSeconds = Integer.parseInt(CONSOLE.readLine("指定任意失败后等待重试的时间(秒)："));
        System.out.println("全局变量设定完毕");
        System.out.println();

        Account account = new Account();
        account.email = email;
        account.key = key;

        boolean exit = false;
        while (!exit) {
            String in = CONSOLE.readLine("请选择一个域名序号(输入exit结束):");
            if (in.equals("exit")) {
                exit = true;
            } else {
                int id = BuildConfigurationJson.readInt(in);
                if (id < responseJson.result.size()) {
                    Result result = responseJson.result.get(id);
                    Domain domain = new Domain();
                    domain.passiveUpdate = CONSOLE.readLine("是否使用被动更新模式?(y/N)").toLowerCase().equals("y");
                    domain.passiveUpdateID = domain.passiveUpdate ? CONSOLE.readLine("被动模式下的唯一辨识号：") : "";
                    domain.identifier = result.id;
                    domain.type = result.type;
                    domain.zone = zoneID;
                    domain.ttl = result.ttl;
                    domain.proixed = result.proxied;
                    domain.nickname = CONSOLE.readLine("给该域名输入一个昵称(仅用于方便标识):");

                    account.domains.add(domain);
                    System.out.println("当前域名设置完毕");
                    System.out.println();

                } else {
                    System.out.println("超出范围!");
                    System.out.println();
                }
            }
        }

        configJson.accounts.add(account);
        System.out.println("该帐号设置完毕");

        System.out.println();
        System.out.println("所有信息设置完毕");
        String name = CONSOLE.readLine("输入文件名称(无须后缀):");
        System.out.println("正在当前文件夹生成配置文件...");
        System.out.println();

        BuildConfigurationJson.buildJson(configJson, name);
    }

    static class CloudflareResponseJson {

        boolean success;
        String[] errors;
        String[] messages;
        ArrayList<Result> result = new ArrayList<>();
        ResultInfo result_info;

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

        class ResultInfo {

            int page;
            int per_page;
            int total_page;
            int count;
            int total_count;
        }
    }
}
