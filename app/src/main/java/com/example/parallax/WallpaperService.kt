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
//        private var nyan: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.chalk_animation))
//        private var goose: Drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.goose))
        private var goose: Drawable = ColorDrawable(Color.TRANSPARENT)
//        private var flower: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, R.drawable.flower))
//        private var nyan2: AnimationFrameHolder = AnimationFrameHolder(applicationContext, resources.openRawResource(R.raw.chalk_animation).use { it.readBytes() })
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

            scope.launch {
                // adding layers in from datastore
                LayerRepository.getLayerFlow(applicationContext).collect{ layerDOs: List<LayerDO> ->
                    layers.clear() // first- remove existing layers
                    Log.d("__walpService", "num layers: ${layerDOs.size}")
                    for (layerDO in layerDOs) {
                        addLayer(layerDO)
                        if (layers[layers.size-1].img is AnimatedImageDrawable) {
                            (layers[layers.size-1].img as AnimatedImageDrawable).start()
                        }
                    }
                }
            }
            Log.d("__walpService", "num layers after adding: ${layers.size}")

        }

        private fun addLayer(layerDO: LayerDO) {
            val uri = Uri.parse(layerDO.uri)
            Log.i("__walpService", "uri: $uri")
            var inputStream: InputStream? = null
            try {
                inputStream = applicationContext.contentResolver.openInputStream(uri)

                var drawable: Any?
                when (ImageType.fromInt(layerDO.imageType)) {
                    ImageType.BITMAP -> {
                        drawable = ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, uri))
                    }
                    ImageType.CONTINUOUS_GIF -> {
                        drawable = Drawable.createFromStream(inputStream, layerDO.uri)
                    }
                    ImageType.INTERACTIVE_GIF -> {
                        val bytes = applicationContext.contentResolver
                            .openInputStream(uri)!!
                            .use { it.readBytes() }
                        drawable = try {
                            AnimationFrameHolder(applicationContext, bytes)
                        } catch (e: Exception) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, uri))
                            Log.e("__walpService", "not a valid interactive gif - draw as bitmap")
                        }
                    }
                }

                if (drawable == null) {
                    Log.e("__walpService", "can't draw image")
                } else {
                    layers.add(Layer(drawable, layerDO.velocity, layerDO.offset))
                }
            } catch (e: FileNotFoundException) { e.printStackTrace()
            } finally {
                inputStream?.close()
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
            Log.i("__walpService", "visible: $visible")
            if (visible) {


                for (i in 0 until layers.size) {
                    if (layers[i].img is AnimatedImageDrawable) {
                        (layers[i].img as AnimatedImageDrawable).start()
                    }
                }
            } else {
//                scope.cancel()

                for (i in 0 until layers.size) {
                    if (layers[i].img is AnimatedImageDrawable) {
                        (layers[i].img as AnimatedImageDrawable).stop()
                    }
                }
            }
        }
    }

    private var engine: Engine = WallpaperEngine()

    override fun onCreateEngine(): Engine {
        Log.e("__walpService", "we're making an engine??!?!?!")
        engine = WallpaperEngine()
        return engine
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

}