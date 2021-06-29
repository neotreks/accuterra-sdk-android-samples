package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip.components

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.sampleapp.R
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.ApkMediaVariant
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.ApkMediaVariantUtil
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.UiUtils
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ComponentImageViewBinding
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia

/**
 * View binder for the [OnlineTripMediaAdapter]
 */
class OnlineTripMediaViewBinder(
    context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
): ListItemAdapterViewBinder<TripMedia> {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripMediaVB"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val context = context.applicationContext

    private val options = UiUtils.getDefaultImageOptions()

    // First get the media service
    private val mediaService = ServiceFactory.getTripMediaService(this.context)

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun bindView(
        view: View,
        image: TripMedia,
        isSelected: Boolean,
        isFavorite: Boolean
    ) {
        // Get the UI binding
        val binding = ComponentImageViewBinding.bind(view)

        // Display The Image
        val baseUrl = image.url
        // Get Full Size or thumbnail Map Image Url
        val displayThumbnail = true // Change it here to get thumbnail or full size
        val variantUrl = if (displayThumbnail) {
            // Get thumbnail image url
            ApkMediaVariantUtil.getUrlForVariant(baseUrl, image.mediaCategoryNumber,
                ApkMediaVariant.THUMBNAIL)
        } else {
            // Get full size image url
            ApkMediaVariantUtil.getUrlForVariant(baseUrl, image.mediaCategoryNumber,
                ApkMediaVariant.DEFAULT)
        }

        // Now lets display the image for that variant URL
        lifecycleScope.launchWhenCreated {
            // Now ask the service to download the media and provide the local file uri
            val result = mediaService.getMediaFile(url = variantUrl)
            if (result.isSuccess) {
                val uri = result.value!! // Result is success so the uri value must be present
                // Display the image using the `uri`
                Glide.with(this@OnlineTripMediaViewBinder.context)
                    .applyDefaultRequestOptions(options)
                    .load(uri)
                    .into(binding.imageView)
            } else {
                // Display broken image
                Glide.with(this@OnlineTripMediaViewBinder.context)
                    .load(R.drawable.ic_broken_image_gray_24dp)
                    .into(binding.imageView)
                val message = "Error while loading online trip image: ${result.buildErrorMessage()}"
                Log.e(TAG, message, result.error)
            }
        }
    }

    override fun getViewResourceId(): Int {
        return R.layout.component_image_view
    }

}