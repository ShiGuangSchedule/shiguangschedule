package com.xingheyuzhuan.shiguangschedule.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.GlobalToastState

// 定义一个 Toast 消息的数据结构
data class ToastItem(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    val message: String,
    val durationMillis: Long = 3000L
)

@Composable
fun DesktopToastHost() {
    val toastList = remember { mutableStateListOf<ToastItem>() }

    LaunchedEffect(Unit) {
        GlobalToastState.messages.collect { message ->
            val newItem = ToastItem(message = message)
            if (toastList.size >= 5) {
                toastList.removeAt(0)
            }
            toastList.add(newItem)
        }
    }

    if (toastList.isNotEmpty()) {
        Column(
            modifier = Modifier
                .padding(end = 24.dp, bottom = 24.dp)
                .widthIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            toastList.forEach { item ->
                key(item.id) {
                    AnimatedToastItem(
                        item = item,
                        onDismiss = {
                            toastList.remove(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedToastItem(
    item: ToastItem,
    onDismiss: () -> Unit
) {
    var progress by remember(item.id) { mutableStateOf(1f) }

    LaunchedEffect(item.id) {
        val startTime = System.currentTimeMillis()
        val duration = item.durationMillis.toFloat()

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val currentProgress = (1f - (elapsed / duration)).coerceIn(0f, 1f)
            progress = currentProgress

            if (currentProgress <= 0f) {
                onDismiss()
                break
            }
            androidx.compose.runtime.withFrameMillis { }
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.widthIn(min = 200.dp, max = 320.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = item.message,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.2f),
                )
            }
        }
    }
}