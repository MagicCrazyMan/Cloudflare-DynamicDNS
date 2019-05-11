package club.magiccrazyman.ddns.components.passiveupdate;

import club.magiccrazyman.ddns.components.ComponentAbstract;
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

    private int port = 0;
    private boolean isExec = false;
    private HashMap<String, String> id2ip = new HashMap<>();

    @Override
    public void register(DDNS ddns) {
        disable();
        if (ddns.getCONFIG().enablePassiveUpdateModule) {
            enable();
            port = ddns.getCONFIG().passiveUpdatePort;
            for (Account account : ddns.getCONFIG().accounts) {
                for (Domain domain0 : account.domains) {
                    if (domain0.passiveUpdate) {
                        enable();
                        id2ip.put(domain0.passiveUpdateID, null);
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
            Logger.getLogger(PassiveUpdate.class.getName()).log(Level.SEVERE, null, ex);
        }
        isExec = true;
    }

    class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            try (OutputStream os = t.getResponseBody()) {

                String idIn = t.getRequestHeaders().getFirst("id");
                if (id2ip.containsKey(idIn)) {
                    String ip = t.getRequestHeaders().getFirst("ip");
                    id2ip.replace(idIn, ip);
                    if (!ip.isEmpty() || ip != null) {
                        String response = "successed " + ip;
                        t.sendResponseHeaders(200, response.length());
                        os.write(response.getBytes());
                        LOGGER_DDNS.info(String.format("辨识号 %s 所指向域名被动更新为 %s ，更新线程结束休眠后将更新域名", idIn, ip));
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
        if (isExec) {
            if (id2ip.containsKey(id)) {
                return id2ip.get(id);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
