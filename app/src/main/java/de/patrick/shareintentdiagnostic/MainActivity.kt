package de.patrick.shareintentdiagnostic

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var output: TextView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val copy = Button(this).apply {
            text = "Diagnose kopieren"
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Share Intent Diagnostic", output.text))
            }
        }
        output = TextView(this).apply { textSize = 14f; setTextIsSelectable(true) }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            addView(copy, LinearLayout.LayoutParams(-1, -2))
            addView(output, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(ScrollView(this).apply { addView(layout) })
        showIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); showIntent(intent)
    }

    private fun showIntent(intent: Intent) {
        val sb = StringBuilder()
        sb.appendLine("ANDROID SHARE INTENT DIAGNOSTIC")
        sb.appendLine("================================")
        sb.appendLine("action        = ${intent.action}")
        sb.appendLine("type          = ${intent.type}")
        sb.appendLine("flags         = 0x${intent.flags.toString(16)}")
        sb.appendLine("categories    = ${intent.categories?.joinToString() ?: "null"}")
        sb.appendLine()

        val single = try { @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } catch (_: Exception) { null }
        val multiple = try { @Suppress("DEPRECATION") intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) } catch (_: Exception) { null }

        sb.appendLine("EXTRA_STREAM")
        sb.appendLine("------------")
        sb.appendLine("single URI    = ${single ?: "null"}")
        sb.appendLine("multiple URIs = ${multiple?.size ?: "null"}")
        multiple?.forEachIndexed { i, uri -> sb.appendLine("  [$i] $uri") }
        sb.appendLine()

        sb.appendLine("CLIPDATA")
        sb.appendLine("--------")
        val clip = intent.clipData
        if (clip == null) sb.appendLine("null") else {
            sb.appendLine("description   = ${clip.description}")
            sb.appendLine("itemCount     = ${clip.itemCount}")
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                sb.appendLine("  item[$i].uri  = ${item.uri}")
                sb.appendLine("  item[$i].text = ${item.text}")
                sb.appendLine("  item[$i].html = ${item.htmlText}")
            }
        }
        sb.appendLine()

        sb.appendLine("OTHER EXTRAS")
        sb.appendLine("------------")
        intent.extras?.keySet()?.forEach { key ->
            if (key != Intent.EXTRA_STREAM) sb.appendLine("$key = ${safe(intent.extras?.get(key))}")
        }
        sb.appendLine()

        sb.appendLine("URI DETAILS")
        sb.appendLine("-----------")
        val uris = linkedSetOf<Uri>()
        single?.let(uris::add); multiple?.forEach(uris::add)
        if (clip != null) for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(uris::add)
        if (uris.isEmpty()) sb.appendLine("no URI found") else uris.forEachIndexed { i, uri ->
            sb.appendLine("[$i] $uri")
            sb.appendLine("    scheme      = ${uri.scheme}")
            sb.appendLine("    authority   = ${uri.authority}")
            sb.appendLine("    MIME        = ${try { contentResolver.getType(uri) } catch (_: Exception) { null }}")
            sb.appendLine("    displayName = ${displayName(uri)}")
            sb.appendLine("    readable    = ${readable(uri)}")
        }
        sb.appendLine()
        sb.appendLine("KEY TEST: compare EXTRA_STREAM with CLIPDATA.")
        output.text = sb.toString()
    }

    private fun displayName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) { null }

    private fun readable(uri: Uri): Boolean = try {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (_: Exception) { false }

    private fun safe(v: Any?): String = when (v) {
        null -> "null"
        is Bundle -> "<Bundle ${v.keySet().size} keys>"
        is ByteArray -> "<ByteArray ${v.size} bytes>"
        else -> v.toString()
    }
}
