package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.PagedSearchCriteria
import com.neotreks.accuterra.mobile.sdk.trip.model.Trip
import com.neotreks.accuterra.mobile.sdk.trip.model.TripInfo
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripNavigation
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetMyActivityFeedCriteria

/**
 * View-model for the [OnlineTripDetailActivity]
 */
class OnlineTripDetailViewModel : ViewModel() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripDetailVM"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    /**
     * List of user activity feed entries
     */
    var feedEntries = listOf<ActivityFeedEntry>()

    /**
     * LiveData of the [Trip] loaded from the BE
     */
    val trip = MutableLiveData<Trip?>()

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    suspend fun loadTripEntries(context: Context) {

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

        // Check the query result - whether it succeeded or failed
        if (entriesQueryResult.isSuccess) {
            // Query has succeeded
            val resultData = entriesQueryResult.value!! // Query succeeded so data must be present
            feedEntries = resultData.entries // Activity Feed Entries returned by the server
        } else {
            // Query has failed
            val message = "Error while listing user trips: ${entriesQueryResult.buildErrorMessage()}"
            Log.e(
                TAG, message,
                entriesQueryResult.error)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

    }

    suspend fun loadTripDetail(context: AppCompatActivity, tripUuid: String) {
        // Init the trip service
        val tripService = ServiceFactory.getTripService(context)

        // Query full trip object from the server
        val tripQueryResult = tripService.getTrip(tripUuid)

        val trip: Trip

        // We have to check if the query succeeded or failed
        if (tripQueryResult.isSuccess) {
            // Query has succeeded
            trip = tripQueryResult.value!! // Query succeeded so data must be present
        } else {
            // Query has failed - log and display the error message
            val message = "Error while getting trip detail for $tripUuid: ${tripQueryResult.errorMessage}"
            Log.e(TAG, message, tripQueryResult.error)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            return
        }

        // Now we can get Trip detail data

        val tripInfo: TripInfo = trip.info // Name, description, tags, etc.
        val navigationInfo: TripNavigation = trip.navigation // Trip Points (POIs), Path (geometry), Trip Map Image
        val statistics: TripStatistics = trip.statistics // Length, driving time, cumulative ascent/descent, etc.
        val tripMedia: List<TripMedia> = trip.media // Media attached to the `root trip` object
        val pointMedia: List<TripMedia> = trip.navigation.points.flatMap { it.media } // Media attached to trip POIs
        // Check the view binder to see how trip media are displayed

        // Let's set the trip detail into livedata - its observer will display the trip content
        this.trip.value = trip

    }

}