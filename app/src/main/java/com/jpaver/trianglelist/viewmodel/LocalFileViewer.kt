package com.jpaver.trianglelist.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class LocalFileViewer(val context: Context) {

    fun view(saveToPrivate: (String) -> Unit, filename: String, getAppLocalFile: (Context, String) -> Uri, type: String ) {
        saveToPrivate(filename)
        val contentUri = getAppLocalFile( context, filename)
        if (contentUri == Uri.EMPTY) return

        viewDxfWithChooser( contentUri, type )
    }

    private fun viewDxfWithChooser( contentUri: Uri, type: String ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            setDataAndType(contentUri, type)
        }

        try {
            val chooserIntent = Intent.createChooser(intent, "Open with")
            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText( context, "No suitable application installed.", Toast.LENGTH_LONG ).show()
        }
    }

}