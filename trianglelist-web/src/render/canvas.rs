//! Canvas rendering for triangles
//!
//! Provides functions for drawing triangles, handling pan/zoom, and viewport management

use eframe::egui::{Color32, Painter, Pos2, Shape, Stroke, Vec2};
use crate::csv::ParsedTriangle;

/// View state for pan and zoom operations
#[derive(Clone, Debug)]
pub struct ViewState {
    /// Pan offset (translation)
    pub pan: Pos2,
    /// Zoom level (1.0 = 100%)
    pub zoom: f32,
    /// Canvas size
    pub canvas_size: Vec2,
}

impl Default for ViewState {
    fn default() -> Self {
        Self {
            pan: Pos2::new(0.0, 0.0),
            zoom: 1.0,
            canvas_size: eframe::egui::Vec2::new(800.0, 600.0),
        }
    }
}

impl ViewState {
    /// Convert model coordinates to screen coordinates
    pub fn model_to_screen(&self, model_pos: Pos2) -> Pos2 {
        Pos2::new(
            model_pos.x * self.zoom + self.pan.x,
            model_pos.y * self.zoom + self.pan.y,
        )
    }

    /// Convert screen coordinates to model coordinates
    pub fn screen_to_model(&self, screen_pos: Pos2) -> Pos2 {
        Pos2::new(
            (screen_pos.x - self.pan.x) / self.zoom,
            (screen_pos.y - self.pan.y) / self.zoom,
        )
    }

    /// Fit view to show all triangles
    pub fn fit_to_triangles(&mut self, triangles: &[ParsedTriangle]) {
        if triangles.is_empty() {
            return;
        }

        // Calculate bounding box (simplified - using side lengths as approximation)
        let mut min_x = f64::MAX;
        let mut min_y = f64::MAX;
        let mut max_x = f64::MIN;
        let mut max_y = f64::MIN;

        // Approximate triangle positions (simplified calculation)
        let mut current_x = 0.0;
        let current_y = 0.0;
        
        for triangle in triangles {
            // Simple approximation: place triangles in a row
            min_x = min_x.min(current_x);
            min_y = min_y.min(current_y);
            max_x = max_x.max(current_x + triangle.side_a);
            max_y = max_y.max(current_y + triangle.side_b);
            
            current_x += triangle.side_a * 1.2; // Add spacing
        }

        let width = max_x - min_x;
        let height = max_y - min_y;

        if width > 0.0 && height > 0.0 {
            // Calculate zoom to fit
            let zoom_x = self.canvas_size.x as f64 / width;
            let zoom_y = self.canvas_size.y as f64 / height;
            self.zoom = (zoom_x.min(zoom_y) * 0.9) as f32; // 90% to add padding

            // Center the view
            let center_x = (min_x + max_x) / 2.0;
            let center_y = (min_y + max_y) / 2.0;
            self.pan = Pos2::new(
                (self.canvas_size.x / 2.0) - (center_x * self.zoom as f64) as f32,
                (self.canvas_size.y / 2.0) - (center_y * self.zoom as f64) as f32,
            );
        }
    }
}

/// Draws a triangle polygon
/// 
/// # Arguments
/// * `painter` - egui Painter
/// * `points` - Three vertices of the triangle
/// * `fill_color` - Fill color
/// * `stroke_color` - Stroke color
/// * `stroke_width` - Stroke width in pixels
pub fn draw_triangle(
    painter: &Painter,
    points: [Pos2; 3],
    fill_color: Color32,
    stroke_color: Color32,
    stroke_width: f32,
) {
    // Create triangle shape
    let triangle_shape = Shape::convex_polygon(
        points.to_vec(),
        fill_color,
        Stroke::new(stroke_width, stroke_color),
    );
    
    painter.add(triangle_shape);
}

/// Draws triangle number text
/// 
/// # Arguments
/// * `painter` - egui Painter
/// * `center` - Center position of the triangle
/// * `number` - Triangle number
/// * `text_height` - Text height in pixels
/// * `color` - Text color
pub fn draw_triangle_number(
    painter: &Painter,
    center: Pos2,
    number: i32,
    text_height: f32,
    color: Color32,
) {
    use crate::render::text::{draw_text_cad_style, TextAlign, HorizontalAlign, VerticalAlign};
    
    let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle);
    draw_text_cad_style(
        painter,
        &number.to_string(),
        center,
        text_height,
        0.0,
        color,
        align,
    );
}

/// Draws side length text
/// 
/// # Arguments
/// * `painter` - egui Painter
/// * `pos` - Position for the text
/// * `length` - Side length value
/// * `text_height` - Text height in pixels
/// * `color` - Text color
pub fn draw_side_length(
    painter: &Painter,
    pos: Pos2,
    length: f64,
    text_height: f32,
    color: Color32,
) {
    use crate::render::text::{draw_text_cad_style, TextAlign, HorizontalAlign, VerticalAlign};
    
    let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Bottom);
    let text = format!("{:.2}", length);
    draw_text_cad_style(
        painter,
        &text,
        pos,
        text_height,
        0.0,
        color,
        align,
    );
}

