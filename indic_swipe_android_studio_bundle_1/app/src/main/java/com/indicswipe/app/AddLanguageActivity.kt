package com.indicswipe.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AddLanguageActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var themeManager: ThemeManager
    private lateinit var adapter: AvailableLanguagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_language)

        settingsManager = SettingsManager(this)
        themeManager = ThemeManager(this)

        applyTheme()
        
        val recycler = findViewById<RecyclerView>(R.id.recycler_available)
        recycler.layoutManager = LinearLayoutManager(this)
        
        adapter = AvailableLanguagesAdapter(themeManager.currentTheme) { langId ->
            settingsManager.addLanguage(langId)
            finish()
        }
        recycler.adapter = adapter

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        refreshList()
    }

    private fun applyTheme() {
        val theme = themeManager.currentTheme
        

        findViewById<View>(R.id.root_layout)?.setBackgroundColor(theme.keyboardBg)
        

        window.statusBarColor = theme.keyboardBg
        window.navigationBarColor = theme.keyboardBg
        

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(theme.keyboardBg)
        toolbar.setTitleTextColor(theme.keyText)
        toolbar.navigationIcon?.setTint(theme.keyText)
        

        findViewById<TextView>(R.id.txt_section_header)?.setTextColor(theme.textSecondary)
        

        findViewById<View>(R.id.divider)?.setBackgroundColor(theme.divider)
    }

    private fun refreshList() {
        val enabledIds = settingsManager.getEnabledLanguageIds()
        val availableLangs = KeyboardConstants.LANGUAGES.filter { !enabledIds.contains(it.id) }
        adapter.submitList(availableLangs)
    }

    private class AvailableLanguagesAdapter(
        private val theme: ThemeManager.Theme,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<AvailableLanguagesAdapter.ViewHolder>() {

        private var items = emptyList<KeyboardConstants.Language>()

        fun submitList(newItems: List<KeyboardConstants.Language>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_available_language, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.txtName.text = item.displayName
            holder.txtName.setTextColor(theme.keyText)
            holder.itemView.setOnClickListener { onSelect(item.id) }
            

            val bg = GradientDrawable().apply {
                setColor(theme.specialKeyBg)
                cornerRadius = 16f * holder.itemView.resources.displayMetrics.density
            }
            holder.itemView.background = bg

            try {
                holder.viewAccent.setBackgroundColor(android.graphics.Color.parseColor(item.accentColor))
            } catch (e: Exception) {
                holder.viewAccent.setBackgroundColor(android.graphics.Color.parseColor("#FF6D00"))
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtName: TextView = view.findViewById(R.id.txt_name)
            val viewAccent: View = view.findViewById(R.id.view_accent)
        }
    }
}