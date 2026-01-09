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
<<<<<<< HEAD
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

    // If rotation is needed, use Shape::Path to draw rotated text
    if rotation_degrees.abs() > 1e-3 {
        // Convert degrees to radians
        let rotation_rad = rotation_degrees.to_radians();
        let cos_r = rotation_rad.cos();
        let sin_r = rotation_rad.sin();
        
        // Get text layout to calculate bounding box
        let galley = painter.layout_no_wrap(
            text.to_string(),
            egui::FontId::monospace(height),
            color,
        );
        
        // Calculate text bounds based on alignment
        let text_width = galley.size().x;
        let text_height = galley.size().y;
        
        // Calculate offset based on alignment
        let offset_x = match align.horizontal {
            HorizontalAlign::Left => 0.0,
            HorizontalAlign::Center => -text_width / 2.0,
            HorizontalAlign::Right => -text_width,
        };
        let offset_y = match align.vertical {
            VerticalAlign::Top => text_height,
            VerticalAlign::Middle => text_height / 2.0,
            VerticalAlign::Bottom => 0.0,
            VerticalAlign::Baseline => 0.0,
        };
        
        // Create rotated text using Path
        // For each glyph, calculate rotated position
        let mut glyphs = Vec::new();
        for row in &galley.rows {
            for glyph in &row.glyphs {
                let glyph_pos = glyph.pos;
                let relative_x = glyph_pos.x + offset_x;
                let relative_y = glyph_pos.y + offset_y;
                
                // Rotate the glyph position around the text position
                let rotated_x = pos.x + relative_x * cos_r - relative_y * sin_r;
                let rotated_y = pos.y + relative_x * sin_r + relative_y * cos_r;
                
                glyphs.push(egui::epaint::text::Glyph {
                    pos: Pos2::new(rotated_x, rotated_y),
                    ..*glyph
                });
            }
        }
        
        // Create a new galley with rotated glyphs
        // Note: This is a simplified approach. For full support, we'd need to
        // create a new Galley with rotated glyphs, which is complex.
        // For now, we'll use a workaround: draw text at rotated position
        // using Shape::Text with a transformed position.
        
        // Workaround: Use painter.with_clip_rect and rotate the entire clip rect
        // This is not ideal, but egui doesn't support text rotation directly.
        // We'll draw the text normally and note that rotation is a limitation.
        
        // For now, draw text without rotation but log a warning
        log::warn!("Text rotation is not fully supported in egui. Drawing text without rotation.");
        painter.text(pos, align2, text, egui::FontId::monospace(height), color);
    } else {
        // No rotation, use standard text drawing
        painter.text(pos, align2, text, egui::FontId::monospace(height), color);
=======
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
>>>>>>> 117a1f878b2ad4e9579df6a8249857d6cdfa0973
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

