package com.example.parallax

// must do all layers or else can't take empty from PreferenceManager

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WallpaperService : WallpaperService() {

    inner class WallpaperEngine : WallpaperService.Engine() {
        val paint = Paint()
        // https://stackoverflow.com/questions/63405673/how-to-call-suspend-function-from-service-android
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.IO + job)
//        private val layers = MutableList(3);
        private var bitmap1: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.flower)
        private var bitmap2: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.flower)
        private var bitmap3: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.flower)


        /**
         * checks to see if
         * @param uriString
         */
        private fun uriStringToBitmap(uriString: String): Bitmap  {
            val uri: Uri = Uri.parse(uriString)

            // tri uri, see if it's valid. if not, don't draw layer (print issue)
            try {
                contentResolver.openInputStream(uri)?.close()
            } catch (e: Exception) {
                Log.d("__walpService", "issue with getting image $e");
                return BitmapFactory.decodeResource(resources, R.drawable.transparent)
            }

            if (Build.VERSION.SDK_INT >= 28) {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, uri)).copy(Bitmap.Config.ARGB_8888, false)
            } else {
                return MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, false)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d("__walpService", "engine onCreate");
            scope.launch {
                PreferenceManager.getL1(applicationContext).collect({ uriString ->
                    Log.d("__walpService", "l1 uri: $uriString");
                    bitmap1 = uriStringToBitmap(uriString)
                })
            }
            scope.launch {
                PreferenceManager.getL2(applicationContext).collect({ uriString ->
                    Log.d("__walpService", "l2 uri: $uriString");
                    bitmap2 = uriStringToBitmap(uriString)
                })
            }
            scope.launch {
                PreferenceManager.getL3(applicationContext).collect({ uriString ->
                    Log.d("__walpService", "l3 uri: $uriString");
                    bitmap3 = uriStringToBitmap(uriString)
                })
            }

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

//                    val bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.flower)
//                    val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.athing)

                    val destRect = Rect(0, 0, canvas.width, canvas.height)
                    val src1Rect = Rect(-offset/2, 0, (-offset/2) + bitmap1.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap1.getScaledHeight(canvas))
                    val src2Rect = Rect(-offset/1, 0, (-offset/1) + bitmap2.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap2.getScaledHeight(canvas))
                    val src3Rect = Rect(-offset/1, 0, (-offset/1) + bitmap3.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap3.getScaledHeight(canvas))
//                    val src1Rect = Rect(0, 0, bitmap1.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap1.getScaledHeight(canvas))
//                    val src2Rect = Rect(0, 0, bitmap2.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap2.getScaledHeight(canvas))
//                    val src3Rect = Rect(0, 0, bitmap3.getScaledWidth(canvas) * canvas.width/canvas.height, bitmap3.getScaledHeight(canvas))

                    canvas.drawBitmap(bitmap1, src1Rect, destRect, paint)
                    canvas.drawBitmap(bitmap2, src2Rect, destRect, paint)
                    canvas.drawBitmap(bitmap3, src3Rect, destRect, paint)
                }

            } catch (e: Exception) {
                Log.d("__walpService", "onsurfacecreated issue $e");
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            job.cancel()
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
//            Log.i("__walpService", "on offsets changed, x offset: ${xOffset} ${xPixelOffset}")
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
//                Log.i("__walpService", "touch event${event.getAxisValue(1)}")
//            }
//            Log.i("__walpService", "touch event")
//            super.onTouchEvent(event)
//        }

    }


    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()
        Log.i("__walpService", "should show")

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

}