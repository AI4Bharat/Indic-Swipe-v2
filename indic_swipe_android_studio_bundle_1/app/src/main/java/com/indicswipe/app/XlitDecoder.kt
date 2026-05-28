package com.indicswipe.app

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.nio.FloatBuffer
import java.nio.LongBuffer


class XlitDecoder(context: Context) {

    companion object {
        private const val TAG = "XlitDecoder"

        
        private const val ASSET_XLIT_DIR = "models/xlit"

        
        private const val MAX_OUTPUT_LENGTH = 24

        
        private const val MAX_INPUT_LENGTH = 100

        
        private const val BOS_IDX = 0  
        private const val PAD_IDX = 1  
        private const val EOS_IDX = 2  
        private const val UNK_IDX = 3  

        
        private val LANG_TAGS = mapOf(
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
            "kashmiri" to "__ks__",
            "kashmir" to "__ks__",
            "nepali" to "__ne__",
            "sindhi_arab" to "__sd__",
            "sindhi_dev" to "__sdd__",
            "sindhi" to "__sdd__",   
            "konkani" to "__gom__",
            "manipuri" to "__mni__",
            "bodo" to "__brx__",
            "dogri" to "__doi__",    
            "santali" to "__sat__",  
        )
    }

    private var currentLangTag = "__hi__"

    fun setLanguage(langId: String) {
        currentLangTag = LANG_TAGS[langId] ?: "__hi__"
        transliterationCache.clear()
        topKCache.clear()
        
        
        
        cachedEncoderInput = null
        cachedEncoderMemory = null
        cachedEncoderShape = null
        Log.d(TAG, "Transliteration language set to: $langId (tag: $currentLangTag)")
    }


    
    
    

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null

    
    private val srcVocab = mutableListOf<String>()
    private val tgtVocab = mutableListOf<String>()

    
    private val srcTokenToIdx = mutableMapOf<String, Int>()

    
    private var initializationError: String? = null

    
    private val transliterationCache = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 128
    }
    private val topKCache = object : LinkedHashMap<String, List<String>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?) = size > 64
    }

    
    private var cachedEncoderInput: String? = null
    private var cachedEncoderMemory: FloatArray? = null
    private var cachedEncoderShape: LongArray? = null

    
    private var logitsBuffer: FloatArray? = null

    
    val isReady: Boolean
        get() = encoderSession != null && decoderSession != null && initializationError == null

    
    
    

    init {
        try {
            val startTime = System.currentTimeMillis()

            
            loadVocabulary(context)

            
            loadModels(context)

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "✓ XlitDecoder initialized in ${elapsed}ms " +
                    "(src=${srcVocab.size}, tgt=${tgtVocab.size})")

        } catch (e: Exception) {
            initializationError = e.message
            Log.e(TAG, "✗ XlitDecoder initialization failed: ${e.message}", e)
        } catch (e: Error) {
            
            initializationError = e.message
            Log.e(TAG, "✗ XlitDecoder initialization fatal error: ${e.message}", e)
        }
    }

    
    private fun loadVocabulary(context: Context) {
        val vocabJson = context.assets
            .open("$ASSET_XLIT_DIR/vocab.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(vocabJson)

        
        val srcArray = root.getJSONArray("src")
        for (i in 0 until srcArray.length()) {
            val token = srcArray.getString(i)
            srcVocab.add(token)
            srcTokenToIdx[token] = i
        }

        
        val tgtArray = root.getJSONArray("tgt")
        for (i in 0 until tgtArray.length()) {
            tgtVocab.add(tgtArray.getString(i))
        }

        
        if (root.has("special_tokens")) {
            val st = root.getJSONObject("special_tokens")
            val bosFromFile = st.optInt("bos", BOS_IDX)
            val padFromFile = st.optInt("pad", PAD_IDX)
            val eosFromFile = st.optInt("eos", EOS_IDX)
            val unkFromFile = st.optInt("unk", UNK_IDX)
            if (bosFromFile != BOS_IDX || padFromFile != PAD_IDX ||
                eosFromFile != EOS_IDX || unkFromFile != UNK_IDX) {
                Log.w(TAG, "Special token mismatch in vocab.json! " +
                    "bos=$bosFromFile pad=$padFromFile eos=$eosFromFile unk=$unkFromFile")
            }
        }

        Log.d(TAG, "Loaded vocabulary: src=${srcVocab.size}, tgt=${tgtVocab.size}")
    }

    
    private fun loadModels(context: Context) {
        val sessionOptions = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(4)
        }

        val t0 = System.currentTimeMillis()
        
        val encoderBytes = context.assets
            .open("$ASSET_XLIT_DIR/indicxlit_encoder.onnx")
            .use { it.readBytes() }
        encoderSession = env.createSession(encoderBytes, sessionOptions)
        val t1 = System.currentTimeMillis()
        Log.d(TAG, "Encoder loaded in ${t1 - t0}ms")

        
        val decoderBytes = context.assets
            .open("$ASSET_XLIT_DIR/indicxlit_decoder.onnx")
            .use { it.readBytes() }
        decoderSession = env.createSession(decoderBytes, sessionOptions)
        val t2 = System.currentTimeMillis()
        Log.d(TAG, "Decoder loaded in ${t2 - t1}ms")

        Log.d(TAG, "Loaded ONNX models (Total: ${t2 - t0}ms)")
    }

    
    
    

    
    fun transliterate(word: String): String {
        val w = word.lowercase()
        
        val cacheKey = "${currentLangTag}:$w"
        transliterationCache[cacheKey]?.let { return it }
        val result = runTransliteration(w)
        if (result.isNotEmpty()) transliterationCache[cacheKey] = result
        return result
    }

    private fun runTransliteration(text: String): String {
        if (!isReady) {
            Log.w(TAG, "Decoder not ready: $initializationError")
            return ""
        }

        val input = text.lowercase().trim()

        if (input.isEmpty()) {
            return ""
        }

        if (input.length > MAX_INPUT_LENGTH) {
            Log.w(TAG, "Input too long (${input.length} > $MAX_INPUT_LENGTH), truncating")
            return transliterate(input.substring(0, MAX_INPUT_LENGTH))
        }

        return try {
            greedyDecode(input)
        } catch (e: Exception) {
            Log.e(TAG, "Transliteration failed for '$input': ${e.message}", e)
            ""
        }
    }

    
    fun transliterateAll(words: List<String>): List<String> {
        return words.map { transliterate(it) }
    }

    
    
    

    
    private fun tokenizeSource(text: String): LongArray {
        val tokens = mutableListOf<Long>()

        
        val langTagIdx = srcTokenToIdx[currentLangTag] ?: UNK_IDX
        tokens.add(langTagIdx.toLong())

        
        for (char in text) {
            val charStr = char.toString()
            val idx = srcTokenToIdx[charStr] ?: UNK_IDX
            tokens.add(idx.toLong())
        }

        
        tokens.add(EOS_IDX.toLong())

        return tokens.toLongArray()
    }

    
    private fun detokenize(tokenIds: List<Int>): String {
        val result = StringBuilder()

        for (i in tokenIds.indices) {
            
            if (i == 0) continue

            val idx = tokenIds[i]

            
            if (idx == EOS_IDX) break

            
            if (idx == BOS_IDX || idx == PAD_IDX || idx == UNK_IDX) continue

            
            if (idx >= 0 && idx < tgtVocab.size) {
                result.append(tgtVocab[idx])
            }
        }

        return result.toString()
    }

    
    
    

    
    private fun runEncoder(text: String): Pair<FloatArray, LongArray> {
        
        if (text == cachedEncoderInput && cachedEncoderMemory != null && cachedEncoderShape != null) {
            return cachedEncoderMemory!! to cachedEncoderShape!!
        }

        val encoder = encoderSession ?: throw IllegalStateException("Encoder not loaded")
        val srcIds = tokenizeSource(text)
        val seqLen = srcIds.size.toLong()

        var srcTensor: OnnxTensor? = null
        var encoderResult: OrtSession.Result? = null

        try {
            srcTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(srcIds),
                longArrayOf(1, seqLen)
            )

            encoderResult = encoder.run(mapOf("src_tokens" to srcTensor))

            val outputTensor = encoderResult[0] as OnnxTensor
            val buffer = outputTensor.floatBuffer

            val memory = FloatArray(buffer.remaining())
            buffer.get(memory)

            
            
            val rawShape = outputTensor.info.shape 

            
            cachedEncoderInput = text
            cachedEncoderMemory = memory
            cachedEncoderShape = rawShape

            return memory to rawShape

        } finally {
            srcTensor?.close()
            encoderResult?.close()
        }
    }

    
    private fun greedyDecode(text: String): String {
        val decoder = decoderSession ?: return ""

        
        val (encoderMemory, encoderShape) = runEncoder(text)

        
        
        

        
        val outputTokens = mutableListOf(EOS_IDX)

        
        var prevLongArray = LongArray(MAX_OUTPUT_LENGTH + 1)
        prevLongArray[0] = EOS_IDX.toLong()

        
        var encoderOutTensor: OnnxTensor? = null

        try {
            encoderOutTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(encoderMemory),
                encoderShape
            )

            for (step in 0 until MAX_OUTPUT_LENGTH) {
                
                val nextToken = decoderStepFast(
                    decoder = decoder,
                    encoderOut = encoderOutTensor,
                    prevLongArray = prevLongArray,
                    prevLen = outputTokens.size
                )

                
                if (nextToken == EOS_IDX) {
                    break
                }

                
                outputTokens.add(nextToken)
                prevLongArray[outputTokens.size - 1] = nextToken.toLong()
            }

        } finally {
            encoderOutTensor?.close()
        }

        
        val result = detokenize(outputTokens)

        Log.d(TAG, "transliterate('$text') → '$result'")

        return result
    }

    
    private fun decoderStep(
        decoder: OrtSession,
        encoderOut: OnnxTensor,
        prevTokens: List<Int>
    ): Int {
        val prevArray = prevTokens.map { it.toLong() }.toLongArray()
        val prevLen = prevArray.size.toLong()

        var prevTensor: OnnxTensor? = null
        var decoderResult: OrtSession.Result? = null

        try {
            prevTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(prevArray),
                longArrayOf(1, prevLen)
            )

            
            decoderResult = decoder.run(
                mapOf(
                    "prev_tokens" to prevTensor,
                    "encoder_out" to encoderOut
                )
            )

            val logitsTensor = decoderResult[0] as OnnxTensor
            val logitsBuffer = logitsTensor.floatBuffer

            
            val logits = FloatArray(logitsBuffer.remaining())
            logitsBuffer.get(logits)

            
            val vocabSize = tgtVocab.size
            val lastPosition = prevTokens.size - 1
            val offset = lastPosition * vocabSize

            
            var bestIdx = 0
            var bestScore = Float.NEGATIVE_INFINITY

            for (v in 0 until vocabSize) {
                val logitIdx = offset + v

                
                if (logitIdx >= logits.size) {
                    Log.w(TAG, "Logit index out of bounds: $logitIdx >= ${logits.size}")
                    break
                }

                val score = logits[logitIdx]
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = v
                }
            }

            return bestIdx

        } finally {
            prevTensor?.close()
            decoderResult?.close()
        }
    }

    
    private fun decoderStepFast(
        decoder: OrtSession,
        encoderOut: OnnxTensor,
        prevLongArray: LongArray,
        prevLen: Int
    ): Int {
        var prevTensor: OnnxTensor? = null
        var decoderResult: OrtSession.Result? = null

        try {
            
            prevTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(prevLongArray, 0, prevLen),
                longArrayOf(1, prevLen.toLong())
            )

            decoderResult = decoder.run(
                mapOf(
                    "prev_tokens" to prevTensor,
                    "encoder_out" to encoderOut
                )
            )

            val logitsTensor = decoderResult[0] as OnnxTensor
            val rawBuffer = logitsTensor.floatBuffer
            val totalSize = rawBuffer.remaining()

            
            val logits = if (logitsBuffer != null && logitsBuffer!!.size >= totalSize) {
                logitsBuffer!!
            } else {
                FloatArray(totalSize).also { logitsBuffer = it }
            }
            rawBuffer.get(logits, 0, totalSize)

            
            val vocabSize = tgtVocab.size
            val offset = (prevLen - 1) * vocabSize

            
            var bestIdx = 0
            var bestScore = Float.NEGATIVE_INFINITY

            val end = minOf(offset + vocabSize, totalSize)
            for (i in offset until end) {
                if (logits[i] > bestScore) {
                    bestScore = logits[i]
                    bestIdx = i - offset
                }
            }

            return bestIdx

        } finally {
            prevTensor?.close()
            decoderResult?.close()
        }
    }

    
    
    

    
    private data class BeamNode(
        val tokens: List<Int>,
        val score: Float,
        val finished: Boolean = false
    ) : Comparable<BeamNode> {
        override fun compareTo(other: BeamNode): Int =
            other.score.compareTo(this.score)
    }

    
    suspend fun transliterateWithBeamSearch(text: String, beamSize: Int = 3): String {
        return transliterateGetTopK(text, beamSize).firstOrNull() ?: ""
    }

    
    suspend fun transliterateGetTopK(text: String, k: Int = 3): List<String> {
        if (!isReady) return emptyList()

        val input = text.lowercase().trim()
        if (input.isEmpty()) return emptyList()

        
        val cacheKey = "${currentLangTag}:$input:$k"
        topKCache[cacheKey]?.let { return it }

        return try {
            val decoder = decoderSession ?: return emptyList()

            
            val (encoderMemory, encoderShape) = runEncoder(input)

            
            var beams = mutableListOf(
                BeamNode(tokens = listOf(EOS_IDX), score = 0f)
            )
            val completedBeams = mutableListOf<BeamNode>()

            var encoderOutTensor: OnnxTensor? = null

            try {
                encoderOutTensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(encoderMemory),
                    encoderShape
                )

                for (step in 0 until MAX_OUTPUT_LENGTH) {
                    kotlinx.coroutines.yield() 
                    
                    val allCandidates = mutableListOf<BeamNode>()

                    for (beam in beams) {
                        if (beam.finished) {
                            completedBeams.add(beam)
                            continue
                        }

                        
                        val topTokens = getTopKTokens(
                            decoder = decoder,
                            encoderOut = encoderOutTensor,
                            prevTokens = beam.tokens,
                            k = k
                        )

                        for ((tokenIdx, logProb) in topTokens) {
                            if (tokenIdx == EOS_IDX) {
                                completedBeams.add(
                                    BeamNode(
                                        tokens = beam.tokens + tokenIdx,
                                        score = beam.score + logProb,
                                        finished = true
                                    )
                                )
                            } else {
                                allCandidates.add(
                                    BeamNode(
                                        tokens = beam.tokens + tokenIdx,
                                        score = beam.score + logProb
                                    )
                                )
                            }
                        }
                    }

                    
                    beams = allCandidates
                        .sortedByDescending { it.score }
                        .take(k)
                        .toMutableList()

                    if (beams.isEmpty()) break
                }

            } finally {
                encoderOutTensor?.close()
            }

            
            val allBeams = (completedBeams + beams)
                .filter { it.tokens.size > 1 }
                .sortedByDescending { it.score / it.tokens.size } 
            
            val results = allBeams.map { detokenize(it.tokens) }
                .distinct()
                .filter { it.isNotEmpty() }
                .take(k)

            
            if (results.isNotEmpty()) topKCache[cacheKey] = results
            results

        } catch (e: kotlinx.coroutines.CancellationException) {
            
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "transliterateGetTopK failed for '$input': ${e.message}", e)
            listOf(transliterate(input))
        }
    }

    
    private suspend fun beamSearchDecode(text: String, beamSize: Int): String {
        val decoder = decoderSession ?: return ""

        
        val (encoderMemory, encoderShape) = runEncoder(text)

        
        var beams = mutableListOf(
            BeamNode(tokens = listOf(EOS_IDX), score = 0f)
        )
        val completedBeams = mutableListOf<BeamNode>()

        var encoderOutTensor: OnnxTensor? = null

        try {
            encoderOutTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(encoderMemory),
                encoderShape
            )

            for (step in 0 until MAX_OUTPUT_LENGTH) {
                kotlinx.coroutines.yield()
                
                val allCandidates = mutableListOf<BeamNode>()

                for (beam in beams) {
                    if (beam.finished) {
                        completedBeams.add(beam)
                        continue
                    }

                    
                    val topTokens = getTopKTokens(
                        decoder = decoder,
                        encoderOut = encoderOutTensor,
                        prevTokens = beam.tokens,
                        k = beamSize
                    )

                    for ((tokenIdx, logProb) in topTokens) {
                        if (tokenIdx == EOS_IDX) {
                            completedBeams.add(
                                BeamNode(
                                    tokens = beam.tokens + tokenIdx,
                                    score = beam.score + logProb,
                                    finished = true
                                )
                            )
                        } else {
                            allCandidates.add(
                                BeamNode(
                                    tokens = beam.tokens + tokenIdx,
                                    score = beam.score + logProb
                                )
                            )
                        }
                    }
                }

                
                beams = allCandidates
                    .sortedByDescending { it.score }
                    .take(beamSize)
                    .toMutableList()

                if (beams.isEmpty()) break
            }

        } finally {
            encoderOutTensor?.close()
        }

        
        val allBeams = completedBeams + beams
        val best = allBeams
            .filter { it.tokens.size > 1 }
            .maxByOrNull { it.score / it.tokens.size } 

        return if (best != null) {
            detokenize(best.tokens)
        } else {
            ""
        }
    }

    
    private fun getTopKTokens(
        decoder: OrtSession,
        encoderOut: OnnxTensor,
        prevTokens: List<Int>,
        k: Int
    ): List<Pair<Int, Float>> {
        val prevLen = prevTokens.size
        val prevArray = LongArray(prevLen)
        for (i in 0 until prevLen) prevArray[i] = prevTokens[i].toLong()

        var prevTensor: OnnxTensor? = null
        var decoderResult: OrtSession.Result? = null

        try {
            prevTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(prevArray),
                longArrayOf(1, prevLen.toLong())
            )

            decoderResult = decoder.run(
                mapOf(
                    "prev_tokens" to prevTensor,
                    "encoder_out" to encoderOut
                )
            )

            val logitsTensor = decoderResult[0] as OnnxTensor
            val rawBuffer = logitsTensor.floatBuffer
            val totalSize = rawBuffer.remaining()

            
            val logits = if (logitsBuffer != null && logitsBuffer!!.size >= totalSize) {
                logitsBuffer!!
            } else {
                FloatArray(totalSize).also { logitsBuffer = it }
            }
            rawBuffer.get(logits, 0, totalSize)

            val vocabSize = tgtVocab.size
            val offset = (prevLen - 1) * vocabSize
            
            
            val stepLogits = FloatArray(vocabSize)
            var maxVal = Float.NEGATIVE_INFINITY
            for (v in 0 until vocabSize) {
                val idx = offset + v
                val score = if (idx < totalSize) logits[idx] else Float.NEGATIVE_INFINITY
                stepLogits[v] = score
                if (score > maxVal) maxVal = score
            }

            var expSum = 0.0
            for (v in stepLogits) {
                if (v > Float.NEGATIVE_INFINITY) {
                    expSum += kotlin.math.exp((v - maxVal).toDouble())
                }
            }
            val logExpSum = kotlin.math.ln(expSum).toFloat()

            
            
            val results = mutableListOf<Pair<Int, Float>>()
            
            
            val topIndices = IntArray(k) { -1 }
            val topScores = FloatArray(k) { Float.NEGATIVE_INFINITY }
            
            for (v in 0 until vocabSize) {
                val logit = stepLogits[v]
                if (logit > topScores[k - 1]) {
                    
                    var i = k - 1
                    while (i > 0 && logit > topScores[i - 1]) {
                        topScores[i] = topScores[i - 1]
                        topIndices[i] = topIndices[i - 1]
                        i--
                    }
                    topScores[i] = logit
                    topIndices[i] = v
                }
            }
            
            for (i in 0 until k) {
                if (topIndices[i] != -1) {
                    val logProb = topScores[i] - maxVal - logExpSum
                    results.add(topIndices[i] to logProb)
                }
            }

            return results

        } finally {
            prevTensor?.close()
            decoderResult?.close()
        }
    }

    
    private fun logSoftmax(logits: FloatArray): FloatArray {
        var maxVal = Float.NEGATIVE_INFINITY
        for (v in logits) {
            if (v > maxVal) maxVal = v
        }

        var expSum = 0.0
        for (v in logits) {
            expSum += kotlin.math.exp((v - maxVal).toDouble())
        }
        val logExpSum = kotlin.math.ln(expSum)

        return FloatArray(logits.size) { i ->
            ((logits[i] - maxVal).toDouble() - logExpSum).toFloat()
        }
    }

    
    
    

    
    fun close() {
        try {
            encoderSession?.close()
            decoderSession?.close()
            encoderSession = null
            decoderSession = null
            transliterationCache.clear()
            topKCache.clear()
            cachedEncoderInput = null
            cachedEncoderMemory = null
            cachedEncoderShape = null
            logitsBuffer = null
            Log.d(TAG, "XlitDecoder closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing XlitDecoder: ${e.message}", e)
        }
    }

    
    
    

    
    fun getDebugInfo(): Map<String, Any?> {
        return mapOf(
            "isReady" to isReady,
            "initializationError" to initializationError,
            "srcVocabSize" to srcVocab.size,
            "tgtVocabSize" to tgtVocab.size,
            "encoderLoaded" to (encoderSession != null),
            "decoderLoaded" to (decoderSession != null)
        )
    }

    
    fun runSelfTest(): Map<String, String> {
        val testWords = listOf("namaste", "bharat", "hindi", "keyboard", "swipe")
        val results = mutableMapOf<String, String>()

        for (word in testWords) {
            results[word] = transliterate(word)
        }

        Log.d(TAG, "Self-test results: $results")
        return results
    }
}