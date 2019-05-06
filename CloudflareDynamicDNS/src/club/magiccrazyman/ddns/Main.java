package club.magiccrazyman.ddns;

import club.magiccrazyman.ddns.tools.BuildConfigurationJson;
import club.magiccrazyman.ddns.tools.ListDNSRecordDetails;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Magic Crazy Man
 */
public class Main {

    /**
     * Development version
     */
    public final static String VERSION = "2.1.0";

    private static String config = "config.json";
    private static boolean isBaidu = false;
    private static boolean start = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CLI(args);

        if (start == true) {
            new DDNS(ConfigurationJson.initConfiguration(config, isBaidu)).startDDNS();
        }
    }

    private static void CLI(String[] args) {
        try {
            Options options = new Options();
            options.addOption(Option.builder("h")
                    .hasArg(false)
                    .longOpt("help")
                    .desc("显示所有可用参数")
                    .build());
            options.addOption(Option.builder("c")
                    .hasArg(true)
                    .longOpt("config")
                    .argName("FILE")
                    .desc("指定配置文件,默认为当前目录的config.json")
                    .build());
            options.addOption(Option.builder("v")
                    .hasArg(false)
                    .longOpt("version")
                    .desc("查看当前版本")
                    .build());
            options.addOption(Option.builder("b")
                    .hasArg(false)
                    .longOpt("baidu")
                    .desc("使用百度(www.baidu.com)查询网络IP" + System.lineSeparator()
                            + "不建议使用，有可能存在泄漏IP的风险，建议仅在无额外服务器可进行IP查询的情况下使用"
                    )
                    .build());
            options.addOption(Option.builder("j")
                    .hasArg(false)
                    .desc("运行配置文件创建工具")
                    .longOpt("json")
                    .build());
            options.addOption(Option.builder("l")
                    .hasArg(false)
                    .desc("运行DNS查询工具并进行配置文件创建")
                    .longOpt("list")
                    .build());

            DefaultParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            for (Option op : cmd.getOptions()) {
                switch (op.getOpt()) {
                    case "h":
                        String header = "Cloudflare 专用DDNS应用" + System.lineSeparator()
                                + "使用前请确认网络服务拥有公网IP地址" + System.lineSeparator()
                                + "请自行保证配置文件的安全，本应用不提供配置文件加密功能" + System.lineSeparator()
                                + "若Cloudflare DNS密钥泄漏，请迅速前往Cloudflare重置密钥！" + System.lineSeparator();
                        String footer = "如有任何问题，请联系 350088648@qq.com";
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp("Cloudflare DDNS", header, options, footer, true);
                        start = false;
                        break;
                    case "v":
                        System.out.println("Cloudflare 专用DDNS服务");
                        System.out.println("    当前版本为 " + VERSION);
                        start = false;
                        break;
                    case "j":
                        BuildConfigurationJson.start();
                        start = false;
                        break;
                    case "l":
                        ListDNSRecordDetails.start();
                        start = false;
                        break;
                    case "c":
                        config = cmd.getOptionValue("c");
                        break;
                    case "b":
                        isBaidu = true;
                        break;
                    default:
                        System.out.println("无效命令：" + op.getOpt());
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
