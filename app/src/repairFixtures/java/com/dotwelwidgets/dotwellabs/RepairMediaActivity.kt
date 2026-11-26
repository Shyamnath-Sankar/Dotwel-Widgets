package com.dotwelwidgets.dotwellabs

import android.app.*
import android.content.Intent
import android.graphics.*
import android.media.*
import android.media.session.*
import android.os.*
import java.io.File

/** Synthetic emulator media only. No audio, network, owner notifications or storage access. */
class RepairMediaActivity:Activity() {
    override fun onCreate(state:Bundle?) { super.onCreate(state);apply(intent) }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent);apply(intent) }
    private fun apply(input:Intent) {
        if(input.getBooleanExtra("stop",false)) {
            session?.isActive=false;session?.release();session=null;finish();return
        }
        val player=session ?: MediaSession(this,"DotwelSyntheticFixture").also { s ->
            session=s
            s.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            s.setCallback(object:MediaSession.Callback() {
                override fun onPlay() { log("play");publish(true) }
                override fun onPause() { log("pause");publish(false) }
                override fun onSkipToNext() { log("next");fixtureTitle="Fixture Next";publish(playing) }
                override fun onSkipToPrevious() { log("previous");fixtureTitle="Fixture Previous";publish(playing) }
            },Handler(Looper.getMainLooper()))
            s.setPlaybackToRemote(object:VolumeProvider(VOLUME_CONTROL_RELATIVE,10,6) {
                override fun onAdjustVolume(direction:Int) { log("volume:$direction");setCurrentVolume((currentVolume+direction).coerceIn(0,10)) }
            })
            s.setSessionActivity(PendingIntent.getActivity(this,9301,Intent(this,RepairMediaActivity::class.java),PendingIntent.FLAG_IMMUTABLE))
        }
        fixtureTitle=input.getStringExtra("title") ?: "Fixture Track"
        publish(input.getBooleanExtra("playing",true))
        setContentView(android.widget.TextView(this).apply { text="Synthetic media fixture: $fixtureTitle" })
    }
    private fun publish(play:Boolean) {
        val player=session ?: return
        playing=play
        // Small art keeps the metadata binder transaction well below its limit.
        val art=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
        Canvas(art).apply {
            drawColor(Color.BLUE)
            drawRect(0f,0f,86f,256f,Paint().apply { color=Color.RED })
            drawRect(170f,0f,256f,256f,Paint().apply { color=Color.GREEN })
        }
        player.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,fixtureTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST,"Synthetic Artist")
            .putLong(MediaMetadata.METADATA_KEY_DURATION,240_000)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,art).build())
        player.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
            .setState(if(play) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,65_000,if(play)1f else 0f,SystemClock.elapsedRealtime()).build())
        // Active only after readable metadata and a transport state exist.
        player.isActive=true
        log("state:$fixtureTitle:$play")
    }
    private fun log(value:String) { File(noBackupFilesDir,"repair-media-events.txt").appendText("$value\n") }
    companion object {
        private var session:MediaSession?=null
        private var playing=true
        private var fixtureTitle="Fixture Track"
    }
}
