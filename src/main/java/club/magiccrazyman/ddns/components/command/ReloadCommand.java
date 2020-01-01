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

    private HashMap<String, Thread> threads;

    @Override
    public void register(Command command) {
        threads = command.getDDNSInstance().getUpdateThreads();
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
