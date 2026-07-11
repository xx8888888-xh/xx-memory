package com.xxmemory.app.domain

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.util.Locale

/**>
 * 统一音频播放器：仅播放用户上传的音频文件（audioUrl）。
 * 不再自动回退到 TTS，避免在复习时“自动朗读”内容。
 * 若需要朗读，由调用方显式调用 playTts。
 */
class AudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    var onCompletion: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
            if (isTtsReady) {
                val result = tts?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.CHINESE
                }
            }
        }
    }

    /**
     * 播放用户上传的音频文件。audioUrl 为空时不再自动 TTS 朗读。
     * 若调用方需要朗读，请使用 playTts。
     */
    fun play(audioUrl: String?, fallbackText: String) {
        stop()
        if (!audioUrl.isNullOrBlank()) {
            playMedia(audioUrl)
        }
    }

    fun playTts(text: String) {
        stopMedia()
        if (text.isBlank() || !isTtsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stop() {
        stopMedia()
        tts?.stop()
    }

    fun release() {
        stopMedia()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun playMedia(url: String) {
        stopMedia()
        try {
            val player = MediaPlayer().apply {
                setDataSource(appContext, Uri.parse(url))
                setOnPreparedListener { it.start() }
                setOnCompletionListener { onCompletion?.invoke() }
                setOnErrorListener { _, _, _ ->
                    onError?.invoke("音频播放失败")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "音频播放失败")
        }
    }

    private fun stopMedia() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }
}
