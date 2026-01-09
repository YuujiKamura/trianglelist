//! Road section Canvas rendering
//!
//! Provides functions for drawing road section geometry

use eframe::egui::{Color32, Painter, Pos2, Stroke};
use crate::road_section::RoadSectionGeometry;
use super::canvas::ViewState;
use super::text_canvas2d::Canvas2dTextRenderer;
use super::text::{TextAlign, HorizontalAlign, VerticalAlign};

/// Default colors for road section rendering
pub const ROAD_LINE_COLOR: Color32 = Color32::from_rgb(200, 200, 200);
pub const ROAD_CENTER_COLOR: Color32 = Color32::from_rgb(255, 100, 100);
pub const ROAD_TEXT_COLOR: Color32 = Color32::WHITE;
pub const ROAD_LABEL_COLOR: Color32 = Color32::from_rgb(100, 150, 255); // Blue

/// Draw road section geometry using egui Painter
pub fn draw_road_section_egui(
    painter: &Painter,
    geometry: &RoadSectionGeometry,
    view_state: &ViewState,
) {
    use super::text::draw_text_cad_style;

    // Draw lines
    for line in &geometry.lines {
        let p1 = view_state.model_to_screen(Pos2::new(line.x1 as f32, line.y1 as f32));
        let p2 = view_state.model_to_screen(Pos2::new(line.x2 as f32, line.y2 as f32));

        let color = dxf_color_to_egui(line.color);
        painter.line_segment([p1, p2], Stroke::new(1.0, color));
    }

    // Screen-pixel offset for dimension text (same as triangle dimensions)
    const TEXT_OFFSET_PIXELS: f32 = 15.0;

    // Draw texts using the same style as triangles (egui, no rotation)
    for text in &geometry.texts {
        let base_pos = view_state.model_to_screen(Pos2::new(text.x as f32, text.y as f32));
        let color = dxf_color_to_egui(text.color);

        // Calculate perpendicular offset (egui doesn't support rotation, but still apply offset)
        let rotation_rad = (-text.rotation as f32).to_radians();
        let offset_x = -rotation_rad.sin() * TEXT_OFFSET_PIXELS;
        let offset_y = rotation_rad.cos() * TEXT_OFFSET_PIXELS;
        let pos = Pos2::new(base_pos.x + offset_x, base_pos.y + offset_y);

        // Convert DXF alignment to TextAlign
        let align = TextAlign::new(
            match text.align_h {
                dxf::HorizontalAlignment::Left => HorizontalAlign::Left,
                dxf::HorizontalAlignment::Center => HorizontalAlign::Center,
                dxf::HorizontalAlignment::Right => HorizontalAlign::Right,
            },
            match text.align_v {
                dxf::VerticalAlignment::Top => VerticalAlign::Top,
                dxf::VerticalAlignment::Middle => VerticalAlign::Middle,
                dxf::VerticalAlignment::Bottom | dxf::VerticalAlignment::Baseline => VerticalAlign::Bottom,
            },
        );

        // Scale font size with zoom (model-coordinate based text height)
        let font_size = 14.0 * view_state.zoom;
        let font_size = font_size.max(8.0).min(48.0); // Clamp to readable range

        draw_text_cad_style(
            painter,
            &text.text,
            pos,
            font_size,
            text.rotation as f32, // ignored by egui
            color,
            align,
        );
    }
}

/// Draw road section geometry using Canvas 2D API (with text rotation)
pub fn draw_road_section_canvas2d(
    painter: &Painter,
    renderer: &Canvas2dTextRenderer,
    geometry: &RoadSectionGeometry,
    view_state: &ViewState,
) {
    // Draw lines using egui Painter
    for line in &geometry.lines {
        let p1 = view_state.model_to_screen(Pos2::new(line.x1 as f32, line.y1 as f32));
        let p2 = view_state.model_to_screen(Pos2::new(line.x2 as f32, line.y2 as f32));

        let color = dxf_color_to_egui(line.color);
        painter.line_segment([p1, p2], Stroke::new(1.0, color));
    }

    // Screen-pixel offset for dimension text (same as triangle dimensions)
    const TEXT_OFFSET_PIXELS: f32 = 15.0;

    // Draw texts using Canvas 2D API
    for text in &geometry.texts {
        let base_pos = view_state.model_to_screen(Pos2::new(text.x as f32, text.y as f32));
        let color_css = dxf_color_to_css(text.color);

        // Calculate rotation: negate DXF rotation (DXF is counterclockwise, Canvas 2D is clockwise)
        // then add view rotation
        let total_rotation = -text.rotation + view_state.rotation.to_degrees() as f64;
        let rotation_rad = total_rotation.to_radians() as f32;

        // Calculate perpendicular offset direction (90 degrees from text direction)
        // For vertical text (-90 deg = 90 deg after negation), perpendicular is horizontal
        let offset_x = -rotation_rad.sin() * TEXT_OFFSET_PIXELS;
        let offset_y = rotation_rad.cos() * TEXT_OFFSET_PIXELS;
        let pos = Pos2::new(base_pos.x + offset_x, base_pos.y + offset_y);

        // Convert DXF alignment to TextAlign
        let align = TextAlign::new(
            match text.align_h {
                dxf::HorizontalAlignment::Left => HorizontalAlign::Left,
                dxf::HorizontalAlignment::Center => HorizontalAlign::Center,
                dxf::HorizontalAlignment::Right => HorizontalAlign::Right,
            },
            match text.align_v {
                dxf::VerticalAlignment::Top => VerticalAlign::Top,
                dxf::VerticalAlignment::Middle => VerticalAlign::Middle,
                dxf::VerticalAlignment::Bottom | dxf::VerticalAlignment::Baseline => VerticalAlign::Bottom,
            },
        );

        // Scale font size with zoom (model-coordinate based text height)
        let font_size = 14.0 * view_state.zoom as f64;
        let font_size = font_size.max(8.0).min(48.0); // Clamp to readable range

        let _ = renderer.draw_text(
            &text.text,
            pos.x as f64,
            pos.y as f64,
            font_size,
            total_rotation,
            align,
            &color_css,
        );
    }
}

/// Convert DXF color index to egui Color32
fn dxf_color_to_egui(color: i32) -> Color32 {
    match color {
        1 => Color32::from_rgb(255, 0, 0),     // Red
        2 => Color32::from_rgb(255, 255, 0),   // Yellow
        3 => Color32::from_rgb(0, 255, 0),     // Green
        4 => Color32::from_rgb(0, 255, 255),   // Cyan
        5 => Color32::from_rgb(100, 150, 255), // Blue
        6 => Color32::from_rgb(255, 0, 255),   // Magenta
        7 | 0 => Color32::WHITE,               // White (default)
        8 => Color32::from_rgb(128, 128, 128), // Gray
        _ => Color32::WHITE,
    }
}

/// Convert DXF color index to CSS color string
fn dxf_color_to_css(color: i32) -> String {
    match color {
        1 => "#ff0000".to_string(),   // Red
        2 => "#ffff00".to_string(),   // Yellow
        3 => "#00ff00".to_string(),   // Green
        4 => "#00ffff".to_string(),   // Cyan
        5 => "#6496ff".to_string(),   // Blue
        6 => "#ff00ff".to_string(),   // Magenta
        7 | 0 => "#ffffff".to_string(), // White (default)
        8 => "#808080".to_string(),   // Gray
        _ => "#ffffff".to_string(),
    }
}

/// Calculate bounding box for road section geometry
pub fn calculate_road_section_bounds(geometry: &RoadSectionGeometry) -> (f32, f32, f32, f32) {
    let mut min_x = f32::MAX;
    let mut min_y = f32::MAX;
    let mut max_x = f32::MIN;
    let mut max_y = f32::MIN;

    for line in &geometry.lines {
        min_x = min_x.min(line.x1 as f32).min(line.x2 as f32);
        min_y = min_y.min(line.y1 as f32).min(line.y2 as f32);
        max_x = max_x.max(line.x1 as f32).max(line.x2 as f32);
        max_y = max_y.max(line.y1 as f32).max(line.y2 as f32);
    }

    for text in &geometry.texts {
        min_x = min_x.min(text.x as f32);
        min_y = min_y.min(text.y as f32);
        max_x = max_x.max(text.x as f32);
        max_y = max_y.max(text.y as f32);
    }

    if min_x == f32::MAX {
        (0.0, 0.0, 100.0, 100.0)
    } else {
        (min_x, min_y, max_x, max_y)
    }
}

impl ViewState {
    /// Fit view to show road section geometry
    pub fn fit_to_road_section(&mut self, geometry: &RoadSectionGeometry) {
        let (min_x, min_y, max_x, max_y) = calculate_road_section_bounds(geometry);

        let content_width = max_x - min_x;
        let content_height = max_y - min_y;

        if content_width <= 0.0 || content_height <= 0.0 {
            return;
        }

        // Calculate zoom to fit content with padding
        let padding = 0.9;
        let zoom_x = self.canvas_size.x * padding / content_width;
        let zoom_y = self.canvas_size.y * padding / content_height;
        self.zoom = zoom_x.min(zoom_y);

        // Center the content
        let center_x = (min_x + max_x) / 2.0;
        let center_y = (min_y + max_y) / 2.0;

        self.pan = Pos2::new(
            self.canvas_size.x / 2.0 - center_x * self.zoom,
            self.canvas_size.y / 2.0 - center_y * self.zoom,
        );

        // Reset rotation
        self.rotation = 0.0;
    }
}
