package ru.otus;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.otus.config.AppConfig;
import ru.otus.generated.LoadBalancerServiceGrpc;
import ru.otus.generated.NodeServiceGrpc;
import ru.otus.generated.Request;
import ru.otus.generated.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LoadBalancerServiceImpl extends LoadBalancerServiceGrpc.LoadBalancerServiceImplBase {

    private static final AppConfig config = AppConfig.getConfiguration().orElseThrow(() -> new IllegalStateException("Failed to load configuration"));

    private final Map<Integer, Server> nodes = new HashMap<>();

    public LoadBalancerServiceImpl(int numberOfNodes, int portNumber) {
        int nextPortNumber = portNumber;
        for (int i = 0; i < numberOfNodes; i++) {
            int nodeId = i + 1;
            Server server = ServerBuilder.forPort(nextPortNumber++).addService(new NodeServiceImpl(nodeId)).build();
            try {
                server.start();
                if (server.getPort() != 0) {
                    log.info("Node {} server started on port {}", nodeId, server.getPort());
                    nodes.put(server.getPort(), server);
                } else {
                    log.error("Node {} server failed to start with port {}", nodeId, nextPortNumber - 1);
                }
            } catch (IOException e) {
                log.error("Failed to start Node {} server", nodeId, e);
            }
        }
    }

    @Override
    public void processRequest(Request request, StreamObserver<Response> responseObserver) {
        if (nodes.isEmpty()) {
            responseObserver.onError(new IllegalStateException("No nodes available to process the request"));
            return;
        }

        int nodeIndex = ThreadLocalRandom.current().nextInt(nodes.size());
        Server node = (Server) nodes.values().toArray()[nodeIndex];
        ManagedChannel channel = ManagedChannelBuilder.forAddress(config.getHost(), node.getPort()).usePlaintext().build();

        NodeServiceGrpc.NodeServiceBlockingStub stub = NodeServiceGrpc.newBlockingStub(channel);

        try {
            Response result = stub.processRequest(request);
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        } finally {
            channel.shutdownNow();
        }
    }

    private Server getNextNode() {
        int randomIndex = ThreadLocalRandom.current().nextInt(nodes.size());
        int randomNodePort = (int) nodes.keySet().toArray()[randomIndex];
        return nodes.get(randomNodePort);
    }

    private void shutdownNode(Server node) {
        try {
            node.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for node to shut down", e);
        }
    }

    public void registerNode(Server node) {
        nodes.put(node.getPort(), node);
    }
}