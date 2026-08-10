package dev.jyotiraditya.dmt.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import dev.jyotiraditya.dmt.domain.model.AccentColor

val TuiBg = Color(0xFF000000)
val TuiSurface = Color(0xFF131415)
val TuiRaised = Color(0xFF080808)
val TuiFg = Color(0xFFC6CBCC)
val TuiBright = Color(0xFFDFE4E5)
val TuiDim = Color(0xFF767D80)
val TuiFaint = Color(0xFF3C4245)
val TuiLine = Color(0xFF1E2122)
var TuiAccent by mutableStateOf(Color(AccentColor.AMBER.argb))
val TuiRed = Color(0xFFB85C50)
val TuiGreen = Color(0xFF7FA05F)

fun AccentColor.toColor(): Color = Color(argb)
