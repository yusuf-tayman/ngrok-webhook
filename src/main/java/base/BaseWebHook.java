package base;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public interface BaseWebHook {
    BaseWrapper wrapper = new BaseWrapper();

    /**
     * It starts http-server and ngrok-client with random port.
     */
    default void startHttpServer() {
        try {
            wrapper.startHttpServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * It listens to the requests through the ngrok-client without timeouts
     *
     * @param path path to listen
     * @return stack of webhook requests
     */
    default Stack<WebhookRequest> waitFor(String path) {
        return waitFor(path, 30);
    }

    /**
     * It listens to the requests through the ngrok-client with timeouts
     *
     * @param path           path to listen
     * @param timeoutSeconds timeout in seconds
     * @return stack of webhook requests
     */
    default Stack<WebhookRequest> waitFor(String path, int timeoutSeconds) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Stack<WebhookRequest> mutator = new Stack<>();

        Stack<WebhookRequest> finalMutator = mutator;
        Stack<WebhookRequest> newlyCreated = wrapper.requests.compute(path, (key, webhookRequests) -> {
            if (webhookRequests != null) {
                return webhookRequests;
            }
            return finalMutator;
        });

        if (newlyCreated instanceof BaseWrapper.DefaultStack) {
            Log.info("returning default listener stack for " + path);
            mutator = newlyCreated;
        } else if (mutator != newlyCreated) {
            throw new RuntimeException("Another process already listening same path");
        } else {
            Log.info("Listening for messages " + path);
            while (CollectionUtils.isEmpty(mutator)
                    && TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) <= timeoutSeconds) {
                try {
                    synchronized (mutator) {
                        mutator.wait(500L);
                    }
                } catch (InterruptedException e) {
                    Log.warn(e.getMessage());
                }
            }
        }

        wrapper.requests.remove(path);
        if (CollectionUtils.isEmpty(mutator)) {
            throw new RuntimeException("didn't received any hook");
        }
        return mutator;
    }

    /**
     * It gets public url from the generated public ngrok-client url
     *
     * @return public url
     */
    default String getPublicUrl() {
        if (wrapper.tunnel == null) {
            throw new RuntimeException("not connected");
        }
        return wrapper.tunnel.getPublicUrl();
    }
}
