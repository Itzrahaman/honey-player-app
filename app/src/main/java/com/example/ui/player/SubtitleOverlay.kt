package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.text.Cue
import com.example.data.local.PlayerSettings
import com.example.ui.theme.HoneyGold

@Composable
fun SubtitleOverlay(
    cues: List<Cue>,
    settings: PlayerSettings,
    modifier: Modifier = Modifier
) {
    if (!settings.subtitlesEnabled || cues.isEmpty()) return

    val alignment = when (settings.subtitlePosition) {
        "top" -> Alignment.TopCenter
        "center" -> Alignment.Center
        "raised" -> Alignment.BottomCenter
        else -> Alignment.BottomCenter
    }

    val bottomPadding = when (settings.subtitlePosition) {
        "raised" -> 80.dp
        "top" -> 24.dp
        else -> 28.dp
    }

    val textColor = when (settings.subtitleColor) {
        "honey_gold" -> HoneyGold
        "yellow" -> Color(0xFFFFEB3B)
        "cyan" -> Color(0xFF00E5FF)
        "green" -> Color(0xFF69F0AE)
        else -> Color.White
    }

    val backgroundColor = when (settings.subtitleBackground) {
        "transparent" -> Color.Transparent
        "solid_black" -> Color.Black
        else -> Color(0xCC000000)
    }

    val shadow = if (settings.subtitleBackground == "transparent") {
        Shadow(
            color = Color.Black,
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    } else null

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                bottom = bottomPadding,
                top = if (settings.subtitlePosition == "top") 36.dp else 0.dp,
                start = 24.dp,
                end = 24.dp
            ),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            cues.forEach { cue ->
                val text = cue.text?.toString() ?: ""
                if (text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .background(backgroundColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = text,
                            color = textColor,
                            fontSize = settings.subtitleSizeSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(shadow = shadow),
                            lineHeight = (settings.subtitleSizeSp * 1.3f).sp
                        )
                    }
                }
            }
        }
    }
}
