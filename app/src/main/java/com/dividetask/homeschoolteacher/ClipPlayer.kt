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

    /**
     * Play [resId] from the start, replacing any clip already playing.
     * Returns the clip's length in milliseconds (0 if it couldn't be
     * played), so callers can wait long enough to let it finish.
     */
    @Synchronized
    fun play(resId: Int): Int {
        release()
        if (resId == 0) return 0
        val ctx = appContext ?: return 0
        val mp = MediaPlayer.create(ctx, resId) ?: return 0
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
        return mp.duration.coerceAtLeast(0)
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
