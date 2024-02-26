package nl.joris2k.spinningled.device

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joris2k.spinningled.CoroutineSocket
import timber.log.Timber
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.AsynchronousSocketChannel
import kotlin.math.min

class SpinningLedDevice(
    private val hostname: String,
    private val port: Int = 26425
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private var socket: CoroutineSocket? = null

    private val _connected = MutableStateFlow(false)
    val connected : StateFlow<Boolean> = _connected.asStateFlow()

    private val _screenSize = MutableStateFlow(IntSize(0,0))
    val screenSize : StateFlow<IntSize> = _screenSize.asStateFlow()

    private val _currentProgram = MutableStateFlow(AxisProgram.Undefined)
    val currentProgram : StateFlow<AxisProgram> = _currentProgram.asStateFlow()

    suspend fun connect() = withContext(coroutineScope.coroutineContext) {
        mutex.withLock {
            Timber.d("Connect() called")
            socket?.let {
                if (it.isConnected) {
                    Timber.d("Closing socket as it's connected")
                    it.close()
                }
            }
            Timber.d("Connecting to ${hostname}:${port}")
            socket = CoroutineSocket(AsynchronousSocketChannel.open()).also { s ->
                // TODO: move the connect function out of here...
                s.connect(InetSocketAddress(hostname, port))
                Timber.d("Connected...")

                launch {
                    _connected.value = true
                    val buffer = ByteBuffer.allocate(8196).order(ByteOrder.LITTLE_ENDIAN)
                    try {
                        while (true) {
                            val r = s.read(buffer)
                            buffer.flip()
                            // Dit we receive the header?
                            while (buffer.remaining() >= 4) {
                                // Get header
                                val header = buffer.getAxisPacketHeader()
                                while (buffer.remaining() < header.payloadLength.toInt()) {
                                    buffer.compact()
                                    val r = s.read(buffer)
                                    buffer.flip()
                                }
                                val position = buffer.position()
                                processPacket(header, buffer)
                                buffer.position(position + header.payloadLength.toInt())
                            }
                            buffer.compact()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error in socket")
                    } finally {
                        Timber.d("Disconnect...")
                        s.close()
                    }
                    _connected.value = false
                }
                Timber.d("Finished launching receiver thread")
            }
            // Only here is the socket is assigned
            sendDummy(DEFAULT_INFO_PAYLOAD_SIZE)
        }
    }

    suspend fun disconnect() = withContext(coroutineScope.coroutineContext) {
        mutex.withLock {
            socket?.close()
        }
    }

    private fun processPacket(header: AxisPacketHeader, buffer: ByteBuffer) {
        when (header.type.toInt()) {
            0 -> Timber.d("Dummy axis packet received, ignoring")
            1 -> processInfoPacket(header, buffer)
            else -> Timber.w("Invalid packet type ${header.type} with ${header.payloadLength} bytes")
        }
    }

    private fun processInfoPacket(header: AxisPacketHeader, buffer: ByteBuffer) {
        if (header.payloadLength < 6u) {
            Timber.e("Invalid into axis packet with too small payload of ${header.payloadLength} bytes")
        } else {
            val width = buffer.getShort()
            val height = buffer.getShort()
            _screenSize.value = IntSize(width.toInt(), height.toInt())
            val currentProgram = buffer.getShort()
            _currentProgram.value = currentProgram.toUShort().toAxisProgram()
            Timber.i("Info packet received")
        }
    }

    suspend fun sendDummy(length: Int) {
        Timber.d("sendDummy(${length}) ")
        assert(length in 0..MAX_PAYLOAD_LENGTH)

        val buffer = ByteBuffer.allocate(length + 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(0u.toShort()) // type = DUMMY
        buffer.putShort(length.toUShort().toShort()) // payloadLength = length
        buffer.put(ByteArray(length) { 255u.toByte() })
        buffer.flip()
        socket?.write(buffer)
    }

    suspend fun setProgram(program: Int) = withContext(coroutineScope.coroutineContext) {
        Timber.d("setProgram(${program})")
        val buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(0x801u.toShort()) // type = SET_PROGRAM
        buffer.putShort(2)// payloadLength = 2
        buffer.putShort(program.toUShort().toShort())
        buffer.flip()
        socket?.write(buffer)

        // Get actual values back...
        sendDummy(DEFAULT_INFO_PAYLOAD_SIZE) // Get Info Packet...
    }

    suspend fun sendRgb565Column(column: Int, pixels: ShortArray) = withContext(coroutineScope.coroutineContext) {
        Timber.d("sendRgb565Column(${column}, ${pixels.size}px)")
        require(pixels.size <= MAX_PAYLOAD_LENGTH / 2)

        val buffer = ByteBuffer.allocate(4 + pixels.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        val sb = buffer.asShortBuffer()
        sb.put((0x4000 + column).toShort()) // type = AXIS_PACKET_TYPE_PIXEL_DATA_START
        sb.put((pixels.size * 2).toShort())
        sb.put(pixels)
        buffer.flip()
        socket?.write(buffer)
    }

    suspend fun sendRgb565Bitmap(bitmap: Bitmap) = withContext(coroutineScope.coroutineContext) {
        val size = _screenSize.value
        if (size.width == 0 || size.height == 0) {
            return@withContext
        }

        // Assume same orientation in this algorithm

        // Calculate srcDestRatio such that:
        //   src = dst * ratio
        //   hence: ratio = src / dst
        val ratio = min(bitmap.width.toFloat() / size.width.toFloat(),
            bitmap.height.toFloat() / size.height.toFloat())
        val top = (bitmap.height.toFloat() - ratio * size.height.toFloat()) / 2
        val left = (bitmap.width.toFloat() - ratio * size.width.toFloat()) / 2

        // Can optimize with larger multi-column transfers...
        //val columnsAtOnce = MAX_PAYLOAD_LENGTH / size.height / 2
        val pixels = ShortArray(size.height)
        for(column in 0..<size.width) {
            val x = (left + column * ratio).toInt()
            for(row in 0..<size.height) {
                val p = bitmap.getPixel(x, (top + row * ratio).toInt())
                pixels[row] = (((Color.red(p) / 8) shl 11) or
                        ((Color.green(p) / 4) shl 5) or
                        ((Color.blue(p) / 8))).toUShort().toShort()
            }
            sendRgb565Column(column, pixels)
        }
    }

    companion object {
        private const val MAX_PACKET_LENGTH = 1368
        private const val MAX_PAYLOAD_LENGTH = MAX_PACKET_LENGTH - 4
        private const val DEFAULT_INFO_PAYLOAD_SIZE = 16
    }
}
