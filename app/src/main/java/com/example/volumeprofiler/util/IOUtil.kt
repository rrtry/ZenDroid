package com.example.volumeprofiler.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.*

private fun getAppSpecificStorageDir(context: Context): String {
    val snapshotsDir: File = File(context.filesDir, DIRECTORY_NAME)
    if (!snapshotsDir.exists()) {
        snapshotsDir.mkdir()
    }
    return snapshotsDir.absolutePath
}

fun resolvePath(context: Context, id: UUID): String {
    return getAppSpecificStorageDir(context) + File.separatorChar + "$id.png"
}

fun writeThumbnail(context: Context, uuid: UUID, bitmap: Bitmap) {
    val out: FileOutputStream = FileOutputStream(File(resolvePath(context, uuid)))
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    out.flush()
    out.close()
}

fun deleteThumbnail(context: Context, uuid: UUID): Boolean {
    return File(resolvePath(context, uuid)).delete()
}

private const val DIRECTORY_NAME: String = "Snapshots"