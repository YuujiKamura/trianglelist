//! Triangle構造体定義
//!
//! Kotlinの Triangle クラスを Rust に移植したもの。
//! 三角形の基本プロパティを保持し、検証と面積計算を提供する。

use trilib::model::PointXY;
use crate::model::{ConnParam, ConnectionSide};

/// 三角形の色を表すenum
#[derive(Clone, Copy, Debug, PartialEq, Eq, Default)]
#[repr(u8)]
pub enum TriangleColor {
    #[default]
    Default = 4,
    Red = 0,
    Green = 1,
    Blue = 2,
    Yellow = 3,
    Gray = 5,
}

/// 三角形構造体
///
/// 三角形の辺の役割:
/// - A辺: 接続辺（親との共有辺）
/// - B辺: 自由辺（他の三角形が接続可能）
/// - C辺: 自由辺（他の三角形が接続可能）
#[derive(Clone, Debug, PartialEq)]
pub struct Triangle {
    // === 識別 ===
    /// 三角形の番号
    pub number: i32,
    /// 三角形の名前
    pub name: String,

    // === 寸法 ===
    /// 各辺の長さ [A, B, C]
    pub lengths: [f32; 3],
    /// スケール適用前の元の長さ [A, B, C]
    pub original_lengths: [f32; 3],
    /// 基準角度（度）
    pub angle: f32,

    // === 頂点座標 ===
    /// CA頂点（A辺とC辺の交点）
    pub point_ca: PointXY,
    /// AB頂点（A辺とB辺の交点）
    pub point_ab: PointXY,
    /// BC頂点（B辺とC辺の交点）
    pub point_bc: PointXY,
    /// 重心
    pub point_center: PointXY,

    // === 内角（度） ===
    /// CA頂点の内角
    pub angle_ca: f32,
    /// AB頂点の内角
    pub angle_ab: f32,
    /// BC頂点の内角
    pub angle_bc: f32,

    // === 接続情報 ===
    /// 親三角形の番号（-1 = 独立）
    pub parent_number: i32,
    /// 親の接続辺（None = 独立）
    pub connection_side: Option<ConnectionSide>,
    /// 接続パラメータ
    pub conn_param: ConnParam,

    // === 表示設定 ===
    /// 三角形の色
    pub color: TriangleColor,
}

impl Triangle {
    /// 独立三角形を作成
    ///
    /// # Arguments
    /// * `a` - A辺の長さ
    /// * `b` - B辺の長さ
    /// * `c` - C辺の長さ
    ///
    /// # Returns
    /// 新しい独立三角形（接続なし）
    pub fn new_independent(a: f32, b: f32, c: f32) -> Self {
        Self {
            number: 1,
            name: String::new(),
            lengths: [a, b, c],
            original_lengths: [a, b, c],
            angle: 180.0,
            point_ca: PointXY::zero(),
            point_ab: PointXY::zero(),
            point_bc: PointXY::zero(),
            point_center: PointXY::zero(),
            angle_ca: 0.0,
            angle_ab: 0.0,
            angle_bc: 0.0,
            parent_number: -1,
            connection_side: None,
            conn_param: ConnParam::independent(),
            color: TriangleColor::Default,
        }
    }

    /// 三角不等式が成立するか検証
    ///
    /// 任意の2辺の和が残りの1辺より大きい必要がある
    pub fn is_valid_lengths(&self) -> bool {
        let [a, b, c] = self.lengths;
        if a <= 0.0 || b <= 0.0 || c <= 0.0 {
            return false;
        }
        a + b > c && b + c > a && c + a > b
    }

    /// ヘロンの公式で面積を計算
    ///
    /// # Returns
    /// 三角形の面積（小数第2位で四捨五入）
    pub fn area(&self) -> f32 {
        let [a, b, c] = self.original_lengths;
        let s = (a + b + c) / 2.0; // 半周長
        let heron = s * (s - a) * (s - b) * (s - c);
        if heron <= 0.0 {
            return 0.0;
        }
        let area = heron.sqrt();
        // 小数第2位で四捨五入
        (area * 100.0).round() / 100.0
    }

    /// 独立三角形かどうか
    pub fn is_independent(&self) -> bool {
        self.parent_number == -1 && self.connection_side.is_none()
    }

    // === アクセサ ===

    /// A辺の長さを取得
    pub fn length_a(&self) -> f32 {
        self.lengths[0]
    }

    /// B辺の長さを取得
    pub fn length_b(&self) -> f32 {
        self.lengths[1]
    }

    /// C辺の長さを取得
    pub fn length_c(&self) -> f32 {
        self.lengths[2]
    }

    /// 元のA辺の長さを取得
    pub fn original_length_a(&self) -> f32 {
        self.original_lengths[0]
    }

    /// 元のB辺の長さを取得
    pub fn original_length_b(&self) -> f32 {
        self.original_lengths[1]
    }

    /// 元のC辺の長さを取得
    pub fn original_length_c(&self) -> f32 {
        self.original_lengths[2]
    }
}

impl Default for Triangle {
    fn default() -> Self {
        Self::new_independent(0.0, 0.0, 0.0)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const EPSILON: f32 = 0.01;

    fn approx_eq(a: f32, b: f32) -> bool {
        (a - b).abs() < EPSILON
    }

    #[test]
    fn test_new_independent() {
        let tri = Triangle::new_independent(3.0, 4.0, 5.0);
        assert_eq!(tri.number, 1);
        assert!(tri.name.is_empty());
        assert_eq!(tri.lengths, [3.0, 4.0, 5.0]);
        assert_eq!(tri.original_lengths, [3.0, 4.0, 5.0]);
        assert!(approx_eq(tri.angle, 180.0));
        assert_eq!(tri.parent_number, -1);
        assert!(tri.connection_side.is_none());
        assert!(tri.is_independent());
    }

    #[test]
    fn test_is_valid_lengths_valid() {
        // 3-4-5直角三角形
        let tri = Triangle::new_independent(3.0, 4.0, 5.0);
        assert!(tri.is_valid_lengths());

        // 正三角形
        let equilateral = Triangle::new_independent(5.0, 5.0, 5.0);
        assert!(equilateral.is_valid_lengths());
    }

    #[test]
    fn test_is_valid_lengths_invalid() {
        // 1+2 = 3なので成立しない
        let degenerate = Triangle::new_independent(1.0, 2.0, 3.0);
        assert!(!degenerate.is_valid_lengths());

        // 1+2 < 4なので成立しない
        let invalid = Triangle::new_independent(1.0, 2.0, 4.0);
        assert!(!invalid.is_valid_lengths());
    }

    #[test]
    fn test_is_valid_lengths_zero_or_negative() {
        let zero = Triangle::new_independent(0.0, 4.0, 5.0);
        assert!(!zero.is_valid_lengths());

        let negative = Triangle::new_independent(-3.0, 4.0, 5.0);
        assert!(!negative.is_valid_lengths());
    }

    #[test]
    fn test_area_right_triangle() {
        // 3-4-5直角三角形の面積 = 6
        let tri = Triangle::new_independent(3.0, 4.0, 5.0);
        assert!(approx_eq(tri.area(), 6.0));
    }

    #[test]
    fn test_area_equilateral() {
        // 正三角形 辺5の面積 = (√3/4) * 25 ≈ 10.825
        let tri = Triangle::new_independent(5.0, 5.0, 5.0);
        let expected = (3.0_f32.sqrt() / 4.0) * 25.0;
        assert!(approx_eq(tri.area(), (expected * 100.0).round() / 100.0));
    }

    #[test]
    fn test_area_invalid_triangle() {
        let invalid = Triangle::new_independent(1.0, 2.0, 4.0);
        assert!(approx_eq(invalid.area(), 0.0));
    }

    #[test]
    fn test_is_independent() {
        let independent = Triangle::new_independent(3.0, 4.0, 5.0);
        assert!(independent.is_independent());

        let mut connected = Triangle::new_independent(3.0, 4.0, 5.0);
        connected.parent_number = 1;
        connected.connection_side = Some(ConnectionSide::B);
        assert!(!connected.is_independent());
    }

    #[test]
    fn test_accessors() {
        let tri = Triangle::new_independent(3.0, 4.0, 5.0);
        assert!(approx_eq(tri.length_a(), 3.0));
        assert!(approx_eq(tri.length_b(), 4.0));
        assert!(approx_eq(tri.length_c(), 5.0));
        assert!(approx_eq(tri.original_length_a(), 3.0));
        assert!(approx_eq(tri.original_length_b(), 4.0));
        assert!(approx_eq(tri.original_length_c(), 5.0));
    }

    #[test]
    fn test_default() {
        let tri: Triangle = Default::default();
        assert_eq!(tri.number, 1);
        assert_eq!(tri.lengths, [0.0, 0.0, 0.0]);
        assert!(!tri.is_valid_lengths());
    }

    #[test]
    fn test_clone() {
        let tri = Triangle::new_independent(3.0, 4.0, 5.0);
        let cloned = tri.clone();
        assert_eq!(tri, cloned);
    }

    #[test]
    fn test_color_default() {
        let tri = Triangle::new_independent(3.0, 4.0, 5.0);
        assert_eq!(tri.color, TriangleColor::Default);
    }
}
