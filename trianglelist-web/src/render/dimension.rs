//! Dimension value rendering for triangles
//!
//! Draws dimension values along triangle edges.
//! Based on Android version: text is drawn along the edge path with offset,
//! no extension lines or dimension auxiliary lines.

use eframe::egui::{Color32, Painter, Pos2, Vec2};
use crate::render::canvas::ViewState;
use crate::render::text::{draw_text_cad_style, TextAlign, HorizontalAlign, VerticalAlign};

/// Dimension style configuration
#[derive(Debug, Clone)]
pub struct DimensionStyle {
    /// Text color
    pub text_color: Color32,
    /// Text height in screen pixels
    pub text_height: f32,
    /// Vertical offset from edge (positive = outside, negative = inside)
    pub offset_v: f32,
}

impl Default for DimensionStyle {
    fn default() -> Self {
        Self {
            text_color: Color32::from_rgb(255, 255, 255),
            text_height: 12.0,
            offset_v: 12.0, // Outside by default
        }
    }
}

/// Draws a dimension value along a triangle edge
///
/// Text is positioned at the midpoint of the edge with vertical offset.
/// Text direction is automatically adjusted to be readable (left to right).
///
/// # Arguments
/// * `painter` - egui Painter
/// * `start` - Start point of the edge (screen coordinates)
/// * `end` - End point of the edge (screen coordinates)
/// * `value` - Dimension value to display
/// * `_view_state` - View state (reserved for future use)
/// * `style` - Dimension style configuration
pub fn draw_dimension_value(
    painter: &Painter,
    start: Pos2,
    end: Pos2,
    value: f64,
    _view_state: &ViewState,
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

    // Calculate text position (midpoint + offset)
    let midpoint = Pos2::new(
        (start.x + end.x) * 0.5,
        (start.y + end.y) * 0.5,
    );
    let text_pos = midpoint + perpendicular * style.offset_v;

    // Calculate rotation angle
    let mut angle_rad = direction.y.atan2(direction.x);

    // Auto-adjust text direction to be readable (left to right)
    // If text would be upside down, flip it 180 degrees
    if angle_rad > std::f32::consts::FRAC_PI_2 || angle_rad < -std::f32::consts::FRAC_PI_2 {
        angle_rad += std::f32::consts::PI;
    }
    let angle_degrees = angle_rad.to_degrees();

    // Format dimension value
    let text = format!("{:.2}", value);

    // Draw dimension text
    let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle);
    draw_text_cad_style(
        painter,
        &text,
        text_pos,
        style.text_height,
        angle_degrees,
        style.text_color,
        align,
    );
}

/// Draws dimension lines for all three sides of a triangle
///
/// # Arguments
/// * `painter` - egui Painter
/// * `points` - Three vertices of the triangle (screen coordinates): [CA, AB, BC]
/// * `side_lengths` - Lengths of the three sides [A, B, C]
/// * `view_state` - View state for coordinate transformations
/// * `style` - Dimension style configuration
/// * `skip_side_a` - If true, skip drawing dimension for side A (connected edge)
pub fn draw_triangle_dimensions(
    painter: &Painter,
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
            painter,
            points[0],
            points[1],
            side_lengths[0],
            view_state,
            style,
        );
    }

    // Side B: AB to BC (points[1] to points[2])
    draw_dimension_value(
        painter,
        points[1],
        points[2],
        side_lengths[1],
        view_state,
        style,
    );

    // Side C: BC to CA (points[2] to points[0])
    draw_dimension_value(
        painter,
        points[2],
        points[0],
        side_lengths[2],
        view_state,
        style,
    );
}
