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
    private var pickedLayer = 1

    private var pickedPhoto1 : Uri? = null
    private var pickedBitMap1 : Bitmap? = null
    private var pickedPhoto2 : Uri? = null
    private var pickedBitMap2 : Bitmap? = null
    private var pickedPhoto3 : Uri? = null
    private var pickedBitMap3 : Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivSmallBot.setOnClickListener(View.OnClickListener { view ->
            pickedLayer = 1
        })
        binding.ivSmallMid.setOnClickListener(View.OnClickListener { view ->
            pickedLayer = 2
        })
        binding.ivSmallTop.setOnClickListener(View.OnClickListener { view ->
            pickedLayer = 3
        })
        binding.btnSelectImage.setOnClickListener(View.OnClickListener { view ->
            pickPhoto(view)
        })
        binding.btnSetWallpaper.setOnClickListener(View.OnClickListener { view ->

            var intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, WallpaperService::class.java))
            startActivity(intent)

//            val wm = WallpaperManager.getInstance(this)
//            wm.setBitmap(pickedBitMap)
//            wm.setWallpaperOffsetSteps(.5f, 0f)
        })
    }

    fun pickPhoto (view: View) {
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

    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onActivityResult(requestCode, resultCode, data)",
        "androidx.appcompat.app.AppCompatActivity"
    )
    )
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i("__walpMain", "called onActivityResult")
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            pickedPhoto1 = data.data;
            Log.i("__walpMain", data.data.toString())
            if (data.data != null) {
                if (Build.VERSION.SDK_INT >= 28) {
                    val source = ImageDecoder.createSource(this.contentResolver, data.data!!)
//                    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                    this.contentResolver.takePersistableUriPermission(data.data!!, flag)

                    if (pickedLayer == 1) {
                        pickedPhoto1 = data.data;
                        pickedBitMap1 = ImageDecoder.decodeBitmap(source)

                        binding.ivSampleBot.setImageBitmap(pickedBitMap1)
                        binding.ivSmallBot.setImageBitmap(pickedBitMap1)

                        lifecycleScope.launch {
                            PreferenceManager.saveL1(applicationContext, data.data.toString())
                        }
                    } else if (pickedLayer == 2) {
                        pickedPhoto2 = data.data;
                        pickedBitMap2 = ImageDecoder.decodeBitmap(source)

                        binding.ivSampleMid.setImageBitmap(pickedBitMap2)
                        binding.ivSmallMid.setImageBitmap(pickedBitMap2)

                        lifecycleScope.launch {
                            PreferenceManager.saveL2(applicationContext, data.data.toString())
                        }
                    } else if (pickedLayer == 3) {
                        pickedPhoto3 = data.data;
                        pickedBitMap3 = ImageDecoder.decodeBitmap(source)
                        
                        binding.ivSampleTop.setImageBitmap(pickedBitMap3)
                        binding.ivSmallTop.setImageBitmap(pickedBitMap3)

                        lifecycleScope.launch {
                            PreferenceManager.saveL3(applicationContext, data.data.toString())
                        }
                    }
                }
                else {
                    pickedBitMap1 = MediaStore.Images.Media.getBitmap(this.contentResolver, pickedPhoto1)
                    binding.ivSampleBot.setImageBitmap(pickedBitMap1)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



}