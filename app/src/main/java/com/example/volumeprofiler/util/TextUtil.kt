package com.example.volumeprofiler.util

import android.util.Log
import androidx.core.text.isDigitsOnly

class TextUtil {

    companion object {

        fun validateCoordinatesInput(source: CharSequence?): Boolean {
            return if (source != null && source.isNotEmpty()) {
                val double: Double? = source.toString().toDoubleOrNull()
                double != null
            } else {
                false
            }
        }

        fun filterCoordinatesInput(source: CharSequence?): CharSequence {
            return if (source != null) {
                val stringBuilder: StringBuilder = StringBuilder()
                for (i in source) {
                    if (Character.isDigit(i) || i == '.' || i == '-') {
                        stringBuilder.append(i)
                    }
                }
                stringBuilder.toString()
            } else {
                ""
            }
        }

        fun filterStreetAddressInput(source: CharSequence?): CharSequence {
            return if (source != null) {
                val stringBuilder: StringBuilder = StringBuilder()
                for (i in source) {
                    if (Character.isLetter(i) || Character.isDigit(i) || i == ',' || i == '.' || i == '-' || Character.isSpaceChar(i)) {
                        stringBuilder.append(i)
                    }
                }
                stringBuilder.toString()
            } else {
                ""
            }
        }

        fun validateAddressInput(source: CharSequence?): Boolean {
            return !(source!!.isEmpty() && source.isBlank() && source.isDigitsOnly())
        }
    }
}