package club.magiccrazyman.ddns.components.command;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

/**
 *
 * @author Magic Crazy Man
 */
public class ListCommand implements CommandInterface {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private HashMap<String, Thread> threads;

    @Override
    public void register(Command commandInstance) {
        threads = commandInstance.getDDNSInstance().getAllUpdateThreads();
    }

    @Override
    public void exec(String[] args) {
        StringBuilder str = new StringBuilder("以下线程正在运行：");
        threads.keySet().forEach((k) -> {
            str.append(k).append(" ");
        });
        LOGGER_DDNS.info(str);
    }

    @Override
    public String desc() {
        return "查询所有正在运行的域名更新线程";
    }

    @Override
    public String name() {
        return "list";
    }

}
