package ru.otus;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.otus.exceptions.NodeFaultException;
import ru.otus.generated.NodeServiceGrpc;
import ru.otus.generated.Request;
import ru.otus.generated.Response;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Getter
public class NodeServiceImpl extends NodeServiceGrpc.NodeServiceImplBase {

    private final int nodeServiceId;
    private Server server;

    public void startServer(int port) throws IOException {
        server = ServerBuilder.forPort(port).addService(this).build().start();
        log.info("Node {} server started on port {}", nodeServiceId, server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.error("Shutting down gRPC server for Node {} since JVM is shutting down", nodeServiceId);
            stopServer();
            log.error("Server for Node {} shut down", nodeServiceId);
        }));
    }

    public void stopServer() {
        if (server != null) {
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for server to shut down", e);
            }
        }
    }

    @Override
    public void processRequest(Request request, StreamObserver<Response> responseObserver) {
        log.info("Start processing request id = {} by node id = {}", request.getId(), nodeServiceId);

        if (ThreadLocalRandom.current().nextInt(100) < 10) {
            responseObserver.onError(new NodeFaultException(String.format("Node id = %d fault by unforeseen circumstances", nodeServiceId)));
        } else {
            var response = Response.newBuilder().setId(ThreadLocalRandom.current().nextInt()).setContent(String.format("Response on request id = %d by node id = %d", request.getId(), nodeServiceId)).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    public void registerWithLoadBalancer(LoadBalancerServiceImpl loadBalancer) {
        loadBalancer.registerNode(this.getServer());
    }
}