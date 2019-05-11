package club.magiccrazyman.ddns.components.command;

import club.magiccrazyman.ddns.Main;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

/**
 *
 * @author Magic Crazy Man
 */
public class ReloadCommand implements CommandInterface {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private HashMap<String, Thread> threads;

    @Override
    public void register(Command command) {
        threads = command.getDDNSInstance().getAllUpdateThreads();
    }

    @Override
    public void exec(String[] args) {
        LOGGER_DDNS.info("正在重新读取配置并重启...");
        threads.values().forEach((t) -> {
            t.interrupt();
        });
        Main.main(Main.inputArgs);
        Thread.currentThread().interrupt();
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String desc() {
        return "重新读取配置文件并重启";
    }

}
