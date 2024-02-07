package nl.joris2k.spinningled.device

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

                async {
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
        Timber.d("sendDummy(${length}) enter")
        assert(length in 0..MAX_PAYLOAD_LENGTH);
        val buffer = ByteBuffer.allocate(length + 4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(0u.toShort()) // type = DUMMY
        buffer.putShort(length.toUShort().toShort()) // payloadLength = length
        buffer.put(ByteArray(length) { 255u.toByte() })
        buffer.flip()
        val written = socket?.write(buffer)
        Timber.d("sendDummy(${length}) done with $written")
    }

    suspend fun setProgram(program: Int) = withContext(coroutineScope.coroutineContext) {
        Timber.d("setProgram(${program})")
        val buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(0x801u.toShort()) // type = SET_PROGRAM
        buffer.putShort(2)// payloadLength = 2
        buffer.putShort(program.toUShort().toShort())
        buffer.flip()
        val written = socket?.write(buffer)
        Timber.d("setProgram(${program}) done with $written")

        // Get actual values back...
        sendDummy(DEFAULT_INFO_PAYLOAD_SIZE); // Get Info Packet...
    }

    suspend fun sendColumnData(column: Int, data: ByteArray) = withContext(coroutineScope.coroutineContext) {
        // TODO: Implement this as well...
    }

    companion object {
        private const val MAX_PACKET_LENGTH = 1368
        private const val MAX_PAYLOAD_LENGTH = MAX_PACKET_LENGTH - 4
        private const val DEFAULT_INFO_PAYLOAD_SIZE = 16
    }
}
