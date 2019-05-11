package club.magiccrazyman.ddns.components.command;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

/**
 *
 * @author Magic Crazy Man
 */
public class UpdateCommand implements CommandInterface {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private HashMap<String, Thread> threads;

    @Override
    public void register(Command command) {
        threads = command.getDDNSInstance().getAllUpdateThreads();
    }

    @Override
    public void exec(String[] args) {
        Iterator<String> targetAll = threads.keySet().iterator();
        while (targetAll.hasNext()) {
            String target = targetAll.next();
            if (threads.containsKey(target)) {
                LOGGER_DDNS.info(String.format("正在强制更新域名线程 %s", target));

                try {
                    //反射获取更新线程的Runnable并执行updateDNS方法
                    Thread updateThread = threads.get(target);
                    Field field = updateThread.getClass().getDeclaredField("target");
                    field.setAccessible(true);
                    Runnable runnable = (Runnable)field.get(updateThread);
                    Method method = runnable.getClass().getDeclaredMethod("updateDNS");
                    method.setAccessible(true);
                    Thread t = new Thread(() -> {
                        try {
                            method.invoke(runnable);
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            java.util.logging.Logger.getLogger(UpdateCommand.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                    t.setName("temp_" + target);
                    t.start();
                } catch (NoSuchFieldException | SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException ex) {
                    java.util.logging.Logger.getLogger(UpdateCommand.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                LOGGER_DDNS.error(String.format("无效线程 %s", target));
            }
        }
    }

    @Override
    public String name() {
        return "update";
    }

    @Override
    public String desc() {
        return "<线程名称> 强制更新域名";
    }

}
