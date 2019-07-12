package com.dellemc.oe.flink;

import com.dellemc.oe.flink.utils.PravegaUtils;
import com.dellemc.oe.flink.sensors.temperature.TemperatureSensorSource;
import com.dellemc.oe.flink.sensors.temperature.SensorReading;
import com.dellemc.oe.flink.sensors.temperature.SensorTimeAssigner;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A processing graph that dumps the generated events directly into a Pravega Stream
 */
public class FlinkWriter extends AbstractJob {
    private static Logger LOG = LoggerFactory.getLogger(FlinkWriter.class);

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(ParameterTool.fromArgs(args));
        new FlinkWriter(config)
            .run();
    }

    public FlinkWriter(Configuration config) {
        super(config);
    }

    public void run() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // use event time for the application
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        // configure watermark interval
        env.getConfig().setAutoWatermarkInterval(1000L);


        // When running locally the Scope may need to be created (Nautilus creates it in a cluster)
        if (env instanceof LocalStreamEnvironment) {
            LOG.info("Creating Scope %s", config.getPravegaConfig());
            PravegaUtils.createScope(config.getPravegaConfig(), config.getPravegaScope());
        }

        TemperatureSensorSource temperatureSensorSource = createTemperatureSensorSource();
        FlinkPravegaWriter<SensorReading> pravegaWriter = createPravegaWriter(config.getStreamName(), config.getNumStreamSegments());

        /** Construct Graph **/
        DataStream<SensorReading> generatedEventStream = env.addSource(temperatureSensorSource)
            .setParallelism(config.getSourceParallelism())
            // assign timestamps and watermarks which are required for event time
            .assignTimestampsAndWatermarks(new SensorTimeAssigner());

        generatedEventStream.addSink(pravegaWriter);

        env.execute("Flink Temperature Sensor Writer");
    }
}
