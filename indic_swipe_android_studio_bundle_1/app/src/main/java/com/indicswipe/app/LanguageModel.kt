package com.indicswipe.app

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class LanguageModel(private val context: Context, private val assetPath: String, private val modelName: String) {
    private var modelPtr: Long = 0
    private var currentStatePtr: Long = 0
    private var nextStatePtr: Long = 0

    companion object {
        private const val TAG = "LanguageModel"
        init {
            try {
                System.loadLibrary("kenlm-jni")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load kenlm-jni: ${e.message}")
            }
        }
    }

    fun init() {
        try {
            val modelFile = File(context.cacheDir, modelName)
            if (!modelFile.exists()) {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            modelPtr = nativeLoadModel(modelFile.absolutePath)
            if (modelPtr != 0L) {
                currentStatePtr = nativeAllocState(modelPtr)
                nextStatePtr = nativeAllocState(modelPtr)
                nativeBeginSentence(modelPtr, currentStatePtr)
                Log.i(TAG, "KenLM Model loaded: $modelName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LanguageModel: ${e.message}")
        }
    }

    fun reset() {
        if (modelPtr != 0L && currentStatePtr != 0L) {
            nativeBeginSentence(modelPtr, currentStatePtr)
        }
    }

    fun getBigramScore(prevWord: String, currentWord: String): Float {
        if (modelPtr == 0L || currentStatePtr == 0L || nextStatePtr == 0L) return -100f
        

        nativeNullContext(modelPtr, currentStatePtr)
        val s1 = nativeScore(modelPtr, currentStatePtr, nextStatePtr, prevWord)
        

        nativeCopyState(modelPtr, nextStatePtr, currentStatePtr)
        val s2 = nativeScore(modelPtr, currentStatePtr, nextStatePtr, currentWord)
        
        Log.d(TAG, "getBigramScore: '$prevWord'($s1) -> '$currentWord'($s2)")
        return s2
    }

    fun getUnigramScore(word: String): Float {
        if (modelPtr == 0L || currentStatePtr == 0L || nextStatePtr == 0L) return -100f
        nativeNullContext(modelPtr, currentStatePtr)
        return nativeScore(modelPtr, currentStatePtr, nextStatePtr, word)
    }

    fun close() {
        if (modelPtr != 0L) {
            nativeFreeState(currentStatePtr)
            nativeFreeState(nextStatePtr)
            nativeCloseModel(modelPtr)
            modelPtr = 0
        }
    }

    private external fun nativeLoadModel(path: String): Long
    private external fun nativeCloseModel(modelPtr: Long)
    private external fun nativeAllocState(modelPtr: Long): Long
    private external fun nativeFreeState(statePtr: Long)
    private external fun nativeCopyState(modelPtr: Long, srcPtr: Long, destPtr: Long)
    private external fun nativeBeginSentence(modelPtr: Long, statePtr: Long)
    private external fun nativeNullContext(modelPtr: Long, statePtr: Long)
    private external fun nativeScore(modelPtr: Long, inStatePtr: Long, outStatePtr: Long, token: String): Float
}