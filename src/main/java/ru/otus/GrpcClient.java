package ru.otus;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.config.AppConfig;
import ru.otus.generated.LoadBalancerServiceGrpc;
import ru.otus.generated.Request;

import java.util.concurrent.TimeUnit;

public class GrpcClient {

    private static final Logger log = LoggerFactory.getLogger(GrpcClient.class);

    private static final AppConfig config = AppConfig.getConfiguration().orElseThrow(() -> new IllegalStateException("Failed to load configuration"));

    public static void main(String[] arg) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(config.getHost(), config.getPort()).usePlaintext().build();

            var stub = LoadBalancerServiceGrpc.newBlockingStub(channel);

            for (int i = 0; i < 10; i++) {
                var request = Request.newBuilder().setId(RandomNumberHelper.getRandomId()).setContent("content").build();

                stub.processRequest(request).forEachRemaining(response -> log.info("Get response: {}", response.getContent()));
            }
        } catch (Exception e) {
            log.error("An error occurred while sending requests", e);
        } finally {
            if (channel != null) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for channel to shut down", e);
                }
            }
        }
    }
}