package club.magiccrazyman.ddns.components.command;

import club.magiccrazyman.ddns.core.DDNS;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

/**
 *
 * @author Magic Crazy Man
 */
public class StopCommand implements CommandInterface {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private HashMap<String, Thread> threads;

    @Override
    public void register(Command commandInstance) {
        threads = commandInstance.getDDNSInstance().getAllUpdateThreads();
    }

    @Override
    public void exec(String[] args) {

        threads.values().forEach((t) -> {
            LOGGER_DDNS.info(String.format("正在中止线程 %s ...", t.getName()));
            t.interrupt();
        });
        LOGGER_DDNS.info("程序中止");
        System.exit(0);
    }

    @Override
    public String desc() {
        return "强制关闭程序";
    }

    @Override
    public String name() {
        return "stop";
    }

}
