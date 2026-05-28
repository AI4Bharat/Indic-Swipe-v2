package com.indicswipe.app

import android.util.Log


object PerformanceTracker {
    private const val TAG = "PerfTracker"
    private var lastTouchDownTime = 0L

    fun onTouchDown() {
        lastTouchDownTime = System.currentTimeMillis()
    }

    fun logTapLatency(char: Char) {
        val latency = System.currentTimeMillis() - lastTouchDownTime
        if (latency > 50) {
            Log.w(TAG, "⚠️ High Tap Latency: ${latency}ms for '$char'")
        } else {
            Log.d(TAG, "✓ Tap Latency: ${latency}ms for '$char'")
        }
    }

    fun logSwipeMetrics(word: String, pathPoints: Int, decodeTime: Long, scores: Map<String, Float>) {
        Log.d(TAG, "━━━ Swipe Metrics: '$word' ━━━")
        Log.d(TAG, "  Points: $pathPoints")
        Log.d(TAG, "  Decode Time: ${decodeTime}ms")
        
        val sorted = scores.entries.sortedByDescending { it.value }.take(3)
        Log.d(TAG, "  Top Candidates: ${sorted.joinToString { "'${it.key}'=${String.format("%.1f", it.value)}" }}")
        
        if (sorted.size >= 2) {
            val gap = sorted[0].value - sorted[1].value
            Log.d(TAG, "  Confidence Gap: ${String.format("%.1f", gap)}")
        }
    }

    fun logDrawTime(timeMs: Long) {
        if (timeMs > 16) {
            Log.w(TAG, "🚫 Frame Drop: onDraw took ${timeMs}ms")
        }
    }
}