package com.francescozoccheddu.tdmclient.data.operation

class FakeSensor(val measurement: SensorDriver.Measurement) : SensorDriver.Sensor {
    override fun measure(): SensorDriver.Measurement {
        return measurement
    }
}