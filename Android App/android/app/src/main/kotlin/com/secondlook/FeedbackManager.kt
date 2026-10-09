package com.secondlook

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.secondlook.secondlook.R

object FeedbackManager {
    private const val TAG = "FeedbackManager"
    private var soundPool: SoundPool? = null
    private var dingSoundId: Int = 0
    private var isSoundLoaded: Boolean = false

    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return
        try {
            // Use USAGE_MEDIA so playback uses the user's active media volume
            // rather than the frequently muted system touch sound stream.
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            isSoundLoaded = true
                            Log.d(TAG, "SoundPool loaded ding sound id $sampleId successfully")
                        } else {
                            Log.w(TAG, "SoundPool failed to load sound sample id $sampleId with status $status")
                        }
                    }
                    dingSoundId = load(context.applicationContext, R.raw.warning_ding, 1)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SoundPool: ${e.message}")
        }
    }

    /**
     * Triggers distinct vibration and warning ding sound based on alert severity:
     * - Red / HIGH alert: Urgent double buzz with high-amplitude waveform + crisp ding
     * - Yellow / MEDIUM alert: Gentle single tap pulse + softer ding
     */
    fun notifyWarning(context: Context, isHighAlert: Boolean) {
        init(context)
        vibrate(context, isHighAlert)
        playDing(context, isHighAlert)
    }

    /**
     * Crisp, low-latency tactile vibration feedback for keyboard touch events (matching Gboard feel).
     */
    fun vibrateKeyPress(context: Context, view: android.view.View? = null) {
        if (!SettingsManager.isHapticFeedbackEnabled(context)) return

        @Suppress("DEPRECATION")
        val handled = view?.performHapticFeedback(
            android.view.HapticFeedbackConstants.KEYBOARD_TAP,
            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        ) ?: false

        if (handled) return

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null || !vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(14, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(14)
            }
        } catch (_: Exception) {}
    }

    private fun vibrate(context: Context, isHighAlert: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null || !vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isHighAlert) {
                    // Urgent red alert: strong double pulse
                    val timings = longArrayOf(0, 80, 60, 140)
                    val amplitudes = intArrayOf(0, 255, 0, 230)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    // Subtle yellow alert: gentle single tap pulse
                    vibrator.vibrate(VibrationEffect.createOneShot(55, 130))
                }
            } else {
                @Suppress("DEPRECATION")
                if (isHighAlert) {
                    vibrator.vibrate(longArrayOf(0, 80, 60, 140), -1)
                } else {
                    vibrator.vibrate(55)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun playDing(context: Context, isHighAlert: Boolean) {
        val volume = if (isHighAlert) 0.80f else 0.60f
        val rate = if (isHighAlert) 1.0f else 0.92f

        // 1. Try low-latency SoundPool first if already loaded
        val sp = soundPool
        if (sp != null && isSoundLoaded && dingSoundId != 0) {
            val streamId = sp.play(dingSoundId, volume, volume, 1, 0, rate)
            if (streamId != 0) {
                Log.d(TAG, "Played gentle chime via SoundPool (stream $streamId, vol $volume, isHigh=$isHighAlert)")
                return
            }
        }

        // 2. Guaranteed fallback: MediaPlayer over Media stream
        try {
            val mp = MediaPlayer.create(context.applicationContext, R.raw.warning_ding)
            if (mp != null) {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mp.setVolume(volume, volume)
                mp.setOnCompletionListener { player ->
                    try {
                        player.release()
                    } catch (_: Exception) {}
                }
                mp.start()
                Log.d(TAG, "Played gentle chime via MediaPlayer fallback (vol $volume, isHigh=$isHighAlert)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer fallback failed: ${e.message}")
        }
    }
}
