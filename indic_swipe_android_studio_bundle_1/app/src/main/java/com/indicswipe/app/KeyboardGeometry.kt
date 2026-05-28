package com.indicswipe.app

import android.content.Context
import android.graphics.RectF
import android.util.Log
import org.json.JSONObject
import kotlin.math.sqrt

class KeyboardGeometry(context: Context) {

    companion object {
        private const val TAG               = "KeyboardGeometry"
        private const val ASSET_NAME        = "keyboard_grid.json"
        private const val SPATIAL_GRID_SIZE = 6
        private const val CHAR_TOKEN_OFFSET = 4
        private const val DEFAULT_WIDTH     = 360f
        private const val DEFAULT_HEIGHT    = 220f
        private const val VERTICAL_PENALTY       = KeyboardConstants.VERTICAL_PENALTY
        private const val OUTSIDE_HITBOX_PENALTY = 0.35f
    }

    val trainWidth:  Float = KeyboardConstants.TRAIN_WIDTH
    val trainHeight: Float = KeyboardConstants.TRAIN_HEIGHT

    var viewWidth:  Float = DEFAULT_WIDTH
    var viewHeight: Float = DEFAULT_HEIGHT

    val scaleX: Float get() = trainWidth / viewWidth.coerceAtLeast(1f)
    val scaleY: Float get() = trainHeight / viewHeight.coerceAtLeast(1f)
    
    val keyCount: Int get() = allKeys.size

    data class Key(
        val char:        Char,
        val cx:          Float,
        val cy:          Float,
        val width:       Float,
        val height:      Float,
        val isSpecial:   Boolean = false,
        val specialName: String? = null
    ) {
        val left:   Float get() = cx - width  / 2f
        val right:  Float get() = cx + width  / 2f
        val top:    Float get() = cy - height / 2f
        val bottom: Float get() = cy + height / 2f
    }

    private val allKeys:    Array<Key>
    private val keyMap:     Map<Char, Key>
    private val specialKeys = mutableMapOf<String, Key>()

    private val spatialGrid: Array<Array<MutableList<Key>>>
    private val cellWidth:   Float
    private val cellHeight:  Float
    private val defaultKey = 'e'

    init {
        val t0 = System.currentTimeMillis()

        val keys = try {
            loadFromJson(context)
        } catch (e: Exception) {
            Log.e(TAG, "JSON load failed: ${e.message}", e)
            createDefaultKeys()
        }

        allKeys = keys
        keyMap  = allKeys
            .filter { !it.isSpecial && it.char in 'a'..'z' }
            .associateBy { it.char }

        cellWidth  = trainWidth  / SPATIAL_GRID_SIZE
        cellHeight = trainHeight / SPATIAL_GRID_SIZE

        spatialGrid = Array(SPATIAL_GRID_SIZE) {
            Array(SPATIAL_GRID_SIZE) { mutableListOf<Key>() }
        }

        for (key in allKeys) {
            val minCol = ((key.left   / trainWidth)  * SPATIAL_GRID_SIZE)
                .toInt().coerceIn(0, SPATIAL_GRID_SIZE - 1)
            val maxCol = ((key.right  / trainWidth)  * SPATIAL_GRID_SIZE)
                .toInt().coerceIn(0, SPATIAL_GRID_SIZE - 1)
            val minRow = ((key.top    / trainHeight) * SPATIAL_GRID_SIZE)
                .toInt().coerceIn(0, SPATIAL_GRID_SIZE - 1)
            val maxRow = ((key.bottom / trainHeight) * SPATIAL_GRID_SIZE)
                .toInt().coerceIn(0, SPATIAL_GRID_SIZE - 1)
            for (row in minRow..maxRow)
                for (col in minCol..maxCol)
                    spatialGrid[row][col].add(key)
        }

        val regular = allKeys.count { !it.isSpecial }
        val special = allKeys.count {  it.isSpecial }
        Log.d(TAG, "Loaded $regular regular + $special special keys " +
                   "in ${System.currentTimeMillis() - t0}ms")
        Log.d(TAG, "Special keys: ${specialKeys.keys.joinToString()}")
    }



    private fun loadFromJson(context: Context): Array<Key> {
        val json = try {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ FATAL: Could not open $ASSET_NAME: ${e.message}")
            throw e
        }
        
        Log.d(TAG, "Loaded JSON (${json.length} chars). Parsing...")
        val root = JSONObject(json)
        val qwerty = root.optJSONObject("qwerty_english")
            ?: throw IllegalStateException("Missing 'qwerty_english' in $ASSET_NAME")
            
        val rawW = qwerty.optDouble("width", DEFAULT_WIDTH.toDouble()).toFloat()
        val rawH = qwerty.optDouble("height", DEFAULT_HEIGHT.toDouble()).toFloat()
        

        val sx = trainWidth / rawW.coerceAtLeast(1f)
        val sy = trainHeight / rawH.coerceAtLeast(1f) 
        Log.d(TAG, "Training Grid: ${rawW}x${rawH} -> Scale: ${sx}x${sy}")
        
        val keyList = mutableListOf<Key>()
        
        val keysArray = qwerty.optJSONArray("keys")
        if (keysArray != null) {
            for (i in 0 until keysArray.length()) {
                parseRegularKey(keysArray.getJSONObject(i), sx, sy)?.let { keyList.add(it) }
            }
        }
        
        val specialArray = qwerty.optJSONArray("special_keys")
        if (specialArray != null) {
            for (i in 0 until specialArray.length()) {
                parseSpecialKey(specialArray.getJSONObject(i), sx, sy)?.let { key ->
                    keyList.add(key)
                    key.specialName?.let { name -> specialKeys[name] = key }
                }
            }
        }
        
        Log.d(TAG, "Parse complete: ${keyList.size} keys found.")
        return keyList.toTypedArray()
    }

    fun setDimensions(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        viewWidth = w
        viewHeight = h
        Log.d(TAG, "Dimensions set: ${w}x${h} | Scale: ${scaleX}x${scaleY}")
    }

    private fun parseRegularKey(obj: JSONObject, sx: Float, sy: Float): Key? {
        val label = obj.optString("label", "")
        if (label.length != 1) return null
        val char = label[0].lowercaseChar()
        if (char !in 'a'..'z') return null

        val hb = obj.optJSONObject("hitbox") ?: return null
        val kx = hb.optDouble("x", 0.0).toFloat() * sx
        val ky = hb.optDouble("y", 0.0).toFloat() * sy
        val kw = hb.optDouble("w", 32.0).toFloat() * sx
        val kh = hb.optDouble("h", 48.0).toFloat() * sy

        val paddingH = kw * KeyboardConstants.HITBOX_PADDING_RATIO_H
        val paddingV = kh * KeyboardConstants.HITBOX_PADDING_RATIO_V

        return Key(char, kx + kw / 2f, ky + kh / 2f, kw - 2 * paddingH, kh - 2 * paddingV)
    }

    private fun parseSpecialKey(obj: JSONObject, sx: Float, sy: Float): Key? {
        val label = obj.optString("label", "")
        if (label.isEmpty()) return null

        val hb = obj.optJSONObject("hitbox") ?: return null
        val kx = hb.optDouble("x", 0.0).toFloat() * sx
        val ky = hb.optDouble("y", 0.0).toFloat() * sy
        val kw = hb.optDouble("w", 50.0).toFloat() * sx
        val kh = hb.optDouble("h", 48.0).toFloat() * sy

        return Key('\u0000', kx + kw / 2f, ky + kh / 2f, kw, kh, true, label)
    }

    private fun createDefaultKeys(): Array<Key> {
        val keys    = mutableListOf<Key>()
        val rows    = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val kw      = trainWidth  / 10f
        val kh      = trainHeight / 4f
        val offsets = listOf(0f, kw * 0.5f, kw * 1.5f)
        val ys      = listOf(kh * 0.5f, kh * 1.5f, kh * 2.5f)

        for ((ri, row) in rows.withIndex()) {
            for ((ci, char) in row.withIndex()) {
                keys.add(Key(char, offsets[ri] + ci * kw + kw / 2f, ys[ri], kw, kh))
            }
        }
        listOf(
            Key('\u0000', 27f,   136f, 54f,  48f,  true, "shift"),
            Key('\u0000', 333f,  136f, 54f,  48f,  true, "backspace"),
            Key('\u0000', 27f,   190f, 54f,  52f,  true, "symbol_toggle"),
            Key('\u0000', 74f,   190f, 40f,  52f,  true, "comma"),
            Key('\u0000', 180f,  190f, 172f, 52f,  true, "space"),
            Key('\u0000', 287f,  190f, 42f,  52f,  true, "period"),
            Key('\u0000', 333f,  190f, 54f,  52f,  true, "enter")
        ).forEach { keys.add(it) }
        return keys.toTypedArray()
    }



    fun getAllKeys(): Array<Key> = allKeys

    fun getKeySize(char: Char): Pair<Float, Float>? {
        val key = keyMap[char.lowercaseChar()] ?: return null
        return Pair(key.width, key.height)
    }

    fun getSpecialKeyRect(name: String): RectF? {
        val key = specialKeys[name] ?: return null
        return RectF(key.left, key.top, key.right, key.bottom)
    }

    fun hitTestSpecial(tx: Float, ty: Float): String? {

        for (key in allKeys) {
            if (key.isSpecial && tx >= key.left && tx <= key.right && ty >= key.top && ty <= key.bottom) {
                return key.specialName
            }
        }



        for (key in allKeys) {
            if (!key.isSpecial && tx >= key.left && tx <= key.right && ty >= key.top && ty <= key.bottom) {
                return null
            }
        }

        var bestKey: Key? = null
        var minDistanceSq = Float.MAX_VALUE
        

        val scale = 1.15f

        for (key in allKeys) {
            if (!key.isSpecial) continue
            
            val expandedW = key.width * scale
            val expandedH = key.height * scale
            val left = key.cx - expandedW / 2f
            val right = key.cx + expandedW / 2f
            val top = key.cy - expandedH / 2f
            val bottom = key.cy + expandedH / 2f

            if (tx >= left && tx <= right && ty >= top && ty <= bottom) {
                val dx = tx - key.cx
                val dy = ty - key.cy
                val distSq = dx * dx + dy * dy
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq
                    bestKey = key
                }
            }
        }
        return bestKey?.specialName
    }

    fun tapKeyChar(tx: Float, ty: Float): Char? {

        for (key in allKeys) {
            if (!key.isSpecial && tx >= key.left && tx <= key.right && ty >= key.top && ty <= key.bottom) {
                return key.char
            }
        }
        


        val char = nearestKeyChar(tx, ty)
        val k = keyMap[char] ?: return null
        val dx = tx - k.cx
        val dy = ty - k.cy
        val distSq = dx * dx + dy * dy
        

        val thresholdSq = (k.width * 1.2f) * (k.width * 1.2f)
        return if (distSq < thresholdSq) char else null
    }

    fun nearestKeyCharEuclidean(px: Float, py: Float): Char {
        val cx = px.coerceIn(0f, trainWidth)
        val cy = py.coerceIn(0f, trainHeight)

        var bestKey = defaultKey
        var bestDistSq = Float.MAX_VALUE

        for (key in allKeys) {
            if (key.isSpecial) continue
            val dx = cx - key.cx
            val dy = cy - key.cy
            val distSq = dx * dx + dy * dy
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestKey = key.char
            }
        }
        return bestKey
    }

    fun nearestKeyChar(px: Float, py: Float): Char {
        val cx = px.coerceIn(0f, trainWidth)
        val cy = py.coerceIn(0f, trainHeight)

        val gridCol = ((cx / trainWidth)  * SPATIAL_GRID_SIZE).toInt().coerceIn(0, SPATIAL_GRID_SIZE - 1)
        val gridRow = ((cy / trainHeight) * SPATIAL_GRID_SIZE).toInt().coerceIn(0, SPATIAL_GRID_SIZE - 1)

        var bestKey   = defaultKey
        var bestScore = Float.MAX_VALUE

        for (dr in -1..1) {
            for (dc in -1..1) {
                val r = gridRow + dr
                val c = gridCol + dc
                if (r !in 0 until SPATIAL_GRID_SIZE || c !in 0 until SPATIAL_GRID_SIZE) continue
                for (key in spatialGrid[r][c]) {
                    if (key.isSpecial) continue
                    val score = scorePointToKey(cx, cy, key)
                    if (score < bestScore) {
                        bestScore = score
                        bestKey   = key.char
                    }
                }
            }
        }
        return bestKey
    }

    
    fun nearestKeyTokenId(px_train: Float, py_train: Float): Long {
        val char = nearestKeyChar(px_train, py_train)
        return (char.code - 'a'.code + 4).toLong()
    }

    
    fun getAlphabetAreaBounds(): Pair<Float, Float> {
        val qKey = keyMap['q'] ?: return Pair(0f, trainHeight)
        val mKey = keyMap['m'] ?: return Pair(0f, trainHeight)
        

        val rowHeight = (mKey.cy - qKey.cy) / 2.0f
        val top = qKey.cy - rowHeight / 2.0f
        val height = 3.0f * rowHeight
        
        return Pair(top.coerceAtLeast(0f), height)
    }

    private fun scorePointToKey(px: Float, py: Float, key: Key): Float {
        val dxOut = when {
            px < key.left  -> key.left  - px
            px > key.right -> px - key.right
            else           -> 0f
        }
        val dyOut = when {
            py < key.top    -> key.top    - py
            py > key.bottom -> py - key.bottom
            else            -> 0f
        }
        val inside = dxOut == 0f && dyOut == 0f

        val normDx = dxOut / key.width.coerceAtLeast(1f)
        val normDy = dyOut / key.height.coerceAtLeast(1f)
        val outsideCost = normDx * normDx + (normDy * VERTICAL_PENALTY) * (normDy * VERTICAL_PENALTY)

        val cDx = (px - key.cx) / key.width.coerceAtLeast(1f)
        val cDy = (py - key.cy) / key.height.coerceAtLeast(1f)
        val centerCost = cDx * cDx + (cDy * VERTICAL_PENALTY) * (cDy * VERTICAL_PENALTY)

        return if (inside) centerCost * 0.25f else centerCost + outsideCost * OUTSIDE_HITBOX_PENALTY
    }



    fun nearestTopKKeyTokens(px: Float, py: Float, k: Int = 3): LongArray {
        val cx = px.coerceIn(0f, trainWidth)
        val cy = py.coerceIn(0f, trainHeight)

        val keysWithDist = allKeys
            .filter { !it.isSpecial && it.char in 'a'..'z' }
            .map { key ->
                val dx = cx - key.cx
                val dy = cy - key.cy
                val distSq = dx * dx + dy * dy
                key to distSq
            }
            .sortedBy { it.second }
            .take(k)

        val tokens = LongArray(k) { 1L }
        for (i in keysWithDist.indices) {
            val char = keysWithDist[i].first.char.lowercaseChar()
            tokens[i] = (char.code - 'a'.code + CHAR_TOKEN_OFFSET).toLong()
        }
        return tokens
    }

    fun getKeyCenter(char: Char): Pair<Float, Float>? {
        val key = keyMap[char.lowercaseChar()] ?: return null
        return Pair(key.cx, key.cy)
    }

    fun getDebugInfo(): String {
        val jsonStatus = if (allKeys.any { !it.isSpecial }) "JSON" else "Default"
        return "Source: $jsonStatus, Keys: ${keyCount}, Grid: ${trainWidth}x${trainHeight}"
    }
}