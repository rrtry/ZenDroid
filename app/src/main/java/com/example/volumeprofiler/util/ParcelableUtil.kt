package com.example.volumeprofiler.util

import android.os.Parcel
import android.os.Parcelable

class ParcelableUtil {

    companion object {

        fun toByteArray(parcelable: Parcelable): ByteArray? {
            val parcel = Parcel.obtain()
            parcelable.writeToParcel(parcel, 0)
            val bytes = parcel.marshall()
            parcel.recycle()
            return bytes
        }

        private fun toParcel(bytes: ByteArray): Parcel {
            val parcel = Parcel.obtain()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            return parcel
        }

        fun <T> toParcelable(bytes: ByteArray, creator: Parcelable.Creator<T>): T {
            val parcel = toParcel(bytes)
            val result = creator.createFromParcel(parcel)
            parcel.recycle()
            return result
        }

        inline fun <reified T : Parcelable> getParcelableCreator(): Parcelable.Creator<T> {
            val creator = T::class.java.getField("CREATOR").get(null)
            @Suppress("UNCHECKED_CAST")
            return creator as Parcelable.Creator<T>
        }
    }
}