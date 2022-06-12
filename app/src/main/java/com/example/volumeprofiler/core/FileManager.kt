package com.example.volumeprofiler.core

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(@ApplicationContext val context: Context) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun writeThumbnail(uuid: UUID, bitmap: Bitmap?) {
        if (bitmap != null) {
            withContext(Dispatchers.IO) {
                FileOutputStream(File(resolvePath(context, uuid))).apply {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                    flush()
                    close()
                }
            }
        }
    }

    suspend fun deleteThumbnail(uuid: UUID): Boolean {
        return withContext(Dispatchers.IO) {
            File(resolvePath(context, uuid)).delete()
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