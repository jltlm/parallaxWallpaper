package com.example.parallax

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.IBinder
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.widget.Toast

class WallpaperService : WallpaperService() {


    inner class WallpaperEngine : WallpaperService.Engine() {
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d("__serviceTag", "engine onCreate");

        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()

                if (canvas != null) {

                    val destRect = Rect(0, 0, canvas.width, canvas.height)
                    val paint = Paint()
                    paint.color = Color.WHITE
                    paint.isAntiAlias = true
                    paint.isFilterBitmap = true
                    paint.isDither = false

                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.flower)
                    Log.i("__serviceTag", "bitmap set")
                    canvas.drawBitmap(bitmap, null, destRect, paint)
                }

            } catch (e: Exception) {
                Log.d("__serviceTag", "onsurfacecreated issue");
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
                Log.i("__serviceTag", "bitmap set2")
            }

        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.AXIS_X) {
                event.getAxisValue(1)
                Log.i("__serviceTag", "touch event${event.getAxisValue(1)}")
            }
            Log.i("__serviceTag", "touch event")
            super.onTouchEvent(event)
        }

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