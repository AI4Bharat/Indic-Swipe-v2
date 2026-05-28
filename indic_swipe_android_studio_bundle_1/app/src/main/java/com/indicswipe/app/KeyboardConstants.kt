package com.indicswipe.app

object KeyboardConstants {

    const val TAG = "IndicSwipeIME"

    
    
    
    const val TRAIN_WIDTH = 360f
    const val TRAIN_HEIGHT = 220f
    const val SAMPLING_CADENCE_MS = 30.0f
    const val TRAIN_ALPHABET_AREA_HEIGHT = 220f

    
    
    
    const val SWIPE_DECODE_TIMEOUT_MS = 5000L
    const val XLIT_DECODE_TIMEOUT_MS = 600L
    const val TRANSLITERATION_DELAY_MS = 10L
    const val BACKSPACE_INITIAL_DELAY_MS = 300L
    const val BACKSPACE_REPEAT_INTERVAL_INITIAL_MS = 80L
    const val BACKSPACE_REPEAT_INTERVAL_FAST_MS = 40L
    const val BACKSPACE_ACCELERATION_THRESHOLD = 5
    const val SPACE_LONG_PRESS_MS = 250L
    const val LANGUAGE_FLASH_DURATION_MS = 600L

    
    
    
    const val MIN_SWIPE_DISTANCE_PX = 30f
    const val MAX_TRAJ_LEN = 150
    const val MAX_WORD_LEN_TARGET = 20
    const val MIN_POINTS_FOR_SWIPE = 5
    const val TRAIL_FADE_DELAY_MS = 90L
    const val TRAIL_FADE_DURATION_MS = 320L

    
    const val VELOCITY_SCALE = 1000f
    const val ACCELERATION_SCALE = 500f
    const val FEATURE_CLIP_VAL = 10.0f

    
    const val VEL_CLIP_MIN   = -10f
    const val VEL_CLIP_MAX   =  10f
    const val ACCEL_CLIP_MIN = -500f
    const val ACCEL_CLIP_MAX =  500f

    
    const val HIGH_VELOCITY_THRESHOLD = 0.95f
    const val LOW_VELOCITY_THRESHOLD = 0.16f
    const val PAUSE_VELOCITY_THRESHOLD = 0.11f
    const val MIN_PAUSE_TIME_MS = 35L
    const val VELOCITY_SQUASH_THRESHOLD = 0.52f
    const val ONE_EURO_MIN_CUTOFF = 1.0f
    const val ONE_EURO_BETA = 0.007f
    const val ONE_EURO_D_CUTOFF = 1.0f

    
    const val SAMPLE_DIST_HIGH_SPEED_SQ = 64f
    const val SAMPLE_DIST_MEDIUM_SPEED_SQ = 36f
    const val SAMPLE_DIST_LOW_SPEED_SQ = 25f
    const val SAMPLE_DIST_PAUSE_SQ = 16f

    
    const val CORNER_ANGLE_THRESHOLD = 0.58f
    const val MIN_POINT_DISTANCE_TRAIN = 1.8f
    const val MIN_POINTS_BEFORE_SIMPLIFY = 16
    const val MAX_PATH_WORD_RATIO = 4.5f
    const val BACKTRACK_DETECT_THRESHOLD = -0.28f
    const val MIN_BACKTRACK_DISTANCE_SQ = 2200f

    
    const val ENDPOINT_SNAP_DISTANCE = 14f
    const val ENDPOINT_VELOCITY_THRESHOLD = 0.14f
    const val START_STABILIZATION_POINTS = 4
    const val END_STABILIZATION_POINTS = 6
    const val TOUCH_SIZE_WEIGHT = 0.35f

    const val SWIPE_MOVE_THRESHOLD_SQ = 400f
    const val SWIPE_DELIBERATE_THRESHOLD_SQ = 900f
    const val SWIPE_START_THRESHOLD_SQ = 1000f 
    const val SPECIAL_TAP_TOLERANCE = 14f

    
    const val VERTICAL_PENALTY = 1.4f
    const val OUTSIDE_HITBOX_PENALTY = 2.8f
    const val EXTREME_DISTANCE_THRESHOLD_KEYS = 2.2f

    
    
    
    const val SWIPE_BEAM_WIDTH = 1             
    const val XLIT_BEAM_WIDTH = 2                 
    const val PRUNING_THRESHOLD_LOG = -320.0f

    
    
    const val BEAM_EARLY_EXIT_MIN_STEP = 6       
    const val BEAM_EARLY_EXIT_MIN_COMPLETED = 4  

    const val NEURAL_SCORE_WEIGHT = 4.0f
    const val LM_WEIGHT_ALPHA = 0.25f
    const val REPETITION_PENALTY = -10.5f
    const val DECODER_TEMPERATURE = 0.55f

    const val SKELETON_MISMATCH_PENALTY = -5.0f
    const val TRIPLE_CHAR_PENALTY = -15.0f 
    const val UNFINISHED_PENALTY = -5.0f
    const val EOS_TERMINATION_PENALTY = -1.5f  

    const val SKELETON_REWARD = 12.0f
    val LANGUAGE_TO_SCRIPT = mapOf(
        "hindi" to "__hi__",
        "bengali" to "__bn__",
        "tamil" to "__ta__",
        "telugu" to "__te__",
        "marathi" to "__mr__",
        "kannada" to "__kn__",
        "gujarati" to "__gu__",
        "punjabi" to "__pa__",
        "malayalam" to "__ml__",
        "odia" to "__or__",
        "assamese" to "__as__",
        "maithili" to "__mai__",
        "sanskrit" to "__sa__",
        "urdu" to "__ur__",
        "kashmir" to "__ks__",
        "nepali" to "__ne__",
        "sindhi_arab" to "__sd__",
        "sindhi_dev" to "__sdd__",
        "konkani" to "__gom__",
        "manipuri" to "__mni__",
        "bodo" to "__brx__",
        "dogri" to "__doi__",
        "santali" to "__sat__",
    )
    const val SKELETON_COVERAGE_REWARD = 15.0f     
    const val SKELETON_ENDPOINT_BONUS = 35.0f      
    const val DICTIONARY_BONUS = 600.0f             
    const val MINIMUM_PATH_COVERAGE = 0.32f

    const val NEURAL_HIGH_CONFIDENCE_THRESHOLD = -11.0f
    const val NEURAL_MEDIUM_CONFIDENCE_THRESHOLD = -14.0f
    const val LEXICON_BIAS_MULTIPLIER_HIGH_CONF = 1.0f
    const val LEXICON_BIAS_MULTIPLIER_MED_CONF = 1.6f
    const val LEXICON_BIAS_MULTIPLIER_LOW_CONF = 2.8f

    const val MAX_TRANSIT_VELOCITY = 2.8f
    const val MAX_KEYPATH_LENGTH = 16

    
    const val LENGTH_REWARD_FACTOR = 45.0f          
    const val TIE_BREAK_CONFIDENCE_THRESHOLD = 3.5f 
    const val PATH_LEN_MISMATCH_PENALTY = -45.0f    

    
    const val RESCUE_BASE_PENALTY = -14.0f
    const val RESCUE_GEOM_THRESHOLD_LOW = 0.48f
    const val RESCUE_GEOM_THRESHOLD_MED = 0.72f

    
    
    
    const val CURSOR_START_THRESHOLD_DP = 14f
    const val CURSOR_MOVE_THRESHOLD_DP = 10f
    const val SOUND_VOLUME = 0.6f

    const val MAX_HINDI_SUGGESTIONS = 6
    const val MAX_ROMAN_SUGGESTIONS = 2  
    const val SUGGESTION_TEXT_SIZE_HINDI = 17.5f
    const val SUGGESTION_TEXT_SIZE_ENGLISH = 14.5f
    const val SUGGESTION_TEXT_SIZE_PRIMARY = 18.5f
    const val SUGGESTION_BAR_HEIGHT_DP = 48
    const val KEYBOARD_HEIGHT_DP = 304
    const val SUGGESTION_HORIZONTAL_PADDING_DP = 14
    const val SUGGESTION_VERTICAL_PADDING_DP = 9

    const val KEY_CORNER_RADIUS_DP = 9f
    const val KEY_CORNER_RADIUS_LARGE_DP = 9f
    const val SPACE_BAR_CORNER_RADIUS_DP = 9f
    const val SPACE_BAR_HEIGHT_DP = 58
    const val BOTTOM_ROW_HEIGHT_DP = 68
    const val BOTTOM_ROW_KEY_MARGIN_DP = 4f

    const val SYMBOL_KEY_TEXT_SIZE = 18f
    const val SYMBOL_SPECIAL_TEXT_SIZE = 16f

    const val EMOJI_COLUMNS = 8

    
    
    
    val PUNCTUATION_CHARS = listOf(",", "?", "!", ":", ";", "'", "\"", "@", "#", "(", ")", "-", "_", "/", "&", "%")
    val PRIMARY_SYMBOLS = listOf(".", ",", "?", "!")

    val SYMBOL_PAGE_1 = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
        listOf("*", "\"", "'", ":", ";", "!", "?")
    )

    val SYMBOL_PAGE_2 = listOf(
        listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "∆"),
        listOf("£", "€", "¥", "^", "°", "=", "{", "}", "\\"),
        listOf("%", "©", "®", "™", "✓", "[", "]")
    )

    val PLACEHOLDER_HINTS = listOf(
        "Swipe to type • Hold space to switch language",
        "Hold . for punctuation • Double-tap ⇧ for CAPS",
        "Hold ?123 for themes"
    )

    data class EmojiCategory(val icon: String, val label: String, val emojis: List<String>)

    val EMOJI_CATEGORIES = listOf(
        EmojiCategory("🕒", "Recent", emptyList()),
        EmojiCategory("😀", "Smileys", listOf(
            "😀","😃","😄","😁","😆","😅","😂","🤣","🥲","😊","😇","🙃","😉","😌","😍","🥰","😘","😗","😙","😚","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🫢","🫣","🤫","🤔","🫡","🤐","🤨","😐","😑","😶","🫥","😶‍🌫️","😏","😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🤧","🥵","🥶","🥴","😵","😵‍💫","🤯","🤠","🥳","😎","🧐","😕","🫤","😟","🙁","☹️","😮","😯","😲","😳","🥺","🥹","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖","😺","😸","😹","😻","😼","😽","🙀","😿","😾","🙈","🙉","🙊"
        )),
        EmojiCategory("👋", "People", listOf(
            "👋","🤚","🖐️","✋","🖖","👌","🤌","🤏","✌️","🤞","🫰","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","👍","👎","✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍️","💅","🤳","💪","🦾","🦵","🦿","🦶","👣","👂","🦻","👃","🫀","🫁","🧠","🦷","🦴","👀","👁️","👅","👄","🫦","💋","🩸"
        )),
        EmojiCategory("🐻", "Nature", listOf(
            "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐻‍❄️","🐨","🐯","🦁","🐮","🐷","🐽","🐸","🐵","🙈","🙉","🙊","🐒","🐔","🐧","🐦","🐤","🐣","🐥","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🪱","🐛","🦋","🐌","🐞","🐜","🪰","🪲","🪳","🦟","🦗","蛛","🕸️","🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈","🐊","🐅","🐆","🦓","🦍","🦧","🦣","🐘","🦛","🦏","🐪","🐫","🦒","🦘","🦬","🐃","🐂","🐄","🐎","🐖","🐏","🐑","🦙","🐐","🦌","🐕","🐩","🦮","🐕‍🦺","🐈","🐈‍⬛","🐓","🦃","🦚","🦜","🦢","🦩","🕊️","🐇","🦝","🦨","🦡","🦫","🦦","🦥","🐁","🐀","🐿️","🦔","🐾","🐉","🐲","🌵","🎄","🌲","🌳","🌴","🪵","🌱","🌿","☘️","🍀","🎍","🪴","🎋","🍃","🍂","🍁","🍄","🐚","🪨","🌾","💐","🌷","🌹","🥀","🌺","🌸","🌼","🌻","🌞","🌝","🌛","🌜","🌚","🌕","🌖","🌗","🌘","🌑","🌒","🌓","🌔","🌙","🌎","🌍","🌏","🪐","💫","⭐️","🌟","✨","⚡️","☄️","💥","🔥","🌪️","🌈","☀️","🌤️","⛅️","🌥️","☁️","🌦️","🌧️","⛈️","🌩️","🌨️","❄️","☃️","⛄️","🌬️","💨","💧","💦","🫧","☔️","☂️","🌊","🌫️"
        )),
        EmojiCategory("🍔", "Food", listOf(
            "🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬","🥒","🌶️","🫑","🌽","🥕","🫒","🧄","🧅","🥔","🍠","🥐","🥯","🍞","🥖","🥨","🧀","🥚","🍳","🧈","🥞","🫓","🥪","🌮","🌯","🫔","🥙","🧆","🥘","🍲","🫕","🥣","🥗"," popcorn","🍿","🧈","🧂","🥫","🍱","🍘","🍙","🍚","🍛","🍜","🍝","🍠","🍢","🍣","🍤","🍥","🥮","🍡"," dumpling","🥟","🥠","🥡","🦀","🦞","🦐","🦑","🦪","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭","🍮","🍯","🍼","🥛","☕️","🫖","🍵","🍶","🍾","🍷","🍸","🍹","🍺","🍻","🥂","🥃","🥤","🧋","🧃","🧉","🧊"
        )),
        EmojiCategory("⚽", "Activities", listOf(
            "⚽️","🏀","🏈","⚾️","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒","👡","⛸️","🎣","🤿","🥊","🥋","🥅","⛳️","🎿","⛷️","🏂","🪂","🏋️‍♀️","🏋️","🏋️‍♂️","🤼‍♀️","🤼","🤼‍♂️","🤸‍♀️","🤸","🤸‍♂️","⛹️‍♀️","⛹️","⛹️‍♂️","🤺","🤾‍♀️","🤾","🤾‍♂️","🏌️‍♀️","🏌️","🏌️‍♂️","🏇","🧘‍♀️","🧘","🧘‍♂️","🏄‍♀️","🏄","🏄‍♂️","🏊‍♀️","🏊","🏊‍♂️","🤽‍♀️","🤽","🤽‍♂️","🚣‍♀️","🚣","🚣‍♂️","🧗‍♀️","🧗","🧗‍♂️","🚵‍♀️","🚵","🚵‍♂️","🚴‍♀️","🚴","🚴‍♂️","🏆","🥇","🥈","🥉","🏅","🎖️","🏵️","🎗️","🎫","🎟️","🎪","🤹‍♀️","🤹","🤹‍♂️","🎭","🩰","🎨","🎬","🎤","🎧","🎼","🎹","🥁","🪘"," saxophone","🎷","🎺","🎸","🪕","🎻","🎲","♟️","🎯","🎳","🎮","🎰","🧩"
        )),
        EmojiCategory("🌇", "Travel", listOf(
            "🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🛻","🚚","🚛","🚜","🛵","🏍️","🛺","🚲","🛴","🛹","🚏","🛣️","🛤️","🛢️","⛽️","🚨","🚥","🚦","🛑","🚧","⚓️","⛵️","🛶","🚤","🛳️","⛴️","🚢","✈️","🛩️","🛫","🛬","🪂","💺","🚁","🚟","🚠","🚡","🛰️","🚀","🛸","🛎️","🧳","⌛️","⏳","⌚️","⏰","⏱️","⏲️","🕰️","🌡️","☀️","🌤️","⛅️","🌥️","☁️","🌦️","🌧️","⛈️","🌩️","🌨️","❄️","☃️","⛄️","🌬️","💨","🌪️","🌫️","🌬️","🌀","🌈","🌂","☂️","☔️","⛱️","⚡️","❄️","☃️","⛄️","🔥","💥","☄️"
        )),
        EmojiCategory("💡", "Objects", listOf(
            "⌚️","📱","📲","💻","⌨️","🖥️","🖨️","マウス","🖱️","🖲️","🕹️","🗜️","💽","💾","💿","📀","📼","📷","📸","📹","🎥","📽️","🎞️","📞","☎️","📟","📠","テレビ","📺","📻","マイクロフォン","🎙️","🎚️","🎛️","コンパス","🧭","ストップウォッチ","⏱️","タイマー","⏲️","時計","🕰️"," hourglass","⌛️","⏳","アンテナ","📡","電池","バッテリー","🔋","プラグ","🔌","電球","💡","懐中電灯","🔦","ろうそく","🕯️","🪔","消火器","🧯","🛢️","お金","💸","💵","💴","💶","💷","コイン","🪙","お金","💰","カード","💳","宝石","💎","はかり","⚖️","はしご","🪜","ツールボックス","🧰","ドライバー","🪛","レンチ","🔧","ハンマー","🔨","⚒️","🛠️","⛏️","のこぎり","🪚","ボルト","ナット","🔩","ギア","⚙️","🪤","レンガ"," bricks","🧱","鎖","⛓️","磁石","🧲","銃","🔫","爆弾","💣","花火","🧨","斧","🪓","ナイフ","🔪","短剣","🗡️","剣","⚔️","盾","🛡️","タバコ","🚬","棺","⚰️","墓","🪦","壺","⚱️","壺","🏺","水晶","🔮","念珠","📿","🧿","💈","フラスコ","⚗️","望遠鏡","🔭","顕微鏡","🔬","穴","🕳️","絆創膏","🩹","聴診器","🩺","薬","💊","注射","💉","血","🩸","DNA","🧬","微生物","🦠","シャーレ","🧫","試験管","🧪","温度計","🌡️","ほうき","🧹","バスケット","🧺","トイレットペーパー","🧻","トイレ","🚽","蛇口","🚰","シャワー","🚿","浴槽","🛁","入浴","🛀","石鹸","🧼","歯ブラシ","🪥","カミソリ","🪒","スポンジ","🧽","バケツ","🪣","ローション","🧴","ベル","🛎️","鍵","🔑","🗝️","ドア","🚪","椅子","🪑","ソファ","🛋️","ベッド"," beds","🛏️","寝る","🛌","ぬいぐるみ","🧸","マトリョーシカ","🪆","絵画","🖼️","鏡","🪞","窓","🪟","買い物","🛍️","カート","🛒","プレゼント","🎁","風船","🎈","こいのぼり","🎏","リボン","🎀","杖","🪄","ピニャータ","🪅","くす玉","🎊","クラッカー","🎉","雛人形","🎎","提灯","🏮","風鈴","🎐","祝儀袋","🧧","手紙","✉️","📩","📧","📨","📤","📥","📦","ラベル","🏷️","看板","🪧","ポスト","📪","📫","📬","📭","📮","郵便","📯","巻物","📜","書類","📃","📄","しおり","📑","グラフ","📊","📈","📉","メモ","🗒️","カレンダー","🗓️","📅","ゴミ箱","🗑️","名刺","📇","ファイル","🗃️","投票","🗳️","ファイル","🗄️","クリップボード","📋","フォルダ","📁","📂","ファイル","🗂️","新聞","🗞️","📰","ノート","📓","📔","📒","📕","📗","📘","📙","本","📚","本","📖","しおり","🔖","安全ピン","🧷","リンク","🔗","クリップ","📎","🖇️","定規","📐","📏","そろばん","🧮","ピン","📌","📍","はさみ","✂️","ペン","🖊️","万年筆","🖋️","ペン先","✒️","筆","🖌️","クレヨン","🖍️","メモ","📝","鉛筆","✏️","虫眼鏡","🔍","🔎","錠","🔏","鍵","🔒","🔓","🔏","🔐"
        )),
        EmojiCategory("🔣", "Symbols", listOf(
            "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","☮️","✝️","☪️","🕉️","☸️","✡️","🔯","🕎","☯️","☦️","🛐","⛎","♈️","♉️","♊️","♋️","♌️","♍️","♎️","♏️","♐️","♑️","♒️","♓️","🆔","⚛️","🉑","☢️","☣️","📴","📳","🈶","🈚️","🈸","🈺","🈷️","✴️","🆚","💮","🉐","㊙️","㊗️","🈴","🈵","🈹","🈲","🅰️","🅱️","🆎","🆑","🅾️","🆘","❌","⭕️","🛑","⛔️","📛","🚫","💯","💢","♨️","🚷","🚯","🚳","🚱","🔞","📵","🚭","❗️","❕","❓","❔","‼️","⁉️","🔅","🔆","〽️","⚠️","🚸"," trident","🔱"," fleur-de-lis","⚜️"," beginner","🔰"," recycle","♻️","✅","🈯️","💹","❇️","✳️","❎","🌐","💠","Ⓜ️","🌀","💤","🏧","🚾","♿️","🅿️","🈳","🈂️","🛂","🛃","🛄","🛅","🚹","🚺","🚼","⚧️","🚻","🚮","🎦","📶","🈁","🔣","ℹ️","🔤","🔡","🔠","🆖","🆗","🆙","🆒","🆕","🆓","0️⃣","1️⃣","2️⃣","3️⃣","4️⃣","5️⃣","6️⃣","7️⃣","8️⃣","9️⃣","🔟","🔢","#️⃣","*️⃣","⏏️","▶️","⏸️","⏯️","⏹️","⏺️","⏭️","⏮️","⏩","⏪","⏫","⏬","◀️","🔼","🔽","➡️","⬅️","⬆️","⬇️","↗️","↘️","↙️","↖️","↕️","↔️","↪️","↩️","⤴️","⤵️","🔀","🔁","🔂","🔄","🔃","🎵","🎶","➕","➖","➗","✖️","♾️","💲","💱","™️","©️","®️","👁️‍🗨️","🔚","🔙","🔛","🔝","🔜","〰️","➰","➿","✔️","☑️","🔘","🔴","🟠","🟡","🟢","🔵","🟣","⚫️","⚪️","🟤","🔺","🔻","🔸","🔹","🔶","🔷","🔳","🔲","🏁","🚩","🎌","🏴","🏳️","🏳️‍🌈","🏳️‍⚧️","🏴‍☠️"
        )),
        EmojiCategory("🏁", "Flags", listOf(
            "🏁","🚩","🎌","🏴","🏳️","🏳️‍🌈","🏳️‍⚧️","🏴‍☠️","🇦🇫","🇦🇽","🇦🇱","🇩🇿","🇦🇸","🇦🇩","🇦🇴","🇦🇮","🇦🇶","🇦🇬","🇦🇷","🇦🇲","🇦🇼","🇦🇺","🇦🇹","🇦🇿","🇧🇸","🇧🇭","🇧🇩","🇧🇧","🇧🇾","🇧🇪","🇧🇿","🇧🇯","🇧🇲","🇧🇹","🇧🇴","🇧🇦","🇧🇼","🇧🇷","🇮🇴","🇻🇬","🇧🇳","🇧🇬","🇧🇫","🇧🇮","🇰🇭","🇨🇲","🇨🇦","🇮🇨","🇨🇻","🇧🇶","🇰🇾","🇨🇫","🇹🇩","🇨🇱","🇨🇳","🇨🇽","🇨🇨","🇨🇴","🇰🇲","🇨🇬","🇨🇩","🇨🇰","🇨🇷","🇨🇮","🇭🇷","🇨🇺","🇨🇼","🇨🇾","🇨🇿","🇩🇰","🇩🇯","🇩🇲","🇩🇴","🇪🇨","🇪🇬","🇸🇻","🇬🇶","🇪🇷","🇪🇪","🇪🇹","🇪🇺","🇫🇰","🇫🇴","🇫🇯","🇫🇮","🇫🇷","🇬🇫","🇵🇫","🇹🇫","🇬🇦","🇬🇲","🇬🇪","🇩🇪","🇬🇭","🇬🇮","🇬🇷","🇬🇱","🇬🇩","🇬🇵","🇬🇺","🇬🇹","🇬🇬","🇬🇳","🇬🇼","🇬🇾","🇭🇹","🇭🇳","🇭🇰","🇭🇺","🇮🇸","🇮🇳","🇮🇩","🇮🇷","🇮🇶","🇮🇪","🇮🇲","🇮🇱","🇮🇹","🇯🇲","🇯🇵","🎌","🇯🇪","🇯🇴","🇰🇿","🇰🇪","🇰🇮","🇽🇰","🇰🇼","🇰🇬","🇱🇦","🇱🇻","🇱🇧","🇱🇸","🇱🇷","🇱🇾","🇱🇮","🇱🇹","🇱🇺","🇲🇴","🇲🇰","🇲🇬","🇲🇼","🇲🇾","🇲🇻","🇲🇱","🇲🇹","🇲🇭","🇲🇶","🇲🇷","🇲🇺","🇾🇹","🇲🇽","🇫🇲","🇲🇩","🇲🇨","🇲🇳","🇲🇪","🇲🇸","🇲🇦","🇲🇿","🇲🇲","🇳🇦","🇳🇷","🇳🇵","🇳🇱","🇳🇨","🇳🇿","🇳🇮","🇳🇪","🇳🇬","🇳🇺","🇳🇫","🇰🇵","🇲🇵","🇳🇴","🇴🇲","🇵🇰","🇵🇼","🇵🇸","🇵🇦","🇵🇬","🇵🇾","🇵🇪","🇵🇭","🇵🇳","🇵🇱","🇵🇹","🇵🇷","🇶🇦","🇷🇪","🇷🇴","🇷🇺","🇷🇼","🇼🇸","🇸🇲","🇸🇹","🇸🇦","🇸🇳","🇷🇸","🇸🇨","🇸🇱","🇸🇬","🇸🇽","🇸🇰","🇸🇮","🇬🇸","🇸🇧","🇸🇴","🇿🇦","🇰🇷","🇸🇸","🇪🇸","🇱🇰","🇧🇱","🇸🇭","🇰🇳","🇱🇨","🇵🇲","🇻🇨","🇸🇩","🇸🇷","🇸🇿","🇸🇪","🇨🇭","🇸🇾","🇹🇼","🇹🇯","🇹🇿","🇹🇭","🇹🇱","🇹🇬","🇹🇰","🇹🇴","🇹🇹","🇹🇳","🇹🇷","🇹🇲","🇹🇨","🇹🇻","🇻🇮","🇺🇬","🇺🇦","🇦🇪","🇬🇧","🏴󠁧󠁢󠁥󠁮󠁧󠁿","🏴󠁧󠁢󠁳󠁣󠁴󠁿","🏴󠁧󠁢󠁷󠁬󠁳󠁿","🇺🇸","🇺🇾","🇺🇿","🇻🇺","🇻🇦","🇻🇪","🇻🇳","🇼🇫","🇪🇭","🇾🇪","🇿🇲","🇿🇼"
        )),
        EmojiCategory("(•‿•)", "Kaomoji", listOf(
            ":)", ":(", ";)", ":D", ":P", "XD", ":/", ":|", ":*", ":O", "B)", ":'(", "o_O", ":-)", ":-(", "(•‿•)","(ᵔᴥᵔ)","(◕‿◕)","(╯°□°）╯︵ ┻━┻","┬─┬ノ( º _ ºノ)","¯\\_(ツ)_/¯","( ͡° ͜ʖ ͡°)","(づ｡◕‿‿◕｡)づ","(っ◕‿◕)っ","(ノಠ益ಠ)ノ","(⁎⁍̴̛ᴗ⁍̴̛⁎)","(๑>◡<๑)","(●´ω｀●)","(๑•̀ㅂ•́)و✧","(づ￣ ³￣)づ","(つ▀¯▀)つ","(⊙_⊙)","(͡° ͜ʖ ͡°)","(▀̿Ĺ̯▀̿ ̿)","( ﾟдﾟ)","(ಠ_ಠ)","(¬_¬)","(◣_◢)","(╯◕_◕)╯","(◕‿◕✿)","(◕ܫ◕)","(づ｡◕‿‿◕｡)づ","(つ◉益◉)つ","(✿◠‿◠)","(◡‿◡✿)","(◕‿◕)","(っ˘ڡ˘ς)","(●´⌓`●)","(・_・)","(o_o)","( u _ u )","( ˃̶͈̀ 🚀 ˂̶͈́)","(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧","(－‸ლ)","( ͡° ʖ̯ ͡°)","( ͠° ͟ʖ ͡°)","( ఠ ͟ʖ ఠ)","( ʘ̆ 📝 ʘ̆ )","(♥_♥)","(>_<)","(^_^)","(T_T)","(O_O)","(°ㅂ°╬)","(ง'̀-'́)ง","(つ◕౪◕)つ","(人◕ω◕)","(╯3╰)","(ó﹏ò｡)","(｡•́︿•̀｡)","(╥﹏╥)","(ノAヽ)","(つ﹏<)･ﾟ｡","(๑•̀ㅂ•́)व","(๑˃̵ᴗ˂̵)व","(•̀ᴗ•́)व","( ˘ ³˘)♥","(❤ω❤)","(♡°▽°♡)","(๑♡⌓♡๑)","(o^ ^o)","( ´ ▽ ` )","(￣▽￣)","(⌒‿⌒)","(*^‿^*)","(☆ω☆)","(✧ω✧)"
        ))
    )


    
    
    
    const val COLOR_KEYBOARD_BG = "#111111"
    const val COLOR_KEY_BG = "#1F1F1F"
    const val COLOR_KEY_PRESSED = "#333333"
    const val COLOR_SPECIAL_KEY_BG = "#1A1A1A"
    const val COLOR_KEY_TEXT = "#F5F5F5"
    const val COLOR_TEXT_SECONDARY = "#999999"
    const val COLOR_ACCENT = "#FF6625"
    const val COLOR_SUGGESTION_BG = "#0D0D0D"
    const val COLOR_SUGGESTION_CHIP_BG = "#1A1A1A"
    const val COLOR_DIVIDER = "#242424"
    const val COLOR_ENTER_BG = "#FF6625"
    const val COLOR_ACTION_ICON = "#F5F5F5"

    const val COLOR_HINDI_ACCENT_FALLBACK = "#FF6625"
    const val COLOR_ENGLISH_ACCENT = "#4CAF50"

    
    
    
    const val DEBUG_DRAW_TRAIL = false  
    const val DEBUG_LOG_DECODE = true  

    
    
    
    data class Language(
        val id: String, 
        val name: String,
        val englishName: String,
        val accentColor: String, 
        val isHindiMode: Boolean = true,
        val assetFolder: String = id,
        val badgeLabel: String? = null
    ) {
        
        val displayName: String get() = "$englishName ($name)"
    }
    
    val LANGUAGES = listOf(
        Language("hindi", "हिन्दी", "Hindi", "#FF6625", true, "hindi"),
        Language("bengali", "বাংলা", "Bengali", "#283593", true, "bengali"),
        Language("tamil", "தமிழ்", "Tamil", "#FFD600", true, "tamil"),
        Language("telugu", "తెలుగు", "Telugu", "#FF1744", true, "telugu"),
        Language("marathi", "मराठी", "Marathi", "#2979FF", true, "marathi"),
        Language("kannada", "ಕನ್ನಡ", "Kannada", "#AA00FF", true, "kannada"),
        Language("gujarati", "ગુજરાતી", "Gujarati", "#AFB42B", true, "gujarati"),
        Language("punjabi", "ਪੰਜਾਬੀ", "Punjabi", "#F9A825", true, "punjabi"),
        Language("malayalam", "മലയാളം", "Malayalam", "#00BFA5", true, "malayalam"),
        Language("odia", "ଓଡ଼ିଆ", "Odia", "#0277BD", true, "odia"),
        Language("assamese", "অসমীয়া", "Assamese", "#00897B", true, "assamese"),
        Language("maithili", "मैथिली", "Maithili", "#7E57C2", true, "maithili"),
        Language("sanskrit", "संस्कृतम्", "Sanskrit", "#FF5722", true, "sanskrit"),
        Language("urdu", "اردو", "Urdu", "#1B5E20", true, "urdu"),
        Language("kashmir", "کٲشُر", "Kashmiri", "#43A047", true, "kashmir"),
        Language("nepali", "नेपाली", "Nepali", "#C62828", true, "nepali"),
        Language("sindhi_arab", "سنڌي", "Sindhi (Arabic)", "#00BCD4", true, "sindhi_arab"),
        Language("sindhi_dev", "सिंधी", "Sindhi (Devanagari)", "#008080", true, "sindhi_dev"),
        Language("konkani", "कोंकणी", "Konkani", "#EC407A", true, "konkani"),
        Language("manipuri", "ꯃꯩꯇꯩꯂꯣꯟ", "Manipuri", "#880E4F", true, "manipuri"),
        Language("bodo", "बड़ो", "Bodo", "#607D8B", true, "bodo"),
        Language("dogri", "डोगरी", "Dogri", "#795548", true, "dogri"),
        Language("santali", "ᱥᱟᱱᱛᱟᱲᱤ", "Santali", "#2E7D32", true, "santali")
    )

    
    
    
    
    const val KEYBOARD_SIDE_MARGIN_RATIO = 0.0125f  
    const val HITBOX_PADDING_RATIO_H = 0.01f       
    const val HITBOX_PADDING_RATIO_V = 0.05f       
    const val TRAIL_SMOOTHING_ENABLED = true      
    const val TRAIL_MAX_POINTS = 25               
}