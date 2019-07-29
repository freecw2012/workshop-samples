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
package com.dellemc.oe.readers;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.dellemc.oe.serialization.JsonNodeSerializer;
import com.dellemc.oe.util.ImageToByteArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pravega.client.ByteStreamClientFactory;
import io.pravega.client.ClientConfig;
import io.pravega.client.ClientFactory;
import io.pravega.client.EventStreamClientFactory;
import io.pravega.client.admin.ReaderGroupManager;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.byteStream.ByteStreamReader;
import io.pravega.client.byteStream.ByteStreamWriter;
import io.pravega.client.byteStream.impl.ByteStreamClientImpl;
import io.pravega.client.netty.impl.ConnectionFactoryImpl;
import io.pravega.client.stream.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.dellemc.oe.util.CommonParams;
import io.pravega.common.io.StreamHelpers;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageWriter;


/**
 * A simple example app that uses a Pravega Writer to write to a given scope and stream.
 */
public class ImageReader {
    // Logger initialization
    private static final Logger LOG = LoggerFactory.getLogger(ImageReader.class);
    private static final int READER_TIMEOUT_MS = 3000;

    public final String scope;
    public final String streamName;
    public final URI controllerURI;

    public ImageReader(String scope, String streamName, URI controllerURI) {
        this.scope = scope;
        this.streamName = streamName;
        this.controllerURI = controllerURI;
    }

    public void run()  {

             //String scope = "image-scope";
            String streamName = "image-stream";
            //URI controllerURI =  new URI("tcp://localhost:9090");
            StreamManager streamManager = StreamManager.create(controllerURI);
            streamManager.createScope(scope);
            StreamConfiguration streamConfig = StreamConfiguration.builder().build();
            streamManager.createStream(scope, streamName, streamConfig);

            // Create reader group and reader config
            final String readerGroup = UUID.randomUUID().toString().replace("-", "");
            final ReaderGroupConfig readerGroupConfig = ReaderGroupConfig.builder()
                    .stream(Stream.of(scope, streamName))
                    .build();
            try (ReaderGroupManager readerGroupManager = ReaderGroupManager.withScope(scope, controllerURI)) {
                readerGroupManager.createReaderGroup(readerGroup, readerGroupConfig);
            }

            // Create client config
            ClientConfig clientConfig = ClientConfig.builder().controllerURI(URI.create(controllerURI.toString())).build();
            ByteStreamReader reader = null;
            // Create  EventStreamClientFactory and  create reader to get stream data
            try
            {
                ByteStreamClientFactory clientFactory = ByteStreamClientFactory.withScope(scope, clientConfig);
                reader = clientFactory.createByteStreamReader(streamName);
                byte[] readBuffer = new byte[1024];

                CompletableFuture<Integer>  count   =   reader. onDataAvailable();
                LOG.info("#######################     count.get()   ######################  "+count.get());
                while (count.get() > 0) {
                    //int  result  =StreamHelpers.readAll(reader, readBuffer, 0, readBuffer.length);
                    byte[]  result  =StreamHelpers.readAll(reader, count.get());
                    LOG.info("#######################     RECEIVED IMAGE DATA   ######################  "+result);
                    // Test whether we are able to create  same image or not
                    //ImageToByteArray.createImage(result);
               }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                reader.close();
            }

    }



    public static void main(String[] args) {
        final String scope = CommonParams.getScope();
        final String streamName = CommonParams.getStreamName();
        final URI controllerURI = CommonParams.getControllerURI();
        LOG.info("#######################     SCOPE   ###################### "+scope);
        LOG.info("#######################     streamName   ###################### "+streamName);
        LOG.info("#######################     controllerURI   ###################### "+controllerURI);
        ImageReader imageReader = new ImageReader(scope, streamName, controllerURI);
        imageReader.run();
    }
}
