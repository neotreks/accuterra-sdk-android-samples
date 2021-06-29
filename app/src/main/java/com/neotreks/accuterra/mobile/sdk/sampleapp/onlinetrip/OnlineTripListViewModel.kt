package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.PagedSearchCriteria
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetMyActivityFeedCriteria

/**
 * View-model for [OnlineTripListActivity]
 */
class OnlineTripListViewModel: ViewModel() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TripListViewModel"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    /**
     * LiveData for list of user activity feed entries
     */
    val feedEntries = MutableLiveData<List<ActivityFeedEntry>>()

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    suspend fun loadUserTrips(context: Context) {
        // Init the trip service
        val tripService = ServiceFactory.getTripService(context)

        // Prepare search criteria
        val searchCriteria = GetMyActivityFeedCriteria(
            // Do not use to big page size. The server might consider returning less than requested.
            pagingCriteria = PagedSearchCriteria(pageNumber = 0, pageSize = 10)
        )

        // Query list of user's Activity Feed Entries - lightweight `trip` objects
        // This performs query to the server
        // Be aware that the returned list depends on `user id` provided in the `IIdentityProvider`
        // that is passed into the `SdkManger.initSdk()` method
        val entriesQueryResult = tripService.getMyActivityFeed(criteria = searchCriteria)

        val trips: List<ActivityFeedEntry>
        val totalNumberOfTrips: Int

        // Check the query result - whether it succeeded or failed
        if (entriesQueryResult.isSuccess) {
            // Query has succeeded
            val resultData = entriesQueryResult.value!! // Query succeeded so data must be present
            trips = resultData.entries // Activity Feed Entries returned by the server
            totalNumberOfTrips = resultData.paging.total // Paging to see total number of user trips
            Log.i(TAG, "Number of returned trips: ${trips.size}")
            Log.i(TAG, "Total number of user trips: $totalNumberOfTrips")
            feedEntries.value = resultData.entries
        } else {
            // Query has failed - log and display the error message
            val message = "Error while listing user trips: ${entriesQueryResult.buildErrorMessage()}"
            Log.e(TAG, message, entriesQueryResult.error)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

    }

}