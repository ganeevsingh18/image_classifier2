package com.example.imageclassifier2

import android.graphics.Bitmap

interface LandmarkClassifier {
    fun classify(bitmap: Bitmap): String
}