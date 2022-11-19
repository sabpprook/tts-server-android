package com.github.jing332.tts_server_android.ui.systts.replace.edit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.constant.KeyConst
import com.github.jing332.tts_server_android.data.ReplaceRuleItemData
import com.github.jing332.tts_server_android.databinding.ActivityReplaceRuleEditBinding
import com.github.jing332.tts_server_android.ui.custom.BackActivity

@Suppress("DEPRECATION")
class ReplaceRuleEditActivity : BackActivity() {
    val binding by lazy { ActivityReplaceRuleEditBinding.inflate(layoutInflater) }
    val viewModel: ReplaceRuleEditActivityViewModel by viewModels()

    var position = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        position = intent.getIntExtra(KeyConst.KEY_POSITION, -1)

        binding.btnPinyinList.setOnClickListener {
            val pinyinList = "ā á ǎ à ō ó ǒ ò ê ē é ě è ī í ǐ ì ū ú ǔ ù ǖ ǘ ǚ ǜ".split(" ")
            AlertDialog.Builder(this).setItems(
                pinyinList.toTypedArray()
            ) { _, which ->
                val pinyin = pinyinList[which]
                if (binding.etPattern.hasFocus()) {
                    binding.etPattern.apply {
                        text.insert(selectionStart, pinyin)
                    }
                } else if (binding.etReplacement.hasFocus()) {
                    binding.etReplacement.apply {
                        text.insert(selectionStart, pinyin)
                    }
                } else if (binding.etName.hasFocus()) {
                    binding.etName.apply {
                        text.insert(selectionStart, pinyin)
                    }
                }
            }.show()
        }

        viewModel.liveData.observe(this) {
            binding.apply {
                etName.setText(it.name)
                etPattern.setText(it.pattern)
                etReplacement.setText(it.replacement)
            }
        }

        var data =
            intent.getSerializableExtra(KeyConst.KEY_DATA)?.let { it as ReplaceRuleItemData }
        if (data == null) data = ReplaceRuleItemData()
        viewModel.load(data)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.replace_rule_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                viewModel.liveData.value?.apply {
                    pattern = binding.etPattern.text.toString()
                    if (pattern.isEmpty()) {
                        binding.etPattern.error = "不可为空！"
                        binding.etPattern.requestFocus()

                        return true
                    }

                    name = binding.etName.text.toString().ifEmpty { pattern }
                    replacement = binding.etReplacement.text.toString()


                }
                val data = viewModel.liveData.value

                val intent = Intent()
                intent.putExtra(
                    KeyConst.KEY_DATA,
                    data
                )
                intent.putExtra(KeyConst.KEY_POSITION, position)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

}