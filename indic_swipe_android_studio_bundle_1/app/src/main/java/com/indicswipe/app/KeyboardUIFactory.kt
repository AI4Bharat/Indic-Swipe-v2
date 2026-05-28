package com.indicswipe.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.ColorUtils

object KeyboardUIFactory {

    fun dp(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.resources.displayMetrics
        ).toInt()

    fun dp(context: Context, dp: Int): Int =
        dp(context, dp.toFloat())

    private fun sp(context: Context, sp: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp,
            context.resources.displayMetrics
        )





    
    fun createThemedKeyBg(
        context: Context,
        theme: ThemeManager.Theme,
        borders: Boolean,
        isAction: Boolean = false,
        isEnter: Boolean = false
    ): Drawable {
        val radius = dp(context, if (isAction || isEnter) theme.specialKeyRadius else theme.keyRadius).toFloat()
        val shadowDy = dp(context, theme.keyShadowDy)
        

        val shadow = GradientDrawable().apply {
            cornerRadius = radius
            setColor(theme.keyShadowColor)
        }
        

        val face = GradientDrawable().apply {
            cornerRadius = radius
            val startColor = when {
                isEnter -> theme.enterKeyBg
                isAction -> theme.specialKeyGradientStart
                else -> theme.keyGradientStart
            }
            val endColor = when {
                isEnter -> theme.enterKeyBg
                isAction -> theme.specialKeyGradientEnd
                else -> theme.keyGradientEnd
            }
            colors = intArrayOf(startColor, endColor)
            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            

            if (Color.alpha(theme.keyBorder) > 0) {
                setStroke(dp(context, 1f), theme.keyBorder)
            } else if (borders) {
                setStroke(dp(context, 1.5f), theme.divider)
            }
        }
        

        val rippleColor = theme.keyPressed 
        
        val layers = arrayOf(shadow, face)
        val ld = LayerDrawable(layers)
        

        ld.setLayerInset(1, 0, 0, 0, shadowDy)
        

        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(rippleColor),
            ld,
            face
        )
    }

    fun createLanguageSpaceBarBg(
        context: Context,
        theme: ThemeManager.Theme,
        borders: Boolean,
        languageColor: Int
    ): Drawable {
        val radius = dp(context, theme.keyRadius).toFloat()
        

        val face = GradientDrawable().apply {
            cornerRadius = radius
            colors = intArrayOf(theme.keyGradientStart, theme.keyGradientEnd)
            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke(dp(context, 2f), languageColor)
        }
        

        val shadow = GradientDrawable().apply {
            cornerRadius = radius
            setColor(theme.keyShadowColor)
        }
        
        val layers = arrayOf(shadow, face)
        val ld = LayerDrawable(layers)
        ld.setLayerInset(1, 0, 0, 0, dp(context, 3f))
        
        return ld
    }

    fun createFlashBg(context: Context, color: Int, radius: Float): Drawable {
        return GradientDrawable().apply {
            cornerRadius = radius
            setColor(color)
        }
    }





    fun createSymbolKey(
        context: Context,
        symbol: String,
        theme: ThemeManager.Theme?,
        borders: Boolean,
        isAction: Boolean = false,
        onClick: (String) -> Unit
    ): View {
        val t = theme ?: return View(context)
        val keyMargin = context.resources.getDimensionPixelSize(R.dimen.key_margin)
        val keyHeight = context.resources.getDimensionPixelSize(R.dimen.key_height)

        return TextView(context).apply {
            text = symbol
            textSize = if (isAction) 16f else 26f
            if (!isAction) typeface = Typeface.DEFAULT_BOLD
            setTextColor(t.keyText)
            gravity = Gravity.CENTER
            

            val hPadding = dp(context, 12)
            setPadding(hPadding, 0, hPadding, 0)
            
            val bgId = if (isAction) R.drawable.key_bg_bottom_special else R.drawable.key_bg_letter
            background = ContextCompat.getDrawable(context, bgId)?.mutate()?.also {
                if (isAction) DrawableCompat.setTint(it, t.specialKeyBg)
                else DrawableCompat.setTint(it, t.keyBg)
            }

            layoutParams = LinearLayout.LayoutParams(0, keyHeight, 1f).apply {
                setMargins(keyMargin, keyMargin, keyMargin, keyMargin)
            }
            setOnClickListener { onClick(symbol) }
            
            setOnTouchListener { v, _ ->

                false
            }
        }
    }
    
    fun createIconButton(
        context: Context,
        iconRes: Int,
        theme: ThemeManager.Theme?,
        isAction: Boolean = true,
        onClick: () -> Unit
    ): View {
        val t = theme ?: return View(context)
        val keyMargin = context.resources.getDimensionPixelSize(R.dimen.key_margin)
        val keyHeight = context.resources.getDimensionPixelSize(R.dimen.key_height)

        return FrameLayout(context).apply {
            val bgId = if (isAction) R.drawable.key_bg_bottom_special else R.drawable.key_bg_letter
            background = ContextCompat.getDrawable(context, bgId)?.mutate()?.also {
                if (isAction) DrawableCompat.setTint(it, t.specialKeyBg)
            }
            
            addView(ImageView(context).apply {
                setImageResource(iconRes)
                DrawableCompat.setTint(this.drawable, t.specialKeyIcon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val padding = dp(context, 14)
                setPadding(padding, padding, padding, padding)
            })

            layoutParams = LinearLayout.LayoutParams(0, keyHeight, 1.5f).apply {
                setMargins(keyMargin, keyMargin, keyMargin, keyMargin)
            }
            setOnClickListener { onClick() }
        }
    }





    fun createSuggestionChip(
        context: Context,
        text: String,
        isPrimary: Boolean,
        isEnglish: Boolean,
        theme: ThemeManager.Theme?,
        overrideAccent: Int? = null,
        onClick: (String) -> Unit
    ): View {
        val t = theme ?: return View(context)
        val accent = overrideAccent ?: t.accent
        
        val chipBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, t.suggestionChipRadius).toFloat() 
            if (isPrimary) {
                setColor(accent) 
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(dp(context, 1f), ColorUtils.setAlphaComponent(t.textSecondary, 50))
            }
        }

        return TextView(context).apply {
            this.text = text
            textSize = if (isPrimary) 17f else 15f
            paint.isFakeBoldText = isPrimary
            setTextColor(
                when {
                    isPrimary -> {
                        val luminance = ColorUtils.calculateLuminance(accent)
                        if (luminance > 0.6) Color.parseColor("#2D2D2D") else Color.WHITE
                    }
                    isEnglish -> t.textSecondary
                    else -> t.keyText
                }
            )
            gravity = Gravity.CENTER
            background = chipBg
            
            val ph = dp(context, t.suggestionChipPaddingH.toFloat())
            val pv = dp(context, t.suggestionChipPaddingV.toFloat())
            setPadding(ph, pv, ph, pv)
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                setMargins(dp(context, 2), 0, dp(context, 2), 0)
            }
            
            setOnClickListener { onClick(text) }
        }
    }

    fun createDivider(context: Context, theme: ThemeManager.Theme?): View {
        val t = theme ?: return View(context)
        return View(context).apply {
            setBackgroundColor(t.divider)
            layoutParams = LinearLayout.LayoutParams(dp(context, 1), dp(context, 28)).apply {
                gravity = Gravity.CENTER_VERTICAL
                setMargins(dp(context, 2), 0, dp(context, 2), 0)
            }
        }
    }





    fun createEmojiTab(
        context: Context,
        icon: String,
        isSelected: Boolean,
        theme: ThemeManager.Theme?,
        accentColor: Int? = null,
        onClick: () -> Unit
    ): View {
        val t = theme ?: return View(context)
        val accent = accentColor ?: t.accent
        return TextView(context).apply {
            text = icon

            textSize = if (icon.length > 2) 15f else 22f
            gravity = Gravity.CENTER
            
            val hPadding = dp(context, 14)
            val vPadding = dp(context, 6)
            setPadding(hPadding, vPadding, hPadding, vPadding)
            
            val bg = GradientDrawable().apply {
                cornerRadius = dp(context, t.specialKeyRadius).toFloat()
                if (isSelected) {

                    setColor(ColorUtils.setAlphaComponent(accent, 40))
                } else {
                    setColor(Color.TRANSPARENT)
                }
            }
            background = bg
            
            if (isSelected) {
                setTextColor(accent)
                alpha = 1.0f
                paint.isFakeBoldText = true
            } else {
                setTextColor(t.keyText)
                alpha = 0.6f
                paint.isFakeBoldText = false
            }
            
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(context, 36)
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                setMargins(dp(context, 4), 0, dp(context, 4), 0)
            }
            
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    
    fun styleEmojiNavButton(
        view: View,
        theme: ThemeManager.Theme,
        borders: Boolean,
        isIcon: Boolean = false
    ) {

        val radius = theme.specialKeyRadius
        
        view.background = createThemedKeyBg(view.context, theme, borders, isAction = true)
        
        if (view is TextView) {
            view.setTextColor(theme.specialKeyText)
            view.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            view.textSize = 14f
        }
        
        if (view is ImageView) {
            DrawableCompat.setTint(view.drawable ?: return, theme.specialKeyIcon)
        }
    }





    fun createPunctuationKey(
        context: Context,
        symbol: String,
        theme: ThemeManager.Theme?,
        onClick: (String) -> Unit
    ): View {
        val t = theme ?: return View(context)
        return TextView(context).apply {
            text = symbol
            textSize = 22f
            setTextColor(t.keyText)
            gravity = Gravity.CENTER
            background = createThemedKeyBg(context, t, false)
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))
            }
            setOnClickListener { onClick(symbol) }
        }
    }





    fun createThemeCard(
        context: Context,
        theme: ThemeManager.Theme,
        activeTheme: ThemeManager.Theme,
        isSelected: Boolean,
        onClick: () -> Unit
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            if (isSelected) {
                setBackgroundColor(activeTheme.keyPressed)
            }

            addView(View(context).apply {
                val bg = GradientDrawable()
                bg.setColor(theme.keyBg)
                bg.cornerRadius = dp(context, theme.keyRadius).toFloat()
                bg.setStroke(2, theme.accent)
                background = bg
                layoutParams = LinearLayout.LayoutParams(dp(context, 24), dp(context, 24)).apply {
                    setMargins(0, 0, dp(context, 12), 0)
                }
            })

            addView(TextView(context).apply {
                text = theme.name
                textSize = 14f
                setTextColor(activeTheme.keyText)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            if (isSelected) {
                addView(TextView(context).apply {
                    text = "✓"
                    textSize = 18f
                    setTextColor(activeTheme.accent)
                })
            }

            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun createBordersToggle(
        context: Context,
        showBorders: Boolean,
        theme: ThemeManager.Theme,
        onClick: () -> Unit
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))

            addView(TextView(context).apply {
                text = "Key Borders"
                textSize = 14f
                setTextColor(theme.keyText)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(context).apply {
                text = if (showBorders) "ON" else "OFF"
                textSize = 14f
                setTextColor(theme.accent)
                paint.isFakeBoldText = true
            })

            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }





    fun createPlaceholder(
        context: Context,
        text: String,
        theme: ThemeManager.Theme?
    ): View {
        val t = theme ?: return View(context)
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(t.textSecondary)
            gravity = Gravity.CENTER
            val paddingPx = dp(context, 16f)
            layoutParams = LinearLayout.LayoutParams(
                context.resources.displayMetrics.widthPixels - paddingPx,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    fun getAllSymbolButtons(vararg rows: LinearLayout): List<Button> {
        val buttons = mutableListOf<Button>()
        for (row in rows) {
            for (i in 0 until row.childCount) {
                val child = row.getChildAt(i)
                if (child is Button) buttons.add(child)
            }
        }
        return buttons
    }

    fun styleNativeSpecialKey(
        view: View,
        theme: ThemeManager.Theme,
        isAction: Boolean = true
    ) {

        view.background = createThemedKeyBg(view.context, theme, false, isAction = isAction)
        
        if (view is TextView) {
            view.setTextColor(theme.specialKeyText)
        }
    }

    
    fun createLanguagePickerView(
        context: Context,
        languages: List<KeyboardConstants.Language>,
        currentIndex: Int,
        theme: ThemeManager.Theme,
        onSelect: (Int) -> Unit,
        onSettingsClick: () -> Unit
    ): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val rootPadding = dp(context, 20)
            setPadding(rootPadding, rootPadding, rootPadding, rootPadding)
            
            val bgRadius = dp(context, theme.keyRadius * 4f).toFloat()
            background = GradientDrawable().apply {
                setColor(theme.keyboardBg)
                cornerRadius = bgRadius
                setStroke(dp(context, 1.5f), theme.divider)
            }
            
            elevation = dp(context, 16f).toFloat()
        }


        root.addView(TextView(context).apply {
            text = "SELECT KEYBOARD"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(theme.textSecondary)
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 4), 0, dp(context, 16))
        })

        val scrollView = android.widget.ScrollView(context).apply {
            isScrollbarFadingEnabled = true
            scrollBarFadeDuration = 500
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        }

        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(context, 12)
            setPadding(pad, 0, pad, dp(context, 8))
        }

        for ((i, lang) in languages.withIndex()) {
            val isSelected = i == currentIndex
            
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val ph = dp(context, 16)
                val pv = dp(context, 14)
                setPadding(ph, pv, ph, pv)
                
                isClickable = true
                isFocusable = true
                
                background = GradientDrawable().apply {
                    cornerRadius = dp(context, theme.keyRadius).toFloat()
                    if (isSelected) {
                        setColor(theme.keyPressed)
                        val color = try { Color.parseColor(lang.accentColor) } catch (e: Exception) { theme.accent }
                        setStroke(dp(context, 2f), color)
                    } else {
                        setColor(theme.keyboardBg)
                        setStroke(dp(context, 1f), theme.divider)
                    }
                }
                
                setOnClickListener { 
                    onSelect(i) 
                }
                
                addView(View(context).apply {
                    val dotSize = dp(context, 12)
                    layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                        marginEnd = dp(context, 16)
                    }
                    val color = try { Color.parseColor(lang.accentColor) } catch (e: Exception) { theme.accent }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                    }
                })
                
                addView(TextView(context).apply {
                    text = lang.displayName
                    textSize = 17f
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(theme.keyText)
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                if (isSelected) {
                    addView(TextView(context).apply {
                        text = "✓"
                        textSize = 20f
                        val color = try { Color.parseColor(lang.accentColor) } catch (e: Exception) { theme.accent }
                        setTextColor(color)
                        setPadding(dp(context, 8), 0, 0, 0)
                    })
                }

                if (lang.badgeLabel != null) {
                    addView(TextView(context).apply {
                        text = lang.badgeLabel
                        textSize = 11f
                        setTextColor(theme.textSecondary)
                        background = GradientDrawable().apply {
                            setColor(theme.keyShadowColor)
                            cornerRadius = dp(context, theme.keyRadius / 2f).toFloat()
                        }
                        setPadding(dp(context, 8), dp(context, 2), dp(context, 8), dp(context, 2))
                    })
                }
            }
            
            listContainer.addView(item)
            if (i < languages.size - 1) {
                listContainer.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 8))
                })
            }
        }

        scrollView.addView(listContainer)
        
        val displayMetrics = context.resources.displayMetrics
        val maxHeight = (displayMetrics.heightPixels * 0.45f).toInt()
        val scrollWrapper = object : FrameLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val heightMode = MeasureSpec.getMode(heightMeasureSpec)
                val heightSize = MeasureSpec.getSize(heightMeasureSpec)
                var newHeightMeasureSpec = heightMeasureSpec
                if (heightMode == MeasureSpec.UNSPECIFIED || heightSize > maxHeight) {
                    newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                }
                super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
            }
        }
        scrollWrapper.addView(scrollView)
        root.addView(scrollWrapper)


        root.addView(View(context).apply {
            setBackgroundColor(theme.divider)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)).apply {
                setMargins(0, dp(context, 12), 0, dp(context, 8))
            }
        })


        root.addView(TextView(context).apply {
            text = "LANGUAGE SETTINGS"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(theme.accent)
            letterSpacing = 0.05f
            gravity = Gravity.CENTER
            val py = dp(context, 16)
            setPadding(0, py, 0, py)
            
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            
            setOnClickListener { onSettingsClick() }
        })

        return root
    }

    
    fun styleEmojiSearchBar(
        container: View,
        icon: ImageView,
        text: TextView,
        theme: ThemeManager.Theme
    ) {
        val isLight = ColorUtils.calculateLuminance(theme.keyboardBg) > 0.5
        container.background = GradientDrawable().apply {
            if (isLight) {

                setColor(ColorUtils.setAlphaComponent(Color.BLACK, 15))
            } else {
                setColor(ColorUtils.setAlphaComponent(theme.specialKeyBg, 180))
            }
            cornerRadius = dp(container.context, theme.keyRadius * 2f).toFloat()
        }
        
        val tint = if (isLight) Color.GRAY else ColorUtils.setAlphaComponent(theme.keyText, 128)
        DrawableCompat.setTint(icon.drawable ?: return, tint)
        text.setTextColor(tint)
    }



    fun createSearchBg(context: Context, theme: ThemeManager.Theme): GradientDrawable {
        val gd = GradientDrawable()
        val luminance = ColorUtils.calculateLuminance(theme.keyboardBg)
        val isLight = luminance > 0.5
        val color = if (isLight) {
            ColorUtils.setAlphaComponent(Color.BLACK, 15)
        } else {
            ColorUtils.setAlphaComponent(Color.WHITE, 25)
        }
        gd.setColor(color)
        gd.cornerRadius = dp(context, theme.keyRadius * 2f).toFloat()
        return gd
    }
}