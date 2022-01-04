package com.example.volumeprofiler.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.*

private fun resolveAbsolutePath(context: Context, uuid: UUID): String {
    return "${getAppSpecificAlbumStorageDir(context).absolutePath}/$uuid.jpeg"
}

fun writeCompressedBitmap(context: Context, uuid: UUID, bitmap: Bitmap): Unit {
    val file: File = File(resolveAbsolutePath(context, uuid))
    val out: FileOutputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
    out.flush()
    out.close()
}

fun getPreviewBitmapFile(context: Context, uuid: UUID): File {
    return File(resolveAbsolutePath(context, uuid))
}

fun getAppSpecificAlbumStorageDir(context: Context): File {
    val file = File(context.getExternalFilesDir(
        Environment.DIRECTORY_PICTURES), DIRECTORY_NAME)
    if (!file.mkdirs()) {
        Log.e(LOG_TAG, "Directory not created")
    }
    return file
}

private const val LOG_TAG: String = "ContextUtil"
private const val DIRECTORY_NAME: String = "Snapshots"