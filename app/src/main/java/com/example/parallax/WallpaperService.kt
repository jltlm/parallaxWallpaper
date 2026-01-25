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
import android.graphics.drawable.BitmapDrawable
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


class WallpaperService : WallpaperService() {

    inner class Layer (img: Any, velocity : Int = 1, offset: Int = 0) {

        var img: Any = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.flower))
        var velocity: Double = 0.0
        var offset: Double = 0.0

        init {
            this.img = img
            this.velocity = 1.0 * velocity / 10
            this.offset = 1.0 * offset
        }
    }

    inner class WallpaperEngine : Engine() {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)

        private val paint = Paint()
        private var nyan: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.chalk_animation))
        private var goose: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.goose))
        private var flower: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, R.drawable.flower))
        private var nyan2: AnimationFrameHolder = AnimationFrameHolder(resources.openRawResource(R.raw.chalk_animation).use { it.readBytes() }, applicationContext)
// maybe add option to mirror image / gif on negative offset
        private var layers: MutableList<Layer> = MutableList(0) { Layer(goose) }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.i("__walpService", "engine onCreate")

//            layers.add(Layer(nyan, 1, 0))
//            layers.add(Layer(goose, 12, 100))
//            layers.add(Layer(nyan2, 3, 600))
//            layers.add(Layer(flower, -16, -500))
//            val bytes = resources.openRawResource(R.raw.nyan_cat).use { it.readBytes() }

            layers.clear() // first- remove existing layers
            scope.launch {
                // adding layers in from datastore
                LayerRepository.getLayerFlow(applicationContext).collect{ layerDOs: List<LayerDO> ->
                    for (layerDO in layerDOs) {
                        val uri = Uri.parse(layerDO.uri)
                        Log.i("__walpService", "uri: $uri")
                        var inputStream: InputStream? = null
                        try {
                            inputStream = applicationContext.contentResolver.openInputStream(uri)
                            val drawable = Drawable.createFromStream(inputStream, layerDO.uri)
                            if (drawable == null) {
                                Log.i("__walpService", "engine onCreate")
                            } else {
                                layers.add(Layer(drawable, layerDO.velocity, layerDO.offset))
                            }
                        } catch (e: FileNotFoundException) { e.printStackTrace()
                        } finally {
                            inputStream?.close()
                        }
                    }
                }
            }

            for (i in 0 until layers.size) {
                if (layers[i].img is AnimatedImageDrawable) {
                    (layers[i].img as AnimatedImageDrawable).start()
                }

            }

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
                        val pos = - ((offset * canvas.width) * layer.velocity - layer.offset).toInt() // canvas width is the multiplier

                        when (layer.img) {
                            is AnimatedImageDrawable -> {
                                val layerImg = (layer.img as AnimatedImageDrawable)
                                layerImg.setBounds(pos, 0, pos + (1.0 * canvas.height / layerImg.minimumHeight * layerImg.minimumWidth).toInt(), canvas.height)
                                layerImg.draw(canvas)
                            }

                            is BitmapDrawable -> {
                                val layerImg = (layer.img as BitmapDrawable)
                                layerImg.setBounds(pos, 0, pos + (1.0 * canvas.height / layerImg.minimumHeight * layerImg.minimumWidth).toInt(), canvas.height)
                                layerImg.draw(canvas)
                            }

                            is AnimationFrameHolder -> {
                                val layerImg = (layer.img as AnimationFrameHolder)
                                val bm = layerImg.getNextFrame()
                                val destRect = Rect(0, 0, canvas.width, canvas.height)
                                val srcRect = Rect(pos * bm.height / canvas.height, 0, pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(), bm.height)
                                canvas.drawBitmap(bm, srcRect, destRect, paint)
                            }

                            is Bitmap -> {
                                val bm = (layer.img as Bitmap)
                                val destRect = Rect(0, 0, canvas.width, canvas.height)
                                val srcRect = Rect(pos * bm.height / canvas.height, 0, pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(), bm.height)
                                canvas.drawBitmap(bm, srcRect, destRect, paint)

                            }
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
            Log.e("__walpService", "visible: $visible")
            if (visible) {
                for (i in 0 until layers.size) {
                    if (layers[i].img is AnimatedImageDrawable) {
                        (layers[i].img as AnimatedImageDrawable).start()
                    }
                }
            } else {
                for (i in 0 until layers.size) {
                    if (layers[i].img is AnimatedImageDrawable) {
                        (layers[i].img as AnimatedImageDrawable).stop()
                    }
                }
            }
        }
    }


    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // If we get killed, after returning from here, restart
        return START_STICKY
    }

}