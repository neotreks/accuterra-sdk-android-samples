package com.neotreks.accuterra.mobile.sdk.sampleapp.common

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.neotreks.accuterra.mobile.sdk.sampleapp.R

/**
 * Utility class to display dialogs
 */
object DialogUtil {

    /**
     * Builds a endless progress dialog
     */
    fun buildBlockingProgressDialog(context: Context, title: String): AlertDialog {
        val progressView = View.inflate(context, R.layout.progress_dialog_view, null)
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setView(progressView)
            .setCancelable(false)
            .create()
    }

}