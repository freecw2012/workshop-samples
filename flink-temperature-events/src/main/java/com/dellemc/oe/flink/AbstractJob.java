package com.dellemc.oe.flink;

import com.dellemc.oe.flink.sensors.temperature.SensorEventRouter;
import com.dellemc.oe.flink.utils.PravegaUtils;
import com.dellemc.oe.flink.sensors.temperature.TemperatureSensorSource;
import com.dellemc.oe.flink.sensors.temperature.SensorReading;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaConfig;
import io.pravega.connectors.flink.serialization.PravegaSerialization;

public abstract class AbstractJob {
    final protected Configuration config;

    public AbstractJob(Configuration config) {
        this.config = config;
    }

    protected TemperatureSensorSource createTemperatureSensorSource() {
        return new TemperatureSensorSource();
    }

    protected FlinkPravegaWriter<SensorReading> createPravegaWriter(String streamName, int numSegments) {
        Stream pravegaStream = getOrCreateStream(config.getPravegaConfig(), streamName, numSegments);

        return FlinkPravegaWriter.<SensorReading>builder()
                .withPravegaConfig(config.getPravegaConfig())
                .forStream(pravegaStream)
                .withEventRouter(new SensorEventRouter())
                .withSerializationSchema(PravegaSerialization.serializationFor(SensorReading.class))
                .build();
    }

    protected FlinkPravegaReader<SensorReading> createPravegaReader(String streamName, int numSegments) {
        Stream pravegaStream = getOrCreateStream(config.getPravegaConfig(), streamName, numSegments);

        return FlinkPravegaReader.<SensorReading>builder()
                .withPravegaConfig(config.getPravegaConfig())
                .forStream(pravegaStream)
                .withDeserializationSchema(PravegaSerialization.deserializationFor(SensorReading.class))
                .build();
    }

    static public Stream getOrCreateStream(PravegaConfig pravegaConfig, String streamName, int numSegments) {
        StreamConfiguration streamConfig = StreamConfiguration.builder()
            .scalingPolicy(ScalingPolicy.fixed(numSegments))
            .build();

        return PravegaUtils.createStream(pravegaConfig, streamName, streamConfig);
    }
}
