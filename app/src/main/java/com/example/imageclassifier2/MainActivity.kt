package com.example.imageclassifier2

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import org.tensorflow.lite.support.image.TensorImage
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult


import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import java.lang.Exception

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var classificationResult by remember { mutableStateOf("No result yet") } // Default text before classification
    val context = LocalContext.current

    // Define the launcher for picking the image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Assume cropAndConvertImage is a function defined elsewhere that processes the image
            bitmap = cropAndConvertImage(context, it)
            if (bitmap == null) Log.e("ImageProcessing", "Failed to load or crop image")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = {
            imagePickerLauncher.launch("image/*")  // Launch image picker
        }) {
            Text(text = "Select an Image")
        }

        Button(onClick = {
            bitmap?.let { bmp ->
                classificationResult = classifyImage(bmp, context) // Update the classification result
            } ?: Log.e("Classifier", "Bitmap is null")
        }) {
            Text(text = "Classify Image")
        }

        Text(text = classificationResult, modifier = Modifier.padding(top = 20.dp)) // Display the classification result
    }
}


fun cropAndConvertImage(context: Context, imageUri: Uri): Bitmap? {
    try {
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            if (inputStream == null) {
                Log.e("ImageCrop", "InputStream is null")
                return null
            }
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            if (originalBitmap == null) {
                Log.e("ImageCrop", "Failed to decode Bitmap")
                return null
            }

            // Check if original dimensions are sufficient for cropping
            if (originalBitmap.width < 312 || originalBitmap.height < 312) {
                Log.e("ImageCrop", "Original image dimensions are too small for cropping")
                return null
            }

            val dimension = minOf(originalBitmap.width, originalBitmap.height)
            val x = (originalBitmap.width - dimension) / 2
            val y = (originalBitmap.height - dimension) / 2

            val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, dimension, dimension)
            val scaleWidth = 312f / croppedBitmap.width
            val scaleHeight = 312f / croppedBitmap.height
            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)

            return Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.width, croppedBitmap.height, matrix, false)
        }
    } catch (e: Exception) {
        Log.e("ImageCrop", "Error processing image", e)
        return null
    }
    return null
}


fun classifyImage(bitmap: Bitmap, context: Context): String {
    val classfier= TfLiteLandmarkClassifier(context,0.5f,1)
    val result=classfier.classify(bitmap)
    return result
}