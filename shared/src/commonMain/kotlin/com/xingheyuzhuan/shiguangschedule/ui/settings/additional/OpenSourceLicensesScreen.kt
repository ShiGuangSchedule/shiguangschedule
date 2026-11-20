package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.text_loading_failed
import shiguangschedule.shared.generated.resources.title_open_source_licenses

// 定义加载状态密封接口
private sealed interface ResourceState<out T> {
    data object Loading : ResourceState<Nothing>
    data class Success<T>(val data: T) : ResourceState<T>
    data class Error(val message: String) : ResourceState<Nothing>
}

@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {

    val librariesState by produceState<ResourceState<Libs?>>(initialValue = ResourceState.Loading) {
        value = try {
            val jsonString = Res.readBytes("files/aboutlibraries.json").decodeToString()
            val libs = Libs.Builder().withJson(jsonString).build()
            ResourceState.Success(libs)
        } catch (e: Exception) {
            ResourceState.Error(e.message ?: e.toString())
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(Res.string.title_open_source_licenses))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = librariesState) {
                is ResourceState.Loading -> {
                    CircularProgressIndicator()
                }
                is ResourceState.Success -> {
                    LibrariesContainer(
                        libraries = state.data,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ResourceState.Error -> {
                    Text(
                        text = stringResource(Res.string.text_loading_failed, state.message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}