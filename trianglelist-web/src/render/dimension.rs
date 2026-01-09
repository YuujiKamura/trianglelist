//! Dimension line rendering for triangles
//!
//! Provides functions for drawing dimension lines with text labels for triangle sides.

use eframe::egui::{Color32, Painter, Pos2, Shape, Stroke, Vec2};
use crate::render::canvas::ViewState;
use crate::render::text::{draw_text_cad_style, TextAlign, HorizontalAlign, VerticalAlign};

/// Dimension line style configuration
#[derive(Debug, Clone)]
pub struct DimensionStyle {
    /// Line color
    pub line_color: Color32,
    /// Text color
    pub text_color: Color32,
    /// Line width
    pub line_width: f32,
    /// Text height in model units
    pub text_height: f32,
    /// Offset distance from the measured line
    pub offset_distance: f32,
    /// Arrow size
    pub arrow_size: f32,
}

impl Default for DimensionStyle {
    fn default() -> Self {
        Self {
            line_color: Color32::from_rgb(200, 200, 200),
            text_color: Color32::from_rgb(255, 255, 255),
            line_width: 1.0,
            text_height: 10.0,
            offset_distance: 15.0,
            arrow_size: 5.0,
        }
    }
}

/// Draws a dimension line for a triangle side
///
/// # Arguments
/// * `painter` - egui Painter
/// * `start` - Start point of the measured line (screen coordinates)
/// * `end` - End point of the measured line (screen coordinates)
/// * `value` - Dimension value to display
/// * `view_state` - View state for coordinate transformations
/// * `style` - Dimension style configuration
pub fn draw_dimension_line(
    painter: &Painter,
    start: Pos2,
    end: Pos2,
    value: f64,
    _view_state: &ViewState,
    style: &DimensionStyle,
) {
    // Calculate the direction vector of the measured line
    let vec = end - start;
    let direction = vec.normalized();
    
    // Calculate perpendicular vector for offset (rotate 90 degrees)
    let perpendicular = Vec2::new(-direction.y, direction.x);
    
    // Calculate dimension line position (offset from the measured line)
    let offset = perpendicular * style.offset_distance;
    let dim_start = start + offset;
    let dim_end = end + offset;
    
    // Draw extension lines (from measured points to dimension line)
    painter.line_segment([start, dim_start], Stroke::new(style.line_width, style.line_color));
    painter.line_segment([end, dim_end], Stroke::new(style.line_width, style.line_color));
    
    // Draw main dimension line
    painter.line_segment([dim_start, dim_end], Stroke::new(style.line_width, style.line_color));
    
    // Draw arrows at both ends
    draw_arrow(painter, dim_start, direction, style);
    draw_arrow(painter, dim_end, -direction, style);
    
    // Calculate text position (center of dimension line)
    let text_pos = Pos2::new(
        (dim_start.x + dim_end.x) * 0.5,
        (dim_start.y + dim_end.y) * 0.5,
    );
    
    // Calculate rotation angle for text (aligned with dimension line)
    let angle_rad = direction.y.atan2(direction.x);
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

/// Draws an arrow at the specified position
fn draw_arrow(painter: &Painter, position: Pos2, direction: Vec2, style: &DimensionStyle) {
    // Arrow head points (perpendicular to direction)
    let perpendicular = Vec2::new(-direction.y, direction.x);
    let arrow_head = position - direction * style.arrow_size;
    let arrow_left = arrow_head + perpendicular * style.arrow_size * 0.5;
    let arrow_right = arrow_head - perpendicular * style.arrow_size * 0.5;
    
    // Draw arrow as a closed triangle
    painter.add(Shape::line(vec![position, arrow_left, arrow_right, position], 
        Stroke::new(style.line_width, style.line_color)));
}

/// Draws dimension lines for all three sides of a triangle
///
/// # Arguments
/// * `painter` - egui Painter
/// * `points` - Three vertices of the triangle (screen coordinates)
/// * `side_lengths` - Lengths of the three sides [A, B, C]
/// * `view_state` - View state for coordinate transformations
/// * `style` - Dimension style configuration
pub fn draw_triangle_dimensions(
    painter: &Painter,
    points: [Pos2; 3],
    side_lengths: [f64; 3],
    view_state: &ViewState,
    style: &DimensionStyle,
) {
    // Side A: from points[1] to points[2] (BC edge)
    draw_dimension_line(
        painter,
        points[1],
        points[2],
        side_lengths[0],
        view_state,
        style,
    );
    
    // Side B: from points[0] to points[2] (CA edge)
    draw_dimension_line(
        painter,
        points[0],
        points[2],
        side_lengths[1],
        view_state,
        style,
    );
    
    // Side C: from points[0] to points[1] (AB edge)
    draw_dimension_line(
        painter,
        points[0],
        points[1],
        side_lengths[2],
        view_state,
        style,
    );
}
