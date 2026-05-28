package com.indicswipe.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var themeManager: ThemeManager
    private lateinit var adapter: EnabledLanguagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        settingsManager = SettingsManager(this)
        themeManager = ThemeManager(this)
        
        applyTheme()

        val recycler = findViewById<RecyclerView>(R.id.recycler_languages)
        recycler.layoutManager = LinearLayoutManager(this)
        
        adapter = EnabledLanguagesAdapter(themeManager.currentTheme) { langId ->
            settingsManager.removeLanguage(langId)
            refreshList()
        }
        recycler.adapter = adapter

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_keyboard).setOnClickListener {
            startActivity(Intent(this, AddLanguageActivity::class.java))
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun applyTheme() {
        val theme = themeManager.currentTheme
        val rootView = findViewById<View>(android.R.id.content).rootView
        

        findViewById<View>(R.id.root_layout)?.setBackgroundColor(theme.keyboardBg)
        

        window.statusBarColor = theme.keyboardBg
        window.navigationBarColor = theme.keyboardBg
        

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(theme.keyboardBg)
        toolbar.setTitleTextColor(theme.keyText)
        toolbar.navigationIcon?.setTint(theme.keyText)
        

        findViewById<TextView>(R.id.txt_section_header)?.setTextColor(theme.textSecondary)
        

        val btnAdd = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_keyboard)
        btnAdd.setBackgroundColor(theme.accent)
        btnAdd.setTextColor(theme.accentText)
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val enabledIds = settingsManager.getEnabledLanguageIds()
        val enabledLangs = KeyboardConstants.LANGUAGES.filter { enabledIds.contains(it.id) }
        adapter.submitList(enabledLangs)
    }

    private class EnabledLanguagesAdapter(
        private val theme: ThemeManager.Theme,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<EnabledLanguagesAdapter.ViewHolder>() {

        private var items = emptyList<KeyboardConstants.Language>()

        fun submitList(newItems: List<KeyboardConstants.Language>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_enabled_language, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.txtName.text = item.displayName
            holder.txtName.setTextColor(theme.keyText)
            holder.btnDelete.setOnClickListener { onDelete(item.id) }
            holder.btnDelete.setColorFilter(theme.textSecondary)
            

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


            holder.btnDelete.visibility = if (items.size > 1) View.VISIBLE else View.INVISIBLE
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtName: TextView = view.findViewById(R.id.txt_name)
            val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
            val viewAccent: View = view.findViewById(R.id.view_accent)
        }
    }
}