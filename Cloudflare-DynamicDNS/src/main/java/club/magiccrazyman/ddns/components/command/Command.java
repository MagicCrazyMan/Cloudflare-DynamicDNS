package club.magiccrazyman.ddns.components.command;

import club.magiccrazyman.ddns.components.ComponentInterface;
import club.magiccrazyman.ddns.core.DDNS;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.reflections.Reflections;

/**
 *
 * @author Magic Crazy Man
 */
public class Command implements ComponentInterface {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private final HashMap<String, CommandInterface> COMMANDS = new HashMap<>();
    private DDNS DDNS;
    private boolean isInit = false;

    @Override
    public void register(DDNS ddnsInstance) {
        DDNS = ddnsInstance;
        initInternalCommand();
        initExternalCommand();
    }

    @Override
    public String name() {
        return "command";
    }

    @Override
    public void run() {
        if (isInit) {
            while (!Thread.currentThread().isInterrupted()) {
                listen();
            }
        }
    }

    private void listen() {
        Scanner scanner = new Scanner(System.in);
        String[] input = scanner.nextLine().trim().replaceAll("\\s+", " ").split(" ");
        String command = input[0];
        String[] args = Arrays.copyOfRange(input, 1, input.length);

        if (!command.equals("")) {
            LOGGER_DDNS.info(String.format("检测到命令：%s", command));
            if (COMMANDS.containsKey(command)) {
                COMMANDS.get(command).exec(args);
            } else {
                LOGGER_DDNS.info(String.format("未知命令 %s ,输入 help 以查看更多命令", command));
            }
        }
    }

    //初始化内置命令
    private void initInternalCommand() {
        Reflections reflections = new Reflections("club.magiccrazyman.ddns.components.command"); //使用反射搜索命令
        Set<Class<? extends CommandInterface>> clazz = reflections.getSubTypesOf(CommandInterface.class);
        ArrayList<Class<? extends CommandInterface>> arrs = new ArrayList<>(clazz);

        arrs.forEach((arr) -> {
            try {
                CommandInterface command = (CommandInterface) arr.newInstance();
                COMMANDS.put(command.name(), command);
            } catch (InstantiationException | IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(Command.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        COMMANDS.values().forEach((command) -> {
            command.register(this);
        });
        isInit = true;
    }

    //初始化外部命令
    private void initExternalCommand() {

    }

    public DDNS getDDNSInstance() {
        return DDNS;
    }

    public HashMap<String, CommandInterface> getAllCommands() {
        return COMMANDS;
    }

    public CommandInterface getCommand(String name) {
        if (COMMANDS.containsKey(name)) {
            return COMMANDS.get(name);
        } else {
            return null;
        }
    }
}
