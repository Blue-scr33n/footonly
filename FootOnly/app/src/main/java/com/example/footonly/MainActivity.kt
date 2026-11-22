package com.example.footonly

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.IOException

/** A consent-based, on-device image censoring MVP. */
class MainActivity : ComponentActivity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var censorButton: Button
    private var original: Bitmap? = null
    private var censored: Bitmap? = null

    private val openImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) loadImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderScreen()
    }

    private fun renderScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(Color.rgb(13, 17, 28))
        }
        val heading = TextView(this).apply {
            text = "FootOnly"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
        val description = TextView(this).apply {
            text = "Private, on-device image censoring"
            textSize = 15f
            setTextColor(Color.rgb(174, 184, 205))
            setPadding(0, dp(4), 0, dp(18))
        }
        preview = ImageView(this).apply {
            contentDescription = "Selected image preview"
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(25, 31, 46))
        }
        status = TextView(this).apply {
            text = "Choose an image to get started. Images stay on your device."
            textSize = 14f
            setTextColor(Color.rgb(205, 213, 227))
            setPadding(0, dp(14), 0, dp(14))
        }
        val chooseButton = button("Choose image") { openImage.launch("image/*") }
        censorButton = button("Pixelate image") { pixelateSelectedImage() }.apply { isEnabled = false }
        val saveButton = button("Save censored copy") { saveCensoredCopy() }.apply { isEnabled = false; tag = "save" }

        root.addView(heading)
        root.addView(description)
        root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(status)
        root.addView(chooseButton, buttonLayout())
        root.addView(censorButton, buttonLayout())
        root.addView(saveButton, buttonLayout())
        setContentView(root)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun buttonLayout() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(6) }

    private fun loadImage(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                }
            } else {
                @Suppress("DEPRECATION")
                contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }
            if (bitmap == null) throw IOException("Could not decode image")
            original = bitmap
            censored = null
            preview.setImageBitmap(bitmap)
            status.text = "Image loaded locally. Pixelation censors the whole image."
            censorButton.isEnabled = true
            findSaveButton()?.isEnabled = false
        } catch (e: Exception) {
            showMessage("Couldn't open that image")
        }
    }

    private fun pixelateSelectedImage() {
        val source = original ?: return
        try {
            // Downsample then scale back with filtering disabled to create a pixelated copy.
            val smallWidth = (source.width / 32).coerceAtLeast(1)
            val smallHeight = (source.height / 32).coerceAtLeast(1)
            val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
            val result = Bitmap.createScaledBitmap(small, source.width, source.height, false)
            if (small !== source && small !== result) small.recycle()
            censored = result
            preview.setImageBitmap(result)
            status.text = "Pixelated preview ready. Save creates a separate copy; the original is unchanged."
            findSaveButton()?.isEnabled = true
        } catch (e: OutOfMemoryError) {
            showMessage("This image is too large to process")
        }
    }

    private fun saveCensoredCopy() {
        val bitmap = censored ?: return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "FootOnly-${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FootOnly")
        }
        try {
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Couldn't create output image")
            val saved = contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) } ?: false
            if (!saved) {
                contentResolver.delete(uri, null, null)
                throw IOException("Couldn't write output image")
            }
            showMessage("Censored copy saved to Pictures/FootOnly")
        } catch (e: Exception) {
            showMessage("Couldn't save the image")
        }
    }

    private fun findSaveButton(): Button? {
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        fun search(group: ViewGroup): Button? {
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is Button && child.text == "Save censored copy") return child
                if (child is ViewGroup) search(child)?.let { return it }
            }
            return null
        }
        return search(root)
    }

    private fun showMessage(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        original?.recycle()
        censored?.recycle()
    }
}
