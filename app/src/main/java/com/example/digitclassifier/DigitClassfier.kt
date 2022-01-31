package com.example.digitclassifier

import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// 생성자를 위해 context 추가

class DigitClassfier(private val context: Context) {

    private var interpreter: Interpreter? = null
    var isInitialized = false
        private set

    // Executor to run inference task in the background
    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth: Int = 0    // will be inferred from TF Lite model
    private var inputImageHeight: Int = 0   // will be inferred from TF Lite model
    private var modelInputSize: Int = 0     // will be inferred from TF Lite model

    fun initialize(): Task<Void>{
        val task = TaskCompletionSource<Void>()
        executorService.execute{
            try {
                initializeInterpreter()     // task를 초기화하는 메소드
                task.setResult(null)
            } catch(e: IOException) {
                task.setException(e)
            }
        }
        return task.task
    }

    @Throws (IOException::class)
    private fun initializeInterpreter(){
        // Load the TF Lite model
        val assetManager = context.assets
        val model = loadModelFile(assetManager)

        // Initialize TF Lite Interpreter with NNAPI enabled
        val options = Interpreter.Options()
        options.setUseNNAPI(true)
        val interpreter = Interpreter(model, options)

        // Read input shape from model file
        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]     // tensorflow 모델 정의시 input_shape와 매치
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE

        // Finish interpreter initialization
        this.interpreter = interpreter
        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter.")


    }

    @Throws (IOException::class)
    private fun loadModelFile(assetManager: AssetManager) : ByteBuffer{
        // assets 에 저장되어 있는 mnist.tflite 파일 로드
        val fileDescriptor = assetManager.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    private fun classify(bitmap: Bitmap): String {  // 동기적으로 분석 * (bitmap형식의 이미지를 인수로 받아)
        if(!isInitialized){
            throw IllegalStateException("TF LiteInterpreter is not initialized yet.")
        }

        var startTime: Long
        var elapsedTime: Long

        // Preprocessing: resize the input
        startTime = System.nanoTime()
        val resizedImage = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)    // ** Byte 로 변환
        elapsedTime = (System.nanoTime() - startTime) / 1000000
        Log.d(TAG, "Preprocessing time = " + elapsedTime + "ms")

        startTime = System.nanoTime()
        val result = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT)}
        interpreter?.run(byteBuffer, result)    // *** input인수에 넣은 후 output 인수로 result 받음
        elapsedTime = (System.nanoTime() - startTime) / 1000000
        Log.d(TAG, "Inference time = " + elapsedTime + "ms")

        return getOutputString(result[0])   // **** getOutputString 으로 처리한 후, String 반환
    }

    fun classifyAsync(bitmap: Bitmap) : Task<String> {  // 이미지를 입력해 비동기로 분석, 백그라운드 형태로 실행
        val task = TaskCompletionSource<String>()
        executorService.execute {
            val result = classify(bitmap)   // 이미지를 인수로 받아 classify 에 넣은 후 바로 호출(나머지는 classify 가 알아서)
            task.setResult(result)
        }
        return task.task
    }

    fun close() {   // 사용 후 close 로 닫아줘야.. 아니면 나중에 문제생김김
       executorService.execute {
            interpreter?.close()
            Log.d(TAG, "Closed TFLite interpreter.")
        }
    }

    // 이미지를 Byte 로 변환
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for(pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            // Convert RGB to grayscale and normalize pixel value to [0..1]
            val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    // output 문자열 만들기
    private fun getOutputString(output: FloatArray): String {
        val maxIndex = output.indices.maxBy { output[it] } ?: -1
        return "Prediction Result: %d\nConfidence: %2f".format(maxIndex, output[maxIndex])
    }

    companion object {
        private const val TAG = "DigitClassifier"

        private const val MODEL_FILE = "mnist.tflite"

        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 1

        private  const val OUTPUT_CLASSES_COUNT = 10
    }

}







