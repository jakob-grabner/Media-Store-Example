package com.example.mediastore

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        saveButton.setOnClickListener {
            save()
        }

    }

    fun save() {
        lifecycleScope.launch {
            val bitmap = loadBitmap(R.drawable.android)
            saveImageToGallery(this@MainActivity, bitmap, "img_${System.currentTimeMillis()}.jpg")?.let {
                printMediaStoreEntry(it)
            }
        }
    }


    suspend fun loadBitmap(@DrawableRes resId: Int): Bitmap = withContext(Dispatchers.IO) {
        BitmapFactory.decodeResource(resources, resId)
    }


    private suspend fun saveImageToGallery(
            context: Context,
            bitmap: Bitmap,
            imageName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.DESCRIPTION, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            // Insert file into MediaStore
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val galleryFileUri = contentResolver.insert(collection, values)
                    ?: return@withContext null

            // Save file to uri from MediaStore
            contentResolver.openOutputStream(galleryFileUri).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
            }

            // Now that we're finished, release the "pending" status, and allow other apps to view the image.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(galleryFileUri, values, null, null)
            }
            return@withContext galleryFileUri
        } catch (ex: Exception) {
            Log.e("MSTEST", "Saving progress pic to gallery failed", ex)
            return@withContext null
        }
    }


    suspend fun printMediaStoreEntry(imageUri: Uri) = withContext(Dispatchers.IO) {

        contentResolver.query(
                imageUri,
                null,
                null,
                null,
                null
        ).use { cursor ->
            val imgInfo = DatabaseUtils.dumpCursorToString(cursor)
            Log.d("MSTEST", imgInfo)
        }
    }

}
