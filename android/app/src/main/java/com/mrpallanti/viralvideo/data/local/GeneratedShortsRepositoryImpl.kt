package com.mrpallanti.viralvideo.data.local

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.mrpallanti.viralvideo.domain.model.GeneratedShortItem
import com.mrpallanti.viralvideo.domain.repository.GeneratedShortsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class GeneratedShortsRepositoryImpl(
    private val context: Context
) : GeneratedShortsRepository {

    private val prefs = context.getSharedPreferences("viral_generated_shorts", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val _shortsFlow = MutableStateFlow<List<GeneratedShortItem>>(emptyList())

    init {
        loadPersistedShorts()
    }

    override fun getRecentShorts(): Flow<List<GeneratedShortItem>> = _shortsFlow.asStateFlow()

    override suspend fun saveShort(item: GeneratedShortItem) = withContext(Dispatchers.IO) {
        val currentList = _shortsFlow.value.toMutableList()
        // Add at the beginning, deduplicating by ID or URI
        currentList.removeAll { it.id == item.id || it.outputUri == item.outputUri }
        currentList.add(0, item)
        // Keep up to 50 recent shorts
        val trimmed = if (currentList.size > 50) currentList.subList(0, 50) else currentList
        persistList(trimmed)
        _shortsFlow.value = trimmed
    }

    override suspend fun deleteShort(id: String) = withContext(Dispatchers.IO) {
        val currentList = _shortsFlow.value.toMutableList()
        val itemToDelete = currentList.find { it.id == id }
        if (itemToDelete != null) {
            currentList.remove(itemToDelete)
            persistList(currentList)
            _shortsFlow.value = currentList

            // Also attempt to delete the local file if it is a local path
            try {
                if (itemToDelete.outputUri.startsWith("/")) {
                    val file = File(itemToDelete.outputUri)
                    if (file.exists()) file.delete()
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun loadPersistedShorts() {
        try {
            val jsonString = prefs.getString("shorts_list_v1", null)
            val list: MutableList<GeneratedShortItem> = if (!jsonString.isNullOrBlank()) {
                json.decodeFromString<List<GeneratedShortItem>>(jsonString).toMutableList()
            } else {
                mutableListOf()
            }

            // Sync with MediaStore for any existing shorts saved to Movies/ViralClips
            val mediaStoreItems = scanMediaStoreForViralClips()
            for (mediaItem in mediaStoreItems) {
                if (list.none { it.outputUri == mediaItem.outputUri || it.title == mediaItem.title }) {
                    list.add(mediaItem)
                }
            }

            // Sort by most recent timestamp descending
            list.sortByDescending { it.timestamp }
            _shortsFlow.value = list
        } catch (t: Throwable) {
            _shortsFlow.value = emptyList()
        }
    }

    private fun persistList(list: List<GeneratedShortItem>) {
        try {
            val serialized = json.encodeToString(list)
            prefs.edit().putString("shorts_list_v1", serialized).apply()
        } catch (ignored: Exception) {}
    }

    private fun scanMediaStoreForViralClips(): List<GeneratedShortItem> {
        val results = mutableListOf<GeneratedShortItem>()
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE
            )

            val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("ViralShort_%")
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                var count = 0
                while (cursor.moveToNext() && count < 25) {
                    val mediaId = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "ViralShort"
                    val durMs = cursor.getLong(durCol)
                    val dateAddedSec = cursor.getLong(dateCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val contentUri = ContentUris.withAppendedId(collection, mediaId).toString()

                    val sizeMb = sizeBytes / (1024f * 1024f)
                    val formattedSize = if (sizeMb > 0) String.format("%.1f MB", sizeMb) else ""

                    results.add(
                        GeneratedShortItem(
                            id = "ms_$mediaId",
                            title = name.removeSuffix(".mp4").replace('_', ' '),
                            outputUri = contentUri,
                            durationSeconds = (durMs / 1000f).coerceAtLeast(1f),
                            timestamp = dateAddedSec * 1000L,
                            fileSizeFormatted = formattedSize,
                            hookCaption = "Auto 9:16 Short"
                        )
                    )
                    count++
                }
            }
        } catch (ignored: Exception) {}
        return results
    }
}
