package com.obsidianbackup.tv.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.leanback.widget.Presenter
import com.obsidianbackup.tv.R

class DashboardCardPresenter : Presenter() {
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_dashboard, parent, false) as CardView
        
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val card = item as MainFragment.DashboardCard
        val cardView = viewHolder.view as CardView
        
        val titleView = cardView.findViewById<TextView>(R.id.card_title)
        val descView = cardView.findViewById<TextView>(R.id.card_description)
        val iconView = cardView.findViewById<ImageView>(R.id.card_icon)
        
        titleView.text = card.title
        descView.text = card.description
        iconView.setImageResource(card.iconRes)
        
        cardView.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.1f else 1.0f
            cardView.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .start()
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        // Cleanup if needed
    }
}

class TVAppCardPresenter : Presenter() {
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_tv_app, parent, false) as CardView
        
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val app = item as MainFragment.TVAppCard
        val cardView = viewHolder.view as CardView
        
        val nameView = cardView.findViewById<TextView>(R.id.app_name)
        val sizeView = cardView.findViewById<TextView>(R.id.app_size)
        val iconView = cardView.findViewById<ImageView>(R.id.app_icon)
        val statusView = cardView.findViewById<TextView>(R.id.backup_status)
        
        nameView.text = app.name
        sizeView.text = formatSize(app.size)
        
        if (app.icon != null) {
            iconView.setImageDrawable(app.icon)
        } else {
            iconView.setImageResource(R.drawable.ic_app_default)
        }
        
        statusView.text = if (app.isBackedUp) "✓ Backed up" else "Not backed up"
        statusView.setTextColor(
            if (app.isBackedUp) Color.GREEN else Color.GRAY
        )
        
        cardView.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.1f else 1.0f
            cardView.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .start()
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        // Cleanup if needed
    }
    
    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}

class SettingsCardPresenter : Presenter() {
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_settings, parent, false) as CardView
        
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val setting = item as MainFragment.SettingsItem
        val cardView = viewHolder.view as CardView
        
        val titleView = cardView.findViewById<TextView>(R.id.setting_title)
        val descView = cardView.findViewById<TextView>(R.id.setting_description)
        val iconView = cardView.findViewById<ImageView>(R.id.setting_icon)
        
        titleView.text = setting.title
        descView.text = setting.description
        iconView.setImageResource(setting.iconRes)
        
        cardView.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.1f else 1.0f
            cardView.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .start()
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        // Cleanup if needed
    }
}
