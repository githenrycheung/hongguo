package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log

class ClipboardObserver(
    private val context: Context,
    private val onNewClipDetected: (String) -> Unit
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private var lastObservedText: String = ""

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard()
    }

    fun start() {
        try {
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
            checkClipboard()
        } catch (e: Exception) {
            Log.w("ClipboardObserver", "Failed to register clip listener", e)
        }
    }

    fun stop() {
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {
            Log.w("ClipboardObserver", "Failed to unregister clip listener", e)
        }
    }

    fun checkClipboard() {
        try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim() ?: ""
                if (text.isNotBlank() && text != lastObservedText) {
                    lastObservedText = text
                    onNewClipDetected(text)
                }
            }
        } catch (e: Exception) {
            Log.w("ClipboardObserver", "Error reading clipboard", e)
        }
    }

    fun copyToClipboard(label: String, text: String) {
        try {
            val clip = ClipData.newPlainText(label, text)
            clipboardManager?.setPrimaryClip(clip)
            lastObservedText = text
        } catch (e: Exception) {
            Log.w("ClipboardObserver", "Error copying to clipboard", e)
        }
    }

    fun getDirectCurrentText(): String {
        return try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim() ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
