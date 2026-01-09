//! CAD-compatible text rendering
//!
//! Provides text drawing functions with DXF-compatible alignment

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

/// Draws text with CAD-compatible alignment and rotation
/// 
/// # Arguments
/// * `painter` - egui Painter
/// * `text` - Text string to draw
/// * `pos` - Base position (DXF alignment point)
/// * `height` - Text height in pixels (CAD text height)
/// * `rotation_degrees` - Rotation angle in degrees (0 = horizontal)
/// * `color` - Text color
/// * `align` - Text alignment
pub fn draw_text_cad_style(
    painter: &Painter,
    text: &str,
    pos: Pos2,
    height: f32,
    rotation_degrees: f32,
    color: Color32,
    align: TextAlign,
) {
    // Note: egui doesn't have built-in text rotation. This is a placeholder.
    if rotation_degrees.abs() > 1e-3 {
        painter.text(
            pos,
            egui::Align2::CENTER_CENTER,
            text,
            egui::FontId::monospace(height),
            color,
        );
        return;
    }

    // Convert CAD alignment to egui's Align2.
    let align2 = egui::Align2([
        match align.horizontal {
            HorizontalAlign::Left => egui::Align::LEFT,
            HorizontalAlign::Center => egui::Align::Center,
            HorizontalAlign::Right => egui::Align::RIGHT,
        },
        match align.vertical {
            VerticalAlign::Top => egui::Align::TOP,
            VerticalAlign::Middle => egui::Align::Center,
            VerticalAlign::Bottom => egui::Align::BOTTOM,
            // egui has no baseline, use bottom as an approximation.
            VerticalAlign::Baseline => egui::Align::BOTTOM,
        },
    ]);

    painter.text(pos, align2, text, egui::FontId::monospace(height), color);
}

