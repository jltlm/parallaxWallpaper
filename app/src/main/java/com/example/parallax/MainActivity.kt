package com.example.parallax

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.RadioButton
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
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible

private const val DATA_STORE_FILE_NAME = "layers.pb"

val Context.layerDataStore: DataStore<LayerListDO> by dataStore(
    fileName = DATA_STORE_FILE_NAME,
    serializer = LayerListDOSerializer
)

class LayerManager(
    val level: Int,
    var layer: ParallaxLayer,
    var imageViewSmall: ImageButton
) {

    fun isEmpty(): Boolean {
        return this.layer.img == ParallaxImg.Empty() || this.layer.uri == ""
    }

    fun setUriServiceImg (uri: Uri, img: ParallaxImg) {
        this.layer.uri = uri.toString()
        this.layer.img = img
    }

    fun setImageType (imageType: ImageType, img: ParallaxImg) {
        this.layer.imageType = imageType
        this.layer.img = img
    }

    fun clearLayer() {
        this.layer.velocity = 0.0
        this.layer.offset = 0.0
        this.layer.uri = ""
        this.layer.imageType = ImageType.BITMAP
        this.layer.img = ParallaxImg.Empty()
        updateUIElementImages()
    }

    fun loadLayer(l: ParallaxLayer) {
        this.layer = l
    }

    fun updateUIElementImages() {
        when (this.layer.img) {
            is ParallaxImg.Empty -> {
                imageViewSmall.setImageBitmap(null)
            }
            else -> { // AnimatedGif, InteractiveGif, StaticBitmap, Error. less movement for more energy saving!
                val img = (this.layer.img.img as Bitmap)
                imageViewSmall.setImageBitmap(img)
            }
        }
    }

    fun setConfigValue(configOption: ConfigOption, amount: Double) {
        when (configOption) {
            ConfigOption.VELOCITY -> this.layer.velocity = amount * 1.0
            ConfigOption.X_OFFSET -> this.layer.offset = amount * 1.0
            ConfigOption.Y_OFFSET ->  println("offset y")
            ConfigOption.SPEED ->  println("speed")
            ConfigOption.IMAGE_TYPE ->  println("imageType")
            ConfigOption.SCALE ->  println("scale")
        }
    }
}

class MainActivity : AppCompatActivity(), RecyclerViewImgEditAdapter.ItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private var pickedLayer : Int = -1
    private val layerManagers : MutableMap<Int, LayerManager> = mutableMapOf()
    private var pageNum = 3
//    private val maxSpeed = 50
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private var maxBackgroundWidthPx = screenWidth * 3
    private lateinit var settingRecyclerViewAdapter: RecyclerViewImgEditAdapter

    // data to populate the RecyclerView and Data with
    val settingsList = mutableListOf<ConfigOptionDataUI>()
    lateinit var settingVelocity: ConfigOptionDataUI
    lateinit var settingXOffset: ConfigOptionDataUI
    lateinit var settingYOffset: ConfigOptionDataUI
    lateinit var settingScale : ConfigOptionDataUI
    lateinit var settingSpeed : ConfigOptionDataUI

//    let, also, apply, with. cool stuff

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("__walpMain", "Parallax Wallpaper activity onCreate")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // gonna want a loop for this later or something
        layerManagers[0] = LayerManager(0, ParallaxLayer.create(applicationContext), binding.ivSmallBot)
        layerManagers[1] = LayerManager(1, ParallaxLayer.create(applicationContext), binding.ivSmallMid)
        layerManagers[2] = LayerManager(2, ParallaxLayer.create(applicationContext), binding.ivSmallTop)

        loadAll()

        // ============== setting setup ============
        // set up the RecyclerView for settings
        val recyclerView: RecyclerView = findViewById(R.id.rvEditOptions)
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        settingRecyclerViewAdapter = RecyclerViewImgEditAdapter(this, settingsList)
        settingRecyclerViewAdapter.setClickListener(this)
        recyclerView.setAdapter(settingRecyclerViewAdapter)

        // this is the fancy schmancy hi-tech wiring between the UI value and the data value!!
        val updatePickedLayer : (ConfigOption, Double) -> Unit = { option, value ->
//            Log.i("__walpMain", "$option --> $value")
            layerManagers[pickedLayer]?.setConfigValue(option, value)
        }

        fun makeSettingData(option: ConfigOption): ConfigOptionDataUI {
            return ConfigOptionDataUI(
                option,
//                layerManagers[pickedLayer]!!.getConfigValue(option),
                0.0,
                updatePickedLayer,
                settingRecyclerViewAdapter
            )
        }

        // init config setting values
        settingVelocity = makeSettingData(ConfigOption.VELOCITY)
        settingXOffset = makeSettingData(ConfigOption.X_OFFSET)
        settingYOffset = makeSettingData(ConfigOption.Y_OFFSET)
        settingScale = makeSettingData(ConfigOption.SCALE)
        settingSpeed = makeSettingData(ConfigOption.SPEED)

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
                    // toggle border for prev selected layer
                    if (pickedLayer == index) {
//                        iv.isSelected = !iv.isSelected
                        if (iv.foreground == null) {
                            iv.foreground = ContextCompat.getDrawable(this, R.drawable.selected_layer_border)
                        } else {
                            iv.foreground = null
                        }
                    } else {
                        if (0 <= pickedLayer && pickedLayer < binding.layoutIvSmall.childCount) {
//                        prevSelectedIv.isSelected = false
                            val prevSelectedIv = binding.layoutIvSmall[binding.layoutIvSmall.childCount - 1 - pickedLayer]
                            prevSelectedIv.foreground = null
                        }
//                        iv.isSelected = true
                        iv.foreground = ContextCompat.getDrawable(this, R.drawable.selected_layer_border)
                    }

                    // toggle logic
                    if (pickedLayer == index) { // if clicking the same layer as selected, toggle
                        binding.llLayerImage.visibility = if (binding.llLayerImage.isVisible) View.INVISIBLE else View.VISIBLE

                        // as visible as the img setter if there is an img selected for the layer, else invisible
                        binding.llLayerConfigs.visibility = if (!lm.isEmpty()) binding.llLayerImage.visibility else View.INVISIBLE
                        Log.i("__walpMain", "$pickedLayer - is empty:${lm.isEmpty()}, and imgType=${lm.layer.imageType} so config visibility is ${binding.llLayerConfigs.visibility}")
                    } else { // if opening a new layer's settings, make visible
                        binding.llLayerImage.visibility = View.VISIBLE

                        // visible if there is an img selected for the layer, else invisible
                        binding.llLayerConfigs.visibility = if (!lm.isEmpty()) View.VISIBLE else View.INVISIBLE
                    }

                    pickedLayer = index

                    Log.i("__walpMain", "picked layer $pickedLayer")

                    // load values into settings UI
                    settingVelocity.setSettingValue(lm.layer.velocity)
                    settingXOffset.setSettingValue(lm.layer.offset)

                    // add the other ones in later
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        binding.rgImageTypeSelector.setOnCheckedChangeListener { _, checkedId ->

            if (pickedLayer == -1) {
                return@setOnCheckedChangeListener
            }
            val lm = layerManagers[pickedLayer]!!

            if (checkedId != -1) {
                val selectedRadioButton = findViewById<RadioButton>(checkedId)
                val text = selectedRadioButton.text
                println("Selected: $text")

                val toImageType = when (text) {
                    resources.getString(R.string.imageTypeStatic) -> ImageType.BITMAP
                    resources.getString(R.string.imageTypeInteractiveGif) -> ImageType.INTERACTIVE_GIF
                    resources.getString(R.string.imageTypeGif) -> ImageType.CONTINUOUS_GIF
                    else -> ImageType.BITMAP
                }

                lm.setImageType(toImageType, ParallaxLayer.getImageFromUri(applicationContext, lm.layer.uri.toUri(), toImageType))

                // update UI sample parallax wallpaper
                binding.ivCanvas.setLayerValues(lm.level, lm.layer)
            } else {
                // No radio button is selected (e.g., after clearCheck())
                lm.setImageType(ImageType.BITMAP, ParallaxImg.Empty())
            }

        }

        binding.btnDeleteImage.setOnClickListener {
            val lm = layerManagers[pickedLayer] ?: return@setOnClickListener
            lm.clearLayer()

            binding.ivCanvas.clearLayer(lm.level)

            binding.llLayerConfigs.visibility = View.INVISIBLE
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

        binding.btnSetWallpaper.setOnClickListener { _ ->
            saveAll()
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, WallpaperService::class.java)
            )
            startActivity(intent)
        }

        //////// page num

        binding.sbPage.max = pageNum
        binding.sbPage.setOnSeekBarChangeListener ( object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.ivCanvas.draw(progress * maxBackgroundWidthPx / pageNum)
                }
            }
        })

    }

//    THIS IS UI SETUP NO MORE

    /**
     * init from settings
     */
    private fun loadAll() {
        lifecycleScope.launch {
            LayerRepository.getLayerFlow(applicationContext).collect { layerDOs: List<LayerDO> ->
                Log.d("__walpMain", "loading ${layerDOs.size} layers from repo")
                val layers = mutableMapOf<Int, ParallaxLayer>()
                for (layerDO in layerDOs) {
                    val uri = layerDO.uri.toUri()
                    Log.i("__walpMain", "loading ${layerDO.level}: vel:${layerDO.velocity} offset:${layerDO.offset} type:${layerDO.imageType}")

                    // updating UI
                    val layer = ParallaxLayer.create(
                        context = applicationContext,
                        uriString = layerDO.uri,
                        imageType = layerDO.imageType,
                        velocity = layerDO.velocity.toDouble(),
                        offset = layerDO.offset.toDouble()
                    )

                    // add layer to layerManagers
                    val lm = layerManagers[layerDO.level]
                    lm?.loadLayer(layer)

                    layers[layerDO.level] = layer
                    lm?.setUriServiceImg(uri, ParallaxLayer.getImageFromUri(applicationContext, uri, ImageType.BITMAP))
                    lm?.updateUIElementImages()
                }
                binding.ivCanvas.setLayers(layers)
            }
        }
    }

    /**
     * save all settings
     */
    private fun saveAll() {
        lifecycleScope.launch {
            LayerRepository.clearLayers(applicationContext)

            for ((_, lm) in layerManagers) {
                val l = lm.layer
                Log.i("__walpMain", "saving ${lm.level}: vel:${l.velocity} offset:${l.offset} type:${l.imageType.value}")

                LayerRepository.addLayer(
                    applicationContext,
                    lm.level,
                    l.uri,
                    l.imageType.value,
                    l.velocity.toInt(),
                    l.offset.toInt()
                )
            }
        }
    }

    private fun clearAll() {
//        lifecycleScope.launch {
//            Log.i("__walpMain", "clearing layers")
//            LayerRepository.clearLayers(applicationContext)
//        }
        for ((_, lm) in layerManagers) {
            val l = lm.layer
            if (l.img == ParallaxImg.Empty()) { continue }
            lm.clearLayer()
        }
        binding.sbPage.progress = 0
//        binding.sbSpeed.progress = 0
//        binding.sbOffset.progress = 0

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
        val img = ParallaxLayer.getImageFromUri(applicationContext, uri, ImageType.BITMAP)

        val lm: LayerManager = layerManagers[pickedLayer]!!
        lm.clearLayer()
        lm.setUriServiceImg(uri, img)
        lm.updateUIElementImages()

        // updating UI background sample wallpaper
        binding.ivCanvas.setLayer(lm.level, lm.layer)

        binding.llLayerConfigs.visibility = View.VISIBLE
        if (!lm.isEmpty()) {
            Log.i("__walpMain", "apparently, layer is not empty. uri: ${lm.layer.uri}")
        }
    }

    // select which setting to be expanded for editing
    override fun onItemClick(view: View?, position: Int) { }

}