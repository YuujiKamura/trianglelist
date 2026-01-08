//! DXF entity data structures
//!
//! This module defines the core data structures for DXF entities:
//! - `DxfLine`: LINE entity with coordinates, color, and layer
//! - `DxfText`: TEXT entity with position, content, styling, and alignment

/// DXF LINE entity
///
/// Represents a line segment with start and end coordinates.
#[derive(Clone, Debug, PartialEq)]
pub struct DxfLine {
    /// X coordinate of the start point
    pub x1: f64,
    /// Y coordinate of the start point
    pub y1: f64,
    /// X coordinate of the end point
    pub x2: f64,
    /// Y coordinate of the end point
    pub y2: f64,
    /// Color number (default: 7 = white)
    pub color: i32,
    /// Layer name (default: "0")
    pub layer: String,
}

impl Default for DxfLine {
    fn default() -> Self {
        Self {
            x1: 0.0,
            y1: 0.0,
            x2: 0.0,
            y2: 0.0,
            color: 7,
            layer: String::from("0"),
        }
    }
}

impl DxfLine {
    /// Creates a new DxfLine with the specified coordinates
    pub fn new(x1: f64, y1: f64, x2: f64, y2: f64) -> Self {
        Self {
            x1,
            y1,
            x2,
            y2,
            ..Default::default()
        }
    }

    /// Creates a new DxfLine with all parameters
    pub fn with_style(x1: f64, y1: f64, x2: f64, y2: f64, color: i32, layer: &str) -> Self {
        Self {
            x1,
            y1,
            x2,
            y2,
            color,
            layer: layer.to_string(),
        }
    }
}

/// DXF TEXT entity
///
/// Represents a text element with position, content, and styling.
#[derive(Clone, Debug, PartialEq)]
pub struct DxfText {
    /// X coordinate of the text position
    pub x: f64,
    /// Y coordinate of the text position
    pub y: f64,
    /// Text content
    pub text: String,
    /// Text height (default: 1.0)
    pub height: f64,
    /// Rotation angle in degrees (default: 0.0)
    pub rotation: f64,
    /// Color number (default: 7 = white)
    pub color: i32,
    /// Horizontal alignment (0=left, 1=center, 2=right)
    pub align_h: i32,
    /// Vertical alignment (0=baseline, 1=bottom, 2=middle, 3=top)
    pub align_v: i32,
    /// Layer name (default: "0")
    pub layer: String,
}

impl Default for DxfText {
    fn default() -> Self {
        Self {
            x: 0.0,
            y: 0.0,
            text: String::new(),
            height: 1.0,
            rotation: 0.0,
            color: 7,
            align_h: 0,
            align_v: 0,
            layer: String::from("0"),
        }
    }
}

impl DxfText {
    /// Creates a new DxfText with position and content
    pub fn new(x: f64, y: f64, text: &str) -> Self {
        Self {
            x,
            y,
            text: text.to_string(),
            ..Default::default()
        }
    }

    /// Creates a new DxfText with all parameters
    #[allow(clippy::too_many_arguments)]
    pub fn with_style(
        x: f64,
        y: f64,
        text: &str,
        height: f64,
        rotation: f64,
        color: i32,
        align_h: i32,
        align_v: i32,
        layer: &str,
    ) -> Self {
        Self {
            x,
            y,
            text: text.to_string(),
            height,
            rotation,
            color,
            align_h,
            align_v,
            layer: layer.to_string(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_dxf_line_default() {
        let line = DxfLine::default();
        assert_eq!(line.x1, 0.0);
        assert_eq!(line.y1, 0.0);
        assert_eq!(line.x2, 0.0);
        assert_eq!(line.y2, 0.0);
        assert_eq!(line.color, 7);
        assert_eq!(line.layer, "0");
    }

    #[test]
    fn test_dxf_line_new() {
        let line = DxfLine::new(1.0, 2.0, 3.0, 4.0);
        assert_eq!(line.x1, 1.0);
        assert_eq!(line.y1, 2.0);
        assert_eq!(line.x2, 3.0);
        assert_eq!(line.y2, 4.0);
        assert_eq!(line.color, 7);
        assert_eq!(line.layer, "0");
    }

    #[test]
    fn test_dxf_line_with_style() {
        let line = DxfLine::with_style(1.0, 2.0, 3.0, 4.0, 5, "Layer1");
        assert_eq!(line.x1, 1.0);
        assert_eq!(line.y1, 2.0);
        assert_eq!(line.x2, 3.0);
        assert_eq!(line.y2, 4.0);
        assert_eq!(line.color, 5);
        assert_eq!(line.layer, "Layer1");
    }

    #[test]
    fn test_dxf_text_default() {
        let text = DxfText::default();
        assert_eq!(text.x, 0.0);
        assert_eq!(text.y, 0.0);
        assert_eq!(text.text, "");
        assert_eq!(text.height, 1.0);
        assert_eq!(text.rotation, 0.0);
        assert_eq!(text.color, 7);
        assert_eq!(text.align_h, 0);
        assert_eq!(text.align_v, 0);
        assert_eq!(text.layer, "0");
    }

    #[test]
    fn test_dxf_text_new() {
        let text = DxfText::new(10.0, 20.0, "Hello");
        assert_eq!(text.x, 10.0);
        assert_eq!(text.y, 20.0);
        assert_eq!(text.text, "Hello");
        assert_eq!(text.height, 1.0);
    }

    #[test]
    fn test_dxf_text_with_style() {
        let text = DxfText::with_style(10.0, 20.0, "Hello", 2.5, 45.0, 3, 1, 2, "TextLayer");
        assert_eq!(text.x, 10.0);
        assert_eq!(text.y, 20.0);
        assert_eq!(text.text, "Hello");
        assert_eq!(text.height, 2.5);
        assert_eq!(text.rotation, 45.0);
        assert_eq!(text.color, 3);
        assert_eq!(text.align_h, 1);
        assert_eq!(text.align_v, 2);
        assert_eq!(text.layer, "TextLayer");
    }

    #[test]
    fn test_dxf_line_clone() {
        let line = DxfLine::new(1.0, 2.0, 3.0, 4.0);
        let cloned = line.clone();
        assert_eq!(line, cloned);
    }

    #[test]
    fn test_dxf_text_clone() {
        let text = DxfText::new(10.0, 20.0, "Test");
        let cloned = text.clone();
        assert_eq!(text, cloned);
    }
}
