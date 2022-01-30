package com.example.digitclassifier

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// 생성자를 위해 context 추가

class DigitClassfier(private val context: Context) {

    private var interpreter: Interpreter? = null
    var inInitialized = false
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

    }


}