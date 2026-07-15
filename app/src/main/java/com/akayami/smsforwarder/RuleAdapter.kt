package com.akayami.smsforwarder

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RuleAdapter(
    private var rules: MutableList<ForwardingRule>,
    private val onDeleteClick: (ForwardingRule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

    class RuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textSource: TextView = view.findViewById(R.id.textSource)
        val textDestinations: TextView = view.findViewById(R.id.textDestinations)
        val buttonDelete: Button = view.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        holder.textSource.text = "Source: ${rule.sourcePattern}"
        holder.textDestinations.text = "Destinations: ${rule.destinationNumbers.joinToString(", ")}"
        holder.buttonDelete.setOnClickListener {
            onDeleteClick(rule)
        }
    }

    override fun getItemCount() = rules.size
    
    @SuppressLint("NotifyDataSetChanged")
    fun updateRules(newRules: List<ForwardingRule>) {
        rules.clear()
        rules.addAll(newRules)
        notifyDataSetChanged()
    }
}
