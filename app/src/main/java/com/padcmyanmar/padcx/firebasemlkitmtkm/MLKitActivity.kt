package com.padcmyanmar.padcx.firebasemlkitmtkm

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.padcmyanmar.padcx.firebasemlkitmtkm.utils.loadBitMapFromUri
import com.padcmyanmar.padcx.firebasemlkitmtkm.utils.scaleToRatio
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_m_l_kit.*
import java.lang.StringBuilder

class MLKitActivity : BaseActivity() {

    var mChosenImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m_l_kit)

        setUpActionListener()
    }

    private fun setUpActionListener() {
        btnTakePhoto.setOnClickListener {
            selectImageFromGallery()
        }
        btnFindFace.setOnClickListener {
            detectFaceAndDrawRectangle()
        }

        btnFindText.setOnClickListener {
            detectTextAndUpdateUI()
        }
    }

    private fun detectFaceAndDrawRectangle() {
        mChosenImageBitmap?.let {
            val inputImage = InputImage.fromBitmap(it, 0)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()
            val detector =
                FaceDetection.getClient(options)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    drawRectangleOnFace(it, faces)
                    ivImage.setImageBitmap(mChosenImageBitmap)
                }
                .addOnFailureListener { exception ->
                    showSnackbar(
                        exception.localizedMessage
                            ?: getString(R.string.cannot_detect_face)
                    )
                }
        }
    }

    private fun detectTextAndUpdateUI() {
        mChosenImageBitmap?.let {
            val inputImage = InputImage.fromBitmap(it, 0)
            val recognizer = TextRecognition.getClient()

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->

                    // Update text in the screen
                    val detectedTextsString = StringBuilder("")
                    visionText.textBlocks.forEach { block ->
                        detectedTextsString.append("${block.text}\n")
                    }

                    tvDetectedTexts.text = ""
                    tvDetectedTexts.text = detectedTextsString.toString()

                    // Draw bounding boxes
                    val paint = Paint()
                    paint.color = Color.GREEN
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2.0f

                    visionText.textBlocks.forEach { block ->
                        val imageCanvas = Canvas(it)
                        block.boundingBox?.let { boundingBox -> imageCanvas.drawRect(boundingBox, paint) }
                    }

                }
                .addOnFailureListener { e ->
                    showSnackbar(
                        e.localizedMessage ?: getString(R.string.cannot_detect_text)
                    )
                }
        }
    }

    private fun drawRectangleOnFace(
        it: Bitmap,
        faces: MutableList<Face>
    ) {
        val imageCanvas = Canvas(it)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 9.0f

        faces.firstOrNull()?.boundingBox?.let {
                boundingBox -> imageCanvas.drawRect(boundingBox, paint)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == INTENT_REQUEST_CODE_SELECT_IMAGE_FROM_GALLERY) {
            val imageUri = data?.data

            imageUri?.let { image ->

                Observable.just(image)
                    .map { it.loadBitMapFromUri(applicationContext) }
                    .map { it.scaleToRatio(0.35) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        mChosenImageBitmap = it
                        ivImage.setImageBitmap(mChosenImageBitmap)
                    }

            }
        }
    }

}