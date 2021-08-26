package com.example.volumeprofiler.util

enum class Priority(name: String) {

    STARRED_CONTACTS("From starred contacts only"),
    ALL_SENDERS("From anyone"),
    CONTACTS("From contacts only"),
    NONE("Deny all")

}