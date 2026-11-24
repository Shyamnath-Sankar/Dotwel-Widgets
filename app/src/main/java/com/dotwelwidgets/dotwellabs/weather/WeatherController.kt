package com.dotwelwidgets.dotwellabs.weather

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process-wide off-main weather work. Every location/unit/disable change calls
 * [invalidate] or [cancelPending]; an in-flight response is ignored and cannot
 * commit because [WeatherRepository.fetch] also checks the generation guard
 * before writing the cache.
 */
object WeatherController {
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)
    private val searchGeneration = AtomicInteger(0)
    private val futures: MutableSet<Future<*>> =
        Collections.synchronizedSet(mutableSetOf<Future<*>>())

    fun invalidate() {
        generation.incrementAndGet()
    }

    fun cancelPending() {
        generation.incrementAndGet()
        searchGeneration.incrementAndGet()
        synchronized(futures) {
            futures.forEach { it.cancel(true) }
            futures.clear()
        }
    }

    fun search(query: String, callback: (locations: List<GeoLocation>?, error: String?) -> Unit) {
        val token = searchGeneration.incrementAndGet()
        val future = executor.submit {
            val outcome = runCatching { WeatherRepository.searchCities(query) }
            main.post {
                if (searchGeneration.get() == token) {
                    callback(outcome.getOrNull(), outcome.exceptionOrNull()?.message)
                }
            }
        }
        futures.add(future)
    }

    fun fetch(
        context: Context,
        auto: Boolean,
        callback: (WeatherResult) -> Unit,
    ): Boolean {
        val appContext = context.applicationContext
        if (!WeatherSettings.isEnabled(appContext)) return false
        val location = WeatherSettings.location(appContext) ?: return false
        val unit = WeatherSettings.unit(appContext)
        if (!auto) {
            WeatherSettings.markAutoAttempt(appContext, System.currentTimeMillis())
        }
        val token = generation.incrementAndGet()
        val future = executor.submit {
            val result = WeatherRepository.fetch(
                context = appContext,
                location = location,
                unit = unit,
                now = System.currentTimeMillis(),
                shouldCommit = {
                    generation.get() == token &&
                        WeatherSettings.isEnabled(appContext) &&
                        WeatherSettings.location(appContext) == location &&
                        WeatherSettings.unit(appContext) == unit
                },
            )
            main.post {
                if (generation.get() == token) {
                    callback(result)
                }
            }
        }
        futures.add(future)
        return true
    }
}
