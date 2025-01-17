/*
 * Copyright (c) 2017 Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 */
package com.dellemc.oe.ingest;


import com.dellemc.oe.util.CommonParams;
import com.dellemc.oe.util.ImageToByteArray;
import com.dellemc.oe.util.Utils;
import io.pravega.client.ByteStreamClientFactory;
import io.pravega.client.ClientConfig;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.byteStream.ByteStreamWriter;
import io.pravega.client.stream.StreamConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;


/**
 * A simple example app that uses a Pravega Writer to write to a given scope and stream.
 */
public class ImageWriter {
    // Logger initialization
    private static final Logger LOG = LoggerFactory.getLogger(ImageWriter.class);

    public final String scope;
    public final String streamName;
    public final URI controllerURI;

    public ImageWriter(String scope, String streamName, URI controllerURI) {
        this.scope = scope;
        this.streamName = streamName;
        this.controllerURI = controllerURI;
    }

    public static void main(String[] args) {
        final String scope = CommonParams.getScope();
        final String streamName = CommonParams.getStreamName();
        final String routingKey = CommonParams.getRoutingKeyAttributeName();
        final String message = CommonParams.getMessage();
        final URI controllerURI = CommonParams.getControllerURI();
        ImageWriter ew = new ImageWriter(scope, streamName, controllerURI);
        ew.run(routingKey, message);
    }

    public void run(String routingKey, String message) {

            //String scope = "image-scope";
            String streamName = "image-stream";
            // Create client config
            ClientConfig clientConfig = ClientConfig.builder().controllerURI(controllerURI).build();
            //  create stream
            boolean  streamCreated = Utils.createStream(scope, streamName, controllerURI);
            LOG.info(" @@@@@@@@@@@@@@@@ STREAM  =  "+streamName+ "  CREATED = "+ streamCreated);

            // Create ByteStreamClientFactory
           try( ByteStreamClientFactory clientFactory = ByteStreamClientFactory.withScope(scope, clientConfig);) {
               ByteStreamWriter writer = clientFactory.createByteStreamWriter(streamName);
               //  Read a image and convert to byte[]
               byte[] payload = ImageToByteArray.readImage();


               while (true) {
                   // write image data.
                   writer.write(payload);
                   LOG.info("@@@@@@@@@@@@@ DATA >>>  " + payload);
                   Thread.sleep(5000);
               }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

    }
}
