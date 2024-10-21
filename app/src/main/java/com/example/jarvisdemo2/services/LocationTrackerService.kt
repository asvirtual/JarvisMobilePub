package com.example.jarvisdemo2.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.utilities.Constants.makeToast
import org.json.JSONObject
import java.util.*

class LocationTrackerService: Service() {

    private var destinationCoordinates: Pair<Double, Double>? = null
    private lateinit var locationManager: LocationManager
    private var currentLocation: Pair<Double, Double> = Pair(Double.NaN, Double.NaN)
    private lateinit var timer: Timer

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createNotificationChannel()

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Jarvis - Posizione")
                .setContentText("Tracciando la posizione in tempo reale")
                .setSmallIcon(R.drawable.ic_location)
                .build()

        startForeground(1234568786, notification)

        timer = Timer()
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val sharedPrefs = application.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
        val destination = intent?.getStringExtra("destination")
        val queue = Volley.newRequestQueue(applicationContext)

        queue.add(
            JsonObjectRequest(
                Request.Method.GET, "https://api.opencagedata.com/geocode/v1/json?q=$destination+${sharedPrefs.getString("city", "Trieste")}&key=${Constants.GEOCODING_API_KEY}", null,
                { response ->
                    destinationCoordinates = try {
                        val coordinates = (response.getJSONArray("results")[0] as JSONObject).getJSONObject("geometry")
                        Pair(coordinates.getDouble("lat"), coordinates.getDouble("lng"))
                    } catch (e: Exception) {
                        makeToast(applicationContext, e.message.toString())
                        null
                    }
                    if (destinationCoordinates != null) {
                        makeToast(
                            applicationContext,
                            "Destination Coordinates: ${destinationCoordinates!!.first}, ${destinationCoordinates!!.second}"
                        )
                        Handler().postDelayed({
                            checkNewLocation()
                        }, 1000)
                    } else {
                        makeToast(applicationContext, "null")
                        stopSelf()
                    }
                },
                { error ->
                    makeToast(applicationContext, "Error ${error.message.toString()} trying to geocode location $destination")
                }
            )
        )

        return START_STICKY
    }

    inner class Location: LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            makeToast(applicationContext, "Location update received!")
            currentLocation = Pair(location.latitude, location.longitude)
            /**
            TODO handle location change
             */
        }

    }

    @SuppressLint("MissingPermission")
    private fun checkNewLocation() {
        /**
         * TODO: Fix problems with getting current location!
         */
        try {
            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            val lastKnownLocation = locationManager.getLastKnownLocation(provider)
            if (lastKnownLocation != null) currentLocation = Pair(lastKnownLocation.latitude, lastKnownLocation.longitude)
            try {
                if (!currentLocation.first.isNaN() && !currentLocation.second.isNaN()) {
                    val distance: FloatArray = floatArrayOf(0F)
                    android.location.Location.distanceBetween(
                        currentLocation.first,
                        currentLocation.second,
                        destinationCoordinates!!.first,
                        destinationCoordinates!!.second,
                        distance
                    )
                    makeToast(
                        applicationContext,
                        "Current location: ${currentLocation.first}, ${currentLocation.second}\n Destination: ${destinationCoordinates?.first}, ${destinationCoordinates?.second}"
                    )
                    if (distance[0] < 10) {
                        makeToast(applicationContext, "Destination reached!")
                        val notification =
                            NotificationCompat.Builder(this, CHANNEL_ID)
                                .setContentTitle("Jarvis - Destinazione raggiunta")
                                .setContentText("Destinazione raggiunta alle ${Calendar.getInstance().get(Calendar.HOUR)} e ${Calendar.getInstance().get(Calendar.MINUTE)}!")
                                .setSmallIcon(R.drawable.ic_location)
                                .build()

                        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1834361531, notification)
                        stopSelf()
                    }
                    makeToast(applicationContext, "Distance from destination: ${distance[0]}")
                } else makeToast(applicationContext, "Can't get current nor last known position")
            } catch (e: Exception) {
                makeToast(applicationContext, e.message.toString())
            }

            locationManager.requestLocationUpdates(provider, 100, 1F, Location())
            Handler().postDelayed({
                checkNewLocation()
            }, 10000)
        } catch (e: Exception) {
            makeToast(applicationContext, e.message.toString())
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "SpeechRecognition",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(notificationChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "location_tracker_channel"
    }
}