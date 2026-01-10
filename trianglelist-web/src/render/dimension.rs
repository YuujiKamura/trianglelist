//! Dimension value rendering for triangles
//!
//! Draws dimension values along triangle edges.
//! Based on Android version: text is drawn along the edge path with offset,
//! no extension lines or dimension auxiliary lines.
//!
//! Supports two rendering modes:
//! - Fixed size: Text stays readable at any zoom level
//! - BBOX-based: Text scales with zoom (CAD equivalent)

use eframe::egui::{Pos2, Vec2};
use crate::render::canvas::ViewState;
use crate::render::text::{TextAlign, TextEntity, HorizontalAlign, VerticalAlign};
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

/// BBOX-based dimension style configuration
#[derive(Debug, Clone)]
pub struct BboxDimensionStyle {
    /// Text color (CSS format)
    pub text_color: String,
    /// Text height in model coordinates
    pub text_height: f32,
    /// Offset from edge in model coordinates
    pub offset: f32,
}

impl Default for BboxDimensionStyle {
    fn default() -> Self {
        Self {
            text_color: "white".to_string(),
            text_height: 0.3,  // Model coordinate units
            offset: 0.15,      // Model coordinate units
        }
    }
}

/// Draws a dimension value using BBOX-based text (scales with zoom)
///
/// Text is positioned at the midpoint of the edge with perpendicular offset.
/// The text scales with view transformations like lines and shapes.
///
/// # Arguments
/// * `renderer` - Canvas 2D text renderer
/// * `start` - Start point of the edge (model coordinates)
/// * `end` - End point of the edge (model coordinates)
/// * `value` - Dimension value to display
/// * `view_state` - View state for coordinate transformations
/// * `style` - BBOX dimension style configuration
/// * `auto_readable` - If true, adjust text to keep it readable
pub fn draw_dimension_value_bbox(
    renderer: &Canvas2dTextRenderer,
    start: Pos2,
    end: Pos2,
    value: f64,
    view_state: &ViewState,
    style: &BboxDimensionStyle,
    auto_readable: bool,
) {
    // Calculate edge direction in model space
    let vec = Vec2::new(end.x - start.x, end.y - start.y);
    let length = vec.length();
    if length < 0.001 {
        return; // Skip very short edges
    }
    let direction = vec / length;

    // Calculate perpendicular vector for offset (in model coordinates)
    let perpendicular = Vec2::new(-direction.y, direction.x);

    // Calculate text position (midpoint + offset) in model coordinates
    let midpoint = Pos2::new(
        (start.x + end.x) * 0.5,
        (start.y + end.y) * 0.5,
    );
    let text_pos = midpoint + perpendicular * style.offset;

    // Calculate rotation angle in model space
    // No negation needed - transform_text_bbox handles coordinate system conversion
    let angle_rad = direction.y.atan2(direction.x);
    let rotation = angle_rad.to_degrees();

    // Format dimension value
    let text = format!("{:.2}", value);

    // Create TextEntity in model coordinates
    let entity = TextEntity::new(&text, text_pos.x, text_pos.y, style.text_height)
        .with_rotation(rotation)
        .with_alignment(TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle))
        .with_color(&style.text_color);

    // Draw using BBOX transformation
    let _ = renderer.draw_text_entity(&entity, |pos| view_state.model_to_screen(pos), auto_readable);
}

/// Draws dimension values for all three sides using BBOX-based text
///
/// # Arguments
/// * `renderer` - Canvas 2D text renderer
/// * `points` - Three vertices of the triangle (model coordinates): [CA, AB, BC]
/// * `side_lengths` - Lengths of the three sides [A, B, C]
/// * `view_state` - View state for coordinate transformations
/// * `style` - BBOX dimension style configuration
/// * `skip_side_a` - If true, skip drawing dimension for side A (connected edge)
/// * `auto_readable` - If true, adjust text to keep it readable
pub fn draw_triangle_dimensions_bbox(
    renderer: &Canvas2dTextRenderer,
    points: [Pos2; 3],
    side_lengths: [f64; 3],
    view_state: &ViewState,
    style: &BboxDimensionStyle,
    skip_side_a: bool,
    auto_readable: bool,
) {
    // Side A: CA to AB (points[0] to points[1])
    if !skip_side_a {
        draw_dimension_value_bbox(
            renderer,
            points[0],
            points[1],
            side_lengths[0],
            view_state,
            style,
            auto_readable,
        );
    }

    // Side B: AB to BC (points[1] to points[2])
    draw_dimension_value_bbox(
        renderer,
        points[1],
        points[2],
        side_lengths[1],
        view_state,
        style,
        auto_readable,
    );

    // Side C: BC to CA (points[2] to points[0])
    draw_dimension_value_bbox(
        renderer,
        points[2],
        points[0],
        side_lengths[2],
        view_state,
        style,
        auto_readable,
    );
}
