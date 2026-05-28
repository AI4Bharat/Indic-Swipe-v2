package com.indicswipe.app

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

class DictionaryManager(private val context: Context) {

    private val trie = VocabularyTrie()
    val vocabularyTrie: VocabularyTrie get() = trie

    @Volatile
    private var isLoaded = false
    private val loadLock = Any()

    private val wordFrequencies = mutableMapOf<String, Float>()
    private var kenlm: LanguageModel? = null

    companion object {
        private const val TAG = "DictionaryManager"
        private const val VOCAB_FILE = "vocabulary.txt"

        private const val SCORE_EXACT_MATCH = 100f
        private const val SCORE_EDIT_DIST_1_BASE = 180f
        private const val SCORE_EDIT_DIST_2_BASE = 120f
        private const val SCORE_PREFIX_MATCH_BASE = 90f
        private const val SCORE_SUBSEQ_BASE = 80f

        private const val PENALTY_PER_EDIT_DIST_1 = 15f
        private const val PENALTY_PER_EDIT_DIST_2 = 12f
        private const val PENALTY_PER_EDIT_PREFIX = 8f
        

        private val PENALTY_NOT_IN_KEYPATH get() = abs(KeyboardConstants.PATH_LEN_MISMATCH_PENALTY)
        private val BONUS_DICT_WORD get() = KeyboardConstants.DICTIONARY_BONUS
        private val BONUS_LENGTH_MATCH get() = KeyboardConstants.LENGTH_REWARD_FACTOR
        private val HIGH_CONFIDENCE_THRESHOLD get() = abs(KeyboardConstants.NEURAL_HIGH_CONFIDENCE_THRESHOLD) * 10f

        private const val PENALTY_WEAK_LENGTH_MISMATCH = 15f
        private const val PENALTY_EXCESSIVE_PATH = 45f
        private const val PENALTY_LAST_KEY_MISMATCH = 50f

        private const val BONUS_SUBSEQUENCE = 25f
        private const val BONUS_FIRST_LAST_MATCH = 15f
        private const val BONUS_ANCHOR_SUBSEQ = 20f
        private const val BONUS_KEYPATH_SUBSEQ_EXTRA = 20f
        private const val BONUS_WEAK_VOCAB_MODEL = 15f
        private const val BONUS_EXACT_ANCHOR_MODEL = 40f
        private const val BONUS_EXACT_PATH_MODEL = 35f
        private const val BONUS_VALID_MODEL_WORD = 120f
        private const val BONUS_SKELETON_MATCH = 25f
        private const val BONUS_DIRECT_SIMILARITY_SCALE = 50f
        private const val BONUS_BIGRAM_MATCH = 65f
        private const val BONUS_UNIGRAM_SCALE = 20f 
    }

    private fun MutableMap<String, Float>.mergeScore(word: String, newScore: Float) {
        val existing = this[word]
        if (existing == null || newScore > existing) {
            this[word] = newScore
        }
    }

    private fun consonantSkeleton(s: String): String {
        return s.lowercase().filter { it in "bcdfghjklmnpqrstvwxyz" }
    }

    fun normalizedSimilarity(a: String, b: String): Float {
        if (a.isEmpty() && b.isEmpty()) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val dist = trie.editDistance(a, b)
        val maxLen = maxOf(a.length, b.length).coerceAtLeast(1)
        return (1f - dist.toFloat() / maxLen.toFloat()).coerceIn(0f, 1f)
    }

    fun setLanguage(langId: String) {
        synchronized(loadLock) {
            isLoaded = false
            trie.clear()
            wordFrequencies.clear()
            

            kenlm?.close()
            kenlm = null


            val lmFile = "models/swipe/$langId/roman_lm.klm"
            kenlm = LanguageModel(context, lmFile, "${langId}_roman_lm.klm")
            kenlm?.init()
            
            loadDictionary(langId)
        }
    }

    fun clear() {
        synchronized(loadLock) {
            isLoaded = false
            trie.clear()
            wordFrequencies.clear()
            kenlm?.close()
            kenlm = null
        }
    }

    fun loadDictionary(langId: String = "hindi"): Boolean {
        if (isLoaded) return true
        synchronized(loadLock) {
            if (isLoaded) return true
            try {
                val startTime = System.currentTimeMillis()
                val basePath = "models/swipe/$langId"
                val vocabPath = "$basePath/$VOCAB_FILE"
                
                context.assets.open(vocabPath).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        reader.forEachLine { line ->
                            val parts = line.split(" ")
                            if (parts.size >= 2) {
                                val word = parts[0].lowercase()
                                val freq = parts[1].toFloatOrNull() ?: 1f
                                trie.addWord(word)
                                wordFrequencies[word] = freq
                            } else if (line.isNotBlank()) {
                                val word = line.trim().lowercase()
                                trie.addWord(word)
                                wordFrequencies[word] = 1f
                            }
                        }
                    }
                }
                isLoaded = true
                

                

                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "Dictionary loaded for '$langId': ${trie.getWordCount()} words in ${elapsed}ms")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dictionary for '$langId': ${e.message}", e)
                return false
            }
        }
    }

    fun isValidPrefix(prefix: String): Boolean = trie.hasPrefix(prefix.lowercase())
    fun isCompleteWord(word: String): Boolean = trie.containsWord(word.lowercase())
    fun getFrequency(word: String): Float = wordFrequencies[word.lowercase()] ?: 0f
    

    
    fun getAllowedNextChars(prefix: String): Set<Char> = trie.getAllowedNextChars(prefix.lowercase())
    fun getTrie(): VocabularyTrie = trie

    fun getBigramScore(prevWord: String, currentWord: String): Float {
        return kenlm?.getBigramScore(prevWord.lowercase(), currentWord.lowercase()) ?: -100f
    }

    fun getUnigramScore(word: String): Float {
        return kenlm?.getUnigramScore(word.lowercase()) ?: -100f
    }

    fun findCandidates(anchors: String, keyPath: String, prevWord: String = ""): List<Pair<String, Float>> {
        if (!isLoaded || anchors.isEmpty()) return emptyList()

        val candidates = mutableMapOf<String, Float>()
        val query = anchors.lowercase()


        val path = if (keyPath.isEmpty()) query else keyPath.lowercase()


        if (trie.containsWord(query.lowercase())) {
            candidates[query] = SCORE_EXACT_MATCH
        }



        val maxFuzzyResults = if (path.isEmpty()) 5 else 250
        val maxFuzzyDist = when {
            query.length <= 3 -> 1
            query.length <= 6 -> 2
            else -> 3
        }
        
        val fuzzy = trie.findSimilarWords(query, maxDist = maxFuzzyDist, maxResults = maxFuzzyResults)
        for ((word, dist) in fuzzy) {
            val score = if (dist == 0) SCORE_EXACT_MATCH 
                        else if (dist == 1) SCORE_EDIT_DIST_1_BASE - dist * PENALTY_PER_EDIT_DIST_1
                        else SCORE_EDIT_DIST_2_BASE - dist * PENALTY_PER_EDIT_DIST_2
            candidates.mergeScore(word, score)
        }


        if (candidates.size < maxFuzzyResults) {
            val prefixLen = (query.length / 2).coerceIn(1, 3)
            val prefix = query.substring(0, prefixLen)
            val prefixWords = trie.wordsWithPrefix(prefix.lowercase(), maxResults = 10)
            for (word in prefixWords) {
                if (candidates.containsKey(word)) continue
                val dist = trie.editDistance(query, word)
                val score = SCORE_PREFIX_MATCH_BASE - dist * PENALTY_PER_EDIT_PREFIX
                candidates.mergeScore(word, score)
            }
        }


        if (path.length >= 2) {
            val isTap = query == path
            val subseqWords = trie.findSubsequenceMatches(
                sequence = path,
                minLength = 2,
                maxLength = query.length + 4,
                maxResults = if (isTap) 10 else 350
            )
            for (word in subseqWords) {
                val dist = trie.editDistance(query, word)
                val penalty = if (isTap) 3.0f else 1.5f
                val score = SCORE_SUBSEQ_BASE - (dist * penalty).coerceAtMost(40f)
                candidates.mergeScore(word, score)
            }
        }
        



        if (path.length >= 2 && path != query.lowercase()) {
            val pathFuzzyDist = if (path.length <= 4) 1 else 2
            val pathFuzzy = trie.findSimilarWords(path, maxDist = pathFuzzyDist, maxResults = 100)
            for ((word, dist) in pathFuzzy) {
                if (!candidates.containsKey(word)) {
                    val score = SCORE_SUBSEQ_BASE - (dist * 15f)
                    candidates.mergeScore(word, score)
                }
            }
        }

        applyBonuses(candidates, query, path, prevWord)

        return candidates.entries
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .take(40)
    }

    private fun applyBonuses(
        candidates: MutableMap<String, Float>,
        anchors: String,
        keyPath: String,
        prevWord: String
    ) {
        if (candidates.isEmpty()) return
        val anchorSkeleton = consonantSkeleton(anchors)
        val prevWordLower = prevWord.lowercase()
        val pathLower = keyPath.lowercase()

        for ((word, score) in candidates.toMap()) {
            var adjustedScore = score

            if (trie.containsWord(word)) adjustedScore += BONUS_DICT_WORD

            if (keyPath.isNotEmpty() && trie.isSubsequenceOf(word, keyPath)) {
                adjustedScore += BONUS_SUBSEQUENCE
            }

            val lengthDiff = abs(word.length - anchors.length)
            when {
                lengthDiff == 0 -> adjustedScore += BONUS_LENGTH_MATCH
                lengthDiff == 1 -> adjustedScore += BONUS_LENGTH_MATCH * 0.7f
                lengthDiff == 2 -> adjustedScore += BONUS_LENGTH_MATCH * 0.4f
                lengthDiff >= 4 -> adjustedScore -= PENALTY_WEAK_LENGTH_MISMATCH
            }

            if (word.isNotEmpty() && anchors.isNotEmpty()) {
                val firstMatch = word.first() == anchors.first()
                val lastMatch = word.last() == anchors.last()
                if (firstMatch && lastMatch) adjustedScore += BONUS_FIRST_LAST_MATCH
                else if (firstMatch || lastMatch) adjustedScore += BONUS_FIRST_LAST_MATCH * 0.6f
            }

            if (trie.isSubsequenceOf(anchors, word) || trie.isSubsequenceOf(word, anchors)) {
                adjustedScore += BONUS_ANCHOR_SUBSEQ
            }

            val directSim = normalizedSimilarity(anchors, word)
            adjustedScore += directSim * BONUS_DIRECT_SIMILARITY_SCALE

            val wordSkeleton = consonantSkeleton(word)
            if (anchorSkeleton.isNotEmpty() && wordSkeleton.isNotEmpty()) {
                val skeletonSim = normalizedSimilarity(anchorSkeleton, wordSkeleton)
                adjustedScore += skeletonSim * BONUS_SKELETON_MATCH
            }

            if (keyPath.isNotEmpty()) {
                val pathSet = keyPath.toSet()
                val wordPathChars = word.filter { it in pathSet }
                val anchorPathChars = anchors.filter { it in pathSet }
                val pathSim = normalizedSimilarity(anchorPathChars, wordPathChars)
                adjustedScore += pathSim * 6f
            }


            val uniScore = kenlm?.getUnigramScore(word) ?: -100f
            if (uniScore > -50f) {

                adjustedScore += 30f + (uniScore + 8f) * 5f
            }


            if (pathLower.length > 5) {
                val wordChars = word.lowercase().toSet()
                var coveredCount = 0
                for (char in pathLower) {
                    if (wordChars.contains(char)) coveredCount++
                }
                val coverageRatio = coveredCount.toFloat() / pathLower.length.toFloat()
                

                if (coverageRatio > 0.7f) adjustedScore += 40f
                


                if (word.length < 5 && coverageRatio < 0.3f && pathLower.length > 15) {
                    adjustedScore -= 180f
                }
            }


            val lenDiff = kotlin.math.abs(word.length - pathLower.length)
            if (lenDiff > 10 && pathLower.length > 15) {
                adjustedScore -= lenDiff * 15f
            }


            if (word.isNotEmpty() && anchors.isNotEmpty()) {
                if (word[0].lowercaseChar() == anchors[0].lowercaseChar()) {
                    adjustedScore += 80f 
                } else {
                    adjustedScore -= 120f 
                }
            }



            if (anchors.length >= 2 && word.length > anchors.length + 2 && word.startsWith(anchors.substring(0, 2))) {
                adjustedScore += 40f
            }











            val unigramFreq = wordFrequencies[word] ?: 1f
            val freqBoost = 0f
            adjustedScore += freqBoost

            candidates[word] = adjustedScore
        }
    }

    fun getHighConfidenceMatch(candidates: List<Pair<String, Float>>): String? {
        if (candidates.isEmpty()) return null
        val top = candidates.first()
        if (top.second < HIGH_CONFIDENCE_THRESHOLD) return null
        if (candidates.size >= 2) {
            val gap = top.second - candidates[1].second
            if (gap < 20f) return null
        }
        return top.first
    }

    fun pickBestResult(
        anchors: String,
        keyPath: String,
        vocabCandidates: List<Pair<String, Float>>,
        modelCandidates: List<Pair<String, Float>>
    ): String {
        val scored = mutableMapOf<String, Float>()
        
        val allWords = mutableSetOf<String>()
        vocabCandidates.forEach { allWords.add(it.first.lowercase()) }
        modelCandidates.forEach { allWords.add(it.first.lowercase()) }
        
        val vocabMap = vocabCandidates.associate { it.first.lowercase() to it.second }
        val modelMap = modelCandidates.associate { it.first.lowercase() to it.second }
        
        val anchorLower = anchors.lowercase()
        val pathLower = keyPath.lowercase()
        
        for (word in allWords) {
            val isValidWord = isCompleteWord(word)
            


            val neuralLogit = modelMap[word] ?: -20f
            val neuralScore = (neuralLogit + 15f).coerceIn(0f, 15f) * (100f / 15f)
            


            val geomScore = if (vocabMap.containsKey(word)) {
                (vocabMap[word]!! + 250f).coerceIn(0f, 350f) * (100f / 350f)
            } else {
                val simToPath = if (pathLower.isNotEmpty()) normalizedSimilarity(pathLower, word) else 1.0f
                simToPath * 75f
            }
            


            val uniScore = getUnigramScore(word)
            val lmScore = (uniScore + 8f).coerceIn(0f, 8f) * (100f / 8f)
            


            var totalScore = (neuralScore * 0.55f) + 
                             (geomScore * 0.35f) + 
                             (lmScore * 0.10f)
                             

            if (pathLower.isNotEmpty()) {
                val lenDiff = kotlin.math.abs(word.length - pathLower.length)
                if (lenDiff >= 4) {
                    totalScore -= 25f
                }
            }
            
            if (word.isNotEmpty() && pathLower.isNotEmpty() && word.last() != pathLower.last()) {
                totalScore -= 10f
            }
            
            if (!isValidWord && neuralScore < 75f) {
                totalScore -= 40f
            }
            
            if (anchorLower.isNotEmpty() && word.isNotEmpty() && word.first() == anchorLower.first()) {
                totalScore += 5f
            }
            
            scored[word] = totalScore
            Log.d("DictionaryManager", "WeightedScore: '$word' | N:${String.format("%.1f", neuralScore)} G:${String.format("%.1f", geomScore)} LM:${String.format("%.1f", lmScore)} | Total: ${String.format("%.1f", totalScore)}")
        }

        val bestWord = scored.maxByOrNull { it.value }?.key ?: anchors
        Log.i("DictionaryManager", "🏆 pickBestResult Winner: '$bestWord'")
        return bestWord
    }

    fun finalizeSwipeResult(
        keyPath: String,
        modelCandidates: List<Pair<String, Float>>,
        isHindiMode: Boolean,
        prevWord: String = ""
    ): String {
        val anchors = modelCandidates.first().first
        val vocabCandidates = findCandidates(anchors, keyPath, prevWord)
        
        val bestWord = pickBestResult(
            anchors = anchors,
            keyPath = keyPath,
            vocabCandidates = vocabCandidates,
            modelCandidates = modelCandidates
        )
        
        Log.d(TAG, "finalizeSwipeResult: Hybrid Output: '$bestWord' (Neural Top: '$anchors', Path: '$keyPath')")
        return bestWord
    }

    fun getStats(): Map<String, Any> = mapOf(
        "isLoaded" to isLoaded,
        "wordCount" to trie.getWordCount(),
        "trieStats" to trie.getStats()
    )

    fun isReady(): Boolean = isLoaded
}