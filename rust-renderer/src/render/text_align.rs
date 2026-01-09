/// 水平アライメント（DXF互換）
#[derive(Clone, Copy, Debug, PartialEq, Default)]
#[repr(i32)]
pub enum HorizontalAlign {
    #[default]
    Left = 0,
    Center = 1,
    Right = 2,
}

/// 垂直アライメント（DXF互換）
#[derive(Clone, Copy, Debug, PartialEq, Default)]
#[repr(i32)]
pub enum VerticalAlign {
    #[default]
    Baseline = 0,
    Bottom = 1,
    Middle = 2,
    Top = 3,
}

/// テキストアライメント設定
#[derive(Clone, Copy, Debug, PartialEq, Default)]
pub struct TextAlign {
    pub horizontal: HorizontalAlign,
    pub vertical: VerticalAlign,
}

impl TextAlign {
    pub fn new(horizontal: HorizontalAlign, vertical: VerticalAlign) -> Self {
        Self { horizontal, vertical }
    }

    /// DXF値から生成
    pub fn from_dxf(align_h: i32, align_v: i32) -> Self {
        let horizontal = match align_h {
            1 => HorizontalAlign::Center,
            2 => HorizontalAlign::Right,
            _ => HorizontalAlign::Left,
        };
        let vertical = match align_v {
            1 => VerticalAlign::Bottom,
            2 => VerticalAlign::Middle,
            3 => VerticalAlign::Top,
            _ => VerticalAlign::Baseline,
        };
        Self { horizontal, vertical }
    }

    /// DXF値に変換
    pub fn to_dxf(&self) -> (i32, i32) {
        (self.horizontal as i32, self.vertical as i32)
    }

    /// テキスト描画位置のオフセットを計算
    /// width: テキストの幅, height: テキストの高さ
    /// 戻り値: (x_offset, y_offset)
    pub fn calc_offset(&self, width: f32, height: f32) -> (f32, f32) {
        let x_offset = match self.horizontal {
            HorizontalAlign::Left => 0.0,
            HorizontalAlign::Center => -width / 2.0,
            HorizontalAlign::Right => -width,
        };
        let y_offset = match self.vertical {
            VerticalAlign::Top => -height,
            VerticalAlign::Middle => -height / 2.0,
            VerticalAlign::Bottom => 0.0,
            VerticalAlign::Baseline => height * 0.2,  // 概算
        };
        (x_offset, y_offset)
    }
}

// プリセット
impl TextAlign {
    pub const LEFT_TOP: Self = Self {
        horizontal: HorizontalAlign::Left,
        vertical: VerticalAlign::Top
    };
    pub const CENTER_CENTER: Self = Self {
        horizontal: HorizontalAlign::Center,
        vertical: VerticalAlign::Middle
    };
    pub const RIGHT_BOTTOM: Self = Self {
        horizontal: HorizontalAlign::Right,
        vertical: VerticalAlign::Bottom
    };
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_from_dxf() {
        let align = TextAlign::from_dxf(1, 2);
        assert_eq!(align.horizontal, HorizontalAlign::Center);
        assert_eq!(align.vertical, VerticalAlign::Middle);
    }

    #[test]
    fn test_to_dxf() {
        let align = TextAlign::CENTER_CENTER;
        assert_eq!(align.to_dxf(), (1, 2));
    }

    #[test]
    fn test_calc_offset_center() {
        let align = TextAlign::CENTER_CENTER;
        let (x, y) = align.calc_offset(100.0, 20.0);
        assert_eq!(x, -50.0);
        assert_eq!(y, -10.0);
    }

    #[test]
    fn test_from_dxf_left_baseline() {
        let align = TextAlign::from_dxf(0, 0);
        assert_eq!(align.horizontal, HorizontalAlign::Left);
        assert_eq!(align.vertical, VerticalAlign::Baseline);
    }

    #[test]
    fn test_from_dxf_right_top() {
        let align = TextAlign::from_dxf(2, 3);
        assert_eq!(align.horizontal, HorizontalAlign::Right);
        assert_eq!(align.vertical, VerticalAlign::Top);
    }

    #[test]
    fn test_calc_offset_left_top() {
        let align = TextAlign::LEFT_TOP;
        let (x, y) = align.calc_offset(100.0, 20.0);
        assert_eq!(x, 0.0);
        assert_eq!(y, -20.0);
    }

    #[test]
    fn test_calc_offset_right_bottom() {
        let align = TextAlign::RIGHT_BOTTOM;
        let (x, y) = align.calc_offset(100.0, 20.0);
        assert_eq!(x, -100.0);
        assert_eq!(y, 0.0);
    }

    #[test]
    fn test_default() {
        let align = TextAlign::default();
        assert_eq!(align.horizontal, HorizontalAlign::Left);
        assert_eq!(align.vertical, VerticalAlign::Baseline);
    }

    #[test]
    fn test_new() {
        let align = TextAlign::new(HorizontalAlign::Right, VerticalAlign::Top);
        assert_eq!(align.horizontal, HorizontalAlign::Right);
        assert_eq!(align.vertical, VerticalAlign::Top);
    }

    #[test]
    fn test_invalid_dxf_values() {
        // 範囲外の値は Left/Baseline にフォールバック
        let align = TextAlign::from_dxf(99, -1);
        assert_eq!(align.horizontal, HorizontalAlign::Left);
        assert_eq!(align.vertical, VerticalAlign::Baseline);
    }
}
