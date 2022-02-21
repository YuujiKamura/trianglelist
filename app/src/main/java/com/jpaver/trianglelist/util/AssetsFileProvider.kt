package com.jpaver.trianglelist.util

// AssetsFileProvider.kt

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.ColorSpace.match
import android.net.Uri
import java.io.FileNotFoundException
import java.io.IOException


class AssetsFileProvider : ContentProvider() {

    @Throws(FileNotFoundException::class)
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
        try {
            return context!!.assets.openFd(uri.pathSegments.joinToString("/"))
        } catch (e: IOException) {
            e.printStackTrace()
            throw FileNotFoundException(e.message)
        }
    }

    override fun onCreate(): Boolean {
        return false
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        /*
        var localSortOrder: String = p4 ?: ""
        var localSelection: String = p2 ?: ""
        val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        when (sUriMatcher.match(p0)) {
            1 -> { // If the incoming URI was for all of table3
                if (localSortOrder.isEmpty()) {
                    localSortOrder = "_ID ASC"
                }
            }
            2 -> {  // If the incoming URI was for a single row
                /*
                 * Because this URI was for a single row, the _ID value part is
                 * present. Get the last path segment from the URI; this is the _ID value.
                 * Then, append the value to the WHERE clause for the query
                 */
                localSelection += "_ID ${p0.lastPathSegment}"
            }
            else -> { // If the URI is not recognized
                // You should do some error handling here.
            }
        }
*/
        return null
        //throw UnsupportedOperationException("Not yet implemented")
        // call the code to actually do the query
    }

    override fun getType(uri: Uri): String? {
        //throw UnsupportedOperationException("Not yet implemented")
        //if (uri.path?.endsWith("pdf") == true) {
          //  return "application/pdf"
        //}
        return "application/pdf"
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        return null //throw UnsupportedOperationException("Not yet implemented")
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        return 0 //throw UnsupportedOperationException("Not yet implemented")
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0 //throw UnsupportedOperationException("Not yet implemented")
    }

    companion object {
        private const val AUTHORITY = "com.jpaver.myapplication.assets"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY")!!
    }
}