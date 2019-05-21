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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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
        threads = command.getDDNSInstance().getUpdateThreads();
    }

    @Override
    public void exec(String[] args) {
        for (String target : args) {
            if (threads.containsKey(target)) {
                LOGGER_DDNS.info(String.format("正在强制更新域名线程 %s", target));

                try {
                    //反射获取更新线程的Runnable并执行updateDNS方法
                    Thread updateThread = threads.get(target);
                    Field field = updateThread.getClass().getDeclaredField("target");
                    field.setAccessible(true);
                    Runnable runnable = (Runnable) field.get(updateThread);
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
