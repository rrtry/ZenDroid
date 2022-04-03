package com.example.volumeprofiler.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.CalendarEventActivityBinding
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.EventDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.IllegalArgumentException

@SuppressLint("Range")
@AndroidEntryPoint
class CalendarEventDetailsActivity: AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>,
    ContentQueryHandler.AsyncQueryCallback {

    @Inject
    lateinit var profileManager: ProfileManager

    @Inject
    lateinit var scheduleManager: ScheduleManager

    @Inject
    lateinit var contentUtil: ContentUtil

    private lateinit var calendarListPopupWindow: ListPopupWindow
    private lateinit var binding: CalendarEventActivityBinding

    private val viewModel: EventDetailsViewModel by viewModels()

    private var elapsedTime: Long = 0L
    private var startProfile: Profile? = null
    private var endProfile: Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()
        setActionBar()

        binding.eventAutoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val eventsCursor: Cursor? = getEventsCursor()
            eventsCursor?.moveToPosition(position)
            eventsCursor?.getInt(eventsCursor.getColumnIndex(CalendarContract.Events._ID))?.let {
                viewModel.setEventId(it)
                contentUtil.queryEventNextInstances(it, TOKEN_EVENT_INSTANCE, null,this)
            }
            eventsCursor?.getLong(eventsCursor.getColumnIndex(CalendarContract.Events.DTSTART))?.let {
                viewModel.startTime = it
            }
            eventsCursor?.getLong(eventsCursor.getColumnIndex(CalendarContract.Events.DTEND))?.let {
                viewModel.endTime = it
            }
            eventsCursor?.getString(eventsCursor.getColumnIndex(CalendarContract.Events.RRULE))?.let {
                viewModel.rrule = it
            }
            viewModel.eventTitle = view.findViewById<TextView>(R.id.event_display_name).text.toString()
        }
        binding.calendarAutoCompleteTextView.setOnClickListener {
            calendarListPopupWindow.show()
        }
        binding.actionViewButton.setOnClickListener {
            viewCalendarEvent()
        }

        createCalendarListPopupWindow(binding.calendarAutoCompleteTextView)
        setEventAutoCompleteTextViewAdapter()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profilesStateFlow.collect {
                        if (!viewModel.isEventSet()) {
                            val event: Event? = getEvent()
                            if (event != null) {
                                viewModel.setEvent(event, it)
                                setSelection(event)
                            }
                        }
                    }
                }
                launch {
                    viewModel.eventStartsProfile.collect {
                        startProfile = it
                    }
                }
                launch {
                    viewModel.eventEndsProfile.collect {
                        endProfile = it
                    }
                }
            }
        }
        startLoader(ID_LOADER_CALENDARS)
    }

    override fun onQueryComplete(cursor: Cursor?, cookie: Any?, token: Int) {
        cursor?.use {
            if (cursor.moveToFirst()) {
                val id: Int = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID))
                val rrule: String? = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.RRULE))
                viewModel.timezoneId = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.EVENT_TIMEZONE))
                viewModel.instanceStartTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN))
                viewModel.instanceEndTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END))
                Log.i("CalendarActivity", "rrule:$ $rrule")
            }
        }
    }

    private fun setBinding(): Unit {
        binding = CalendarEventActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
    }

    private fun viewCalendarEvent(): Unit {
        val id: Int = viewModel.getEventId()
        if (id != -1) {
            val uri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id.toLong())
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
            }
            startActivity(intent)
        } else {
            Snackbar.make(binding.root, "Select an event first", Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun setSelection(event: Event): Unit {
        binding.calendarAutoCompleteTextView.setText(event.calendarTitle, false)
        binding.eventAutoCompleteTextView.setText(event.title, false)
    }

    private fun getEvent(): Event? {
        return intent.getParcelableExtra(EXTRA_EVENT) as? Event
    }

    private fun setEventAutoCompleteTextViewAdapter(): Unit {
        val cursorAdapter = object : SimpleCursorAdapter(
            this,
            R.layout.calendar_event_item_view,
            null,
            arrayOf(CalendarContract.Events.TITLE),
            intArrayOf(R.id.event_display_name),
            0
        ) {
            override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
                super.bindView(view, context, cursor)
                val title: String? = cursor?.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE))
                val textView: TextView = view!!.findViewById(R.id.event_display_name)
                if (title != null && title.isNotEmpty()) {
                    textView.text = title
                }
                else {
                    textView.text = "(No title)"
                }
            }
        }
        cursorAdapter.setCursorToStringConverter {
            val title: String = it.getString(it.getColumnIndex(CalendarContract.Events.TITLE))
            if (title.isEmpty()) "(No title)" else title
        }
        cursorAdapter.setFilterQueryProvider {
            runOnUiThread {
                val bundle: Bundle? = if (it != null) {
                    Bundle().apply {
                        putString(EXTRA_QUERY, it.toString())
                    }
                } else null
                restartLoader(ID_LOADER_EVENTS, bundle)
            }
            null
        }
        binding.eventAutoCompleteTextView.setAdapter(cursorAdapter)
    }

    private fun setListPopupWindowAdapter(cursor: Cursor?): Unit {
        calendarListPopupWindow.setAdapter(SimpleCursorAdapter(
            this,
            R.layout.calendar_item_view,
            cursor,
            arrayOf(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
            intArrayOf(R.id.calendar_display_name),
            0
        ))
    }

    private fun getCalendarCursor(): Cursor? {
        val adapter = calendarListPopupWindow.listView?.adapter as? SimpleCursorAdapter
        return adapter?.cursor
    }

    private fun setEventsCursor(cursor: Cursor?): Unit {
        (binding.eventAutoCompleteTextView.adapter as? SimpleCursorAdapter)?.swapCursor(cursor)
    }

    private fun getEventsCursor(): Cursor? {
        return (binding.eventAutoCompleteTextView.adapter as? SimpleCursorAdapter)?.cursor
    }

    private fun createCalendarListPopupWindow(anchorView: View): Unit {
        calendarListPopupWindow = ListPopupWindow(this)
        calendarListPopupWindow.softInputMode = ListPopupWindow.INPUT_METHOD_FROM_FOCUSABLE
        calendarListPopupWindow.isModal = true
        calendarListPopupWindow.anchorView = anchorView
        calendarListPopupWindow.setBackgroundDrawable(ViewUtil.resolveResourceAttribute(this, R.attr.colorOnPrimary))
        setListPopupWindowAdapter(null)
        calendarListPopupWindow.setOnItemClickListener { parent, view, position, id ->
            val calendarCursor: Cursor? = getCalendarCursor()
            if (calendarCursor != null) {

                calendarCursor.moveToPosition(position)

                val calendarId: Int = calendarCursor.getInt(calendarCursor.getColumnIndex(CalendarContract.Calendars._ID))
                val calendarTitle: String = calendarCursor.getString(calendarCursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))

                if (viewModel.calendarId != calendarId) {
                    viewModel.setEventId(-1)
                    binding.eventAutoCompleteTextView.setText(null, false)
                }

                viewModel.setCalendarID(calendarId)
                viewModel.calendarTitle = calendarTitle
                binding.calendarAutoCompleteTextView.setText(viewModel.calendarTitle, false)

                restartLoader(ID_LOADER_EVENTS, null)

                calendarListPopupWindow.dismiss()
            }
        }
        calendarListPopupWindow.setOnDismissListener {
            binding.calendarAutoCompleteTextView.clearFocus()
        }
    }

    private fun restartLoader(id: Int, bundle: Bundle? = null): Unit {
        val loaderManager: LoaderManager = LoaderManager.getInstance(this)
        loaderManager.restartLoader(id, bundle, this)
    }

    private fun startLoader(id: Int, bundle: Bundle? = null): Unit {
        val loaderManager: LoaderManager = LoaderManager.getInstance(this)
        loaderManager.initLoader(id, bundle, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return when (id) {
            ID_LOADER_CALENDARS -> {
                CalendarCursorLoader(this)
            }
            ID_LOADER_EVENTS -> {
                val query: String? = args?.getString(EXTRA_QUERY)
                EventsCursorLoader(this, query, viewModel.calendarId)
            }
            else -> throw IllegalArgumentException("Invalid loader id")
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (data != null) {
            when (loader.id) {
                ID_LOADER_CALENDARS -> {
                    setListPopupWindowAdapter(data)
                    restartLoader(ID_LOADER_EVENTS, null)
                }
                ID_LOADER_EVENTS -> {
                    setEventsCursor(data)
                }
            }
        }
    }

    private fun setActionBar(): Unit {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (intent.extras == null) {
            supportActionBar?.title = "Create event"
        } else {
            supportActionBar?.title = "Edit event"
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (elapsedTime + ViewUtil.DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
            setResultCancelled()
        } else {
            Toast.makeText(this, "Press back button again to exit", Toast.LENGTH_LONG).show()
        }
        elapsedTime = System.currentTimeMillis()
    }

    private fun onSaveChangesItemClick(): Unit {
        val event: Event = viewModel.getEvent()
        val currentMillis: Long = System.currentTimeMillis()
        if (event.endTime != 0L && currentMillis >= event.endTime) {
            Snackbar.make(binding.root, "This event has completed", Snackbar.LENGTH_LONG)
                .show()
            return
        }
        /*
        if (currentMillis > event.instanceBeginTime && currentMillis < event.instanceEndTime) {
            profileUtil.setProfile(startProfile!!)
            alarmUtil.scheduleAlarm(event, endProfile!!, Event.State.END)
        } else {
            alarmUtil.scheduleAlarm(event, startProfile!!, Event.State.START)
        }
         */
        setSuccessfulResult(event)
    }

    private fun setResultCancelled(): Unit {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun setSuccessfulResult(event: Event): Unit {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_EVENT, event)
            putExtra(EXTRA_UPDATE, intent.extras != null)
        })
        finish()
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        Log.i("CalendarActivity", "onLoaderReset()")
    }

    companion object {

        private const val TOKEN_EVENT_INSTANCE: Int = 4
        private const val ID_LOADER_CALENDARS: Int = 0
        private const val ID_LOADER_EVENTS: Int = 1
        private const val EXTRA_QUERY: String = "extra_query"

        private const val EXTRA_CALENDAR_ID: String = "calendar_id"
        internal const val EXTRA_EVENT: String = "extra_event"
        internal const val EXTRA_UPDATE: String = "extra_update"
        internal const val EXTRA_START_PROFILE: String = "extra_start_profile"
        internal const val EXTRA_END_PROFILE: String = "extra_end_profile"
    }
}