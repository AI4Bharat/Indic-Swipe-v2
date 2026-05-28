package com.indicswipe.app

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DebugSwipeActivity : AppCompatActivity() {

    private lateinit var swipeView:   SwipeView
    private lateinit var debugOutput: TextView
    private lateinit var decoder:     SwipeDecoder
    private lateinit var geometry:    KeyboardGeometry

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_swipe)

        swipeView   = findViewById(R.id.debug_swipe_view)
        debugOutput = findViewById(R.id.debug_output_text)


        geometry = KeyboardGeometry(this)
        decoder  = SwipeDecoder(this)
        decoder.geometry = geometry

        swipeView.setKeyboardGeometry(geometry)
        swipeView.applyTheme(ThemeManager(this).currentTheme, true)

        swipeView.onSwipeComplete = { points -> handleSwipe(points) }

        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            debugOutput.text = "Ready...\n"
            swipeView.clearTrail()
        }

        setupSimulations()
        log("✅ Ready for swipe.")
        log("Geometry: ${geometry.getDebugInfo()}")


        scope.launch {
            delay(2000)
            runAllTests()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        decoder.close()
    }





    private fun setupSimulations() {
        findViewById<Button>(R.id.sim_tumhara).setOnClickListener   { simulateWord("tumhara")   }
        findViewById<Button>(R.id.sim_namaste).setOnClickListener   { simulateWord("namaste")   }
        findViewById<Button>(R.id.sim_google).setOnClickListener    { simulateWord("google")    }
        findViewById<Button>(R.id.sim_kaise).setOnClickListener     { simulateWord("kaise")     }
        findViewById<Button>(R.id.sim_shukriya).setOnClickListener  { simulateWord("shukriya")  }
        findViewById<Button>(R.id.sim_dhanyawad).setOnClickListener { simulateWord("dhanyawad") }
        findViewById<Button>(R.id.sim_khabar).setOnClickListener    { simulateWord("khabar")    }
        findViewById<Button>(R.id.sim_zindagi).setOnClickListener   { simulateWord("zindagi")   }
        findViewById<Button>(R.id.sim_koshish).setOnClickListener   { simulateWord("koshish")   }
        findViewById<Button>(R.id.sim_aapka).setOnClickListener     { simulateWord("aapka")     }
        findViewById<Button>(R.id.btn_test_all).setOnClickListener  { runAllTests()             }
    }





    private fun runAllTests() {
        scope.launch {
            val testWords = listOf(
                "tumhara", "namaste", "google", "kaise", "shukriya",
                "dhanyawad", "khabar", "zindagi", "koshish", "aapka"
            )

            log("\n=== BULK TEST: ${testWords.size} words ===\n")

            var passed = 0
            val t0 = System.currentTimeMillis()

            for (word in testWords) {
                log("Testing '$word'...")
                val result = simulateWordInternal(word)
                if (result.trim().lowercase() == word.lowercase()) {
                    passed++
                    log("  ✅ PASS → '$result'")
                } else {
                    log("  ❌ FAIL → got '$result'")
                }
                delay(150)
            }

            val elapsed = System.currentTimeMillis() - t0
            val pct = if (testWords.isNotEmpty())
                          (passed.toFloat() / testWords.size * 100f) else 0f

            log("\n=== RESULTS ===")
            log("Passed : $passed / ${testWords.size}")
            log("Accuracy: ${"%.1f".format(pct)}%")
            log("Total time: ${elapsed}ms\n")
        }
    }

    private suspend fun simulateWordInternal(word: String): String =
        withContext(Dispatchers.Default) {
            val points     = buildSimulatedPoints(word)
            val result = decoder.decodeDetailed(points)
            result.candidates.firstOrNull()?.word ?: ""
        }









    private fun buildSimulatedPoints(word: String): List<FloatArray> {
        val geom = swipeView.getGeometry() ?: return emptyList()
        val tw = KeyboardConstants.TRAIN_WIDTH
        val th = KeyboardConstants.TRAIN_HEIGHT


        val keyPoints = mutableListOf<Pair<Float, Float>>()
        var lastChar = ' '
        for (char in word.lowercase()) {
            if (char == lastChar) continue
            val center = geom.getKeyCenter(char) ?: continue
            keyPoints.add(center.first / tw to center.second / th)
            lastChar = char
        }

        if (keyPoints.isEmpty()) return emptyList()
        

        val hiResPath = mutableListOf<Pair<Float, Float>>()
        val stepsPerSegment = 30
        

        repeat(5) { hiResPath.add(keyPoints[0]) }

        for (i in 0 until keyPoints.size - 1) {
            val (p0x, p0y) = keyPoints[i]
            val (p1x, p1y) = keyPoints[i + 1]
            
            for (step in 1..stepsPerSegment) {
                val tLinear = step.toFloat() / stepsPerSegment

                val t = 0.5f * (1.0f - Math.cos(Math.PI * tLinear.toDouble()).toFloat())
                
                val x = p0x + (p1x - p0x) * t
                val y = p0y + (p1y - p0y) * t
                hiResPath.add(x to y)
            }

            repeat(3) { hiResPath.add(keyPoints[i+1]) }
        }
        

        repeat(5) { hiResPath.add(keyPoints.last()) }


        val targetCount = 100
        val n = hiResPath.size
        
        return List(targetCount) { i ->
            val idx = i.toFloat() * (n - 1) / (targetCount - 1).toFloat()
            val low = idx.toInt().coerceIn(0, n - 2)
            val high = (low + 1).coerceIn(0, n - 1)
            val alpha = idx - low.toFloat()

            val (pLowX, pLowY) = hiResPath[low]
            val (pHighX, pHighY) = hiResPath[high]

            val xNorm = pLowX * (1f - alpha) + pHighX * alpha
            val yNorm = pLowY * (1f - alpha) + pHighY * alpha

            floatArrayOf(
                xNorm.coerceIn(0f, 1f),
                yNorm.coerceIn(0f, 1f),
                (i * 20).toFloat()
            )
        }
    }





    private fun simulateWord(word: String) {
        log("\n--- SIMULATING: '$word' ---")
        val points = buildSimulatedPoints(word)
        log("Built ${points.size} points")


        val geom = swipeView.getGeometry()
        val keyPath = word.lowercase()
            .fold(StringBuilder()) { sb, c ->
                val center = geom?.getKeyCenter(c)
                if (center != null) sb.append(c) else sb.append('?')
                sb
            }.toString()
        log("Key path: $keyPath")

        handleSwipe(points)
    }

    private fun handleSwipe(points: List<FloatArray>) {
        if (points.isEmpty()) {
            log("⚠️ No points to decode")
            return
        }


        val first = points.first()
        val last  = points.last()
        log("Points: ${points.size}")
        log("  First: (${
            "%.3f".format(first[0])},${
            "%.3f".format(first[1])}) t=${first[2].toInt()}ms")
        log("  Last:  (${
            "%.3f".format(last[0])},${
            "%.3f".format(last[1])}) t=${last[2].toInt()}ms")

        scope.launch {
            val t0 = System.currentTimeMillis()

            val result: SwipeDecoder.DecodeResult = withContext(Dispatchers.Default) {
                try {
                    decoder.decodeDetailed(points)
                } catch (e: Exception) {
                    log("❌ Exception: ${e.message}")
                    SwipeDecoder.DecodeResult(emptyList(), 0, "")
                }
            }

            val elapsed = System.currentTimeMillis() - t0
            log("Decode: ${elapsed}ms")

            if (result.candidates.isEmpty()) {
                log("❌ No candidates returned")
                return@launch
            }

            log("Candidates:")
            result.candidates.take(5).forEachIndexed { i, cand ->
                log("  ${i + 1}. '${cand.word}' (${String.format("%.1f", cand.score)})")
            }
            if (result.candidates.isNotEmpty()) {
                log("🏆 WINNER: '${result.bestWord}'")
            }
        }
    }





    private fun log(message: String) {
        Log.d("DebugSwipeActivity", message)
        runOnUiThread {
            debugOutput.append("$message\n")


            val scrollView = debugOutput.parent
            if (scrollView is android.widget.ScrollView) {
                scrollView.post { scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN) }
            }
        }
    }
}