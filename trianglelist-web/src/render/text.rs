//! CAD-compatible text rendering
//!
//! Provides text drawing functions with DXF-compatible alignment and rotation

use eframe::egui::{Color32, Painter, Pos2};

/// Horizontal text alignment (DXF compatible)
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum HorizontalAlign {
    Left = 0,
    Center = 1,
    Right = 2,
}

/// Vertical text alignment (DXF compatible)
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum VerticalAlign {
    Baseline = 0,
    Bottom = 1,
    Middle = 2,
    Top = 3,
}

/// Text alignment configuration
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct TextAlign {
    pub horizontal: HorizontalAlign,
    pub vertical: VerticalAlign,
}

impl TextAlign {
    pub fn new(horizontal: HorizontalAlign, vertical: VerticalAlign) -> Self {
        Self { horizontal, vertical }
    }

    /// Create from DXF alignment values
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
}

/// Draws text with CAD-compatible alignment
///
/// Note: egui does not support text rotation. Text is always drawn horizontally.
/// For rotated text, use Canvas 2D API (text_canvas2d.rs) instead.
///
/// # Arguments
/// * `painter` - egui Painter
/// * `text` - Text string to draw
/// * `pos` - Base position (DXF alignment point)
/// * `height` - Text height in pixels
/// * `_rotation_degrees` - Ignored (egui doesn't support rotation)
/// * `color` - Text color
/// * `align` - Text alignment
pub fn draw_text_cad_style(
    painter: &Painter,
    text: &str,
    pos: Pos2,
    height: f32,
    _rotation_degrees: f32,  // Ignored - egui doesn't support text rotation
    color: Color32,
    align: TextAlign,
) {
    // Convert alignment to egui's Align2
    let align2 = egui::Align2([
        match align.horizontal {
            HorizontalAlign::Left => egui::Align::LEFT,
            HorizontalAlign::Center => egui::Align::Center,
            HorizontalAlign::Right => egui::Align::RIGHT,
        },
        match align.vertical {
            VerticalAlign::Top => egui::Align::TOP,
            VerticalAlign::Middle => egui::Align::Center,
            VerticalAlign::Bottom | VerticalAlign::Baseline => egui::Align::BOTTOM,
        },
    ]);

    painter.text(
        pos,
        align2,
        text,
        egui::FontId::monospace(height),
        color,
    );
}

