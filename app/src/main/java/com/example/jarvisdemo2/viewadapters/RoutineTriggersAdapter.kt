package com.example.jarvisdemo2.viewadapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatEditText
import androidx.recyclerview.widget.RecyclerView
import com.example.jarvisdemo2.R
import kotlinx.android.synthetic.main.voice_trigger_layout.view.*

class RoutineTriggersAdapter(private val triggersList: ArrayList<String>) : RecyclerView.Adapter<RoutineTriggersAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the voice_trigger_layout view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.voice_trigger_layout, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.triggerPhraseTv.setText(triggersList[position])
        holder.deleteVoiceTriggerBtn.setOnClickListener {
            triggersList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return triggersList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val deleteVoiceTriggerBtn: ImageView = itemView.findViewById(R.id.deleteVoiceTriggerBtn)
        val triggerPhraseTv: AppCompatEditText = itemView.findViewById(R.id.voiceTriggerEt)
    }
}