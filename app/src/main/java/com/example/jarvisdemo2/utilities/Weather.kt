package com.example.jarvisdemo2.utilities

import android.content.Context
import android.widget.Toast
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashMap
import kotlin.math.truncate

object Weather {
    fun getWeather(context: Context, callback: WeatherCallback, cityName: String? = null): String {
        val sharedPrefs = context.getSharedPreferences("jarvis", Context.MODE_PRIVATE)
        if (sharedPrefs.getString("city", null) != null) {
            val queue = Volley.newRequestQueue(context)
            val url = "https://api.openweathermap.org/data/2.5/weather?q=${cityName ?: sharedPrefs.getString("city", null)}&appid=${Constants.WEATHER_API_KEY}&lang=it"
            var toReturn = ""
            queue.add(object : JsonObjectRequest(
                Method.GET, url, null,
                { response ->
                    val jsonRes = JSONObject(response.toString()).toMap()
                    toReturn = "Oggi il meteo a ${cityName ?: sharedPrefs.getString("city", null)} è ${(jsonRes["weather"] as List<Map<String, Any>>)[0]["description"]}, la temperatura minima è di  ${
                        truncate(((jsonRes["main"] as Map<String, Any>)["temp_min"]).toString().toDouble() - 275.15).toInt()
                    } gradi e quella massima è di  ${
                        truncate(((jsonRes["main"] as Map<String, Any>)["temp_max"]).toString().toDouble() - 275.15).toInt()
                    } gradi, mentre la temperatura percepita è di ${
                        truncate(((jsonRes["main"] as Map<String, Any>)["feels_like"]).toString().toDouble() - 275.15).toInt()
                    } gradi. Il vento soffia a ${truncate((jsonRes["wind"] as Map<String, Any>)["speed"].toString().toDouble()) * 3.6} chilometri orari e l'umidità è al ${(jsonRes["main"] as Map<String, Any>)["humidity"]} percento"
                    callback.onResponse(toReturn)
                    // RETURN FROM INSIDE HERE
                },
                { err ->
                    Toast.makeText(
                        context,
                        "Error $err occured",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback.onError(err)
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer "
                    return headers
                }
            })
            return toReturn
            // Toast.makeText(this, "no location", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "no location", Toast.LENGTH_SHORT).show()
            return "no location"
        }
    }

    private fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith { it ->
        when (val value = this[it]) {
            is JSONArray -> {
                val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
                JSONObject(map).toMap().values.toList()
            }
            is JSONObject -> value.toMap()
            JSONObject.NULL -> null
            else -> value
        }
    }

    interface WeatherCallback {
        fun onResponse(res: String)
        fun onError(err: VolleyError)
    }
}