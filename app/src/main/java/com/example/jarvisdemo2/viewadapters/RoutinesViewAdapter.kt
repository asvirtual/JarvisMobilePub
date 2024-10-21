package com.example.jarvisdemo2.viewadapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.jarvisdemo2.R
import com.example.jarvisdemo2.activities.RoutineActivity
import com.example.jarvisdemo2.activities.RoutinesSettingsActivity
import com.example.jarvisdemo2.database.RoutineDB
import com.example.jarvisdemo2.database.RoutinesDatabaseDAO
import com.example.jarvisdemo2.utilities.Constants
import com.example.jarvisdemo2.viewmodels.RoutineViewModel
import kotlin.reflect.jvm.internal.impl.load.java.Constant

class RoutinesViewAdapter(private val routinesList: ArrayList<RoutineViewModel>) : RecyclerView.Adapter<RoutinesViewAdapter.ViewHolder>() {

    private lateinit var routineDbDAO: RoutinesDatabaseDAO

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.routine_cardview, parent, false)

        routineDbDAO = RoutineDB.getInstance(parent.context).routinesDatabaseDAO()

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val routineViewModel = routinesList[position]

        holder.routineIcon.setImageResource(routineViewModel.icon)
        holder.routineName.text = routineViewModel.name

        holder.routineLl.setOnClickListener { view ->
            val intent = Intent(view.context, RoutineActivity::class.java)
            intent.putExtra(Constants.ROUTINE_NAME, routineViewModel.name)
            view?.context?.startActivity(intent)
            (view?.context as RoutinesSettingsActivity).finish()
        }

        holder.deleteRoutineBtn.setOnClickListener {
            Constants.deleteAlarm(holder.itemView.context, routineViewModel.routine)
            routinesList.remove(routineViewModel)
            routineDbDAO.delete(routineViewModel.routine)
            notifyItemRemoved(position)
            notifyDataSetChanged()
        }

        if (!routineViewModel.routine.active) {
            holder.routineLl.setBackgroundResource(R.drawable.routine_card_background_disabled)
            holder.routineArrowIcon.setBackgroundColor(holder.itemView.context.resources.getColor(R.color.backgroundDisabled))
            holder.routineIcon.alpha = 0.5F
            holder.routineName.setTextColor(holder.itemView.context.resources.getColor(android.R.color.tab_indicator_text))
            holder.routineActions.setTextColor(holder.itemView.context.resources.getColor(android.R.color.tab_indicator_text))
        }
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return routinesList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val routineIcon: ImageView = itemView.findViewById(R.id.routineIcon)
        val routineArrowIcon: ImageView = itemView.findViewById(R.id.arrowLeftIconIv)
        val routineName: TextView = itemView.findViewById(R.id.routineName)
        val routineActions: TextView = itemView.findViewById(R.id.routineActions)
        val routineLl: LinearLayout = itemView.findViewById(R.id.routineLl)
        val deleteRoutineBtn: ImageView = itemView.findViewById(R.id.deleteRoutineBtn)
    }
}