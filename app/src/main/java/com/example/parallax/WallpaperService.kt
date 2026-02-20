package com.example.parallax

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.example.parallax.datastore.LayerDO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.InputStream


sealed class ServiceImg {
    abstract val img: Any

    class Empty(override val img: Drawable = ColorDrawable(Color.TRANSPARENT)) : ServiceImg()
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
}

class WallpaperService : WallpaperService() {

    inner class WallpaperEngine : Engine() {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        private var flower: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, R.drawable.flower))
        private var layers: MutableList<ServiceLayer> = MutableList(0) { ServiceLayer(ServiceImg.StaticBitmap(flower), ImageType.BITMAP) }
        private val paint = Paint()

//        private var nyan: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.chalk_animation))
//        private var goose: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.goose))
//        private var goose: Drawable = ColorDrawable(Color.TRANSPARENT)

        override fun onDestroy() {
            job.cancel()
            for (l in layers) {
                val img = l.img
                if (img is ServiceImg.AnimatedGif) (img.img).stop()
            }

            Log.e("__walpService", "bye bye")
            super.onDestroy()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.i("__walpService", "engine onCreate")

//            val bytes = resources.openRawResource(R.raw.nyan_cat).use { it.readBytes() }

            scope.launch {
                // adding layers in from datastore
                LayerRepository.getLayerFlow(applicationContext).collect{ layerDOs: List<LayerDO> ->
                    Log.d("__walpService", "num layers: ${layerDOs.size}")
                    layers.clear() // first- remove existing layers
                    for (layerDO in layerDOs) {
                        val img = getImageFromUri(
                            Uri.parse(layerDO.uri),
                            ImageType.fromInt(layerDO.imageType)
                        ) ?: continue // if we can't get the image just skip it

                        val l = ServiceLayer(img, ImageType.fromInt(layerDO.imageType), layerDO.uri, layerDO.velocity, layerDO.offset)
                        layers.add(l)

                        if (img is ServiceImg.AnimatedGif) {
                            img.img.start()
                        }

                    }
                }
            }
            // code here shall not come to pass

        }

        private fun getImageFromUri(uri: Uri, imageType: ImageType) : ServiceImg? {
            Log.i("__walpService", "uri: $uri")
            var inputStream: InputStream? = null
            var img: ServiceImg? = null
            try {
                inputStream = applicationContext.contentResolver.openInputStream(uri)

                // always skip back to bitmap as default image if to gif goes wrong
                when (imageType) {
                    ImageType.BITMAP -> {
                        val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, uri))
                        img = ServiceImg.StaticBitmap(default)
                    }
                    ImageType.CONTINUOUS_GIF -> {
                        val d = Drawable.createFromStream(inputStream, uri.toString())
                        if (d is AnimatedImageDrawable) {
                            img = ServiceImg.AnimatedGif(d)
                        } else {
                            val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, uri))
                            img = ServiceImg.StaticBitmap(default)
                        }
                    }
                    ImageType.INTERACTIVE_GIF -> {
                        val bytes = applicationContext.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                        val d = AnimationFrameHolder.create(applicationContext, bytes)
                        if (d != null) {
                            img = ServiceImg.InteractiveGif(d)
                        } else {
                            val default = ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, uri))
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

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            paint.color = Color.WHITE
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.isDither = false

            draw()
        }

        private fun draw(offset: Float = 0f) {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockHardwareCanvas()
                if (canvas != null) {

                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                    val canvasRatio = canvas.width.toDouble()/canvas.height

                    for (i in 0 until layers.size) {
                        val layer = layers[i]
                        val pos = - ((offset * canvas.width) * (layer.velocity / 5.0) - layer.offset).toInt() // canvas width is the multiplier

                        when (layer.img) {
                            is ServiceImg.AnimatedGif -> {
                                val layerImg = (layer.img.img as AnimatedImageDrawable)
                                layerImg.setBounds(pos, 0, pos + (1.0 * canvas.height / layerImg.minimumHeight * layerImg.minimumWidth).toInt(), canvas.height)
                                layerImg.draw(canvas)
                            }

                            is ServiceImg.InteractiveGif -> {
                                val layerImg = (layer.img.img as AnimationFrameHolder)
                                val bm = layerImg.getNextFrame()
                                val destRect = Rect(0, 0, canvas.width, canvas.height)
                                val srcRect = Rect(pos * bm.height / canvas.height, 0, pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(), bm.height)
                                canvas.drawBitmap(bm, srcRect, destRect, paint)
                            }

                            is ServiceImg.StaticBitmap -> {
                                val bm = (layer.img.img as Bitmap)
                                val destRect = Rect(0, 0, canvas.width, canvas.height)
                                val srcRect = Rect(pos * bm.height / canvas.height, 0, pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(), bm.height)
                                canvas.drawBitmap(bm, srcRect, destRect, paint)

                            }

                            is ServiceImg.Empty -> {} // well, we don't care
                        }
                    }

                }

            } catch (e: Exception) {
                Log.e("__walpService", "on surface creation $e")
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            draw(xOffset)
            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
//            Log.i("__walpService", "visible: $visible")
            if (visible) {
                for (l in layers) {
                    // this was for potential memory problems... but you know...
                    // if the problems are that bad, just kill the engine...
//                    l.img = getDrawableFromUri(Uri.parse(l.uri), l.imageType) ?: continue
                    val img = l.img
                    if (img is ServiceImg.AnimatedGif) (img.img).start()
                }
            } else {
                for (l in layers) {
                    val img = l.img
                    if (img is ServiceImg.AnimatedGif) (img.img).stop()
//                    l.img = ServiceImg.Empty()
                }
            }
        }
    }

    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

}