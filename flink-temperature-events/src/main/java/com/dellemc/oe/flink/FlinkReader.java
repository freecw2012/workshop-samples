package com.dellemc.oe.flink;

import com.dellemc.oe.flink.utils.PravegaUtils;
import com.dellemc.oe.flink.sensors.temperature.SensorEventRouter;
import com.dellemc.oe.flink.sensors.temperature.SensorReading;
import com.dellemc.oe.flink.sensors.temperature.SensorTimeAssigner;
import com.dellemc.oe.flink.sensors.temperature.TemperatureSensorSource;
import io.pravega.connectors.flink.FlinkPravegaReader;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import io.pravega.client.stream.Stream;
import io.pravega.client.stream.StreamConfiguration;
import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.FlinkPravegaWriter;
import io.pravega.connectors.flink.PravegaConfig;
import io.pravega.connectors.flink.serialization.PravegaSerialization;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlinkReader extends AbstractJob {
    private static Logger LOG = LoggerFactory.getLogger(FlinkWriter.class);

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration(ParameterTool.fromArgs(args));
        new FlinkReader(config)
            .run();
    }

    public FlinkReader(Configuration config) {
        super(config);
    }

    public void run() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // When running locally the Scope may need to be created (Nautilus creates it in a cluster)
        if (env instanceof LocalStreamEnvironment) {
            LOG.info("Creating Scope %s", config.getPravegaConfig());
            PravegaUtils.createScope(config.getPravegaConfig(), config.getPravegaScope());
        }

        FlinkPravegaReader<SensorReading> pravegaReader = createPravegaReader(config.getStreamName(), config.getNumStreamSegments());

        // ingest sensor stream
        DataStream<SensorReading> sensorData = env
            // SensorSource generates random temperature readings
            .addSource(pravegaReader);
           

         DataStream<SensorReading> avgTemp = sensorData
            .map( r -> new SensorReading(r.id, r.timestamp, (r.temperature - 32) * (5.0 / 9.0))) // convert Fahrenheit to Celsius using and inlined map function
            .keyBy(r -> r.id) // organize stream by sensor
            .timeWindow(Time.seconds(1))   // group readings in 1 second windows
            .apply(new TemperatureAverager()); // compute average temperature using a user-defined function

        // print result stream to standard out
        //avgTemp.print();

        Stream sensor_stream = getOrCreateStream(config.getPravegaConfig(), "sensor-stream", config.getNumStreamSegments());
        // create the Pravega sink to write a stream of text
        FlinkPravegaWriter<SensorReading> writer = FlinkPravegaWriter.<SensorReading>builder()
                .withPravegaConfig(config.getPravegaConfig())
                .forStream(sensor_stream)
                .withEventRouter(new SensorEventRouter())
                .withSerializationSchema(PravegaSerialization.serializationFor(SensorReading.class))
                .build();
        avgTemp.addSink(writer).name("Pravega Sensor Stream");

        // create another output sink to print to stdout for verification
        //avgTemp.print().name("stdout");
        System.out.println("============== Average Temperature ===============");
        avgTemp.print();

        /*StreamTableEnvironment tableEnv = TableEnvironment.getTableEnvironment(env);

        String[] fieldNames = {"sensor_id", "timestamp", "temperature"};

        // Read data from the stream using Table reader
        TableSchema tableSchema = TableSchema.builder()
                .field("sensor_id", Types.STRING())
                .field("timestamp", Types.STRING())
                .field("temperature", Types.STRING())
                .build();

        // Write the table to a Pravega Stream
        FlinkPravegaJsonTableSink sink = FlinkPravegaJsonTableSink.builder()
            .forStream("sensor_stream")
            .withPravegaConfig(config)
            .withRoutingKeyField("sensor_id")
            .withWriterMode(EXACTLY_ONCE)
            .build();
        
        table.writeToSink(sink);*/

        // execute application
        env.execute("Compute average sensor temperature");
       
    }

    /**
     *  User-defined WindowFunction to compute the average temperature of SensorReadings
     */
    public static class TemperatureAverager implements WindowFunction<SensorReading, SensorReading, String, TimeWindow> {

        /**
         * apply() is invoked once for each window.
         *
         * @param sensorId the key (sensorId) of the window
         * @param window meta data for the window
         * @param input an iterable over the collected sensor readings that were assigned to the window
         * @param out a collector to emit results from the function
         */
        @Override
        public void apply(String sensorId, TimeWindow window, Iterable<SensorReading> input, Collector<SensorReading> out) {

            // compute the average temperature
            int cnt = 0;
            double sum = 0.0;
            for (SensorReading r : input) {
                cnt++;
                sum += r.temperature;
            }
            double avgTemp = sum / cnt;

            // emit a SensorReading with the average temperature
            out.collect(new SensorReading(sensorId, window.getEnd(), avgTemp));
        }
    }
}
