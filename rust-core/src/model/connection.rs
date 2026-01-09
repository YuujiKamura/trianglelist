/// 接続辺を表すenum
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
#[repr(i32)]
pub enum ConnectionSide {
    B = 1,
    C = 2,
}

/// 配置を表すenum
#[derive(Clone, Copy, Debug, PartialEq, Eq, Default)]
#[repr(i32)]
pub enum Alignment {
    Left = 0,
    #[default]
    Center = 2,
    Right = 4,
}

/// 三角形の接続パラメータ
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct ConnParam {
    /// 接続辺（None = 非接続）
    pub side: Option<ConnectionSide>,
    /// 接続タイプ（0=通常）
    pub type_: i32,
    /// 配置
    pub lcr: Alignment,
    /// 共有辺（A辺）の長さ
    pub len_a: f32,
}

impl ConnParam {
    pub fn new(side: Option<ConnectionSide>, type_: i32, lcr: Alignment, len_a: f32) -> Self {
        Self { side, type_, lcr, len_a }
    }

    /// B辺接続（デフォルト設定）
    pub fn b_edge(len_a: f32) -> Self {
        Self::new(Some(ConnectionSide::B), 0, Alignment::Center, len_a)
    }

    /// C辺接続（デフォルト設定）
    pub fn c_edge(len_a: f32) -> Self {
        Self::new(Some(ConnectionSide::C), 0, Alignment::Center, len_a)
    }

    /// 独立（非接続）
    pub fn independent() -> Self {
        Self::new(None, 0, Alignment::Left, 0.0)
    }

    pub fn is_connected(&self) -> bool {
        self.side.is_some()
    }
}

impl Default for ConnParam {
    fn default() -> Self {
        Self::independent()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_new() {
        let param = ConnParam::new(Some(ConnectionSide::B), 0, Alignment::Center, 5.0);
        assert_eq!(param.side, Some(ConnectionSide::B));
        assert_eq!(param.type_, 0);
        assert_eq!(param.lcr, Alignment::Center);
        assert_eq!(param.len_a, 5.0);
    }

    #[test]
    fn test_b_edge() {
        let param = ConnParam::b_edge(5.0);
        assert_eq!(param.side, Some(ConnectionSide::B));
        assert_eq!(param.lcr, Alignment::Center);
        assert_eq!(param.len_a, 5.0);
        assert!(param.is_connected());
    }

    #[test]
    fn test_c_edge() {
        let param = ConnParam::c_edge(4.0);
        assert_eq!(param.side, Some(ConnectionSide::C));
        assert_eq!(param.lcr, Alignment::Center);
        assert_eq!(param.len_a, 4.0);
        assert!(param.is_connected());
    }

    #[test]
    fn test_independent() {
        let param = ConnParam::independent();
        assert_eq!(param.side, None);
        assert!(!param.is_connected());
    }

    #[test]
    fn test_default() {
        let param = ConnParam::default();
        assert_eq!(param, ConnParam::independent());
    }

    #[test]
    fn test_copy() {
        let param = ConnParam::b_edge(5.0);
        let copied = param;
        assert_eq!(param, copied);
    }

    #[test]
    fn test_alignment_values() {
        assert_eq!(Alignment::Left as i32, 0);
        assert_eq!(Alignment::Center as i32, 2);
        assert_eq!(Alignment::Right as i32, 4);
    }

    #[test]
    fn test_connection_side_values() {
        assert_eq!(ConnectionSide::B as i32, 1);
        assert_eq!(ConnectionSide::C as i32, 2);
    }
}
