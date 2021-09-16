package com.example.volumeprofiler.fragments

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.MapsActivity
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.BaseSelectionObserver
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.DetailsLookup
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.KeyProvider
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.models.LocationTrigger
import com.example.volumeprofiler.util.animations.AnimUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference

class LocationsListFragment: Fragment(), ActionModeProvider<String> {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tracker: SelectionTracker<String>
    private lateinit var notificationManager: NotificationManager
    private val locationAdapter: LocationAdapter = LocationAdapter()

    /*
    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(requireContext(), "notification_channel_id")
                .setContentTitle("Example notification")
                .setSmallIcon(R.drawable.baseline_alarm_deep_purple_300_24dp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel().also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): NotificationChannel {
        return NotificationChannel(
                "notification_channel_id", "notification_channel_name", NotificationManager.IMPORTANCE_DEFAULT
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onResume() {
        super.onResume()
        notificationManager.notify(7, createNotification())
    }
     */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.locations_list_fragment, container, false)
        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            val intent: Intent = Intent(requireContext(), MapsActivity::class.java)
            startActivity(intent)
        }
        return view
    }

    private fun initRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = locationAdapter
        initSelectionTracker()
    }

    private fun initSelectionTracker(): Unit {
        tracker = SelectionTracker.Builder<String>(
                SELECTION_ID,
                recyclerView,
                KeyProvider(locationAdapter),
                DetailsLookup(recyclerView),
                StorageStrategy.createStringStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()
        tracker.addObserver(BaseSelectionObserver<String>(WeakReference(this)))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)
    }

    private inner class LocationViewHolder(private val itemView: View): RecyclerView.ViewHolder(itemView) {

        private val addressTextView: TextView = itemView.findViewById(R.id.addressTextView)
        private val profileTextView: TextView = itemView.findViewById(R.id.profileTextView)
        private val deleteGeofenceButton: Button = itemView.findViewById(R.id.deleteAlarmButton)

        fun bind(locationTrigger: LocationTrigger, isSelected: Boolean): Unit {
            AnimUtil.selectedItemAnimation(itemView, isSelected)
            addressTextView.text = locationTrigger.location.address
            profileTextView.text = locationTrigger.profile.title
            deleteGeofenceButton.setOnClickListener {

            }
        }
    }

    private inner class LocationAdapter : ListAdapter<LocationTrigger, LocationViewHolder>(object : DiffUtil.ItemCallback<LocationTrigger>() {
        override fun areItemsTheSame(oldItem: LocationTrigger, newItem: LocationTrigger): Boolean {
            return oldItem.location.id == newItem.location.id && oldItem.profile.id == newItem.profile.id
        }

        override fun areContentsTheSame(oldItem: LocationTrigger, newItem: LocationTrigger): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<String> {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            return LocationViewHolder(layoutInflater.inflate(R.layout.location_item_view, parent, false))
        }

        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            val locationTrigger: LocationTrigger = getItem(position)
            tracker.let {
                holder.bind(locationTrigger, it.isSelected(locationTrigger.location.id.toString()))
            }
        }

        override fun getItemKey(position: Int): String {
            return this.currentList[position].location.id.toString()
        }

        override fun getPosition(key: String): Int {
            return this.currentList.indexOfFirst { key == it.location.id.toString() }
        }
    }

    companion object {

        private const val SELECTION_ID: String = "LOCATION"
        private const val NOTIFICATION_ID: String = "n_id"
    }

    override fun onActionItemRemove() {

    }

    override fun getTracker(): SelectionTracker<String> {
        return tracker
    }

    override fun getFragmentActivity(): FragmentActivity {
        return requireActivity()
    }
}