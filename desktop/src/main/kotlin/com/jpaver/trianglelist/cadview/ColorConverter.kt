package com.jpaver.trianglelist.cadview

import androidx.compose.ui.graphics.Color
import com.jpaver.trianglelist.dxf.DxfColor
import com.jpaver.trianglelist.dxf.DxfConstants

/**
 * DxfColorをCompose.Colorに変換するデスクトップ固有の実装
 * AutoCAD Color Index (ACI) 0-255 をサポート
 */
object ColorConverter {

    // ACI カラーテーブル (0-255)
    private val aciColorTable: Array<Color> = arrayOf(
        // 0: ByBlock
        Color.Black,
        // 1-9: 基本色
        Color(0xFFFF0000), // 1: Red
        Color(0xFFFFFF00), // 2: Yellow
        Color(0xFF00FF00), // 3: Green
        Color(0xFF00FFFF), // 4: Cyan
        Color(0xFF0000FF), // 5: Blue
        Color(0xFFFF00FF), // 6: Magenta
        Color(0xFFFFFFFF), // 7: White
        Color(0xFF808080), // 8: Gray
        Color(0xFFC0C0C0), // 9: Light Gray
        // 10-19: Red variations
        Color(0xFFFF0000), // 10
        Color(0xFFFF7F7F), // 11
        Color(0xFFCC0000), // 12
        Color(0xFFCC6666), // 13
        Color(0xFF990000), // 14
        Color(0xFF994C4C), // 15
        Color(0xFF7F0000), // 16
        Color(0xFF7F3F3F), // 17
        Color(0xFF4C0000), // 18
        Color(0xFF4C2626), // 19
        // 20-29: Orange-Red variations
        Color(0xFFFF3F00), // 20
        Color(0xFFFF9F7F), // 21
        Color(0xFFCC3300), // 22
        Color(0xFFCC7F66), // 23
        Color(0xFF992600), // 24
        Color(0xFF995F4C), // 25
        Color(0xFF7F1F00), // 26
        Color(0xFF7F4F3F), // 27
        Color(0xFF4C1300), // 28
        Color(0xFF4C2F26), // 29
        // 30-39: Orange variations
        Color(0xFFFF7F00), // 30: Orange
        Color(0xFFFFBF7F), // 31
        Color(0xFFCC6600), // 32
        Color(0xFFCC9966), // 33
        Color(0xFF994C00), // 34
        Color(0xFF99724C), // 35
        Color(0xFF7F3F00), // 36
        Color(0xFF7F5F3F), // 37
        Color(0xFF4C2600), // 38
        Color(0xFF4C3926), // 39
        // 40-49: Yellow-Orange variations
        Color(0xFFFFBF00), // 40
        Color(0xFFFFDF7F), // 41
        Color(0xFFCC9900), // 42
        Color(0xFFCCB266), // 43
        Color(0xFF997200), // 44
        Color(0xFF99854C), // 45
        Color(0xFF7F5F00), // 46
        Color(0xFF7F6F3F), // 47
        Color(0xFF4C3900), // 48
        Color(0xFF4C4226), // 49
        // 50-59: Yellow variations
        Color(0xFFFFFF00), // 50
        Color(0xFFFFFF7F), // 51
        Color(0xFFCCCC00), // 52
        Color(0xFFCCCC66), // 53
        Color(0xFF999900), // 54
        Color(0xFF99994C), // 55
        Color(0xFF7F7F00), // 56
        Color(0xFF7F7F3F), // 57
        Color(0xFF4C4C00), // 58
        Color(0xFF4C4C26), // 59
        // 60-69: Yellow-Green variations
        Color(0xFFBFFF00), // 60
        Color(0xFFDFFF7F), // 61
        Color(0xFF99CC00), // 62
        Color(0xFFB2CC66), // 63
        Color(0xFF729900), // 64
        Color(0xFF85994C), // 65
        Color(0xFF5F7F00), // 66
        Color(0xFF6F7F3F), // 67
        Color(0xFF394C00), // 68
        Color(0xFF424C26), // 69
        // 70-79: Green-Yellow variations
        Color(0xFF7FFF00), // 70
        Color(0xFFBFFF7F), // 71
        Color(0xFF66CC00), // 72
        Color(0xFF99CC66), // 73
        Color(0xFF4C9900), // 74
        Color(0xFF72994C), // 75
        Color(0xFF3F7F00), // 76
        Color(0xFF5F7F3F), // 77
        Color(0xFF264C00), // 78
        Color(0xFF394C26), // 79
        // 80-89: Green variations
        Color(0xFF3FFF00), // 80
        Color(0xFF9FFF7F), // 81
        Color(0xFF33CC00), // 82
        Color(0xFF7FCC66), // 83
        Color(0xFF269900), // 84
        Color(0xFF5F994C), // 85
        Color(0xFF1F7F00), // 86
        Color(0xFF4F7F3F), // 87
        Color(0xFF134C00), // 88
        Color(0xFF2F4C26), // 89
        // 90-99: Green variations
        Color(0xFF00FF00), // 90
        Color(0xFF7FFF7F), // 91
        Color(0xFF00CC00), // 92
        Color(0xFF66CC66), // 93
        Color(0xFF009900), // 94
        Color(0xFF4C994C), // 95
        Color(0xFF007F00), // 96
        Color(0xFF3F7F3F), // 97
        Color(0xFF004C00), // 98
        Color(0xFF264C26), // 99
        // 100-109: Green-Cyan variations
        Color(0xFF00FF3F), // 100
        Color(0xFF7FFF9F), // 101
        Color(0xFF00CC33), // 102
        Color(0xFF66CC7F), // 103
        Color(0xFF009926), // 104
        Color(0xFF4C995F), // 105
        Color(0xFF007F1F), // 106
        Color(0xFF3F7F4F), // 107
        Color(0xFF004C13), // 108
        Color(0xFF264C2F), // 109
        // 110-119: Cyan-Green variations
        Color(0xFF00FF7F), // 110
        Color(0xFF7FFFBF), // 111
        Color(0xFF00CC66), // 112
        Color(0xFF66CC99), // 113
        Color(0xFF00994C), // 114
        Color(0xFF4C9972), // 115
        Color(0xFF007F3F), // 116
        Color(0xFF3F7F5F), // 117
        Color(0xFF004C26), // 118
        Color(0xFF264C39), // 119
        // 120-129: Cyan variations
        Color(0xFF00FFBF), // 120
        Color(0xFF7FFFDF), // 121
        Color(0xFF00CC99), // 122
        Color(0xFF66CCB2), // 123
        Color(0xFF009972), // 124
        Color(0xFF4C9985), // 125
        Color(0xFF007F5F), // 126
        Color(0xFF3F7F6F), // 127
        Color(0xFF004C39), // 128
        Color(0xFF264C42), // 129
        // 130-139: Cyan variations
        Color(0xFF00FFFF), // 130
        Color(0xFF7FFFFF), // 131
        Color(0xFF00CCCC), // 132
        Color(0xFF66CCCC), // 133
        Color(0xFF009999), // 134
        Color(0xFF4C9999), // 135
        Color(0xFF007F7F), // 136
        Color(0xFF3F7F7F), // 137
        Color(0xFF004C4C), // 138
        Color(0xFF264C4C), // 139
        // 140-149: Blue-Cyan variations
        Color(0xFF00BFFF), // 140
        Color(0xFF7FDFFF), // 141
        Color(0xFF0099CC), // 142
        Color(0xFF66B2CC), // 143
        Color(0xFF007299), // 144
        Color(0xFF4C8599), // 145
        Color(0xFF005F7F), // 146
        Color(0xFF3F6F7F), // 147
        Color(0xFF00394C), // 148
        Color(0xFF26424C), // 149
        // 150-159: Blue variations
        Color(0xFF007FFF), // 150
        Color(0xFF7FBFFF), // 151
        Color(0xFF0066CC), // 152
        Color(0xFF6699CC), // 153
        Color(0xFF004C99), // 154
        Color(0xFF4C7299), // 155
        Color(0xFF003F7F), // 156
        Color(0xFF3F5F7F), // 157
        Color(0xFF00264C), // 158
        Color(0xFF26394C), // 159
        // 160-169: Blue variations
        Color(0xFF003FFF), // 160
        Color(0xFF7F9FFF), // 161
        Color(0xFF0033CC), // 162
        Color(0xFF667FCC), // 163
        Color(0xFF002699), // 164
        Color(0xFF4C5F99), // 165
        Color(0xFF001F7F), // 166
        Color(0xFF3F4F7F), // 167
        Color(0xFF00134C), // 168
        Color(0xFF262F4C), // 169
        // 170-179: Blue variations
        Color(0xFF0000FF), // 170
        Color(0xFF7F7FFF), // 171
        Color(0xFF0000CC), // 172
        Color(0xFF6666CC), // 173
        Color(0xFF000099), // 174
        Color(0xFF4C4C99), // 175
        Color(0xFF00007F), // 176
        Color(0xFF3F3F7F), // 177
        Color(0xFF00004C), // 178
        Color(0xFF26264C), // 179
        // 180-189: Blue-Magenta variations
        Color(0xFF3F00FF), // 180
        Color(0xFF9F7FFF), // 181
        Color(0xFF3300CC), // 182
        Color(0xFF7F66CC), // 183
        Color(0xFF260099), // 184
        Color(0xFF5F4C99), // 185
        Color(0xFF1F007F), // 186
        Color(0xFF4F3F7F), // 187
        Color(0xFF13004C), // 188
        Color(0xFF2F264C), // 189
        // 190-199: Magenta-Blue variations
        Color(0xFF7F00FF), // 190
        Color(0xFFBF7FFF), // 191
        Color(0xFF6600CC), // 192
        Color(0xFF9966CC), // 193
        Color(0xFF4C0099), // 194
        Color(0xFF724C99), // 195
        Color(0xFF3F007F), // 196
        Color(0xFF5F3F7F), // 197
        Color(0xFF26004C), // 198
        Color(0xFF39264C), // 199
        // 200-209: Magenta variations
        Color(0xFFBF00FF), // 200
        Color(0xFFDF7FFF), // 201
        Color(0xFF9900CC), // 202
        Color(0xFFB266CC), // 203
        Color(0xFF720099), // 204
        Color(0xFF854C99), // 205
        Color(0xFF5F007F), // 206
        Color(0xFF6F3F7F), // 207
        Color(0xFF39004C), // 208
        Color(0xFF42264C), // 209
        // 210-219: Magenta variations
        Color(0xFFFF00FF), // 210
        Color(0xFFFF7FFF), // 211
        Color(0xFFCC00CC), // 212
        Color(0xFFCC66CC), // 213
        Color(0xFF990099), // 214
        Color(0xFF994C99), // 215
        Color(0xFF7F007F), // 216
        Color(0xFF7F3F7F), // 217
        Color(0xFF4C004C), // 218
        Color(0xFF4C264C), // 219
        // 220-229: Red-Magenta variations
        Color(0xFFFF00BF), // 220
        Color(0xFFFF7FDF), // 221
        Color(0xFFCC0099), // 222
        Color(0xFFCC66B2), // 223
        Color(0xFF990072), // 224
        Color(0xFF994C85), // 225
        Color(0xFF7F005F), // 226
        Color(0xFF7F3F6F), // 227
        Color(0xFF4C0039), // 228
        Color(0xFF4C2642), // 229
        // 230-239: Red-Magenta variations
        Color(0xFFFF007F), // 230
        Color(0xFFFF7FBF), // 231
        Color(0xFFCC0066), // 232
        Color(0xFFCC6699), // 233
        Color(0xFF99004C), // 234
        Color(0xFF994C72), // 235
        Color(0xFF7F003F), // 236
        Color(0xFF7F3F5F), // 237
        Color(0xFF4C0026), // 238
        Color(0xFF4C2639), // 239
        // 240-249: Red variations
        Color(0xFFFF003F), // 240
        Color(0xFFFF7F9F), // 241
        Color(0xFFCC0033), // 242
        Color(0xFFCC667F), // 243
        Color(0xFF990026), // 244
        Color(0xFF994C5F), // 245
        Color(0xFF7F001F), // 246
        Color(0xFF7F3F4F), // 247
        Color(0xFF4C0013), // 248
        Color(0xFF4C262F), // 249
        // 250-255: Grayscale
        Color(0xFF333333), // 250
        Color(0xFF505050), // 251
        Color(0xFF696969), // 252
        Color(0xFF828282), // 253
        Color(0xFFBEBEBE), // 254
        Color(0xFFFFFFFF), // 255
    )

    fun toComposeColor(dxfColor: DxfColor): Color {
        val index = dxfColor.aciIndex
        return when {
            index == DxfConstants.Colors.WHITE -> Color.White // 256: ByLayer
            index in 0..255 -> aciColorTable[index]
            else -> aciColorTable[7] // 範囲外は白
        }
    }

    fun aciToColor(aciIndex: Int): Color {
        return toComposeColor(DxfColor.fromAci(aciIndex))
    }
}
