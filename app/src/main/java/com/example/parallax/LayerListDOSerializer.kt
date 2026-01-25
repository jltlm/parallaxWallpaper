package com.example.parallax

import androidx.datastore.core.Serializer
import com.example.parallax.datastore.LayerListDO
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object LayerListDOSerializer : Serializer<LayerListDO> {

    override val defaultValue: LayerListDO =
        LayerListDO.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LayerListDO =
        try {
            LayerListDO.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            defaultValue
        }

    override suspend fun writeTo(
        t: LayerListDO,
        output: OutputStream
    ) = t.writeTo(output)
}
