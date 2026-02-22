package com.example.parallax

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.lifecycleScope
import com.example.parallax.databinding.ActivityMainBinding
import com.example.parallax.datastore.LayerDO
import com.example.parallax.datastore.LayerListDO
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.InputStream
import androidx.core.net.toUri

private const val DATA_STORE_FILE_NAME = "layers.pb"

val Context.layerDataStore: DataStore<LayerListDO> by dataStore(
    fileName = DATA_STORE_FILE_NAME,
    serializer = LayerListDOSerializer
)

class Layer(uri: String = "", velocity: Int = 0, offset: Int = 0, drawable: Drawable? = null, imageType: ImageType = ImageType.BITMAP) {

    var uri: Uri? = null
    var drawable: Drawable? = null
    var velocity: Int = 1
    var offset: Int = 0
    var imageType: ImageType = ImageType.BITMAP

    init {
        this.uri = if (uri == "") null else uri.toUri()
        this.imageType = imageType
        this.drawable = drawable
        this.velocity = velocity
        this.offset = offset
    }

}

class LayerManager(
    val level: Int,
    var layer: Layer,
    var imageViewSmall: ImageButton,
    var imageViewSample: ImageView
) {

    fun setUriDrawable (uri: Uri, drawable: Drawable?) {
        this.layer.uri = uri
        this.layer.drawable = drawable
    }

    fun setImageType (imageType: ImageType) {
        this.layer.imageType = imageType
    }

    fun clearLayer() {
        this.layer.velocity = 1 // hehe
        this.layer.offset = 0
        this.layer.uri = null
        this.layer.imageType = ImageType.BITMAP
        this.layer.drawable = null
    }

    fun loadLayer(l: Layer) {
        this.layer = l
    }

    fun updateUIElementImages() {
        if (this.layer.drawable == null) {
            imageViewSample.setImageBitmap(null)
            imageViewSmall.setImageBitmap(null)
            return
        }
        if (this.layer.drawable is AnimatedImageDrawable) {
            (this.layer.drawable as AnimatedImageDrawable).start()
            this.layer.imageType = ImageType.CONTINUOUS_GIF
        }

        val dr = this.layer.drawable!!

        imageViewSmall.setImageDrawable(dr)


        val matrix = Matrix()
        val drw = (dr.minimumWidth).toFloat()
        val drh = (dr.minimumHeight).toFloat()
        val fullHeight = (Resources.getSystem().displayMetrics.heightPixels).toFloat()
        if (drw == 0f || drh == 0f) { // if the drawable doesn't have a h/w, who cares?
        } else {
            val fullWidth = drw/drh * fullHeight // this is the width of the image when scaled to fit the height of the screen
            matrix.setScale(fullWidth/drw, fullHeight/drh)
//            Log.i("__walpMain", "$matrix")

            imageViewSample.layoutParams.width = fullWidth.toInt() // to fix some problem of imageView not scaling width
        }

        // the order of these three following things matter >:(
        imageViewSample.imageMatrix = matrix
        imageViewSample.scaleType = ImageView.ScaleType.MATRIX
        imageViewSample.setImageDrawable(dr)

//        Log.i("__walpMain", "${imageViewSample.width} x ${imageViewSample.height}")
        // https://developer.android.com/reference/android/widget/ImageView#setImageMatrix(android.graphics.Matrix)
    }

}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pickedLayer : Int = 1
    private val layerManagers : MutableMap<Int, LayerManager> = mutableMapOf()
    private var pageNum = 3
    private var maxBackgroundWidthPx = pageNum * 1000
    private val maxSpeed = 50
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels

    private val canvas: Canvas = Canvas()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("__walpMain", "Parallax Wallpaper activity onCreate")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // gonna want a loop for this later or something
        layerManagers[0] = LayerManager(0, Layer(), binding.ivSmallBot, binding.ivSampleBot)
        layerManagers[1] = LayerManager(1, Layer(), binding.ivSmallMid, binding.ivSampleMid)
        layerManagers[2] = LayerManager(2, Layer(), binding.ivSmallTop, binding.ivSampleTop)

        // creating a wide imageView so horizontalScrollView will have a ways to scroll
        binding.ivSampleEmpty.layoutParams.width = maxBackgroundWidthPx + screenWidth + 100 // magic. fix later

//        for (index in 0 .. 2) { // 3 layers
//            val ivSmall = View.inflate(applicationContext, R.layout. ...
//            val ivBig = View.inflate(applicationContext, R.layout. ...
//            layerManagers[index] = LayerManager(index, Layer(), ivSmall, ivBig)
//        }

        for (index in (0..<binding.layoutIvSmall.childCount)) {
            val iv = binding.layoutIvSmall[binding.layoutIvSmall.childCount - 1 - index] // haha this is so flimsy maybe i should not loop
            val lm = layerManagers[index] ?: continue
            try {
                iv.setOnClickListener {
                    pickedLayer = index
                    binding.tvSpeedValue.text = "${lm.layer.velocity}"
                    binding.sbSpeed.progress = lm.layer.velocity
                    binding.sbOffset.progress = lm.layer.offset
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        binding.btnClearAllLayers.setOnClickListener {
            clearAll()
        }

        //////// image picking
        val imgPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            for (uri in uris) {
                contentResolver.takePersistableUriPermission(uri, flags)
                selectImage(uri)
            }
        }

        binding.btnSelectImage.setOnClickListener { _ ->
            getImgPermissions()
            imgPicker.launch(arrayOf("image/*"))
        }
        // =============

        binding.sbSpeed.max = maxSpeed
        binding.sbSpeed.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvSpeedValue.text = "$progress"
                layerManagers[pickedLayer]!!.layer.velocity = progress
            }
        })

        binding.sbOffset.max = screenWidth // add help note: if you can't find layer, check your offset
        binding.sbOffset.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvOffsetValue.text = "$progress"
                layerManagers[pickedLayer]!!.layer.offset = progress
            }
        })

        binding.btnSetWallpaper.setOnClickListener { _ ->
            saveAll()
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, WallpaperService::class.java)
            )
            startActivity(intent)
        }

        binding.sbPage.max = pageNum
//        binding.ivSampleBot.minimumWidth = maxBackgroundWidthPx + screenWidth
        binding.hsvSample.isHorizontalScrollBarEnabled = false
        binding.hsvSample.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            if (binding.sbPage.max == 0 || maxBackgroundWidthPx == 0) { return@setOnScrollChangeListener }

//            val scrollPercent = scrollX * 1f / maxBackgroundWidthPx
//            val scrollMultiplier = screenWidth * scrollPercent

            for ((_,v) in layerManagers) { // this is where the parallax scroll for the app UI happens
                // multiplication by 1.0 so we get the right fractions
                v.imageViewSample.translationX = (-1.0 * scrollX * (1.0 * v.layer.velocity / 2)).toFloat() + scrollX + v.layer.offset
            }

            if (scrollX > maxBackgroundWidthPx ) {
                binding.hsvSample.scrollX = maxBackgroundWidthPx
                binding.sbPage.progress = binding.sbPage.max
            } else {
                binding.sbPage.progress = ((scrollX * 1f / maxBackgroundWidthPx) / (1f / binding.sbPage.max)).toInt()
            }
        }

        binding.sbPage.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.hsvSample.smoothScrollTo(progress * maxBackgroundWidthPx / binding.sbPage.max, 0)
//                    binding.hsvSample.scrollX = progress * maxBackgroundWidthPx / binding.sbPage.max
                }
//                Log.i("__walpMain", "current scroll: ${binding.hsvSample.scrollX}")
            }
        })

        loadAll()

    }

//    THIS IS UI SETUP NO MORE

    /**
     * init from settings
     */
    private fun loadAll() {
        lifecycleScope.launch {
            LayerRepository.getLayerFlow(applicationContext).collect { layerDOs: List<LayerDO> ->
                Log.d("__walpMain", "loading ${layerDOs.size} layers from repo")
                for (layerDO in layerDOs) {
                    val uri = layerDO.uri.toUri()
                    Log.i("__walpMain", "loading ${layerDO.level}: vel:${layerDO.velocity} offset:${layerDO.offset} type:${layerDO.imageType}")

                    // create new layer
                    val l = Layer(
                        uri = layerDO.uri,
                        velocity = layerDO.velocity,
                        offset = layerDO.offset,
                        imageType = ImageType.fromInt(layerDO.imageType))

                    // add layer to layerManagers
                    val lm = layerManagers[layerDO.level]
                    lm?.loadLayer(l)

                    // updating UI
                    lm?.setUriDrawable(uri, drawableFromUri(uri))
                    lm?.updateUIElementImages()
                }
            }
        }
    }

    /**
     * save all settings
     */
    private fun saveAll() {
        lifecycleScope.launch {
            Log.i("__walpMain", "saving settings")

            LayerRepository.clearLayers(applicationContext)

            for ((_, lm) in layerManagers) {
                val l = lm.layer
                if (l.uri == null) { continue }

                Log.i("__walpMain", "saving ${lm.level}: vel:${l.velocity} offset:${l.offset} type:${l.imageType.value}")

                LayerRepository.addLayer(
                    applicationContext,
                    lm.level,
                    l.uri.toString(),
                    l.imageType.value,
                    l.velocity,
                    l.offset
                )
            }
        }
    }

    private fun clearAll() {
        lifecycleScope.launch {
            Log.i("__walpMain", "clearing layers")
            LayerRepository.clearLayers(applicationContext)
        }
        for ((_, lm) in layerManagers) {
            val l = lm.layer
            if (l.uri == null) { continue }
            lm.clearLayer()
            lm.updateUIElementImages()
        }
        binding.sbPage.progress = 0
        binding.sbSpeed.progress = 0
        binding.sbOffset.progress = 0
    }

    private fun getImgPermissions () {
        Log.i("__walpMain", "getting permissions...")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            Log.i("__walpMain", "requesting perms")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    private fun selectImage(uri: Uri) {
        Log.i("__walpMain", uri.toString())

        val layer: LayerManager = layerManagers[pickedLayer]!!
        layer.setUriDrawable(uri, drawableFromUri(uri))
        layerManagers[pickedLayer]!!.layer.velocity = 0 // if new image picked, set to 0
        layer.updateUIElementImages()
    }

    /**
     * returns a bitmap from uri
     */
//    private fun bitmapFromUri(uri: Uri) : Bitmap {
//        val source = ImageDecoder.createSource(this.contentResolver, uri)
//        return ImageDecoder.decodeBitmap(source)
//    }

    private fun drawableFromUri(uri: Uri) : Drawable? {
        var drawable: Drawable? = null
        var inputStream: InputStream? = null
        try {
            inputStream = applicationContext.contentResolver.openInputStream(uri)
            drawable = Drawable.createFromStream(inputStream, uri.toString())
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.e("__walpMain", "can't find drawable $uri")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("__walpMain", "convert from uri exception $uri")
        } finally { inputStream?.close() }

        return drawable
    }

}