package ru.rrtry.silentdroid.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(@ApplicationContext val context: Context) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun writeThumbnail(uuid: UUID, bitmap: Bitmap?) {
        if (bitmap != null) {
            withContext(Dispatchers.IO) {
                try {
                    FileOutputStream(File(resolvePath(context, uuid))).apply {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                        flush()
                        close()
                    }
                } catch (e: IOException) {
                    Log.e("FileManager", "writeThumbnail: $e")
                }
            }
        }
    }

    suspend fun deleteThumbnail(uuid: UUID): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                File(resolvePath(context, uuid)).delete()
            } catch (e: IOException) {
                Log.e("FileManager", "deleteThumbnail: $e")
                false
            }
        }
    }

    companion object {

        private const val DIRECTORY_NAME: String = "Snapshots"

        fun resolvePath(context: Context, id: UUID): String {
            return getSnapshotsDir(context) + File.separatorChar + "$id.png"
        }

        private fun getSnapshotsDir(context: Context): String {
            val snapshotsDir: File = File(context.filesDir, DIRECTORY_NAME)
            if (!snapshotsDir.exists()) {
                snapshotsDir.mkdir()
            }
            return snapshotsDir.absolutePath
        }
    }
}