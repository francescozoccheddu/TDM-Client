package com.francescozoccheddu.tdmclient.data

class FakeSensor(val measurement: Measurement) :
    SensorDriver.Sensor {
    override fun measure(): Measurement {
        return measurement
    }
}