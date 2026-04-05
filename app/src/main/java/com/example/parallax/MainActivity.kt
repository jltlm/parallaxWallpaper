package com.example.parallax

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.parallax.SettingData
import kotlin.properties.Delegates

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
    var imageViewSmall: ImageButton
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
            imageViewSmall.setImageBitmap(null)
            return
        }
        if (this.layer.drawable is AnimatedImageDrawable) {
            (this.layer.drawable as AnimatedImageDrawable).start()
            this.layer.imageType = ImageType.CONTINUOUS_GIF
        }

        val dr = this.layer.drawable!!

        imageViewSmall.setImageDrawable(dr)

    }

    fun setConfigValue(configOption: ConfigOption, amount: Int) {
        when (configOption) {
            ConfigOption.VELOCITY -> this.layer.velocity = amount
            ConfigOption.X_OFFSET -> this.layer.offset = amount
            ConfigOption.Y_OFFSET ->  this.layer.offset = amount // todo
            ConfigOption.SPEED ->  println("speed")
            ConfigOption.IMAGE_TYPE ->  println("imagetype")
            ConfigOption.SCALE ->  println("scale")
        }
    }

}

class MainActivity : AppCompatActivity(), RecyclerViewImgEditAdapter.ItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private var pickedLayer : Int = 0
    private val layerManagers : MutableMap<Int, LayerManager> = mutableMapOf()
    private var pageNum = 3
    private val maxSpeed = 50
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private var maxBackgroundWidthPx = screenWidth * 3
    private var settingRecyclerViewAdapter: RecyclerViewImgEditAdapter? = null

    // data to populate the RecyclerView and Data with
    val settingsList = mutableListOf<SettingData>()
    lateinit var settingVelocity: SettingData
    lateinit var settingXOffset: SettingData
    lateinit var settingYOffset: SettingData
    lateinit var settingScale : SettingData
    lateinit var settingSpeed : SettingData

//    let, also, apply, with. cool stuff

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("__walpMain", "Parallax Wallpaper activity onCreate")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // gonna want a loop for this later or something
        layerManagers[0] = LayerManager(0, Layer(), binding.ivSmallBot)
        layerManagers[1] = LayerManager(1, Layer(), binding.ivSmallMid)
        layerManagers[2] = LayerManager(2, Layer(), binding.ivSmallTop)

        // set up the RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.rvEditOptions)
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        settingRecyclerViewAdapter = RecyclerViewImgEditAdapter(this, settingsList)
        settingRecyclerViewAdapter?.setClickListener(this)
        recyclerView.setAdapter(settingRecyclerViewAdapter)

        // this is the fancy schmancy hi-tech wiring between the UI value and the data value!!
        val updatePickedLayer : (ConfigOption, Int) -> Unit = { option, value ->
            Log.i("__walpMain", "$option --> $value")
            layerManagers[pickedLayer]?.setConfigValue(option, value)
        }

        // init config setting values
        settingVelocity = SettingData(ConfigOption.VELOCITY, 0, updatePickedLayer)
        settingXOffset = SettingData(ConfigOption.X_OFFSET, 0, updatePickedLayer)
        settingYOffset = SettingData(ConfigOption.Y_OFFSET, 0, updatePickedLayer)
        settingScale = SettingData(ConfigOption.SCALE, 0, updatePickedLayer)
        settingSpeed = SettingData(ConfigOption.SPEED, 0, updatePickedLayer)

        // let's fill the RecyclerView for now
        settingsList.add(settingVelocity)
        settingsList.add(settingXOffset)
        settingsList.add(settingYOffset)
        settingsList.add(settingScale)
        settingsList.add(settingSpeed)

        // creating a wide imageView so horizontalScrollView will have a ways to scroll
//        binding.ivCanvas.layoutParams.width = maxBackgroundWidthPx + screenWidth + 100 // magic. fix later
//        binding.ivCanvas.isHorizontalScrollBarEnabled = true

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

//        binding.sbSpeed.max = maxSpeed
//        binding.sbSpeed.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
//            override fun onStartTrackingTouch(seekBar: SeekBar) {}
//            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                val slayer = binding.ivCanvas.getLayer(pickedLayer) ?: return
//                slayer.velocity = binding.sbSpeed.progress * - 1.0
//                binding.ivCanvas.setLayer(pickedLayer, slayer)
//            }
//            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                binding.tvSpeedValue.text = "$progress"
//                layerManagers[pickedLayer]!!.layer.velocity = progress
//            }
//        })
//
//        binding.sbOffset.max = screenWidth // add help note: if you can't find layer, check your offset
//        binding.sbOffset.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
//            override fun onStartTrackingTouch(seekBar: SeekBar) {}
//            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                val slayer = binding.ivCanvas.getLayer(pickedLayer) ?: return
//                slayer.offset = binding.sbOffset.progress * 1.0
//                binding.ivCanvas.setLayer(pickedLayer, slayer)
//            }
//            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                binding.tvOffsetValue.text = "$progress"
//                layerManagers[pickedLayer]!!.layer.offset = progress
//            }
//        })

//        val offsetView = ConfigOptionSelectorView.create(applicationContext, "offset", "21", null)
//        binding.linvOptionMenu.addView(offsetView)

        binding.sbOffset.max = screenWidth // add help note: if you can't find layer, check your offset
        binding.sbOffset.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val slayer = binding.ivCanvas.getLayer(pickedLayer) ?: return
                slayer.offset = binding.sbOffset.progress * 1.0
                binding.ivCanvas.setLayer(pickedLayer, slayer)
            }
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
        binding.sbPage.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.ivCanvas.draw(progress * maxBackgroundWidthPx / pageNum)
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
                val serviceLayers = mutableMapOf<Int, ServiceLayer>()
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
                    val serviceLayer = ServiceLayer.create(
                        context = applicationContext,
                        uriString = layerDO.uri,
                        imageType = layerDO.imageType,
                        velocity = layerDO.velocity,
                        offset = layerDO.offset
                    ) ?: continue
                    serviceLayers[layerDO.level] = serviceLayer
                    lm?.setUriDrawable(uri, drawableFromUri(uri))
                    lm?.updateUIElementImages()
                }
                binding.ivCanvas.setLayers(serviceLayers)
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

        binding.ivCanvas.clear()
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

        // updating UI
        val serviceLayer = ServiceLayer.create(
            applicationContext,layer.layer.uri.toString(), layer.layer.imageType.value, layer.layer.velocity, layer.layer.offset
        )
        if (serviceLayer != null) {
            binding.ivCanvas.setLayer(layer.level, serviceLayer)
        }
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

    // select which setting to be expanded for editing
    override fun onItemClick(view: View?, position: Int) {
        Log.e("__walpMain", "boohoo!!")

        if (view == null) {
            return
        }

        // make the scroll visible
        val sb = view.findViewById<SeekBar>(R.id.sbSetting)
        sb.visibility = View.VISIBLE
    }

}