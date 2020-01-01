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
package club.magiccrazyman.ddns.components.command;

import club.magiccrazyman.ddns.components.ComponentAbstract;
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
public class Command extends ComponentAbstract {

    private final static Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");

    private final HashMap<String, CommandInterface> COMMANDS = new HashMap<>();
    private DDNS DDNS;
    private boolean isInit = false;

    @Override
    public void register(DDNS ddns) {
        DDNS = ddns;
        initInternalCommand();
        initExternalCommand();
        isInit = true;
    }

    @Override
    public String name() {
        return "command";
    }

    @Override
    public void exec() {
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
        return COMMANDS.get(name);
    }
}
