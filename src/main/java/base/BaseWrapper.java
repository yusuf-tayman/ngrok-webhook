package base;


import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

class BaseWrapper {
    private HttpServer httpServer;
    Tunnel tunnel;
    private NgrokClient ngrokClient;
    final Map<String, Stack<WebhookRequest>> requests = new ConcurrentHashMap<>();

    static class DefaultStack extends Stack<WebhookRequest> {
    }

    /**
     * It starts http-server and ngrok-client with random port.
     *
     * @throws IOException if http-server or ngrok-client fails
     */
    synchronized void startHttpServer() throws IOException {
        if (httpServer != null) {
            return;
        }
        int counter = 0;
        int port = RandomUtil.getRandomInteger(1024, 2 << 16 - 1);
        while (counter < 1000) {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(port), 0);
                Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            } catch (BindException e) {
                Log.warn("Unable to bind for port " + port);
                e.printStackTrace();
                counter++;
                continue;
            }

            createHttpContext();

            httpServer.start();
            ngrokClient = new NgrokClient.Builder().build();
            CreateTunnel createTunnel = new CreateTunnel.Builder().withProto(Proto.HTTP).withAddr(port).build();
            tunnel = ngrokClient.connect(createTunnel);
            Log.info(String.format("ngrok tunnel \"%s\" -> \"http://127.0.0.1:%d\"", tunnel.getPublicUrl(), port));
            break;
        }
    }

    /**
     * It creates http-server context.
     */
    private void createHttpContext() {
        httpServer.createContext("/", request -> {
            request.sendResponseHeaders(200, 0);
            try {
                requests.compute(request.getRequestURI().getPath(), (key, webhookRequests) -> {
                    if (webhookRequests == null) {
                        webhookRequests = new DefaultStack();
                    }

                    try {
                        webhookRequests.push(new WebhookRequest(IOUtils.toString(request.getRequestBody(), StandardCharsets.UTF_8), request.getRequestHeaders()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    synchronized (webhookRequests) {
                        webhookRequests.notifyAll();
                    }
                    return webhookRequests;
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
            request.close();
        });
    }


    /**
     * It stops http-server and kills ngrok-client.
     */
    private void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (tunnel != null && ngrokClient != null) {
            ngrokClient.kill();
        }
    }

}
