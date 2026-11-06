package com.abdulbasir.sdplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abdulbasir.sdplayer.data.Store

private val SPEED_CHOICES = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val STEP_CHOICES = listOf(5, 10, 15, 30)
private val SUBTITLE_SIZES = listOf("Small", "Medium", "Large", "Huge")

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store(context) }
    BackHandler(onBack = onBack)

    var speed by remember { mutableStateOf(store.speed) }
    var subtitle by remember { mutableStateOf(store.subtitleSize) }
    var step by remember { mutableStateOf(store.seekStepSec) }
    var resume by remember { mutableStateOf(store.resume) }
    var rotate by remember { mutableStateOf(store.autoRotate) }
    var pip by remember { mutableStateOf(store.autoPip) }
    var gestures by remember { mutableStateOf(store.gestures) }

    Column(
        Modifier
            .fillMaxSize()
            .aurora()
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .glass(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(14.dp))
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Header("Playback")
            SwitchRow("Resume where you stopped", "Continue a video from the last position", resume) {
                resume = it
                store.resume = it
            }
            ChoiceRow("Default speed", "${speedLabel(speed)}x") {
                speed = SPEED_CHOICES[(SPEED_CHOICES.indexOf(speed).let { i -> if (i < 0) 2 else i } + 1) % SPEED_CHOICES.size]
                store.speed = speed
            }
            ChoiceRow("Double tap and buttons skip", "$step seconds") {
                step = STEP_CHOICES[(STEP_CHOICES.indexOf(step).let { i -> if (i < 0) 1 else i } + 1) % STEP_CHOICES.size]
                store.seekStepSec = step
            }

            Header("Screen")
            SwitchRow("Swipe gestures", "Brightness, volume and seek by swiping", gestures) {
                gestures = it
                store.gestures = it
            }
            SwitchRow("Rotate with the video", "Landscape videos open in landscape", rotate) {
                rotate = it
                store.autoRotate = it
            }
            SwitchRow("Picture in picture", "Keep playing in a small window when you leave", pip) {
                pip = it
                store.autoPip = it
            }

            Header("Subtitles")
            ChoiceRow("Subtitle size", SUBTITLE_SIZES[subtitle.coerceIn(0, 3)]) {
                subtitle = (subtitle + 1) % SUBTITLE_SIZES.size
                store.subtitleSize = subtitle
            }

            Header("About")
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("SD Player 1.2.2", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Made by ABDUL BASIR", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("MIT License", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')

@Composable
private fun Header(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glass(RoundedCornerShape(20.dp), fill = 0.36f)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glass(RoundedCornerShape(20.dp), fill = 0.36f)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}
