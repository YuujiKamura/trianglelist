package com.jpaver.trianglelist.dxf

/**
 * DXF 仕様の共通定数定義
 * Android版とKMP版で統一して使用する
 */
object DxfConstants {
    
    /**
     * DXF バージョン情報
     */
    object Version {
        const val AC1015 = "AC1015"  // AutoCAD 2000 format
    }
    
    /**
     * 単位系定数
     */
    object Units {
        const val INCH = 1
        const val MILLIMETER = 4
        const val CENTIMETER = 5
        const val METER = 6
    }
    
    /**
     * ACI色番号定数 (AutoCAD Color Index)
     */
    object Colors {
        const val BY_BLOCK = 0
        const val RED = 1
        const val YELLOW = 2
        const val GREEN = 3
        const val CYAN = 4
        const val BLUE = 5
        const val MAGENTA = 6
        const val WHITE_BLACK = 7  // 白/黒（背景反転）
        const val GRAY = 8
        const val LIGHT_GRAY = 9
        
        // Android版で使用される特殊色定数
        const val WHITE = 256  // ByLayer白色
        
        // ハッチング用RGB色（Android版互換）
        const val HATCH_RED = 16769517
        const val HATCH_ORANGE = 16770756
        const val HATCH_YELLOW = 16777180
        const val HATCH_GREEN = 14482130
        const val HATCH_BLUE = 14939391
    }
    
    /**
     * グループコード定数
     */
    object GroupCodes {
        // エンティティ基本情報
        const val ENTITY_TYPE = 0
        const val PRIMARY_TEXT = 1
        const val BLOCK_NAME = 2
        const val OTHER_TEXT = 3
        const val HANDLE = 5
        const val LINETYPE_NAME = 6
        const val TEXT_STYLE_NAME = 7
        const val LAYER_NAME = 8
        const val VARIABLE_NAME = 9
        
        // 座標関連
        const val X_COORDINATE = 10
        const val Y_COORDINATE = 20
        const val Z_COORDINATE = 30
        const val X_COORDINATE_2 = 11
        const val Y_COORDINATE_2 = 21
        const val Z_COORDINATE_2 = 31
        
        // 数値データ
        const val THICKNESS = 39
        const val FLOATING_POINT = 40
        const val REPEAT_VALUE = 49
        const val ANGLE = 50
        const val COLOR_NUMBER = 62
        const val ENTITIES_FOLLOW = 66
        
        // テキスト関連
        const val HORIZONTAL_ALIGNMENT = 72
        const val VERTICAL_ALIGNMENT = 73
    }
    
    /**
     * エンティティタイプ名
     */
    object EntityTypes {
        const val LINE = "LINE"
        const val CIRCLE = "CIRCLE"
        const val ARC = "ARC"
        const val TEXT = "TEXT"
        const val MTEXT = "MTEXT"
        const val LWPOLYLINE = "LWPOLYLINE"
        const val POLYLINE = "POLYLINE"
        const val HATCH = "HATCH"
        const val INSERT = "INSERT"
        const val DIMENSION = "DIMENSION"
    }
    
    /**
     * レイヤー名定数
     */
    object Layers {
        const val DEFAULT = "0"
        const val COLUMN_COLOR_1 = "C-COL-COL1"
        const val TITLE_FRAME = "C-TTL-FRAM"
    }
    
    /**
     * 線タイプ名定数
     */
    object LineTypes {
        const val BY_BLOCK = "ByBlock"
        const val BY_LAYER = "ByLayer"
        const val CONTINUOUS = "Continuous"
    }
    
    /**
     * スタイル名定数
     */
    object Styles {
        const val STANDARD = "Standard"
        const val DIM_STANDARD = "DIMSTANDARD"
    }
    
    /**
     * 固定ハンドル値（Android版互換）
     */
    object Handles {
        const val MODEL_SPACE = "36"
        const val PAPER_SPACE = "32"
        const val ROOT_DICTIONARY = "C"
        const val LTYPE_TABLE = "5"
        const val LAYER_TABLE = "2"
        const val STYLE_TABLE = "3"
    }
    
    /**
     * セクション名
     */
    object Sections {
        const val HEADER = "HEADER"
        const val TABLES = "TABLES"
        const val BLOCKS = "BLOCKS"
        const val ENTITIES = "ENTITIES"
        const val OBJECTS = "OBJECTS"
    }
    
    /**
     * デフォルト値
     */
    object Defaults {
        const val SCALE = 1.0
        const val TEXT_HEIGHT = 0.2
        const val LINE_WIDTH = -3
        const val COORDINATE = 0.0
        const val ROTATION_ANGLE = 0.0
        const val TEXT_SCALE = 1.0
        
        // アライメント
        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1
        const val ALIGN_RIGHT = 2
        const val ALIGN_BASELINE = 0
        const val ALIGN_BOTTOM = 1
        const val ALIGN_MIDDLE = 2
        const val ALIGN_TOP = 3
    }
    
    /**
     * DXFヘッダー変数名
     */
    object HeaderVars {
        const val ACAD_VER = "\$ACADVER"
        const val INS_UNITS = "\$INSUNITS"
        const val EXT_MIN = "\$EXTMIN"
        const val EXT_MAX = "\$EXTMAX"
        const val LIM_MIN = "\$LIMMIN"
        const val LIM_MAX = "\$LIMMAX"
        const val INS_BASE = "\$INSBASE"
        const val DIM_SCALE = "\$DIMSCALE"
        const val LT_SCALE = "\$LTSCALE"
        const val TEXT_SIZE = "\$TEXTSIZE"
        const val TRACE_WID = "\$TRACEWID"
        const val TEXT_STYLE = "\$TEXTSTYLE"
        const val C_LAYER = "\$CLAYER"
        const val CEL_TYPE = "\$CELTYPE"
        const val CE_COLOR = "\$CECOLOR"
        const val CELT_SCALE = "\$CELTSCALE"
    }
    
    /**
     * Paper設定（Android版互換）
     */
    object Paper {
        const val A3_WIDTH = 420f
        const val A3_HEIGHT = 297f
        const val A3_NAME = "A3"
    }
}