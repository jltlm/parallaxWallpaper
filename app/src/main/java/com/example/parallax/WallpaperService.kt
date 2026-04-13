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
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.example.parallax.datastore.LayerDO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class WallpaperService : WallpaperService() {

    inner class WallpaperEngine : Engine() {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
        private var flower: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, R.drawable.flower))
        private var layers: MutableList<ParallaxLayer> = MutableList(0) { ParallaxLayer(ParallaxImg.StaticBitmap(flower)) }
        private val paint = Paint()

//        private var nyan: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.chalk_animation))
//        private var goose: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.goose))
//        private var goose: Drawable = ColorDrawable(Color.TRANSPARENT)

        override fun onDestroy() {
            job.cancel()
            for (l in layers) {
                val img = l.img
                if (img is ParallaxImg.AnimatedGif) (img.img).stop()
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
                        val l = ParallaxLayer.create(applicationContext, layerDO.uri, layerDO.imageType, layerDO.velocity*1.0, layerDO.offset*1.0)
                        layers.add(l)

                        val img = l.img
                        if (img is ParallaxImg.AnimatedGif) {
                            img.img.start()
                        }

                    }
                }
            }
            // code here shall not come to pass

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
                        val pos = ((offset * canvas.width) * (layer.velocity / 50.0) - layer.offset).toInt() // canvas width is the multiplier

                        when (layer.img) {
                            is ParallaxImg.AnimatedGif -> {
                                val layerImg = (layer.img.img as AnimatedImageDrawable)
                                layerImg.setBounds(-pos, 0, -pos + (1.0 * canvas.height / layerImg.minimumHeight * layerImg.minimumWidth).toInt(), canvas.height)
                                layerImg.draw(canvas)
                            }

                            is ParallaxImg.InteractiveGif -> {
                                val layerImg = (layer.img.img as AnimationFrameHolder)
                                val bm = layerImg.getNextFrame()
                                val destRect = Rect(0, 0, canvas.width, canvas.height)
                                val srcRect = Rect(pos * bm.height / canvas.height, 0, pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(), bm.height)
                                canvas.drawBitmap(bm, srcRect, destRect, paint)
                            }

                            is ParallaxImg.StaticBitmap -> {
                                val bm = (layer.img.img as Bitmap)
                                val destRect = Rect(0, 0, canvas.width, canvas.height)
                                val srcRect = Rect(pos * bm.height / canvas.height, 0, pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(), bm.height)
                                canvas.drawBitmap(bm, srcRect, destRect, paint)

                            }

                            is ParallaxImg.Empty -> {} // well, we don't care
                            is ParallaxImg.Error -> {} // and bad images would also be invisible
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
                    if (img is ParallaxImg.AnimatedGif) (img.img).start()
                }
            } else {
                for (l in layers) {
                    val img = l.img
                    if (img is ParallaxImg.AnimatedGif) (img.img).stop()
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