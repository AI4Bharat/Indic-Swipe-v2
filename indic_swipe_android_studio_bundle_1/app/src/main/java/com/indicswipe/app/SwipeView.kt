package com.indicswipe.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class SwipeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SwipeView"


        private const val TRAIN_WIDTH  = KeyboardConstants.TRAIN_WIDTH
        private const val TRAIN_HEIGHT = KeyboardConstants.TRAIN_HEIGHT


        private const val VEL_CLIP_MIN   = KeyboardConstants.VEL_CLIP_MIN
        private const val VEL_CLIP_MAX   = KeyboardConstants.VEL_CLIP_MAX
        private const val ACCEL_CLIP_MIN = KeyboardConstants.ACCEL_CLIP_MIN
        private const val ACCEL_CLIP_MAX = KeyboardConstants.ACCEL_CLIP_MAX

        private const val MIN_SWIPE_DISTANCE_PX    = KeyboardConstants.MIN_SWIPE_DISTANCE_PX
        private const val MIN_POINTS_FOR_SWIPE      = KeyboardConstants.MIN_POINTS_FOR_SWIPE
        private const val TRAIL_FADE_DELAY_MS       = KeyboardConstants.TRAIL_FADE_DELAY_MS
        private const val TRAIL_FADE_DURATION_MS    = KeyboardConstants.TRAIL_FADE_DURATION_MS

        private const val HIGH_VELOCITY_THRESHOLD   = KeyboardConstants.HIGH_VELOCITY_THRESHOLD
        private const val LOW_VELOCITY_THRESHOLD    = KeyboardConstants.LOW_VELOCITY_THRESHOLD
        private const val PAUSE_VELOCITY_THRESHOLD  = KeyboardConstants.PAUSE_VELOCITY_THRESHOLD
        private const val MIN_PAUSE_TIME_MS         = KeyboardConstants.MIN_PAUSE_TIME_MS
        private const val SAMPLE_DIST_HIGH_SPEED_SQ = KeyboardConstants.SAMPLE_DIST_HIGH_SPEED_SQ
        private const val SAMPLE_DIST_MEDIUM_SPEED_SQ = KeyboardConstants.SAMPLE_DIST_MEDIUM_SPEED_SQ
        private const val SAMPLE_DIST_LOW_SPEED_SQ  = KeyboardConstants.SAMPLE_DIST_LOW_SPEED_SQ
        private const val SAMPLE_DIST_PAUSE_SQ      = KeyboardConstants.SAMPLE_DIST_PAUSE_SQ
        private const val CORNER_ANGLE_THRESHOLD    = KeyboardConstants.CORNER_ANGLE_THRESHOLD

        private const val MIN_POINT_DISTANCE_TRAIN  = KeyboardConstants.MIN_POINT_DISTANCE_TRAIN

        private const val ONE_EURO_MIN_CUTOFF  = KeyboardConstants.ONE_EURO_MIN_CUTOFF
        private const val ONE_EURO_BETA        = KeyboardConstants.ONE_EURO_BETA
        private const val ONE_EURO_D_CUTOFF    = KeyboardConstants.ONE_EURO_D_CUTOFF
        private const val ENDPOINT_SNAP_DISTANCE           = KeyboardConstants.ENDPOINT_SNAP_DISTANCE
        private const val ENDPOINT_VELOCITY_THRESHOLD      = KeyboardConstants.ENDPOINT_VELOCITY_THRESHOLD
        private const val START_STABILIZATION_POINTS       = KeyboardConstants.START_STABILIZATION_POINTS
        private const val END_STABILIZATION_POINTS         = KeyboardConstants.END_STABILIZATION_POINTS
        private const val TOUCH_SIZE_WEIGHT                = KeyboardConstants.TOUCH_SIZE_WEIGHT
        private const val SWIPE_MOVE_THRESHOLD_SQ          = KeyboardConstants.SWIPE_MOVE_THRESHOLD_SQ
        private const val SWIPE_DELIBERATE_THRESHOLD_SQ    = KeyboardConstants.SWIPE_DELIBERATE_THRESHOLD_SQ

        private const val LONG_PRESS_TIMEOUT_MS = 300L


        private const val KEY_LABEL_TEXT_SIZE_FACTOR    = 0.10f
        private const val SPECIAL_LABEL_TEXT_SIZE_FACTOR = 0.08f
        private const val PREVIEW_TEXT_SIZE_FACTOR      = 0.18f
        private const val KEY_CORNER_RADIUS_FACTOR      = 0.035f
        private const val SPECIAL_KEY_CORNER_FACTOR     = 0.05f
        private const val PREVIEW_CORNER_FACTOR         = 0.032f
        private const val KEY_MARGIN_FACTOR             = 0.008f 
        private const val PREVIEW_WIDTH_FACTOR          = 0.155f
        private const val PREVIEW_HEIGHT_FACTOR         = 0.21f
        private const val PREVIEW_GAP_FACTOR            = 0.03f
        private const val KEY_PRESS_SCALE               = 1.05f


        private const val TRAIL_MAIN_WIDTH          = 8f
        private const val TRAIL_GLOW_WIDTH          = 28f
        private const val TRAIL_GLOW_ALPHA_FACTOR   = 0.15f


        private const val DECODER_MIN_KEEP_DISTANCE = 10.0f
        private const val DECODER_MAX_POINTS        = KeyboardConstants.MAX_TRAJ_LEN


        private const val TRAIL_MAX_POINTS = 25
        private const val TRAIL_WIDTH_MIN = 3f
        private const val TRAIL_WIDTH_MAX = 14f
        private const val TRAIL_GLOW_RADIUS = 25f
        

        private const val STABLE_KEY_CONFIRM_COUNT         = 2
        private const val STABLE_KEY_MIN_DISTANCE          = 7f
        private const val CENTER_PROXIMITY_COMMIT_DISTANCE = 6f
        

        private const val HAPTIC_SWIPE_TICK = 1001
    }


    var onSwipeStart:               (() -> Unit)?                = null
    var onSwipeComplete:            ((List<FloatArray>) -> Unit)? = null
    var onKeyTap:                   ((Char) -> Unit)?            = null
    var onKeyLongPress:             ((Char) -> Unit)?            = null
    var onShiftTap:                 (() -> Unit)?                = null
    var onBackspaceTap:             (() -> Unit)?                = null
    var onBackspaceLongPressStart:  (() -> Unit)?                = null
    var onBackspaceUp:              (() -> Unit)?                = null
    var onSpaceTap:                 (() -> Unit)?                = null
    var onSpaceLongPress:           (() -> Unit)?                = null
    var onSpaceMove:                ((Float) -> Unit)?           = null
    var onSpaceMoveEnd:             (() -> Unit)?                = null
    var onCommaTap:                 (() -> Unit)?                = null
    var onPeriodTap:                (() -> Unit)?                = null
    var onPeriodLongPress:           (() -> Unit)?                = null
    var onCommaLongPress:            (() -> Unit)?                = null
    var onEnterTap:                 (() -> Unit)?                = null
    var onSymbolToggleTap:          (() -> Unit)?                = null
    var onSymbolToggleLongPress:    (() -> Unit)?                = null



    private var geometry: KeyboardGeometry? = null


    private var scaleX = 1f
    private var scaleY = 1f
    private var internalPaddingX = 0f
    private var internalPaddingY = 0f
    private var internalPaddingBottom = 0f


    private var isShifted   = false
    private var isCapsLock  = false
    private var isHindiMode = true
    private var currentLanguageName = "हिन्दी"
    private var currentLanguageColor = Color.BLUE
    private var isSymbolMode = false
    private var symbolPage   = 1
    private var isSpaceMovingUI = false
    private var spaceSliderX = 0f
    private var spaceMoveAccumulatedX = 0f
    private var enterIcon: Drawable?          = null
    private var shiftIcon: Drawable?          = null
    private var shiftFilledIcon: Drawable?    = null
    private var shiftCapsIcon: Drawable?      = null
    private var backspaceIcon: Drawable?      = null
    private var emojiIcon: Drawable?          = null


    private data class PointerState(
        var downX: Float = 0f,
        var downY: Float = 0f,
        var lastX: Float = 0f,
        var lastY: Float = 0f,
        var downTime: Long = 0L,
        var hasMovedEnoughForSwipe: Boolean = false,
        var hasTriggeredSwipeStart: Boolean = false,
        var swipeDisabled: Boolean = false,
        var currentNearestKey: Char?    = null,
        var currentSpecialKey: String?  = null,
        var isSwipePointer: Boolean     = false,
        var previewChar: Char?          = null,
        var previewScreenX: Float       = 0f,
        var previewScreenY: Float       = 0f,
        var showPreview: Boolean        = false
    )

    private val activePointers = mutableMapOf<Int, PointerState>()
    private var swipePointerId  = -1
    private var isTouchActive   = false
    private var systemTouchSlop = 0f


    private var isBackspacePressed          = false
    private var backspaceLongPressTriggered = false
    private var isSpacePressed              = false
    private var spaceLongPressTriggered     = false
    private var isSymbolTogglePressed       = false
    private var symbolToggleLongPressTriggered = false
    private var isPeriodPressed             = false
    private var periodLongPressTriggered    = false
    private var isCommaPressed              = false
    private var commaLongPressTriggered     = false


    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable:              Runnable? = null
    private var spaceLongPressRunnable:         Runnable? = null
    private var symbolToggleLongPressRunnable:  Runnable? = null
    private var alphaKeyLongPressRunnable:      Runnable? = null
    private var alphaKeyLongPressTriggered = false


    private var trailAlpha       = 200
    private var trailFadeAnimator: ValueAnimator? = null


    private val topRowHints = arrayOf('1','2','3','4','5','6','7','8','9','0')
    private val topRowChars = arrayOf('q','w','e','r','t','y','u','i','o','p')


    private var colorKeyBg           = Color.parseColor("#80232427")
    private var colorKeyPressed      = Color.parseColor("#757575")
    private var colorKeyText         = Color.parseColor("#F9FCFF")
    private var colorTextSecondary   = Color.parseColor("#9AA0A6")
    private var colorSpecialBg       = Color.parseColor("#80B1BEFF")
    private var colorSpecialPressed  = Color.parseColor("#5090A0D0")
    private var colorSpecialIconColor= Color.parseColor("#181921")
    private var colorAccent          = Color.parseColor("#8AB4F8")
    private var colorKeyboardBg      = Color.parseColor("#191C1E")
    private var colorKeyStroke       = Color.parseColor("#80232427")
    private var colorPreviewBg       = Color.parseColor("#232427")
    private var colorPreviewText     = Color.parseColor("#F9FCFF")
    private var colorPreviewShadow   = Color.parseColor("#80000000")
    private var colorTrail           = Color.parseColor("#8AB4F8")
    private var colorTrailGlow       = Color.parseColor("#408AB4F8")
    private var colorDebugBorder     = Color.argb(0, 0, 0, 0)
    private var themeShadowRadius    = 0f
    private var themeShadowDy        = 0f
    private var themeShadowColor     = Color.TRANSPARENT
    private var showBorders          = false
    private var cornerRadius         = 0f
    private var specialCornerRadius  = 0f

    private var currentTheme: ThemeManager.Theme? = null
    private var currentEnterIconRes = R.drawable.ic_enter


    private var consecutiveSlowPoints = 0




    private val rawPoints       = mutableListOf<TouchPoint>()
    private val processedPoints = mutableListOf<FloatArray>()
    private val trailPath       = Path()
    private var trailStarted    = false


    private val filterX = OneEuroFilter(ONE_EURO_MIN_CUTOFF, ONE_EURO_BETA, ONE_EURO_D_CUTOFF)
    private val filterY = OneEuroFilter(ONE_EURO_MIN_CUTOFF, ONE_EURO_BETA, ONE_EURO_D_CUTOFF)

    private var gestureStartTime    = 0L
    private val startPoints         = mutableListOf<TouchPoint>()
    private var stabilizedStartPoint: TouchPoint? = null
    private val detectedCorners     = mutableListOf<Int>()


    private var stableNearestKey:      Char? = null
    private var pendingNearestKey:     Char? = null
    private var pendingNearestKeyCount = 0



    private var keyboardBitmap:       Bitmap? = null
    private var keyboardBitmapCanvas: Canvas? = null
    private var keyboardCacheDirty   = true


    private val tempRect        = RectF()
    private val eraseRect       = RectF()
    private val shadowLayerRect = RectF()





    
    private data class TouchPoint(
        val screenX: Float,
        val screenY: Float,
        val trainX:  Float,
        val trainY:  Float,
        val timestamp: Long,
        val pressure:  Float,
        val drawX: Float,
        val drawY: Float,

        val vx: Float = 0f,
        val vy: Float = 0f,

        val vxNorm: Float = 0f,
        val vyNorm: Float = 0f,
        val axNorm: Float = 0f,
        val ayNorm: Float = 0f,
        val isPause:          Boolean = false,
        val isStableKeyChange: Boolean = false,
        val nearestKey: Char? = null
    )





    private class OneEuroFilter(
        private val minCutoff: Float,
        private val beta:      Float,
        private val dCutoff:   Float
    ) {
        private var xPrev:  Float? = null
        private var dxPrev  = 0f
        private var tPrev   = 0L

        fun reset() { xPrev = null; dxPrev = 0f; tPrev = 0L }

        fun filter(x: Float, t: Long): Float {
            val prev = xPrev ?: run { xPrev = x; tPrev = t; return x }
            val dt = (t - tPrev) / 1000f
            if (dt <= 0f) return prev
            val dx  = (x - prev) / dt
            val dxS = expSmooth(dx, dxPrev, alpha(dCutoff, dt))
            val cut = minCutoff + beta * abs(dxS)
            val xF  = expSmooth(x, prev, alpha(cut, dt))
            xPrev  = xF; dxPrev = dxS; tPrev = t
            return xF
        }

        fun getVelocity() = dxPrev

        private fun alpha(c: Float, dt: Float): Float {
            val tau = 1f / (2f * Math.PI.toFloat() * c)
            return 1f / (1f + tau / dt)
        }
        private fun expSmooth(c: Float, p: Float, a: Float) = a * c + (1f - a) * p
    }





    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorKeyboardBg
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 1.5f * Resources.getSystem().displayMetrics.density
        color       = colorKeyStroke
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 0.5f
        color       = colorDebugBorder
    }

    private val keyLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = colorKeyText
        textSize  = 40f
        textAlign = Paint.Align.CENTER
    }

    private val specialLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.WHITE
        textSize  = 36f
        textAlign = Paint.Align.CENTER
    }

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 24f
        textAlign = Paint.Align.RIGHT
        alpha     = 204
    }

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = colorTrail
        style       = Paint.Style.STROKE
        strokeWidth = TRAIL_MAIN_WIDTH
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
        alpha       = 200
    }

    private val trailGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = colorTrailGlow
        style       = Paint.Style.STROKE
        strokeWidth = TRAIL_GLOW_WIDTH
        strokeCap   = Paint.Cap.ROUND
        strokeJoin  = Paint.Join.ROUND
        alpha       = 30
    }

    private val previewBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorPreviewBg
    }

    private val popupBlurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(Color.WHITE, 20)
    }

    private val previewShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val previewTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color          = colorPreviewText
        textAlign      = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val clearTrailRunnable = Runnable { startTrailFade() }





    init {
        isFocusable          = true
        isFocusableInTouchMode = true
        isHapticFeedbackEnabled = true
        setBackgroundColor(colorKeyboardBg)
        loadIcons()
        systemTouchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    }

    fun setKeyboardBackgroundColor(color: Int) {
        if (colorKeyboardBg != color) {
            colorKeyboardBg = color
            setBackgroundColor(color)
            erasePaint.color = color
            keyboardCacheDirty = true
            invalidate()
        }
    }





    fun applyTheme(theme: ThemeManager.Theme, borders: Boolean) {
        currentTheme          = theme
        colorKeyBg            = theme.keyBg
        colorKeyPressed       = theme.keyPressed
        colorKeyText          = theme.keyText
        colorSpecialBg        = theme.specialKeyBg
        colorSpecialPressed   = theme.specialKeyPressed
        colorSpecialIconColor = theme.specialKeyIcon
        colorAccent           = currentLanguageColor
        colorTextSecondary    = theme.textSecondary
        colorKeyboardBg       = theme.keyboardBg
        colorKeyStroke        = theme.keyStroke
        colorPreviewBg        = theme.previewBg
        colorPreviewText      = theme.previewText
        colorPreviewShadow    = theme.previewShadow
        colorTrail            = currentLanguageColor
        colorTrailGlow        = androidx.core.graphics.ColorUtils.setAlphaComponent(currentLanguageColor, 80)
        colorDebugBorder      = theme.keyBorder
        themeShadowRadius     = theme.keyShadowRadius
        themeShadowDy         = theme.keyShadowDy
        themeShadowColor      = theme.keyShadowColor
        showBorders           = borders
        cornerRadius          = dp(context, theme.keyRadius).toFloat()
        specialCornerRadius   = dp(context, theme.specialKeyRadius).toFloat()

        keyLabelPaint.color     = colorKeyText
        specialLabelPaint.color = theme.specialKeyText
        trailPaint.color        = colorTrail
        trailGlowPaint.color    = colorTrailGlow
        previewBgPaint.color    = colorPreviewBg
        previewTextPaint.color  = colorPreviewText
        erasePaint.color        = colorKeyboardBg
        strokePaint.color       = colorKeyStroke
        hintPaint.color         = colorTextSecondary
        hintPaint.alpha         = 220
        borderPaint.color       = colorDebugBorder

        setBackgroundColor(colorKeyboardBg)

        shiftIcon?.let      { DrawableCompat.setTint(it, theme.specialKeyIcon) }
        shiftFilledIcon?.let{ DrawableCompat.setTint(it, theme.specialKeyIcon) }
        shiftCapsIcon?.let  { DrawableCompat.setTint(it, theme.specialKeyIcon) }
        backspaceIcon?.let  { DrawableCompat.setTint(it, theme.specialKeyIcon) }
        emojiIcon?.let      { DrawableCompat.setTint(it, theme.specialKeyIcon) }


        keyboardCacheDirty = true
        invalidate()
    }

    private fun loadIcons() {
        try {
            shiftIcon       = ContextCompat.getDrawable(context, R.drawable.ic_shift)?.mutate()
            shiftFilledIcon = ContextCompat.getDrawable(context, R.drawable.ic_shift_filled)?.mutate()
            shiftCapsIcon   = ContextCompat.getDrawable(context, R.drawable.ic_shift_caps_lock)?.mutate()
            backspaceIcon   = ContextCompat.getDrawable(context, R.drawable.ic_backspace)?.mutate()
            emojiIcon       = ContextCompat.getDrawable(context, R.drawable.ic_emoji_outline)?.mutate()
            
            val iconColor = colorSpecialIconColor
            shiftIcon?.let       { DrawableCompat.setTint(it, iconColor) }
            shiftFilledIcon?.let { DrawableCompat.setTint(it, iconColor) }
            shiftCapsIcon?.let   { DrawableCompat.setTint(it, iconColor) }
            backspaceIcon?.let   { DrawableCompat.setTint(it, iconColor) }
            emojiIcon?.let       { DrawableCompat.setTint(it, iconColor) }
        } catch (e: Exception) { Log.w(TAG, "Icons: ${e.message}") }
    }

    fun setKeyboardGeometry(geom: KeyboardGeometry) {
        geometry = geom
        updateCoordinateMapping()
        keyboardCacheDirty = true
        invalidate()
    }

    fun getGeometry(): KeyboardGeometry? = geometry

    fun setLanguageInfo(name: String, color: Int, isHindi: Boolean) {
        if (currentLanguageName != name || currentLanguageColor != color || isHindiMode != isHindi) {
            currentLanguageName = name
            currentLanguageColor = color
            isHindiMode = isHindi
            


            colorAccent = color
            colorTrail = color
            colorTrailGlow = androidx.core.graphics.ColorUtils.setAlphaComponent(color, 80)
            trailPaint.color = colorTrail
            trailGlowPaint.color = colorTrailGlow
            
            keyboardCacheDirty = true
            invalidate()
        }
    }

    fun setSymbolMode(enabled: Boolean, page: Int = 1) {
        if (isSymbolMode != enabled || symbolPage != page) {
            isSymbolMode = enabled
            symbolPage = page
            keyboardCacheDirty = true
            invalidate()
        }
    }

    fun setShifted(shift: Boolean, caps: Boolean) {
        if (isShifted != shift || isCapsLock != caps) {
            isShifted  = shift
            isCapsLock = caps
            keyboardCacheDirty = true
            invalidate()
        }
    }

    fun setEnterActionIcon(resId: Int) {
        try {
            enterIcon = ContextCompat.getDrawable(context, resId)?.mutate()
            enterIcon?.let { DrawableCompat.setTint(it, colorSpecialIconColor) }
            keyboardCacheDirty = true
            invalidate()
        } catch (_: Exception) {}
    }





    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        updateCoordinateMapping()
        keyboardBitmap?.recycle()
        keyboardBitmap       = null
        keyboardBitmapCanvas = null
        if (w > 0 && h > 0) {
            keyboardBitmap       = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            keyboardBitmapCanvas = Canvas(keyboardBitmap!!)
        }
        keyboardCacheDirty = true
    }

    
    private fun updateCoordinateMapping() {
        val geom = geometry ?: return
        if (width <= 0 || height <= 0) return

        internalPaddingX = width * KeyboardConstants.KEYBOARD_SIDE_MARGIN_RATIO
        val drawWidth = width - 2 * internalPaddingX
        

        scaleX = KeyboardConstants.TRAIN_WIDTH / drawWidth.coerceAtLeast(1f)
        



        internalPaddingY = dp(context, 6f).toFloat()
        internalPaddingBottom = dp(context, 18f).toFloat()
        val drawHeight = height - internalPaddingY - internalPaddingBottom
        scaleY = KeyboardConstants.TRAIN_HEIGHT / drawHeight.coerceAtLeast(1f)
        
        geom.setDimensions(drawWidth, drawHeight)
        
        Log.i(TAG, "📐 True North Calibrated: screen=${width}x${height} | drawW=$drawWidth | drawH=$drawHeight | paddingY=$internalPaddingY | scale=(${"%.4f".format(scaleX)},${"%.4f".format(scaleY)})")
        updateTextSizes()
    }

    private fun updateTextSizes() {
        val scaledDensity = resources.displayMetrics.scaledDensity
        

        val baseLetterSize = 24f * scaledDensity
        val baseSymbolSize = KeyboardConstants.SYMBOL_KEY_TEXT_SIZE * scaledDensity
        
        keyLabelPaint.textSize = if (isSymbolMode) baseSymbolSize else baseLetterSize
        specialLabelPaint.textSize = KeyboardConstants.SYMBOL_SPECIAL_TEXT_SIZE * scaledDensity
        previewTextPaint.textSize  = 30f * scaledDensity
        hintPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)
    }





    
    private fun sx2tx(sx: Float) = (sx - internalPaddingX) * scaleX
    private fun sy2ty(sy: Float) = (sy - internalPaddingY) * scaleY

    
    private fun tx2sx(tx: Float) = (tx / scaleX) + internalPaddingX
    private fun ty2sy(ty: Float) = (ty / scaleY) + internalPaddingY

    private fun dp(context: Context, dp: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            context.resources.displayMetrics
        ).toInt()





    private fun isScreenOnBackspace(sx: Float, sy: Float): Boolean {
        val g = geometry ?: return false
        return g.hitTestSpecial(sx2tx(sx), sy2ty(sy)) == "backspace"
    }

    private fun getSpecialKeyAt(tx: Float, ty: Float): String? =
        geometry?.hitTestSpecial(tx, ty)

    private fun rebuildKeyboardCache() {
        val bitmap  = keyboardBitmap        ?: return
        val bCanvas = keyboardBitmapCanvas  ?: return
        val geom    = geometry              ?: return

        bitmap.eraseColor(colorKeyboardBg)

        val cornerR        = cornerRadius
        val specialCornerR = specialCornerRadius

        val resMargin = context.resources.getDimensionPixelSize(R.dimen.key_margin)
        val marginH = resMargin.toFloat() / 2f
        val marginV = resMargin.toFloat() / 2f

        updateTextSizes()
        
        val symbolSet = if (symbolPage == 1) KeyboardConstants.SYMBOL_PAGE_1 else KeyboardConstants.SYMBOL_PAGE_2
        val row1Symbols = symbolSet[0]
        val row2Symbols = symbolSet[1]
        val row3Symbols = symbolSet[2]

        for (key in geom.getAllKeys()) {
            val cx = tx2sx(key.cx)
            val cy = ty2sy(key.cy)
            val kw = key.width  / scaleX
            val kh = key.height / scaleY

            tempRect.set(
                cx - kw / 2 + marginH, 
                cy - kh / 2 + marginV, 
                cx + kw / 2 - marginH, 
                cy + kh / 2 - marginV
            )


            if (key.isSpecial) {
                drawSpecialKeyTo(bCanvas, key, tempRect, cornerR, specialCornerR)
            } else {

                if (themeShadowRadius > 0f) {
                    fillPaint.color = themeShadowColor
                    val sr = RectF(tempRect.left, tempRect.top + themeShadowDy, tempRect.right, tempRect.bottom + themeShadowDy)
                    bCanvas.drawRoundRect(sr, cornerR, cornerR, fillPaint)
                }


                val startColor = currentTheme?.keyGradientStart ?: colorKeyBg
                val endColor = currentTheme?.keyGradientEnd ?: colorKeyBg
                fillPaint.color = startColor
                fillPaint.shader = null
                bCanvas.drawRoundRect(tempRect, cornerR, cornerR, fillPaint)

                if (Color.alpha(colorKeyStroke) > 0)
                    bCanvas.drawRoundRect(tempRect, cornerR, cornerR, strokePaint)
                if (showBorders || Color.alpha(colorDebugBorder) > 0)
                    bCanvas.drawRoundRect(tempRect, cornerR, cornerR, borderPaint)

                val label = if (isSymbolMode) {
                    if (symbolPage == 0) "" else {

                        val char = key.char.lowercaseChar()
                        when {
                            "qwertyuiop".contains(char) -> row1Symbols.getOrNull("qwertyuiop".indexOf(char)) as? String ?: ""
                            "asdfghjkl".contains(char) -> row2Symbols.getOrNull("asdfghjkl".indexOf(char)) as? String ?: ""
                            "zxcvbnm".contains(char) -> row3Symbols.getOrNull("zxcvbnm".indexOf(char)) as? String ?: ""
                            else -> ""
                        }
                    }
                } else {
                    if (isShifted) key.char.uppercaseChar().toString() else key.char.lowercaseChar().toString()
                }
                
                bCanvas.drawText(label, cx, cy + (keyLabelPaint.textSize / 3), keyLabelPaint)

                val hintIdx = topRowChars.indexOf(key.char.lowercaseChar())
                if (hintIdx != -1 && !isSymbolMode) {
                    val p = context.resources.displayMetrics.density * 4f
                    hintPaint.alpha = 140
                    bCanvas.drawText(
                        topRowHints[hintIdx].toString(),
                        cx + (kw / 2f) - marginH - p,
                        cy - (kh / 2f) + marginV + p * 2.5f,
                        hintPaint
                    )
                }
            }
        }

        keyboardCacheDirty = false
    }





    private fun drawSpecialKeyTo(canvas: Canvas, key: KeyboardGeometry.Key, rect: RectF, cornerR: Float, specialCornerR: Float) {
        when (key.specialName) {
            "shift" -> if (isSymbolMode) drawGenericSpecialKeyTo(canvas, false, rect, specialCornerR, "symbol_page_toggle") 
                       else drawShiftKeyTo(canvas, false, rect, specialCornerR)
            "backspace" -> drawBackspaceKeyTo(canvas, false, rect, specialCornerR)
            "space" -> drawSpaceKeyTo(canvas, false, rect, specialCornerR)
            "enter" -> drawEnterKeyTo(canvas, false, rect, specialCornerR)
            else -> drawGenericSpecialKeyTo(canvas, false, rect, specialCornerR, key.specialName ?: "")
        }
    }

    private fun drawShiftKeyTo(canvas: Canvas, highlighted: Boolean, rect: RectF, cornerR: Float) {
        val faceColor = when {
            highlighted   -> colorSpecialPressed
            isCapsLock    -> colorAccent
            isShifted     -> colorAccent.withAlpha(200)
            else          -> colorSpecialBg
        }


        if (themeShadowRadius > 0f) {
            fillPaint.color = themeShadowColor
            val sr = RectF(rect.left, rect.top + themeShadowDy, rect.right, rect.bottom + themeShadowDy)
            canvas.drawRoundRect(sr, cornerR, cornerR, fillPaint)
        }


        fillPaint.color = faceColor
        canvas.drawRoundRect(rect, cornerR, cornerR, fillPaint)

        if (Color.alpha(colorKeyStroke) > 0)
            canvas.drawRoundRect(rect, cornerR, cornerR, strokePaint)
        if (showBorders || Color.alpha(colorDebugBorder) > 0)
            canvas.drawRoundRect(rect, cornerR, cornerR, borderPaint)

        val iconSize = rect.height() * 0.32f
        val icon = when {
            isCapsLock -> shiftCapsIcon
            isShifted  -> shiftFilledIcon
            else       -> shiftIcon
        }
        icon?.let {
            it.setBounds(
                (rect.centerX() - iconSize / 2).toInt(), (rect.centerY() - iconSize / 2).toInt(),
                (rect.centerX() + iconSize / 2).toInt(), (rect.centerY() + iconSize / 2).toInt()
            )
            val iconColor = if (highlighted || isShifted || isCapsLock) {
                val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(faceColor)
                if (luminance > 0.6) Color.parseColor("#2D2D2D") else Color.WHITE
            } else {
                colorSpecialIconColor
            }
            DrawableCompat.setTint(it, iconColor)
            it.draw(canvas)
        }
    }

    private fun drawBackspaceKeyTo(canvas: Canvas, highlighted: Boolean, rect: RectF, cornerR: Float) {

        if (themeShadowRadius > 0f) {
            fillPaint.color = themeShadowColor
            val sr = RectF(rect.left, rect.top + themeShadowDy, rect.right, rect.bottom + themeShadowDy)
            canvas.drawRoundRect(sr, cornerR, cornerR, fillPaint)
        }


        fillPaint.color = if (highlighted) colorAccent else colorSpecialBg
        canvas.drawRoundRect(rect, cornerR, cornerR, fillPaint)

        if (Color.alpha(colorKeyStroke) > 0)
            canvas.drawRoundRect(rect, cornerR, cornerR, strokePaint)
        if (showBorders || Color.alpha(colorDebugBorder) > 0)
            canvas.drawRoundRect(rect, cornerR, cornerR, borderPaint)

        val bH = rect.height() * 0.32f
        backspaceIcon?.let {
            val aspect = if (it.intrinsicHeight > 0) it.intrinsicWidth.toFloat() / it.intrinsicHeight else 1.25f
            val bW = bH * aspect
            it.setBounds(
                (rect.centerX() - bW / 2).toInt(), (rect.centerY() - bH / 2).toInt(),
                (rect.centerX() + bW / 2).toInt(), (rect.centerY() + bH / 2).toInt()
            )
            val iconColor = if (highlighted) {
                val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(colorAccent)
                if (luminance > 0.6) Color.parseColor("#2D2D2D") else Color.WHITE
            } else {
                colorSpecialIconColor
            }
            DrawableCompat.setTint(it, iconColor)
            it.draw(canvas)
        }
    }

    private fun drawEnterKeyTo(canvas: Canvas, highlighted: Boolean, rect: RectF, cornerR: Float) {

        if (themeShadowRadius > 0f) {
            fillPaint.color = themeShadowColor
            val sr = RectF(rect.left, rect.top + themeShadowDy, rect.right, rect.bottom + themeShadowDy)
            canvas.drawRoundRect(sr, cornerR, cornerR, fillPaint)
        }


        fillPaint.color = colorAccent.withAlpha(255)
        canvas.drawRoundRect(rect, cornerR, cornerR, fillPaint)

        if (Color.alpha(colorKeyStroke) > 0)
            canvas.drawRoundRect(rect, cornerR, cornerR, strokePaint)
        if (showBorders || Color.alpha(colorDebugBorder) > 0)
            canvas.drawRoundRect(rect, cornerR, cornerR, borderPaint)

        val iconSize = rect.height() * 0.32f
        enterIcon?.let {
            it.setBounds(
                (rect.centerX() - iconSize / 2).toInt(), (rect.centerY() - iconSize / 2).toInt(),
                (rect.centerX() + iconSize / 2).toInt(), (rect.centerY() + iconSize / 2).toInt()
            )
            val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(colorAccent)
            val iconColor = if (luminance > 0.6) Color.parseColor("#2D2D2D") else Color.WHITE
            DrawableCompat.setTint(it, iconColor)
            it.draw(canvas)
        }
    }

    private fun drawSpaceKeyTo(canvas: Canvas, highlighted: Boolean, rect: RectF, cornerR: Float) {
        fillPaint.color = if (highlighted) colorKeyPressed else colorKeyBg
        canvas.drawRoundRect(rect, cornerR, cornerR, fillPaint) 

        val label = currentLanguageName
        specialLabelPaint.color = colorKeyText

        specialLabelPaint.alpha = if (isSpaceMovingUI) 60 else (if (highlighted) 255 else 180)
        
        canvas.drawText(
            label, rect.centerX(),
            rect.centerY() - (specialLabelPaint.descent() + specialLabelPaint.ascent()) / 2,
            specialLabelPaint
        )
        



        if (isSpaceMovingUI) {
            val trackHeight = 2f * resources.displayMetrics.density
            val pillWidth = 32f * resources.displayMetrics.density
            val pillHeight = rect.height() * 0.45f
            

            fillPaint.color = colorKeyText
            fillPaint.alpha = 45
            val trackRect = RectF(rect.left + cornerR, rect.centerY() - trackHeight/2, 
                                 rect.right - cornerR, rect.centerY() + trackHeight/2)
            canvas.drawRoundRect(trackRect, trackHeight/2, trackHeight/2, fillPaint)
            

            fillPaint.color = colorAccent
            fillPaint.alpha = 255
            val pillX = spaceSliderX.coerceIn(rect.left + pillWidth/2 + 4f, rect.right - pillWidth/2 - 4f)
            val pillRect = RectF(pillX - pillWidth/2, rect.centerY() - pillHeight/2,
                                pillX + pillWidth/2, rect.centerY() + pillHeight/2)
            canvas.drawRoundRect(pillRect, pillHeight/2, pillHeight/2, fillPaint)
        }
        
    }

    private fun drawGenericSpecialKeyTo(
        canvas: Canvas, highlighted: Boolean,
        rect: RectF, cornerR: Float, name: String
    ) {

        if (themeShadowRadius > 0f) {
            fillPaint.color = themeShadowColor
            val sr = RectF(rect.left, rect.top + themeShadowDy, rect.right, rect.bottom + themeShadowDy)
            canvas.drawRoundRect(sr, cornerR, cornerR, fillPaint)
        }


        fillPaint.color = if (highlighted) colorSpecialPressed else colorSpecialBg
        fillPaint.shader = null
        canvas.drawRoundRect(rect, cornerR, cornerR, fillPaint)
        fillPaint.shader = null



        val label = when (name) {
            "symbol_toggle" -> if (isSymbolMode) "ABC" else "?123"
            "symbol_page_toggle" -> if (symbolPage == 1) "=\\<" else "?123"
            "comma"         -> ","
            "period"        -> "."
            else            -> ""
        }
        specialLabelPaint.color = colorSpecialIconColor
        val verticalOffset = if (name == "comma") (rect.height() * 0.12f) else 0f
        canvas.drawText(
            label, rect.centerX(),
            rect.centerY() - (specialLabelPaint.descent() + specialLabelPaint.ascent()) / 2 + verticalOffset,
            specialLabelPaint
        )
        
        if (name == "comma") {
            emojiIcon?.let { icon ->
                val p = context.resources.displayMetrics.density * 2f
                val iconSize = rect.height() * 0.28f
                val left = (rect.centerX() - iconSize / 2).toInt()
                val top = (rect.top + p * 4f).toInt()
                icon.setBounds(left, top, (left + iconSize).toInt(), (top + iconSize).toInt())
                icon.draw(canvas)
            }
        }
    }

    private fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha shl 24)





    override fun onDraw(canvas: Canvas) {
        val t0 = System.currentTimeMillis()
        super.onDraw(canvas)

        if (keyboardCacheDirty) rebuildKeyboardCache()
        keyboardBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        drawActiveOverlays(canvas)
 

        if ((isTouchActive || (trailAlpha > 0 && !trailPath.isEmpty)) && rawPoints.size > 1) {
            val pointsToDraw = rawPoints.takeLast(TRAIL_MAX_POINTS)
            val n = pointsToDraw.size
            if (n > 1) {

                val fadeFactor = if (isTouchActive) 1f else (trailAlpha.toFloat() / 200f)
                
                for (i in 0 until n - 1) {
                    val p1 = pointsToDraw[i]
                    val p2 = pointsToDraw[i + 1]
                    

                    val progress = i.toFloat() / (n - 1).toFloat()
                    

                    trailPaint.alpha = (progress * 255 * fadeFactor).toInt()
                    trailPaint.strokeWidth = TRAIL_WIDTH_MIN + (TRAIL_WIDTH_MAX - TRAIL_WIDTH_MIN) * progress
                    canvas.drawLine(p1.drawX, p1.drawY, p2.drawX, p2.drawY, trailPaint)
                    

                    if (i % 2 == 0) {
                        trailGlowPaint.alpha = (progress * 45 * fadeFactor).toInt()
                        trailGlowPaint.strokeWidth = trailPaint.strokeWidth * 2.5f
                        canvas.drawLine(p1.drawX, p1.drawY, p2.drawX, p2.drawY, trailGlowPaint)
                    }
                }
            }
        }

        drawKeyPreviews(canvas)
        PerformanceTracker.logDrawTime(System.currentTimeMillis() - t0)
    }

    private fun drawActiveOverlays(canvas: Canvas) {
        val geom       = geometry ?: return
        val cornerR    = cornerRadius
        val specialCornerR = specialCornerRadius
        
        val resMargin = context.resources.getDimensionPixelSize(R.dimen.key_margin)
        val marginH = resMargin.toFloat() / 2f
        val marginV = resMargin.toFloat() / 2f

        for (ps in activePointers.values) {

            if (ps.currentNearestKey != null && !ps.hasMovedEnoughForSwipe) {
                val char = ps.currentNearestKey!!
                val tc   = geom.getKeyCenter(char) ?: continue
                val ks   = geom.getKeySize(char)   ?: continue

                val cx = tx2sx(tc.first);  val cy = ty2sy(tc.second)
                val kw = ks.first / scaleX; val kh = ks.second / scaleY

                val shadowExtend = themeShadowRadius + 3f
                eraseRect.set(cx - kw/2 - shadowExtend, cy - kh/2 - shadowExtend,
                               cx + kw/2 + shadowExtend, cy + kh/2 + shadowExtend + themeShadowDy)
                canvas.drawRect(eraseRect, erasePaint)

                canvas.save()
                canvas.scale(KEY_PRESS_SCALE, KEY_PRESS_SCALE, cx, cy)
                tempRect.set(cx - kw / 2 + marginH, cy - kh / 2 + marginV,
                             cx + kw / 2 - marginH, cy + kh / 2 - marginV)
                fillPaint.color = colorKeyPressed
                canvas.drawRoundRect(tempRect, cornerR, cornerR, fillPaint)
                if (Color.alpha(colorKeyStroke) > 0)
                    canvas.drawRoundRect(tempRect, cornerR, cornerR, strokePaint)
                if (showBorders || Color.alpha(colorDebugBorder) > 0)
                    canvas.drawRoundRect(tempRect, cornerR, cornerR, borderPaint)
                val label = if (isSymbolMode) {
                    val symbolSet = if (symbolPage == 1) KeyboardConstants.SYMBOL_PAGE_1 else KeyboardConstants.SYMBOL_PAGE_2
                    val c = char.lowercaseChar()
                    when {
                        "qwertyuiop".contains(c) -> symbolSet[0].getOrNull("qwertyuiop".indexOf(c)) as? String ?: ""
                        "asdfghjkl".contains(c) -> symbolSet[1].getOrNull("asdfghjkl".indexOf(c)) as? String ?: ""
                        "zxcvbnm".contains(c) -> symbolSet[2].getOrNull("zxcvbnm".indexOf(c)) as? String ?: ""
                        else -> ""
                    }
                } else {
                    if (isShifted) char.uppercaseChar().toString() else char.toString()
                }
                

                updateTextSizes()
                val textY = cy - (keyLabelPaint.descent() + keyLabelPaint.ascent()) / 2
                canvas.drawText(label, cx, textY, keyLabelPaint)
                canvas.restore()
            }

            val sk = ps.currentSpecialKey
            if (sk != null) {
                geometry?.getSpecialKeyRect(sk)?.let { rect ->
                    val r = rectTrainToScreen(rect)
                    eraseSpecialKeyArea(canvas, r)
                    when (sk) {
                        "shift" -> drawShiftKeyTo(canvas, true, r, specialCornerR)
                        "backspace" -> drawBackspaceKeyTo(canvas, true, r, specialCornerR)
                        "space" -> drawSpaceKeyTo(canvas, true, r, specialCornerR)
                        "enter" -> drawEnterKeyTo(canvas, true, r, specialCornerR)
                        else -> drawGenericSpecialKeyTo(canvas, true, r, specialCornerR, sk)
                    }
                }
            }
        }
    }

    private fun eraseSpecialKeyArea(canvas: Canvas, rect: RectF) {
        val shadowExtend = themeShadowRadius + 3f
        eraseRect.set(rect.left - shadowExtend, rect.top - shadowExtend,
                      rect.right + shadowExtend, rect.bottom + shadowExtend + themeShadowDy)
        canvas.drawRect(eraseRect, erasePaint)
    }

    
    private fun rectTrainToScreen(r: RectF): RectF {
        val cx = tx2sx(r.centerX()); val cy = ty2sy(r.centerY())
        val hw = r.width()  / scaleX / 2f
        val hh = r.height() / scaleY / 2f
        return RectF(cx - hw, cy - hh, cx + hw, cy + hh)
    }

    private fun drawKeyPreviews(canvas: Canvas) {

        for (ps in activePointers.values) {
            if (!ps.showPreview) continue
            val char = ps.previewChar ?: continue

            val bW      = height * PREVIEW_WIDTH_FACTOR
            val bH      = height * PREVIEW_HEIGHT_FACTOR
            val cornerR = height * PREVIEW_CORNER_FACTOR
            val gap     = height * PREVIEW_GAP_FACTOR
            val cx      = ps.previewScreenX

            val bL = (cx - bW / 2).coerceIn(4f, width.toFloat() - bW - 4f)
            val bB = ps.previewScreenY - gap
            val bT = (bB - bH).coerceAtLeast(4f)
            tempRect.set(bL, bT, bL + bW, bB)

            for (layer in 4 downTo 1) {
                val spread = layer * 2f
                val yOff   = layer * 1.5f
                val alpha  = when (layer) { 4 -> 4; 3 -> 10; 2 -> 18; else -> 28 }
                shadowLayerRect.set(tempRect.left - spread, tempRect.top - spread + yOff,
                                    tempRect.right + spread, tempRect.bottom + spread + yOff)
                previewShadowPaint.color = ColorUtils.setAlphaComponent(colorPreviewShadow, alpha)
                canvas.drawRoundRect(shadowLayerRect, cornerR + spread, cornerR + spread, previewShadowPaint)
            }


            val popupColor = currentTheme?.popupBg ?: colorPreviewBg
            previewBgPaint.color = ColorUtils.setAlphaComponent(popupColor, 230)
            canvas.drawRoundRect(tempRect, cornerR, cornerR, previewBgPaint)
            

            canvas.drawRoundRect(tempRect, cornerR, cornerR, popupBlurPaint)

            val popupStroke = currentTheme?.popupStroke ?: colorKeyStroke
            if (Color.alpha(popupStroke) > 0) {
                strokePaint.color = popupStroke
                strokePaint.strokeWidth = dp(context, 1.5f).toFloat()
                canvas.drawRoundRect(tempRect, cornerR, cornerR, strokePaint)
                strokePaint.strokeWidth = 1f * resources.displayMetrics.density
            }

            canvas.drawText(
                char.toString(),
                bL + bW / 2,
                bT + bH / 2 - (previewTextPaint.descent() + previewTextPaint.ascent()) / 2,
                previewTextPaint
            )
        }
    }





    private fun startTrailFade() {
        trailFadeAnimator?.cancel()
        trailFadeAnimator = ValueAnimator.ofInt(200, 0).apply {
            duration     = TRAIL_FADE_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { trailAlpha = it.animatedValue as Int; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    trailPath.reset(); trailStarted = false; trailAlpha = 200; invalidate()
                }
            })
            start()
        }
    }

    private fun cancelTrailFade() {
        trailFadeAnimator?.cancel()
        trailFadeAnimator = null
        trailAlpha = 200
    }





    private fun showPreview(ps: PointerState, sx: Float, sy: Float, char: Char) {
        ps.previewChar    = if (isShifted) char.uppercaseChar() else char
        ps.previewScreenX = sx
        ps.previewScreenY = sy
        ps.showPreview    = true
    }

    private fun hidePreview(ps: PointerState) {
        ps.showPreview = false
        ps.previewChar = null
    }

    private fun updatePreviewForPosition(ps: PointerState, sx: Float, sy: Float): Boolean {
        ps.lastX = sx; ps.lastY = sy
        val g = geometry ?: return false
        val tx = sx2tx(sx); val ty = sy2ty(sy)
        if (getSpecialKeyAt(tx, ty) != null) {
            val changed = ps.showPreview
            hidePreview(ps)
            return changed
        }
        val c  = g.tapKeyChar(tx, ty) ?: return false
        val kc = g.getKeyCenter(c) ?: return false
        val old = ps.previewChar
        ps.previewChar    = if (isShifted) c.uppercaseChar() else c
        ps.previewScreenX = tx2sx(kc.first)
        ps.previewScreenY = ty2sy(kc.second)
        ps.showPreview    = true
        return ps.previewChar != old
    }





    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN         -> { PerformanceTracker.onTouchDown(); handleDown(event);     true }
            MotionEvent.ACTION_POINTER_DOWN -> { handlePointerDown(event);  true }
            MotionEvent.ACTION_MOVE         -> { handleMoveInternal(event); true }
            MotionEvent.ACTION_POINTER_UP   -> { handlePointerUp(event);    true }
            MotionEvent.ACTION_UP           -> { handleUp(event);           true }
            MotionEvent.ACTION_CANCEL -> {
                cancelAllLongPresses()
                for (ps in activePointers.values) hidePreview(ps)
                resetAllState()
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    private fun handleDown(event: MotionEvent) {
        removeCallbacks(clearTrailRunnable)
        cancelTrailFade()
        gestureStartTime = event.eventTime
        isTouchActive    = true

        val pid = event.getPointerId(0)
        val calibratedY = event.y
        val ps  = PointerState(downX = event.x, downY = calibratedY,
                               lastX = event.x, lastY = calibratedY,
                               downTime = event.eventTime)
        activePointers[pid] = ps
        swipePointerId = pid


        rawPoints.clear(); processedPoints.clear()
        trailPath.reset(); trailStarted = false
        startPoints.clear(); stabilizedStartPoint = null
        detectedCorners.clear()
        filterX.reset(); filterY.reset()
        consecutiveSlowPoints = 0
        stableNearestKey = null; pendingNearestKey = null; pendingNearestKeyCount = 0

        updatePointerKey(ps, event.x, calibratedY)
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, event.getPressure(0))
        playSoundEffect(SoundEffectConstants.CLICK)

        handleSpecialKeyDown(event.x, calibratedY, ps)
        invalidate()
    }

    private fun handlePointerDown(event: MotionEvent) {
        val ai  = event.actionIndex
        val pid = event.getPointerId(ai)
        val px  = event.getX(ai); val py = event.getY(ai)
        val calibratedY = py
        val ps  = PointerState(downX = px, downY = calibratedY, lastX = px, lastY = calibratedY,
                               downTime = event.eventTime)
        activePointers[pid] = ps
        updatePointerKey(ps, px, calibratedY)
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, event.getPressure(ai))
        playSoundEffect(SoundEffectConstants.CLICK)
        handleSpecialKeyDown(px, calibratedY, ps)
        invalidate()
    }

    
    private fun handleSpecialKeyDown(sx: Float, sy: Float, ps: PointerState) {
        val tx = sx2tx(sx); val ty = sy2ty(sy)
        when (getSpecialKeyAt(tx, ty)) {
            "space"         -> { isSpacePressed = true; spaceLongPressTriggered = false; scheduleSpaceLongPress() }
            "symbol_toggle" -> { isSymbolTogglePressed = true; symbolToggleLongPressTriggered = false; scheduleSymbolToggleLongPress() }
            "backspace"     -> { isBackspacePressed = true; backspaceLongPressTriggered = false; scheduleBackspaceLongPress() }
            "period"        -> { isPeriodPressed = true; periodLongPressTriggered = false; schedulePeriodLongPress() }
            "comma"         -> { isCommaPressed = true; commaLongPressTriggered = false; scheduleCommaLongPress() }

            "enter"         -> {  }
            null -> {
                val g  = geometry ?: return
                val c  = g.tapKeyChar(tx, ty) ?: return
                val kc = g.getKeyCenter(c) ?: return
                showPreview(ps, tx2sx(kc.first), ty2sy(kc.second), c)
                
                if (!isSymbolMode) {
                    val cLower = c.lowercaseChar()
                    val hintIdx = topRowChars.indexOf(cLower)
                    if (hintIdx != -1) {
                        alphaKeyLongPressTriggered = false
                        scheduleAlphaKeyLongPress(topRowHints[hintIdx])
                    }
                }
            }
        }
    }

    private fun handleMoveInternal(event: MotionEvent) {
        if (!isTouchActive) return
        var needsInvalidate = false

        for (i in 0 until event.pointerCount) {
            val pid = event.getPointerId(i)
            val ps  = activePointers[pid] ?: continue
            val cx  = event.getX(i)
            val cy  = event.getY(i)


            if (pid == swipePointerId && isBackspacePressed) {
                if (!isScreenOnBackspace(cx, cy)) {
                    cancelBackspaceLongPress()
                    onBackspaceUp?.invoke()
                    isBackspacePressed = false
                    backspaceLongPressTriggered = false
                }
                updatePointerKey(ps, cx, cy); needsInvalidate = true
                continue
            }

            if (pid == swipePointerId && isSpacePressed) {
                val dx = cx - ps.lastX
                spaceMoveAccumulatedX += dx
                

                if (!isSpaceMovingUI && abs(spaceMoveAccumulatedX) > (KeyboardConstants.CURSOR_START_THRESHOLD_DP * resources.displayMetrics.density)) {
                    isSpaceMovingUI = true
                }
                
                spaceSliderX = cx
                onSpaceMove?.invoke(dx)
                ps.lastX = cx; ps.lastY = cy
                invalidate()
                continue
            }


            if (activePointers.size > 1) {
                ps.swipeDisabled = true
            }

            val dx = cx - ps.downX; val dy = cy - ps.downY
            val distSq = dx * dx + dy * dy
            if (distSq < 16f && ps.currentNearestKey != null) continue

            val time      = event.eventTime - ps.downTime
            val velocity  = if (time > 0) sqrt(distSq) / time else 0f
            


            val slopSq = systemTouchSlop * systemTouchSlop
            val isSwipe = if (isSymbolMode || ps.swipeDisabled) false else {
                (velocity > HIGH_VELOCITY_THRESHOLD && distSq > slopSq) ||
                distSq > SWIPE_DELIBERATE_THRESHOLD_SQ
            }

            if (pid == swipePointerId && !ps.hasMovedEnoughForSwipe && isSwipe) {
                ps.hasMovedEnoughForSwipe = true
                ps.isSwipePointer = true

                cancelAlphaKeyLongPress()
            }


            if (ps.isSwipePointer && !ps.hasTriggeredSwipeStart && distSq > KeyboardConstants.SWIPE_START_THRESHOLD_SQ) {
                ps.hasTriggeredSwipeStart = true
                onSwipeStart?.invoke()
                for (anyPs in activePointers.values) hidePreview(anyPs)
            }

            if (pid == swipePointerId) {


                if (processedPoints.isEmpty()) {
                    processSwipePoint(ps.downX, ps.downY, ps.downTime, event.getPressure(i), forceAdd = true)
                }
                
                for (h in 0 until event.historySize) {
                    val calHistY = event.getHistoricalY(i, h)
                    processSwipePoint(event.getHistoricalX(i, h), calHistY,
                                      event.getHistoricalEventTime(h), event.getHistoricalPressure(i, h))
                }
                processSwipePoint(cx, cy, event.eventTime, event.getPressure(i))
                

                if (ps.hasTriggeredSwipeStart) {
                    needsInvalidate = true
                }
            } else {
                val tx = sx2tx(cx); val ty = sy2ty(cy)
                val newSpecial = getSpecialKeyAt(tx, ty)
                if (ps.currentSpecialKey != newSpecial) {
                    ps.currentSpecialKey = newSpecial
                    needsInvalidate = true
                }
                
                val old = ps.currentNearestKey
                updatePointerKey(ps, cx, cy)
                if (updatePreviewForPosition(ps, cx, cy) || ps.currentNearestKey != old) {
                    cancelAlphaKeyLongPress()
                    needsInvalidate = true
                }
            }
        }
        if (needsInvalidate) invalidate()
    }

    private fun handlePointerUp(event: MotionEvent) {
        val ai  = event.actionIndex
        val pid = event.getPointerId(ai)
        val ps  = activePointers.remove(pid) ?: return
        hidePreview(ps)

        val ux = event.getX(ai)
        val uy = event.getY(ai)
        handleSpecialKeyUp(pid, ux, uy, ps)
        if (pid == swipePointerId) swipePointerId = -1
        invalidate()
    }

    private fun handleUp(event: MotionEvent) {
        if (!isTouchActive) return
        val pid = event.getPointerId(0)
        val ps  = activePointers.remove(pid) ?: run { resetTouchState(); invalidate(); return }
        hidePreview(ps)

        val ux = event.x
        val uy = event.y
        handleSpecialKeyUp(pid, ux, uy, ps)

        resetTouchState()
        invalidate()
        postDelayed(clearTrailRunnable, TRAIL_FADE_DELAY_MS)
    }

    
    private fun handleSpecialKeyUp(pid: Int, ux: Float, uy: Float, ps: PointerState) {
        if (pid == swipePointerId) {
            when {
                isBackspacePressed -> {
                    cancelBackspaceLongPress()
                    onBackspaceUp?.invoke()
                    if (!backspaceLongPressTriggered) onBackspaceTap?.invoke()
                    isBackspacePressed = false; backspaceLongPressTriggered = false
                    return
                }
                isSpacePressed -> {
                    cancelSpaceLongPress()
                    onSpaceMoveEnd?.invoke()
                    if (!spaceLongPressTriggered) onSpaceTap?.invoke()
                    isSpacePressed = false; spaceLongPressTriggered = false
                    isSpaceMovingUI = false; spaceMoveAccumulatedX = 0f
                    invalidate()
                    return
                }
                isSymbolTogglePressed -> {
                    cancelSymbolToggleLongPress()
                    if (!symbolToggleLongPressTriggered) onSymbolToggleTap?.invoke()
                    isSymbolTogglePressed = false; symbolToggleLongPressTriggered = false
                    return
                }
                isPeriodPressed -> {
                    cancelPeriodLongPress()
                    if (!periodLongPressTriggered) onPeriodTap?.invoke()
                    isPeriodPressed = false; periodLongPressTriggered = false
                    return
                }
                isCommaPressed -> {
                    cancelCommaLongPress()
                    if (!commaLongPressTriggered) onCommaTap?.invoke()
                    isCommaPressed = false; commaLongPressTriggered = false
                    return
                }
            }
        }
        if (!ps.hasMovedEnoughForSwipe) {
            handleTap(ux, uy)
        } else if (pid == swipePointerId) {
            finishSwipe(ux, uy, System.currentTimeMillis(), 0.5f)
        }
    }






private fun finishSwipe(ux: Float, uy: Float, ts: Long, pressure: Float) {
    processSwipePoint(ux, uy, ts, pressure, forceAdd = true)

    Log.d(TAG, "finishSwipe: processedPoints.size=${processedPoints.size}")


    val totalPathDist = calculateTotalPathDistance(processedPoints)
    if (processedPoints.size < KeyboardConstants.MIN_POINTS_FOR_SWIPE || totalPathDist < KeyboardConstants.MIN_SWIPE_DISTANCE_PX) {
        Log.w(TAG, "finishSwipe: path too short (${totalPathDist.toInt()}px) or sparse (${processedPoints.size}pts), treating as tap")

        val ps = activePointers[swipePointerId]
        if (ps != null) handleTap(ps.downX, ps.downY) else handleTap(ux, uy)
        return
    }

    val rawPts = processedPoints.toList()
    Log.d(TAG, "Swipe finished: ${rawPts.size} raw points passed to decoder.")
    onSwipeComplete?.invoke(rawPts)
}

private fun calculateTotalPathDistance(points: List<FloatArray>): Float {
    var total = 0f
    for (i in 1 until points.size) {
        val dx = points[i][0] - points[i-1][0]
        val dy = points[i][1] - points[i-1][1]
        total += sqrt(dx * dx + dy * dy)
    }
    return total
}









private fun processSwipePoint(
    rawX: Float, rawY: Float, ts: Long,
    pressure: Float, forceAdd: Boolean = false
) {



    val drawX = filterX.filter(rawX, ts)
    val drawY = filterY.filter(rawY, ts)





    val tx = sx2tx(rawX)
    val ty = sy2ty(rawY)
    


val xPixel = tx.coerceIn(0f, TRAIN_WIDTH)
val yPixel = ty.coerceIn(0f, TRAIN_HEIGHT)




    val rawVx = filterX.getVelocity()
    val rawVy = filterY.getVelocity()
    val velMag = sqrt(rawVx * rawVx + rawVy * rawVy)

    if (velMag < PAUSE_VELOCITY_THRESHOLD) consecutiveSlowPoints++
    else consecutiveSlowPoints = 0

    val lp = rawPoints.lastOrNull()
    val tsl = if (lp != null) ts - lp.timestamp else 0L

    val isPause = (consecutiveSlowPoints >= 4 && tsl > MIN_PAUSE_TIME_MS) ||
        (lp?.isPause == true && tsl > 16L && velMag < PAUSE_VELOCITY_THRESHOLD * 2f)




    val currentNearest = geometry?.nearestKeyChar(tx, ty)
    val stableKeyChanged = updateStableKeyState(currentNearest, tx, ty)
    

    if (stableKeyChanged && trailStarted) {
        performHapticFeedback(HAPTIC_SWIPE_TICK, pressure)
    }






    val tp = TouchPoint(
        screenX = rawX,  screenY = rawY,
        trainX  = tx,    trainY  = ty,
        timestamp = ts,
        pressure  = pressure.coerceIn(0f, 1f),
        drawX = drawX,   drawY = drawY,
        vx = rawVx,      vy = rawVy,
        isPause = isPause,
        isStableKeyChange = stableKeyChanged,
        nearestKey = stableNearestKey ?: currentNearest
    )
    rawPoints.add(tp)




    if (startPoints.size < START_STABILIZATION_POINTS) {
        startPoints.add(tp)
        if (startPoints.size == START_STABILIZATION_POINTS) {
            stabilizedStartPoint = computeStabilizedStart()
        }
    }




    val prev = processedPoints.lastOrNull()
    if (forceAdd || shouldSamplePoint(tp, prev, velMag, stableKeyChanged)) {



        val relTs = (tp.timestamp - gestureStartTime).toFloat()
        processedPoints.add(floatArrayOf(xPixel, yPixel, relTs))


        if (KeyboardConstants.DEBUG_LOG_DECODE) {
            Log.v(TAG, "📍 True North Point: rawY=${"%.1f".format(rawY)} | ty=${"%.2f".format(yPixel)} | yRel=${"%.3f".format(yPixel / KeyboardConstants.TRAIN_HEIGHT)}")
            Log.v(TAG, "Point[${processedPoints.size-1}]: " +
                       "raw=(${"%.1f".format(rawX)},${"%.1f".format(rawY)}) " +
                       "pixel=(${"%.1f".format(xPixel)},${"%.1f".format(yPixel)}) " +
                       "t=${tp.timestamp}ms")
        }




        if (!trailStarted) {
            trailPath.moveTo(drawX, drawY)
            trailStarted = true
        } else {
            if (KeyboardConstants.TRAIL_SMOOTHING_ENABLED) {
                val lx = lp?.screenX ?: drawX
                val ly = lp?.screenY ?: drawY
                val calibratedLx = lx
                val calibratedLy = ly

                trailPath.quadTo(calibratedLx, calibratedLy, (calibratedLx + drawX) / 2f, (calibratedLy + drawY) / 2f)
            } else {
                trailPath.lineTo(drawX, drawY)
            }
        }
    }
}








private fun resampleIndexBased(points: List<FloatArray>, targetN: Int): List<FloatArray> {
    val n = points.size
    if (n < 2) return List(targetN) { points.getOrElse(0) { floatArrayOf(0f, 0f, 0f) }.copyOf() }

    return List(targetN) { i ->
        val idx = (i.toFloat() * (n - 1)) / (targetN - 1).toFloat()
        val low = Math.floor(idx.toDouble()).toInt()
        val high = Math.ceil(idx.toDouble()).toInt()
        val alpha = idx - low

        val p1 = points[low]
        val p2 = points[high]

        floatArrayOf(
            p1[0] * (1 - alpha) + p2[0] * (alpha.toFloat()),
            p1[1] * (1 - alpha) + p2[1] * (alpha.toFloat()),
            (i * KeyboardConstants.SAMPLING_CADENCE_MS).toFloat()
        )
    }
}


private fun resamplePivotAware(points: List<FloatArray>): List<FloatArray> {
    val n = points.size
    if (n < 2) return List(100) { points.getOrElse(0) { floatArrayOf(0f, 0f, 0f) } }

    val targetCount = 100
    val weights = FloatArray(n) { 1.0f }

    for (i in 1 until n - 1) {
        val p1 = points[i - 1]
        val p  = points[i]
        val p2 = points[i + 1]

        val dx1 = p[0] - p1[0]
        val dy1 = p[1] - p1[1]
        val d1 = sqrt(dx1 * dx1 + dy1 * dy1)


        val speedW = 2.0f / (d1 + 0.1f)

        val dx2 = p2[0] - p[0]
        val dy2 = p2[1] - p[1]
        val d2 = sqrt(dx2 * dx2 + dy2 * dy2)

        var turnW = 0.0f
        if (d1 > 0.05f && d2 > 0.05f) {

            val cosVal = ((dx1 * dx2 + dy1 * dy2) / (d1 * d2)).coerceIn(-1.0f, 1.0f)
            turnW = acos(cosVal).toFloat() * 5.0f
        }

        weights[i] = 1.0f + speedW + turnW
    }


    weights[0] = 5.0f
    weights[n - 1] = 5.0f

    val cum = FloatArray(n)
    var sum = 0f
    for (i in 0 until n) {
        sum += weights[i]
        cum[i] = sum
    }

    val result = mutableListOf<FloatArray>()
    for (i in 0 until targetCount) {
        val tw = (i * sum) / (targetCount - 1)
        
        var idx = 0
        while (idx < n && cum[idx] < tw) idx++
        
        if (idx == 0) {
            result.add(points[0].copyOf())
        } else if (idx >= n) {
            result.add(points[n-1].copyOf())
        } else {
            val prevCum = cum[idx - 1]
            val alpha = (tw - prevCum) / (cum[idx] - prevCum)
            val p1 = points[idx - 1]
            val p2 = points[idx]
            
            result.add(floatArrayOf(
                p1[0] * (1f - alpha) + p2[0] * alpha,
                p1[1] * (1f - alpha) + p2[1] * alpha,
                (i * 20).toFloat()
            ))
        }
    }
    return result
}


    private fun updateStableKeyState(currentNearest: Char?, tx: Float, ty: Float): Boolean {
        val g = geometry ?: return false
        if (currentNearest == null) return false

        if (stableNearestKey == null) {
            stableNearestKey = currentNearest
            pendingNearestKey = null; pendingNearestKeyCount = 0
            return true
        }
        if (currentNearest == stableNearestKey) {
            pendingNearestKey = null; pendingNearestKeyCount = 0
            return false
        }

        val stableCenter = g.getKeyCenter(stableNearestKey ?: currentNearest)
        val distFromStable = if (stableCenter != null) {
            val dx = tx - stableCenter.first; val dy = ty - stableCenter.second
            sqrt(dx * dx + dy * dy)
        } else Float.MAX_VALUE

        if (pendingNearestKey == currentNearest) pendingNearestKeyCount++
        else { pendingNearestKey = currentNearest; pendingNearestKeyCount = 1 }

        val currentCenter = g.getKeyCenter(currentNearest)
        val distFromCurrent = if (currentCenter != null) {
            val dx = tx - currentCenter.first; val dy = ty - currentCenter.second
            sqrt(dx * dx + dy * dy)
        } else Float.MAX_VALUE

        val shouldCommit =
            pendingNearestKeyCount >= STABLE_KEY_CONFIRM_COUNT ||
            distFromStable >= STABLE_KEY_MIN_DISTANCE ||
            (pendingNearestKeyCount >= 1 && distFromCurrent < CENTER_PROXIMITY_COMMIT_DISTANCE)

        return if (shouldCommit) {
            stableNearestKey = currentNearest
            pendingNearestKey = null; pendingNearestKeyCount = 0
            true
        } else false
    }




private fun shouldSamplePoint(
    c: TouchPoint,
    prev: FloatArray?,
    velMag: Float,
    isStableKeyChange: Boolean
): Boolean {
    if (prev == null) return true
    

    if (isStableKeyChange) return true
    

    val dx = sx2tx(c.screenX) - (prev[0])
    val dy = sy2ty(c.screenY) - (prev[1])
    val distSq = dx * dx + dy * dy
    
    val thresholdSq = when {
        velMag > HIGH_VELOCITY_THRESHOLD -> SAMPLE_DIST_HIGH_SPEED_SQ
        velMag > LOW_VELOCITY_THRESHOLD  -> SAMPLE_DIST_MEDIUM_SPEED_SQ
        else -> SAMPLE_DIST_LOW_SPEED_SQ
    }
    
    if (distSq >= thresholdSq) return true
    

    if (c.isPause) return true
    
    return false
}





    private fun computeStabilizedStart(): TouchPoint {
        if (startPoints.isEmpty()) return rawPoints.first()
        var sx = 0f; var sy = 0f; var ws = 0f
        for (p in startPoints) {
            val w = 1f + p.pressure * TOUCH_SIZE_WEIGHT
            sx += p.trainX * w; sy += p.trainY * w; ws += w
        }
        val atx = sx / ws; val aty = sy / ws
        return startPoints.first().copy(
            screenX = tx2sx(atx), screenY = ty2sy(aty),
            trainX  = atx,        trainY  = aty
        )
    }





    private fun handleTap(sx: Float, sy: Float) {
        val g  = geometry ?: return
        val tx = sx2tx(sx); val ty = sy2ty(sy)
        val special = g.hitTestSpecial(tx, ty)
        
        Log.d(TAG, "[@${System.identityHashCode(this)}] handleTap: screen=($sx, $sy) train=($tx, $ty) special=$special")

        if (special != null) {
            when (special) {
                "shift"         -> onShiftTap?.invoke()
                "backspace"     -> onBackspaceTap?.invoke()
                "space"         -> onSpaceTap?.invoke()
                "enter"         -> onEnterTap?.invoke()
                "comma"         -> if (!commaLongPressTriggered) onCommaTap?.invoke()
                "period"        -> if (!periodLongPressTriggered) onPeriodTap?.invoke()
                "symbol_toggle" -> onSymbolToggleTap?.invoke()

            }
            return
        }
        val char = g.tapKeyChar(tx, ty) ?: return
        Log.d(TAG, "[@${System.identityHashCode(this)}] handleTap: baseChar='$char' isSymbolMode=$isSymbolMode symbolPage=$symbolPage")
        
        if (isSymbolMode) {
            val symbolSet = if (symbolPage == 1) KeyboardConstants.SYMBOL_PAGE_1 else KeyboardConstants.SYMBOL_PAGE_2
            val c = char.lowercaseChar()
            val resolved = when {
                "qwertyuiop".contains(c) -> symbolSet[0].getOrNull("qwertyuiop".indexOf(c)) as? String
                "asdfghjkl".contains(c) -> symbolSet[1].getOrNull("asdfghjkl".indexOf(c)) as? String
                "zxcvbnm".contains(c) -> symbolSet[2].getOrNull("zxcvbnm".indexOf(c)) as? String
                else -> null
            }
            Log.d(TAG, "[@${System.identityHashCode(this)}] handleTap: resolvedSymbol='$resolved'")
            if (resolved != null) {
                if (!alphaKeyLongPressTriggered) onKeyTap?.invoke(resolved[0])
            }
        } else {
            if (!alphaKeyLongPressTriggered) onKeyTap?.invoke(if (isShifted) char.uppercaseChar() else char.lowercaseChar())
        }
    }

    private fun updatePointerKey(ps: PointerState, sx: Float, sy: Float) {
        val tx = sx2tx(sx); val ty = sy2ty(sy)
        
        ps.currentSpecialKey = getSpecialKeyAt(tx, ty)
        ps.currentNearestKey = if (ps.currentSpecialKey == null) geometry?.nearestKeyChar(tx, ty) else null
    }





    private fun scheduleBackspaceLongPress() {
        cancelBackspaceLongPress()
        longPressRunnable = Runnable {
            if (isBackspacePressed && isTouchActive) {
                backspaceLongPressTriggered = true
                onBackspaceLongPressStart?.invoke()
            }
        }
        handler.postDelayed(longPressRunnable!!, LONG_PRESS_TIMEOUT_MS)
    }

    private fun cancelBackspaceLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun scheduleSpaceLongPress() {
        cancelSpaceLongPress()
        spaceLongPressRunnable = Runnable {
            if (isSpacePressed && isTouchActive) {
                spaceLongPressTriggered = true; onSpaceLongPress?.invoke()
            }
        }
        handler.postDelayed(spaceLongPressRunnable!!, LONG_PRESS_TIMEOUT_MS)
    }

    private fun cancelSpaceLongPress() {
        spaceLongPressRunnable?.let { handler.removeCallbacks(it) }
        spaceLongPressRunnable = null
    }

    private fun scheduleSymbolToggleLongPress() {
        cancelSymbolToggleLongPress()
        symbolToggleLongPressRunnable = Runnable {
            if (isSymbolTogglePressed && isTouchActive) {
                symbolToggleLongPressTriggered = true; onSymbolToggleLongPress?.invoke()
            }
        }
        handler.postDelayed(symbolToggleLongPressRunnable!!, LONG_PRESS_TIMEOUT_MS)
    }

    private fun cancelSymbolToggleLongPress() {
        symbolToggleLongPressRunnable?.let { handler.removeCallbacks(it) }
        symbolToggleLongPressRunnable = null
    }
    
    private var periodLongPressRunnable: Runnable? = null
    private fun schedulePeriodLongPress() {
        cancelPeriodLongPress()
        periodLongPressRunnable = Runnable {
            periodLongPressTriggered = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onPeriodLongPress?.invoke()
        }
        handler.postDelayed(periodLongPressRunnable!!, LONG_PRESS_TIMEOUT_MS)
    }
    private fun cancelPeriodLongPress() {
        periodLongPressRunnable?.let { handler.removeCallbacks(it) }
        periodLongPressRunnable = null
    }

    private var commaLongPressRunnable: Runnable? = null
    private fun scheduleCommaLongPress() {
        cancelCommaLongPress()
        commaLongPressRunnable = Runnable {
            commaLongPressTriggered = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onCommaLongPress?.invoke()
        }
        handler.postDelayed(commaLongPressRunnable!!, LONG_PRESS_TIMEOUT_MS)
    }
    private fun cancelCommaLongPress() {
        commaLongPressRunnable?.let { handler.removeCallbacks(it) }
        commaLongPressRunnable = null
    }

    private fun scheduleAlphaKeyLongPress(charToEmit: Char) {
        cancelAlphaKeyLongPress()
        alphaKeyLongPressRunnable = Runnable {
            if (isTouchActive) {
                alphaKeyLongPressTriggered = true
                onKeyLongPress?.invoke(charToEmit)
                val ps = activePointers[swipePointerId]
                if (ps != null) hidePreview(ps)
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                invalidate()
            }
        }
        handler.postDelayed(alphaKeyLongPressRunnable!!, LONG_PRESS_TIMEOUT_MS)
    }

    private fun cancelAlphaKeyLongPress() {
        alphaKeyLongPressRunnable?.let { handler.removeCallbacks(it) }
        alphaKeyLongPressRunnable = null
    }

    private fun cancelAllLongPresses() {
        cancelBackspaceLongPress()
        cancelSpaceLongPress()
        cancelSymbolToggleLongPress()
        cancelAlphaKeyLongPress()
        cancelPeriodLongPress()
        cancelCommaLongPress()
    }





    private fun resetTouchState() {
        isTouchActive               = false
        isBackspacePressed          = false
        isSpaceMovingUI             = false
        spaceMoveAccumulatedX       = 0f
        backspaceLongPressTriggered = false
        alphaKeyLongPressTriggered  = false
        periodLongPressTriggered    = false
        commaLongPressTriggered     = false
        isPeriodPressed             = false
        isCommaPressed              = false
        cancelAllLongPresses()
        activePointers.clear()
        swipePointerId = -1
    }

    private fun resetAllState() {
        resetTouchState()
        cancelTrailFade()
        rawPoints.clear(); processedPoints.clear()
        trailPath.reset(); trailStarted = false
        startPoints.clear(); stabilizedStartPoint = null
        detectedCorners.clear()
        filterX.reset(); filterY.reset()
        consecutiveSlowPoints = 0
        stableNearestKey = null; pendingNearestKey = null; pendingNearestKeyCount = 0
        invalidate()
    }

    fun clearTrail() {
        removeCallbacks(clearTrailRunnable)
        resetAllState()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAllLongPresses()
        cancelTrailFade()
        removeCallbacks(clearTrailRunnable)
        keyboardBitmap?.recycle()
        keyboardBitmap = null
        keyboardBitmapCanvas = null
    }

    fun performSuccessFeedback() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun performLightFeedback() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun performHapticFeedback(effectId: Int): Boolean {
        return performHapticFeedback(effectId, 0.5f)
    }

    
    fun performHapticFeedback(effectId: Int, pressure: Float): Boolean {
        if (!isHapticFeedbackEnabled) return false
        
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return false
        


        val p = pressure.coerceIn(0.1f, 0.9f)
        val scaleFactor = 0.7f + (p - 0.1f) * 0.75f 

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {



                val effect = when (effectId) {
                    HapticFeedbackConstants.LONG_PRESS  -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
                    HapticFeedbackConstants.VIRTUAL_KEY -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                    HapticFeedbackConstants.KEYBOARD_TAP-> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                    HAPTIC_SWIPE_TICK                   -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                    else -> null
                }
                if (effect != null) {
                    vibrator.vibrate(effect)
                    true
                } else {
                    super.performHapticFeedback(effectId)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val (baseDuration, baseAmp) = when (effectId) {
                    HapticFeedbackConstants.LONG_PRESS -> 12L to 160
                    HapticFeedbackConstants.VIRTUAL_KEY -> 8L to 120
                    HapticFeedbackConstants.KEYBOARD_TAP -> 5L to 100
                    HAPTIC_SWIPE_TICK -> 3L to 80
                    else -> 8L to 128
                }
                
                val finalAmp = (baseAmp * scaleFactor).toInt().coerceIn(1, 255)
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(baseDuration, finalAmp))
                true
            } else {
                super.performHapticFeedback(effectId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pressure-Haptic failure: ${e.message}")
            super.performHapticFeedback(effectId)
        }
    }




}