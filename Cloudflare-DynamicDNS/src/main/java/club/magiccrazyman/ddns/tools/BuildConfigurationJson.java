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

import club.magiccrazyman.ddns.Configuration;
import club.magiccrazyman.ddns.Configuration.Account;
import club.magiccrazyman.ddns.Configuration.Account.Domain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magic Crazy Man
 */
public class BuildConfigurationJson {

    /**
     * Tools development version
     */
    public static final String VERSION = "1.0.2";

    private static final Console CONSOLE = System.console();

    /**
     * Start tool
     */
    public static void start() {
        System.out.println(String.format("正在使用配置文件创建工具(版本 %s )", VERSION));

        Configuration json = new Configuration();

        System.out.println("开始设置全局变量");
        json.whereGetYourIP = CONSOLE.readLine("指定获取本机IP的URL(若无并使用百度作为搜索来源请留空)：");
        json.defaultSleepSconds = readInt(CONSOLE.readLine("指定成功更新后等待的时间(秒)："));
        json.failedSleepSeconds = readInt(CONSOLE.readLine("指定任意失败后等待重试的时间(秒)："));
        System.out.println("全局变量设置完毕");
        System.out.println();

        boolean accountFinished = false;
        while (!accountFinished) {
            System.out.println("开始设置帐号信息");

            Account account = new Account();
            account.email = String.valueOf(CONSOLE.readLine("指定帐号邮箱："));
            account.key = String.valueOf(CONSOLE.readPassword("指定Cloudflare密钥(密钥输入过程不可见，请直接粘贴复制后回车)："));
            System.out.println();

            boolean domainsFinished = false;
            while (!domainsFinished) {
                System.out.println("开始设置域名信息");

                Domain domain = new Domain();
                domain.zone = String.valueOf(CONSOLE.readLine("目标域名属下的zone："));
                domain.identifier = String.valueOf(CONSOLE.readLine("目标域名的identifier："));
                for (String type = ""; !Configuration.VALID_TYPE.contains(type);) {
                    type = CONSOLE.readLine("该域名的类型：");
                    if (Configuration.VALID_TYPE.contains(type)) {
                        domain.type = type;
                    } else {
                        System.out.println("无效的类型，可用的值有" + Configuration.VALID_TYPE);
                        type = CONSOLE.readLine("重新输入该域名的类型：");
                    }
                }
                for (Integer ttl = 0; !Configuration.VALID_TTL.contains(ttl);) {
                    ttl = readInt(CONSOLE.readLine("该域名的TTL值："));
                    if (Configuration.VALID_TTL.contains(ttl)) {
                        domain.ttl = ttl;
                    } else {
                        System.out.println("无效的TTL值，可用的值有" + Configuration.VALID_TTL);
                        ttl = Integer.valueOf(CONSOLE.readLine("重新输入该域名的TTL值："));
                    }
                }
                domain.proixed = CONSOLE.readLine("该域名是否使用Cloudflare CDN代理(y/N)：").toLowerCase().equals("y");
                domain.nickname = CONSOLE.readLine("给该域名输入一个昵称(仅用于方便标识):");
                account.domains.add(domain);

                System.out.println("该域名设置完毕");
                System.out.println();

                domainsFinished = !CONSOLE.readLine("是否继续加入另一个域名?(y/N)").toLowerCase().equals("y");
            }
            json.accounts.add(account);
            System.out.println("该账户设置完毕");
            System.out.println();

            accountFinished = !CONSOLE.readLine("是否继续加入另一个帐号?(y/N)").toLowerCase().equals("y");
        }
        System.out.println();
        System.out.println("所有信息设置完毕");
        String name = CONSOLE.readLine("输入文件名称(无须后缀):");
        System.out.println("正在当前文件夹生成配置文件...");
        System.out.println();

        buildJson(json, name);
    }

    static Integer readInt(String str) {
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException ex) {
            return readInt(CONSOLE.readLine("输入错误，请重新输入："));
        }
    }

    static void buildJson(Object json, String name) {
        File file = new File(name + ".json");

        if (file.exists()) {
            boolean isCover = CONSOLE.readLine("当前文件夹下已有同名文件,是否覆盖(y/N)").toLowerCase().equals("y");
            if (isCover) {
                file.delete();
            } else {
                file = new File(name + "_new.json");
            }
        }
        try {
            file.createNewFile();
            String os = System.getProperty("os.name").toLowerCase();
            switch (os) {
                case "linux":
                    Files.setPosixFilePermissions(file.toPath(), Configuration.VALID_PERMISSIONS);
                    break;
            }
            try (FileWriter fw = new FileWriter(file)) {
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
                fw.write(gson.toJson(json));
            }
        } catch (IOException ex) {
            Logger.getLogger(BuildConfigurationJson.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("配置文件已创建" + file.getAbsolutePath());
    }
}
