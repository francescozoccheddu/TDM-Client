package com.francescozoccheddu.tdmclient.ui

import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.getDirections
import com.francescozoccheddu.tdmclient.utils.data.point
import kotlinx.android.synthetic.main.testactivity.bt_route
import kotlinx.android.synthetic.main.testactivity.tv_connected
import kotlinx.android.synthetic.main.testactivity.tv_coverage
import kotlinx.android.synthetic.main.testactivity.tv_locatable
import kotlinx.android.synthetic.main.testactivity.tv_location
import kotlinx.android.synthetic.main.testactivity.tv_locationInArea
import kotlinx.android.synthetic.main.testactivity.tv_online
import kotlinx.android.synthetic.main.testactivity.tv_score

class TestActivity : AppCompatActivity() {

    private lateinit var service: MainService

    private val connection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            println("TESTACTIVITY: Disconnected")
            Toast.makeText(this@TestActivity, "Disconnected", Toast.LENGTH_SHORT).show()
            service.onConnectedChange -= this@TestActivity::onConnectionChange
            service.onOnlineChange -= this@TestActivity::onOnlineChange
            service.onLocatableChange -= this@TestActivity::onLocatableChange
            service.onCoverageDataChange -= this@TestActivity::onCoverageDataChange
            service.onScoreChange -= this@TestActivity::onScoreChange
            service.onLocationChange -= this@TestActivity::onLocationChange
            bt_route.isEnabled = false
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            println("TESTACTIVITY: Connected")
            service = (binder as MainService.Binding).service
            service.onConnectedChange += this@TestActivity::onConnectionChange
            service.onOnlineChange += this@TestActivity::onOnlineChange
            service.onLocatableChange += this@TestActivity::onLocatableChange
            service.onCoverageDataChange += this@TestActivity::onCoverageDataChange
            service.onScoreChange += this@TestActivity::onScoreChange
            service.onLocationChange += this@TestActivity::onLocationChange
            onConnectionChange(service)
            onOnlineChange(service)
            onLocatableChange(service)
            onCoverageDataChange(service)
            onScoreChange(service)
            onLocationChange(service)
            bt_route.isEnabled = true
        }

    }

    private fun onConnectionChange(service: MainService) {
        tv_connected.change("Connected=${service.connected}")
    }

    private fun onOnlineChange(service: MainService) {
        tv_online.change("Online=${service.online}")
    }

    private fun onLocationChange(service: MainService) {
        tv_location.change("Location=${service.location?.latitude};${service.location?.longitude}")
        tv_locationInArea.change("InArea=${service.insideMeasurementArea}")
    }

    private fun onLocatableChange(service: MainService) {
        tv_locatable.change("Locatable=${service.locatable}")
    }

    private fun onCoverageDataChange(service: MainService) {
        tv_coverage.change("Coverage=${service.coverageData != null}")
    }

    private fun onScoreChange(service: MainService) {
        tv_score.change("Score=${service.score}")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testactivity)
        bt_route.setOnClickListener {
            bt_route.isEnabled = false
            service.requestRoute(null, 3600f) { req, res ->
                if (res != null)
                    getDirections(req.from.point, res) {
                        val ok = it != null
                        Toast.makeText(this@TestActivity, "Directions=$ok", Toast.LENGTH_SHORT).show()
                        bt_route.isEnabled = true
                    }
                else
                    bt_route.isEnabled = true
                val ok = res != null
                Toast.makeText(this@TestActivity, "Route=$ok", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun TextView.change(text: String) {
        this.text = text
        setTextColor(Color.RED)
        postDelayed({
            setTextColor(Color.BLACK)
        }, 1000)
    }

    override fun onResume() {
        super.onResume()
        println("TESTACTIVITY: Resume")
        MainService.bind(this, connection)
        println("TESTACTIVITY: Bind requested")
    }

    override fun onPause() {
        super.onPause()
        println("TESTACTIVITY: Pause")
        if (this::service.isInitialized) {
            service.onConnectedChange -= this@TestActivity::onConnectionChange
            service.onOnlineChange -= this@TestActivity::onOnlineChange
            service.onLocatableChange -= this@TestActivity::onLocatableChange
            service.onCoverageDataChange -= this@TestActivity::onCoverageDataChange
            service.onScoreChange -= this@TestActivity::onScoreChange
            service.onLocationChange -= this@TestActivity::onLocationChange
        }
        unbindService(connection)
        println("TESTACTIVITY: Unbind requested")
    }


}