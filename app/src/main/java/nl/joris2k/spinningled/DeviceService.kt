package nl.joris2k.spinningled

import android.app.Service
import android.content.Intent
import android.os.IBinder

// TODO: Determine if this is really needed ???

class DeviceService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}