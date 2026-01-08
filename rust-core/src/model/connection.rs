/// 三角形の接続パラメータ
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct ConnParam {
    /// 接続辺（1=B辺, 2=C辺）
    pub side: i32,
    /// 接続タイプ（0=通常）
    pub type_: i32,
    /// 配置（0=Left, 2=Center, 4=Right）
    pub lcr: i32,
    /// 共有辺（A辺）の長さ
    pub len_a: f32,
}

impl ConnParam {
    pub fn new(side: i32, type_: i32, lcr: i32, len_a: f32) -> Self {
        Self { side, type_, lcr, len_a }
    }

    /// B辺接続（デフォルト設定）
    pub fn b_edge(len_a: f32) -> Self {
        Self::new(1, 0, 2, len_a)
    }

    /// C辺接続（デフォルト設定）
    pub fn c_edge(len_a: f32) -> Self {
        Self::new(2, 0, 2, len_a)
    }

    /// 独立（非接続）
    pub fn independent() -> Self {
        Self::new(-1, 0, 0, 0.0)
    }

    pub fn is_connected(&self) -> bool {
        self.side > 0
    }
}

impl Default for ConnParam {
    fn default() -> Self {
        Self::independent()
    }
}

/// 配置定数
pub struct Alignment;

impl Alignment {
    pub const LEFT: i32 = 0;
    pub const CENTER: i32 = 2;
    pub const RIGHT: i32 = 4;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_new() {
        let param = ConnParam::new(1, 0, 2, 5.0);
        assert_eq!(param.side, 1);
        assert_eq!(param.type_, 0);
        assert_eq!(param.lcr, 2);
        assert_eq!(param.len_a, 5.0);
    }

    #[test]
    fn test_b_edge() {
        let param = ConnParam::b_edge(5.0);
        assert_eq!(param.side, 1);
        assert_eq!(param.lcr, Alignment::CENTER);
        assert_eq!(param.len_a, 5.0);
        assert!(param.is_connected());
    }

    #[test]
    fn test_c_edge() {
        let param = ConnParam::c_edge(4.0);
        assert_eq!(param.side, 2);
        assert_eq!(param.lcr, Alignment::CENTER);
        assert_eq!(param.len_a, 4.0);
        assert!(param.is_connected());
    }

    #[test]
    fn test_independent() {
        let param = ConnParam::independent();
        assert_eq!(param.side, -1);
        assert!(!param.is_connected());
    }

    #[test]
    fn test_default() {
        let param = ConnParam::default();
        assert_eq!(param, ConnParam::independent());
    }

    #[test]
    fn test_clone() {
        let param = ConnParam::b_edge(5.0);
        let cloned = param;
        assert_eq!(param, cloned);
    }

    #[test]
    fn test_alignment_constants() {
        assert_eq!(Alignment::LEFT, 0);
        assert_eq!(Alignment::CENTER, 2);
        assert_eq!(Alignment::RIGHT, 4);
    }
}
