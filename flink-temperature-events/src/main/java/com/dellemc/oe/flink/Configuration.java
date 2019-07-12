package com.dellemc.oe.flink;

import io.pravega.connectors.flink.PravegaConfig;
import org.apache.flink.api.java.utils.ParameterTool;

public class Configuration {
    static private String SOURCE_PARALLELISM_PARAM = "sourceParallelism";
    static private int DEFAULT_SOURCE_PARALLELISM = 3;

    static private String READER_PARALLELISM_PARAM = "readerParallelism";
    static private int DEFAULT_READER_PARALLELISM = 3;

    static private String NUM_STREAM_SEGMENTS_PARAM = "streamSegments";
    static private int DEFAULT_NUM_STREAM_SEGEMENTS = 3;

    static private String STREAM_NAME_PARAM = "streamName";
    static private String DEFAULT_STREAM_NAME = "flink-temperature-events";

    final private ParameterTool params;

    public Configuration(ParameterTool params) {
        this.params = params;
    }

    public int getNumStreamSegments() {
        return params.getInt(NUM_STREAM_SEGMENTS_PARAM, DEFAULT_NUM_STREAM_SEGEMENTS);
    }

    public int getSourceParallelism() {
        return params.getInt(SOURCE_PARALLELISM_PARAM, DEFAULT_SOURCE_PARALLELISM);
    }

    public int getReaderParallelism() {
        return params.getInt(READER_PARALLELISM_PARAM, DEFAULT_READER_PARALLELISM);
    }

    public String getStreamName() {
        return params.get(STREAM_NAME_PARAM, DEFAULT_STREAM_NAME);
    }

    public PravegaConfig getPravegaConfig() {
        return PravegaConfig.fromParams(params);
    }

    public String getPravegaScope() {
        return getPravegaConfig().getDefaultScope();
    }

}
