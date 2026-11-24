package com.dotwelwidgets.dotwellabs.media

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dotwelwidgets.dotwellabs.WidgetUpdater

/**
 * Shared bridge between Android media sessions and the Dotwel media widgets.
 *
 * The service is declared with BIND_NOTIFICATION_LISTENER_SERVICE and is only
 * bound by Android after the installer explicitly enables notification access
 * in the system settings screen. No polling loop, foreground service or
 * background network is used. Notification text and cover art are held in
 * process memory only and cleared on disconnect or revoke.
 */
class MediaNotificationService : NotificationListenerService() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: MediaNotificationService? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaSessionRepository.onAccessChanged(applicationContext, true)
    }

    override fun onListenerDisconnected() {
        MediaSessionRepository.onAccessChanged(applicationContext, false)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        MediaSessionRepository.onNotificationPosted(applicationContext, sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        MediaSessionRepository.onNotificationRemoved(applicationContext, sbn)
    }
}

object MediaSessionRepository {
    private const val TAG = "DotwelMedia"

    @Volatile
    private var cachedNotification: CachedNotification? = null

    private data class CachedNotification(
        val key: String,
        val title: String?,
        val text: String?,
        val appLabel: String?,
        val postedAtMillis: Long,
    )

    fun onAccessChanged(context: Context, granted: Boolean) {
        if (!granted) cachedNotification = null
        WidgetUpdater.updateMediaProviders(context)
    }

    fun onNotificationPosted(context: Context, sbn: StatusBarNotification) {
        if (sbn.packageName == context.packageName) return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()?.trim()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return
        val appLabel = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrNull()
        cachedNotification = CachedNotification(
            key = sbn.key,
            title = title,
            text = text,
            appLabel = appLabel,
            postedAtMillis = sbn.postTime,
        )
        WidgetUpdater.updateMediaProviders(context)
    }

    fun onNotificationRemoved(context: Context, sbn: StatusBarNotification) {
        if (cachedNotification?.key == sbn.key) {
            cachedNotification = null
            WidgetUpdater.updateMediaProviders(context)
        }
    }

    fun clear() {
        cachedNotification = null
    }

    /**
     * Clears the latest cached notification. If Android still has the listener
     * bound, the same notification is also dismissed through the supported
     * NotificationListenerService API. No other notification is touched.
     */
    fun clearLatestNotification(context: Context) {
        val cached = cachedNotification
        cachedNotification = null
        if (cached != null) {
            runCatching {
                MediaNotificationService.instance?.cancelNotification(cached.key)
            }
        }
        WidgetUpdater.updateMediaProviders(context)
    }

    fun snapshot(context: Context, nowMillis: Long = System.currentTimeMillis()): MediaSnapshot {
        val access = MediaAccess.isGranted(context)
        if (!access) {
            cachedNotification = null
            return MediaSnapshot.noAccess()
        }
        val controller = bestController(context)
        val showText = MediaPrefs.showNotificationText(context)
        val notification = cachedNotification?.let { cached ->
            MediaNotification(
                title = if (showText) cached.title else cached.title?.let(MediaPrivacy::mask),
                text = if (showText) cached.text else cached.text?.let(MediaPrivacy::mask),
                appLabel = cached.appLabel,
                postedAtMillis = cached.postedAtMillis,
            )
        }
        if (controller == null) {
            return MediaSnapshot.noAccess().copy(
                accessGranted = true,
                notification = notification,
                capturedAtMillis = nowMillis,
            )
        }
        val metadata = runCatching { controller.metadata }.getOrNull()
        val playback = runCatching { controller.playbackState }.getOrNull()
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val art = runCatching {
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        }.getOrNull()
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val actions = playback?.actions ?: 0L
        val state = playback?.state ?: PlaybackState.STATE_NONE
        val playing = state == PlaybackState.STATE_PLAYING ||
            state == PlaybackState.STATE_BUFFERING
        val position = extrapolatePosition(playback, duration)
        return MediaSnapshot(
            accessGranted = true,
            hasSession = true,
            title = title,
            artist = artist,
            album = album,
            packageName = controller.packageName,
            appLabel = appLabel(context, controller.packageName),
            albumArt = art,
            isPlaying = playing,
            playbackState = state,
            positionMillis = position,
            durationMillis = duration.coerceAtLeast(0L),
            canPlayPause = actions and (
                PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE
                ) != 0L,
            canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            capturedAtMillis = nowMillis,
            notification = notification,
        )
    }

    fun playerIntent(context: Context): Intent? {
        val controller = bestController(context) ?: return null
        return runCatching {
            context.packageManager.getLaunchIntentForPackage(controller.packageName)
        }.getOrNull()?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun perform(context: Context, action: MediaTransportAction): Boolean {
        val controller = bestController(context) ?: return false
        val controls = controller.transportControls ?: return false
        val ok = runCatching {
            when (action) {
                MediaTransportAction.PLAY_PAUSE -> {
                    if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        controls.pause()
                    } else {
                        controls.play()
                    }
                }
                MediaTransportAction.PREVIOUS -> controls.skipToPrevious()
                MediaTransportAction.NEXT -> controls.skipToNext()
            }
        }.isSuccess
        if (ok) WidgetUpdater.updateMediaProviders(context)
        return ok
    }

    private fun bestController(context: Context): MediaController? {
        if (!MediaAccess.isGranted(context)) return null
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return null
        val component = ComponentName(context, MediaNotificationService::class.java)
        val sessions = try {
            manager.getActiveSessions(component)
        } catch (error: SecurityException) {
            Log.w(TAG, "Media session access unavailable")
            return null
        } catch (error: RuntimeException) {
            Log.w(TAG, "Media session query failed")
            return null
        }
        if (sessions.isEmpty()) return null
        return sessions.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: sessions.firstOrNull {
            it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null
        } ?: sessions.first()
    }

    private fun extrapolatePosition(state: PlaybackState?, duration: Long): Long {
        if (state == null) return 0L
        val base = state.position.coerceAtLeast(0L)
        val playing = state.state == PlaybackState.STATE_PLAYING
        if (!playing) return clamp(base, duration)
        val elapsed = (SystemClock.elapsedRealtime() - state.lastPositionUpdateTime)
            .coerceAtLeast(0L)
        val speed = state.playbackSpeed.takeIf { it > 0f } ?: 1f
        return clamp(base + (elapsed * speed).toLong(), duration)
    }

    private fun clamp(position: Long, duration: Long): Long =
        if (duration > 0L) position.coerceIn(0L, duration) else position

    private fun appLabel(context: Context, packageName: String): String? = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

}

internal object MediaPrivacy {
    fun mask(value: String): String = value.map { char ->
        if (char.isWhitespace()) char else '\u2022'
    }.joinToString("")
}

enum class MediaTransportAction {
    PLAY_PAUSE,
    PREVIOUS,
    NEXT,
}

class MediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CLEAR_NOTIFICATION) {
            MediaSessionRepository.clearLatestNotification(context)
            return
        }
        if (intent.action != ACTION) return
        val action = parseAction(intent.getStringExtra(EXTRA_TRANSPORT)) ?: return
        MediaSessionRepository.perform(context, action)
    }

    companion object {
        const val ACTION = "com.dotwelwidgets.dotwellabs.action.MEDIA_TRANSPORT"
        const val ACTION_CLEAR_NOTIFICATION =
            "com.dotwelwidgets.dotwellabs.action.MEDIA_CLEAR_NOTIFICATION"
        const val EXTRA_TRANSPORT = "com.dotwelwidgets.dotwellabs.extra.MEDIA_TRANSPORT"

        fun parseAction(raw: String?): MediaTransportAction? = runCatching {
            raw?.let(MediaTransportAction::valueOf)
        }.getOrNull()
    }
}
