package com.example.volumeprofiler.ui.views

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.content.Intent
import android.content.Intent.ACTION_SEARCH
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager.MATCH_ALL
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH
import android.speech.RecognizerIntent.ACTION_WEB_SEARCH
import android.transition.ChangeBounds
import android.transition.TransitionManager.beginDelayedTransition
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.LocationSuggestionItemViewBinding
import com.example.volumeprofiler.databinding.SearchViewBinding
import com.example.volumeprofiler.ui.activities.MapsActivity
import com.example.volumeprofiler.util.AddressWrapper

class AddressSearchView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null):
    FrameLayout(context, attributeSet),
    View.OnClickListener,
    View.OnFocusChangeListener,
    TextView.OnEditorActionListener {

    class AddressAdapter(private val listener: OnSuggestionListener):
        ListAdapter<AddressWrapper, AddressAdapter.AddressViewHolder>(object : DiffUtil.ItemCallback<AddressWrapper>() {

        override fun areItemsTheSame(oldItem: AddressWrapper, newItem: AddressWrapper): Boolean {
            return oldItem.latitude == newItem.latitude && oldItem.longitude == newItem.longitude
        }

        override fun areContentsTheSame(oldItem: AddressWrapper, newItem: AddressWrapper): Boolean {
            return oldItem == newItem
        }

    }) {

        class AddressViewHolder(
            private val binding: LocationSuggestionItemViewBinding,
            private val listener: OnSuggestionListener
        ): RecyclerView.ViewHolder(binding.root) {

            fun bind(address: AddressWrapper) {
                binding.locationIcon.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        itemView.context.resources,
                        if (address.recentQuery) DRAWABLE_SAVED_QUERY else DRAWABLE_LOCATION_SUGGESTION,
                        itemView.context.theme
                    )
                )
                binding.locationName.text = address.address
                binding.root.setOnClickListener { listener.onSuggestionSelected(address) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
            return AddressViewHolder(
                LocationSuggestionItemViewBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), listener
            )
        }

        override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
            holder.bind(currentList[position])
        }
    }

    interface OnSuggestionListener {

        fun onSuggestionSelected(address: AddressWrapper)

        fun onSuggestionRemoved(address: AddressWrapper)
    }

    interface OnQueryTextChangeListener {

        fun onQueryTextChange(query: String?)

        fun onQueryTextSubmit(query: String?)
    }

    private var notifyListener: Boolean = true
    var queryListener: OnQueryTextChangeListener? = null
    var selectionListener: OnSuggestionListener? = null

    var adapter: ListAdapter<AddressWrapper, AddressAdapter.AddressViewHolder>? = null
    set(value) { binding.suggestionsList.adapter = value; field = value }

    var query: String? = null
    private set

    val suggestionsVisible: Boolean get() = binding.suggestionsList.isVisible
    private val isQueryEmpty: Boolean get() = binding.searchEditText.text.isNullOrEmpty()
    private var binding: SearchViewBinding = SearchViewBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    init {
        binding.rightImageView.setOnClickListener(this)
        binding.leftImageView.setOnClickListener(this)
        binding.searchEditText.setOnEditorActionListener(this)
        binding.searchEditText.onFocusChangeListener = this
        binding.searchEditText.addDebounceTextWatcher(
            { text ->
                if (notifyListener) queryListener?.onQueryTextChange(text)
                notifyListener = true
                query = text
            },
            { onUpdate() }
        )
        binding.suggestionsList.layoutManager = LinearLayoutManager(context)
        onUpdate()
    }

    private fun submitQuery(results: List<AddressWrapper>?) {
        if (binding.searchEditText.isFocused) {
            beginDelayedTransition(this, ChangeBounds())
            binding.suggestionsList.isVisible = !results.isNullOrEmpty()
            adapter?.submitList(results?.ifEmpty { null })
        }
    }

    fun setQuery(query: String, notifyChange: Boolean = true) {
        notifyListener = notifyChange
        binding.searchEditText.setText(query)
    }

    fun updateAdapter(results: List<AddressWrapper>) {
        submitQuery(results)
    }

    fun closeSuggestions() {
        onActionUp()
    }

    private fun onActionUp() {
        submitQuery(null)
        toggleSoftInput(false)
        binding.searchEditText.clearFocus()
    }

    private fun clearQuery() {
        beginDelayedTransition(this, ChangeBounds())
        toggleSoftInput(true)
        binding.searchEditText.requestFocus()
        binding.searchEditText.text?.clear()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.rightImageView -> if (isQueryEmpty) onVoiceSearchRequested() else clearQuery()
            binding.leftImageView -> onActionUp()
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            queryListener?.onQueryTextSubmit(binding.searchEditText.text.toString())
            return true
        }
        return false
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) setQuery(binding.searchEditText.text.toString(), true)
        binding.leftImageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                if (hasFocus) DRAWABLE_ACTION_UP else DRAWABLE_SEARCH_LOGO,
                context.theme
            )
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        queryListener = null
        selectionListener = null
    }

    private fun onVoiceSearchRequested() {
        getVoiceSearchIntent()?.let { intent ->
            startActivityForResult(context as Activity, intent, 100, null)
        }
    }

    private fun getVoiceSearchIntent(): Intent? {
        val componentName: ComponentName = ComponentName(context, MapsActivity::class.java)
        val recognizerIntent: Intent? = when {
            isAppVoiceSearchAvailable() -> {
                Intent(ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
            }
            isWebVoiceSearchAvailable() -> {
                Intent(ACTION_WEB_SEARCH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                }
            } else -> null
        }
        val searchPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(ACTION_SEARCH).apply { component = componentName },
            FLAG_ONE_SHOT or FLAG_MUTABLE
        )
        return recognizerIntent?.apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            putExtra(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT, searchPendingIntent)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, componentName.flattenToShortString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun isWebVoiceSearchAvailable(): Boolean {
        return context.packageManager
            .queryIntentActivities(Intent(ACTION_WEB_SEARCH), MATCH_ALL)
            .isNotEmpty()
    }

    private fun isAppVoiceSearchAvailable(): Boolean {
        return context.packageManager
            .queryIntentActivities(Intent(ACTION_RECOGNIZE_SPEECH), MATCH_ALL)
            .isNotEmpty()
    }

    private fun toggleSoftInput(show: Boolean) {
        val inputMethodService = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            inputMethodService.showSoftInput(binding.searchEditText, 0)
        } else {
            inputMethodService.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        }
    }

    private fun onUpdate() {

        val empty: Boolean =
            binding.searchEditText.text.isNullOrEmpty() ||
            binding.searchEditText.text.isNullOrBlank()

        binding.rightImageView.isVisible = !(empty && !isAppVoiceSearchAvailable())
        binding.rightImageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                if (empty) DRAWABLE_VOICE_SEARCH else DRAWABLE_CLEAR_SEARCH,
                context.theme
            )
        )
    }

    companion object {

        private const val DRAWABLE_SAVED_QUERY: Int = R.drawable.ic_baseline_timelapse_24
        private const val DRAWABLE_LOCATION_SUGGESTION: Int = R.drawable.baseline_location_on_black_24dp
        private const val DRAWABLE_ACTION_UP: Int = R.drawable.ic_baseline_arrow_back_24
        private const val DRAWABLE_SEARCH_LOGO: Int = R.drawable.ic_baseline_search_24
        private const val DRAWABLE_VOICE_SEARCH: Int = R.drawable.ic_baseline_keyboard_voice_24
        private const val DRAWABLE_CLEAR_SEARCH: Int = R.drawable.ic_baseline_clear_24
    }
}