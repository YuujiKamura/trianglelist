//! Dimension value rendering for triangles
//!
//! Draws dimension values along triangle edges.
//! Based on Android version: text is drawn along the edge path with offset,
//! no extension lines or dimension auxiliary lines.

use eframe::egui::{Pos2, Vec2};
use crate::render::canvas::ViewState;
use crate::render::text::{TextAlign, HorizontalAlign, VerticalAlign};
use crate::render::text_canvas2d::Canvas2dTextRenderer;
use wasm_bindgen::JsValue;

/// Dimension style configuration
#[derive(Debug, Clone)]
pub struct DimensionStyle {
    /// Text color (CSS format)
    pub text_color: String,
    /// Text height in model units
    pub text_height: f64,
    /// Vertical offset from edge in model units (positive = outside)
    pub offset_v: f64,
}

impl Default for DimensionStyle {
    fn default() -> Self {
        Self {
            text_color: "white".to_string(),
            text_height: 3.0,  // Model units
            offset_v: 2.0,     // Model units
        }
    }
}

/// Draws a dimension value along a triangle edge using Canvas 2D API
///
/// Text is positioned at the midpoint of the edge with vertical offset.
/// Text direction is automatically adjusted to be readable (left to right).
///
/// # Arguments
/// * `renderer` - Canvas 2D text renderer
/// * `start` - Start point of the edge (model coordinates)
/// * `end` - End point of the edge (model coordinates)
/// * `value` - Dimension value to display
/// * `view_state` - View state for coordinate transformations
/// * `style` - Dimension style configuration
pub fn draw_dimension_value(
    renderer: &Canvas2dTextRenderer,
    start: Pos2,
    end: Pos2,
    value: f64,
    view_state: &ViewState,
    style: &DimensionStyle,
) {
    // Calculate edge direction
    let vec = end - start;
    let length = vec.length();
    if length < 0.01 {
        return; // Skip zero-length edges
    }
    let direction = vec / length;

    // Calculate perpendicular vector for offset
    let perpendicular = Vec2::new(-direction.y, direction.x);

    // Calculate text position (midpoint + offset) in model coordinates
    let midpoint = Pos2::new(
        (start.x + end.x) * 0.5,
        (start.y + end.y) * 0.5,
    );
    let text_pos = midpoint + perpendicular * style.offset_v as f32;

    // Calculate rotation angle
    let mut angle_rad = direction.y.atan2(direction.x);

    // Auto-adjust text direction to be readable (left to right)
    // If text would be upside down, flip it 180 degrees
    if angle_rad > std::f32::consts::FRAC_PI_2 || angle_rad < -std::f32::consts::FRAC_PI_2 {
        angle_rad += std::f32::consts::PI;
    }
    let angle_degrees = angle_rad.to_degrees() as f64;

    // Format dimension value
    let text = format!("{:.2}", value);

    #[cfg(target_arch = "wasm32")]
    web_sys::console::log_1(&JsValue::from_str(&format!(
        "Drawing dimension: {} at ({:.1}, {:.1}), angle: {:.1}, zoom: {}",
        text, text_pos.x, text_pos.y, angle_degrees, view_state.zoom
    )));

    // Draw dimension text using Canvas 2D API (supports rotation)
    let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle);
    let _ = renderer.draw_text_cad_style(
        &text,
        text_pos.x as f64,
        text_pos.y as f64,
        style.text_height,
        angle_degrees,
        align,
        &style.text_color,
        view_state,
        true, // Zoom-dependent size
    );
}

/// Draws dimension values for all three sides of a triangle
///
/// # Arguments
/// * `renderer` - Canvas 2D text renderer
/// * `points` - Three vertices of the triangle (model coordinates): [CA, AB, BC]
/// * `side_lengths` - Lengths of the three sides [A, B, C]
/// * `view_state` - View state for coordinate transformations
/// * `style` - Dimension style configuration
/// * `skip_side_a` - If true, skip drawing dimension for side A (connected edge)
pub fn draw_triangle_dimensions(
    renderer: &Canvas2dTextRenderer,
    points: [Pos2; 3],
    side_lengths: [f64; 3],
    view_state: &ViewState,
    style: &DimensionStyle,
    skip_side_a: bool,
) {
    // Side A: CA to AB (points[0] to points[1])
    // Skip if this is a connected edge (shared with parent)
    if !skip_side_a {
        draw_dimension_value(
            renderer,
            points[0],
            points[1],
            side_lengths[0],
            view_state,
            style,
        );
    }

    // Side B: AB to BC (points[1] to points[2])
    draw_dimension_value(
        renderer,
        points[1],
        points[2],
        side_lengths[1],
        view_state,
        style,
    );

    // Side C: BC to CA (points[2] to points[0])
    draw_dimension_value(
        renderer,
        points[2],
        points[0],
        side_lengths[2],
        view_state,
        style,
    );
}
