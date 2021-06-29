package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip.components

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.sampleapp.extensions.uuidToLong
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry

/**
 * Adapter of user trips
 */
class OnlineTripListAdapter(
    context: Context,
    items: List<ActivityFeedEntry>,
    lifecycleScope: LifecycleCoroutineScope,
) : ListItemAdapter<ActivityFeedEntry>(
    context = context,
    items = items,
    viewBinder = OnlineTripListItemViewBinder(
        context, lifecycleScope
    )
) {

    override fun getItemId(item: ActivityFeedEntry): Long {
        return item.trip.uuid.uuidToLong()
    }

}