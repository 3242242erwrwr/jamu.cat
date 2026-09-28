package com.example.jamuchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JamuLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val ringGradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF3B82F6),
            Color(0xFFA855F7),
            Color(0xFF00E5FF)
        )
    )

    val bubbleGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF2563EB),
            Color(0xFF4F46E5)
        )
    )

    val factor = size.value / 100f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border((3 * factor).dp, ringGradient, CircleShape)
            .background(Color(0xFF0A0F2D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding((4 * factor).dp)
        ) {
            // Speech Bubble Icon
            Box(
                modifier = Modifier
                    .size((46 * factor).dp)
                    .clip(CircleShape)
                    .background(bubbleGradient),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "J",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = (22 * factor).sp
                    )
                    Text(
                        text = "...",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = (15 * factor).sp,
                        letterSpacing = (-1).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height((3 * factor).dp))

            // JAMU.chat Brand Text
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JAMU",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = (11 * factor).sp
                )
                Text(
                    text = ".chat",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    fontSize = (11 * factor).sp
                )
            }

            Spacer(modifier = Modifier.height((2 * factor).dp))

            // Neon Accent Bar
            Box(
                modifier = Modifier
                    .width((22 * factor).dp)
                    .height((2 * factor).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFFA855F7))
                        )
                    )
            )
        }
    }
}
