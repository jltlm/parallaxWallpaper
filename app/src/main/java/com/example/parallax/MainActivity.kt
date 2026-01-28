package com.example.parallax

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.lifecycleScope
import com.example.parallax.databinding.ActivityMainBinding
import com.example.parallax.datastore.LayerDO
import com.example.parallax.datastore.LayerListDO
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.InputStream


private const val DATA_STORE_FILE_NAME = "layers.pb"

val Context.layerDataStore: DataStore<LayerListDO> by dataStore(
    fileName = DATA_STORE_FILE_NAME,
    serializer = LayerListDOSerializer
)

class Layer(uri: String = "", velocity: Int = 0, offset: Int = 0, drawable: Drawable? = null, imageType: ImageType = ImageType.BITMAP) {

    var uri: Uri? = null
    var drawable: Drawable? = null
    var velocity: Double = 0.0
    var offset: Double = 0.0
    var imageType: ImageType = ImageType.BITMAP

    init {
        this.uri = if (uri == "") null else Uri.parse(uri)
        this.imageType = imageType
        this.drawable = drawable
        this.velocity = velocity * 1.0
        this.offset = offset * 1.0
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
        this.layer.velocity = 0.0
        this.layer.offset = 0.0
        this.layer.uri = null
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

        val dr = this.layer.drawable!!

        imageViewSample.setImageDrawable(dr)
        imageViewSmall.setImageDrawable(dr)
//        // this is to make the UI background long, like the actual background
//        val bmWidth = dr.minimumWidth
//        val bmHeight = dr.minimumWidth.toDouble() // funny sheet cuz of kotlin's integer division T_T
//        imageViewSample.setImageBitmap(dr.scale((
//            bmWidth/bmHeight * Resources.getSystem().displayMetrics.heightPixels).toInt(),
//            Resources.getSystem().displayMetrics.heightPixels))
//        imageViewSmall.setImageBitmap(dr.scale((
//                bmWidth/bmHeight * imageViewSmall.height).toInt(), imageViewSmall.height))
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

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("__walpMain", "MainActivity onCreate")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // gonna want a loop for this later or something
        layerManagers[1] = LayerManager(1, Layer(), binding.ivSmallBot, binding.ivSampleBot)
        layerManagers[2] = LayerManager(2, Layer(), binding.ivSmallMid, binding.ivSampleMid)
        layerManagers[3] = LayerManager(3, Layer(), binding.ivSmallTop, binding.ivSampleTop)

        binding.btnClearAllLayers.setOnClickListener {
            clearAll()
        }

        binding.ivSmallBot.setOnClickListener {
            pickedLayer = 1
            binding.tvSpeedValue.text = "${layerManagers[pickedLayer]!!.layer.velocity}"
            binding.sbSpeed.progress = (layerManagers[pickedLayer]!!.layer.velocity * 10).toInt()
            binding.sbOffset.progress = (layerManagers[pickedLayer]!!.layer.offset).toInt()
        }
        binding.ivSmallMid.setOnClickListener{
            pickedLayer = 2
            binding.tvSpeedValue.text = "${layerManagers[pickedLayer]!!.layer.velocity}"
            binding.sbSpeed.progress = (layerManagers[pickedLayer]!!.layer.velocity * 10).toInt()
            binding.sbOffset.progress = (layerManagers[pickedLayer]!!.layer.offset).toInt()
        }
        binding.ivSmallTop.setOnClickListener {
            pickedLayer = 3
            binding.tvSpeedValue.text = "${layerManagers[pickedLayer]!!.layer.velocity}"
            binding.sbSpeed.progress = (layerManagers[pickedLayer]!!.layer.velocity * 10).toInt()
            binding.sbOffset.progress = (layerManagers[pickedLayer]!!.layer.offset).toInt()
        }

        // guh
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

        binding.sbSpeed.visibility = View.GONE
        binding.tvSpeedValue.setOnClickListener {
            binding.sbSpeed.visibility = View.VISIBLE
        }
        binding.tvSpeedLabel.setOnClickListener {
            binding.sbSpeed.visibility = View.VISIBLE
        }
        binding.sbSpeed.max = maxSpeed
        binding.sbSpeed.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.sbSpeed.visibility = View.GONE
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvSpeedValue.text = "${progress/1.0}"
                val speed = progress.toDouble()
                layerManagers[pickedLayer]!!.layer.velocity = speed/1.0
            }
        })

        binding.sbOffset.visibility = View.GONE
        binding.tvOffsetValue.setOnClickListener {
            binding.sbOffset.visibility = View.VISIBLE
        }
        binding.tvOffsetLabel.setOnClickListener {
            binding.sbOffset.visibility = View.VISIBLE
        }
        binding.sbOffset.max = screenWidth / 20
        binding.sbOffset.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.sbOffset.visibility = View.GONE
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvOffsetValue.text = "$progress"
                layerManagers[pickedLayer]!!.layer.offset = progress.toDouble() * -20 // this is my constant multiplier
            }
        })

//        binding.inputOffset.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_DONE) {
//                val offset = v.text.toString().toDouble()
//                layerManagers[pickedLayer]?.setVelocity(offset)
//                Log.i("__walpMain", "offset set: $pickedLayer $offset")
//                return@OnEditorActionListener true
//            }
//            false
//        })

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
        maxBackgroundWidthPx = pageNum * screenWidth
        binding.ivSampleBot.minimumWidth = maxBackgroundWidthPx + screenWidth
        binding.hsvSample.isHorizontalScrollBarEnabled = false
        binding.hsvSample.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            if (binding.sbPage.max == 0 || maxBackgroundWidthPx == 0) { return@setOnScrollChangeListener }

            val scrollPercent = scrollX * 1f / maxBackgroundWidthPx
            val scrollMultiplier = screenWidth * scrollPercent

            for ((_,v) in layerManagers) { // this is where the parallax scroll for the app UI happens
                v.imageViewSample.translationX = (-scrollMultiplier * v.layer.velocity).toFloat() + scrollX + (-v.layer.offset).toFloat()
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
                    binding.hsvSample.scrollX = progress * maxBackgroundWidthPx / binding.sbPage.max
                }
                Log.i("__walpMain", "current scroll: ${binding.hsvSample.scrollX}")
            }
        })

        loadAll()


    }

    /**
     * init from settings
     */
    private fun loadAll() {
        lifecycleScope.launch {
            LayerRepository.getLayerFlow(applicationContext).collect { layerDOs: List<LayerDO> ->
                Log.d("__walpMain", "loading ${layerDOs.size} layers from repo")
                for (layerDO in layerDOs) {
                    val uri = Uri.parse(layerDO.uri)
                    Log.d("__walpMain", "loading ${layerDO.imageType} ")

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
            Log.i("__walpMain", "l1: " + layerManagers[1]!!.layer.velocity + " l2: " + layerManagers[2]!!.layer.velocity + " l3: " + layerManagers[3]!!.layer.velocity)

            LayerRepository.clearLayers(applicationContext)

            for ((_, lm) in layerManagers) {
                val l = lm.layer
                if (l.uri == null) { continue }

                LayerRepository.addLayer(
                    applicationContext,
                    lm.level,
                    l.uri.toString(),
                    l.imageType.value,
                    l.velocity.toInt(),
                    l.offset.toInt()
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
    }

    private fun getImgPermissions () {
        Log.i("__walpMain", "getting permissions...")
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            Log.i("__walpMain", "requesting perms")
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    private fun selectImage(uri: Uri) {
        Log.i("__walpMain", uri.toString())

        val layer: LayerManager = layerManagers[pickedLayer]!!
        layer.setUriDrawable(uri, drawableFromUri(uri))
        layerManagers[pickedLayer]!!.layer.velocity = 0.0 // if new image picked, set to 0
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