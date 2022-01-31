package com.example.digitclassifier

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

    }
}