package nl.joris2k.spinningled.device

enum class AxisProgram(val value: UShort) {
    Hue(1u),
    RGB(2u),
    Interlace(3u),
    ImageRGB565(16u),
    Undefined(65535u)
}

fun UShort.toAxisProgram(): AxisProgram {
    return AxisProgram.entries.find { it.value == this } ?: AxisProgram.Undefined
}
