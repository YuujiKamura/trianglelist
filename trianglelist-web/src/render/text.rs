//! CAD-compatible text rendering
//!
//! Provides text drawing functions with DXF-compatible alignment

use eframe::egui::{Color32, Painter, Pos2, Vec2};
use std::f32::consts::PI;

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
    // Calculate text bounds (approximate)
    let text_width = text.len() as f32 * height * 0.6; // Approximate character width
    let text_height = height;

    // Calculate offset based on alignment
    let (x_offset, y_offset) = align.calc_offset(text_width, text_height);

    // Apply rotation if needed
    if rotation_degrees != 0.0 {
        let radians = rotation_degrees * PI / 180.0;
        let cos_r = radians.cos();
        let sin_r = radians.sin();
        
        // Rotate the offset around the base position
        let rotated_x = x_offset * cos_r - y_offset * sin_r;
        let rotated_y = x_offset * sin_r + y_offset * cos_r;
        
        let final_pos = Pos2::new(pos.x + rotated_x, pos.y + rotated_y);
        
        // Draw rotated text (simplified - just draw at rotated position)
        // Note: egui doesn't have built-in text rotation, so we approximate
        painter.text(
            final_pos,
            egui::Align2::CENTER_CENTER,
            text,
            egui::FontId::monospace(height),
            color,
        );
    } else {
        // Draw non-rotated text
        let final_pos = Pos2::new(pos.x + x_offset, pos.y + y_offset);
        let align2 = match (align.horizontal, align.vertical) {
            (HorizontalAlign::Left, VerticalAlign::Top) => egui::Align2::LEFT_TOP,
            (HorizontalAlign::Center, VerticalAlign::Middle) => egui::Align2::CENTER_CENTER,
            (HorizontalAlign::Right, VerticalAlign::Bottom) => egui::Align2::RIGHT_BOTTOM,
            (HorizontalAlign::Left, _) => egui::Align2::LEFT_CENTER,
            (HorizontalAlign::Center, _) => egui::Align2::CENTER_CENTER,
            (HorizontalAlign::Right, _) => egui::Align2::RIGHT_CENTER,
            (_, VerticalAlign::Top) => egui::Align2::CENTER_TOP,
            (_, VerticalAlign::Bottom) => egui::Align2::CENTER_BOTTOM,
            _ => egui::Align2::CENTER_CENTER,
        };
        
        painter.text(final_pos, align2, text, egui::FontId::monospace(height), color);
    }
}

impl TextAlign {
    /// Calculate offset for text position based on alignment
    fn calc_offset(&self, width: f32, height: f32) -> (f32, f32) {
        let x_offset = match self.horizontal {
            HorizontalAlign::Left => 0.0,
            HorizontalAlign::Center => -width / 2.0,
            HorizontalAlign::Right => -width,
        };
        let y_offset = match self.vertical {
            VerticalAlign::Top => -height,
            VerticalAlign::Middle => -height / 2.0,
            VerticalAlign::Bottom => 0.0,
            VerticalAlign::Baseline => height * 0.2, // Approximate baseline offset
        };
        (x_offset, y_offset)
    }
}

