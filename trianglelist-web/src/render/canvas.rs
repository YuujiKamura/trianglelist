//! Canvas rendering for triangles
//!
//! Provides functions for drawing triangles, handling pan/zoom, and viewport management

use eframe::egui::{Color32, Painter, Pos2, Shape, Stroke, Vec2};
use crate::csv::ParsedTriangle;

/// Calculate triangle vertex positions from side lengths
/// 
/// Returns three points: [p1, p2, p3]
/// - p1: Origin (0, 0)
/// - p2: On X-axis at distance side_c
/// - p3: Calculated using cosine rule
pub fn calculate_triangle_points(triangle: &ParsedTriangle) -> [Pos2; 3] {
    if !triangle.is_valid_triangle() {
        return [Pos2::ZERO; 3];
    }

    let a = triangle.side_a as f32;
    let b = triangle.side_b as f32;
    let c = triangle.side_c as f32;

    // Point 1: Origin
    let p1 = Pos2::new(0.0, 0.0);

    // Point 2: On X-axis at distance c
    let p2 = Pos2::new(c, 0.0);

    // Point 3: Calculate using cosine rule
    // cos(A) = (b² + c² - a²) / (2bc)
    let cos_a = (b * b + c * c - a * a) / (2.0 * b * c);
    let cos_a_clamped = cos_a.clamp(-1.0, 1.0);
    let sin_a = (1.0 - cos_a_clamped * cos_a_clamped).sqrt();

    let p3 = Pos2::new(b * cos_a_clamped, b * sin_a);

    [p1, p2, p3]
}

/// Padding factor for fit_to_triangles (90% of available space)
const PADDING_FACTOR: f32 = 0.9;

/// View state for pan and zoom operations
#[derive(Clone, Debug)]
pub struct ViewState {
    /// Pan offset (translation)
    pub pan: Pos2,
    /// Zoom level (1.0 = 100%)
    pub zoom: f32,
    /// Canvas size (should be updated from UI when available)
    pub canvas_size: Vec2,
}

impl Default for ViewState {
    fn default() -> Self {
        Self {
            pan: Pos2::new(0.0, 0.0),
            zoom: 1.0,
            // Default size - should be updated from actual canvas size when available
            canvas_size: Vec2::new(800.0, 600.0),
        }
    }
}

impl ViewState {
    /// Update canvas size from UI
    pub fn set_canvas_size(&mut self, size: Vec2) {
        self.canvas_size = size;
    }
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
    /// 
    /// Note: This is a simplified implementation. For accurate results,
    /// ensure canvas_size is set to the actual canvas dimensions before calling.
    pub fn fit_to_triangles(&mut self, triangles: &[ParsedTriangle]) {
        if triangles.is_empty() {
            return;
        }

        // Calculate bounding box (simplified - using side lengths as approximation)
        // Convert to f32 early to avoid type mixing
        let mut min_x = f32::MAX;
        let mut min_y = f32::MAX;
        let mut max_x = f32::MIN;
        let mut max_y = f32::MIN;

        // Approximate triangle positions (simplified calculation)
        let mut current_x = 0.0f32;
        let current_y = 0.0f32;
        
        for triangle in triangles {
            // Simple approximation: place triangles in a row
            let side_a = triangle.side_a as f32;
            let side_b = triangle.side_b as f32;
            
            min_x = min_x.min(current_x);
            min_y = min_y.min(current_y);
            max_x = max_x.max(current_x + side_a);
            max_y = max_y.max(current_y + side_b);
            
            current_x += side_a * 1.2; // Add spacing
        }

        let width = max_x - min_x;
        let height = max_y - min_y;

        if width > 0.0 && height > 0.0 {
            // Calculate zoom to fit with padding
            let zoom_x = self.canvas_size.x / width;
            let zoom_y = self.canvas_size.y / height;
            self.zoom = zoom_x.min(zoom_y) * PADDING_FACTOR;

            // Center the view
            let center_x = (min_x + max_x) / 2.0;
            let center_y = (min_y + max_y) / 2.0;
            self.pan = Pos2::new(
                self.canvas_size.x / 2.0 - center_x * self.zoom,
                self.canvas_size.y / 2.0 - center_y * self.zoom,
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

