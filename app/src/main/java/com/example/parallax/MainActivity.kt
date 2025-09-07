package com.example.parallax

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.parallax.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val CONF_NAME = "settings"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = CONF_NAME)

class Layer() {

    lateinit var uri: Uri
    lateinit var bitmap: Bitmap
    var offset: Float = 0f

}

class LayerManager(level: Int, layer: Layer) {

    val level : Int = level
    val layer: Layer = layer

    lateinit var imageViewSmall: ImageButton
    lateinit var imageViewSample: ImageView

    fun getUri(): Uri { return this.layer.uri }
//    fun getBitmap(): Bitmap { return this.layer.bitmap }
//    fun getOffset(): Float { return this.layer.offset }

    fun setDetails (uri: Uri, bitmap: Bitmap, offset: Float) {
        this.layer.uri = uri
        this.layer.bitmap = bitmap
        this.layer.offset = offset
    }

    fun updateUIElementImages() {
        imageViewSmall.setImageBitmap(this.layer.bitmap)
        imageViewSample.setImageBitmap(this.layer.bitmap)
    }

}

object PreferenceManager {
    private val L1_KEY = stringPreferencesKey("layer1_key")
    private val L2_KEY = stringPreferencesKey("layer2_key")
    private val L3_KEY = stringPreferencesKey("layer3_key")

    suspend fun saveL1(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[L1_KEY] = value
        }
    }
    suspend fun saveL2(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[L2_KEY] = value
        }
    }
    suspend fun saveL3(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[L3_KEY] = value
        }
    }

    fun getL1(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[L1_KEY].toString()
        }
    }
    fun getL2(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[L2_KEY].toString()
        }
    }
    fun getL3(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[L3_KEY].toString()
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pickedLayer : Int = 1
    private val layerManagers : MutableMap<Int, LayerManager> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        for (i in 1..3) {
            val layerManager = LayerManager(i, Layer())

            when (i) {
                1 -> {
                    layerManager.imageViewSmall = binding.ivSmallBot
                    layerManager.imageViewSample = binding.ivSampleBot
                }
                2 -> {
                    layerManager.imageViewSmall = binding.ivSmallMid
                    layerManager.imageViewSample = binding.ivSampleMid
                }
                3 -> {
                    layerManager.imageViewSmall = binding.ivSmallTop
                    layerManager.imageViewSample = binding.ivSampleTop
                }
            }

            layerManagers[i] = layerManager
        }
        binding.ivSmallBot.setOnClickListener {
            pickedLayer = 1
        }
        binding.ivSmallMid.setOnClickListener{
            pickedLayer = 2
        }
        binding.ivSmallTop.setOnClickListener {
            pickedLayer = 3
        }
        binding.btnSelectImage.setOnClickListener { view ->
            pickPhoto(view)
        }
        binding.btnSetWallpaper.setOnClickListener { view ->
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, WallpaperService::class.java)
            )
            startActivity(intent)
        }
    }

    /**
     * save all settings
     */
    private fun saveAll() {
        lifecycleScope.launch {
            PreferenceManager.saveL1(applicationContext, layerManagers[1]?.getUri().toString())
            PreferenceManager.saveL2(applicationContext, layerManagers[2]?.getUri().toString())
            PreferenceManager.saveL3(applicationContext, layerManagers[3]?.getUri().toString())
        }
    }

    /**
     * save settings for a particular layer
     * @param layer the layer to save the details of
     */
    private fun save(layer: Int) {
        lifecycleScope.launch {
            when (layer) {
                1 -> {
                    PreferenceManager.saveL1(applicationContext, layerManagers[1]?.getUri().toString())
                }
                2 -> {
                    PreferenceManager.saveL2(applicationContext, layerManagers[2]?.getUri().toString())
                }
                3 -> {
                    PreferenceManager.saveL3(applicationContext, layerManagers[3]?.getUri().toString())
                }
            }
        }
    }

        private fun pickPhoto (view: View) {
        Log.i("__walpMain", "called pickPhoto")
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            Log.i("__walpMain", "requesting perms")
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            Log.i("__walpMain", "already have perms")
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, 2)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.i("__walpMain", "called onRequestPermissionsResult")
        if (requestCode == 1) {
            Log.i("__walpMain", "request granted?${grantResults[0]}")
            Log.i("__walpMain", "${ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)}")
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("__walpMain", "request granted x2??")
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, 2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * returns a bitmap from uri
     */
    private fun bitmapFromUri(uri: Uri) : Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(this.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source)
        } else {
            return MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onActivityResult(requestCode, resultCode, data)",
        "androidx.appcompat.app.AppCompatActivity"))
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i("__walpMain", "called onActivityResult")
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            Log.i("__walpMain", data.data.toString())
            if (data.data != null) {

                val layer: LayerManager = layerManagers[pickedLayer]!!
                layer.setDetails(data.data!!, bitmapFromUri(data.data!!), 0f)
                layer.updateUIElementImages()

                save(pickedLayer)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}