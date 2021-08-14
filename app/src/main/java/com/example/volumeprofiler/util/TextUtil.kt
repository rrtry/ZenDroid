package com.example.volumeprofiler.util

import android.util.Log

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

        fun filterRadiusInput(source: CharSequence?): CharSequence {
            return if (source != null) {
                val stringBuilder: StringBuilder = StringBuilder()
                for (i in source) {
                    if (Character.isDigit(i) || i == '.') {
                        stringBuilder.append(i)
                    }
                }
                stringBuilder.toString()
            } else {
                ""
            }
        }

        fun validateRadiusInput(s: CharSequence?, currentMetric: Metrics): Boolean {
            if (s == null || s.toString().isEmpty()) {
                return false
            }
            return if (currentMetric == Metrics.METERS) {
                s.toString().toFloat() <= Metrics.METERS.sliderMaxValue
            } else {
                s.toString().toFloat() <= Metrics.KILOMETERS.sliderMaxValue
            }
        }
    }
}