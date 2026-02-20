package com.example.parallax.ui.screens.homescreen

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.parallax.ImageType
import com.example.parallax.R
import androidx.core.net.toUri

class HomeScreen {

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

    // the little buttons to press to select which layer you're on
//    @Composable
//    fun LayerSelectButton() {
//        Text("yoohoo")
//    }

    // the background image view
    @Composable
    fun LayerSelectButton(layer: Layer) {

        Image(
            painter = painterResource(R.drawable.flower),
            contentDescription = "Contact profile picture",
            modifier = Modifier
                .height(120.dp)
                .width(80.dp)
                .border(1.dp, androidx.compose.ui.graphics.Color.White)
        )

    }

    @Composable
    fun LayerSelectButtons(layers: List<Layer>) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            for (layer in layers) {
                LayerSelectButton(layer)
            }
        }
    }

    @Composable
    fun BackgroundLayer(layer: Layer) {

        Image(
            painter = painterResource(R.drawable.flower),
            contentDescription = "Contact profile picture",
            modifier = Modifier
                .fillMaxHeight(1f)
//                .fillMaxSize(1f)
        )

    }

    @Preview
    @Composable
    fun PreviewSmallView() {
        BackgroundLayer(Layer())
        LayerSelectButtons(List<Layer>(3) { Layer() } )

    }

}