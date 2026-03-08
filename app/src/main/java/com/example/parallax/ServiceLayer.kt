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

sealed class ServiceImg {
    abstract val img: Any

    class Empty(override val img: Drawable = Color.TRANSPARENT.toDrawable()) : ServiceImg()
    class StaticBitmap(override val img: Bitmap) : ServiceImg()
    class AnimatedGif(override val img: AnimatedImageDrawable) : ServiceImg()
    class InteractiveGif(override val img: AnimationFrameHolder) : ServiceImg()
}

class ServiceLayer (img: ServiceImg, imageType: ImageType, uri: String = "", velocity : Int = 1, offset: Int = 0) {

    var img: ServiceImg = ServiceImg.Empty()
    var imageType: ImageType = ImageType.BITMAP
    var uri: String = ""
    var velocity: Double = 0.0
    var offset: Double = 0.0

    init {
        this.img = img
        this.uri = uri
        this.imageType = imageType
        this.velocity = 1.0 * velocity / 10
        this.offset = 1.0 * offset
    }

    companion object {
        // builder, but if errors, return null
        fun create(context: Context, uriString: String = "", imageType: Int, velocity : Int = 1, offset: Int = 0) : ServiceLayer? {
            val img = getImageFromUri(
                context,
                uriString.toUri(),
                ImageType.fromInt(imageType)
            ) ?: return null // if we can't get the image just skip it

            try {
                return ServiceLayer(img, ImageType.fromInt(imageType), uriString, velocity, offset)
            } catch (e: Exception) {
                Log.d("__walpServiceLayerCreation", "Can't create a valid layer")
                e.printStackTrace()
            }
            return null
        }

        private fun getImageFromUri(context: Context, uri: Uri, imageType: ImageType) : ServiceImg? {
            Log.i("__walpService", "uri: $uri")
            var inputStream: InputStream? = null
            var img: ServiceImg? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)

                // always skip back to bitmap as default image if to gif goes wrong
                when (imageType) {
                    ImageType.BITMAP -> {
                        val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                        img = ServiceImg.StaticBitmap(default)
                    }
                    ImageType.CONTINUOUS_GIF -> {
                        val d = Drawable.createFromStream(inputStream, uri.toString())
                        if (d is AnimatedImageDrawable) {
                            img = ServiceImg.AnimatedGif(d)
                        } else {
                            val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            img = ServiceImg.StaticBitmap(default)
                        }
                    }
                    ImageType.INTERACTIVE_GIF -> {
                        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                        val d = AnimationFrameHolder.create(context, bytes)
                        if (d != null) {
                            img = ServiceImg.InteractiveGif(d)
                        } else {
                            val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            img = ServiceImg.StaticBitmap(default)
                        }
                    }
                }

            } catch (e: FileNotFoundException) { e.printStackTrace()
            } finally { inputStream?.close() }

            if (img == null)
                Log.e("__walpService", "can't draw image")

            return img
        }
    }
}

