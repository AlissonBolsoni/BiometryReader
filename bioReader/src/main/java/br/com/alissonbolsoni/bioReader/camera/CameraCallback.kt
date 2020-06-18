package br.com.alissonbolsoni.bioReader.camera

import org.opencv.android.JavaCameraView

interface CameraCallback {

    fun faceDetect(detected: Boolean)
    fun getSurface(): BioReaderJavaCameraView
    fun getPicturePath(): String
}