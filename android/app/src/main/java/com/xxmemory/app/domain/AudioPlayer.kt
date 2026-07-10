package com.xxmemory.app.domain

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 统一音频播放器：优先播放网络/本地音频文件，回退到 TTS。
 * 支持暂停/停止与自动释放。
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
     * 播放音频。如果 audioUrl 非空则使用 MediaPlayer，否则使用 TTS 朗读文本。
     */
    fun play(audioUrl: String?, fallbackText: String) {
        stop()
        if (!audioUrl.isNullOrBlank()) {
            playMedia(audioUrl)
        } else if (fallbackText.isNotBlank() && isTtsReady) {
            tts?.speak(fallbackText, TextToSpeech.QUEUE_FLUSH, null, null)
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
