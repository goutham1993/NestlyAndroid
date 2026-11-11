package com.lifestyle.nestly.data.supabase

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.io.File

/**
 * Activity photos live in the private `activity-photos` bucket at
 * {family_id}/{activity_id}/{file}. The UI works with local file paths, so
 * picked photos are uploaded on save and remote photos are downloaded to a
 * cache file when an entry is opened for editing.
 */
class PhotoStore(
    private val context: Context,
    private val client: SupabaseClient,
) {
    private val cacheDir = File(context.cacheDir, "remote_photos").apply { mkdirs() }
    private val downloaded = mutableMapOf<String, String>() // local cache path -> storage path

    fun isLocalFile(path: String?): Boolean = path != null && File(path).exists()

    /** Uploads a picked local photo and returns its storage path. Non-file values pass through. */
    suspend fun uploadIfLocal(photoPath: String?, familyId: String, activityId: String): String? {
        if (photoPath == null) return null
        downloaded[photoPath]?.let { return it }
        val file = File(photoPath)
        if (!file.exists()) return photoPath // already a storage path
        val storagePath = "$familyId/$activityId/${file.name}"
        client.storage.from(BUCKET).upload(storagePath, file.readBytes()) { upsert = true }
        return storagePath
    }

    /** Returns a local path for display/editing, downloading from Storage if needed. */
    suspend fun resolveToLocal(photoPath: String?): String? {
        if (photoPath == null) return null
        if (File(photoPath).exists()) return photoPath
        val cached = File(cacheDir, photoPath.replace('/', '_'))
        if (!cached.exists()) {
            val bytes = client.storage.from(BUCKET).downloadAuthenticated(photoPath)
            cached.writeBytes(bytes)
        }
        downloaded[cached.absolutePath] = photoPath
        return cached.absolutePath
    }

    /**
     * Uploads a newly picked child photo. A storage path (or null) is returned unchanged.
     * Staged picks live as files under app storage; cached downloads are recorded in [downloaded].
     */
    suspend fun uploadProfileIfLocal(photoPath: String?, familyId: String, profileId: String): String? {
        if (photoPath == null) return null
        downloaded[photoPath]?.let { return it }
        val file = File(photoPath)
        if (!file.exists()) return photoPath
        val storagePath = "$familyId/$profileId/${file.name}"
        client.storage.from(PROFILE_BUCKET).upload(storagePath, file.readBytes()) { upsert = true }
        return storagePath
    }

    /** Local path for a child photo, downloading from the profile-photos bucket when needed. */
    suspend fun resolveProfileToLocal(photoPath: String?): String? {
        if (photoPath == null) return null
        if (File(photoPath).exists()) return photoPath
        val cached = File(cacheDir, "profile_" + photoPath.replace('/', '_'))
        if (!cached.exists()) {
            val bytes = client.storage.from(PROFILE_BUCKET).downloadAuthenticated(photoPath)
            cached.writeBytes(bytes)
        }
        downloaded[cached.absolutePath] = photoPath
        return cached.absolutePath
    }

    /** Removes one shared child photo. Local staged files are left for the caller. */
    suspend fun deleteProfile(path: String?) {
        if (path.isNullOrBlank() || File(path).exists()) return
        client.storage.from(PROFILE_BUCKET).delete(listOf(path))
    }

    /** Deletes every photo stored under {family_id}/… (used by the test-only clear-data action). */
    suspend fun clearFamilyPhotos(familyId: String) = clearBucket(BUCKET, familyId)

    /** Deletes every shared child photo for this family. */
    suspend fun clearFamilyProfilePhotos(familyId: String) = clearBucket(PROFILE_BUCKET, familyId)

    private suspend fun clearBucket(bucketId: String, familyId: String) {
        val bucket = client.storage.from(bucketId)
        val paths = mutableListOf<String>()
        bucket.list(familyId).forEach { entry ->
            if (entry.id == null) {
                // Folder (an activity or profile id): collect the files inside it.
                bucket.list("$familyId/${entry.name}").forEach { file ->
                    paths += "$familyId/${entry.name}/${file.name}"
                }
            } else {
                paths += "$familyId/${entry.name}"
            }
        }
        if (paths.isNotEmpty()) bucket.delete(paths)
    }

    companion object {
        const val BUCKET = "activity-photos"
        const val PROFILE_BUCKET = "profile-photos"
    }
}
