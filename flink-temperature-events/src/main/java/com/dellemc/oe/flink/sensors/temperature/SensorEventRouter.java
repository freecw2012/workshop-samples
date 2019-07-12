package com.dellemc.oe.flink.sensors.temperature;

import com.dellemc.oe.flink.sensors.temperature.SensorReading;
import io.pravega.connectors.flink.PravegaEventRouter;
import java.io.Serializable;
import java.util.UUID;

public class SensorEventRouter implements PravegaEventRouter<SensorReading> {
    @Override
    public String getRoutingKey(SensorReading sensorReading) {
        return UUID.randomUUID().toString();
    }
}
