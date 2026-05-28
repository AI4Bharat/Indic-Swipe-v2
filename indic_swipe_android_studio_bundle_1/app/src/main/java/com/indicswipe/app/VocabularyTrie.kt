package com.indicswipe.app

import android.util.Log


class VocabularyTrie {

    companion object {
        private const val TAG = "VocabularyTrie"
        private const val MAX_FUZZY_RESULTS = 30
        private const val MAX_WORD_LENGTH = 25
    }





    private class TrieNode {


        val children = HashMap<Char, TrieNode>(4)
        var isWord = false



    }





    private val root = TrieNode()
    private var wordCount = 0


    private val wordsByLength = HashMap<Int, MutableList<String>>()


    private val wordCache = HashSet<String>(10000)





    
    fun loadFromLines(lines: Sequence<String>) {
        val startTime = System.currentTimeMillis()
        var totalLines = 0
        var validWords = 0

        for (line in lines) {
            totalLines++
            val word = line.trim().lowercase()


            if (word.isNotEmpty() &&
                word.length <= MAX_WORD_LENGTH &&
                word.all { it in 'a'..'z' }) {

                if (insert(word)) {
                    validWords++
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Loaded $wordCount unique words from $totalLines lines ($validWords valid) in ${elapsed}ms")
    }

    
    private fun insert(word: String): Boolean {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }

        if (!node.isWord) {
            node.isWord = true
            wordCount++


            wordsByLength.getOrPut(word.length) { mutableListOf() }.add(word)


            wordCache.add(word)

            return true
        }
        return false
    }

    
    fun addWord(word: String): Boolean {
        val w = word.trim().lowercase()
        if (w.isEmpty() || w.length > MAX_WORD_LENGTH || !w.all { it in 'a'..'z' }) {
            return false
        }
        return insert(w)
    }





    
    fun hasPrefix(prefix: String): Boolean {
        var node = root
        for (char in prefix.lowercase()) {
            node = node.children[char] ?: return false
        }
        return true
    }

    
    fun containsWord(word: String): Boolean {
        val w = word.lowercase()


        if (wordCache.contains(w)) {
            return true
        }


        var node = root
        for (char in w) {
            node = node.children[char] ?: return false
        }
        return node.isWord
    }

    
    fun getAllowedNextChars(prefix: String): Set<Char> {
        var node = root
        for (char in prefix.lowercase()) {
            node = node.children[char] ?: return emptySet()
        }
        return node.children.keys.toSet()
    }

    
    fun getWordCount(): Int = wordCount

    
    fun isEmpty(): Boolean = wordCount == 0





    
    fun wordsWithPrefix(prefix: String, maxResults: Int = 20): List<String> {
        val results = mutableListOf<String>()
        val p = prefix.lowercase()


        var node = root
        for (char in p) {
            node = node.children[char] ?: return results
        }


        collectWords(node, StringBuilder(p), results, maxResults)
        return results
    }

    
    private fun collectWords(
        node: TrieNode,
        current: StringBuilder,
        results: MutableList<String>,
        maxResults: Int
    ) {
        if (results.size >= maxResults) return

        if (node.isWord) {
            results.add(current.toString())
            if (results.size >= maxResults) return
        }


        for ((char, child) in node.children.entries.sortedBy { it.key }) {
            current.append(char)
            collectWords(child, current, results, maxResults)
            current.deleteCharAt(current.lastIndex)

            if (results.size >= maxResults) return
        }
    }





    
    fun editDistance(a: String, b: String): Int {
        val s = a.lowercase()
        val t = b.lowercase()


        if (s.length > t.length) {
            return editDistance(b, a)
        }

        val m = s.length
        val n = t.length


        if (m == 0) return n
        if (n == 0) return m


        var prevRow = IntArray(m + 1) { it }
        var currRow = IntArray(m + 1)

        for (j in 1..n) {
            currRow[0] = j

            for (i in 1..m) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                currRow[i] = minOf(
                    currRow[i - 1] + 1,
                    prevRow[i] + 1,
                    prevRow[i - 1] + cost
                )
            }


            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }

        return prevRow[m]
    }





    
    fun findSimilarWords(
        query: String,
        maxDist: Int = 1,
        maxResults: Int = MAX_FUZZY_RESULTS
    ): List<Pair<String, Int>> {
        val q = query.lowercase()

        if (q.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<Pair<String, Int>>()



        val initialRow = IntArray(q.length + 1) { it }


        findSimilarDFS(
            node = root,
            query = q,
            previousRow = initialRow,
            currentWord = StringBuilder(),
            results = results,
            maxDist = maxDist,
            maxResults = maxResults
        )


        return results
            .sortedWith(compareBy({ it.second }, { it.first }))
            .take(maxResults)
    }

    
    private fun findSimilarDFS(
        node: TrieNode,
        query: String,
        previousRow: IntArray,
        currentWord: StringBuilder,
        results: MutableList<Pair<String, Int>>,
        maxDist: Int,
        maxResults: Int
    ) {

        if (results.size >= maxResults) return

        val columns = query.length + 1


        if (node.isWord && currentWord.isNotEmpty()) {

            val distance = previousRow[columns - 1]
            if (distance <= maxDist) {
                results.add(currentWord.toString() to distance)


                if (results.size >= maxResults) return
            }
        }


        for ((char, child) in node.children) {

            val currentRow = IntArray(columns)



            currentRow[0] = previousRow[0] + 1


            var rowMin = currentRow[0]

            for (col in 1 until columns) {
                val insertCost = currentRow[col - 1] + 1
                val deleteCost = previousRow[col] + 1
                val replaceCost = previousRow[col - 1] +
                        if (query[col - 1] == char) 0 else 1

                currentRow[col] = minOf(insertCost, deleteCost, replaceCost)

                if (currentRow[col] < rowMin) {
                    rowMin = currentRow[col]
                }
            }


            if (rowMin <= maxDist) {
                currentWord.append(char)
                findSimilarDFS(child, query, currentRow, currentWord, results, maxDist, maxResults)
                currentWord.deleteCharAt(currentWord.lastIndex)
            }


            if (results.size >= maxResults) return
        }
    }





    
    fun isSubsequenceOf(word: String, sequence: String): Boolean {
        val w = word.lowercase()
        val s = sequence.lowercase()

        if (w.isEmpty()) return true
        if (s.isEmpty()) return false

        var wordIdx = 0
        var seqIdx = 0

        while (wordIdx < w.length && seqIdx < s.length) {
            if (w[wordIdx] == s[seqIdx]) {
                val currentChar = w[wordIdx]
                wordIdx++

                if (wordIdx < w.length && w[wordIdx] == currentChar) {
                    continue
                }
                seqIdx++
            } else {
                seqIdx++
            }
        }
        return wordIdx == w.length
    }

    
    fun findSubsequenceMatches(
        sequence: String,
        minLength: Int = 2,
        maxLength: Int = 15,
        maxResults: Int = MAX_FUZZY_RESULTS
    ): List<String> {
        val s = sequence.lowercase()
        val results = mutableListOf<String>()

        if (s.isEmpty()) return results


        findSubsequenceDFS(
            node = root,
            sequence = s,
            seqStartIdx = 0,
            currentWord = StringBuilder(),
            results = results,
            minLength = minLength,
            maxLength = maxLength,
            maxResults = maxResults
        )

        return results
    }

    
    private fun findSubsequenceDFS(
        node: TrieNode,
        sequence: String,
        seqStartIdx: Int,
        currentWord: StringBuilder,
        results: MutableList<String>,
        minLength: Int,
        maxLength: Int,
        maxResults: Int
    ) {
        if (results.size >= maxResults) return
        if (currentWord.length > maxLength) return


        if (node.isWord && currentWord.length >= minLength) {
            results.add(currentWord.toString())
            if (results.size >= maxResults) return
        }


        if (seqStartIdx >= sequence.length) return


        for ((char, child) in node.children) {

            val matchIdx = sequence.indexOf(char, seqStartIdx)
            if (matchIdx != -1) {
                currentWord.append(char)
                findSubsequenceDFS(
                    child, sequence, matchIdx + 1,
                    currentWord, results, minLength, maxLength, maxResults
                )
                currentWord.deleteCharAt(currentWord.lastIndex)
                if (results.size >= maxResults) return
            }
            


            if (seqStartIdx > 0 && sequence[seqStartIdx - 1] == char) {
                currentWord.append(char)
                findSubsequenceDFS(
                    child, sequence, seqStartIdx,
                    currentWord, results, minLength, maxLength, maxResults
                )
                currentWord.deleteCharAt(currentWord.lastIndex)
                if (results.size >= maxResults) return
            }
        }
    }





    
    fun getSimilarLengthWords(
        query: String,
        maxResults: Int = 10
    ): List<Pair<String, Int>> {
        val q = query.lowercase()
        val qLen = q.length
        val results = mutableListOf<Pair<String, Int>>()


        for (len in maxOf(1, qLen - 1)..minOf(MAX_WORD_LENGTH, qLen + 1)) {
            val words = wordsByLength[len] ?: continue

            for (word in words) {
                val dist = editDistance(q, word)
                if (dist <= 2) {
                    results.add(word to dist)
                }


                if (results.size >= maxResults * 3) break
            }
        }

        return results
            .sortedBy { it.second }
            .take(maxResults)
    }

    
    fun exactMatch(query: String): List<String> {
        val q = query.lowercase()
        return if (containsWord(q)) listOf(q) else emptyList()
    }

    
    fun getStats(): Map<String, Any> {
        var totalNodes = 0
        var leafNodes = 0
        var maxDepth = 0

        fun countNodes(node: TrieNode, depth: Int) {
            totalNodes++
            if (depth > maxDepth) maxDepth = depth
            if (node.children.isEmpty()) leafNodes++
            for (child in node.children.values) {
                countNodes(child, depth + 1)
            }
        }

        countNodes(root, 0)

        return mapOf(
            "wordCount" to wordCount,
            "totalNodes" to totalNodes,
            "leafNodes" to leafNodes,
            "maxDepth" to maxDepth,
            "lengthBuckets" to wordsByLength.size
        )
    }

    
    fun clear() {
        root.children.clear()
        wordCount = 0
        wordsByLength.clear()
        wordCache.clear()
    }
}