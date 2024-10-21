package com.example.jarvisdemo2.utilities

import android.content.Context
import android.location.*
import android.location.Location
import android.os.Bundle
import java.io.IOException
import java.util.*


/*---------- Listener class to get coordinates ------------- */
class Location(context: Context): LocationListener {

    private var sharedPrefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
    private val context = context

    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onLocationChanged(loc: Location) {
        /*------- To get city name from coordinates -------- */
        var cityName: String? = null
        val gcd = Geocoder(context, Locale.getDefault())
        val addresses: List<Address>
        try {
            addresses = gcd.getFromLocation(
                loc.latitude,
                loc.longitude, 1
            )
            if (addresses.isNotEmpty()) {
                cityName = addresses[0].locality
                sharedPrefs.edit().putString("city", cityName).apply()
                // Toast.makeText(context, cityName, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            // Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
        }
        /*val s = (longitude + "\n" + latitude + "\n\nMy Current City is: "
                + cityName)
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()*/
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
}