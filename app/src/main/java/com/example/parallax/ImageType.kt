package com.example.parallax

enum class ImageType(val value: Int) {
    BITMAP(1),
    CONTINUOUS_GIF(2),
    INTERACTIVE_GIF(3);

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == (if (value == 0) 1 else value) }
    }
}