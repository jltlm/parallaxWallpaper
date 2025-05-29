package com.example.parallax

import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
            Log.d("mytagagain", "engine onCreate");

        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
//            val canvas = holder.lockCanvas()

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.goose)
            Log.i("mytagagain", "bitmap set?")
//            canvas.setBitmap(bitmap);

            holder.unlockCanvasAndPost(Canvas(bitmap))
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.AXIS_X) {
                event.getAxisValue(1)
            }
            Log.i("mytagagain", "touch event")
            super.onTouchEvent(event)
        }

    }

    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()
        Log.i("mytagagain", "should show")

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

}