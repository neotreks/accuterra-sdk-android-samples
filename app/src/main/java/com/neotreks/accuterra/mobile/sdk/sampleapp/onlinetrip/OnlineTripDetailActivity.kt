package com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.sdk.sampleapp.BaseActivity
import com.neotreks.accuterra.mobile.sdk.sampleapp.common.DialogUtil
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityOnlineTripDetailBinding
import com.neotreks.accuterra.mobile.sdk.sampleapp.onlinetrip.components.OnlineTripMediaAdapter
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripBasicInfoEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripProcessingStatus

/**
 * Example of displaying Online Trip Detail
 */
class OnlineTripDetailActivity : BaseActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripDetailAct"

        private const val KEY_TRIP_UUID = "TRIP_UUID"

        fun createNavigateToIntent(context: Context, tripUuid: String? = null): Intent {
            return Intent(context, OnlineTripDetailActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_UUID, tripUuid)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: OnlineTripDetailViewModel by viewModels()

    private lateinit var binding: ActivityOnlineTripDetailBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOnlineTripDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()

        parseIntent()

        // Add observer
        setupObserver()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun parseIntent() {

        val tripUuid = intent.getStringExtra(KEY_TRIP_UUID)
        Log.d(TAG, "Trip UUID: $tripUuid")

        if (tripUuid.isNullOrBlank()) {
            loadFirstProcessedTrip()
        } else {
            loadTripByUuid(tripUuid)
        }

    }

    private fun loadTripByUuid(tripUuid: String) {
        // Show the progress dialog
        val loadDialog = DialogUtil.buildBlockingProgressDialog(this, "Loading Trip By UUID")
        loadDialog.show()

        lifecycleScope.launchWhenCreated {
            // Load trip of particular UUID
            viewModel.loadTripDetail(this@OnlineTripDetailActivity, tripUuid)
            // Hide the progress dialog
            loadDialog.dismiss()
        }
    }

    private fun loadFirstProcessedTrip() {
        // Show the progress dialog
        val loadDialog = DialogUtil.buildBlockingProgressDialog(this, "Loading First Processed Trip")
        loadDialog.show()

        // Load the trip detail from the BE server
        lifecycleScope.launchWhenCreated {
            // Get the list of user trip entries from the BE server
            viewModel.loadTripEntries(this@OnlineTripDetailActivity)
            // Filer just "Processed" trip entries.
            // This is because we might not be able to get the full detail trip object
            // for trips which were not fully processed yet on the server side!
            val processedTripEntries = viewModel.feedEntries.filter {
                it.trip.processingStatus == TripProcessingStatus.PROCESSED
            }
            // Check if there is at least one trip and load its detail
            val tripBasicInfo: TripBasicInfoEntry? = processedTripEntries.firstOrNull()?.trip
            if (tripBasicInfo == null) {
                loadDialog.dismiss()
                Toast.makeText(
                    this@OnlineTripDetailActivity,
                    "There is no processed trip available for that query",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                viewModel.loadTripDetail(this@OnlineTripDetailActivity, tripBasicInfo.uuid)
                loadDialog.dismiss()
            }
        }
    }

    private fun setupObserver() {
        viewModel.trip.observe(this) { trip ->
            if (trip == null) {
                return@observe
            }
            // Display Trip Name
            binding.activityOnlineTripDetailTripName.text = trip.info.name
            // Display Trip Tags
            binding.activityOnlineTripDetailTripDescription.text = trip.info.description
            // Display TRIP root media
            val mediaOfTheTrip = trip.media // Trips might not have media set to Trip.media property
            val tripMediaAdapter = OnlineTripMediaAdapter(this, mediaOfTheTrip, lifecycleScope)
            binding.activityOnlineTripDetailTripMediaList.adapter = tripMediaAdapter
            // Display POI media
            val tripPoints = trip.navigation.points // Points usually have some media attached
            val mediaOfAllPoints = tripPoints.flatMap { it.media }
            val poiAdapter = OnlineTripMediaAdapter(this, mediaOfAllPoints, lifecycleScope)
            binding.activityOnlineTripDetailPoiMediaList.adapter = poiAdapter
        }
    }

}