package com.example.teleprompter

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teleprompter.databinding.ActivityEditTextBinding

class EditTextActivity : Activity() {
    private lateinit var binding: ActivityEditTextBinding
    private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    private var scrollSpeed: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.apply {
            // 速度控制按钮
            speedUpButton.setOnClickListener {
                scrollSpeed += 0.2f
                updateSpeedText()
            }
            
            speedDownButton.setOnClickListener {
                if (scrollSpeed > 0.2f) {
                    scrollSpeed -= 0.2f
                    updateSpeedText()
                }
            }

            // 粘贴按钮
            pasteButton.setOnClickListener {
                pasteText()
            }

            // 清除按钮
            clearButton.setOnClickListener {
                promptEditText.setText("")
                Toast.makeText(this@EditTextActivity, "文本已清除", Toast.LENGTH_SHORT).show()
            }

            // 开始提词按钮
            startPrompterButton.setOnClickListener {
                val text = promptEditText.text.toString()
                if (text.isBlank()) {
                    Toast.makeText(this@EditTextActivity, "请先输入文本", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val intent = Intent(this@EditTextActivity, MainActivity::class.java).apply {
                    putExtra(EXTRA_PROMPT_TEXT, text)
                    putExtra(EXTRA_SCROLL_SPEED, scrollSpeed)
                }
                startActivity(intent)
            }
        }
    }

    private fun updateSpeedText() {
        binding.speedText.text = "速度: ${String.format("%.1f", scrollSpeed)}x"
    }

    private fun pasteText() {
        if (clipboardManager.hasPrimaryClip()) {
            val text = clipboardManager.primaryClip?.getItemAt(0)?.text
            if (text != null) {
                binding.promptEditText.setText(text)
                Toast.makeText(this, "文本已粘贴", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_PROMPT_TEXT = "extra_prompt_text"
        const val EXTRA_SCROLL_SPEED = "extra_scroll_speed"
    }
} 