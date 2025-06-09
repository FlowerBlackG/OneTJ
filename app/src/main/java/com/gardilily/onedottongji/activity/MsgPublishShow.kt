// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class MsgPublishShow : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Extract data from intent. In a real-world app, consider using a safer
        // way to pass data, like through a navigation library.
        val basicDataObjString = intent.getStringExtra("basicDataObj")
        if (basicDataObjString == null) {
            Toast.makeText(this, "Error: Data missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val basicData = JSONObject(basicDataObjString)

        setContent {
            OneTJTheme {
                MsgPublishShowScreen(
                    basicData = basicData,
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}

@Composable
private fun MsgPublishShowScreen(
    basicData: JSONObject,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current!!
    val coroutineScope = rememberCoroutineScope()

    // --- State Management ---
    var isLoading by remember { mutableStateOf(true) }
    var contentData by remember { mutableStateOf<JSONObject?>(null) }


    // --- Data Fetching ---
    // LaunchedEffect runs the block when the composable enters the screen.
    // It's the modern replacement for launching a thread in onCreate.
    LaunchedEffect(key1 = Unit) {
        isLoading = true
        val fetchedData = withContext(Dispatchers.IO) {
            TongjiApi.instance.getOneTongjiMessageDetail(
                activity,
                basicData.getInt("id")
            )
        }
        contentData = fetchedData
        isLoading = false
    }

    // --- UI Description ---
    OneTJScreenBase(
        title = basicData.getString("title"),
        onNavigateUp = onNavigateUp,
        isLoading = isLoading
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            // Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = basicData.optString("createUser", "Unknown"), fontSize = 16.sp)
                Text(text = basicData.optString("publishTime", ""), fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area - Reacts to state changes
            val htmlContent = contentData?.optString("content")
            if (!htmlContent.isNullOrEmpty()) {
                ContentWebView(htmlContent = htmlContent)
            }

            // Attachments Area
            val attachments = contentData?.optJSONArray("commonAttachmentList")
            if (attachments != null && attachments.length() > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Attachments", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                for (i in 0 until attachments.length()) {
                    AttachmentCard(
                        attachmentJson = attachments.getJSONObject(i),
                        onCardClicked = { attachment ->
                            val filename = "(${basicData.getInt("id")}) ${attachment.getString("fileName")}"

                            Toast.makeText(context, "暂不支持下载。请前往网页端操作", Toast.LENGTH_SHORT).show()

                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}


/**
 * A composable that wraps the legacy TextView and its HtmlCompat logic.
 * This is the perfect way to reuse complex View logic without a full rewrite.
 */
@Composable
private fun ContentTextView(htmlContent: String) {
    AndroidView(
        factory = { context -> TextView(context) },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(
                htmlContent,
                HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS or HtmlCompat.FROM_HTML_MODE_COMPACT,
                { source ->
                    val base64str = source.substring(source.indexOf(',') + 1)
                    val data = Base64.decode(base64str, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    BitmapDrawable(textView.resources, bitmap).apply {
                        setBounds(0, 0, textView.width, intrinsicHeight)
                    }
                },
                null
            )
        }
    )
}

/**
 * A composable that safely wraps a WebView for displaying HTML content.
 */
@Composable
private fun ContentWebView(htmlContent: String) {
    val mobileFriendlyHtml = remember(htmlContent) {
        val head = "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +
                "<style>img{max-width: 100%; width:auto; height:auto;}*{margin:0px;padding:0px;}</style>" +
                "</head>"
        "<html>$head<body>$htmlContent</body></html>"
    }

    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
        }
    }, update = { webView ->
        webView.loadDataWithBaseURL(null, mobileFriendlyHtml, "text/html", "utf-8", null)
    })
}



@Composable
private fun AttachmentCard(
    attachmentJson: JSONObject,
    onCardClicked: (JSONObject) -> Unit
) {
    Card(
        onClick = { onCardClicked(attachmentJson) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // In a real app, you might show a file type icon here.
            Icon(Icons.Filled.Star, contentDescription = "Attachment")
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = attachmentJson.optString("fileName", "Unnamed File"),
                modifier = Modifier.weight(1f))
        }
    }
}
