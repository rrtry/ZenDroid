package com.example.volumeprofiler.ui.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class DebounceEditText @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
): AppCompatEditText(context, attributeSet) {

    private var job: Job = Job()
    private val scope: CoroutineScope get() = CoroutineScope(Dispatchers.Main + job)

    fun addDebounceTextWatcher(
        onTextChangedAction: (String?) -> Unit,
        afterTextChangedAction: (String?) -> Unit
    ) {
        job = getDebounceTextWatcher(afterTextChangedAction)
            .debounce(DEBOUNCE_TIMEOUT)
            .onEach { onTextChangedAction(it.toString()) }
            .launchIn(scope)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        job.cancel()
    }

    private fun getDebounceTextWatcher(
        onAfterTextChanged: (String) -> Unit
    ): Flow<CharSequence?> {
        return callbackFlow<CharSequence?> {
            val textWatcher: TextWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { onAfterTextChanged(s.toString()) }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { trySend(s) }
            }
            addTextChangedListener(textWatcher)
            awaitClose { removeTextChangedListener(textWatcher) }
        }.onStart { emit(text) }
    }

    companion object {

        private const val DEBOUNCE_TIMEOUT: Long = 1000L
    }
}