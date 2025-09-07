package com.example.parallax

// must do all layers or else can't take empty from PreferenceManager

import android.app.WallpaperManager
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
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.graphics.scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WallpaperService : WallpaperService() {

    inner class WallpaperEngine : WallpaperService.Engine() {
        private val paint = Paint()
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
                Log.w("__walpService", "issue with getting image $e")
                return BitmapFactory.decodeResource(resources, R.drawable.transparent)
            }
            Log.d("__walpService", "valid uri: $uriString")

            if (Build.VERSION.SDK_INT >= 28) {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, uri)).copy(Bitmap.Config.ARGB_8888, false)
            } else {
                return MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, false)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.i("__walpService", "engine onCreate")

            var canvasHeight = 0f;
            try {
                val metrics = applicationContext.resources.displayMetrics
                canvasHeight = metrics.heightPixels.toFloat()
                Log.d("__walpService", "canvas pixel width: " + metrics.widthPixels + " height: " + metrics.heightPixels)
            } catch (e: Exception) {
                Log.e("__walpService", "error accessing display height: $e")
            }

            scope.launch {
                PreferenceManager.getL1(applicationContext).collect { uriString ->
                    Log.i("__walpService", "l1 uri: $uriString")
                    bitmap1 = uriStringToBitmap(uriString)
                    bitmap1 = bitmap1.scale((canvasHeight / bitmap1.height * bitmap1.width).toInt(), canvasHeight.toInt()
                    )
                }
            }
            scope.launch {
                PreferenceManager.getL2(applicationContext).collect{ uriString ->
                    Log.i("__walpService", "l2 uri: $uriString")
                    bitmap2 = uriStringToBitmap(uriString)
                    bitmap2 = bitmap2.scale((canvasHeight / bitmap2.height * bitmap2.width).toInt(), canvasHeight.toInt()
                    )
                }
            }
            scope.launch {
                PreferenceManager.getL3(applicationContext).collect{ uriString ->
                    Log.i("__walpService", "l3 uri: $uriString")
                    bitmap3 = uriStringToBitmap(uriString)
                    bitmap3 = bitmap3.scale((canvasHeight / bitmap3.height * bitmap3.width).toInt(), canvasHeight.toInt()
                    )
                }
            }

//            val source : ImageDecoder.Source = ImageDecoder.createSource(resources, R.drawable.gif)
//            val ani = ImageDecoder.decodeDrawable(source) // this is for the gifs

        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            paint.color = Color.WHITE
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.isDither = false

            draw()
        }

        private fun draw(offset: Int = 0) {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()

                // linear movement (diff equation for something else)
                fun getPos(velocity: Double): Int {
                    return (-offset * velocity).toInt()
                }

                if (canvas != null) {

                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                    val destRect = Rect(0, 0, canvas.width, canvas.height)
                    // width of a srcRect should be c.w/c.h * bm.h for scaling to full image height and cropping width to screen

                    // bottommost to topmost (1-3) layers
                    // lower velocity is slower. negative velocity is backwards.
                    val pos1 = getPos(.1)
                    val pos2 = getPos(.1)
                    val pos3 = getPos(.3)

                    val src1Rect = Rect(pos1, 0, pos1 + bitmap1.height * canvas.width/canvas.height, bitmap1.height)
                    val src2Rect = Rect(pos2, 0, pos2 + bitmap2.getScaledHeight(canvas) * canvas.width/canvas.height, bitmap2.getScaledHeight(canvas))
                    val src3Rect = Rect(pos3, 0, pos3 + bitmap3.getScaledHeight(canvas) * canvas.width/canvas.height, bitmap3.getScaledHeight(canvas))

                    canvas.drawBitmap(bitmap1, src1Rect, destRect, paint)
                    canvas.drawBitmap(bitmap2, src2Rect, destRect, paint)
                    canvas.drawBitmap(bitmap3, src3Rect, destRect, paint)
                }

            } catch (e: Exception) {
                Log.e("__walpService", "onsurfacecreated issue $e")
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
    }


    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // If we get killed, after returning from here, restart
        return START_STICKY
    }

}