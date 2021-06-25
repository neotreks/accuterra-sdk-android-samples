package com.neotreks.accuterra.mobile.sdk.sampleapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.mapboxsdk.Mapbox
import com.neotreks.accuterra.mobile.sdk.*
import com.neotreks.accuterra.mobile.sdk.cache.model.OfflineCacheConfig
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.sampleapp.databinding.ActivityMainBinding
import com.neotreks.accuterra.mobile.sdk.security.model.SdkEndpointConfig
import java.util.*

class MainActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "MainActivity"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityMainBinding

            /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Mapbox.getInstance(this, BuildConfig.MAPBOX_CLIENT_TOKEN)

        lifecycleScope.launchWhenCreated {
            if (initSdk(this@MainActivity).isSuccess) {
                showSampleList()
            }
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun showSampleList() {
        Log.i(TAG, "Show sample list")
        // Show the list
        binding.activityMainExampleList.visibility = View.VISIBLE
        // Setup Listeners
        binding.activityMainSampleTrailHeads.setOnClickListener { onTrailHeads() }
        binding.activityMainSampleTrailWaypoints.setOnClickListener { onTrailWaypoints() }
    }

    private suspend fun initSdk(activity: Activity): Result<Boolean> {

        val sdkConfig = ApkSdkConfig(
            sdkEndpointConfig = SdkEndpointConfig(
                wsBaseUrl = BuildConfig.WS_BASE_URL,
                wsAuthUrl = BuildConfig.WS_AUTH_URL,
            ),
            // Request to NOT initialize the overlay map download during SDK initialization
            offlineCacheConfig = OfflineCacheConfig(
                downloadOverlayMap = false,
            ),
        )

        val optionalListener = object : SdkInitListener {

            override fun onProgressChanged(progress: Float) {
                lifecycleScope.launchWhenCreated {
                    binding.activityMainProgressBar.progress = (progress * 100.0).toInt()
                }
            }

            override fun onStateChanged(state: SdkInitState, detail: SdkInitStateDetail?) {
                when (state) {
                    SdkInitState.IN_PROGRESS -> {
                        displayProgressBar(detail)
                    }
                    SdkInitState.WAITING,
                    SdkInitState.WAITING_FOR_NETWORK,
                    SdkInitState.PAUSED -> alertSdkInitStateChanged(activity, state)
                    SdkInitState.COMPLETED -> {
                        toast(activity, "SDK initialization completed successfully")
                        hideProgressBar()
                    }
                    SdkInitState.CANCELED,
                    SdkInitState.FAILED -> {
                        alertSdkInitCeased(activity, state)
                        hideProgressBar()
                    }
                    SdkInitState.UNKNOWN -> throw IllegalStateException()
                }
            }
        }

        val accessProvider: IAccessProvider = SampleAccessProvider()
        val identityProvider: IIdentityProvider = SampleIdentityProvider()

        return SdkManager.initSdk(
            context = activity,
            config = sdkConfig,
            accessProvider = accessProvider,
            identityProvider = identityProvider,
            listener = optionalListener
        )
    }

    private fun hideProgressBar() {
        lifecycleScope.launchWhenCreated {
            binding.activityMainProgressBar.visibility = View.GONE
            binding.activityMainProgressBarText.visibility = View.GONE
        }
    }

    private fun displayProgressBar(detail: SdkInitStateDetail?) {
        lifecycleScope.launchWhenCreated {
            binding.activityMainProgressBar.visibility = View.VISIBLE
            binding.activityMainProgressBarText.visibility = View.VISIBLE
            binding.activityMainProgressBarText.text = detail?.name
        }
    }

    private fun alertSdkInitStateChanged(activity: Activity, state: SdkInitState) {
        lifecycleScope.launchWhenCreated {
            AlertDialog.Builder(activity)
                .setTitle("SDK Initialization")
                .setMessage("SDK initialization ${state.name.lowercase(Locale.getDefault())}")
                .setPositiveButton("Ok") { _, _ -> }
                .show()
        }
    }

    private fun toast(activity: Activity, message: String) {
        lifecycleScope.launchWhenCreated {
            Toast.makeText(
                activity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun alertSdkInitCeased(activity: Activity, state: SdkInitState) {
        lifecycleScope.launchWhenCreated {
            AlertDialog.Builder(activity)
                .setTitle("SDK Initialization")
                .setMessage("SDK initialization ${state.name.lowercase(Locale.getDefault())}")
                .setPositiveButton("Retry") { _, _ ->
                    lifecycleScope.launchWhenCreated {
                        initSdk(activity)
                    }
                }
                .setNegativeButton("Quit") { _, _ ->
                    finish()
                }
                .show()
        }
    }

    /* * * * * * * * * * * * */
    /*    BUTTON LISTENERS   */
    /* * * * * * * * * * * * */

    private fun onTrailHeads() {
        startActivity(Intent(this, TrailHeadsActivity::class.java))
    }

    private fun onTrailWaypoints() {
        startActivity(Intent(this, TrailWaypointsActivity::class.java))
    }

}
