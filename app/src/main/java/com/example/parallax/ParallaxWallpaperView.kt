package com.example.parallax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.AnimatedImageDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class ParallaxWallpaperView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            Log.i("__walpMainView", "down: x:${e.x}")
            return true
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            fling((-velocityX).toInt(), (-velocityY).toInt())
            return true
        }

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
        ): Boolean {
            scroll(distanceX)
            return true
        }
    }

    private val paint = Paint()
    private var layers: MutableMap<Int, ParallaxLayer> = mutableMapOf()
    private var offset: Int = 0

    private var maxScroll: Int = 3000

    init {
        holder.addCallback(this)
        val gestureDetector = GestureDetector(context, gestureListener)
        setOnTouchListener{ _, event -> gestureDetector.onTouchEvent(event)}

        overScrollMode = OVER_SCROLL_NEVER
    }

    // only to be used when loading all layers from data
    fun setLayers(layers: MutableMap<Int, ParallaxLayer>) {
        this.layers.clear()
        this.layers = layers
        draw()
    }

    fun clear() {
        this.layers.clear()
        draw()
    }

    fun setLayer(level: Int, layer: ParallaxLayer) {
        layers[level] = layer
        if (layer.img.img is ParallaxImg.AnimatedGif) {
            (layer.img.img as AnimatedImageDrawable).start()
        }

        draw()
    }

    // only use this if you know the layer already exists
    fun setLayerValues(level: Int, layer: ParallaxLayer) {
        if (layers[level] != null) {
            layers[level]!!.copyLayerValues(layer)
        }

        val layerImg = layers[level]!!.img
        if (layerImg.img is AnimatedImageDrawable) {
            (layerImg.img as AnimatedImageDrawable).start()
            Log.i("__walpUIView", "layer is animated ${layer.imageType.name} - layer $level")
        }
        Log.i("__walpUIView", "layer is ${layer.imageType.name} - layer $level")

        draw()
    }

    fun clearLayer(level: Int) {
        layers.remove(level)
        draw()
    }

    fun getLayer(level: Int) : ParallaxLayer? {
        return layers[level]
    }

    fun draw(offset: Int = 0) {
        var canvas: Canvas? = null
        try {
            canvas = holder.lockHardwareCanvas()
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                val canvasRatio = canvas.width.toDouble()/canvas.height

                // hard-coded - 3 LAYERS ONLY
                for (i in 0..2) {
                    val layer = layers[i] ?: continue
                    val pos =
                        (offset * (layer.velocity / 50.0) - layer.offset).toInt() // canvas width is the multiplier

                    when (layer.img) {
                        is ParallaxImg.AnimatedGif -> {
                            val layerImg = (layer.img.img as AnimatedImageDrawable)
                            layerImg.setBounds(
                                -pos,
                                0,
                                -pos + (1.0 * canvas.height / layerImg.minimumHeight * layerImg.minimumWidth).toInt(),
                                canvas.height
                            )
                            layerImg.draw(canvas)
                        }
                        is ParallaxImg.InteractiveGif -> {
                            val layerImg = (layer.img.img as AnimationFrameHolder)
                            val bm = layerImg.getNextFrame()
                            val destRect = Rect(0, 0, canvas.width, canvas.height)
                            val srcRect = Rect(
                                pos * bm.height / canvas.height,
                                0,
                                pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(),
                                bm.height
                            )
                            canvas.drawBitmap(bm, srcRect, destRect, paint)
                        }
                        is ParallaxImg.StaticBitmap -> {
                            val bm = (layer.img.img as Bitmap)
                            val destRect = Rect(0, 0, canvas.width, canvas.height)
                            val srcRect = Rect(
                                pos * bm.height / canvas.height,
                                0,
                                pos * bm.height / canvas.height + (bm.height * canvasRatio).toInt(),
                                bm.height
                            )
                            canvas.drawBitmap(bm, srcRect, destRect, paint)

                        }
                        is ParallaxImg.Empty -> {} // well, we don't care
                        is ParallaxImg.Error -> {}
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("__walpMainView", "on surface creation $e")
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
//        Log.i("__walpMainView", "surfaceCreated")
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.isDither = false

        holder.isCreating

        draw()
    }

    override fun surfaceChanged(
        p0: SurfaceHolder,
        p1: Int,
        p2: Int,
        p3: Int
    ) {
        return
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        return//        TODO("Not yet implemented")
    }

    // https://android.googlesource.com/platform/frameworks/base/+/4662611/core/java/android/view/View.java

    override fun scrollTo(x: Int, y: Int) {
        boo(x)
        super.scrollTo(0, 0) // don't scroll, lol
    }

    // works mostly like onScrollChanged
    private fun boo(x: Int) {
    // because scrollTo is locked to 0, x acts as dist traveled and not absolute scroll dist
        val offsetTemp = offset + x * 3 // x2 is magic number

        val offsetDraw = if (offsetTemp > maxScroll * 3) { maxScroll * 3 }
        else if (offsetTemp < 0) { 0 }
        else { offsetTemp }

        draw(offsetDraw)
        offset = offsetDraw
//        Log.i("__walpMainView", "SCROLL CHANGED, $offset")
    }

    private fun fling(velocityX: Int, velocityY: Int) {
//        Log.i("__walpMainView", "we flingaling")
        dispatchNestedPreFling(velocityX.toFloat(), velocityY.toFloat())
    }

    private fun scroll(distanceX: Float, distanceY: Float = 0f) {
//        Log.i("__walpMainView", "we scrollin, $distanceX")
        scrollBy(distanceX.toInt(), 0)
    }

}

