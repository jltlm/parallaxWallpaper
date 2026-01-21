package com.example.parallax

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider

class AnimationFrameHolder(bytes: ByteArray, context: Context, speed : Int = 20) {
    private val frames = mutableListOf<Bitmap>()
    private var frameIndex = 0
    private val delays = mutableListOf<Int>()
    private var delayIndex = 0
    private var delaySpeed = 1

    init {
        val parser = GifHeaderParser()
        parser.setData(bytes)

        val header = parser.parseHeader()
        if (header.numFrames <= 0) { error("Not a valid GIF") }

        val decoder = StandardGifDecoder(GifBitmapProvider(Glide.get(context).bitmapPool))
        decoder.setData(header, bytes)

        try {
            for (i in 0 until header.numFrames) {
                decoder.advance()              // move to next frame
                val frame = decoder.nextFrame  // composited bitmap
                if (frame != null) {
                    frames.add(frame.copy(Bitmap.Config.HARDWARE, false))
                    delays.add(decoder.nextDelay)
                }
            }
        } catch (e: Exception) {
            Log.e("__walpService", "getting frames of gif: $e")
        }

        delaySpeed = speed
        delayIndex = delays[frameIndex] / delaySpeed
    }

    /**
     * Includes delay for timing
     */
    fun getNextFrame() : Bitmap {
        if (delayIndex <= 0) {
            frameIndex = (frameIndex + 1) % frames.size
            delayIndex = delays[frameIndex] / delaySpeed
        }
        delayIndex -= 1
        return frames[frameIndex]
    }
}