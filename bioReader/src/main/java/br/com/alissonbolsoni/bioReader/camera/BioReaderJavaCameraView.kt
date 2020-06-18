package br.com.alissonbolsoni.bioReader.camera

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import org.opencv.android.JavaCameraView

class BioReaderJavaCameraView(context: Context, attrs: AttributeSet) : JavaCameraView(context, attrs) {

    fun takePicture(callback: Camera.PictureCallback) {
        mCamera.takePicture(null, null, callback)
    }

    fun startPreview() = mCamera.startPreview()

}