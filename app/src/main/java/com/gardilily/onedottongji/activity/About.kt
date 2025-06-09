// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.tools.GarCloudApi
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi


class About : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneTJTheme {
                // 主题的 surface 颜色会自动应用到系统栏
                AboutScreen(
                    onNavigateUp = { finish() } // 点击返回时关闭 Activity
                )
            }
        }
    }
}


@Composable
private fun AboutScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var showDebugDialog by remember { mutableStateOf(false) }

    // 1. 使用我们创建的 OneTJScreenBase 作为屏幕的基础框架
    OneTJScreenBase(
        title = stringResource(id = R.string.about),
        onNavigateUp = onNavigateUp
    ) {
        // 2. Box 用于模拟 RelativeLayout，方便将一个元素固定在底部
        Box(modifier = Modifier.fillMaxSize()) {

            // 3. 主要内容区域，可滚动
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 匹配 XML 中的 ScrollView
                    .verticalScroll(rememberScrollState())
                    // 添加一些内边距，避免内容贴边
                    .padding(horizontal = 16.dp),
                // 匹配 XML 中的 android:gravity="center_horizontal"
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部间隔
                Spacer(modifier = Modifier.height(36.dp))

                // Logo, Compose 的方式更简洁
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        // 匹配 XML 中的 elevation
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(36.dp))
                        // 匹配代码中设置的 cornerRadius，无需手动回收 bitmap
                        .clip(RoundedCornerShape(36.dp))
                )

                Text(
                    text = stringResource(id = R.string.app_name),
                    fontSize = 28.sp,
                    modifier = Modifier.padding(top = 28.dp)
                )

                // 版本信息
                Text(
                    text = makeVersionInfo(context),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Text(
                    text = stringResource(id = R.string.directed_by_the_informatization_office_of_tongji_university),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 18.dp)
                )

                Text(
                    text = stringResource(id = R.string.licensed_under_Mulan_Permissive_Software_License_Version_2),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // 检查更新按钮
                Button(
                    onClick = { GarCloudApi.checkUpdate(activity!!, true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.check_for_update),
                        fontSize = 16.sp
                    )
                }

                // 查看源码按钮
                OutlinedButton(
                    onClick = {
                        val uri = "https://github.com/FlowerBlackG/OneTJ".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.get_source_code),
                        fontSize = 16.sp
                    )
                }

                // 底部留出空间，避免被 bottomText 遮挡
                Spacer(modifier = Modifier.height(64.dp))
            }

            // 4. 底部触发调试信息的文本，位于 Box 内部，但在 Column 外部
            BottomDebugText(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                onTrigger = { showDebugDialog = true }
            )
        }
    }

    // 5. 根据状态决定是否显示调试弹窗
    if (showDebugDialog) {
        DebugInfoDialog(onDismiss = { showDebugDialog = false })
    }
}


@Composable
private fun BottomDebugText(modifier: Modifier = Modifier, onTrigger: () -> Unit) {
    // 移植 prepareBottomTextClickListener 的逻辑
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val clickIntervalThresholdMillis = 800
    val triggerThreshold = 5

    Text(
        text = "Based on Tongji Open Platform APIs\nDeveloped by GTY",
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.clickable {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < clickIntervalThresholdMillis) {
                clickCount++
            } else {
                clickCount = 1
            }
            lastClickTime = currentTime

            if (clickCount >= triggerThreshold) {
                clickCount = 0
                onTrigger()
            }
        }
    )
}

@Composable
private fun DebugInfoDialog(onDismiss: () -> Unit) {
    // 移植 showDebugInfo 的逻辑
    val context = LocalContext.current
    val debugMessage = remember { buildDebugMessage(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调试信息") },
        text = {
            val scrollState = rememberScrollState()
            // SelectionContainer 允许用户自由复制弹窗内的文本
            SelectionContainer {
                Text(
                    text = debugMessage,
                    modifier = Modifier.verticalScroll(scrollState) // Make the text scrollable
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OneTJ Debug", debugMessage)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已复制。", Toast.LENGTH_SHORT).show()
            }) {
                Text("复制")
            }
        }
    )
}



private fun buildDebugMessage(context: Context): String {
    val tongjiApiToken = TongjiApi.instance.getTokenData()
    // 使用 Kotlin 的原始字符串使构建更清晰
    return """
        以下信息仅用于调试使用。请勿将ta们交给任何不可信的人！

        tjapi token:
        ${tongjiApiToken.token}

        tjapi token expire:
        ${tongjiApiToken.expireTimeSec}

        tjapi ref token:
        ${tongjiApiToken.refreshToken}

        tjapi ref token exp:
        ${tongjiApiToken.refreshTokenExpireSec}

        version info:
        ${makeVersionInfo(context).replace("\n", "[\\n]")}
    """.trimIndent()
}


// 将工具函数保留，但现在接收 Context 作为参数
private fun makeVersionInfo(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return "${packageInfo.versionName} (${packageInfo.longVersionCode})\n${context.getString(R.string.app_buildTime)}"
}
