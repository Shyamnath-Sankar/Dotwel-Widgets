package com.dotwelwidgets.dotwellabs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.dotwelwidgets.dotwellabs.weather.*
import java.time.Instant
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

/** Explicit synthetic replay, never a live API success claim. */
class RepairWeatherActivity:Activity() {
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        if(intent.getBooleanExtra("clear",false)) WeatherController.disable(this,true)
        else {
            val now=System.currentTimeMillis()
            val city=GeoLocation("Synthetic City","IN",null,11.0,77.0,"Asia/Kolkata")
            WeatherController.enable(this)
            WeatherController.selectCity(this,city)
            val date=Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()
            val daily=JSONObject().apply {
                put("time",JSONArray((0..5).map { date.plusDays(it.toLong()).toString() }))
                put("weather_code",JSONArray(listOf(2,61,0,3,80,1)))
                put("temperature_2m_max",JSONArray(listOf(29,30,31,28,27,30)))
                put("temperature_2m_min",JSONArray(listOf(20,21,22,20,19,21)))
                put("sunrise",JSONArray((0..5).map { date.plusDays(it.toLong()).atTime(6,0).atZone(ZoneId.of("Asia/Kolkata")).toEpochSecond() }))
                put("sunset",JSONArray((0..5).map { date.plusDays(it.toLong()).atTime(18,0).atZone(ZoneId.of("Asia/Kolkata")).toEpochSecond() }))
                put("precipitation_probability_max",JSONArray(listOf(30,70,0,40,80,5)))
            }
            val forecast=JSONObject().put("current",JSONObject().put("temperature_2m",27.0).put("apparent_temperature",28.0).put("weather_code",2).put("is_day",1).put("time",now/1000)).put("daily",daily).toString()
            WeatherCache.save(this,forecast,city,TemperatureUnit.CELSIUS,now,WeatherAirQuality(65,now))
        }
        WidgetUpdater.updateAll(this)
        setContentView(android.widget.TextView(this).apply { text="Synthetic weather fixture. No network request." })
        finish()
    }
}
