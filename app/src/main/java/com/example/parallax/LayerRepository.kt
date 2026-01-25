package com.example.parallax

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.example.parallax.datastore.LayerDO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.FileNotFoundException
import java.io.InputStream


object LayerRepository {

    fun getLayerFlow( context: Context ): Flow<MutableList<LayerDO>> {
        return context.layerDataStore.data
                .map { it.layersList }
    }

//    suspend fun getLayers(context: Context ) : MutableList<LayerDO> {
//        var out: MutableList<LayerDO> = mutableListOf()
//        getLayerFlow(context).first(){ layerDOs: List<LayerDO> ->
//            out = layerDOs.toMutableList()
//        }
//        return out
//    }
//    val layersFlow: Flow<List<LayerDO>> =
//        context.layerDataStore.data
//            .map { it.layersList }

    suspend fun addLayer(
        context: Context,
        level: Int,
        uri: String,
        velocity: Int,
        offset: Int
    ) {
        context.layerDataStore.updateData { current ->
            current.toBuilder()
                .addLayers(
                    LayerDO.newBuilder()
                        .setLevel(level)
                        .setUri(uri)
                        .setVelocity(velocity)
                        .setOffset(offset)
                        .build()
                )
                .build()
        }
    }

//    suspend fun removeLayer(index: Int) {
//        context.layerDataStore.updateData { current ->
//            current.toBuilder()
//                .removeLayers(index)
//                .build()
//        }
//    }

    suspend fun clearLayers(context: Context) {
        context.layerDataStore.updateData { current ->
            current.toBuilder()
                .clearLayers()
                .build()
        }
    }

}