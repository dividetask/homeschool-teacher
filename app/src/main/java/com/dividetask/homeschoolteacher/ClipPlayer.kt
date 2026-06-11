package com.dividetask.homeschoolteacher

import android.content.Context
import android.media.MediaPlayer

/**
 * Plays short bundled audio clips (raw resources) for lessons that use
 * recorded sound instead of text-to-speech — e.g. Letter Sounds, where
 * each problem plays a pre-cut word clip and asks for its first letter.
 *
 * Only one clip plays at a time: a new [play] releases whatever was
 * playing first, so a rapid sequence of problems never stacks audio. The
 * engine is a singleton, mirroring [Tts]; [init] just captures the
 * application context so screens can request a clip by resource id.
 */
object ClipPlayer {
    @Volatile
    private var appContext: Context? = null

    private var player: MediaPlayer? = null

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    /** Play [resId] from the start, replacing any clip already playing. */
    @Synchronized
    fun play(resId: Int) {
        release()
        if (resId == 0) return
        val ctx = appContext ?: return
        val mp = MediaPlayer.create(ctx, resId) ?: return
        mp.setOnCompletionListener { done ->
            synchronized(this) {
                if (player === done) {
                    done.release()
                    player = null
                }
            }
        }
        player = mp
        mp.start()
    }

    /** Stop and release the current clip, if any. */
    @Synchronized
    fun stopAll() {
        release()
    }

    private fun release() {
        player?.let { mp ->
            runCatching { mp.stop() }
            mp.release()
        }
        player = null
    }
}
