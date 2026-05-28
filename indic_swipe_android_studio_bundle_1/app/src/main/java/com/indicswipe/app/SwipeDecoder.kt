package com.indicswipe.app

import android.content.Context
import android.util.Log
import ai.onnxruntime.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.*
import kotlin.math.*

class SwipeDecoder(private val context: Context) : AutoCloseable {
    
    constructor(context: Context, dictionary: DictionaryManager) : this(context) {
        this.dictionaryManager = dictionary
    }
    
    data class Candidate(val word: String, val score: Float)
    
    data class DecodeResult(
        val candidates: List<Candidate>,
        val decodeTimeMs: Long,
        val keyPath: String,
        val modelLogProbs: Map<String, Float> = emptyMap()
    ) {
        val bestWord: String get() = candidates.firstOrNull()?.word ?: ""
    }

    companion object {
        private const val TAG = "SwipeDecoder"
        private const val TARGET_POINTS = 150
        private const val RESAMPLE_COUNT = 40
        private const val D_MODEL = 256
        private const val VOCAB_SIZE = 30
        private const val SOS_IDX = 2
        private const val EOS_IDX = 3
    }

    private var env: OrtEnvironment? = null
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null

    
    private var modelAvailable: Boolean = false


    internal var geometry: KeyboardGeometry? = null
    internal var dictionaryManager: DictionaryManager? = null

    private var currentLang: String = ""
    private val modelLock = Any()

    init {
        try {
            env = OrtEnvironment.getEnvironment()
            loadModels("hindi")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Init Error", e)
        }
    }

    private fun loadModels(lang: String) {
        synchronized(modelLock) {
            val folder = lang.lowercase()
            if (folder == currentLang) return@synchronized


            encoderSession?.close(); encoderSession = null
            decoderSession?.close(); decoderSession = null
            currentLang = folder
            modelAvailable = false

            val encoderPath = "models/swipe/$folder/swipe_model_character_quant.onnx"
            val decoderPath = "models/swipe/$folder/swipe_decoder_character_quant.onnx"

            try {
                val sessOpts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(4) }
                val encoderBytes = context.assets.open(encoderPath).readBytes()
                val decoderBytes = context.assets.open(decoderPath).readBytes()
                encoderSession = env?.createSession(encoderBytes, sessOpts)
                decoderSession = env?.createSession(decoderBytes, sessOpts)
                modelAvailable = true
                Log.i(TAG, "🚀 Neural Core Calibrated for $folder")
            } catch (e: java.io.FileNotFoundException) {
                Log.w(TAG, "⚠️ No swipe ONNX model for '$folder' — using dictionary-only fallback")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Model Loading Error ($lang)", e)
            }
        }
    }

    
    private fun geometricFallbackDecode(
        rawPoints: List<FloatArray>,
        prevWord: String,
        startTime: Long
    ): DecodeResult {
        val keyPath = computeSkeletonPath(
            resampleTimeBased(rawPoints, KeyboardConstants.SAMPLING_CADENCE_MS)
                .take(TARGET_POINTS)
        )
        Log.v(TAG, "geometricFallbackDecode: keyPath='$keyPath' (no neural model)")

        val finalCandidates = mutableListOf<Candidate>()
        val sampledSwipe = samplePointsEquidistant(rawPoints, 40)

        dictionaryManager?.let { dict ->

            val candidatesFromDict = dict.findCandidates(keyPath, keyPath, prevWord)
            for ((word, _) in candidatesFromDict) {
                val ideal = getIdealPath(word, 40) ?: continue
                val rawDist = calculateShapeDistance(sampledSwipe, ideal)
                val wordLc = word.lowercase()

                val uniScore = dict.getUnigramScore(word)
                val uniBonus = if (uniScore > -20f) (uniScore + 20f) * 30f else 0f

                var bigramBonus = 0f
                if (prevWord.isNotEmpty()) {
                    val biScore = dict.getBigramScore(prevWord, word)
                    if (biScore > -20f) bigramBonus = (biScore + 20f).coerceIn(0f, 20f) * 8f
                }

                val editDist = calculateKeyboardEditDistance(wordLc, keyPath.lowercase())
                val spatialEditPenalty = editDist * 25f
                val spatialBonus = (calculatePhysicalPathLength(wordLc) / 100f) * 15f

                var precisionBonus = 0f
                if (editDist < 0.5f) precisionBonus = 80f

                var anchorPenalty = 0f
                if (wordLc.isNotEmpty() && keyPath.isNotEmpty()) {
                    if (wordLc.first() != keyPath.first().lowercaseChar()) anchorPenalty += 100f
                    if (wordLc.last() != keyPath.last().lowercaseChar()) anchorPenalty += 60f
                }

                val lenDiff = kotlin.math.abs(wordLc.length - keyPath.length)
                val lengthGuard = if (lenDiff >= 1) lenDiff * 15f else 0f

                val dictBonus = if (dict.isCompleteWord(wordLc)) KeyboardConstants.DICTIONARY_BONUS else 0f

                val finalScore = (rawDist * 100f) - uniBonus - bigramBonus - precisionBonus - dictBonus +
                        anchorPenalty + spatialEditPenalty - spatialBonus + lengthGuard

                finalCandidates.add(Candidate(word, finalScore))
            }
        }

        val sorted = finalCandidates.sortedBy { it.score }.take(6)
        Log.i(TAG, "Fallback Swipe | Path: '$keyPath' | Top: '${sorted.firstOrNull()?.word}'")
        return DecodeResult(sorted, System.currentTimeMillis() - startTime, keyPath)
    }

    override fun close() {
        encoderSession?.close(); decoderSession?.close(); env?.close()
    }

    fun setGeometry(geom: KeyboardGeometry) { this.geometry = geom }
    fun setDictionary(dict: DictionaryManager?) { this.dictionaryManager = dict }
    fun setLanguage(lang: String) { 
        Log.d(TAG, "Language: $lang")
        loadModels(lang)
    }

    fun clear() {
        synchronized(modelLock) {
            encoderSession?.close()
            decoderSession?.close()
            encoderSession = null
            decoderSession = null
            currentLang = ""
        }
    }

    fun decode(rawPoints: List<FloatArray>): List<Candidate> {
        return decodeDetailed(rawPoints).candidates
    }

    fun decodeDetailed(rawPoints: List<FloatArray>, prevWord: String = ""): DecodeResult {
        synchronized(modelLock) {
            val startTime = System.currentTimeMillis()
            if (rawPoints.isEmpty()) return DecodeResult(emptyList(), 0, "")


            if (!modelAvailable || encoderSession == null || env == null) {
                return geometricFallbackDecode(rawPoints, prevWord, startTime)
            }



        val resampled = resampleTimeBased(rawPoints, KeyboardConstants.SAMPLING_CADENCE_MS)
        Log.d(TAG, "decodeDetailed: prevWord='$prevWord' points=${rawPoints.size} resampled=${resampled.size}")
        val activePoints = resampled.take(TARGET_POINTS)
        
        val trajData = FloatArray(TARGET_POINTS * 6)
        val keysData = LongArray(TARGET_POINTS)
        val maskData = ByteBuffer.allocateDirect(TARGET_POINTS).order(ByteOrder.nativeOrder())
        
        for (i in 0 until TARGET_POINTS) maskData.put(i, 1.toByte())
        
        var lastPoint = activePoints[0]
        var lastVelX = 0f
        var lastVelY = 0f
        val activeCount = activePoints.size
        
        for (i in 0 until TARGET_POINTS) {
            val p = if (i < activeCount) activePoints[i] else null
            if (p != null) {
                val px = p[0]; val py = p[1]

                val npx = px / KeyboardConstants.TRAIN_WIDTH
                val npy = py / KeyboardConstants.TRAIN_HEIGHT
                
                val nlastX = lastPoint?.get(0)?.let { it / KeyboardConstants.TRAIN_WIDTH } ?: npx
                val nlastY = lastPoint?.get(1)?.let { it / KeyboardConstants.TRAIN_HEIGHT } ?: npy
                

                var dt = p[2] - (lastPoint?.get(2) ?: p[2])
                if (dt <= 0f) dt = KeyboardConstants.SAMPLING_CADENCE_MS
                
                val vx = (npx - nlastX) / dt
                val vy = (npy - nlastY) / dt
                val ax = (vx - lastVelX) / dt
                val ay = (vy - lastVelY) / dt
                
                trajData[i*6+0] = npx; trajData[i*6+1] = npy
                trajData[i*6+2] = vx.coerceIn(-KeyboardConstants.FEATURE_CLIP_VAL, KeyboardConstants.FEATURE_CLIP_VAL)
                trajData[i*6+3] = vy.coerceIn(-KeyboardConstants.FEATURE_CLIP_VAL, KeyboardConstants.FEATURE_CLIP_VAL)
                trajData[i*6+4] = ax.coerceIn(-KeyboardConstants.FEATURE_CLIP_VAL, KeyboardConstants.FEATURE_CLIP_VAL)
                trajData[i*6+5] = ay.coerceIn(-KeyboardConstants.FEATURE_CLIP_VAL, KeyboardConstants.FEATURE_CLIP_VAL)
                
                maskData.put(i, 0.toByte())
                val char = geometry?.nearestKeyCharEuclidean(px, py)?.lowercaseChar() ?: ' '
                val charIdx = char.code - 'a'.code
                keysData[i] = if (charIdx in 0..25) (charIdx + 4).toLong() else 1L
                
                lastPoint = p; lastVelX = vx; lastVelY = vy
            } else {
                trajData[i*6+0] = 0f; trajData[i*6+1] = 0f
                trajData[i*6+2] = 0f; trajData[i*6+3] = 0f
                trajData[i*6+4] = 0f; trajData[i*6+5] = 0f
                keysData[i] = 0L; maskData.put(i, 1.toByte())
            }
        }
        maskData.rewind()
        
        val keyPath = computeSkeletonPath(activePoints)
        val encoderInputs = mapOf(
            "trajectory_features" to OnnxTensor.createTensor(env!!, FloatBuffer.wrap(trajData), longArrayOf(1, TARGET_POINTS.toLong(), 6)),
            "nearest_keys" to OnnxTensor.createTensor(env!!, LongBuffer.wrap(keysData), longArrayOf(1, TARGET_POINTS.toLong())),
            "src_mask" to OnnxTensor.createTensor(env!!, maskData, longArrayOf(1, TARGET_POINTS.toLong()), OnnxJavaType.BOOL)
        )

        val memory = FloatArray(TARGET_POINTS * D_MODEL)
        try {
            encoderSession!!.run(encoderInputs).use { result ->
                (result[0] as OnnxTensor).floatBuffer.get(memory)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Encoder Fail", e); return DecodeResult(emptyList(), 0, "")
        } finally { encoderInputs.values.forEach { it.close() } }


        data class Beam(val tokens: List<Int>, val logProb: Float, val word: String, val finished: Boolean = false)

        val beamWidth = 1 
        var beams = listOf(Beam(listOf(SOS_IDX), 0f, ""))
        val maxTargetLen = 20
        

        val batchSize = beamWidth
        val tokensBuffer = ByteBuffer.allocateDirect(batchSize * maxTargetLen * 8).order(ByteOrder.nativeOrder()).asLongBuffer()
        val tMaskBuffer = ByteBuffer.allocateDirect(batchSize * maxTargetLen).order(ByteOrder.nativeOrder())
        val srcMaskBuffer = ByteBuffer.allocateDirect(batchSize * TARGET_POINTS).order(ByteOrder.nativeOrder())
        val memoryBuffer = ByteBuffer.allocateDirect(batchSize * TARGET_POINTS * D_MODEL * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        

        memoryBuffer.put(memory).rewind()
        maskData.rewind()
        for (i in 0 until TARGET_POINTS) srcMaskBuffer.put(i, maskData.get())
        srcMaskBuffer.rewind()

        val decodeInputs = mutableMapOf<String, OnnxTensor>()
        try {
            decodeInputs["memory"] = OnnxTensor.createTensor(env!!, memoryBuffer, longArrayOf(batchSize.toLong(), TARGET_POINTS.toLong(), D_MODEL.toLong()))
            decodeInputs["src_mask"] = OnnxTensor.createTensor(env!!, srcMaskBuffer, longArrayOf(batchSize.toLong(), TARGET_POINTS.toLong()), OnnxJavaType.BOOL)
            

            decodeInputs["target_tokens"] = OnnxTensor.createTensor(env!!, tokensBuffer, longArrayOf(batchSize.toLong(), maxTargetLen.toLong()))
            decodeInputs["target_mask"] = OnnxTensor.createTensor(env!!, tMaskBuffer, longArrayOf(batchSize.toLong(), maxTargetLen.toLong()), OnnxJavaType.BOOL)

            for (step in 0 until maxTargetLen - 1) {
                val activeBeams = beams.filter { !it.finished }
                if (activeBeams.isEmpty()) break
                
                val nextBeams = mutableListOf<Beam>()
                nextBeams.addAll(beams.filter { it.finished })
                

                tokensBuffer.rewind()
                tMaskBuffer.rewind()
                for (b in 0 until batchSize) {
                    val beam = if (b < activeBeams.size) activeBeams[b] else activeBeams.last()
                    for (i in 0 until maxTargetLen) {
                        val token = if (i < beam.tokens.size) beam.tokens[i] else 0
                        tokensBuffer.put(token.toLong())
                        tMaskBuffer.put(if (i < beam.tokens.size) 0.toByte() else 1.toByte())
                    }
                }
                tokensBuffer.rewind()
                tMaskBuffer.rewind()
                
                decoderSession!!.run(decodeInputs).use { result ->
                    val data = (result[0] as OnnxTensor).floatBuffer
                    for (b in 0 until activeBeams.size) {
                        val beam = activeBeams[b]
                        val seqIdx = beam.tokens.size - 1
                        

                        var bestIdx = -1
                        var bestProb = Float.NEGATIVE_INFINITY
                        
                        val offset = b * maxTargetLen * VOCAB_SIZE + seqIdx * VOCAB_SIZE
                        for (v in 0 until VOCAB_SIZE) {
                            var logit = data.get(offset + v)
                            





                            if (beam.tokens.size >= 2 && 
                                v == beam.tokens.last() && 
                                v == beam.tokens[beam.tokens.size - 2]) {
                                logit += KeyboardConstants.TRIPLE_CHAR_PENALTY
                            }

                            if (logit > bestProb) {
                                bestProb = logit
                                bestIdx = v
                            }
                        }
                        
                        if (bestIdx != -1) {

                            if (bestIdx == EOS_IDX) {
                                nextBeams.add(beam.copy(tokens = beam.tokens + bestIdx, logProb = beam.logProb + bestProb, finished = true))
                            } else if (bestIdx >= 4) {
                                val nextChar = (bestIdx - 4 + 'a'.code).toChar()
                                nextBeams.add(beam.copy(tokens = beam.tokens + bestIdx, logProb = beam.logProb + bestProb, word = beam.word + nextChar))
                            }
                        }
                    }
                }
                
                beams = nextBeams.sortedByDescending { it.logProb }.take(beamWidth)
                if (beams.all { it.finished }) break
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decoder Loop Fail", e)
        } finally {
            decodeInputs.values.forEach { it.close() }
        }


        val neuralGuesses = beams.map { it.word.lowercase() }
        val neuralGuess = neuralGuesses.firstOrNull() ?: ""
        val sampledSwipe = samplePointsEquidistant(rawPoints, 40)
        val pathPhysicalLength = calculateTotalPathDistanceRaw(rawPoints)
        val isExtremelyShort = pathPhysicalLength < KeyboardConstants.MIN_SWIPE_DISTANCE_PX * 1.5f
        
        val finalCandidates = mutableListOf<Candidate>()

        dictionaryManager?.let { dict ->

            val candidatesFromDict = dict.findCandidates(neuralGuess, keyPath, prevWord)
            val rescueWords = candidatesFromDict.map { it.first }.toMutableList()
            


            for (guess in neuralGuesses) {
                val cleaned = guess.replace(Regex("(.)\\1{2,}"), "$1$1")
                if (cleaned.isNotEmpty() && !rescueWords.contains(cleaned)) {
                    rescueWords.add(cleaned)
                }
            }
            

            val pathLc = keyPath.lowercase()
            
            for (word in rescueWords.distinct()) {
                val ideal = getIdealPath(word, 40) ?: continue
                val rawDist = calculateShapeDistance(sampledSwipe, ideal)
                
                val freq = dict.getFrequency(word)
                val wordLc = word.lowercase()
                


                val uniScore = dict.getUnigramScore(word)
                val uniBonus = if (uniScore > -20f) (uniScore + 20f) * 30f else 0f
                
                var bigramBonus = 0f
                if (prevWord.isNotEmpty()) {
                    val biScore = dict.getBigramScore(prevWord, word)
                    if (biScore > -20f) bigramBonus = (biScore + 20f).coerceIn(0f, 20f) * 8f
                }
                

                var neuralWeight = 0f
                val neuralTop = neuralGuesses.firstOrNull() ?: ""
                val neuralSim = dict.normalizedSimilarity(wordLc, neuralTop)

                if (neuralGuesses.contains(wordLc)) {
                    val rank = neuralGuesses.indexOf(wordLc)
                    neuralWeight = when(rank) {
                        0 -> 300f 
                        1 -> 200f
                        else -> 100f
                    }
                }
                


                val neuralSimBonus = neuralSim * 250f


                val editDist = calculateKeyboardEditDistance(wordLc, keyPath.lowercase())
                val spatialEditPenalty = editDist * 25f
                val spatialBonus = (calculatePhysicalPathLength(wordLc) / 100f) * 15f
                

                var precisionBonus = 0f
                if (editDist < 0.5f) {
                    precisionBonus = 80f
                }
                
                var anchorPenalty = 0f
                if (wordLc.isNotEmpty() && keyPath.isNotEmpty()) {
                    if (wordLc.first() != keyPath.first().lowercaseChar()) anchorPenalty += 100f
                    if (wordLc.last() != keyPath.last().lowercaseChar()) anchorPenalty += 60f
                }
                

                var lengthGuard = 0f
                val lenDiff = abs(wordLc.length - keyPath.length)
                if (lenDiff >= 1) {
                    lengthGuard = lenDiff * 15f
                }
                

                var dictBonus = 0f
                var stutterPenalty = 0f
                val isValid = dict.isCompleteWord(wordLc)
                
                if (isValid) {
                    dictBonus = KeyboardConstants.DICTIONARY_BONUS
                } else {


                    if (wordLc.length >= 2 && wordLc[wordLc.length - 1] == wordLc[wordLc.length - 2]) {
                        stutterPenalty = 150f
                    }
                }


                val finalScore = (rawDist * 100f) - neuralWeight - neuralSimBonus - uniBonus - bigramBonus - precisionBonus - dictBonus + anchorPenalty + spatialEditPenalty - spatialBonus + lengthGuard + stutterPenalty
                
                finalCandidates.add(Candidate(word, finalScore))
            }
        }



        val fallbackGuess = if (neuralGuess.isNotEmpty()) neuralGuess.replace(Regex("(.)\\1{2,}"), "$1$1") else ""
        if (fallbackGuess.isNotEmpty() && finalCandidates.none { it.word == fallbackGuess }) {


            finalCandidates.add(Candidate(fallbackGuess, 500f)) 
        }

        val sorted = finalCandidates.sortedBy { it.score }.take(6)
        

        Log.i(TAG, "Swipe Complete | Path: '$keyPath' | Neural Guess: '$neuralGuess'")
        sorted.take(3).forEachIndexed { i, c ->
            Log.i(TAG, "  Top ${i+1}: '${c.word}' | Score: ${c.score}")
        }

            return DecodeResult(sorted, System.currentTimeMillis() - startTime, keyPath)
        }
    }

    private fun samplePointsEquidistant(points: List<FloatArray>, targetCount: Int): List<Pair<Float, Float>> {
        if (points.isEmpty()) return emptyList()
        val distances = mutableListOf<Float>()
        var totalDist = 0f
        distances.add(0f)
        for (i in 1 until points.size) {
            val d = sqrt((points[i][0] - points[i-1][0]).pow(2) + (points[i][1] - points[i-1][1]).pow(2))
            totalDist += d
            distances.add(totalDist)
        }
        val result = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until targetCount) {
            val target = (i.toFloat() / (targetCount - 1)) * totalDist
            var low = 0; var high = distances.size - 1
            while (high - low > 1) {
                val mid = (low + high) / 2
                if (distances[mid] < target) low = mid else high = mid
            }
            val t = if (distances[high] - distances[low] > 0) (target - distances[low]) / (distances[high] - distances[low]) else 0f
            val x = points[low][0] + t * (points[high][0] - points[low][0])
            val y = points[low][1] + t * (points[high][1] - points[low][1])
            result.add(Pair(x, y))
        }
        return result
    }


    private fun getIdealPath(word: String, targetCount: Int): List<Pair<Float, Float>>? {
        val centers = word.map { geometry?.getKeyCenter(it) ?: return null }
        val points = mutableListOf<FloatArray>()
        centers.forEach { points.add(floatArrayOf(it.first, it.second)) }
        val equidistant = samplePointsEquidistant(points, targetCount)
        return equidistant
    }

    private fun calculateShapeDistance(sampled: List<Pair<Float, Float>>, ideal: List<Pair<Float, Float>>): Float {
        if (sampled.isEmpty() || ideal.isEmpty()) return 1000f
        


        val sMeanX = sampled.map { it.first }.average().toFloat()
        val sMeanY = sampled.map { it.second }.average().toFloat()
        val iMeanX = ideal.map { it.first }.average().toFloat()
        val iMeanY = ideal.map { it.second }.average().toFloat()

        var weightedSum = 0f
        var totalWeight = 0f
        
        for (i in sampled.indices) {
            val sx = sampled[i].first - sMeanX
            val sy = sampled[i].second - sMeanY
            val ix = ideal[i].first - iMeanX
            val iy = ideal[i].second - iMeanY
            
            val dist = sqrt((sx - ix).pow(2) + (sy - iy).pow(2))
            


            val weight = when {
                i < 3 || i > sampled.size - 4 -> 4.0f 
                else -> 1.0f
            }
            
            weightedSum += dist * weight
            totalWeight += weight
        }
        
        return weightedSum / totalWeight
    }

    private fun runSingleStep(tokens: List<Int>, memory: FloatArray, srcMask: ByteBuffer): FloatArray {
        val maxTargetLen = 20
        val tokensData = LongArray(maxTargetLen) { if (it < tokens.size) tokens[it].toLong() else 0L }
        val tMask = ByteBuffer.allocateDirect(maxTargetLen).order(ByteOrder.nativeOrder())
        for (i in 0 until maxTargetLen) tMask.put(i, if (i < tokens.size) 0.toByte() else 1.toByte()) 
        srcMask.rewind(); tMask.rewind()
        val inputs = mapOf(
            "memory" to OnnxTensor.createTensor(env!!, FloatBuffer.wrap(memory), longArrayOf(1, TARGET_POINTS.toLong(), D_MODEL.toLong())),
            "target_tokens" to OnnxTensor.createTensor(env!!, LongBuffer.wrap(tokensData), longArrayOf(1, maxTargetLen.toLong())),
            "src_mask" to OnnxTensor.createTensor(env!!, srcMask, longArrayOf(1, TARGET_POINTS.toLong()), OnnxJavaType.BOOL),
            "target_mask" to OnnxTensor.createTensor(env!!, tMask, longArrayOf(1, maxTargetLen.toLong()), OnnxJavaType.BOOL)
        )
        val out = FloatArray(VOCAB_SIZE)
        try {
            decoderSession!!.run(inputs).use {
                val data = (it[0] as OnnxTensor).floatBuffer
                val seqIdx = (tokens.size - 1).coerceAtMost(maxTargetLen - 1)
                for (v in 0 until VOCAB_SIZE) {
                    val logit = data.get(seqIdx * VOCAB_SIZE + v)
                    out[v] = if (logit.isNaN()) -100f else logit
                }
            }
        } finally { inputs.values.forEach { it.close() } }
        return out
    }

    private fun resampleTimeBased(points: List<FloatArray>, intervalMs: Float): List<FloatArray> {
        if (points.isEmpty()) return emptyList()
        val result = mutableListOf<FloatArray>()
        
        var nextTargetTime = points[0][2].toDouble()
        var i = 0
        var loopCount = 0
        var stationaryCount = 0
        val maxLoops = 2000

        while (i < points.size - 1 && loopCount < maxLoops) {
            loopCount++
            val p1 = points[i]
            val p2 = points[i + 1]

            val t1 = p1[2].toDouble()
            val t2 = p2[2].toDouble()

            if (nextTargetTime >= t1 && nextTargetTime <= t2) {
                val t = if (t2 != t1) (nextTargetTime - t1) / (t2 - t1) else 0.0
                val x = p1[0] + t.toFloat() * (p2[0] - p1[0])
                val y = p1[1] + t.toFloat() * (p2[1] - p1[1])
                


                val isStationary = result.isNotEmpty() && 
                                   abs(x - result.last()[0]) < 0.1f && 
                                   abs(y - result.last()[1]) < 0.1f
                
                if (isStationary) {
                    stationaryCount++
                } else {
                    stationaryCount = 0
                }
                
                if (stationaryCount < 2) {
                    result.add(floatArrayOf(x, y, nextTargetTime.toFloat()))
                }
                
                nextTargetTime += intervalMs
            } else if (nextTargetTime < t1) {
                nextTargetTime += intervalMs
            } else {
                i++
            }
        }
        

        if (result.size < 2 || (points.last()[2] - result.last()[2]) > intervalMs / 2) {
            result.add(points.last())
        }
        
        return result
    }

    private fun logSoftmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f; var sum = 0f
        for (v in logits) sum += exp(v - max)
        val lse = max + ln(sum)
        return FloatArray(logits.size) { i -> logits[i] - lse }
    }



    private fun computeSkeletonPath(points: List<FloatArray>): String {
        if (points.size < 2) return ""
        val sb = StringBuilder()
        var lastChar = ' '


        val PAUSE_VEL_THRESHOLD = 0.5f
        val CORNER_DEG_THRESHOLD = 35.0



        val velocities = mutableListOf<Float>()
        for (i in 1 until points.size) {
            val dx = points[i][0] - points[i-1][0]
            val dy = points[i][1] - points[i-1][1]
            velocities.add(sqrt(dx * dx + dy * dy))
        }
        val avgVelocity = if (velocities.isNotEmpty()) velocities.average().toFloat() else 1.0f
        val dynamicPauseThreshold = (avgVelocity * 0.55f).coerceIn(0.1f, 0.8f)

        for (i in points.indices) {
            val p = points[i]
            val c = geometry?.nearestKeyChar(p[0], p[1]) ?: continue


            if (i == 0 || i == points.size - 1) {
                if (c != lastChar) {
                    sb.append(c)
                    lastChar = c
                }
                continue
            }


            val pPrev = points[i - 1]
            val pNext = points[if (i + 1 < points.size) i + 1 else i]


            val dx = p[0] - pPrev[0]
            val dy = p[1] - pPrev[1]
            val velocity = sqrt(dx * dx + dy * dy)


            var isCorner = false
            if (i > 0 && i < points.size - 1) {
                val v1x = p[0] - pPrev[0]; val v1y = p[1] - pPrev[1]
                val v2x = pNext[0] - p[0]; val v2y = pNext[1] - p[1]
                val dot = v1x * v2x + v1y * v2y
                val mag1 = sqrt(v1x * v1x + v1y * v1y)
                val mag2 = sqrt(v2x * v2x + v2y * v2y)
                if (mag1 > 0.05f && mag2 > 0.05f) {
                    val angle = acos((dot / (mag1 * mag2)).coerceIn(-1f, 1f)) * (180.0 / PI)
                    if (angle > CORNER_DEG_THRESHOLD) isCorner = true
                }
            }


            if (velocity < dynamicPauseThreshold || isCorner) {
                if (c != lastChar) {
                    sb.append(c)
                    lastChar = c
                }
            }
        }
        
        val finalPath = sb.toString()
        Log.d(TAG, "HAS Filtered Path: '$finalPath' (Original size was ${points.size})")
        return finalPath
    }
    private fun calculateKeyboardEditDistance(s1: String, s2: String): Float {
        val n = s1.length; val m = s2.length
        val dp = Array(n + 1) { FloatArray(m + 1) }
        for (i in 0..n) dp[i][0] = i.toFloat()
        for (j in 0..m) dp[0][j] = j.toFloat()
        
        for (i in 1..n) {
            for (j in 1..m) {
                if (s1[i-1] == s2[j-1]) {
                    dp[i][j] = dp[i-1][j-1]
                } else {
                    val c1 = geometry?.getKeyCenter(s1[i-1])
                    val c2 = geometry?.getKeyCenter(s2[j-1])
                    val rawDist = if (c1 != null && c2 != null) {
                        sqrt((c1.first - c2.first).pow(2) + (c1.second - c2.second).pow(2)) / 50f
                    } else 1f
                    


                    val substitutionPenalty = if (rawDist < 1.0f) 0.8f else rawDist
                    


                    val insertionCost = if (i > 1 && s1[i-1] == s1[i-2]) 0.45f else 1.2f
                    
                    dp[i][j] = minOf(
                        dp[i-1][j] + insertionCost,
                        dp[i][j-1] + 1.2f,
                        dp[i-1][j-1] + substitutionPenalty
                    )
                }
            }
        }
        return dp[n][m]
    }

    private fun calculatePhysicalPathLength(word: String): Float {
        var length = 0f
        val centers = word.mapNotNull { geometry?.getKeyCenter(it) }
        for (i in 1 until centers.size) {
            length += sqrt((centers[i].first - centers[i-1].first).pow(2) + (centers[i].second - centers[i-1].second).pow(2))
        }
        return length
    }

    private fun calculateTotalPathDistanceRaw(points: List<FloatArray>): Float {
        var total = 0f
        for (i in 1 until points.size) {
            val dx = points[i][0] - points[i-1][0]
            val dy = points[i][1] - points[i-1][1]
            total += sqrt(dx * dx + dy * dy)
        }
        return total
    }
}