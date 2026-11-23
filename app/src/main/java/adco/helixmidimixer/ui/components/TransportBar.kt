package adco.helixmidimixer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import adco.helixmidimixer.ui.theme.HelixBadgeBg
import adco.helixmidimixer.ui.theme.HelixCardBorder
import adco.helixmidimixer.ui.theme.HelixGreenPrimary
import adco.helixmidimixer.ui.theme.HelixMidiMixerTheme
import adco.helixmidimixer.ui.theme.HelixSurface
import adco.helixmidimixer.ui.theme.HelixTextPrimary
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height

@Composable
fun TransportBar(
    isPlaying: Boolean,
    playStatus: String = "On Cue",
    currentSongTitle: String = "",
    currentSongKey: String = "",
    currentSongCapo: String = "",
    currentSongTempo: String = "",
    prevSongTitle: String = "",
    nextSongTitle: String = "",
    onPrev: () -> Unit,
    onReturnToZero: () -> Unit,
    onPlayToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.smallestScreenWidthDp < 600

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(HelixSurface)
            .border(width = 1.dp, color = HelixCardBorder),
        color = HelixSurface,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .padding(horizontal = if (isCompact) 4.dp else 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                if (isCompact) 2.dp else 4.dp,
                Alignment.CenterHorizontally
            )
        ) {
            // 1. Prev Button / Prev Song Title
            if (prevSongTitle.isNotBlank()) {
                TransportButtonWithTitle(
                    title = prevSongTitle,
                    icon = Icons.Rounded.SkipPrevious,
                    isPrev = true,
                    compact = isCompact,
                    onClick = onPrev,
                    modifier = Modifier
                        .weight(1.25f, fill = false)
                )
            } else {
                TransportButton(
                    label = "PREV",
                    icon = Icons.Rounded.SkipPrevious,
                    compact = isCompact,
                    onClick = onPrev
                )
            }

             // 2. Return to Zero Button
            TransportButton(
                label = "RT0",
                icon = Icons.Rounded.Replay,
                compact = isCompact,
                onClick = onReturnToZero
            )


            // 3. Current Song Info Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HelixBadgeBg)
                    .border(1.dp, HelixCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (currentSongTitle.isNotBlank()) currentSongTitle else "Helix Companion",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HelixTextPrimary
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (currentSongKey.isNotBlank()) {
                        Text(
                            text = currentSongKey,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HelixTextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    if (currentSongCapo.isNotBlank()) {
                        Text(
                            text = currentSongCapo,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HelixTextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    if (currentSongTempo.isNotBlank()) {
                        Text(
                            text = currentSongTempo,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HelixTextPrimary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }



            // 4. Play / Pause Button with Status Text
            TransportPlayButton(
                isPlaying = isPlaying,
                statusText = playStatus,
                compact = isCompact,
                onClick = onPlayToggle
            )

            // 5. Next Button / Next Song Title
            if (nextSongTitle.isNotBlank()) {
                TransportButtonWithTitle(
                    title = nextSongTitle,
                    icon = Icons.Rounded.SkipNext,
                    isPrev = false,
                    compact = isCompact,
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1.25f, fill = false)
                )
            } else {
                TransportButton(
                    label = "NEXT",
                    icon = Icons.Rounded.SkipNext,
                    compact = isCompact,
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean = false,
    activeColor: Color = HelixGreenPrimary,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = if (isActive) activeColor else HelixBadgeBg
    val contentColor = if (isActive) Color.White else HelixTextPrimary
    val borderColor = if (isActive) activeColor else HelixCardBorder

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(if (compact) 16.dp else 20.dp)
        )
        Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = if (compact) 10.sp else 12.sp
            )
        )
    }
}

@Composable
private fun TransportButtonWithTitle(
    title: String,
    icon: ImageVector,
    isPrev: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HelixBadgeBg)
            .border(1.dp, HelixCardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isPrev) {
            Icon(
                imageVector = icon,
                contentDescription = "Previous",
                tint = HelixTextPrimary,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = HelixTextPrimary,
                    fontSize = if (compact) 10.sp else 12.sp
                ),
                modifier = Modifier.weight(1f, fill = false)
            )
        } else {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = HelixTextPrimary,
                    fontSize = if (compact) 10.sp else 12.sp
                ),
                modifier = Modifier.weight(1f, fill = false),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = icon,
                contentDescription = "Next",
                tint = HelixTextPrimary,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp)
            )
        }
    }
}

@Composable
private fun TransportPlayButton(
    isPlaying: Boolean,
    statusText: String,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val activeColor = HelixGreenPrimary
    val backgroundColor = if (isPlaying) activeColor else HelixBadgeBg
    val contentColor = if (isPlaying) Color.White else HelixTextPrimary
    val borderColor = if (isPlaying) activeColor else HelixCardBorder

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = contentColor,
            modifier = Modifier.size(if (compact) 16.dp else 22.dp)
        )
        if (statusText.isNotBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusText,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = if (compact) 9.sp else 11.sp
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 900)
@Composable
fun TransportBarPreview() {
    HelixMidiMixerTheme {
        TransportBar(
            isPlaying = false,
            playStatus = "On Cue",
            currentSongTitle = "Comfortably Numb",
            currentSongKey = " (Bm)",
            currentSongCapo = " [Capo 0]",
            currentSongTempo = " 60 bpm",
            prevSongTitle = "Highway to Hell",
            nextSongTitle = "Wicked Game",
            onPrev = {},
            onReturnToZero = {},
            onPlayToggle = {},
            onNext = {}
        )
    }
}
