package ch.dissem.apps.abit.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ch.dissem.apps.abit.R
import ch.dissem.apps.abit.util.getColor
import ch.dissem.apps.abit.util.getIcon
import ch.dissem.apps.abit.util.getText
import ch.dissem.bitmessage.entity.valueobject.Label
import com.mikepenz.iconics.view.IconicsImageView
import org.jetbrains.anko.backgroundColor

class LabelAdapter internal constructor(private val ctx: Context, labels: Collection<Label>) :
    RecyclerView.Adapter<LabelAdapter.ViewHolder>() {

    var labels = labels.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelAdapter.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val contactView = inflater.inflate(R.layout.item_label, parent, false)

        // Return a new holder instance
        return ViewHolder(contactView)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: LabelAdapter.ViewHolder, position: Int) {
        // Get the data model based on position
        val label = labels[position]

        viewHolder.icon.icon?.icon(label.getIcon())
        viewHolder.label.text = label.getText(ctx)
        viewHolder.setBackground(label.getColor(0xFF607D8B.toInt()))
    }

    override fun getItemCount() = labels.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val background = itemView
        var icon = itemView.findViewById<IconicsImageView>(R.id.icon)!!
        var label = itemView.findViewById<TextView>(R.id.label)!!

        fun setBackground(@ColorInt color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                background.backgroundTintList = ColorStateList.valueOf(color)
            } else {
                background.backgroundColor = color
            }
        }
    }
}
