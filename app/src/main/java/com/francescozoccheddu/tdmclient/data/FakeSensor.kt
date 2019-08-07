package com.francescozoccheddu.tdmclient.data

class FakeSensor(val measurement: SensorDriver.Measurement) :
    SensorDriver.Sensor {
    override fun measure(): SensorDriver.Measurement {
        return measurement
    }
}