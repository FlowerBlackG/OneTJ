// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTJScreenBase(
    modifier: Modifier = Modifier,
    title: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    isLoading: Boolean = false,
    content: @Composable (padding: PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            // 仅当 title 不为 null 时，才创建 TopAppBar
            if (title != null) {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        // 仅当 onNavigateUp 回调不为 null 时，才显示返回按钮
                        if (onNavigateUp != null) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate back" // or stringResource
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            // 将 Scaffold 提供的 padding 应用于 Box，以确保内容不会被顶栏遮挡
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 渲染传入的具体屏幕内容
            content(innerPadding) // 也可选择不传入 padding，因为 Box 已经应用了

            // 如果正在加载，就在屏幕中央显示一个加载圈
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
