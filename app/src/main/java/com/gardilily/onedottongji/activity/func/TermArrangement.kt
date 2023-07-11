// SPDX-License-Identifier: MulanPSL-2.0

package com.gardilily.onedottongji.activity.func

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isEmpty
import androidx.core.view.setPadding
import com.gardilily.common.view.card.InfoCard
import com.gardilily.onedottongji.R
import com.gardilily.onedottongji.activity.OneTJActivityBase
import com.gardilily.onedottongji.tools.tongjiapi.TongjiApi
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.EndIconMode
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class TermArrangement : OneTJActivityBase(
    hasTitleBar = true,
    backOnTitleBar = true
) {

    private lateinit var rootContainer: RelativeLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var courseCardContainer: LinearLayout
    private lateinit var progressIndicator: TextView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var centerHintTextView: TextView

    private lateinit var termData: List<TongjiApi.CourseArrangement>

    data class FilterInfo(
        val key: String,
        val defaultValue: String,
        val fieldNameInCourseArrangement: String,
    )

    private val filterAdapters = HashMap<String, ArrayAdapter<String>>()
    private val filterViews = HashMap<String, Spinner>()
    private val filterPrevValue = HashMap<String, String>()

    private val filterInfoList = listOf(
        FilterInfo(
            key = "campus",
            defaultValue = FILTER_TEXT_ALL_CAMPUS,
            fieldNameInCourseArrangement = TongjiApi.CourseArrangement::campusI18n.name
        ),
        FilterInfo(
            key = "label",
            defaultValue = FILTER_TEXT_ALL_LABEL,
            fieldNameInCourseArrangement = TongjiApi.CourseArrangement::courseLabelName.name
        ),
        FilterInfo(
            key = "faculty",
            defaultValue = FILTER_TEXT_ALL_FACULTY,
            fieldNameInCourseArrangement = TongjiApi.CourseArrangement::facultyI18n.name
        ),
        FilterInfo(
            key = "assessment",
            defaultValue = FILTER_TEXT_ALL_ASSESSMENT,
            fieldNameInCourseArrangement = TongjiApi.CourseArrangement::assessmentModeI18n.name
        ),
    )

    companion object {
        const val FILTER_TEXT_ALL_CAMPUS = "全部校区"
        const val FILTER_TEXT_ALL_FACULTY = "全部学院"
        const val FILTER_TEXT_ALL_LABEL = "全部分类"
        const val FILTER_TEXT_ALL_ASSESSMENT = "全部考核"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareLayout()
        setContentView(rootContainer)

        title = "全校课表（Beta）"

        setSpinning(true)

        loadData(intent.getStringExtra("calendarId") ?: "0", true)

    }

    private var nextViewId = 0x114514
    private fun genViewId(): Int {
        return nextViewId++
    }

    private fun setProgress(visible: Boolean, indeterminate: Boolean = false, text: String = "", progress: Int = 0) {
        runOnUiThread {
            if (visible) {
                progressBar.visibility = View.VISIBLE
                progressIndicator.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
                progressIndicator.visibility = View.GONE
                return@runOnUiThread
            }

            if (progressBar.isIndeterminate != indeterminate) {
                progressBar.isIndeterminate = indeterminate
            }

            if (!indeterminate) {
                progressBar.progress = progress
            }

            progressIndicator.text = text
        }
    }

    private fun prepareScrollContainer(filterAreaId: Int) {
        val scrollView = ScrollView(this)
        val scrollParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        scrollParams.addRule(RelativeLayout.BELOW, filterAreaId)
        scrollView.layoutParams = scrollParams
        rootContainer.addView(scrollView)

        courseCardContainer = LinearLayout(this)
        val cardContainerParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        courseCardContainer.layoutParams = cardContainerParams
        courseCardContainer.orientation = LinearLayout.VERTICAL
        courseCardContainer.layoutTransition = LayoutTransition()
        scrollView.addView(courseCardContainer)
    }


    private fun Field.forceGet(obj: Any): Any {
        val prevAcc = this.isAccessible
        this.isAccessible = true
        val res = this.get(obj)
        this.isAccessible = prevAcc
        return res
    }

    private val schoolCalendarNameToId = HashMap<String, String>()

    private fun prepareFilterArea(): Int {

        val filterArea = LinearLayout(this)
        val filterAreaParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        filterArea.layoutParams = filterAreaParams
        val filterAreaId = genViewId()
        filterArea.id = filterAreaId
        filterArea.orientation = LinearLayout.VERTICAL

        fun prepareSearchBar() {
            val searchLayout = TextInputLayout(this, null, R.style.Widget_Material3_TextInputLayout_OutlinedBox)
            val searchView = TextInputEditText(searchLayout.context)
            searchEditText = searchView
            searchLayout.addView(searchView)
            val searchLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 144)
            searchLayoutParams.marginStart = 36
            searchLayoutParams.marginEnd = 36
            searchLayoutParams.bottomMargin = 24
            searchLayoutParams.topMargin = 24
            searchView.maxLines = 1
            searchView.isSingleLine = true
            searchLayout.layoutParams = searchLayoutParams
            searchLayout.hint = "搜索关键词...（多关键词间加空格）"
            searchLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            filterArea.addView(searchLayout)

            searchView.setOnEditorActionListener { v, actionId, event ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        v.clearFocus()
                        stageData(termData.toFiltered())
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                        return@setOnEditorActionListener true
                    }
                }

                false
            }

        }

        prepareSearchBar()

        fun prepareSelectTermSpinner() {
            val view = Spinner(this)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            view.layoutParams = params

            params.marginStart = 56
            params.marginEnd = 56
            params.bottomMargin = 56

            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)

            view.adapter = adapter
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            filterArea.addView(view)

            var defaultTermName = ""
            thread {
                val calendars = TongjiApi.instance.getOneTongjiSchoolCalendarAllTermCalendar(this@TermArrangement) ?: return@thread
                runOnUiThread {
                    val thisTermId = intent.getStringExtra("calendarId")
                    for (idx in 0 until calendars.length()) {

                        val it = calendars.getJSONObject(idx)

                        val starMark = it.getBoolean("currentTermFlag") or it.getBoolean("nextTermFlag")

                        val fullName = it.getString("fullName") + (if (starMark) " ✨" else "")

                        schoolCalendarNameToId[fullName] = it.getInt("id").toString()
                        if (thisTermId == it.getInt("id").toString()) {
                            defaultTermName = fullName
                        }

                        adapter.add(fullName)
                    }

                    view.setSelection(adapter.getPosition(defaultTermName))
                }
            }

            view.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    v: View?,
                    position: Int,
                    id: Long
                ) {
                    loadData(schoolCalendarNameToId[view.selectedItem.toString()]!!)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }
        }

        prepareSelectTermSpinner()

        fun makeRow(): LinearLayout {
            val res = LinearLayout(this)
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            params.marginStart = 32
            params.marginEnd = 32
            params.bottomMargin = 24

            res.layoutParams = params
            res.orientation = LinearLayout.HORIZONTAL
            res.setPadding(24)

            return res
        }

        val filterRows = listOf(makeRow(), makeRow())
        filterRows.forEach { filterArea.addView(it) }


        filterInfoList.forEachIndexed { idx, info ->
            val filter = Spinner(this)
            val filterParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            filterParams.weight = 1f
            filter.layoutParams = filterParams
            filterRows[idx / 2].addView(filter)

            filterViews[info.key] = filter

            filter.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (filterPrevValue[info.key] != filter.selectedItem.toString()) {
                        filterPrevValue[info.key] = filter.selectedItem.toString()
                        stageData(termData.toFiltered())
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }

            filterPrevValue[info.key] = info.defaultValue

            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
            filterAdapters[info.key] = adapter

            filter.adapter = adapter
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }


        rootContainer.addView(filterArea)
        return filterArea.id

    }

    private fun filterAndStageDataSmooth() {
        courseCardContainer.removeAllViews()
        setProgress(true, true)
        thread {
            val filtered = termData.toFiltered()
            runOnUiThread { stageData(filtered) }
        }
    }

    private fun processRawCourseData() {

        filterInfoList.forEach { info ->
            val set = HashSet<String>()
            val targetField = TongjiApi.CourseArrangement::class.java.getDeclaredField(
                info.fieldNameInCourseArrangement
            )

            termData.forEach { course ->
                set.add(targetField.forceGet(course) as String? ?: "null")
            }

            val adapter = filterAdapters[info.key]!!

            runOnUiThread {
                adapter.add(info.defaultValue)
                set.forEach { value ->
                    adapter.add(value)
                }
            }
        }

        setProgress(true, true, "staging...")

        runOnUiThread {
            stageData(termData)
        }

    }

    private fun prepareProgressIndicator() {
        progressBar = LinearProgressIndicator(this)
        val pbParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        pbParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        val pbMargin = 24
        pbParams.marginStart = pbMargin
        pbParams.marginEnd = pbMargin
        progressBar.layoutParams = pbParams
        progressBar.isIndeterminate = true

        rootContainer.addView(progressBar)

        progressIndicator = TextView(this)
        progressIndicator.text = "0%"
        val piParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

        val progressBarId = genViewId()
        progressBar.id = progressBarId
        piParams.addRule(RelativeLayout.ABOVE, progressBar.id)
        piParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        piParams.bottomMargin = 48
        progressIndicator.textSize = 48f
        progressIndicator.layoutParams = piParams
        rootContainer.addView(progressIndicator)
    }

    private fun prepareRootContainer() {
        rootContainer = RelativeLayout(this)
        val rootParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        rootContainer.layoutParams = rootParams
    }

    private fun prepareCenterHintTextView() {
        centerHintTextView = TextView(this)
        val view = centerHintTextView
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        view.layoutParams = params

        view.textSize = 24f

        rootContainer.addView(view)
    }

    private fun prepareLayout() {
        prepareRootContainer()
        val filterAreaId = prepareFilterArea()
        prepareProgressIndicator()
        prepareScrollContainer(filterAreaId)
        prepareCenterHintTextView()
    }

    private fun List<TongjiApi.CourseArrangement>.toFiltered(): List<TongjiApi.CourseArrangement> {
        val res = ArrayList<TongjiApi.CourseArrangement>()

        this.forEach { course ->
            var nice = true


            // 输入框
            if (nice) {
                val tokens = searchEditText.text.toString().split(" ")

                for (token in tokens) {
                    if (token.isBlank()) {
                        continue
                    }

                    nice = false

                    val fields = course::class.java.declaredFields.filter { it.name != "Companion" }
                    for (field in fields) {
                        if (field.type != String::class.java) {
                            Log.e("type not matched!!", "${field.name}: ${field.type.name}")
                            continue
                        }

                        if (token in (field.forceGet(course) as String? ?: "")) {
                            nice = true
                            break
                        }
                    }

                    if (!nice) {
                        break
                    }

                }
            }


            // 下拉框
            if (nice) {
                for (filterInfo in filterInfoList) {
                    val filterValue = filterViews[filterInfo.key]!!.selectedItem.toString()

                    if (filterValue == filterInfo.defaultValue) {
                        continue
                    }

                    val field = course::class.java.getDeclaredField(filterInfo.fieldNameInCourseArrangement)
                    val value = field.forceGet(course)
                    if (value != filterValue) {
                        nice = false
                        break
                    }
                }
            }

            if (nice) {
                res.add(course)
            }
        }

        return res
    }


    private fun stageData(data: List<TongjiApi.CourseArrangement>) {
        courseCardContainer.removeAllViews()
        for (idx in data.indices) {

            val maxEntriesToShow = 50
            if (idx > maxEntriesToShow) {
                Toast.makeText(this, "结果过多，仅显示${maxEntriesToShow}条。请适当设置筛选条件", Toast.LENGTH_SHORT).show()
                break
            }

            val it = data[idx]

            val card = InfoCard.Builder(this)
                .setTitle(it.courseName.toString())
                .setOuterMarginTopSp(0f)
                .setEndMarkMarginBottomSp(16f)
                .setOuterMarginStartSp(16f)
                .setOuterMarginEndSp(16f)
                .setSpMultiply(resources.displayMetrics.scaledDensity)
                .setHasIcon(true)
                .setIcon(
                    when (it.campus) {
                        "1" /* 四平路 */ -> "fluentemoji/cherry_blossom_color.svg"
                        "3" /* 嘉定 */ -> "fluentemoji/ghost_color.svg"
                        "4" /* 沪西 */ -> "fluentemoji/hospital_color.svg"
                        else -> "fluentemoji/flushed_face_color.svg"
                    }
                )
                .addInfo(InfoCard.Info("学院", it.facultyI18n))
                .addInfo(InfoCard.Info("类别", it.courseLabelName))
                .addInfo(InfoCard.Info("学分", it.credits))
                .addInfo(InfoCard.Info("校区", it.campusI18n))
                .addInfo(InfoCard.Info("安排", it.arrangeInfo))
                .addInfo(InfoCard.Info("考核", it.assessmentModeI18n))
                .build()

            courseCardContainer.addView(card)
        }

        setProgress(false)

        centerHintTextView.text = if (courseCardContainer.isEmpty()) {
            "没有满足条件的课呀.."
        } else {
            ""
        }
    }

    private var fetchDataApiControl = TongjiApi.GetOneTongjiTermArrangementApiControl()
    private var targetCalendarId: AtomicInteger = AtomicInteger(0)
    private val fetchDataRequestId = AtomicInteger(0)

    private fun loadData(calendarId: String = intent.getStringExtra("calendarId") ?: "0", forceLoad: Boolean = false) {

        if (calendarId.toInt() == targetCalendarId.get() && !forceLoad) {
            return
        }

        val requestTicket = fetchDataRequestId.incrementAndGet()
        fun ticketAvailabel() = requestTicket == fetchDataRequestId.get()

        targetCalendarId.set(calendarId.toInt())
        fetchDataApiControl.stop.set(true)
        fetchDataApiControl = TongjiApi.GetOneTongjiTermArrangementApiControl()

        setProgress(true, true, "preparing...")

        courseCardContainer.removeAllViews()

        this.searchEditText.setText("")
        filterInfoList.forEach {
            filterPrevValue[it.key] = it.defaultValue
            val adapter = filterAdapters[it.key]!!
            adapter.clear()
            adapter.add(it.defaultValue)
        }

        thread {
            termData = TongjiApi.instance.getOneTongjiTermArrangement(
                calendarId,
                this@TermArrangement,
                fetchDataApiControl
            ) {

                if (it < 5) {
                    return@getOneTongjiTermArrangement
                }

                if (!ticketAvailabel()) {
                    return@getOneTongjiTermArrangement
                }

                setProgress(true, false, "$it%", it)

            } ?: return@thread

            if (!ticketAvailabel()) {
                return@thread
            }

            runOnUiThread {
                setProgress(true, true, "processing...")

                thread {
                    processRawCourseData()
                }
            }
        }
    }

    override fun onDestroy() {
        fetchDataApiControl.stop.set(true)
        super.onDestroy()
    }

}

