package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.sdk.sampleapp.BaseActivity
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.DialogUtil
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityOnlineTripListBinding
import com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip.components.OnlineTripListAdapter
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

        loadTrips()
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

        // Add livedata observer
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
            binding.activityTripListListView.adapter = adapter
        }
    }

}