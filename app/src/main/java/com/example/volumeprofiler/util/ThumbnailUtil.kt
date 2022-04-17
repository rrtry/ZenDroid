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
    val file: File = File(resolvePath(context, uuid))
    val out: FileOutputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    out.flush()
    out.close()
}

fun deleteThumbnail(context: Context, uuid: UUID): Boolean {
    val file: File = File(resolvePath(context, uuid))
    return file.delete()
}

private const val DIRECTORY_NAME: String = "Snapshots"