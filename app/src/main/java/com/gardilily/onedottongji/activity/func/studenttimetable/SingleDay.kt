// SPDX-License-Identifier: MulanPSL-2.0
package com.gardilily.onedottongji.activity.func.studenttimetable

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Calendar

private data class Course(
    val timeStart: Int,
    val dayOfWeek: Int,
    val weeks: List<Int>,
    val courseName: String,
    val timeEnd: String,
    val room: String,
    val classCode: String,
    val teacherName: String
)

private data class TodayInfo(val week: Int, val dayOfWeek: Int)

class SingleDay : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTermWeek = intent.getIntExtra("TermWeek", 1)
        setContent {
            OneTJTheme {
                SingleDayScreen(
                    initialTermWeek = initialTermWeek,
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}

@Composable
fun SingleDayScreen(initialTermWeek: Int, onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current!!

    var isLoading by remember { mutableStateOf(true) }
    var allCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    val today = remember { getTodayInfo(initialTermWeek) }

    var displayedWeek by rememberSaveable { mutableIntStateOf(today.week) }
    var displayedDayOfWeek by rememberSaveable { mutableIntStateOf(today.dayOfWeek) }

    LaunchedEffect(Unit) {
        isLoading = true
        val timetableJson = withContext(Dispatchers.IO) {
            TongjiApi.instance.getOneTongjiStudentTimetable(activity)
        }
        allCourses = parseTimetableJson(timetableJson)
        isLoading = false
    }


    val coursesForDisplayedDay = remember(displayedWeek, displayedDayOfWeek, allCourses) {
        allCourses.filter { course ->
            course.weeks.contains(displayedWeek) && course.dayOfWeek == displayedDayOfWeek
        }
    }


    OneTJScreenBase(
        title = stringResource(id = R.string.single_day_curriculums),
        onNavigateUp = onNavigateUp,
        isLoading = isLoading
    ) { padding ->
        val configuration = LocalConfiguration.current
        val dayOfWeekCh = remember { arrayOf("日", "一", "二", "三", "四", "五", "六") }

        val onPrevDay: () -> Unit = {
            if (displayedWeek == 1 && displayedDayOfWeek == 1) {
                Toast.makeText(context, "不能再往前啦", Toast.LENGTH_SHORT).show()
            } else {
                if (displayedDayOfWeek == 1) displayedWeek--
                displayedDayOfWeek = (displayedDayOfWeek + 6) % 7
            }
        }
        val onToday: () -> Unit = {
            displayedWeek = today.week
            displayedDayOfWeek = today.dayOfWeek
        }
        val onNextDay: () -> Unit = {
            if (displayedWeek == 21 && displayedDayOfWeek == 0) {
                Toast.makeText(context, "不能再往后啦", Toast.LENGTH_SHORT).show()
            } else {
                if (displayedDayOfWeek == 0) displayedWeek++
                displayedDayOfWeek = (displayedDayOfWeek + 1) % 7
            }
        }

        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LandscapeLayout(
                week = displayedWeek, dayOfWeek = displayedDayOfWeek, dayOfWeekCh = dayOfWeekCh,
                courses = coursesForDisplayedDay, onPrevDay = onPrevDay, onToday = onToday, onNextDay = onNextDay,
                modifier = Modifier.padding(padding)
            )
        } else {
            PortraitLayout(
                week = displayedWeek, dayOfWeek = displayedDayOfWeek, dayOfWeekCh = dayOfWeekCh,
                courses = coursesForDisplayedDay, onPrevDay = onPrevDay, onToday = onToday, onNextDay = onNextDay,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    week: Int, dayOfWeek: Int, dayOfWeekCh: Array<String>, courses: List<Course>,
    onPrevDay: () -> Unit, onToday: () -> Unit, onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        TimetableHeader(week = week, dayOfWeek = dayOfWeek, dayOfWeekCh = dayOfWeekCh)
        CourseList(courses = courses, isPortrait = true, modifier = Modifier.weight(1f))
        TimetableNavigation(
            onPrev = onPrevDay, onToday = onToday, onNext = onNextDay,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun LandscapeLayout(
    week: Int, dayOfWeek: Int, dayOfWeekCh: Array<String>, courses: List<Course>,
    onPrevDay: () -> Unit, onToday: () -> Unit, onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            Modifier.weight(0.4f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimetableHeader(week = week, dayOfWeek = dayOfWeek, dayOfWeekCh = dayOfWeekCh)
            Spacer(modifier = Modifier.height(24.dp))
            TimetableNavigation(onPrev = onPrevDay, onToday = onToday, onNext = onNextDay, prevText = "前", nextText = "后")
        }
        Spacer(modifier = Modifier.width(16.dp))
        CourseList(courses = courses, isPortrait = false, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun TimetableHeader(week: Int, dayOfWeek: Int, dayOfWeekCh: Array<String>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(text = "第${week}周", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(text = "星期${dayOfWeekCh[dayOfWeek]}", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TimetableNavigation(
    onPrev: () -> Unit, onToday: () -> Unit, onNext: () -> Unit,
    modifier: Modifier = Modifier, prevText: String = "前一天", todayText: String = "今天", nextText: String = "后一天"
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f).height(48.dp)) { Text(prevText) }
        Button(onClick = onToday, modifier = Modifier.weight(1f).height(48.dp)) { Text(todayText) }
        OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f).height(48.dp)) { Text(nextText) }
    }
}

@Composable
private fun CourseList(courses: List<Course>, isPortrait: Boolean, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (courses.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("今天没有课哦", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            items(courses) { course ->
                CourseCard(course = course, isPortrait = isPortrait)
            }
        }
    }
}


@Composable
private fun CourseCard(course: Course, isPortrait: Boolean, modifier: Modifier = Modifier) {

    AndroidView(
        // The modifier is now applied to the AndroidView composable, which will
        // correctly size and position your InfoCard in the LazyColumn.
        modifier = modifier,
        factory = { context ->
            // This 'factory' block is where you create your traditional View.
            // It runs only once to create the initial view.
            // Use the 'context' provided by the factory lambda.
            val builder = InfoCard.Builder(context)
                .setSpMultiply(context.resources.displayMetrics.scaledDensity)
                .setHasEndMark(true)
                .setHasIcon(true)
                .setIcon(getCourseIconPath(course.timeStart))
                .setEndMark(
                    "${course.timeStart}-${course.timeEnd}"
                )
                .setTitle(course.courseName)
                .setInfoTextSizeSp(16f)
                .setIconSize(144)
                .setTitleTextSizeSp(20f)
                .setEndMarkTextSizeSp(24f)
                .setEndMarkMarginEndSp(16f)
                .setOuterMarginStartSp(4f)
                .setOuterMarginEndSp(4f)
                .addInfo("地点", course.room)

            if (isPortrait) {
                builder.addInfo("课号", course.classCode)
                builder.addInfo("教师", course.teacherName)
            }

            builder.build()
        },
        update = { infoCard ->
            // UPDATE: This runs during the initial composition AND
            // every time the state read here changes.
            // Update the view with the latest `course` data.
            infoCard.clearInfo() // Clear previous info entries before adding new ones
            infoCard.setIcon(getCourseIconPath(course.timeStart))
            infoCard.setEndMark("${course.timeStart}-${course.timeEnd}")
            infoCard.setTitle(course.courseName)
            infoCard.addInfo(InfoCard.Info("地点", course.room))

            if (isPortrait) {
                infoCard.addInfo("课号", course.classCode)
                infoCard.addInfo("教师", course.teacherName)
            }
        }
    )

}

// --- Helper Functions (transplanted and cleaned) ---


private fun parseTimetableJson(json: JSONArray?): List<Course> {
    if (json == null) return emptyList()
    val courses = mutableListOf<Course>()
    for (i in 0 until json.length()) {
        try {
            val timeTableList = json.getJSONObject(i).getJSONArray("timeTableList")
            for (j in 0 until timeTableList.length()) {
                val courseObj = timeTableList.getJSONObject(j)
                val weeks = courseObj.getJSONArray("weeks")
                courses.add(
                    Course(
                        timeStart = courseObj.getInt("timeStart"),
                        dayOfWeek = courseObj.getInt("dayOfWeek") % 7,
                        weeks = List(weeks.length()) { k -> weeks.getInt(k) },
                        courseName = courseObj.getString("courseName"),
                        timeEnd = courseObj.getString("timeEnd"),
                        room = courseObj.getString("roomIdI18n").ifEmpty { courseObj.getString("roomLable") },
                        classCode = courseObj.getString("classCode"),
                        teacherName = courseObj.getString("teacherName")
                    )
                )
            }
        } catch (_: Exception) { /* Ignore parsing errors */ }
    }
    return courses.sortedBy { it.timeStart }
}

private fun getTodayInfo(initialTermWeek: Int): TodayInfo {
    val calendar = Calendar.getInstance()
    // Calendar.DAY_OF_WEEK: Sunday is 1, Monday is 2... Saturday is 7
    // We need Sunday = 0, Monday = 1... Saturday = 6
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 1).let { if (it < 0) 6 else it }
    return TodayInfo(week = initialTermWeek, dayOfWeek = dayOfWeek)
}

private fun getCourseIconPath(timeBegin: Int): String {
    return when {
        timeBegin <= 2 -> "fluentemoji/bread_color.svg"
        timeBegin <= 4 -> "fluentemoji/curry_rice_color.svg"
        timeBegin <= 6 -> "fluentemoji/tropical_drink_color.svg"
        timeBegin <= 9 -> "fluentemoji/hamburger_color.svg"
        else -> "fluentemoji/moon_cake_color.svg"
    }
}