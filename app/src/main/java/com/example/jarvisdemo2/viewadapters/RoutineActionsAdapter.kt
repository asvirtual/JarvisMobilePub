package com.example.jarvisdemo2.viewadapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.database.RoutineDB
import com.example.jarvisdemo2.database.RoutinesDatabaseDAO
import com.example.jarvisdemo2.viewmodels.RoutineActionsModel
import java.lang.Exception

class RoutineActionsAdapter(private val routinesList: ArrayList<RoutineActionsModel>) : RecyclerView.Adapter<RoutineActionsAdapter.ViewHolder>() {

    private val holders = arrayListOf<ViewHolder>()
    private lateinit var routinesDbDao: RoutinesDatabaseDAO

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        routinesDbDao = RoutineDB.getInstance(parent.context).routinesDatabaseDAO()
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.routine_action_cardview, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val routineViewModel = routinesList[position]
        holders.add(holder)

        // sets the image to the imageview from our itemHolder class
        holder.actionIcon.setImageResource(routineViewModel.icon)

        // sets the text to the textview from our itemHolder class
        holder.actionName.text = routineViewModel.name

        holder.actionIndexTv.text = (position + 1).toString()

        holder.actionDeleteIb.setOnClickListener {
            try {
                routineViewModel.routine.commands?.remove(routineViewModel.action)
                routinesDbDao.update(routineViewModel.routine)

                routinesList.remove(routineViewModel)
                holders.remove(holder)
                notifyItemRemoved(position)
                notifyDataSetChanged()

                holders.forEachIndexed { index,  viewHolder ->
                    viewHolder.actionIndexTv.text = (index + 1).toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return routinesList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val actionIcon: ImageView = itemView.findViewById(R.id.routineActionIcon)
        val actionName: TextView = itemView.findViewById(R.id.routineActionName)
        val actionIndexTv: TextView = itemView.findViewById(R.id.actionIndexTv)
        val actionDeleteIb: ImageButton = itemView.findViewById(R.id.actionDeleteIb)
    }
}