package com.indicswipe.app

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import java.io.File
import kotlin.math.*
import android.util.Log
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class HybridDecoderEvaluationTest {

    private lateinit var context: Context
    private lateinit var decoder: SwipeDecoder
    private lateinit var dictionary: DictionaryManager
    private lateinit var geometry: KeyboardGeometry

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dictionary = DictionaryManager(context)
        assertTrue("Dictionary failed to load", dictionary.loadDictionary())
        decoder = SwipeDecoder(context, dictionary)
        geometry = dictionary.getGeometry()
    }

    data class EvaluationResult(
        val word: String,
        val intended: String,
        val topCandidates: List<String>,
        val latencyMs: Long,
        val isID: Boolean,
        val isLong: Boolean
    )

    @Test
    fun runComprehensiveEvaluation() {

        val idWords = listOf("tumhara", "shukriya", "aapka", "kaise", "khabar", "zindagi", "koshish", "google")
        val oodWords = listOf("namaste", "dhanyawad")
        
        val allTestWords = idWords + oodWords
        val results = mutableListOf<EvaluationResult>()

        Log.d("EVAL", "Starting Hybrid Swipe Decoder Evaluation...")

        for (intended in allTestWords) {

            repeat(5) {
                val points = simulateSwipePath(intended)
                val startTime = System.currentTimeMillis()
                val decodeResult = decoder.decodeDetailed(points)
                val finalWord = dictionary.finalizeSwipeResult(
                    modelCandidates = decodeResult?.candidates ?: emptyList(),
                    isHindiMode = false
                )
                val latency = System.currentTimeMillis() - startTime
                
                results.add(EvaluationResult(
                    word = finalWord,
                    intended = intended,
                    topCandidates = decodeResult?.candidates?.map { it.first } ?: emptyList(),
                    latencyMs = latency,
                    isID = dictionary.isCompleteWord(intended),
                    isLong = intended.length >= 7
                ))
            }
        }

        reportMetrics(results)
    }

    private fun simulateSwipePath(word: String): List<FloatArray> {
        val path = mutableListOf<FloatArray>()
        var time = 0.0f
        
        for (i in word.indices) {
            val char = word[i]
            val center = geometry.getKeyCenter(char) ?: continue
            

            val targetX = center.first + (Math.random().toFloat() - 0.5f) * 10f
            val targetY = center.second + (Math.random().toFloat() - 0.5f) * 10f

            if (path.isEmpty()) {
                path.add(floatArrayOf(targetX, targetY, time))
            } else {
                val last = path.last()
                val dist = sqrt((targetX - last[0]).pow(2) + (targetY - last[1]).pow(2))
                val steps = max(5, (dist / 15f).toInt())
                
                for (s in 1..steps) {
                    val f = s.toFloat() / steps

                    val curve = sin(f * PI.toFloat()) * (Math.random().toFloat() - 0.5f) * 15f
                    val px = last[0] + (targetX - last[0]) * f + (last[1] - targetY) / (dist + 1) * curve
                    val py = last[1] + (targetY - last[1]) * f + (targetX - last[0]) / (dist + 1) * curve
                    time += 16f
                    path.add(floatArrayOf(px, py, time))
                }
            }

            repeat((Math.random() * 3 + 2).toInt()) {
                time += 16f
                path.add(floatArrayOf(targetX + (Math.random().toFloat() - 0.5f) * 2f, targetY + (Math.random().toFloat() - 0.5f) * 2f, time))
            }
        }
        return path
    }

    private fun reportMetrics(results: List<EvaluationResult>) {
        val total = results.size
        val top1 = results.count { it.word.lowercase() == it.intended.lowercase() }
        val top3 = results.count { res -> res.topCandidates.take(3).any { it.lowercase() == res.intended.lowercase() } }
        val top5 = results.count { res -> res.topCandidates.take(5).any { it.lowercase() == res.intended.lowercase() } }
        
        val idResults = results.filter { it.isID }
        val oodResults = results.filter { !it.isID }
        val shortResults = results.filter { it.intended.length <= 4 }
        val longResults = results.filter { it.intended.length >= 7 }

        val avgLatency = results.map { it.latencyMs }.average()
        

        val hallucinations = results.count { it.word.lowercase() != it.intended.lowercase() && it.word.isNotEmpty() && dictionary.isCompleteWord(it.word) }

        val out = StringBuilder()
        out.append("\n" + "═".repeat(80) + "\n")
        out.append("HYBRID SWIPE DECODER - END-TO-END EVALUATION REPORT\n")
        out.append("═".repeat(80) + "\n\n")

        out.append("OVERALL ACCURACY:\n")
        out.append("  Top-1 Accuracy:  %.1f%% (%d/%d)\n".format(top1.toFloat() / total * 100, top1, total))
        out.append("  Top-3 Accuracy:  %.1f%% (%d/%d)\n".format(top3.toFloat() / total * 100, top3, total))
        out.append("  Top-5 Accuracy:  %.1f%% (%d/%d)\n".format(top5.toFloat() / total * 100, top5, total))
        out.append("  Avg Latency:     %.1f ms\n".format(avgLatency))
        out.append("  Hallucination:   %.1f%% (%d cases)\n".format(hallucinations.toFloat() / total * 100, hallucinations))
        out.append("\n")

        out.append("BREAKDOWN BY CATEGORY:\n")
        out.append("  In-Dictionary:   %.1f%% Accuracy\n".format(if (idResults.isEmpty()) 0f else idResults.count { it.word.lowercase() == it.intended.lowercase() }.toFloat() / idResults.size * 100))
        out.append("  Out-of-Dictionary: %.1f%% Accuracy\n".format(if (oodResults.isEmpty()) 0f else oodResults.count { it.word.lowercase() == it.intended.lowercase() }.toFloat() / oodResults.size * 100))
        out.append("  Short Words (<=4): %.1f%% Accuracy\n".format(if (shortResults.isEmpty()) 0f else shortResults.count { it.word.lowercase() == it.intended.lowercase() }.toFloat() / shortResults.size * 100))
        out.append("  Long Words (>=7):  %.1f%% Accuracy\n".format(if (longResults.isEmpty()) 0f else longResults.count { it.word.lowercase() == it.intended.lowercase() }.toFloat() / longResults.size * 100))
        out.append("\n")

        out.append("CONFUSION LIST (FAILURES):\n")
        out.append("%-15s | %-15s | %-15s\n".format("Intended", "Predicted", "Status"))
        out.append("-".repeat(50) + "\n")
        

        val failures = results.filter { it.word.lowercase() != it.intended.lowercase() }
            .distinctBy { it.intended + it.word }
            .take(20)

        for (f in failures) {
            val status = if (f.word.isEmpty()) "REJECTED" else if (dictionary.isCompleteWord(f.word)) "HALLUCINATED" else "MISSPELLED"
            out.append("%-15s | %-15s | %-15s\n".format(f.intended, if (f.word.isEmpty()) "(none)" else f.word, status))
        }

        out.append("\n" + "═".repeat(80) + "\n")
        
        Log.i("EVAL_REPORT", out.toString())
        println(out.toString())
    }
}