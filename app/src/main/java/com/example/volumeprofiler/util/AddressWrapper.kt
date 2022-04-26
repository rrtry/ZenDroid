package com.example.volumeprofiler.util

import java.util.*

data class AddressWrapper(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    var recentQuery: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false

        val wrapper: AddressWrapper = other as AddressWrapper
        return (Objects.equals(address, wrapper.address)
                && Objects.equals(latitude, wrapper.latitude)
                && Objects.equals(longitude, wrapper.longitude))
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + address.hashCode()
        return result
    }
}
