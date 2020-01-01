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
