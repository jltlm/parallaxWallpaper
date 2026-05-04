package com.example.parallax

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider

class AnimationFrameHolder(context: Context, bytes: ByteArray, speed : Int = 20) {
    private var decoder: StandardGifDecoder
    private var delaySpeed = speed
    private var delayRemaining = 0

    init {
        val parser = GifHeaderParser().apply { setData(bytes) }
        val header = parser.parseHeader()

        require(header.numFrames > 0) { "Not a valid GIF" }
//        if (header.numFrames <= 0) { throw IllegalArgumentException("Not a valid GIF") }

        decoder = StandardGifDecoder(GifBitmapProvider(Glide.get(context).bitmapPool))
            .apply {
                setData(header, bytes)
                advance()
                delayRemaining = nextDelay / delaySpeed
            }
    }

    companion object {
        // builder, but if errors, return null
        fun create(context: Context, bytes: ByteArray, speed : Int = 20) : AnimationFrameHolder? {
            try {
                return AnimationFrameHolder(context, bytes, speed)
            } catch (e: Exception) {
                Log.d("__walpService", "Not a valid GIF")
            }
            return null
        }
    }


    /**
     * Includes delay for timing
     */
    fun getNextFrame(): Bitmap {
        if (delayRemaining <= 0) {
            decoder.advance()
            delayRemaining = decoder.nextDelay / delaySpeed
        }
        delayRemaining--

        return decoder.nextFrame
            ?: throw IllegalStateException("Decoder returned null frame")
    }

}