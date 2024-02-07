package nl.joris2k.spinningled.device

import java.nio.ByteBuffer

data class AxisPacketHeader(
    val type: UShort,
    val payloadLength: UShort
)

fun ByteBuffer.getAxisPacketHeader(): AxisPacketHeader {
    return AxisPacketHeader(
        getShort().toUShort(),
        getShort().toUShort()
    )
}
