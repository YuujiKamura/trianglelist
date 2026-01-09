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
    // Get text layout
    let galley = painter.layout_no_wrap(
        text.to_string(),
        egui::FontId::monospace(height),
        color,
    );

    let text_width = galley.size().x;
    let text_height = galley.size().y;

    // Calculate offset based on alignment (relative to pos)
    let offset_x = match align.horizontal {
        HorizontalAlign::Left => 0.0,
        HorizontalAlign::Center => -text_width / 2.0,
        HorizontalAlign::Right => -text_width,
    };
    let offset_y = match align.vertical {
        VerticalAlign::Top => 0.0,
        VerticalAlign::Middle => -text_height / 2.0,
        VerticalAlign::Bottom | VerticalAlign::Baseline => -text_height,
    };

    if rotation_degrees.abs() > 0.1 {
        // Rotated text: draw each character individually at rotated positions
        let rotation_rad = rotation_degrees.to_radians();
        let cos_r = rotation_rad.cos();
        let sin_r = rotation_rad.sin();

        // Draw each character at rotated position
        let mut char_x = 0.0f32;
        for ch in text.chars() {
            let ch_str = ch.to_string();
            let ch_galley = painter.layout_no_wrap(
                ch_str.clone(),
                egui::FontId::monospace(height),
                color,
            );
            let char_width = ch_galley.size().x;

            // Calculate character position relative to alignment point
            let rel_x = offset_x + char_x + char_width / 2.0;
            let rel_y = offset_y + text_height / 2.0;

            // Rotate around pos
            let rotated_x = pos.x + rel_x * cos_r - rel_y * sin_r;
            let rotated_y = pos.y + rel_x * sin_r + rel_y * cos_r;

            // Draw character centered at rotated position
            painter.text(
                Pos2::new(rotated_x, rotated_y),
                egui::Align2::CENTER_CENTER,
                &ch_str,
                egui::FontId::monospace(height),
                color,
            );

            char_x += char_width;
        }
    } else {
        // No rotation: use standard text drawing with alignment
        let text_pos = Pos2::new(pos.x + offset_x, pos.y + offset_y);
        painter.text(
            text_pos,
            egui::Align2::LEFT_TOP,
            text,
            egui::FontId::monospace(height),
            color,
        );
    }
}

