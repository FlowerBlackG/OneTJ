// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity.func

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.OneTJScreenBase
import com.gardilily.onedottongji.activity.OneTJTheme
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

private data class CetScoreInfo(
    val isCet4: Boolean,
    val termName: String,
    val score: String,
    val ticketNumber: String,
    val studentName: String,
    val studentId: String,
    val subjectName: String,
    val oralScore: String
)

class CetScore : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneTJTheme {
                CetScoreScreen(
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}

@Composable
private fun CetScoreScreen(onNavigateUp: () -> Unit) {
    val activity = LocalActivity.current!!

    var isLoading by remember { mutableStateOf(true) }
    var scores by remember { mutableStateOf<List<CetScoreInfo>>(emptyList()) }

    LaunchedEffect(key1 = Unit) {
        isLoading = true
        val data = withContext(Dispatchers.IO) {
            TongjiApi.instance.getOneTongjiCetScore(activity)
        }
        Log.i("json array", data.toString())
        scores = parseJsonToCetScoreInfo(data)
        isLoading = false
    }

    OneTJScreenBase(
        title = stringResource(id = R.string.cet),
        onNavigateUp = onNavigateUp,
        isLoading = isLoading
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(scores) { scoreInfo ->
                CetScoreCard(
                    scoreInfo = scoreInfo,
                    // The outer margins from the original builder are now applied here
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}


@Composable
private fun CetScoreCard(
    scoreInfo: CetScoreInfo,
    modifier: Modifier = Modifier
) {
    // AndroidView is the bridge between Compose and the traditional View system.
    // It takes a 'factory' lambda to create the view.
    AndroidView(
        // The modifier is now applied to the AndroidView composable, which will
        // correctly size and position your InfoCard in the LazyColumn.
        modifier = modifier,
        factory = { context ->
            // This 'factory' block is where you create your traditional View.
            // It runs only once to create the initial view.
            // Use the 'context' provided by the factory lambda.
            InfoCard.Builder(context)
                .setHasIcon(true)
                .setHasEndMark(true)
                .setEndMark(scoreInfo.score)
                .setTitle(scoreInfo.termName)
                .setSpMultiply(context.resources.displayMetrics.scaledDensity)
                // These outer margins are now handled by the 'modifier' on AndroidView,
                // so they are not strictly necessary here, but we keep them for consistency
                // with the original builder's intent.
                .setOuterMarginTopSp(0f) // Margin is applied by LazyColumn's spacedBy
                .setOuterMarginBottomSp(0f)
                .setOuterMarginStartSp(0f) // Margin is applied by the modifier's padding
                .setOuterMarginEndSp(0f)
                .addInfo(InfoCard.Info("准考证", scoreInfo.ticketNumber))
                .addInfo(InfoCard.Info("学生", "${scoreInfo.studentId} ${scoreInfo.studentName}"))
                .addInfo(InfoCard.Info("科目", scoreInfo.subjectName))
                .addInfo(InfoCard.Info("口试", scoreInfo.oralScore))
                .setIcon(
                    if (scoreInfo.isCet4) "fluentemoji/thinking_face_color.svg"
                    else "fluentemoji/exploding_head_color.svg"
                )
                .setIconSize(96)
                .setEndMarkTextSizeSp(36f)
                .build() // The factory must return the created View object.
        }
    )
}


// The InfoRow composable is no longer needed as InfoCard handles this internally.

private fun parseJsonToCetScoreInfo(jsonData: JSONArray?): List<CetScoreInfo> {
    if (jsonData == null) return emptyList()
    return List(jsonData.length()) { i ->
        val it = jsonData.getJSONObject(i)
        CetScoreInfo(
            isCet4 = it.optString("cetType") == "1",
            termName = it.optString("calendarYearTermCn"),
            score = it.optString("score", "0"),
            ticketNumber = it.optString("cardNo"),
            studentName = it.optString("studentName"),
            studentId = it.optString("studentId"),
            subjectName = it.optString("writtenSubjectName"),
            oralScore = it.optString("oralScore", "无").let { if (it == "null") "无" else it }
        )
    }
}