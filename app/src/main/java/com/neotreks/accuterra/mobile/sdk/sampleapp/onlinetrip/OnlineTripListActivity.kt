package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.sdk.sampleapp.BaseActivity
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.DialogUtil
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.OnListItemClickedListener
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityOnlineTripListBinding
import com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip.components.OnlineTripListAdapter
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripBasicInfoEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripProcessingStatus

/**
 * Activity displaying list of trips
 */
class OnlineTripListActivity : BaseActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripListActivity"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityOnlineTripListBinding

    private val viewModel: OnlineTripListViewModel by viewModels()

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineTripListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()

        // Load trip list from the BE
        loadTrips()

        // Add livedata observer
        setupObserver()
    }

    private fun loadTrips() {

        // Show the progress dialog
        val loadDialog = DialogUtil.buildBlockingProgressDialog(this, "Loading User Trips")
        loadDialog.show()

        // Load list of user trips from the BE
        lifecycleScope.launchWhenCreated {
            Log.d(TAG, "Start loading user trips")
            viewModel.loadUserTrips(this@OnlineTripListActivity)
            Log.d(TAG, "End of loading user trips")
            // Hide the progress dialog
            loadDialog.dismiss()
        }

    }

    private fun setupObserver() {
        viewModel.feedEntries.observe(this) { activityEntries ->
            // We might consider filtering trips by status - it depends if we want to display
            // also trips which were uploaded to the server but not fully processed yet.
            // We might not be able to get the full detail object for unprocessed trips though.
            val displayJustProcessed = false
            val displayedItems = if (displayJustProcessed) {
                activityEntries.filter { it.trip.processingStatus == TripProcessingStatus.PROCESSED }
            } else {
                activityEntries
            }
            // Crate the adapter
            val adapter = OnlineTripListAdapter(this, displayedItems, lifecycleScope)
            adapter.setOnListItemClickedListener(object: OnListItemClickedListener<ActivityFeedEntry> {
                override fun onListItemClicked(item: ActivityFeedEntry, view: View) {
                    displayTripDetail(item.trip)
                }
            })
            binding.activityTripListListView.adapter = adapter
        }
    }

    /**
     * Opens the [OnlineTripDetailActivity] if trip detail can be displayed
     */
    private fun displayTripDetail(trip: TripBasicInfoEntry) {
        if (trip.processingStatus == TripProcessingStatus.PROCESSED) {
            // We can get detail only for Processed Trips
            val intent = OnlineTripDetailActivity.createNavigateToIntent(this, trip.uuid)
            startActivity(intent)
        } else {
            Toast.makeText(this,
                "We shall display details only for processed trips",
                Toast.LENGTH_LONG
            ).show()
        }
    }

}