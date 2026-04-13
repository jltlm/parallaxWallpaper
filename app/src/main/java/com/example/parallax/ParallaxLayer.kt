package com.example.parallax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import java.io.FileNotFoundException
import java.io.InputStream

sealed class ParallaxImg {
    abstract val img: Any

    class Empty(override val img: Drawable = Color.TRANSPARENT.toDrawable()) : ParallaxImg()
    class Error(override val img: Drawable = Color.RED.toDrawable()) : ParallaxImg()
    class StaticBitmap(override val img: Bitmap) : ParallaxImg()
    class AnimatedGif(override val img: AnimatedImageDrawable) : ParallaxImg()
    class InteractiveGif(override val img: AnimationFrameHolder) : ParallaxImg()
}

class ParallaxLayer (img: ParallaxImg, uri: String = "", imageType: ImageType = ImageType.BITMAP, velocity : Double = 1.0, offset: Double = 0.0) {

    var img: ParallaxImg = ParallaxImg.Empty()
    var imageType: ImageType = ImageType.BITMAP
    var uri: String = ""
    var velocity: Double = 0.0
    var offset: Double = 0.0

    init {
        this.img = img
        this.uri = uri
        this.imageType = imageType
        this.velocity = 1.0 * velocity
        this.offset = 1.0 * offset
    }

    fun copyLayerValues(layer: ParallaxLayer) {
        if (layer.img != this.img) { this.img = layer.img }
        if (layer.uri != this.uri) { this.uri = layer.uri }
        this.imageType = layer.imageType
        this.velocity = layer.velocity
        this.offset = layer.offset
    }

    companion object {
        // builder, but if errors, return null
        fun create(context: Context, uriString: String = "", imageType: Int=1, velocity : Double = 1.0, offset: Double = 0.0) : ParallaxLayer {
            val img = getImageFromUri(
                context,
                uriString.toUri(),
                ImageType.fromInt(imageType)
            ) // if we can't get the image just skip it

//            try {
            return ParallaxLayer(img, uriString, ImageType.fromInt(imageType), velocity, offset)
//            } catch (e: Exception) {
//                Log.d("__walpServiceLayerCreation", "Can't create a valid layer")
//                e.printStackTrace()
//            }
//            return null
        }

        fun getImageFromUri(context: Context, uri: Uri, imageType: ImageType) : ParallaxImg {
            Log.i("__walpService", "uri: $uri")
            if (uri.toString() == "") {
                return ParallaxImg.Empty()
            }

            var inputStream: InputStream? = null
            var img: ParallaxImg? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)

                // always skip back to bitmap as default image if to gif goes wrong
                when (imageType) {
                    ImageType.BITMAP -> {
                        val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                        img = ParallaxImg.StaticBitmap(default)
                    }
                    ImageType.CONTINUOUS_GIF -> {
                        val d = Drawable.createFromStream(inputStream, uri.toString())
                        if (d is AnimatedImageDrawable) {
                            img = ParallaxImg.AnimatedGif(d)
                        } else {
                            val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            img = ParallaxImg.StaticBitmap(default)
                        }
                    }
                    ImageType.INTERACTIVE_GIF -> {
                        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                        val d = AnimationFrameHolder.create(context, bytes)
                        if (d != null) {
                            img = ParallaxImg.InteractiveGif(d)
                        } else {
                            val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            img = ParallaxImg.StaticBitmap(default)
                        }
                    }
                }

            } catch (e: FileNotFoundException) { e.printStackTrace()
            } finally { inputStream?.close() }

            if (img == null) {
                Log.e("__walpService", "can't draw image")
                img = ParallaxImg.Error()
            }

            return img
        }
    }
}

