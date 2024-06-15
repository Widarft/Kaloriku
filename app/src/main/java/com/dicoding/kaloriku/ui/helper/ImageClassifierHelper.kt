package com.dicoding.kaloriku.ui.helper

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.dicoding.kaloriku.ml.FoodDetectionModel2
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImageClassifierHelper(private val context: Context) {

    private lateinit var model: FoodDetectionModel2

    fun setupImageClassifier() {
        model = FoodDetectionModel2.newInstance(context)
    }

    fun classifyStaticImage(image: Bitmap): String {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(416, 416, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .build()


        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
        val byteBuffer = convertBitmapToByteBuffer(tensorImage.bitmap)

        // Debug: Print first few values of the byte buffer
        byteBuffer.rewind()
        val debugBuffer = ByteArray(100)
        byteBuffer.get(debugBuffer, 0, 100)
        Log.d("ImageClassifierHelper", "Input ByteBuffer: ${debugBuffer.joinToString(", ")}")

        // Debug: Visualize the preprocessed image
        val preprocessedBitmap = convertByteBufferToBitmap(byteBuffer)
        // Use an ImageView to display the bitmap (you need to do this in your activity/fragment)
        // imageView.setImageBitmap(preprocessedBitmap)

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 416, 416, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        // Extract the float array from the output tensor
        val outputArray = outputFeature0.floatArray

        model.close()

        // Debugging: print the output array
        val outputString = outputArray.joinToString(", ") { "%.2f".format(it) }
        Log.d("ImageClassifierHelper", "Output Array: $outputString")

        // Process the output array to get a meaningful result
        return processOutput(outputArray)
    }

    private fun convertByteBufferToBitmap(byteBuffer: ByteBuffer): Bitmap {
        byteBuffer.rewind()
        val bitmap = Bitmap.createBitmap(416, 416, Bitmap.Config.ARGB_8888)
        val intValues = IntArray(416 * 416)
        for (i in intValues.indices) {
            val r = (byteBuffer.getFloat() * 255).toInt()
            val g = (byteBuffer.getFloat() * 255).toInt()
            val b = (byteBuffer.getFloat() * 255).toInt()
            intValues[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(intValues, 0, 416, 0, 0, 416, 416)
        return bitmap
    }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 416 * 416 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(416 * 416)
        bitmap.getPixels(intValues, 0, 416, 0, 0, 416, 416)
        var pixel = 0

        for (i in 0 until 416) {
            for (j in 0 until 416) {
                val value = intValues[pixel++]
                // Normalize the pixel values to [0, 1] range
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }

        return byteBuffer
    }



    private fun processOutput(outputArray: FloatArray): String {
        val labels = listOf("apel", "gudeg", "anggur", "capcay", "kacang", "kentang", "bakwan", "donat", "bakso", "ikan", "jeruk", "kopi", "air", "burger", "kerupuk", "durian", "es_krim", "batagor", "ayam", "cakwe", "crepes", "fu_yung_hai", "cumi", "bubur", "kebab")
        println("$outputArray")
        val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: -1

        return if (maxIndex != -1) {
            val label = labels[maxIndex]
            val confidence = outputArray[maxIndex]
            "$label: ${"%.2f".format(confidence * 100)}%"
        } else {
            "Unknown"
        }
    }
}