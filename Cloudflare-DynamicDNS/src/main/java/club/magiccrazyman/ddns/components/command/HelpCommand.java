package club.magiccrazyman.ddns.components.command;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Magic Crazy Man
 */
public class HelpCommand implements CommandInterface {

    private HashMap<String, CommandInterface> commands;

    @Override
    public void exec(String[] args) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keys = new ArrayList(commands.keySet());
        for (int i = 0; i < keys.size(); i++) {
            sb.append(String.format("%-8s %s", keys.get(i), commands.get(keys.get(i)).desc()));
            if (i < keys.size() - 1) {
                sb.append(System.lineSeparator());
            }
        }
        System.out.println(sb);
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String desc() {
        return "查看所有命令";
    }

    @Override
    public void register(Command command) {
        commands = command.getAllCommands();
    }

}
