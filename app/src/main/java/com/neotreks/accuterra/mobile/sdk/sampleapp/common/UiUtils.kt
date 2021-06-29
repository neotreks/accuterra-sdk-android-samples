package com.neotreks.accuterra.mobile.sdk.sampleapp.common

import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.neotreks.accuterra.mobile.sdk.sampleapp.R

/**
 *  UI related utility methods
 */
object UiUtils {

    /**
     * Returns default display options for Glide
     */
    fun getDefaultImageOptions(): RequestOptions {
        return RequestOptions()
            .placeholder(R.drawable.ic_image_gray_24px)
            .error(R.drawable.ic_broken_image_gray_24dp)
            .fallback(android.R.drawable.ic_dialog_alert)
            .diskCacheStrategy(DiskCacheStrategy.NONE) // No caching - AccuTerra SDK has own cache
            .skipMemoryCache(true)  // No caching - AccuTerra SDK has own cache
            .fitCenter()
    }

}