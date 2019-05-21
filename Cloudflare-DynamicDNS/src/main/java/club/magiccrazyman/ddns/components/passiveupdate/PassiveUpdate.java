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
package club.magiccrazyman.ddns.components.passiveupdate;

import club.magiccrazyman.ddns.components.ComponentAbstract;
import club.magiccrazyman.ddns.core.Configuration;
import club.magiccrazyman.ddns.core.Configuration.Account;
import club.magiccrazyman.ddns.core.Configuration.Account.Domain;
import club.magiccrazyman.ddns.core.DDNS;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

/**
 *
 * @author Magic Crazy Man
 */
public class PassiveUpdate extends ComponentAbstract {

    private final static org.apache.logging.log4j.Logger LOGGER_DDNS = ((LoggerContext) LogManager.getContext()).getLogger("ddns");
    private final static org.apache.logging.log4j.Logger LOGGER_EX = ((LoggerContext) LogManager.getContext()).getLogger("exception");

    private Configuration config;
    private int port = 0;
    private HashMap<String, String> id2ip = new HashMap<>();
    private HashMap<String, Object> id2lock = new HashMap<>();

    @Override
    public void register(DDNS ddns) {
        disable();
        config = ddns.getCONFIG();
        if (config.enablePassiveUpdateModule) {
            enable();
            port = config.passiveUpdatePort;
            for (Account a : config.accounts) {
                for (Domain d : a.domains) {
                    if (d.passiveUpdate) {
                        id2ip.put(d.passiveUpdateID, null);
                        id2lock.put(d.passiveUpdateID, new Object());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public String name() {
        return "passiveUpdate";
    }

    @Override
    public void exec() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/ip", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException ex) {
            LOGGER_EX.fatal("发生错误,进程已终止", ex);
            Logger.getLogger(PassiveUpdate.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            try (OutputStream os = t.getResponseBody()) {

                String idIn = t.getRequestHeaders().getFirst("id");
                if (id2ip.containsKey(idIn)) {
                    String ip = t.getRequestHeaders().getFirst("ip");
                    if (!ip.isEmpty() && ip != null) {
                        if (!ip.equals(id2ip.get(idIn))) {
                            id2ip.replace(idIn, ip);
                            LOGGER_DDNS.info(String.format("辨识号 %s 所指向域名被动更新为 %s", idIn, ip));
                            Object lock = id2lock.get(idIn);
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                        String response = "successed " + ip;
                        t.sendResponseHeaders(200, response.length());
                        os.write(response.getBytes());
                    } else {
                        String response = "failed";
                        t.sendResponseHeaders(200, response.length());
                        os.write(response.getBytes());
                    }
                }
            }
        }
    }

    public String getIP(String id) {
        return id2ip.get(id);
    }

    public Object getLOCK(String id) {
        return id2lock.get(id);
    }
}
