package com.github.jing332.tts_server_android.compose.nav.systts.edit.ui

import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.nav.systts.edit.BasicInfoEditScreen
import com.github.jing332.tts_server_android.compose.nav.systts.edit.IntSlider
import com.github.jing332.tts_server_android.compose.nav.systts.edit.ui.base.AuditionTextField
import com.github.jing332.tts_server_android.compose.nav.systts.edit.ui.base.TtsTopAppBar
import com.github.jing332.tts_server_android.compose.widgets.ExposedDropTextField
import com.github.jing332.tts_server_android.compose.widgets.LoadingDialog
import com.github.jing332.tts_server_android.data.entities.systts.SystemTts
import com.github.jing332.tts_server_android.model.speech.tts.BaseAudioFormat
import com.github.jing332.tts_server_android.model.speech.tts.PluginTTS
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.launch
import java.util.Locale

class PluginTtsUI : TtsUI() {

    @Composable
    override fun ParamsEditScreen(
        modifier: Modifier,
        systts: SystemTts,
        onSysttsChange: (SystemTts) -> Unit,
    ) {
        val context = LocalContext.current
        val tts = (systts.tts as PluginTTS)
        Column(modifier) {
            val rateStr =
                stringResource(
                    id = R.string.label_speech_rate,
                    if (tts.rate == 0) stringResource(id = R.string.follow) else tts.rate.toString()
                )
            IntSlider(
                label = rateStr,
                value = tts.rate.toFloat(),
                onValueChange = {
                    onSysttsChange(
                        systts.copy(
                            tts = tts.copy(rate = it.toInt())
                        )
                    )
                },
                valueRange = 0f..100f
            )

            val volumeStr =
                stringResource(
                    id = R.string.label_speech_volume,
                    if (tts.volume == 0) stringResource(id = R.string.follow) else tts.volume.toString()
                )
            IntSlider(
                label = volumeStr, value = tts.volume.toFloat(), onValueChange = {
                    onSysttsChange(
                        systts.copy(
                            tts = tts.copy(volume = it.toInt())
                        )
                    )
                }, valueRange = 0f..100f
            )

            val pitchStr = stringResource(
                id = R.string.label_speech_pitch,
                if (tts.pitch == 0) stringResource(id = R.string.follow) else tts.pitch.toString()
            )
            IntSlider(
                label = pitchStr, value = tts.pitch.toFloat(), onValueChange = {
                    onSysttsChange(
                        systts.copy(
                            tts = tts.copy(pitch = it.toInt())
                        )
                    )
                }, valueRange = 0f..100f
            )
        }
    }

    @Preview
    @Composable
    private fun PreviewParamsEditScreen() {
        var systts by remember { mutableStateOf(SystemTts(tts = PluginTTS())) }
        ParamsEditScreen(Modifier, systts = systts, onSysttsChange = { systts = it })
    }

    @Composable
    override fun FullEditScreen(
        modifier: Modifier,
        systts: SystemTts,
        onSysttsChange: (SystemTts) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit,
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TtsTopAppBar(
                    title = { Text(text = stringResource(id = R.string.edit_plugin_tts)) },
                    onBackAction = onCancel,
                    onSaveAction = {
                        onSave()
                    }
                )
            }
        ) { paddingValues ->
            EditContentScreen(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(
                        rememberScrollState()
                    ),
                systts = systts, onSysttsChange = onSysttsChange,
            )
        }
    }

    @Composable
    fun EditContentScreen(
        modifier: Modifier,
        systts: SystemTts,
        onSysttsChange: (SystemTts) -> Unit,
        vm: PluginTtsViewModel = viewModel(),
    ) {
        var displayName by remember { mutableStateOf("") }

        @Suppress("NAME_SHADOWING")
        val systts by rememberUpdatedState(newValue = systts)
        val tts by rememberUpdatedState(newValue = systts.tts as PluginTTS)
        val context = LocalContext.current

        SaveActionHandler {
            if (systts.displayName.isNullOrBlank())
                onSysttsChange(systts.copy(displayName = displayName))

            val sampleRate = try {
                withIO { vm.engine.getSampleRate(tts.locale, tts.voice) ?: 16000 }
            } catch (e: Exception) {
                context.displayErrorDialog(
                    e,
                    context.getString(R.string.plugin_tts_get_sample_rate_failed)
                )
                null
            }

            val isNeedDecode = try {
                withIO { vm.engine.isNeedDecode(tts.locale, tts.voice) }
            } catch (e: Exception) {
                context.displayErrorDialog(
                    e,
                    context.getString(R.string.plugin_tts_get_need_decode_failed)
                )
                null
            }

            if (sampleRate != null && isNeedDecode != null) {
                onSysttsChange(
                    systts.copy(
                        tts = tts.copy(
                            audioFormat = BaseAudioFormat(
                                sampleRate = sampleRate,
                                isNeedDecode = isNeedDecode
                            )
                        )
                    )
                )

                true
            } else
                false
        }

        var showLoadingDialog by remember { mutableStateOf(false) }
        if (showLoadingDialog)
            LoadingDialog(onDismissRequest = { showLoadingDialog = false })

        var showAuditionDialog by remember { mutableStateOf(false) }
        if (showAuditionDialog)
            AuditionDialog(systts = systts) {
                showAuditionDialog = false
            }

        Column(modifier) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                BasicInfoEditScreen(
                    Modifier.fillMaxWidth(),
                    systts = systts,
                    onSysttsChange = onSysttsChange
                )

                ExposedDropTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    label = { Text(stringResource(id = R.string.language)) },
                    key = tts.locale,
                    keys = vm.locales,
                    values = vm.locales.map { Locale.forLanguageTag(it).displayName },
                    onSelectedChange = { key, _ ->
                        onSysttsChange(
                            systts.copy(tts = tts.copy(locale = key as String))
                        )
                    },
                )

                LaunchedEffect(key1 = tts.locale) {
                    runCatching {
                        vm.onLocaleChanged(tts.locale)
                    }
                }

                ExposedDropTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    label = { Text(stringResource(id = R.string.label_voice)) },
                    key = tts.voice,
                    keys = vm.voices.map { it.first },
                    values = vm.voices.map { it.second },
                    onSelectedChange = { key, name ->
                        val lastName = vm.voices.find { it.first == tts.voice }?.second ?: ""
                        onSysttsChange(
                            systts.copy(
                                displayName = if (lastName == systts.displayName) name else systts.displayName,
                                tts = tts.copy(voice = key as String)
                            )
                        )
                        runCatching {
                            vm.onVoiceChanged(tts.locale, key)
                        }.onFailure {
                            context.displayErrorDialog(it)
                        }

                        displayName = name
                    }
                )

                AuditionTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onAudition = {
                        showAuditionDialog = true
                    }
                )

                val scope = rememberCoroutineScope()
                suspend fun load(linearLayout: LinearLayout) {
                    showLoadingDialog = true
                    runCatching {
                        vm.initEngine(systts.tts as PluginTTS)
                        withIO { vm.engine.onLoadData() }

                        vm.engine.onLoadUI(context, linearLayout)

                        vm.onLocaleChanged(tts.locale)
                        vm.initData()
                    }.onFailure {
                        it.printStackTrace()
                        context.displayErrorDialog(it)
                    }


                    showLoadingDialog = false
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    factory = {
                        LinearLayout(it).apply {
                            orientation = LinearLayout.VERTICAL
                            scope.launch { load(this@apply) }
                        }
                    }
                )
            }


            ParamsEditScreen(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                systts = systts,
                onSysttsChange = onSysttsChange
            )
        }
    }
}