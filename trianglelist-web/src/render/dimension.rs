//! Dimension value rendering for triangles
//!
//! Draws dimension values along triangle edges.
//! Based on Android version: text is drawn along the edge path with offset,
//! no extension lines or dimension auxiliary lines.

use eframe::egui::{Pos2, Vec2};
use crate::render::canvas::ViewState;
use crate::render::text::{TextAlign, HorizontalAlign, VerticalAlign};
use crate::render::text_canvas2d::Canvas2dTextRenderer;

/// Dimension style configuration
#[derive(Debug, Clone)]
pub struct DimensionStyle {
    /// Text color (CSS format)
    pub text_color: String,
    /// Font size in screen pixels
    pub font_size: f64,
    /// Vertical offset from edge in screen pixels
    pub offset_pixels: f64,
}

impl Default for DimensionStyle {
    fn default() -> Self {
        Self {
            text_color: "white".to_string(),
            font_size: 14.0,
            offset_pixels: 15.0,
        }
    }
}

/// Draws a dimension value along a triangle edge using Canvas 2D API
///
/// Coordinates are converted from model to screen using ViewState.
/// Text is positioned at the midpoint of the edge with perpendicular offset.
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
    // Convert to screen coordinates
    let start_screen = view_state.model_to_screen(start);
    let end_screen = view_state.model_to_screen(end);

    // Calculate edge direction in screen space
    let vec = end_screen - start_screen;
    let length = vec.length();
    if length < 1.0 {
        return; // Skip very short edges
    }
    let direction = vec / length;

    // Calculate perpendicular vector for offset
    let perpendicular = Vec2::new(-direction.y, direction.x);

    // Calculate text position (midpoint + offset) in screen coordinates
    let midpoint = Pos2::new(
        (start_screen.x + end_screen.x) * 0.5,
        (start_screen.y + end_screen.y) * 0.5,
    );
    let text_pos = midpoint + perpendicular * style.offset_pixels as f32;

    // Calculate rotation angle from screen-space direction
    // (view rotation is already applied via model_to_screen)
    let mut angle_rad = direction.y.atan2(direction.x);

    // Auto-adjust text direction to be readable (left to right)
    if angle_rad > std::f32::consts::FRAC_PI_2 || angle_rad < -std::f32::consts::FRAC_PI_2 {
        angle_rad += std::f32::consts::PI;
    }
    let angle_degrees = angle_rad.to_degrees() as f64;

    // Format dimension value
    let text = format!("{:.2}", value);

    // Draw using simple screen-coordinate API
    let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle);
    let _ = renderer.draw_text(
        &text,
        text_pos.x as f64,
        text_pos.y as f64,
        style.font_size,
        angle_degrees,
        align,
        &style.text_color,
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
