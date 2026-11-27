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
 * Process-wide off-main weather work. Consent, selected city, unit, generation
 * and cache mutations share [WeatherSettings.STATE_LOCK]. The lock is held only
 * around short state checks/transactions, never across network I/O.
 *
 * Every disable/location/unit change bumps the generation and clears the cache
 * inside the lock, so an in-flight response can neither start a new request nor
 * write cached data after the user revokes consent.
 */
object WeatherController {
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)
    private val searchGeneration = AtomicInteger(0)
    private val futures: MutableSet<Future<*>> =
        Collections.synchronizedSet(mutableSetOf<Future<*>>())

    fun invalidate() {
        synchronized(WeatherSettings.STATE_LOCK) {
            generation.incrementAndGet()
        }
    }

    /** Enables consent under the state lock before any request may start. */
    fun enable(context: Context) {
        val appContext = context.applicationContext
        synchronized(WeatherSettings.STATE_LOCK) {
            WeatherSettings.setEnabled(appContext, true)
            generation.incrementAndGet()
        }
    }

    fun cancelPending() {
        synchronized(WeatherSettings.STATE_LOCK) {
            generation.incrementAndGet()
            searchGeneration.incrementAndGet()
        }
        cancelOutstanding()
    }

    /**
     * Turns weather off, deleting the selected city, last failure and cached
     * forecast data as promised by the settings dialog. When [clearUnit] is set
     * the unit preference is removed too (full delete).
     */
    fun disable(context: Context, clearUnit: Boolean = false) {
        val appContext = context.applicationContext
        val plan = WeatherClearPlan.disable(clearUnit)
        synchronized(WeatherSettings.STATE_LOCK) {
            WeatherSettings.setEnabled(appContext, false)
            if (plan.clearLocation) WeatherSettings.clearLocation(appContext)
            if (plan.clearUnit) WeatherSettings.clearUnit(appContext)
            if (plan.clearFailure) WeatherSettings.clearFailure(appContext)
            if (plan.clearCache) WeatherCache.clear(appContext)
            generation.incrementAndGet()
            searchGeneration.incrementAndGet()
        }
        cancelOutstanding()
    }

    /** Applies a manually selected city only while weather is already enabled. */
    fun selectCity(context: Context, location: GeoLocation): Boolean {
        val appContext = context.applicationContext
        val plan = WeatherClearPlan.selectCity()
        val accepted = synchronized(WeatherSettings.STATE_LOCK) {
            if (!WeatherSettings.isEnabled(appContext)) {
                false
            } else {
                WeatherSettings.setLocation(appContext, location)
                if (plan.clearFailure) WeatherSettings.clearFailure(appContext)
                if (plan.clearCache) WeatherCache.clear(appContext)
                generation.incrementAndGet()
                searchGeneration.incrementAndGet()
                true
            }
        }
        if (accepted) cancelOutstanding()
        return accepted
    }

    /** Applies a unit change, clearing cache and failure inside the same lock. */
    fun changeUnit(context: Context, unit: TemperatureUnit) {
        val appContext = context.applicationContext
        val plan = WeatherClearPlan.changeUnit()
        synchronized(WeatherSettings.STATE_LOCK) {
            WeatherSettings.setUnit(appContext, unit)
            if (plan.clearFailure) WeatherSettings.clearFailure(appContext)
            if (plan.clearCache) WeatherCache.clear(appContext)
            generation.incrementAndGet()
            searchGeneration.incrementAndGet()
        }
        cancelOutstanding()
    }

    fun search(context: Context, query: String, callback: (List<GeoLocation>?) -> Unit) {
        val appContext = context.applicationContext
        val token = synchronized(WeatherSettings.STATE_LOCK) {
            if (!WeatherSettings.isEnabled(appContext)) {
                main.post { callback(emptyList()) }
                return
            }
            searchGeneration.incrementAndGet()
        }
        val future = executor.submit {
            val allowed = synchronized(WeatherSettings.STATE_LOCK) {
                searchGeneration.get() == token && WeatherSettings.isEnabled(appContext)
            }
            if (!allowed) return@submit
            val outcome = runCatching { WeatherRepository.searchCities(query) }
            main.post {
                val current = synchronized(WeatherSettings.STATE_LOCK) {
                    searchGeneration.get() == token && WeatherSettings.isEnabled(appContext)
                }
                if (current) callback(outcome.getOrNull())
            }
        }
        track(future)
    }

    fun fetch(
        context: Context,
        auto: Boolean,
        callback: (WeatherResult) -> Unit,
    ): Boolean {
        val appContext = context.applicationContext
        val token: Int
        val location: GeoLocation
        val unit: TemperatureUnit
        synchronized(WeatherSettings.STATE_LOCK) {
            if (!WeatherSettings.isEnabled(appContext)) return false
            location = WeatherSettings.location(appContext) ?: return false
            unit = WeatherSettings.unit(appContext)
            token = generation.incrementAndGet()
        }
        if (!auto) {
            WeatherSettings.markAutoAttempt(appContext, System.currentTimeMillis())
        }
        val shouldCommit = {
            synchronized(WeatherSettings.STATE_LOCK) {
                WeatherAccessPolicy(
                    enabled = WeatherSettings.isEnabled(appContext),
                    location = WeatherSettings.location(appContext),
                    unit = WeatherSettings.unit(appContext),
                    generation = generation.get(),
                ).mayCommit(token, location, unit)
            }
        }
        val future = executor.submit {
            val result = WeatherRepository.fetch(
                context = appContext,
                location = location,
                unit = unit,
                now = System.currentTimeMillis(),
                shouldCommit = shouldCommit,
            )
            synchronized(WeatherSettings.STATE_LOCK) {
                if (generation.get() == token &&
                    WeatherSettings.isEnabled(appContext) &&
                    WeatherSettings.location(appContext) == location
                ) {
                    if (result is WeatherResult.Failure &&
                        result.reason != WeatherResult.Reason.CANCELLED
                    ) {
                        WeatherSettings.recordFailure(
                            appContext,
                            location,
                            result.reason,
                            System.currentTimeMillis(),
                        )
                    } else if (result is WeatherResult.Success) {
                        WeatherSettings.clearFailure(appContext)
                    }
                }
            }
            main.post {
                val current = synchronized(WeatherSettings.STATE_LOCK) {
                    generation.get() == token && WeatherSettings.isEnabled(appContext)
                }
                if (current) callback(result)
            }
        }
        track(future)
        return true
    }

    private fun track(future: Future<*>) {
        synchronized(futures) {
            futures.removeIf { it.isDone }
            futures.add(future)
        }
    }

    private fun cancelOutstanding() {
        val snapshot = synchronized(futures) {
            val copy = futures.toList()
            futures.clear()
            copy
        }
        snapshot.forEach { it.cancel(true) }
    }
}
