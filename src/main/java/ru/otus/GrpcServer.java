package ru.otus;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.config.AppConfig;

import java.io.IOException;

public class GrpcServer {

    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    private static final AppConfig config = AppConfig.getConfiguration().orElseThrow(() -> new IllegalStateException("Failed to load configuration"));

    public static void main(String[] args) {
        LoadBalancerServiceImpl loadBalancer = new LoadBalancerServiceImpl(config.getNumberOfNodes(), config.getNodesPort());

        Server server = null;
        try {
            server = ServerBuilder.forPort(config.getPort()).addService(loadBalancer).build().start();
            log.info("Server waiting for client connections...");

            Server finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.error("Shutting down gRPC server since JVM is shutting down");
                if (finalServer != null) {
                    finalServer.shutdown();
                }
                log.error("Server shut down");
            }));

            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            log.error("Server encountered an error", e);
        } finally {
            if (server != null) {
                try {
                    server.shutdown().awaitTermination();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Server shutdown was interrupted", e);
                }
            }
        }
    }
}