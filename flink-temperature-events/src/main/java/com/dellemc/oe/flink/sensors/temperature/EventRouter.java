package com.dellemc.oe.flink.sensors.temperature;

import com.dellemc.oe.flink.sensors.temperature.SensorReading;
import io.pravega.connectors.flink.PravegaEventRouter;
import java.io.Serializable;
import java.util.UUID;

public class EventRouter implements PravegaEventRouter<String> {
    @Override
    public String getRoutingKey(String sensorReading) {
        return UUID.randomUUID().toString();
    }
}
