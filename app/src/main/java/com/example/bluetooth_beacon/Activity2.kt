package com.example.bluetooth_beacon

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_2.*

class Activity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_2)

        val data = intent.getIntegerArrayListExtra("DATA")
        Log.d("DBG", "final data = $data")
        if (data != null && data.isNotEmpty()) {
            val datapoints = Array(data.size) { i ->
                DataPoint(
                    i.toDouble(),
                    data[i].toDouble()
                )
            }

            Log.d("DBG", "Graph activity ${datapoints.size}")
            graph.viewport.isScalable = true
            graph.viewport.isScrollable = true
            graph.gridLabelRenderer.horizontalAxisTitle = "Time"
            graph.gridLabelRenderer.verticalAxisTitle = "BPM"
            graph.addSeries(LineGraphSeries<DataPoint>(datapoints))
        }
    }
}