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
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityOnlineTripListItemBinding
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMapImage
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry

/**
 * View binder for the [OnlineTripListAdapter]
 */
class OnlineTripListItemViewBinder(
    context: Context,
    private val lifecycleScope: LifecycleCoroutineScope): ListItemAdapterViewBinder<ActivityFeedEntry> {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripListItemVB"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val context = context.applicationContext

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun bindView(
        view: View,
        item: ActivityFeedEntry,
        isSelected: Boolean,
        isFavorite: Boolean
    ) {
        val binding = ActivityOnlineTripListItemBinding.bind(view)

        // Bind Name, Description, Status
        val tripBasicInfo = item.trip
        binding.activityOnlineTripListItemTripName.text = tripBasicInfo.tripName
        binding.activityOnlineTripListItemTripDescription.text = tripBasicInfo.description
        binding.activityOnlineTripListItemTripStatus.text = "Status: ${tripBasicInfo.processingStatus}"

        // Display Trip Map Image

        // First get the media service
        val mediaService = ServiceFactory.getTripMediaService(context)
        // Get The Map
        val image: TripMapImage = tripBasicInfo.mapImage
            ?: return
        val baseUrl = image.url
        // Get Full Size or thumbnail Map Image Url
        val displayThumbnail = true
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
            val options = UiUtils.getDefaultImageOptions()
            if (result.isSuccess) {
                val uri = result.value!! // Result is success so the uri value must be present
                // Display the image using the `uri`
                Glide.with(context)
                    .applyDefaultRequestOptions(options)
                    .load(uri)
                    .into(binding.activityOnlineTripListItemMap)
            } else {
                // Display broken image
                Glide.with(context)
                    .load(R.drawable.ic_broken_image_gray_24dp)
                    .into(binding.activityOnlineTripListItemMap)
                Log.e(TAG, "Error while loading online trip map image: ${result.buildErrorMessage()}")
            }
        }
    }

    override fun getViewResourceId(): Int {
        return R.layout.activity_online_trip_list_item
    }

}