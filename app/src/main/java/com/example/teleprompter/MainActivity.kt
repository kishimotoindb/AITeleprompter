package com.example.teleprompter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.teleprompter.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private var scrollSpeed: Float = 1.0f
    private var isScrolling = false
    private var isAIMode = false
    private lateinit var speechRecognizer: SpeechRecognizer
    private var promptText = ""
    private var currentPosition = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 获取传入的参数
        intent.getStringExtra(EditTextActivity.EXTRA_PROMPT_TEXT)?.let { text ->
            binding.promptText.text = text
            promptText = text
        }
        scrollSpeed = intent.getFloatExtra(EditTextActivity.EXTRA_SCROLL_SPEED, 1.0f)
        
        setupSpeechRecognizer()
        setupUI()
        checkPermission()
    }
    
    private fun setupUI() {
        binding.apply {
            // 匀速提词按钮
            normalStartButton.setOnClickListener {
                isAIMode = false
                toggleScrolling()
            }

            // AI智能提词按钮
            aiStartButton.setOnClickListener {
                isAIMode = true
                toggleScrolling()
            }
        }
    }

    private fun toggleScrolling() {
        isScrolling = !isScrolling
        if (isScrolling) {
            if (isAIMode) {
                startAIScrolling()
                binding.aiStartButton.text = "停止"
                binding.normalStartButton.isEnabled = false
                binding.recognizedText.visibility = View.VISIBLE
            } else {
                startNormalScrolling()
                binding.normalStartButton.text = "停止"
                binding.aiStartButton.isEnabled = false
            }
        } else {
            stopScrolling()
            if (isAIMode) {
                binding.aiStartButton.text = "AI智能提词"
                binding.normalStartButton.isEnabled = true
                binding.recognizedText.visibility = View.GONE
            } else {
                binding.normalStartButton.text = "匀速提词"
                binding.aiStartButton.isEnabled = true
            }
        }
    }
    
    private fun startNormalScrolling() {
        binding.scrollView.post {
            object : Runnable {
                override fun run() {
                    if (isScrolling && !isAIMode) {
                        binding.scrollView.smoothScrollBy(0, (scrollSpeed * 2).toInt())
                        binding.scrollView.postDelayed(this, 16) // 约60fps
                    }
                }
            }.run()
        }
    }

    private fun startAIScrolling() {
        startSpeechRecognition()
    }
    
    private fun stopScrolling() {
        isScrolling = false
        if (isAIMode) {
            stopSpeechRecognition()
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (isScrolling && isAIMode) {
                    startSpeechRecognition()
                }
            }
            override fun onError(error: Int) {
                if (isScrolling && isAIMode) {
                    startSpeechRecognition()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.get(0)?.let { spokenText ->
                    binding.recognizedText.text = "识别到的文本: $spokenText"
                    
                    // 计算说话速度并调整滚动
                    val wordsPerMinute = calculateWordsPerMinute(spokenText)
                    adjustScrollSpeed(wordsPerMinute)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopSpeechRecognition() {
        speechRecognizer.stopListening()
    }

    private fun calculateWordsPerMinute(spokenText: String): Float {
        // 简单计算每分钟说话的字数
        return (spokenText.length * 60 / 5).toFloat() // 假设每5秒识别一次
    }

    private fun adjustScrollSpeed(wordsPerMinute: Float) {
        // 根据说话速度调整滚动速度
        val adjustedSpeed = wordsPerMinute / 200 // 假设200字/分钟是标准速度
        if (isScrolling && isAIMode) {
            binding.scrollView.post {
                binding.scrollView.smoothScrollBy(0, (adjustedSpeed * 50).toInt())
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "需要麦克风权限才能使用AI智能提词功能", Toast.LENGTH_LONG).show()
                binding.aiStartButton.isEnabled = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
} 