package com.example.parallax

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.IBinder
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.widget.Toast

class WallpaperService : WallpaperService() {


    inner class WallpaperEngine : WallpaperService.Engine() {
        val paint = Paint()

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d("__serviceTag", "engine onCreate");

        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            paint.color = Color.WHITE
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.isDither = false

            draw()

        }

        fun draw(offset: Int = 0) {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()

                if (canvas != null) {

                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
//                    val source : ImageDecoder.Source = ImageDecoder.createSource(resources, R.drawable.gif)
//                    val ani = ImageDecoder.decodeDrawable(source) // this is for the gifs

                    val bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.flower)
                    val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.goose)

                    val destRect = Rect(0, 0, canvas.width, canvas.height)
                    val src1Rect = Rect(-offset/2, 0, (-offset/2) + bitmap1.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap1.getScaledHeight(canvas))
                    val src2Rect = Rect(-offset/1, 0, (-offset/1) + bitmap2.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap2.getScaledHeight(canvas))

//                    Log.i("__serviceTag", "window of bitmap set, x from ${-offset} to ${-(offset - bitmap1.getScaledWidth(canvas))}")
                    canvas.drawBitmap(bitmap1, src1Rect, destRect, paint)
                    canvas.drawBitmap(bitmap2, src2Rect, destRect, paint)
                }

            } catch (e: Exception) {
                Log.d("__serviceTag", "onsurfacecreated issue");
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
//                Log.i("__serviceTag", "bitmap set2")
            }
        }

//        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder?) {
//            Log.i("__serviceTag", "on surface redraw needed")
//
//            super.onSurfaceRedrawNeeded(holder)
//        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            Log.i("__serviceTag", "on offsets changed, x offset: ${xOffset} ${xPixelOffset}")
            draw(xPixelOffset)

            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
        }

//        override fun onTouchEvent(event: MotionEvent) {
//            if (event.action == MotionEvent.AXIS_X) {
//                event.getAxisValue(1)
//                Log.i("__serviceTag", "touch event${event.getAxisValue(1)}")
//            }
//            Log.i("__serviceTag", "touch event")
//            super.onTouchEvent(event)
//        }

    }

    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()
        Log.i("__serviceTag", "should show")

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

}