package com.dividetask.homeschoolteacher

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Text-to-speech that pronounces each request three times in sequence at
 * 1.0×, 0.5×, then 0.25× speed. Initialization is async; speech requests
 * that arrive before the engine is ready are queued and flushed once it
 * comes up. Calling speak() again cancels any in-flight sequence.
 */
object Tts {
    // Each speak() request plays the text three times in a row at these
    // rates. The slowest pass is one-eighth normal speed so kids hear each
    // syllable distinctly.
    private val RATES: List<Float> = listOf(1.0f, 0.5f, 0.125f)

    // Pause between successive utterances within one speak() request, so
    // the listener gets a clear gap before the next, slower pronunciation.
    private const val GAP_BETWEEN_RATES_MS: Long = 1000L

    // Delay after stop() before the first speak() of a new sequence. Many
    // Android TTS engines clip the first 100-300 ms of an utterance when
    // it's queued immediately after a stop() — this gives the engine time
    // to settle so the first word is heard in full.
    private const val POST_STOP_SETTLE_MS: Long = 250L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var engine: TextToSpeech? = null

    @Volatile
    private var ready: Boolean = false

    private var pending: String? = null
    private var currentSequence: Job? = null

    // utteranceId -> deferred that completes when the engine reports the
    // utterance finished (or errored).
    private val pendingUtterances = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    fun init(context: Context) {
        if (engine != null) return
        val app = context.applicationContext
        engine = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val e = engine ?: return@TextToSpeech
                e.language = Locale.US
                e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) = Unit

                    override fun onDone(utteranceId: String) {
                        pendingUtterances.remove(utteranceId)?.complete(Unit)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String) {
                        pendingUtterances.remove(utteranceId)?.complete(Unit)
                    }

                    override fun onError(utteranceId: String, errorCode: Int) {
                        pendingUtterances.remove(utteranceId)?.complete(Unit)
                    }
                })
                ready = true
                val queued = pending
                pending = null
                if (queued != null) speak(queued)
            }
        }
    }

    /**
     * Immediately silence everything: cancels any in-flight speak sequence,
     * halts the engine mid-utterance, and drops any queued request. Lesson
     * screens call this after their post-answer delay, right before
     * advancing, so audio from a finished problem never bleeds into the
     * next lesson.
     */
    fun stopAll() {
        pending = null
        currentSequence?.cancel()
        currentSequence = null
        engine?.stop()
        pendingUtterances.values.forEach { it.complete(Unit) }
        pendingUtterances.clear()
    }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        currentSequence?.cancel()
        currentSequence = scope.launch {
            val e = engine ?: return@launch
            e.stop()
            // Resolve any utterances the stop() preempted so we don't leak.
            pendingUtterances.values.forEach { it.complete(Unit) }
            pendingUtterances.clear()
            kotlinx.coroutines.delay(POST_STOP_SETTLE_MS)

            RATES.forEachIndexed { i, rate ->
                val id = "homeschool-tts-${System.nanoTime()}-$i"
                val completion = CompletableDeferred<Unit>()
                pendingUtterances[id] = completion
                e.setSpeechRate(rate)
                val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val result = e.speak(text, mode, null, id)
                if (result != TextToSpeech.SUCCESS) {
                    pendingUtterances.remove(id)
                    return@forEachIndexed
                }
                completion.await()
                if (i < RATES.size - 1) {
                    kotlinx.coroutines.delay(GAP_BETWEEN_RATES_MS)
                }
            }
        }
    }

    /**
     * Speak a list of words at every [RATES] rate in sequence — so for
     * `["ball", "bed", "bat"]` the engine says:
     *   ball, bed, bat (1.0x)  →  pause  →  ball, bed, bat (0.5x)
     *   pause  →  ball, bed, bat (0.125x).
     * Used by the Phonemes lesson where each problem is a trio of words
     * sharing a first sound.
     */
    fun speakAll(words: List<String>) {
        if (words.isEmpty()) return
        if (!ready) {
            // Fall back to queuing the first word at the slowest rate; this
            // is rare (only fires if the engine hasn't initialized by the
            // time the first problem composes).
            pending = words.first()
            return
        }
        currentSequence?.cancel()
        currentSequence = scope.launch {
            val e = engine ?: return@launch
            e.stop()
            pendingUtterances.values.forEach { it.complete(Unit) }
            pendingUtterances.clear()
            kotlinx.coroutines.delay(POST_STOP_SETTLE_MS)

            var firstUtterance = true
            for ((rateIdx, rate) in RATES.withIndex()) {
                e.setSpeechRate(rate)
                for ((wordIdx, word) in words.withIndex()) {
                    val id = "homeschool-tts-seq-${System.nanoTime()}-$rateIdx-$wordIdx"
                    val completion = CompletableDeferred<Unit>()
                    pendingUtterances[id] = completion
                    val mode = if (firstUtterance) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    firstUtterance = false
                    val result = e.speak(word, mode, null, id)
                    if (result != TextToSpeech.SUCCESS) {
                        pendingUtterances.remove(id)
                        continue
                    }
                    completion.await()
                    if (wordIdx < words.size - 1) {
                        kotlinx.coroutines.delay(500L)
                    }
                }
                if (rateIdx < RATES.size - 1) {
                    kotlinx.coroutines.delay(GAP_BETWEEN_RATES_MS)
                }
            }
        }
    }
}
