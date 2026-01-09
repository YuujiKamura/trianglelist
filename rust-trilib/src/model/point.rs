use std::f32::consts::PI;

/// 2D座標点
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct PointXY {
    pub x: f32,
    pub y: f32,
}

impl PointXY {
    /// 丸め誤差の閾値
    const THRESHOLD: f32 = 1e-5;
    /// フォーマット時のデフォルト小数点以下桁数
    pub const DECIMAL: usize = 4;

    /// 新しい座標点を作成（丸め誤差調整あり）
    /// 注意: 演算のたびに丸めが発生します。丸めなしで作成する場合は`raw`を使用してください。
    pub fn new(x: f32, y: f32) -> Self {
        Self {
            x: Self::adjust_for_rounding_error(x),
            y: Self::adjust_for_rounding_error(y),
        }
    }

    /// 丸め誤差調整なしで座標点を作成
    /// 精密な計算が必要な場合はこちらを使用してください。
    pub fn raw(x: f32, y: f32) -> Self {
        Self { x, y }
    }

    /// ゼロ点を作成
    pub fn zero() -> Self {
        Self { x: 0.0, y: 0.0 }
    }

    /// スケール付きコンストラクタ
    pub fn with_scale(x: f32, y: f32, scale: f32) -> Self {
        Self::new(x * scale, y * scale)
    }

    /// 丸め誤差の調整
    fn adjust_for_rounding_error(value: f32) -> f32 {
        if value.abs() < Self::THRESHOLD {
            0.0
        } else {
            value
        }
    }

    /// X座標をフォーマットして返す
    pub fn get_formatted_x(&self, decimal_places: usize) -> String {
        format!("{:.1$}", self.x, decimal_places)
    }

    /// Y座標をフォーマットして返す
    pub fn get_formatted_y(&self, decimal_places: usize) -> String {
        format!("{:.1$}", self.y, decimal_places)
    }

    /// 座標をフォーマットして返す
    pub fn format(&self, decimal_places: usize) -> String {
        format!(
            "({},{})",
            self.get_formatted_x(decimal_places),
            self.get_formatted_y(decimal_places)
        )
    }

    // === 基本演算 ===

    /// 加算（新しい点を返す）
    pub fn add(&self, other: &PointXY) -> PointXY {
        PointXY::new(self.x + other.x, self.y + other.y)
    }

    /// 減算（新しい点を返す）
    pub fn subtract(&self, other: &PointXY) -> PointXY {
        PointXY::new(self.x - other.x, self.y - other.y)
    }

    /// スケーリング（新しい点を返す）
    pub fn scale(&self, factor: f32) -> PointXY {
        PointXY::new(self.x * factor, self.y * factor)
    }

    /// 異なるスケールで座標変換
    pub fn scale_xy(&self, scale_x: f32, scale_y: f32) -> PointXY {
        PointXY::new(self.x * scale_x, self.y * scale_y)
    }

    /// 基準点からの相対スケーリング
    pub fn scale_from(&self, origin: &PointXY, scale_x: f32, scale_y: f32) -> PointXY {
        let relative_x = self.x - origin.x;
        let relative_y = self.y - origin.y;
        let scaled_x = relative_x * scale_x;
        let scaled_y = relative_y * scale_y;
        PointXY::new(scaled_x + origin.x, scaled_y + origin.y)
    }

    // === ベクトル演算 ===

    /// 他の点へのベクトル
    pub fn vector_to(&self, other: &PointXY) -> PointXY {
        PointXY::new(other.x - self.x, other.y - self.y)
    }

    /// ベクトルの長さ（原点からの距離）
    pub fn length(&self) -> f32 {
        (self.x * self.x + self.y * self.y).sqrt()
    }

    /// 2点間の距離
    pub fn distance_to(&self, other: &PointXY) -> f32 {
        self.vector_to(other).length()
    }

    /// 正規化（単位ベクトル）
    pub fn normalize(&self) -> PointXY {
        let len = self.length();
        if len == 0.0 {
            return PointXY::zero();
        }
        PointXY::new(self.x / len, self.y / len)
    }

    /// ベクトルの大きさ（f64）
    pub fn magnitude(&self) -> f64 {
        ((self.x * self.x + self.y * self.y) as f64).sqrt()
    }

    /// ベクトル内積
    pub fn inner_product(&self, other: &PointXY) -> f64 {
        (self.x * other.x + self.y * other.y) as f64
    }

    /// ベクトル外積（2D、スカラー値）
    pub fn outer_product(&self, other: &PointXY) -> f64 {
        (self.x * other.y - self.y * other.x) as f64
    }

    /// ベクトルが右向きかどうか
    pub fn is_vector_to_right(&self, p2: &PointXY) -> bool {
        self.vector_to(p2).x > 0.0
    }

    // === 幾何演算 ===

    /// 角度方向にオフセット
    pub fn offset(&self, distance: f32, angle_degrees: f32) -> PointXY {
        let offset_vec = Self::to_vector(angle_degrees).scale(distance);
        self.add(&offset_vec)
    }

    /// 他の点への方向にオフセット
    pub fn offset_toward(&self, target: &PointXY, distance: f32) -> PointXY {
        let offset_vec = self.vector_to(target).normalize().scale(distance);
        self.add(&offset_vec)
    }

    /// 角度からベクトルを生成
    pub fn to_vector(angle_degrees: f32) -> PointXY {
        let radians = angle_degrees * PI / 180.0;
        PointXY::new(radians.cos(), radians.sin())
    }

    /// 中心点を基準に回転
    pub fn rotate(&self, center: &PointXY, degrees: f32) -> PointXY {
        let radians = degrees * PI / 180.0;
        let offset_x = (self.x - center.x) as f64;
        let offset_y = (self.y - center.y) as f64;
        let cos_r = (radians as f64).cos();
        let sin_r = (radians as f64).sin();
        PointXY::new(
            (offset_x * cos_r - offset_y * sin_r) as f32 + center.x,
            (offset_x * sin_r + offset_y * cos_r) as f32 + center.y,
        )
    }

    /// 直線に対してミラー
    pub fn mirror(&self, line_start: &PointXY, line_end: &PointXY, clockwise: f32) -> PointXY {
        let angle = self.calc_angle_180(line_start, line_end);
        self.rotate(line_start, angle * 2.0 * clockwise)
    }

    /// X軸方向への移動と回転
    pub fn move_x(&self, length: f32, angle: f32) -> PointXY {
        let moved = PointXY::new(self.x + length, self.y);
        moved.rotate(self, angle)
    }

    /// 直交方向へのオフセット
    pub fn cross_offset(&self, target: &PointXY, distance: f32, clockwise: f32) -> PointXY {
        let rotated = target.rotate(self, clockwise);
        let offset_vec = self.vector_to(&rotated).normalize().scale(distance);
        self.subtract(&offset_vec)
    }

    // === 角度計算 ===

    /// 3点から角度を計算（-180〜180度）
    /// self → p2 と p3 → p2 のなす角度
    /// atan2を使用して境界値（0度、180度）でも正確な結果を返す
    pub fn calc_angle_180(&self, p2: &PointXY, p3: &PointXY) -> f32 {
        let v1 = p2.subtract(self);
        let v2 = p2.subtract(p3);
        // atan2(外積, 内積)で符号付き角度を計算
        let angle_rad = v1.outer_product(&v2).atan2(v1.inner_product(&v2));
        angle_rad.to_degrees() as f32
    }

    /// 3点から角度を計算（0〜360度）
    pub fn calc_angle_360(&self, p2: &PointXY, p3: &PointXY) -> f32 {
        let angle = self.calc_angle_180(p2, p3);
        if angle < 0.0 {
            360.0 + angle
        } else {
            angle
        }
    }

    /// X軸との角度を計算（0〜360度）
    pub fn calc_angle_with_x_axis(&self, p2: &PointXY) -> f32 {
        let dx = p2.x - self.x;
        let dy = p2.y - self.y;
        let mut angle = dy.atan2(dx) * 180.0 / PI;
        if angle < 0.0 {
            angle += 360.0;
        }
        angle
    }

    /// 寸法用角度を計算
    pub fn calc_dim_angle(&self, other: &PointXY) -> f32 {
        let mut angle = ((other.x - self.x) as f64).atan2((other.y - self.y) as f64) * 180.0
            / std::f64::consts::PI;
        angle = -angle;
        angle += 90.0;
        if angle > 90.0 {
            angle -= 180.0;
        }
        if angle < 0.0 {
            angle += 360.0;
        }
        angle as f32
    }

    // === ユーティリティ ===

    /// 中点を計算
    pub fn midpoint(&self, other: &PointXY) -> PointXY {
        PointXY::new((self.x + other.x) / 2.0, (self.y + other.y) / 2.0)
    }

    /// 各成分の最小値
    pub fn min(&self, other: &PointXY) -> PointXY {
        PointXY::new(self.x.min(other.x), self.y.min(other.y))
    }

    /// 各成分の最大値
    pub fn max(&self, other: &PointXY) -> PointXY {
        PointXY::new(self.x.max(other.x), self.y.max(other.y))
    }

    /// 近傍判定（矩形範囲）
    pub fn is_near(&self, target: &PointXY, range: f32) -> bool {
        self.x > target.x - range
            && self.x < target.x + range
            && self.y > target.y - range
            && self.y < target.y + range
    }

    /// 許容範囲内で等価かどうか
    pub fn equals_approx(&self, x: f32, y: f32, tolerance: f32) -> bool {
        (self.x - x).abs() < tolerance && (self.y - y).abs() < tolerance
    }

    /// 三角形内判定用のヘルパー関数
    fn sign(p1: &PointXY, p2: &PointXY, p3: &PointXY) -> f32 {
        (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
    }

    /// 三角形内判定
    pub fn is_inside_triangle(&self, a: &PointXY, b: &PointXY, c: &PointXY) -> bool {
        let d1 = Self::sign(self, a, b) < 0.0;
        let d2 = Self::sign(self, b, c) < 0.0;
        let d3 = Self::sign(self, c, a) < 0.0;
        d1 == d2 && d2 == d3
    }

    /// 円形範囲での衝突判定
    pub fn is_collide_circle(&self, target: &PointXY, radius: f32) -> bool {
        self.distance_to(target) <= radius
    }

    /// 複数点との衝突判定
    pub fn is_collide_any(&self, targets: &[PointXY], radius: f32) -> bool {
        targets.iter().any(|t| self.distance_to(t) <= radius)
    }

    /// 複数点への距離リスト
    pub fn distances_to(&self, targets: &[PointXY]) -> Vec<f32> {
        targets.iter().map(|t| self.distance_to(t)).collect()
    }

    /// ビュー座標変換
    pub fn translate_and_scale(
        &self,
        base_in_view: &PointXY,
        center_in_model: &PointXY,
        zoom: f32,
    ) -> PointXY {
        self.subtract(base_in_view)
            .scale(1.0 / zoom)
            .add(&center_in_model.scale_xy(1.0, -1.0))
    }
}

// 演算子オーバーロード
impl std::ops::Add for PointXY {
    type Output = PointXY;
    fn add(self, other: PointXY) -> PointXY {
        PointXY::new(self.x + other.x, self.y + other.y)
    }
}

impl std::ops::Add<&PointXY> for PointXY {
    type Output = PointXY;
    fn add(self, other: &PointXY) -> PointXY {
        PointXY::new(self.x + other.x, self.y + other.y)
    }
}

impl std::ops::Sub for PointXY {
    type Output = PointXY;
    fn sub(self, other: PointXY) -> PointXY {
        PointXY::new(self.x - other.x, self.y - other.y)
    }
}

impl std::ops::Sub<&PointXY> for PointXY {
    type Output = PointXY;
    fn sub(self, other: &PointXY) -> PointXY {
        PointXY::new(self.x - other.x, self.y - other.y)
    }
}

impl std::ops::Mul<f32> for PointXY {
    type Output = PointXY;
    fn mul(self, scalar: f32) -> PointXY {
        PointXY::new(self.x * scalar, self.y * scalar)
    }
}

impl Default for PointXY {
    fn default() -> Self {
        Self::zero()
    }
}

impl std::fmt::Display for PointXY {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "(x={}, y={})", self.x, self.y)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const EPSILON: f32 = 0.001;

    fn approx_eq(a: f32, b: f32) -> bool {
        (a - b).abs() < EPSILON
    }

    #[test]
    fn test_new() {
        let p = PointXY::new(3.0, 4.0);
        assert!(approx_eq(p.x, 3.0));
        assert!(approx_eq(p.y, 4.0));
    }

    #[test]
    fn test_zero() {
        let p = PointXY::zero();
        assert!(approx_eq(p.x, 0.0));
        assert!(approx_eq(p.y, 0.0));
    }

    #[test]
    fn test_with_scale() {
        let p = PointXY::with_scale(2.0, 3.0, 2.0);
        assert!(approx_eq(p.x, 4.0));
        assert!(approx_eq(p.y, 6.0));
    }

    #[test]
    fn test_rounding_error_adjustment() {
        let p = PointXY::new(0.000001, -0.000001);
        assert!(approx_eq(p.x, 0.0));
        assert!(approx_eq(p.y, 0.0));
    }

    #[test]
    fn test_distance() {
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(3.0, 4.0);
        assert!(approx_eq(p1.distance_to(&p2), 5.0));
    }

    #[test]
    fn test_length() {
        let p = PointXY::new(3.0, 4.0);
        assert!(approx_eq(p.length(), 5.0));
    }

    #[test]
    fn test_rotate_90() {
        let p = PointXY::new(1.0, 0.0);
        let center = PointXY::zero();
        let rotated = p.rotate(&center, 90.0);
        assert!(approx_eq(rotated.x, 0.0));
        assert!(approx_eq(rotated.y, 1.0));
    }

    #[test]
    fn test_rotate_180() {
        let p = PointXY::new(1.0, 0.0);
        let center = PointXY::zero();
        let rotated = p.rotate(&center, 180.0);
        assert!(approx_eq(rotated.x, -1.0));
        assert!(approx_eq(rotated.y, 0.0));
    }

    #[test]
    fn test_rotate_with_center() {
        let p = PointXY::new(2.0, 0.0);
        let center = PointXY::new(1.0, 0.0);
        let rotated = p.rotate(&center, 90.0);
        assert!(approx_eq(rotated.x, 1.0));
        assert!(approx_eq(rotated.y, 1.0));
    }

    #[test]
    fn test_offset() {
        let p = PointXY::zero();
        let offset = p.offset(10.0, 0.0);
        assert!(approx_eq(offset.x, 10.0));
        assert!(approx_eq(offset.y, 0.0));
    }

    #[test]
    fn test_offset_45_degrees() {
        let p = PointXY::zero();
        let offset = p.offset(10.0, 45.0);
        let expected = 10.0 / (2.0_f32).sqrt();
        assert!(approx_eq(offset.x, expected));
        assert!(approx_eq(offset.y, expected));
    }

    #[test]
    fn test_normalize() {
        let p = PointXY::new(3.0, 4.0);
        let n = p.normalize();
        assert!(approx_eq(n.length(), 1.0));
        assert!(approx_eq(n.x, 0.6));
        assert!(approx_eq(n.y, 0.8));
    }

    #[test]
    fn test_normalize_zero() {
        let p = PointXY::zero();
        let n = p.normalize();
        assert!(approx_eq(n.x, 0.0));
        assert!(approx_eq(n.y, 0.0));
    }

    #[test]
    fn test_inside_triangle() {
        let a = PointXY::new(0.0, 0.0);
        let b = PointXY::new(10.0, 0.0);
        let c = PointXY::new(5.0, 10.0);

        let inside = PointXY::new(5.0, 3.0);
        let outside = PointXY::new(15.0, 5.0);

        assert!(inside.is_inside_triangle(&a, &b, &c));
        assert!(!outside.is_inside_triangle(&a, &b, &c));
    }

    #[test]
    fn test_midpoint() {
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(10.0, 10.0);
        let mid = p1.midpoint(&p2);
        assert!(approx_eq(mid.x, 5.0));
        assert!(approx_eq(mid.y, 5.0));
    }

    #[test]
    fn test_add_operator() {
        let p1 = PointXY::new(1.0, 2.0);
        let p2 = PointXY::new(3.0, 4.0);
        let result = p1 + p2;
        assert!(approx_eq(result.x, 4.0));
        assert!(approx_eq(result.y, 6.0));
    }

    #[test]
    fn test_sub_operator() {
        let p1 = PointXY::new(5.0, 7.0);
        let p2 = PointXY::new(3.0, 4.0);
        let result = p1 - p2;
        assert!(approx_eq(result.x, 2.0));
        assert!(approx_eq(result.y, 3.0));
    }

    #[test]
    fn test_mul_operator() {
        let p = PointXY::new(2.0, 3.0);
        let result = p * 2.0;
        assert!(approx_eq(result.x, 4.0));
        assert!(approx_eq(result.y, 6.0));
    }

    #[test]
    fn test_inner_product() {
        let p1 = PointXY::new(1.0, 0.0);
        let p2 = PointXY::new(0.0, 1.0);
        assert!(approx_eq(p1.inner_product(&p2) as f32, 0.0));

        let p3 = PointXY::new(1.0, 0.0);
        assert!(approx_eq(p1.inner_product(&p3) as f32, 1.0));
    }

    #[test]
    fn test_outer_product() {
        let p1 = PointXY::new(1.0, 0.0);
        let p2 = PointXY::new(0.0, 1.0);
        assert!(approx_eq(p1.outer_product(&p2) as f32, 1.0));
        assert!(approx_eq(p2.outer_product(&p1) as f32, -1.0));
    }

    #[test]
    fn test_calc_angle_with_x_axis() {
        let origin = PointXY::zero();
        let p1 = PointXY::new(1.0, 0.0);
        let p2 = PointXY::new(0.0, 1.0);
        let p3 = PointXY::new(-1.0, 0.0);

        assert!(approx_eq(origin.calc_angle_with_x_axis(&p1), 0.0));
        assert!(approx_eq(origin.calc_angle_with_x_axis(&p2), 90.0));
        assert!(approx_eq(origin.calc_angle_with_x_axis(&p3), 180.0));
    }

    #[test]
    fn test_is_near() {
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(0.5, 0.5);
        let p3 = PointXY::new(2.0, 2.0);

        assert!(p1.is_near(&p2, 1.0));
        assert!(!p1.is_near(&p3, 1.0));
    }

    #[test]
    fn test_collide_circle() {
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(3.0, 4.0);

        assert!(p1.is_collide_circle(&p2, 5.0));
        assert!(p1.is_collide_circle(&p2, 5.1));
        assert!(!p1.is_collide_circle(&p2, 4.9));
    }

    #[test]
    fn test_min_max() {
        let p1 = PointXY::new(1.0, 5.0);
        let p2 = PointXY::new(3.0, 2.0);

        let min = p1.min(&p2);
        let max = p1.max(&p2);

        assert!(approx_eq(min.x, 1.0));
        assert!(approx_eq(min.y, 2.0));
        assert!(approx_eq(max.x, 3.0));
        assert!(approx_eq(max.y, 5.0));
    }

    #[test]
    fn test_format() {
        let p = PointXY::new(1.2345678, 9.8765432);
        let formatted = p.format(2);
        assert_eq!(formatted, "(1.23,9.88)");
    }

    #[test]
    fn test_display() {
        let p = PointXY::new(1.5, 2.5);
        assert_eq!(format!("{}", p), "(x=1.5, y=2.5)");
    }

    #[test]
    fn test_default() {
        let p: PointXY = Default::default();
        assert!(approx_eq(p.x, 0.0));
        assert!(approx_eq(p.y, 0.0));
    }

    #[test]
    fn test_mirror() {
        // mirrorは回転ベースのミラーリング
        // calc_angle_180で計算した角度の2倍 * clockwise だけline_start中心に回転
        let p = PointXY::new(1.0, 0.0);
        let line_start = PointXY::zero();
        let line_end = PointXY::new(1.0, 1.0);
        // angle = -135度, rotation = 270度 -> (0, -1)
        let mirrored = p.mirror(&line_start, &line_end, -1.0);
        assert!(approx_eq(mirrored.x, 0.0));
        assert!(approx_eq(mirrored.y, -1.0));
    }

    #[test]
    fn test_scale_from_origin() {
        let p = PointXY::new(2.0, 3.0);
        let origin = PointXY::new(1.0, 1.0);
        let scaled = p.scale_from(&origin, 2.0, 2.0);
        assert!(approx_eq(scaled.x, 3.0));
        assert!(approx_eq(scaled.y, 5.0));
    }

    #[test]
    fn test_raw_no_rounding() {
        // rawは丸め誤差調整なし
        let p = PointXY::raw(0.000001, -0.000001);
        assert!(approx_eq(p.x, 0.000001));
        assert!(approx_eq(p.y, -0.000001));
    }

    #[test]
    fn test_calc_angle_180_colinear_opposite() {
        // 同一直線上で反対方向 -> 180度
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(1.0, 0.0);
        let p3 = PointXY::new(2.0, 0.0);
        let angle = p1.calc_angle_180(&p2, &p3);
        assert!(approx_eq(angle, 180.0) || approx_eq(angle, -180.0));
    }

    #[test]
    fn test_calc_angle_180_colinear_same() {
        // 同一直線上で同じ方向 -> 0度
        // p1→p2 と p3→p2 が同じ方向
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(1.0, 0.0);
        let p3 = PointXY::new(0.5, 0.0); // p1とp3はp2に対して同じ側
        let angle = p1.calc_angle_180(&p2, &p3);
        assert!(approx_eq(angle, 0.0));
    }

    #[test]
    fn test_calc_angle_180_right_angle() {
        // 直角 -> 90度 or -90度
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(1.0, 0.0);
        let p3 = PointXY::new(1.0, 1.0);
        let angle = p1.calc_angle_180(&p2, &p3);
        assert!(approx_eq(angle.abs(), 90.0));
    }

    #[test]
    fn test_calc_angle_360_colinear() {
        // 180度 (0-360範囲)
        let p1 = PointXY::new(0.0, 0.0);
        let p2 = PointXY::new(1.0, 0.0);
        let p3 = PointXY::new(2.0, 0.0);
        let angle = p1.calc_angle_360(&p2, &p3);
        assert!(approx_eq(angle, 180.0));
    }
}
