package com.dellemc.nautilus.pravega.client.auth;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import io.pravega.client.ClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.client.stream.impl.JavaSerializer;


public class PravegaAuthHello {

    private final static String CONTROLLER_URI = "tcp://10.247.118.176:9090";
    private final static String SCOPE_NAME = "auth-demo-scope";
    private final static String STREAM_NAME = "auth-demo-stream";

    public final String scope;
    public final String streamName;
    public final URI controllerURI;

    public PravegaAuthHello(String scope, String streamName, URI controllerURI) {
        this.scope = scope;
        this.streamName = streamName;
        this.controllerURI = controllerURI;
    }

    public void run(String routingKey, String message) {
        StreamManager streamManager = StreamManager.create(controllerURI);
        streamManager.createScope(scope);
        System.out.println("@@@@@@@@@@ Scope created @@@@@@");
        StreamConfiguration streamConfig = StreamConfiguration.builder()
            .scalingPolicy(ScalingPolicy.fixed(1))
            .build();
        streamManager.createStream(scope, streamName, streamConfig);
        System.out.println("@@@@@@@@@@ Stream created @@@@@@");

        try (ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
             EventStreamWriter<String> writer = clientFactory.createEventWriter(streamName,
                 new JavaSerializer<String>(),
                 EventWriterConfig.builder().build())) {
            try
            {
                while(true)
                {
                    System.out.format("####################### START Writing message: '%s' with routing-key: '%s' to stream '%s / %s'%n",
                    message, routingKey, scope, streamName);
                    writer.writeEvent(routingKey, message);
                    System.out.format(" ");
                    System.out.format("####################### END message #########################");
                    Thread.sleep(10000);
                }
            }
            catch(Exception e){e.printStackTrace();}
            
            
            
        }
    }

    public static void main(String[] args) {
        final String scope = SCOPE_NAME;
        final String streamName = STREAM_NAME;
        final String uriString = CONTROLLER_URI;
        System.out.format("Params ==> scope: '%s' stream name: '%s' uriString: '%s'%n",
                scope, streamName, uriString);
        final URI controllerURI = URI.create(uriString);

        PravegaAuthHello hww = new PravegaAuthHello(scope, streamName, controllerURI);

        final String routingKey = UUID.randomUUID().toString();
        final String message = "Authentication Demo Basic message";
        hww.run(routingKey, message);
    }

    private static String getEnv(String variable) {
        //Optional<String> value = Optional.ofNullable(System.getenv(variable));
        //System.out.println(" env variable "+ variable);
        //return value.toString();
        return null;
    }
}
