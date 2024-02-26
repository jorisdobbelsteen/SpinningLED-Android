package nl.joris2k.spinningled.screen

import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import nl.joris2k.spinningled.R
import timber.log.Timber
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onBitmapCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val previewView: PreviewView = remember { PreviewView(context) }
    val cameraController = remember { LifecycleCameraController(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    cameraController.bindToLifecycle(lifecycleOwner)
    cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
    cameraController.imageCaptureFlashMode = ImageCapture.FLASH_MODE_OFF
    cameraController.imageCaptureTargetSize = CameraController.OutputSize(Size(400, 114))
    cameraController.previewTargetSize = CameraController.OutputSize(Size(400, 114))
    previewView.controller = cameraController

    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        IconButton(modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
            onClick = {
                cameraController.takePicture(executor, object: ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        Timber.i("onCaptureSuccess: $image")
                        val bitmap = image.toBitmap()
                        onBitmapCaptured(bitmap)
                    }
                })
            }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_camera_24),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}