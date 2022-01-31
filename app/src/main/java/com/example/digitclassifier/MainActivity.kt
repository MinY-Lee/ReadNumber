package com.example.digitclassifier

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import com.divyanshu.draw.widget.DrawView

class MainActivity : AppCompatActivity() {

    private var drawView: DrawView? = null
    private var clearButton: Button? = null
    private var predictedTextView: TextView? = null
    private var digitClassfier = DigitClassfier(this)   // DigitClassfier 이용 위한 선언

    override fun onCreate(savedInstanceState: Bundle?) {    // Activity 생성시 호출
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup view instances
        // 화면 컨트롤 연결
        drawView = findViewById(R.id.draw_view)
        drawView?.setStrokeWidth(70.0f)
        drawView?.setColor(Color.WHITE)
        drawView?.setBackgroundColor(Color.BLACK)
        clearButton = findViewById(R.id.clear_button)
        predictedTextView = findViewById(R.id.predicted_text)

        // Setup clear drawing button
        clearButton?.setOnClickListener {
            drawView?.clearCanvas()
            predictedTextView?.text = getString(R.string.tfe_dc_prediction_text_placeholder)
        }

        // Setup classification trigger so that it classify after every stroke drew
        // 손으로 입력하는 drawView 에서 onTouchListener 인터페이스의 이벤트들 구현
        drawView?.setOnTouchListener { _, event ->
            // As we have interrupted DrawView's touch event,
            // we first need to pass touch events through to the instance for the drawing to show up
            drawView?.onTouchEvent(event)

            // Then if user finished a touch event, run classification
            if (event.action == MotionEvent.ACTION_UP) {
                classifyDrawing()
            }

            true
        }

        // Setup digit classifier
        digitClassfier
            .initialize()
            .addOnFailureListener { e -> Log.e(TAG, "Error to setting up digit classifier.", e) }
    }

    private fun classifyDrawing() {
        val bitmap = drawView?.getBitmap()

        if((bitmap != null) && (digitClassfier.isInitialized)) {
            digitClassfier
                .classifyAsync(bitmap)  // 손으로 입력한 숫자를 bitmap 이미지로 가져와 digitClassifier 에 넘겨주는 코드
                .addOnSuccessListener { resultText -> predictedTextView?.text = resultText }
                .addOnFailureListener { e ->
                        predictedTextView?.text = getString(
                            R.string.tfe_dc_classification_error_message,
                            e.localizedMessage
                        )
                    Log.e(TAG, "Error classifying drawing.", e)
                }
        }
    }

    override fun onDestroy() {  // Activity 가 없어질 때 불려오는 메소드(메로리 릭 방지)
        digitClassfier.close()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}