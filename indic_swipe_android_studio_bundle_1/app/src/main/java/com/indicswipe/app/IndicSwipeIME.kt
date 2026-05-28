package com.indicswipe.app

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.graphics.drawable.GradientDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import android.animation.ValueAnimator
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import android.net.Uri
import android.widget.ProgressBar
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.content.ClipDescription
import android.content.Intent

class IndicSwipeIME : InputMethodService(), android.content.SharedPreferences.OnSharedPreferenceChangeListener {





    private var themeManager: ThemeManager? = null
    private var currentTheme: ThemeManager.Theme? = null
    private var swipeView: SwipeView? = null
    private var keyboardContainer: View? = null
    private var suggestionBar: LinearLayout? = null
    private var suggestionBarScroll: HorizontalScrollView? = null
    private var suggestionDivider: View? = null
    private var debugTextView: TextView? = null
    private var rootView: View? = null

    private var symbolKeyboard: LinearLayout? = null
    private var symbolRow1: LinearLayout? = null
    private var symbolRow2: LinearLayout? = null
    private var symbolRow3: LinearLayout? = null

    private var emojiKeyboard: LinearLayout? = null
    private var emojiCategoryBar: LinearLayout? = null
    private var emojiCategoryScroll: HorizontalScrollView? = null
    private var emojiRecyclerView: RecyclerView? = null
    private var emojiBackspace: View? = null
    private var emojiBtnGif: View? = null
    private var emojiKeyboardBack: View? = null
    private var emojiSpace: TextView? = null
    private var emojiGridDivider: View? = null
    private var emojiAdapter: EmojiAdapter? = null
    
    private data class EmojiData(val emoji: String, val name: String, val category: String)
    private var allEmojiData: List<EmojiData> = emptyList()
    
    private var cachedEmojiItems: List<EmojiItem> = emptyList()

    private var stickyShiftContainer: FrameLayout? = null
    private var stickyBackspaceContainer: FrameLayout? = null
    private var stickyShiftButton: View? = null
    private var stickyBackspaceButton: View? = null

    private var btnSymbolToggle: TextView? = null
    private var btnEmojiToggle: TextView? = null
    private var btnComma: TextView? = null
    private var btnPeriod: TextView? = null
    private var btnSpace: TextView? = null
    private var btnBackspace: ImageView? = null
    private var btnEnter: ImageView? = null
    private var btnRomanizationToggle: FrameLayout? = null
    private var toggleThumb: View? = null
    private var toggleEn: TextView? = null
    private var toggleNative: TextView? = null
    private var bottomControlBar: View? = null


    private var mediaKeyboard: View? = null
    private var mediaSearchHeader: View? = null
    private var gifRecyclerView: RecyclerView? = null
    private var gifAdapter: GifAdapter? = null
    private var gifSearchText: TextView? = null
    private var gifSearchClear: View? = null
    private var gifSearchBar: View? = null
    private var gifCategoryBar: LinearLayout? = null
    private var gifLoadingSpinner: ProgressBar? = null
    private var gifEmptyState: View? = null
    private var gifStatusText: TextView? = null
    private var btnCancelMedia: View? = null
    private var mediaSearchQuery = StringBuilder()
    private var gifSearchJob: Job? = null
    private var isMediaSearchActive = false
    private var mediaSearchSymbolMode = false
    private lateinit var historyDb: SearchHistoryDbHelper


    private var btnSymbolSpace: TextView? = null
    private var btnSymbolEnter: ImageView? = null
    private var btnSymbolToggleBack: TextView? = null
    private var btnSymbolComma: TextView? = null
    private var btnSymbolPeriod: TextView? = null
    private var suggestionContainer: View? = null
    private var settingsManager: SettingsManager? = null

    private var swipeDecoder: SwipeDecoder? = null
    private var dictionaryManager: DictionaryManager? = null
    private var xlitDecoder: XlitDecoder? = null
    private var keyboardGeometry: KeyboardGeometry? = null
    private var serviceScope: CoroutineScope? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    private var isShiftEnabled = false
    private var isCapsLockEnabled = false
    private var lastShiftTapTime = 0L

    private var composingRomanText = ""
    private var currentHindiSuggestions: List<String> = emptyList()
    private var currentComposedHindi = ""
    private var accumulatedSpaceDx = 0f
    private var isSpaceMoving = false

    private var transliterationJob: Job? = null
    private var swipeDecodeJob: Job? = null
    private var languageReloadJob: Job? = null

    private var isSwipeComposing = false
    private var swipeComposedText = ""
    private var swipeAlternatives: List<String> = emptyList()
    private var swipeEnglishWord = ""
    private var lastRomanWord = ""

    private val PREF_LANG_INDEX = "selected_lang_index"
    private var currentLanguageIndex = 0
    private var loadedLanguageId: String? = null
    private val currentLanguage: KeyboardConstants.Language
        get() = KeyboardConstants.LANGUAGES[currentLanguageIndex]
    
    private var isRomanizedToggleOn = false
    
    private val isHindiMode: Boolean
        get() = currentLanguage.isHindiMode && !isRomanizedToggleOn
        
    private val languageMutex = Mutex()

    private enum class KeyboardMode { LETTERS, SYMBOLS, EMOJI, MEDIA }
    private var keyboardMode = KeyboardMode.LETTERS

    private var symbolPage = 1
    private var currentEmojiCategory = 1
    private val recentEmojis = mutableListOf<String>()
    private val maxRecentEmojis = 40

    private var backspaceRepeatJob: Job? = null
    private var punctuationPopup: PopupWindow? = null
    private var themePickerPopup: PopupWindow? = null
    private var languagePickerPopup: PopupWindow? = null
    private var hintIndex = 0

    private val spaceBarHandler = Handler(Looper.getMainLooper())
    private val uiHandler = Handler(Looper.getMainLooper())

    private var cursorStartThresholdPx = 0f
    private var cursorMoveThresholdPx = 0f





override fun onCreate() {
    super.onCreate()
    serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    themeManager = ThemeManager(this)
    initVibrator()
    audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
    historyDb = SearchHistoryDbHelper(this)
    settingsManager = SettingsManager(this)
    val settingsPrefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE)
    settingsPrefs.registerOnSharedPreferenceChangeListener(this)

    val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
    currentLanguageIndex = prefs.getInt(PREF_LANG_INDEX, 0)

    cursorStartThresholdPx =
        KeyboardConstants.CURSOR_START_THRESHOLD_DP * resources.displayMetrics.density
    cursorMoveThresholdPx =
        KeyboardConstants.CURSOR_MOVE_THRESHOLD_DP * resources.displayMetrics.density

    serviceScope?.launch(Dispatchers.IO) {
        try {
            Log.d(KeyboardConstants.TAG, "Warmup starting...")

            if (keyboardGeometry == null) {
                keyboardGeometry = KeyboardGeometry(this@IndicSwipeIME)
                Log.d(KeyboardConstants.TAG, "Warmup: KeyboardGeometry loaded (keys=${keyboardGeometry?.keyCount})")
            }

            withContext(Dispatchers.Main) {
                updateDebugText("Ready")
                Log.d(KeyboardConstants.TAG, "Warmup complete ✅")
            }
        } catch (t: Throwable) {
            Log.e(KeyboardConstants.TAG, "Warmup failed: ${t.message}", t)
            withContext(Dispatchers.Main) { updateDebugText("Init error") }
        }
    }
}

    override fun onCreateInputView(): View {
        val themed = ContextThemeWrapper(this, applicationInfo.theme)
        rootView = LayoutInflater.from(themed).inflate(R.layout.keyboard_layout, null)
        bindViews()
        if (keyboardGeometry == null) keyboardGeometry = KeyboardGeometry(this)
        setupEmojiRecycler()
        setupSwipeView()
        setupControlButtons()
        setupEmojiBackspace()
        setupStickyActionButtons()
        setupMediaKeyboard()
        serviceScope?.launch(Dispatchers.IO) {
            prebuildEmojiCache()
        }
        

        preloadWebView()

        reloadLanguage()
        applyCurrentTheme()
        debugTextView?.visibility = View.GONE
        suggestionDivider?.visibility = View.VISIBLE
        return rootView!!
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun preloadWebView() {
        if (gifWebView == null) {
            gifWebView = android.webkit.WebView(this@IndicSwipeIME).apply {
                visibility = View.GONE
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"

                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                }

                cookieManager.setCookie(".google.com", "CONSENT=YES+cb.20240101-17-p0.en+FX+123")
            }
            (rootView as? android.view.ViewGroup)?.addView(gifWebView)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        clearAllCompositionState()
        swipeView?.clearTrail()
        stopBackspaceRepeat()
        currentTheme = themeManager?.currentTheme
        reloadLanguage()
        if (keyboardMode != KeyboardMode.LETTERS) showLettersMode()
        applyCurrentTheme()
        enableAutoShift()
        clearSuggestions()
        hintIndex++
        updateEnterKeyIcon()
    }
    
    private fun reloadLanguage() {
        val lang = currentLanguage
        

        updateSpaceBarLabel()
        updateSpaceBarAppearance()
        swipeView?.setLanguageInfo(lang.name, Color.parseColor(lang.accentColor), this.isHindiMode)
        applyCurrentTheme()
        updateRomanizationToggleAppearance()


        if (loadedLanguageId == lang.id) {
            return
        }
        loadedLanguageId = lang.id
        


        swipeDecoder?.clear()
        dictionaryManager?.clear()
        

        transliterationJob?.cancel()
        swipeDecodeJob?.cancel()
        



        languageReloadJob?.cancel()
        
        clearSuggestions()


        languageReloadJob = serviceScope?.launch {
            languageMutex.withLock {


                if (loadedLanguageId != lang.id) {
                    Log.d(KeyboardConstants.TAG, "⏭️ Skipping stale reload for: ${lang.id} (current: $loadedLanguageId)")
                    return@withLock
                }
                
                Log.d(KeyboardConstants.TAG, "🔄 Reloading assets for: ${lang.id}")
                

                val dm = dictionaryManager ?: withContext(Dispatchers.IO) { DictionaryManager(this@IndicSwipeIME).also { dictionaryManager = it } }
                val sd = swipeDecoder ?: withContext(Dispatchers.IO) { SwipeDecoder(this@IndicSwipeIME).also { swipeDecoder = it } }
                val xd = xlitDecoder ?: withContext(Dispatchers.IO) { XlitDecoder(this@IndicSwipeIME).also { xlitDecoder = it } }
                

                sd.geometry = keyboardGeometry
                sd.dictionaryManager = dm
                

                coroutineScope {
                    val swipeJob = async(Dispatchers.IO) {
                        sd.setLanguage(lang.assetFolder)
                    }
                    val dictJob = async(Dispatchers.IO) {
                        dm.setLanguage(lang.assetFolder)
                    }
                    val xlitJob = async(Dispatchers.IO) {


                        xd.setLanguage(lang.id)
                    }
                    
                    awaitAll(swipeJob, dictJob, xlitJob)
                }
                
                withContext(Dispatchers.Main) {
                    Log.i(KeyboardConstants.TAG, "✅ Language reload complete: ${lang.id}")
                }
            }
        }
    }



    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        



        if (candidatesStart == -1 && newSelStart == 0 && newSelEnd == 0) {
            if (isSwipeComposing || composingRomanText.isNotEmpty() || swipeDecodeJob?.isActive == true) {
                clearAllCompositionState()
                clearSuggestions()
            }
        }





        if (newSelStart == newSelEnd) {
            val isAtEnd = candidatesStart != -1 && newSelStart == candidatesEnd
            if (!isAtEnd && (isSwipeComposing || currentComposedHindi.isNotBlank() || composingRomanText.isNotBlank())) {
                currentInputConnection?.finishComposingText()
                clearAllCompositionState(cancelSwipe = true)
                clearSuggestions()
            }
        }


        if (newSelStart != newSelEnd) {
            if (isSwipeComposing || composingRomanText.isNotEmpty()) {
                currentInputConnection?.finishComposingText()
                clearAllCompositionState()
                clearSuggestions()
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        cancelPendingTransliteration()
        stopBackspaceRepeat()
        dismissAllPopups()
        clearSuggestions()
    }

    private fun dismissAllPopups() {
        punctuationPopup?.dismiss()
        themePickerPopup?.dismiss()
        languagePickerPopup?.dismiss()
        punctuationPopup = null
        themePickerPopup = null
        languagePickerPopup = null
    }

    override fun onDestroy() {
        cancelPendingTransliteration()
        stopBackspaceRepeat()
        dismissAllPopups()
        serviceScope?.cancel()
        serviceScope = null
        try { 
            swipeDecoder?.close() 
            xlitDecoder?.close()
        } catch (_: Exception) {}
        swipeDecoder = null
        xlitDecoder = null
        super.onDestroy()
    }





    private fun bindViews() {
        swipeView = rootView?.findViewById(R.id.swipe_view)
        suggestionBar = rootView?.findViewById(R.id.suggestion_bar)
        keyboardContainer = rootView?.findViewById(R.id.keyboard_container)
        suggestionBarScroll = rootView?.findViewById(R.id.suggestion_bar_scroll)
        debugTextView = rootView?.findViewById(R.id.debug_text)
        symbolKeyboard = rootView?.findViewById(R.id.symbol_keyboard)
        symbolRow1 = rootView?.findViewById(R.id.symbol_row_1)
        symbolRow2 = rootView?.findViewById(R.id.symbol_row_2)
        symbolRow3 = rootView?.findViewById(R.id.symbol_row_3)
        emojiKeyboard = rootView?.findViewById(R.id.emoji_keyboard)
        emojiCategoryBar = rootView?.findViewById(R.id.emoji_category_bar)
        emojiCategoryScroll = rootView?.findViewById(R.id.emoji_category_scroll)
        emojiRecyclerView = rootView?.findViewById(R.id.emoji_recycler_view)
        btnSymbolToggle = rootView?.findViewById(R.id.btn_symbol_toggle)
        btnComma = rootView?.findViewById(R.id.btn_comma)
        btnPeriod = rootView?.findViewById(R.id.btn_period)
        btnSpace = rootView?.findViewById(R.id.btn_space)
        btnEnter = rootView?.findViewById(R.id.btn_enter)
        btnRomanizationToggle = rootView?.findViewById(R.id.btn_romanization_toggle)
        toggleThumb = rootView?.findViewById(R.id.toggle_thumb)
        toggleEn = rootView?.findViewById(R.id.toggle_en)
        toggleNative = rootView?.findViewById(R.id.toggle_native)
        bottomControlBar = rootView?.findViewById(R.id.bottom_control_bar)
        suggestionContainer = rootView?.findViewById(R.id.suggestion_container)
        rootView?.findViewById<View>(R.id.suggestion_divider)?.let { suggestionDivider = it }


        btnSymbolSpace = rootView?.findViewById(R.id.symbol_btn_space)
        btnSymbolEnter = rootView?.findViewById(R.id.symbol_btn_enter)
        btnSymbolToggleBack = rootView?.findViewById(R.id.symbol_btn_toggle)
        btnSymbolComma = rootView?.findViewById(R.id.symbol_btn_comma)
        btnSymbolPeriod = rootView?.findViewById(R.id.symbol_btn_period)
        

        emojiKeyboardBack = rootView?.findViewById(R.id.emoji_btn_back)
        emojiBackspace = rootView?.findViewById(R.id.emoji_btn_del)
        emojiBtnGif = rootView?.findViewById(R.id.emoji_btn_gif)
        
        emojiBtnGif?.setOnClickListener {
            showMediaMode()
            performSuccessFeedback()
        }
        


        btnBackspace = null
        btnEmojiToggle = null


        mediaKeyboard = rootView?.findViewById(R.id.media_keyboard)
        mediaSearchHeader = rootView?.findViewById(R.id.media_search_header)
        gifRecyclerView = rootView?.findViewById(R.id.gif_recycler_view)
        gifSearchText = rootView?.findViewById(R.id.gif_search_text)
        gifSearchClear = rootView?.findViewById(R.id.gif_search_clear)
        gifSearchBar = rootView?.findViewById(R.id.gif_search_bar)
        gifCategoryBar = rootView?.findViewById(R.id.gif_category_bar)
        gifLoadingSpinner = rootView?.findViewById(R.id.gif_loading_spinner)
        gifEmptyState = rootView?.findViewById(R.id.gif_empty_state)
        gifStatusText = rootView?.findViewById(R.id.gif_status_text)
        btnCancelMedia = rootView?.findViewById(R.id.btn_cancel_media)
    }

private fun getSwipeDecoder(): SwipeDecoder {
    if (keyboardGeometry == null) {
        keyboardGeometry = KeyboardGeometry(this)
        Log.d(KeyboardConstants.TAG, "getSwipeDecoder(): created KeyboardGeometry")
    }
    if (swipeDecoder == null) {
        swipeDecoder = SwipeDecoder(this)
        Log.d(KeyboardConstants.TAG, "getSwipeDecoder(): created SwipeDecoder")
    }


    val sd = swipeDecoder ?: SwipeDecoder(this).also { swipeDecoder = it }
    sd.geometry = keyboardGeometry
    sd.dictionaryManager = dictionaryManager
    return sd
}





    private fun setupSwipeView() {
        swipeView?.let { v ->
            keyboardGeometry?.let { g -> v.setKeyboardGeometry(g) }
            v.setShifted(isShiftEnabled, isCapsLockEnabled)
            v.onKeyTap = { c -> 
                if (keyboardMode == KeyboardMode.SYMBOLS) {
                    commitCurrentCompositionWithoutSpace()
                    commitRawText(c.toString())
                    performSuccessFeedback()
                } else {
                    handleLetterInput(c) 
                }
            }
            v.onKeyLongPress = { c -> handleKeyLongPress(c) }
            v.onSwipeStart = { onSwipeStart() }
            v.onSwipeComplete = { p -> handleSwipeComplete(p) }
            v.onShiftTap = { handleShiftTap() }
            v.onBackspaceTap = { handleBackspace() }
            v.onBackspaceLongPressStart = { startBackspaceRepeat() }
            v.onBackspaceUp = { stopBackspaceRepeat() }
            v.onSpaceTap = { handleSpacePress(); performSuccessFeedback() }
            v.onSpaceLongPress = { showLanguagePickerPopup(); performSuccessFeedback() }
            v.onSpaceMove = { dx -> handleSpaceMove(dx) }
            v.onSpaceMoveEnd = { 
                isSpaceMoving = false
                accumulatedSpaceDx = 0f
                updateSpaceBarLabel()
                updateSpaceBarAppearance() 
            }
            v.onCommaTap = {
                commitCurrentCompositionWithoutSpace()
                commitRawText(",")
                performSuccessFeedback()
                checkAutoCapitalize()
            }
            v.onPeriodTap = {
                commitCurrentCompositionWithoutSpace()
                commitRawText(".")
                enableAutoShift()
                performSuccessFeedback()
            }
            v.onPeriodLongPress = { showPunctuationPopup() }
            v.onCommaLongPress = { showEmojiMode(); performSuccessFeedback() }
            v.onEnterTap = { handleEnter(); performSuccessFeedback() }
            v.onSymbolToggleTap = {
                commitCurrentCompositionWithoutSpace()
                if (keyboardMode == KeyboardMode.LETTERS) {
                    showSymbolsMode(1)
                } else {
                    showLettersMode()
                }
                performSuccessFeedback()
            }
            v.onSymbolToggleLongPress = { showThemePicker(); performSuccessFeedback() }
            v.onShiftTap = {

                if (keyboardMode == KeyboardMode.SYMBOLS) {
                    symbolPage = if (symbolPage == 1) 2 else 1
                    showSymbolsMode(symbolPage)
                    performSuccessFeedback()
                } else {
                    handleShiftTap()
                }
            }
        }
    }

    private fun setupEmojiRecycler() {
        val rv = emojiRecyclerView ?: return
        
        rv.layoutManager = GridLayoutManager(this, 8).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val adapter = rv.adapter as? EmojiAdapter ?: return 1
                    val isHeader = adapter.getItemViewType(position) == 0
                    if (isHeader) return 8
                    val item = adapter.getItemAt(position)

                    return if (item is EmojiItem.Emoji && item.isKaomoji) 2 else 1
                }
            }
        }
        
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastSyncedCategory = -1
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                if (Math.abs(dy) < 10) return
                
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible == RecyclerView.NO_POSITION) return
                
                val categoryId = (rv.adapter as? EmojiAdapter)?.getCategoryIdAt(firstVisible) ?: return
                if (categoryId != lastSyncedCategory) {
                    lastSyncedCategory = categoryId
                    currentEmojiCategory = categoryId
                    buildEmojiCategoryBar()
                }
            }
        })
        
        rv.setHasFixedSize(true)
        rv.itemAnimator = null
        if (rv.itemDecorationCount == 0) {
            rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
                private val spacing = (2 * resources.displayMetrics.density).toInt()
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(spacing, spacing, spacing, spacing)
                }
            })
        }
        emojiAdapter = EmojiAdapter { emoji ->
            commitEmoji(emoji)
            performSuccessFeedback()
        }
        rv.adapter = emojiAdapter
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupControlButtons() {
        btnSymbolToggle?.setOnClickListener { toggleSymbolMode(); performSuccessFeedback() }
        btnSymbolToggle?.setOnLongClickListener { showThemePicker(); performSuccessFeedback(); true }
        
        btnComma?.setOnClickListener {
            commitCurrentCompositionWithoutSpace()
            commitRawText(",")
            performSuccessFeedback()
            checkAutoCapitalize()
        }
        btnComma?.setOnLongClickListener { showEmojiMode(); performSuccessFeedback(); true }
        
        btnPeriod?.setOnClickListener {
            commitCurrentCompositionWithoutSpace()
            commitRawText(".")
            enableAutoShift()
            performSuccessFeedback()
        }

        btnPeriod?.setOnLongClickListener { 
            showEmojiMode(); 
            performSuccessFeedback(); 
            true 
        }
        
        btnEnter?.setOnClickListener { handleEnter(); performSuccessFeedback() }


        btnSymbolEnter?.setOnClickListener { handleEnter(); performSuccessFeedback() }
        btnSymbolToggleBack?.setOnClickListener { showLettersMode(); performSuccessFeedback() }
        btnSymbolComma?.setOnClickListener { commitRawText(","); performSuccessFeedback() }
        btnSymbolPeriod?.setOnClickListener { commitRawText("."); performSuccessFeedback() }
        btnSymbolSpace?.setOnClickListener { handleSpacePress(); performSuccessFeedback() }
        
        btnSymbolSpace?.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_UP) {
                handleSpacePress()
                performSuccessFeedback()
            }
            true
        }
        
        btnRomanizationToggle?.setOnClickListener {
            isRomanizedToggleOn = !isRomanizedToggleOn
            updateRomanizationToggleAppearance(animate = true)
            


            commitCurrentCompositionWithoutSpace()
            
            clearAllCompositionState()
            clearSuggestions()
            swipeView?.setLanguageInfo(currentLanguage.name, Color.parseColor(currentLanguage.accentColor), isHindiMode)
            performSuccessFeedback()
        }
        


        var spaceDownX = 0f
        var spaceHasMoved = false
        var spaceLongPressTriggered = false
        var spaceLongPressJob: Job? = null

        btnSpace?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    spaceDownX = event.rawX
                    spaceHasMoved = false
                    spaceLongPressTriggered = false
                    spaceLongPressJob = serviceScope?.launch {
                        delay(KeyboardConstants.SPACE_LONG_PRESS_MS)
                        if (!spaceHasMoved) {
                            spaceLongPressTriggered = true
                            withContext(Dispatchers.Main) { showLanguagePickerPopup() }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - spaceDownX
                    if (!spaceHasMoved && abs(dx) > cursorStartThresholdPx) {
                        spaceHasMoved = true
                        spaceLongPressJob?.cancel()
                        accumulatedSpaceDx = 0f
                        commitCurrentCompositionWithoutSpace()
                        btnSpace?.text = "◄ ─── ►"
                    }
                    if (spaceHasMoved) {
                        accumulatedSpaceDx += event.rawX - spaceDownX
                        spaceDownX = event.rawX
                        if (abs(accumulatedSpaceDx) > cursorMoveThresholdPx) {
                            if (accumulatedSpaceDx > 0) sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                            else sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
                            accumulatedSpaceDx = 0f
                            performLightFeedback()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    spaceLongPressJob?.cancel()
                    if (spaceHasMoved) {
                        updateSpaceBarLabel()
                        updateSpaceBarAppearance()
                    } else if (!spaceLongPressTriggered) {
                        handleSpacePress()
                        performSuccessFeedback()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun handleSpaceMove(dx: Float) {
        if (dx == 0f) {
            accumulatedSpaceDx = 0f
            isSpaceMoving = false
            commitCurrentCompositionWithoutSpace()
            btnSpace?.text = "◄ ─── ►"
            return
        }
        
        accumulatedSpaceDx += dx
        

        if (!isSpaceMoving) {
            if (abs(accumulatedSpaceDx) > cursorStartThresholdPx) {
                isSpaceMoving = true
                commitCurrentCompositionWithoutSpace()
                btnSpace?.text = "◄ ─── ►"

                accumulatedSpaceDx = if (accumulatedSpaceDx > 0) accumulatedSpaceDx - cursorStartThresholdPx 
                                     else accumulatedSpaceDx + cursorStartThresholdPx
            } else {
                return
            }
        }
        



        var movesThisUpdate = 0
        val maxMovesPerUpdate = 2
        
        while (abs(accumulatedSpaceDx) > cursorMoveThresholdPx && movesThisUpdate < maxMovesPerUpdate) {
            if (accumulatedSpaceDx > 0) {
                sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                accumulatedSpaceDx -= cursorMoveThresholdPx
            } else {
                sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
                accumulatedSpaceDx += cursorMoveThresholdPx
            }
            movesThisUpdate++
        }
        

        if (movesThisUpdate > 0) {

            performHapticFeedback(2)
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupEmojiBackspace() {
        emojiBackspace?.setOnClickListener {
            handleBackspace()
            performHapticFeedback()
        }
        emojiBackspace?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) startBackspaceRepeat()
            else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) stopBackspaceRepeat()
            false
        }
        emojiKeyboardBack?.setOnClickListener {
            showLettersMode()
            performSuccessFeedback()
        }
    }

    private fun setupStickyActionButtons() {

    }

    private fun updateStickyLeftButton(): View {
        return View(this)
    }

    private fun refreshStickyButtons() {

    }





    private fun refreshAllUI() {
        refreshShiftState()
        updateSpaceBarLabel()
        updateSpaceBarAppearance()
        updateToggleLabels()
        updateRomanizationToggleAppearance()
        clearSuggestions()
    }

    private fun applyCurrentTheme() {
        val tm = themeManager ?: return
        val theme = tm.currentTheme
        val borders = tm.showBorders
        currentTheme = theme

        swipeView?.applyTheme(theme, borders)
        swipeView?.setKeyboardBackgroundColor(theme.keyboardBg)
        rootView?.setBackgroundColor(theme.keyboardBg)
        keyboardContainer?.setBackgroundColor(theme.keyboardBg)
        

        suggestionBar?.setBackgroundColor(theme.suggestionBarBg)
        suggestionDivider?.setBackgroundColor(theme.divider)
        suggestionDivider?.visibility = View.VISIBLE
        suggestionBarScroll?.setBackgroundColor(theme.suggestionBarBg)
        
        symbolKeyboard?.setBackgroundColor(theme.keyboardBg)
        emojiKeyboard?.setBackgroundColor(theme.keyboardBg)
        updateEnterKeyIcon()
        
        emojiRecyclerView?.setBackgroundColor(theme.keyboardBg)
        

        emojiCategoryScroll?.let {
            val bg = GradientDrawable().apply {

                setColor(theme.specialKeyBg)
                cornerRadius = KeyboardUIFactory.dp(this@IndicSwipeIME, theme.keyRadius * 2f).toFloat()
            }
            it.background = bg
            it.setPadding(0, 0, 0, 0)
        }
        
        emojiKeyboard?.setBackgroundColor(theme.keyboardBg)





        emojiKeyboardBack?.let {
            KeyboardUIFactory.styleEmojiNavButton(it, theme, borders, isIcon = true)
        }
        emojiBackspace?.let {
            KeyboardUIFactory.styleEmojiNavButton(it, theme, borders, isIcon = true)
        }
        emojiBtnGif?.let {
            KeyboardUIFactory.styleEmojiNavButton(it, theme, borders, isIcon = true)
        }
        emojiSpace?.let {
            it.background = KeyboardUIFactory.createThemedKeyBg(this, theme, borders, isAction = false)
            it.text = currentLanguage.name
            it.alpha = 0.8f
            if (it is TextView) it.setTextColor(theme.keyText)
        }
        

        val actionBg = KeyboardUIFactory.createThemedKeyBg(this, theme, borders, isAction = true)

        btnSymbolToggle?.background = actionBg
        btnSymbolToggle?.setTextColor(theme.keyText)

        btnComma?.background = KeyboardUIFactory.createThemedKeyBg(this, theme, borders)
        btnComma?.setTextColor(theme.keyText)

        btnPeriod?.background = KeyboardUIFactory.createThemedKeyBg(this, theme, borders)
        btnPeriod?.setTextColor(theme.keyText)

        btnEnter?.background = actionBg
        btnEnter?.imageTintList = ColorStateList.valueOf(theme.specialKeyIcon)
        updateEnterKeyIcon()


        btnSymbolToggle?.elevation = 0f
        btnComma?.elevation = 0f
        btnSpace?.elevation = 0f
        btnPeriod?.elevation = 0f
        btnEnter?.elevation = 0f

        updateSpaceBarAppearance()
        updateRomanizationToggleAppearance()
        
        emojiAdapter?.updateTheme(theme.keyText)
        emojiAdapter?.notifyDataSetChanged()

        when (keyboardMode) {
            KeyboardMode.LETTERS -> {
                swipeView?.visibility = View.VISIBLE
                swipeView?.setSymbolMode(false)
                symbolKeyboard?.visibility = View.GONE
                emojiKeyboard?.visibility = View.GONE
            }
            KeyboardMode.SYMBOLS -> {
                swipeView?.visibility = View.VISIBLE
                swipeView?.setSymbolMode(true, symbolPage)
                symbolKeyboard?.visibility = View.GONE
                emojiKeyboard?.visibility = View.GONE
            }
            KeyboardMode.EMOJI -> {
                swipeView?.visibility = View.GONE
                symbolKeyboard?.visibility = View.GONE
                emojiKeyboard?.visibility = View.VISIBLE
                buildEmojiCategoryBar()
                loadEmojiSubMode()
            }
            KeyboardMode.MEDIA -> {
                swipeView?.visibility = View.GONE
                symbolKeyboard?.visibility = View.GONE
                emojiKeyboard?.visibility = View.GONE
                mediaKeyboard?.visibility = View.VISIBLE
            }
        }

        updateToggleLabels()
        refreshSuggestionsForCurrentState()
        refreshStickyButtons()
        swipeView?.invalidate()
    }

    private fun updateEnterKeyIcon() {
        val info = currentInputEditorInfo ?: return
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        val inputType = info.inputType
        val isMultiLine = (inputType and android.view.inputmethod.EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val noEnterAction = (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        
        val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        val isSearchVariation = variation == android.text.InputType.TYPE_TEXT_VARIATION_FILTER || 
                                variation == 176
                                
        val hasSearchActionLabel = info.actionLabel?.toString()?.contains("search", ignoreCase = true) == true
        
        val hintText = info.hintText?.toString()?.lowercase() ?: ""
        val isHintSearch = hintText.contains("search") || hintText.contains("find")
        
        val iconRes = when {

            action == EditorInfo.IME_ACTION_SEARCH || isSearchVariation || hasSearchActionLabel || isHintSearch -> R.drawable.ic_search
            

            isMultiLine || noEnterAction -> R.drawable.ic_enter
            

            action == EditorInfo.IME_ACTION_SEND   -> R.drawable.ic_send
            action == EditorInfo.IME_ACTION_GO     -> R.drawable.ic_go
            action == EditorInfo.IME_ACTION_NEXT   -> R.drawable.ic_go
            action == EditorInfo.IME_ACTION_DONE   -> R.drawable.ic_enter
            

            else -> R.drawable.ic_enter
        }
        
        btnEnter?.setImageResource(iconRes)
        swipeView?.setEnterActionIcon(iconRes)
    }

    private fun handleEnter() {

        if (isMediaSearchActive) {
            handleMediaSearchEnter()
            return
        }

        commitCurrentCompositionWithoutSpace()
        
        val info = currentInputEditorInfo
        if (info != null) {
            val actionId = info.imeOptions and EditorInfo.IME_MASK_ACTION
            val noEnterAction = (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            
            if (actionId == EditorInfo.IME_ACTION_NONE || 
                actionId == EditorInfo.IME_ACTION_UNSPECIFIED || 
                noEnterAction) {
                sendKey(KeyEvent.KEYCODE_ENTER)
            } else {
                currentInputConnection?.performEditorAction(actionId)
            }
        } else {
            sendKey(KeyEvent.KEYCODE_ENTER)
        }
        
        enableAutoShift()
    }

    private fun handleKeyLongPress(c: Char) {
        commitCurrentCompositionWithoutSpace()
        commitRawText(c.toString())
        performSuccessFeedback()
    }

    private fun refreshSuggestionsForCurrentState() {
        when {
            isSwipeComposing -> updateSwipeSuggestions()
            composingRomanText.isNotBlank() ||
                    currentHindiSuggestions.isNotEmpty() -> updateTypingSuggestions()
            else -> clearSuggestions()
        }
    }

    private fun getLanguageColor(): Int =
        Color.parseColor(currentLanguage.accentColor)

    private fun updateSpaceBarAppearance() {
        val space = btnSpace ?: return
        val theme = currentTheme ?: return
        space.background = KeyboardUIFactory.createLanguageSpaceBarBg(
            this, theme, themeManager?.showBorders ?: false, getLanguageColor()
        )
        space.setTextColor(theme.keyText)
    }
    private fun showLanguageSwitchFeedback() {
        val space = btnSpace ?: return
        val radius = currentTheme?.keyRadius ?: 22f
        space.background = KeyboardUIFactory.createFlashBg(this, getLanguageColor(), KeyboardUIFactory.dp(this, radius).toFloat())
        space.setTextColor(Color.WHITE)
        

        space.animate().cancel()
        space.scaleX = 1f
        space.scaleY = 1f

        space.animate().scaleX(1.03f).scaleY(1.03f).setDuration(80).withEndAction {
            space.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
        }.start()
        
        space.text = currentLanguage.name
        performSuccessFeedback()
        

        showLanguageFlash(currentLanguage.name)
        
        uiHandler.removeCallbacksAndMessages(null)
        uiHandler.postDelayed(
            { updateSpaceBarLabel(); updateSpaceBarAppearance() },
            KeyboardConstants.LANGUAGE_FLASH_DURATION_MS
        )
    }
    
    private fun showLanguageFlash(label: String) {
        val root = rootView ?: return
        val theme = currentTheme ?: return
        

        val flashView = TextView(this).apply {
            text = label
            textSize = 24f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            
            val paddingH = 32 * resources.displayMetrics.density.toInt()
            val paddingV = 16 * resources.displayMetrics.density.toInt()
            setPadding(paddingH, paddingV, paddingH, paddingV)
            
            background = GradientDrawable().apply {
                setColor(theme.accent.withAlpha(220))
                cornerRadius = KeyboardUIFactory.dp(this@IndicSwipeIME, theme.keyRadius * 2.5f).toFloat()
            }
            elevation = 20f
            alpha = 0f
            translationY = 50f
        }
        
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        (root as? FrameLayout)?.addView(flashView, params)
        
        flashView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200)
            .withEndAction {
                flashView.animate()
                    .alpha(0f)
                    .translationY(-50f)
                    .setStartDelay(KeyboardConstants.LANGUAGE_FLASH_DURATION_MS / 2)
                    .setDuration(300)
                    .withEndAction { (root as? FrameLayout)?.removeView(flashView) }
                    .start()
            }
            .start()
    }

    private fun showLanguagePickerPopup() {
        val root = rootView ?: return
        val theme = currentTheme ?: return
        

        val enabledIds = settingsManager?.getEnabledLanguageIds() ?: listOf("hindi")
        val enabledLangs = KeyboardConstants.LANGUAGES.filter { enabledIds.contains(it.id) }
        

        val displayIndex = enabledLangs.indexOfFirst { it.id == currentLanguage.id }.coerceAtLeast(0)


        val pickerView = KeyboardUIFactory.createLanguagePickerView(
            this, enabledLangs, displayIndex, theme,
            onSelect = { index ->
                val selectedLang = enabledLangs[index]
                val globalIndex = KeyboardConstants.LANGUAGES.indexOfFirst { it.id == selectedLang.id }
                if (globalIndex != -1) switchLanguage(globalIndex)
                languagePickerPopup?.dismiss()
            },
            onSettingsClick = {
                val intent = Intent(this, LanguageSettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                languagePickerPopup?.dismiss()
            }
        )
        

        languagePickerPopup = PopupWindow(
            pickerView,
            (root.width * 0.9f).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = KeyboardUIFactory.dp(this@IndicSwipeIME, 16f).toFloat()
            animationStyle = android.R.style.Animation_Dialog
            


            val metrics = resources.displayMetrics
            val screenHeight = metrics.heightPixels
            val keyboardHeight = root.height
            val centerYOffset = -(screenHeight / 2 - keyboardHeight / 2)

            showAtLocation(root, Gravity.CENTER, 0, centerYOffset)
        }
        
        performHapticFeedback(15)
    }


    private fun switchLanguage(index: Int) {
        commitCurrentCompositionWithoutSpace()
        clearAllCompositionState()
        clearSuggestions()
        
        currentLanguageIndex = index
        getSharedPreferences("theme_prefs", MODE_PRIVATE).edit().putInt(PREF_LANG_INDEX, index).apply()
        isRomanizedToggleOn = false
        updateRomanizationToggleAppearance()
        showLanguageSwitchFeedback()
        reloadLanguage()
    }





    private fun updateSpaceBarLabel() {
        btnSpace?.text = currentLanguage.name
    }

    private fun toggleLanguageMode() {
        showLanguagePickerPopup()
    }

    private fun toggleSymbolMode() {
        commitCurrentCompositionWithoutSpace()
        if (keyboardMode == KeyboardMode.LETTERS) {
            showSymbolsMode(1)
        } else {
            showLettersMode()
        }
    }

    private fun toggleEmojiMode() {
        if (keyboardMode == KeyboardMode.EMOJI) showLettersMode()
        else { commitCurrentCompositionWithoutSpace(); showEmojiMode() }
    }

    private fun showLettersMode() {
        if (keyboardMode == KeyboardMode.LETTERS) return
        animateKeyboardHeight(KeyboardConstants.KEYBOARD_HEIGHT_DP)
        keyboardMode = KeyboardMode.LETTERS


        mediaSearchHeader?.visibility = View.GONE
        suggestionBarScroll?.visibility = View.VISIBLE
        btnRomanizationToggle?.visibility = View.VISIBLE
        suggestionBar?.visibility = View.VISIBLE
        suggestionContainer?.visibility = View.VISIBLE
        suggestionDivider?.visibility = View.VISIBLE

        hideAllKeyboards()
        swipeView?.visibility = View.VISIBLE
        swipeView?.setSymbolMode(false)
        updateToggleLabels()
        refreshStickyButtons()
    }

    private fun hideAllKeyboards() {
        swipeView?.visibility = View.GONE
        symbolKeyboard?.visibility = View.GONE
        emojiKeyboard?.visibility = View.GONE
        mediaKeyboard?.visibility = View.GONE
        mediaSearchHeader?.visibility = View.GONE
    }

    private fun showSymbolsMode(page: Int = 1) {
        animateKeyboardHeight(KeyboardConstants.KEYBOARD_HEIGHT_DP)
        keyboardMode = KeyboardMode.SYMBOLS
        symbolPage = page
        
        suggestionBar?.visibility = View.VISIBLE 
        

        isShiftEnabled = false
        isCapsLockEnabled = false
        swipeView?.setShifted(false, false)
        
        hideAllKeyboards()
        swipeView?.visibility = View.VISIBLE
        swipeView?.setSymbolMode(true, page)
        
        updateToggleLabels()
        refreshStickyButtons()
        swipeView?.invalidate()
    }


    private fun showEmojiMode() {
        if (keyboardMode == KeyboardMode.EMOJI) return 
        keyboardMode = KeyboardMode.EMOJI

        animateKeyboardHeight(384)
        
        hideAllKeyboards()
        suggestionContainer?.visibility = View.GONE
        suggestionDivider?.visibility = View.GONE
        emojiKeyboard?.visibility = View.VISIBLE
        
        loadEmojiSubMode() 
        applyCurrentTheme()
    }

    private fun animateKeyboardHeight(targetHeightDp: Int) {
        val container = keyboardContainer ?: return
        val targetPx = KeyboardUIFactory.dp(this, targetHeightDp)
        if (container.layoutParams.height == targetPx) return
        
        container.layoutParams.height = targetPx
        container.requestLayout()
    }

    private fun updateToggleLabels() {
        btnSymbolToggle?.text = if (keyboardMode == KeyboardMode.LETTERS) "?123" else "ABC"
        updateSpaceBarLabel()
    }

    private fun getNativeCharForLang(langId: String): String {
        return when (langId) {
            "tamil" -> "அ"
            "telugu" -> "అ"
            "kannada" -> "ಅ"
            "malayalam" -> "അ"
            "bengali", "assamese", "manipuri" -> "অ"
            "gujarati" -> "અ"
            "punjabi" -> "ਅ"
            "odia" -> "ଅ"
            "urdu", "sindhi_arab", "sindhi" -> "ا"
            "kashmir" -> "ک"
            "santali" -> "ᱚ"
            else -> "अ"
        }
    }

    private fun updateRomanizationToggleAppearance(animate: Boolean = false) {
        val container = btnRomanizationToggle ?: return
        val thumb = toggleThumb ?: return
        val tvEn = toggleEn ?: return
        val tvNative = toggleNative ?: return
        

        tvNative.text = getNativeCharForLang(currentLanguage.id)
        

        thumb.animate().cancel()


        container.post {
            val theme = currentTheme ?: return@post
            val langColor = Color.parseColor(currentLanguage.accentColor)
            
            val targetView = if (isRomanizedToggleOn) tvEn else tvNative
            

            if (targetView.width <= 0) {
                container.post { updateRomanizationToggleAppearance(animate) }
                return@post
            }
            
            val targetWidth = targetView.width
            val targetX = targetView.left.toFloat()


            val trackColor = when (theme.name) {
                "Light" -> ColorUtils.setAlphaComponent(theme.specialKeyBg, 255)
                "AMOLED" -> Color.parseColor("#1A1A1A")
                else -> ColorUtils.blendARGB(theme.keyboardBg, Color.WHITE, 0.12f)
            }
            val strokeColor = if (theme.name == "Light") theme.divider else ColorUtils.setAlphaComponent(theme.keyShadowColor, 150)

            container.background = GradientDrawable().apply {
                setColor(trackColor)
                cornerRadius = KeyboardUIFactory.dp(this@IndicSwipeIME, theme.keyRadius).toFloat()
                setStroke(KeyboardUIFactory.dp(this@IndicSwipeIME, 1f), strokeColor)
            }

            val activeBg = GradientDrawable().apply {
                setColor(langColor)
                cornerRadius = KeyboardUIFactory.dp(this@IndicSwipeIME, theme.keyRadius).toFloat()
            }
            thumb.background = activeBg
            
            val luminance = ColorUtils.calculateLuminance(langColor)
            val activeTextColor = if (luminance > 0.6) Color.parseColor("#2D2D2D") else Color.WHITE

            val enColor = if (isRomanizedToggleOn) activeTextColor else theme.textSecondary
            val nativeColor = if (!isRomanizedToggleOn) activeTextColor else theme.textSecondary
            
            if (animate) {
                val startWidth = thumb.width.takeIf { it > 0 } ?: targetWidth
                val widthAnim = ValueAnimator.ofInt(startWidth, targetWidth)
                widthAnim.addUpdateListener { anim ->
                    val lp = thumb.layoutParams
                    lp.width = anim.animatedValue as Int
                    thumb.layoutParams = lp
                }
                widthAnim.duration = 200

                thumb.animate()
                    .translationX(targetX)
                    .setDuration(200)
                    .withLayer()
                    .start()
                widthAnim.start()
                
                val enColorAnim = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), tvEn.currentTextColor, enColor)
                enColorAnim.addUpdateListener { tvEn.setTextColor(it.animatedValue as Int) }
                enColorAnim.duration = 200
                enColorAnim.start()
                
                val nativeColorAnim = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), tvNative.currentTextColor, nativeColor)
                nativeColorAnim.addUpdateListener { tvNative.setTextColor(it.animatedValue as Int) }
                nativeColorAnim.duration = 200
                nativeColorAnim.start()
            } else {
                val lp = thumb.layoutParams
                lp.width = targetWidth
                thumb.layoutParams = lp
                thumb.translationX = targetX
                
                tvEn.setTextColor(enColor)
                tvNative.setTextColor(nativeColor)
            }
            
            tvEn.background = null
            tvNative.background = null
        }
    }





    private fun buildSymbolRows() {
        val pg = if (symbolPage == 1) KeyboardConstants.SYMBOL_PAGE_1
        else KeyboardConstants.SYMBOL_PAGE_2
        val theme = currentTheme
        val borders = themeManager?.showBorders ?: false

        symbolRow1?.apply { 
            removeAllViews()
            weightSum = 10.0f
            pg[0].forEach { addView(createSymKey(it)) }
        }
        symbolRow2?.apply {
            removeAllViews()
            weightSum = 10.0f

            addView(View(this@IndicSwipeIME).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 0.5f) })
            pg[1].forEach { addView(createSymKey(it)) }
            addView(View(this@IndicSwipeIME).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 0.5f) })
        }
        symbolRow3?.apply {
            removeAllViews()
            weightSum = 10.0f
            

            val toggleLabel = if (symbolPage == 1) "1/2" else "2/2"
            addView(KeyboardUIFactory.createSymbolKey(this@IndicSwipeIME, toggleLabel, theme, borders, true) {
                symbolPage = if (symbolPage == 1) 2 else 1
                buildSymbolRows()
                performSuccessFeedback()
            }.apply {
                (layoutParams as LinearLayout.LayoutParams).weight = 1.5f
            })


            pg[2].forEach { addView(createSymKey(it)) }


            addView(KeyboardUIFactory.createIconButton(this@IndicSwipeIME, R.drawable.ic_backspace, theme, true) {
                handleBackspace()
            })
        }
    }

    private fun buildEmojiCategoryBar() {
        val bar = emojiCategoryBar ?: return
        bar.removeAllViews()
        

        bar.visibility = View.VISIBLE
        
        val langColor = try { Color.parseColor(currentLanguage.accentColor) } catch (e: Exception) { currentTheme?.accent ?: Color.RED }
        for ((i, cat) in KeyboardConstants.EMOJI_CATEGORIES.withIndex()) {

            if (i == 0 && recentEmojis.isEmpty()) continue
            
            bar.addView(
                KeyboardUIFactory.createEmojiTab(
                    this, cat.icon, i == currentEmojiCategory, currentTheme, langColor
                ) {
                    scrollToCategory(i)
                    performSuccessFeedback()
                }
            )
        }


    }

    private fun createSymKey(sym: String): View =
        KeyboardUIFactory.createSymbolKey(
            this, sym, currentTheme, themeManager?.showBorders ?: false, false
        ) { s ->
            commitRawText(s)
            performSuccessFeedback()
            if (s == "?" || s == "!") enableAutoShift()
        }

    private fun loadEmojiSubMode() {
        val scope = serviceScope ?: CoroutineScope(Dispatchers.Main)
        scope.launch {
            val items = withContext(Dispatchers.Default) {
                if (cachedEmojiItems.isEmpty()) prebuildEmojiCache()
                cachedEmojiItems
            }
            
            emojiAdapter?.submitList(items)
            emojiRecyclerView?.scrollToPosition(0)
            buildEmojiCategoryBar()
        }
    }

    private fun prebuildEmojiCache() {
        if (allEmojiData.isEmpty()) {
            loadEmojiDataFromJson()
        }
        generateCachedItems()
    }

    private fun loadEmojiDataFromJson() {
        try {
            val jsonString = assets.open("emojis.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONArray(jsonString)
            val allData = mutableListOf<EmojiData>()
            
            for (i in 0 until root.length()) {
                val group = root.getJSONObject(i)
                val groupName = group.getString("name")
                val groupEmojis = group.getJSONArray("emojis")
                
                for (j in 0 until groupEmojis.length()) {
                    val e = groupEmojis.getJSONObject(j)
                    val code = e.getString("emoji")
                    val name = e.getString("name")
                    allData.add(EmojiData(code, name, groupName))
                }
            }
            

            val cat = KeyboardConstants.EMOJI_CATEGORIES.last()
            cat.emojis.forEach { code ->
                allData.add(EmojiData(code, "kaomoji", "Kaomoji"))
            }
            
            allEmojiData = allData
            groupedEmojiData = emptyMap()
        } catch (e: Exception) {
            Log.e(KeyboardConstants.TAG, "Failed to load emojis.json", e)
        }
    }

    private var groupedEmojiData: Map<String, List<EmojiData>> = emptyMap()

    private fun generateCachedItems() {
        val emojis = mutableListOf<EmojiItem>()
        

        if (allEmojiData.isEmpty()) {
            KeyboardConstants.EMOJI_CATEGORIES.forEachIndexed { i, c ->
                emojis.add(EmojiItem.Header(c.label.uppercase(), i))
                val isKao = c.label == "Kaomoji"
                c.emojis.forEach { emojis.add(EmojiItem.Emoji(it, isKaomoji = isKao)) }
            }
            cachedEmojiItems = emojis
            return
        }


        if (recentEmojis.isNotEmpty()) {
            emojis.add(EmojiItem.Header("RECENT", 0))
            recentEmojis.forEach { emojis.add(EmojiItem.Emoji(it)) }
        }


        if (groupedEmojiData.isEmpty()) {
            groupedEmojiData = allEmojiData.groupBy { it.category }
        }
        
        groupedEmojiData.forEach { (category, data) ->
            if (category == "Kaomoji" || category == "kaomoji") return@forEach 
            

            val idx = when {
                category.contains("Smiley", ignoreCase = true) -> 1
                category.contains("People", ignoreCase = true) -> 2
                category.contains("Nature", ignoreCase = true) -> 3
                category.contains("Food", ignoreCase = true) -> 4
                category.contains("Activity", ignoreCase = true) || category.contains("Activities", ignoreCase = true) -> 5
                category.contains("Travel", ignoreCase = true) -> 6
                category.contains("Object", ignoreCase = true) -> 7
                category.contains("Symbol", ignoreCase = true) -> 8
                category.contains("Flag", ignoreCase = true) -> 9
                else -> 99
            }
            
            emojis.add(EmojiItem.Header(category.uppercase(), idx))
            data.forEach { emojis.add(EmojiItem.Emoji(it.emoji)) }
        }


        val kaomojiCat = KeyboardConstants.EMOJI_CATEGORIES.firstOrNull { it.label == "Kaomoji" }
        if (kaomojiCat != null) {
            val idx = KeyboardConstants.EMOJI_CATEGORIES.indexOf(kaomojiCat)
            emojis.add(EmojiItem.Header(kaomojiCat.label.uppercase(), idx))
            kaomojiCat.emojis.forEach { emojis.add(EmojiItem.Emoji(it, isKaomoji = true)) }
        }
        
        cachedEmojiItems = emojis
    }

    private fun scrollToCategory(categoryIndex: Int) {
        val adapter = emojiAdapter ?: return
        val rv = emojiRecyclerView ?: return
        

        for (i in 0 until adapter.itemCount) {
            val item = adapter.getItemAt(i)
            if (item is EmojiItem.Header && item.categoryId == categoryIndex) {
                (rv.layoutManager as? GridLayoutManager)?.scrollToPositionWithOffset(i, 0)
                currentEmojiCategory = categoryIndex
                buildEmojiCategoryBar()
                return
            }
        }
    }

    private fun loadEmojiCategory(categoryIndex: Int) {
        scrollToCategory(categoryIndex)
    }

    private fun commitEmoji(emoji: String) {
        commitCurrentCompositionWithoutSpace()
        currentInputConnection?.commitText(emoji, 1)
        

        recentEmojis.remove(emoji)
        recentEmojis.add(0, emoji)
        if (recentEmojis.size > maxRecentEmojis) recentEmojis.removeAt(recentEmojis.lastIndex)
        

        generateCachedItems()
        if (keyboardMode == KeyboardMode.EMOJI) {
            loadEmojiSubMode() 
        }
    }





    private fun handleLetterInput(char: Char) {


        if (isMediaSearchActive) {
            handleMediaSearchInput(char)
            return
        }

        cancelPendingSwipe()
        Log.d(KeyboardConstants.TAG, "handleLetterInput: char='$char' mode=$keyboardMode")


        if (char.isDigit() || !char.isLetter()) {
            commitCurrentCompositionWithoutSpace()
            commitRawText(char.toString())
            performSuccessFeedback()
            

            if (isShiftEnabled && !isCapsLockEnabled) {
                isShiftEnabled = false
                refreshShiftState()
            }
            return
        }

        if (keyboardMode == KeyboardMode.SYMBOLS) {
            val symbol = getSymbolForChar(char)
            if (symbol.isNotEmpty()) {
                commitRawText(symbol)
                performSuccessFeedback()
                if (symbol == "?" || symbol == "!") enableAutoShift()
            }
            return
        }



        if (composingRomanText.isEmpty() && (isSwipeComposing || currentComposedHindi.isNotBlank())) {
            commitCurrentCompositionWithSpace()
        }
        
        Log.d(KeyboardConstants.TAG, "handleLetterInput: char='$char' isHindiMode=$isHindiMode")
        
        val rc = if (isShiftEnabled || isCapsLockEnabled)
            char.uppercaseChar() else char.lowercaseChar()

        if (!isHindiMode) {

            composingRomanText += rc
            currentInputConnection?.setComposingText(composingRomanText, 1)
            updateTypingSuggestions() 
        } else {

            composingRomanText += rc.lowercaseChar()
            currentInputConnection?.setComposingText(composingRomanText, 1)
            scheduleTransliteration()
        }
        
        if (isShiftEnabled && !isCapsLockEnabled) {
            isShiftEnabled = false
            refreshShiftState()
        }
    }

    private fun getSymbolForChar(char: Char): String {
        val c = char.lowercaseChar()
        val symbolSet = if (symbolPage == 1) KeyboardConstants.SYMBOL_PAGE_1 else KeyboardConstants.SYMBOL_PAGE_2
        return when {
            "qwertyuiop".contains(c) -> symbolSet[0].getOrNull("qwertyuiop".indexOf(c)) ?: ""
            "asdfghjkl".contains(c) -> symbolSet[1].getOrNull("asdfghjkl".indexOf(c)) ?: ""
            "zxcvbnm".contains(c) -> symbolSet[2].getOrNull("zxcvbnm".indexOf(c)) ?: ""
            else -> ""
        }
    }

    private fun handleShiftTap() {
        if (keyboardMode == KeyboardMode.SYMBOLS) {
            symbolPage = if (symbolPage == 1) 2 else 1
            swipeView?.setSymbolMode(true, symbolPage)
            performSuccessFeedback()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastShiftTapTime < 300L) {
            isCapsLockEnabled = !isCapsLockEnabled
            isShiftEnabled = isCapsLockEnabled
        } else {
            if (isCapsLockEnabled) {
                isCapsLockEnabled = false
                isShiftEnabled = false
            } else {
                isShiftEnabled = !isShiftEnabled
            }
        }
        lastShiftTapTime = now
        refreshShiftState()
    }

    private fun handleBackspace() {

        if (isMediaSearchActive) {
            handleMediaSearchBackspace()
            return
        }

        val ic = currentInputConnection ?: return
        



        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.finishComposingText()
            ic.commitText("", 1)
            clearAllCompositionState()
            clearSuggestions()
            return
        }

        cancelPendingSwipe()
        if (isSwipeComposing) { 
            clearSwipeComposition() 
            return 
        }
        if (composingRomanText.isNotEmpty()) {
            composingRomanText = composingRomanText.dropLast(1)
            if (composingRomanText.isEmpty()) {
                currentComposedHindi = ""
                currentHindiSuggestions = emptyList()
                currentInputConnection?.finishComposingText()
                clearSuggestions()
                cancelPendingTransliteration()
            } else {
                currentInputConnection?.setComposingText(composingRomanText, 1)
                if (isHindiMode) scheduleTransliteration()
                else updateTypingSuggestions()
            }
            return
        }



        val before = ic.getTextBeforeCursor(16, 0)
        if (!before.isNullOrEmpty()) {
            val iterator = java.text.BreakIterator.getCharacterInstance()
            iterator.setText(before.toString())
            val lastBoundary = iterator.last()
            val prevBoundary = iterator.previous()
            
            if (prevBoundary != java.text.BreakIterator.DONE) {
                val deleteLength = lastBoundary - prevBoundary
                ic.deleteSurroundingText(deleteLength, 0)
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        } else {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                ic.deleteSurroundingTextInCodePoints(1, 0)
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        }
        clearSuggestions()
        checkAutoCapitalize()
    }

    private fun startBackspaceRepeat() {
        stopBackspaceRepeat()
        backspaceRepeatJob = serviceScope?.launch {
            delay(KeyboardConstants.BACKSPACE_INITIAL_DELAY_MS)
            var count = 0
            while (true) {
                handleBackspace()
                performLightFeedback()
                count++
                delay(
                    if (count > KeyboardConstants.BACKSPACE_ACCELERATION_THRESHOLD)
                        KeyboardConstants.BACKSPACE_REPEAT_INTERVAL_FAST_MS
                    else
                        KeyboardConstants.BACKSPACE_REPEAT_INTERVAL_INITIAL_MS
                )
            }
        }
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeatJob?.cancel()
        backspaceRepeatJob = null
    }

    private fun handleSpacePress() {

        if (isMediaSearchActive) {
            mediaSearchQuery.append(' ')
            updateMediaSearchDisplay()
            triggerDebouncedSearch()
            performSuccessFeedback()
            return
        }


        commitCurrentCompositionWithSpace(addSpaceIfEmpty = true)
    }






private fun handleSwipeComplete(points: List<FloatArray>) {
    if (points.size < KeyboardConstants.MIN_POINTS_FOR_SWIPE) {
        Log.w(KeyboardConstants.TAG, "Swipe ignored: too few points (${points.size})")
        return
    }


    if (isSwipeComposing || composingRomanText.isNotEmpty()) {
        commitCurrentCompositionWithSpace()
    }

    clearSuggestions()
    performHapticFeedback()
    updateDebugText("Decoding...")

    Log.d(KeyboardConstants.TAG, "Swipe complete: ${points.size} points")
    Log.d(
        KeyboardConstants.TAG,
        "Swipe first=(${points.first()[0]},${points.first()[1]}) last=(${points.last()[0]},${points.last()[1]})"
    )



    serviceScope?.launch {
        decodeSwipe(points)
    }
}

    private fun getWordBeforeCursor(): String? {
        val ic = currentInputConnection ?: return null
        val before = ic.getTextBeforeCursor(100, 0)?.toString().orEmpty()
        if (before.isEmpty()) return null
        
        val matches = Regex("[a-zA-Z\u0900-\u097F]+").findAll(before)
        val lastWord = matches.lastOrNull()?.value
        
        return lastWord?.lowercase()?.trim()
    }

    fun onSwipeStart() {

        if (isSwipeComposing || currentComposedHindi.isNotBlank()) {
            commitCurrentCompositionWithSpace()
        }
        cancelPendingTransliteration()

    }

    private suspend fun decodeSwipe(points: List<FloatArray>) {

        languageMutex.lock()
        try {
            if (isMediaSearchActive) {
                val results = swipeDecoder?.decode(points) ?: emptyList()
                val best = results.firstOrNull()?.word ?: return
                withContext(Dispatchers.Main) {
                    handleMediaSearchInputString(best + " ")
                    swipeView?.clearTrail()
                }
                return
            }
            val decoder = withContext(Dispatchers.Main) { getSwipeDecoder() }

            if (decoder.geometry == null) {
                Log.e(KeyboardConstants.TAG, "decodeSwipe(): geometry is null")
                withContext(Dispatchers.Main) {
                    updateDebugText("Keyboard not ready")
                    swipeView?.clearTrail()
                }
                return
            }

            val t0 = System.currentTimeMillis()
            val prevWordDisplay = withContext(Dispatchers.Main) { getWordBeforeCursor() } ?: ""
            


            val prevWord = if (isHindiMode && prevWordDisplay.isNotEmpty() && lastRomanWord.isNotEmpty()) {
                lastRomanWord
            } else {
                prevWordDisplay
            }

            val result: SwipeDecoder.DecodeResult? = withTimeoutOrNull(KeyboardConstants.SWIPE_DECODE_TIMEOUT_MS) {
                withContext(Dispatchers.Default) {
                    try {
                        decoder.decodeDetailed(points, prevWord)
                    } catch (e: Exception) {
                        Log.e(KeyboardConstants.TAG, "decodeDetailed failed: ${e.message}", e)
                        null
                    }
                }
            }

            val candidates = result?.candidates ?: emptyList()
            val keyPath = result?.keyPath ?: ""


            val best = if (candidates.isNotEmpty()) {
                candidates.first().word
            } else if (keyPath.isNotEmpty()) {

                Log.w(KeyboardConstants.TAG, "No neural candidates — falling back to keyPath='$keyPath'")
                keyPath
            } else {
                ""
            }

            val dt = System.currentTimeMillis() - t0
            Log.d(KeyboardConstants.TAG, "Decode ${dt}ms | keyPath='$keyPath' | best='$best' | candidates=${candidates.take(5).joinToString { "'${it.word}'(${String.format("%.1f", it.score)})" }}")


            val xlitDec = xlitDecoder
            val hindiMode = isHindiMode

            val textToCommit: String
            val altTexts: List<String>
            val englishWord: String
            var primaryWordShown = ""

            if (best.isEmpty()) {
                withContext(Dispatchers.Main) {
                    updateDebugText("No result (${dt}ms)")
                    swipeView?.clearTrail()
                }
                return
            }

            val altEnglishWords = candidates.map { it.word }.filter { it != best }.take(KeyboardConstants.MAX_ROMAN_SUGGESTIONS)

            if (hindiMode && xlitDec != null && xlitDec.isReady) {


                val primaryHindi = withContext(Dispatchers.Default) {
                    try { xlitDec.transliterate(best) } catch (e: Exception) { "" }
                }

                if (primaryHindi.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (!isActive) return@withContext
                        

                        commitCurrentCompositionWithSpace()
                        
                        currentInputConnection?.setComposingText(primaryHindi, 1)
                        
                        isSwipeComposing = true
                        swipeComposedText = primaryHindi
                        swipeEnglishWord = best
                        primaryWordShown = primaryHindi
                    }
                }



                val hindiCandidates = withContext(Dispatchers.Default) {
                    try { 
                        xlitDec.transliterateGetTopK(best, KeyboardConstants.XLIT_BEAM_WIDTH) 
                    } catch (e: Exception) {
                        Log.e(KeyboardConstants.TAG, "Xlit failed for '$best': ${e.message}")
                        emptyList<String>()
                    }
                }
                
                textToCommit = if (primaryHindi.isNotEmpty()) primaryHindi else (hindiCandidates.firstOrNull() ?: best)
                englishWord = best
                

                altTexts = if (hindiCandidates.size > 1) {
                    hindiCandidates.drop(1).filter { it != textToCommit }
                } else {
                    emptyList()
                }
                
                Log.d(KeyboardConstants.TAG, "Hindi (Pipeline): '$best' → '$textToCommit' | alts=$altTexts")
            } else {

                textToCommit = best
                englishWord = "" 
                altTexts = emptyList()
                
                withContext(Dispatchers.Main) {
                    if (!isActive) return@withContext
                    

                    commitCurrentCompositionWithSpace()
                    

                    isSwipeComposing = true
                    swipeComposedText = textToCommit
                    swipeEnglishWord = best
                    primaryWordShown = textToCommit
                    currentInputConnection?.setComposingText(textToCommit, 1)
                }
                
                Log.d(KeyboardConstants.TAG, "Roman: '$best' | alts=$altTexts")
            }

            withContext(Dispatchers.Main) {
                if (!isActive) return@withContext
                


                if (!isSwipeComposing || swipeComposedText != primaryWordShown) {
                    Log.d(KeyboardConstants.TAG, "Refinement skipped: composition changed or committed.")
                    return@withContext
                }

                Log.d(KeyboardConstants.TAG, "Composing: '$textToCommit'")
                updateDebugText("$textToCommit | ${candidates.take(3).joinToString { it.word }} | ${dt}ms")





                val finalizedText = when {
                    isCapsLockEnabled -> textToCommit.uppercase()
                    isShiftEnabled -> textToCommit.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    else -> textToCommit
                }

                currentInputConnection?.setComposingText(finalizedText, 1)

                isSwipeComposing = true
                swipeComposedText = finalizedText
                swipeEnglishWord = englishWord
                swipeAlternatives = altTexts
                updateSwipeSuggestions()

                if (isShiftEnabled && !isCapsLockEnabled) {
                    isShiftEnabled = false
                }
                
                checkAutoCapitalize()
            }
        } finally {
            languageMutex.unlock()
        }
    }





    private fun scheduleTransliteration() {
        transliterationJob?.cancel()
        if (!isHindiMode || composingRomanText.isBlank()) return
        transliterationJob = serviceScope?.launch {
            delay(KeyboardConstants.TRANSLITERATION_DELAY_MS)
            executeTransliteration()
        }
    }

    private fun cancelPendingTransliteration() {
        transliterationJob?.cancel()
        transliterationJob = null
    }

    private fun cancelPendingSwipe() {
        swipeDecodeJob?.cancel()
        swipeDecodeJob = null
    }

    private suspend fun executeTransliteration() {
        val roman = composingRomanText
        if (roman.isBlank()) return
        
        languageMutex.withLock {
            val xlit = xlitDecoder ?: return@withLock
            if (!xlit.isReady) return@withLock


            val fastResult = withContext(Dispatchers.Default) {
                if (!isActive) return@withContext null
                try { xlit.transliterate(roman) } catch (e: Exception) { null }
            }

            if (!fastResult.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    if (!isActive || roman != composingRomanText) return@withContext
                    currentComposedHindi = fastResult
                    currentInputConnection?.setComposingText(fastResult, 1)
                }
            }


            val results = withTimeoutOrNull(KeyboardConstants.XLIT_DECODE_TIMEOUT_MS) {
                withContext(Dispatchers.Default) { 
                    if (!isActive) return@withContext null
                    try {
                        xlit.transliterateGetTopK(roman, KeyboardConstants.XLIT_BEAM_WIDTH)
                    } catch (e: Exception) { null }
                }
            }
            
            if (results != null && results.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (!isActive || roman != composingRomanText) return@withContext
                    val bestResult = results.first()
                    if (bestResult.isNotBlank()) {
                        currentComposedHindi = bestResult
                        currentHindiSuggestions = results
                        currentInputConnection?.setComposingText(bestResult, 1)
                        updateTypingSuggestions()
                    }
                }
            }
        }
    }





    private fun commitCurrentCompositionWithSpace(addSpaceIfEmpty: Boolean = false) {
        cancelPendingTransliteration()
        cancelPendingSwipe()
        
        val text = when {
            isSwipeComposing && swipeComposedText.isNotBlank() -> swipeComposedText
            currentComposedHindi.isNotBlank() -> currentComposedHindi
            composingRomanText.isNotBlank() -> composingRomanText
            else -> null
        }
        
        if (text != null) {


            currentInputConnection?.commitText(text, 1)
            currentInputConnection?.commitText(" ", 1)
            

            lastRomanWord = when {
                isSwipeComposing -> swipeEnglishWord
                isHindiMode -> composingRomanText
                else -> text
            }
            
            clearAllCompositionState()
            checkAutoCapitalize()
        } else if (addSpaceIfEmpty) {
            currentInputConnection?.commitText(" ", 1)
            checkAutoCapitalize()
        }
    }

    private fun commitCurrentCompositionWithoutSpace() {
        cancelPendingTransliteration()
        cancelPendingSwipe()
        
        val text = when {
            isSwipeComposing && swipeComposedText.isNotBlank() -> swipeComposedText
            currentComposedHindi.isNotBlank() -> currentComposedHindi
            composingRomanText.isNotBlank() -> composingRomanText
            else -> null
        }
        
        if (text != null) {
            currentInputConnection?.commitText(text, 1)
            

            lastRomanWord = when {
                isSwipeComposing -> swipeEnglishWord
                isHindiMode -> composingRomanText
                else -> text
            }
            
            clearAllCompositionState()
        }
    }

    private fun clearSwipeComposition() {


        currentInputConnection?.commitText("", 1)
        clearSwipeCompositionState()
        clearSuggestions()
    }

    private fun commitSuggestion(text: String) {
        cancelPendingTransliteration()



        currentInputConnection?.commitText("$text ", 1)
        
        clearAllCompositionState()
        clearSuggestions()
        swipeView?.clearTrail()
        checkAutoCapitalize()
    }

    private fun clearSwipeCompositionState() {
        isSwipeComposing = false
        swipeComposedText = ""
        swipeAlternatives = emptyList()
        swipeEnglishWord = ""
    }

    private fun clearTypingCompositionState() {
        composingRomanText = ""
        currentHindiSuggestions = emptyList()
        currentComposedHindi = ""
    }

    private fun clearAllCompositionState(cancelSwipe: Boolean = true) {
        if (cancelSwipe) cancelPendingSwipe()
        


        currentInputConnection?.finishComposingText()
        
        clearSwipeCompositionState()
        clearTypingCompositionState()
        clearSuggestions()
    }

    private fun commitRawText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun enableAutoShift() {
        if (!isCapsLockEnabled) {
            isShiftEnabled = true
            refreshShiftState()
        }
    }

    private fun checkAutoCapitalize() {
        if (isCapsLockEnabled) return
        val before = currentInputConnection?.getTextBeforeCursor(2, 0) ?: ""
        isShiftEnabled = if (before.isEmpty() ||
            before.endsWith(". ") ||
            before.endsWith("? ") ||
            before.endsWith("! ") ||
            before.endsWith("\n")
        ) {
            true
        } else {



            false
        }
        refreshShiftState()
    }

    private fun refreshShiftState() {
        swipeView?.setShifted(isShiftEnabled || isCapsLockEnabled, isCapsLockEnabled)
        refreshStickyButtons()
    }





    private fun updateSwipeSuggestions() {
        val bar = suggestionBar ?: return
        bar.removeAllViews()
        

        val langColor = try { Color.parseColor(currentLanguage.accentColor) } catch (e: Exception) { currentTheme?.accent ?: Color.BLUE }
        bar.setBackgroundColor(
            currentTheme?.suggestionBarBg ?: Color.parseColor(KeyboardConstants.COLOR_SUGGESTION_BG)
        )
        val all = mutableListOf<String>()


        if (swipeComposedText.isNotBlank() && isHindiMode) all.add(swipeComposedText)
        all.addAll(swipeAlternatives)
        val english = swipeEnglishWord

        if (all.isEmpty() && english.isBlank()) { showPlaceholder(); return }

        bar.removeAllViews()
        bar.alpha = 0f

        for ((i, hindi) in all.withIndex()) {
            if (i > 0) bar.addView(KeyboardUIFactory.createDivider(this, currentTheme))
            bar.addView(
                KeyboardUIFactory.createSuggestionChip(
                    this, hindi, i == 0, false, currentTheme, langColor
                ) { commitSuggestion(it); performSuccessFeedback() }
            )
        }
        if (english.isNotBlank()) {
            if (bar.childCount > 0) bar.addView(KeyboardUIFactory.createDivider(this, currentTheme))
            bar.addView(
                KeyboardUIFactory.createSuggestionChip(
                    this, english, all.isEmpty(), true, currentTheme, langColor
                ) { commitSuggestion(it); performSuccessFeedback() }
            )
        }
        

        bar.addView(android.widget.Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(KeyboardUIFactory.dp(this@IndicSwipeIME, 84), 1)
        })
        
        bar.animate().alpha(1f).setDuration(40).start()
    }

    private fun updateTypingSuggestions() {
        val bar = suggestionBar ?: return
        bar.removeAllViews()
        
        val langColor = try { Color.parseColor(currentLanguage.accentColor) } catch (e: Exception) { currentTheme?.accent ?: Color.BLUE }
        bar.setBackgroundColor(
            currentTheme?.suggestionBarBg ?: Color.parseColor(KeyboardConstants.COLOR_SUGGESTION_BG)
        )
        
        val roman = composingRomanText
        if (roman.isBlank()) { showPlaceholder(); return }

        val suggestions = if (isHindiMode) {
            currentHindiSuggestions
        } else {

            emptyList()
        }

        bar.removeAllViews()
        bar.alpha = 0f


        if (isHindiMode) {
            bar.addView(
                KeyboardUIFactory.createSuggestionChip(
                    this, roman, true, true, currentTheme, langColor
                ) { commitSuggestion(it); performSuccessFeedback() }
            )
        }


        for (word in suggestions) {
            if (word.equals(roman, ignoreCase = true)) continue
            if (bar.childCount > 0) bar.addView(KeyboardUIFactory.createDivider(this, currentTheme))
            bar.addView(
                KeyboardUIFactory.createSuggestionChip(
                    this, word, false, false, currentTheme, langColor
                ) { commitSuggestion(it); performSuccessFeedback() }
            )
        }
        

        bar.addView(android.widget.Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(KeyboardUIFactory.dp(this@IndicSwipeIME, 84), 1)
        })
        
        bar.animate().alpha(1f).setDuration(40).start()
    }

    private fun showPlaceholder() {
        val bar = suggestionBar ?: return
        bar.removeAllViews()
        val hint = KeyboardConstants.PLACEHOLDER_HINTS[
            hintIndex % KeyboardConstants.PLACEHOLDER_HINTS.size
        ]
        bar.addView(KeyboardUIFactory.createPlaceholder(this, hint, currentTheme))
    }

    private fun clearSuggestions() {
        suggestionBar?.removeAllViews()
        showPlaceholder()
    }

    private fun updateDebugText(message: String) {
        debugTextView?.text = message
        debugTextView?.setTextColor(
            currentTheme?.textSecondary ?: Color.parseColor(KeyboardConstants.COLOR_TEXT_SECONDARY)
        )
    }





    private fun showPunctuationPopup() {
        commitCurrentCompositionWithoutSpace()
        dismissPunctuationPopup()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(
                currentTheme?.keyboardBg ?: Color.parseColor(KeyboardConstants.COLOR_KEYBOARD_BG)
            )
            setPadding(8, 8, 8, 8)
        }
        for (p in KeyboardConstants.PUNCTUATION_CHARS) {
            container.addView(
                KeyboardUIFactory.createPunctuationKey(this, p, currentTheme) { s ->
                    commitRawText(s)
                    dismissPunctuationPopup()
                    performSuccessFeedback()
                    if (s == "?" || s == "!") enableAutoShift()
                    checkAutoCapitalize()
                }
            )
        }
        val scroll = HorizontalScrollView(this).apply {
            addView(container)
            isHorizontalScrollBarEnabled = false
        }
        punctuationPopup = PopupWindow(
            scroll,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 20f
            setOnDismissListener { punctuationPopup = null }
        }
        
        val anchor = btnPeriod ?: swipeView
        anchor?.let { 

            punctuationPopup?.showAsDropDown(it, 0, -it.height) 
        }
    }

    private fun showThemePicker() {
        dismissThemePicker()
        val tm = themeManager ?: return
        val activeTheme = tm.currentTheme
        
        val cornerRadiusPx = 24f * resources.displayMetrics.density
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setCornerRadius(cornerRadiusPx)
            setColor(activeTheme.keyboardBg)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bgDrawable
            setPadding(0, 24, 0, 24)
            elevation = 16f
        }
        
        container.addView(TextView(this).apply {
            text = "Choose Theme"
            textSize = 18f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            paint.isFakeBoldText = true
            setTextColor(activeTheme.keyText)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        })

        for ((key, theme) in tm.themes) {
            container.addView(
                KeyboardUIFactory.createThemeCard(this, theme, activeTheme, key == tm.currentThemeKey) {
                    tm.setTheme(key)
                    applyCurrentTheme()
                    dismissThemePicker()
                    performSuccessFeedback()
                }
            )
        }

        val scroll = android.widget.ScrollView(this).apply {
            addView(container)
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        themePickerPopup = PopupWindow(scroll, (resources.displayMetrics.widthPixels * 0.9).toInt(), 
            LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(null)
            elevation = 20f
            setOnDismissListener { themePickerPopup = null }
        }

        val anchor = btnSymbolToggle ?: rootView ?: return
        themePickerPopup?.showAtLocation(anchor, Gravity.CENTER, 0, 0)
    }

    private fun dismissPunctuationPopup() {
        punctuationPopup?.dismiss()
        punctuationPopup = null
    }

    private fun dismissThemePicker() {
        themePickerPopup?.dismiss()
        themePickerPopup = null
    }





    private fun playKeyClick() {
        audioManager?.playSoundEffect(
            AudioManager.FX_KEYPRESS_STANDARD,
            KeyboardConstants.SOUND_VOLUME
        )
    }

    private fun sendKey(code: Int) {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun initVibrator() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            vibrator = null
        }
    }

    
    private fun performHapticFeedback(msOrEffect: Long = 8) {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val effect = when (msOrEffect) {
                    0L -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
                    1L -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                    2L -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                    else -> android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                }
                v.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (duration, amp) = when (msOrEffect) {
                    0L -> 12L to 160
                    1L -> 5L to 100
                    2L -> 3L to 80
                    else -> msOrEffect to 128
                }
                v.vibrate(android.os.VibrationEffect.createOneShot(duration, amp))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(if (msOrEffect == 0L) 12 else msOrEffect)
            }
        } catch (e: Exception) {
            Log.w("IndicSwipeIME", "Haptic error: ${e.message}")
        }
    }

    private fun performSuccessFeedback() { performHapticFeedback(0); playKeyClick() }
    private fun performLightFeedback() { performHapticFeedback(1); playKeyClick() }

    private fun Int.withAlpha(alpha: Int): Int = (this and 0x00FFFFFF) or (alpha shl 24)





    private val GIF_CATEGORIES = listOf(
        "Trending" to "",
        "Reactions" to "reactions",
        "Love" to "love",
        "Sad" to "sad",
        "Happy" to "happy",
        "Thumbs Up" to "thumbs up",
        "LOL" to "lol",
        "OMG" to "omg",
        "Bye" to "bye",
        "Celebrate" to "celebrate",
        "Dance" to "dance",
        "Angry" to "angry"
    )

    private fun setupMediaKeyboard() {
        val rv = gifRecyclerView ?: return
        rv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        gifAdapter = GifAdapter { url -> commitGif(url) }
        rv.adapter = gifAdapter

        btnCancelMedia?.setOnClickListener {
            exitMediaMode()
            performSuccessFeedback()
        }

        gifSearchClear?.setOnClickListener {
            mediaSearchQuery.clear()
            updateMediaSearchDisplay()
            if (isMediaSearchActive) exitMediaSearchTyping()
            searchGifs("")
            performSuccessFeedback()
        }




        gifSearchBar?.setOnClickListener {
            if (isMediaSearchActive) {

                val query = mediaSearchQuery.toString().trim()
                if (query.isNotEmpty()) searchGifs(query)
                exitMediaSearchTyping()
            } else {
                enterMediaSearchTyping()
            }
            performSuccessFeedback()
        }

        rv.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING && isMediaSearchActive) {
                    exitMediaSearchTyping()
                }
            }
        })

        buildGifCategoryChips()
    }

    private fun buildGifCategoryChips() {
        val bar = gifCategoryBar ?: return
        bar.removeAllViews()
        val density = resources.displayMetrics.density

        val history = historyDb.getHistory()
        

        val visibleHistory = history.take(5)
        
        if (visibleHistory.isNotEmpty()) {
            for (query in visibleHistory) {
                addChipToBar(bar, "🕒 $query", query, density, isHistory = true)
            }
            

            val clearChip = TextView(this).apply {
                text = "✕"
                textSize = 12f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                val theme = currentTheme
                val isLight = theme != null && androidx.core.graphics.ColorUtils.calculateLuminance(theme.keyboardBg) > 0.5
                setTextColor(if (isLight) Color.parseColor("#CC000000") else Color.parseColor("#CCFFFFFF"))
                gravity = Gravity.CENTER
                setPadding(
                    (10 * density).toInt(), (4 * density).toInt(),
                    (10 * density).toInt(), (4 * density).toInt()
                )
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (isLight) Color.parseColor("#20FF0000") else Color.parseColor("#30FF4444"))
                    cornerRadius = KeyboardUIFactory.dp(this@IndicSwipeIME, (theme?.keyRadius ?: 9f) * 2f).toFloat()
                    setStroke((1 * density).toInt(), if (isLight) Color.parseColor("#40FF0000") else Color.parseColor("#50FF4444"))
                }
                background = bg
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (12 * density).toInt()
                }
                layoutParams = lp
                setOnClickListener {
                    historyDb.clearAll()
                    buildGifCategoryChips()
                    android.widget.Toast.makeText(this@IndicSwipeIME, "Search history cleared", android.widget.Toast.LENGTH_SHORT).show()
                    performSuccessFeedback()
                }
            }
            bar.addView(clearChip)
        }


        for ((label, query) in GIF_CATEGORIES) {
            addChipToBar(bar, label, query, density, isHistory = false)
        }
    }

    private fun addChipToBar(bar: LinearLayout, label: String, query: String, density: Float, isHistory: Boolean) {
        val theme = currentTheme ?: return
        val isLight = androidx.core.graphics.ColorUtils.calculateLuminance(theme.keyboardBg) > 0.5
        val accent = try { Color.parseColor(currentLanguage.accentColor) } catch (e: Exception) { Color.parseColor("#FF6D00") }
        
        val chip = TextView(this).apply {
            text = label
            textSize = 12f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            

            setTextColor(if (isLight) Color.BLACK else Color.WHITE)
            
            setPadding(
                (14 * density).toInt(), (4 * density).toInt(),
                (14 * density).toInt(), (4 * density).toInt()
            )
            val bg = android.graphics.drawable.GradientDrawable().apply {
                if (isHistory) {

                    setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, if (isLight) 60 else 90))
                    setStroke((1.5f * density).toInt(), accent)
                } else {

                    setColor(if (isLight) Color.parseColor("#25000000") else Color.parseColor("#35FFFFFF"))
                    if (isLight) setStroke((1 * density).toInt(), Color.parseColor("#30000000"))
                }
                cornerRadius = KeyboardUIFactory.dp(this@IndicSwipeIME, theme.keyRadius * 2f).toFloat()
            }
            background = bg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (8 * density).toInt()
            }
            layoutParams = lp

            setOnClickListener {
                mediaSearchQuery.clear()
                mediaSearchQuery.append(if (query.isEmpty()) "" else query)
                updateMediaSearchDisplay()
                searchGifs(query)

                if (isMediaSearchActive) {
                    exitMediaSearchTyping()
                }
                performSuccessFeedback()
            }
            
            if (isHistory) {
                setOnLongClickListener {
                    historyDb.removeSearch(query)
                    buildGifCategoryChips()
                    android.widget.Toast.makeText(this@IndicSwipeIME, "\"$query\" removed", android.widget.Toast.LENGTH_SHORT).show()
                    performSuccessFeedback()
                    true
                }
            }
        }
        bar.addView(chip)
    }

    
    fun showMediaMode() {
        keyboardMode = KeyboardMode.MEDIA
        isMediaSearchActive = false
        mediaSearchQuery.clear()

        hideAllKeyboards()
        suggestionContainer?.visibility = View.VISIBLE
        mediaKeyboard?.visibility = View.VISIBLE
        

        suggestionBarScroll?.visibility = View.GONE
        btnRomanizationToggle?.visibility = View.GONE
        mediaSearchHeader?.visibility = View.VISIBLE
        suggestionDivider?.visibility = View.GONE
        bottomControlBar?.visibility = View.GONE


        clearSuggestions()

        updateMediaSearchDisplay()
        searchGifs("")

        applyMediaTheme()
    }

    
    private fun enterMediaSearchTyping() {
        isMediaSearchActive = true
        


        val standardHeightPx = KeyboardUIFactory.dp(this, KeyboardConstants.KEYBOARD_HEIGHT_DP)
        swipeView?.layoutParams = (swipeView?.layoutParams as? android.widget.FrameLayout.LayoutParams)?.apply {
            height = standardHeightPx
            gravity = android.view.Gravity.BOTTOM
        } ?: android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            standardHeightPx,
            android.view.Gravity.BOTTOM
        )
        
        swipeView?.visibility = View.VISIBLE
        swipeView?.alpha = 0.95f
        swipeView?.isClickable = true
        swipeView?.isFocusable = true
        swipeView?.setSymbolMode(false)
        gifSearchBar?.background = resources.getDrawable(R.drawable.bg_search_bar_active, null)
        gifSearchText?.hint = "Type to search… tap here to close ▼"
    }

    private fun exitMediaSearchTyping() {
        isMediaSearchActive = false

        swipeView?.visibility = View.GONE
        

        swipeView?.layoutParams = (swipeView?.layoutParams as? android.widget.FrameLayout.LayoutParams)?.apply {
            height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            gravity = android.view.Gravity.NO_GRAVITY
        }
        
        gifSearchBar?.background = resources.getDrawable(R.drawable.bg_search_bar, null)
        gifSearchText?.hint = "Search GIFs..."
        updateMediaSearchDisplay()
    }

    private fun handleMediaSearchInputString(text: String) {
        mediaSearchQuery.append(text)
        updateMediaSearchDisplay()
        triggerDebouncedSearch()
    }

    
    private fun exitMediaMode() {
        isMediaSearchActive = false
        mediaSearchQuery.clear()
        gifSearchJob?.cancel()

        mediaSearchHeader?.visibility = View.GONE
        suggestionBarScroll?.visibility = View.VISIBLE
        btnRomanizationToggle?.visibility = View.VISIBLE
        suggestionDivider?.visibility = View.VISIBLE
        showLettersMode()
    }

    
    private fun updateMediaSearchDisplay() {
        val query = mediaSearchQuery.toString()
        if (query.isEmpty()) {
            gifSearchText?.text = ""
            gifSearchText?.hint = "Search GIFs..."
            gifSearchClear?.visibility = View.GONE
        } else {
            gifSearchText?.text = query
            gifSearchClear?.visibility = View.VISIBLE
        }
    }

    
    fun handleMediaSearchInput(char: Char) {
        mediaSearchQuery.append(char.lowercaseChar())
        updateMediaSearchDisplay()
        triggerDebouncedSearch()
        performSuccessFeedback()
    }

    
    fun handleMediaSearchBackspace() {
        if (mediaSearchQuery.isNotEmpty()) {
            mediaSearchQuery.deleteCharAt(mediaSearchQuery.length - 1)
            updateMediaSearchDisplay()
            triggerDebouncedSearch()
        }
        performSuccessFeedback()
    }

    
    fun handleMediaSearchEnter() {
        gifSearchJob?.cancel()
        val query = mediaSearchQuery.toString().trim()
        searchGifs(query)
        exitMediaSearchTyping()
        performSuccessFeedback()
    }

    
    private fun triggerDebouncedSearch() {
        gifSearchJob?.cancel()
        gifSearchJob = serviceScope?.launch {
            delay(500)
            val query = mediaSearchQuery.toString().trim()
            searchGifs(query)
        }
    }

    private var gifWebView: android.webkit.WebView? = null

    
    @SuppressLint("SetJavaScriptEnabled")
    private fun searchGifs(query: String) {
        gifEmptyState?.visibility = View.VISIBLE
        gifLoadingSpinner?.visibility = View.VISIBLE
        gifStatusText?.text = if (query.isEmpty()) "Loading trending GIFs..." else "Searching \"$query\"..."

        if (query.isNotEmpty()) {
            serviceScope?.launch(Dispatchers.IO) {
                historyDb.addSearch(query)
            }
            buildGifCategoryChips()
        }

        serviceScope?.launch(Dispatchers.Main) {
            try {
                preloadWebView()

                val searchQuery = if (query.isEmpty()) "trending gifs" else "$query gif"
                val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                val urlString = "https://www.google.com/search?q=$encodedQuery&udm=2&tbs=itp:animated&safe=active&gl=US&hl=en"

                Log.d(KeyboardConstants.TAG, "GIF API (WebView Scraper): $urlString")

                var isFinished = false
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (!isFinished) {
                        isFinished = true
                        gifWebView?.stopLoading()
                        gifLoadingSpinner?.visibility = View.GONE
                        gifStatusText?.text = "Search timed out"
                    }
                }
                handler.postDelayed(timeoutRunnable, 10_000)

                val GIF_DATA_PATTERN = Regex("""\[\d+,\s*"[^"]*",\s*\["([^"]+)",\s*\d+,\s*\d+[^\]]*\],\s*\["([^"]+)",\s*(\d+),\s*(\d+)[^\]]*\]""")
                val EMPTY_STATE_PATTERN = Regex("(?i)(\"About 0 results\"|id=\"botstuff\".*?It looks like there aren['’‘]t any|did not match any image results)")
                val UNICODE_ESCAPE = Regex("""\\u([0-9a-fA-F]{4})""")

                val pollRunnable = object : Runnable {
                    override fun run() {
                        if (isFinished) return

                        gifWebView?.evaluateJavascript("(function() { return document.documentElement.innerHTML; })();") { htmlResult ->
                            if (isFinished) return@evaluateJavascript

                            val html = try {
                                org.json.JSONTokener(htmlResult).nextValue() as? String ?: htmlResult
                            } catch (e: Exception) {
                                htmlResult
                            }

                            val isGifDataMatched = GIF_DATA_PATTERN.containsMatchIn(html)
                            val isEmptyStateMatched = EMPTY_STATE_PATTERN.containsMatchIn(html)

                            if (isGifDataMatched) {
                                isFinished = true
                                handler.removeCallbacks(timeoutRunnable)
                                
                                val extractedUrls = mutableListOf<String>()
                                val seenUrls = mutableSetOf<String>()
                                val matches = GIF_DATA_PATTERN.findAll(html)
                                for (match in matches) {
                                    val (_, fullUrlEscaped, _, _) = match.destructured
                                    val fullUrl = UNICODE_ESCAPE.replace(fullUrlEscaped) { m ->
                                        m.groupValues[1].toInt(16).toChar().toString()
                                    }
                                    if (!seenUrls.contains(fullUrl) && fullUrl.endsWith(".gif", ignoreCase = true)) {
                                        extractedUrls.add(fullUrl)
                                        seenUrls.add(fullUrl)
                                    }
                                }
                                
                                gifLoadingSpinner?.visibility = View.GONE
                                if (extractedUrls.isEmpty()) {
                                    gifStatusText?.text = "No GIFs found"
                                } else {
                                    gifEmptyState?.visibility = View.GONE
                                    gifAdapter?.setGifs(extractedUrls)
                                }
                            } else if (isEmptyStateMatched) {
                                isFinished = true
                                handler.removeCallbacks(timeoutRunnable)
                                gifLoadingSpinner?.visibility = View.GONE
                                gifStatusText?.text = "No GIFs found"
                            } else {
                                handler.postDelayed(this, 50)
                            }
                        }
                    }
                }

                gifWebView?.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript("""
                            (function() {
                                window.dataLayer = window.dataLayer || [];
                                function gtag(){dataLayer.push(arguments);}
                                gtag('consent', 'default', {
                                  'ad_storage': 'denied',
                                  'ad_user_data': 'denied',
                                  'ad_personalization': 'denied',
                                  'analytics_storage': 'denied'
                                });
                            })();
                        """.trimIndent(), null)
                    }

                    override fun onPageFinished(view: android.webkit.WebView?, finishedUrl: String?) {
                        super.onPageFinished(view, finishedUrl)
                        if (isFinished || view == null) return
                        if (finishedUrl?.contains("google.com/search") == true) {
                            handler.removeCallbacks(pollRunnable)
                            handler.post(pollRunnable)
                        }
                    }

                    override fun onReceivedError(view: android.webkit.WebView?, webReq: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                        super.onReceivedError(view, webReq, error)
                        if (webReq?.isForMainFrame == true) {
                            if (!isFinished) {
                                isFinished = true
                                handler.removeCallbacks(timeoutRunnable)
                                handler.removeCallbacks(pollRunnable)
                                gifLoadingSpinner?.visibility = View.GONE
                                gifStatusText?.text = "Search failed — WebView error"
                            }
                        }
                    }
                }

                gifWebView?.loadUrl(urlString)
                
            } catch (e: Exception) {
                Log.e(KeyboardConstants.TAG, "GIF search failed: ${e.message}", e)
                gifLoadingSpinner?.visibility = View.GONE
                gifStatusText?.text = "Search failed — check connection"
            }
        }
    }

    private fun commitGif(url: String) {
        serviceScope?.launch(Dispatchers.IO) {
            try {

                val imagesDir = File(cacheDir, "images")
                if (!imagesDir.exists() && !imagesDir.mkdirs()) {
                    Log.e(KeyboardConstants.TAG, "Failed to create images directory")
                    return@launch
                }
                
                val file = File(imagesDir, "${System.currentTimeMillis()}.gif")
                java.net.URL(url).openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }



                try {
                    val files = imagesDir.listFiles()
                    if (files != null && files.size > 50) {
                        files.sortBy { it.lastModified() }
                        for (i in 0 until (files.size - 50)) {
                            files[i].delete()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(KeyboardConstants.TAG, "Cache cleanup failed", e)
                }
                
                val contentUri = FileProvider.getUriForFile(
                    this@IndicSwipeIME, 
                    "$packageName.fileprovider", 
                    file
                )
                
                withContext(Dispatchers.Main) {
                    val inputConnection = currentInputConnection ?: return@withContext
                    val editorInfo = currentInputEditorInfo ?: return@withContext
                    
                    var flags = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                    } else {
                        flags = 0
                        try {
                            grantUriPermission(editorInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (e: Exception) {
                            Log.e(KeyboardConstants.TAG, "grantUriPermission failed", e)
                        }
                    }

                    val contentInfo = InputContentInfoCompat(
                        contentUri,
                        ClipDescription("GIF", arrayOf("image/gif")),
                        Uri.parse(url)
                    )
                    
                    val committed = InputConnectionCompat.commitContent(
                        inputConnection,
                        editorInfo,
                        contentInfo,
                        flags,
                        null
                    )
                    
                    if (!committed) {

                        inputConnection.commitText(url, 1)
                    }
                    
                    performSuccessFeedback()
                }
            } catch (e: Exception) {
                Log.e(KeyboardConstants.TAG, "Failed to commit GIF", e)
                withContext(Dispatchers.Main) {

                    currentInputConnection?.commitText(url, 1)
                    performSuccessFeedback()
                }
            }
        }
    }

    private fun applyMediaTheme() {
        val theme = currentTheme ?: return
        val isLight = androidx.core.graphics.ColorUtils.calculateLuminance(theme.keyboardBg) > 0.5
        mediaKeyboard?.setBackgroundColor(theme.keyboardBg)
        

        gifStatusText?.setTextColor(if (isLight) Color.parseColor("#80000000") else Color.parseColor("#80FFFFFF"))
        

        mediaSearchHeader?.setBackgroundColor(theme.keyboardBg)
        
        val accent = try { Color.parseColor(currentLanguage.accentColor) } catch (e: Exception) { theme.accent }
        

        gifSearchBar?.background = KeyboardUIFactory.createSearchBg(this, theme)
        

        gifSearchText?.setTextColor(if (isLight) Color.BLACK else Color.WHITE)
        gifSearchText?.setHintTextColor(if (isLight) Color.parseColor("#80000000") else Color.parseColor("#80FFFFFF"))
        

        btnCancelMedia?.let {
            if (it is TextView) {
                it.setTextColor(accent)
                it.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            }
        }
        

        mediaKeyboard?.let {
            val lp = it.layoutParams
            lp.height = KeyboardUIFactory.dp(this, 328f)
            it.layoutParams = lp
        }
        

        buildGifCategoryChips()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
        if (key == "enabled_languages") {
            Log.d(KeyboardConstants.TAG, "🔄 Enabled languages changed, syncing...")
            
            val enabledIds = settingsManager?.getEnabledLanguageIds() ?: listOf("hindi")
            val currentLang = currentLanguage
            

            if (!enabledIds.contains(currentLang.id)) {
                Log.i(KeyboardConstants.TAG, "Current language ${currentLang.id} was removed. Switching to first available.")
                val firstEnabledId = enabledIds.firstOrNull() ?: "hindi"
                val newIndex = KeyboardConstants.LANGUAGES.indexOfFirst { it.id == firstEnabledId }
                if (newIndex != -1) {
                    switchLanguage(newIndex)
                }
            } else {

                updateSpaceBarLabel()
                updateSpaceBarAppearance()
            }
        }
    }
}