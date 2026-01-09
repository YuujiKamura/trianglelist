//! Canvas rendering for triangles
//!
//! Provides functions for drawing triangles, handling pan/zoom, and viewport management

use eframe::egui::{Color32, Painter, Pos2, Shape, Stroke, Vec2};
use crate::csv::{ParsedTriangle, ConnectionType};
use std::f32::consts::PI;

/// Triangle with calculated position in world coordinates
#[derive(Clone, Debug)]
pub struct PositionedTriangle {
    /// Original parsed triangle data
    pub data: ParsedTriangle,
    /// Three vertices in world coordinates: [CA, AB, BC]
    /// - CA (point[0]): Base point where A and C sides meet
    /// - AB: Point where A and B sides meet
    /// - BC: Point where B and C sides meet
    pub points: [Pos2; 3],
    /// Rotation angle in radians
    pub angle: f32,
}

impl PositionedTriangle {
    /// Calculate centroid of the triangle
    pub fn centroid(&self) -> Pos2 {
        Pos2::new(
            (self.points[0].x + self.points[1].x + self.points[2].x) / 3.0,
            (self.points[0].y + self.points[1].y + self.points[2].y) / 3.0,
        )
    }

    /// Get internal angle at vertex CA (angle between sides A and C)
    pub fn angle_ca(&self) -> f32 {
        calculate_internal_angle(self.points[1], self.points[0], self.points[2])
    }

    /// Get internal angle at vertex AB (angle between sides A and B)
    pub fn angle_ab(&self) -> f32 {
        calculate_internal_angle(self.points[0], self.points[1], self.points[2])
    }
}

/// Calculate internal angle at vertex p2 (angle formed by p1-p2-p3)
fn calculate_internal_angle(p1: Pos2, p2: Pos2, p3: Pos2) -> f32 {
    let v1 = Vec2::new(p1.x - p2.x, p1.y - p2.y);
    let v2 = Vec2::new(p3.x - p2.x, p3.y - p2.y);
    let dot = v1.x * v2.x + v1.y * v2.y;
    let mag1 = (v1.x * v1.x + v1.y * v1.y).sqrt();
    let mag2 = (v2.x * v2.x + v2.y * v2.y).sqrt();
    if mag1 == 0.0 || mag2 == 0.0 {
        return 0.0;
    }
    let cos_angle = (dot / (mag1 * mag2)).clamp(-1.0, 1.0);
    cos_angle.acos()
}

/// Calculate triangle vertex positions from side lengths at given base point and angle
///
/// # Arguments
/// * `side_a` - Length of side A (CA to AB)
/// * `side_b` - Length of side B (AB to BC)
/// * `side_c` - Length of side C (BC to CA)
/// * `base_point` - Position of CA vertex
/// * `angle` - Rotation angle in radians
///
/// # Returns
/// Three points: [CA, AB, BC]
pub fn calculate_triangle_points_at(
    side_a: f32,
    side_b: f32,
    side_c: f32,
    base_point: Pos2,
    angle: f32,
) -> [Pos2; 3] {
    // Validate triangle inequality
    if side_a + side_b <= side_c || side_b + side_c <= side_a || side_c + side_a <= side_b {
        return [base_point; 3];
    }

    // CA point (base point)
    let ca = base_point;

    // AB point: offset from CA by side_a at the given angle
    let ab = Pos2::new(
        ca.x + side_a * angle.cos(),
        ca.y + side_a * angle.sin(),
    );

    // BC point: calculated using cosine rule
    // Angle at AB vertex (angle between CA-AB and AB-BC)
    let theta = (ca.y - ab.y).atan2(ca.x - ab.x);
    let cos_angle_ab = (side_a * side_a + side_b * side_b - side_c * side_c)
        / (2.0 * side_a * side_b);
    let cos_angle_ab_clamped = cos_angle_ab.clamp(-1.0, 1.0);
    let alpha = cos_angle_ab_clamped.acos();
    let bc_angle = theta + alpha;

    let bc = Pos2::new(
        ab.x + side_b * bc_angle.cos(),
        ab.y + side_b * bc_angle.sin(),
    );

    [ca, ab, bc]
}

/// Calculate all triangle positions based on connections
///
/// This is the main function that processes all triangles and calculates
/// their world positions based on parent-child connections.
pub fn calculate_all_triangle_positions(triangles: &[ParsedTriangle]) -> Vec<PositionedTriangle> {
    let mut positioned: Vec<PositionedTriangle> = Vec::with_capacity(triangles.len());

    for triangle in triangles {
        let (base_point, angle) = if triangle.is_independent() {
            // Independent triangle: place at origin with default angle (PI = 180°)
            (Pos2::new(0.0, 0.0), PI)
        } else {
            // Connected triangle: find parent and calculate position
            let parent = positioned.iter().find(|p| p.data.number == triangle.parent_number);

            if let Some(parent) = parent {
                match triangle.connection_type {
                    ConnectionType::ToParentB => {
                        // Connect to parent's B side
                        // Child's base point = Parent's BC point
                        // Child's angle = Parent's angle + Parent's angle_AB
                        let base = parent.points[2]; // BC point
                        let new_angle = parent.angle + parent.angle_ab();
                        (base, new_angle)
                    }
                    ConnectionType::ToParentC => {
                        // Connect to parent's C side
                        // Child's base point = Parent's CA point
                        // Child's angle = Parent's angle - Parent's angle_CA
                        let base = parent.points[0]; // CA point
                        let new_angle = parent.angle - parent.angle_ca();
                        (base, new_angle)
                    }
                    ConnectionType::Independent => {
                        // Should not happen, but handle gracefully
                        (Pos2::new(0.0, 0.0), PI)
                    }
                }
            } else {
                // Parent not found, place as independent
                log::warn!("Parent {} not found for triangle {}", triangle.parent_number, triangle.number);
                (Pos2::new(0.0, 0.0), PI)
            }
        };

        let points = calculate_triangle_points_at(
            triangle.side_a as f32,
            triangle.side_b as f32,
            triangle.side_c as f32,
            base_point,
            angle,
        );

        positioned.push(PositionedTriangle {
            data: triangle.clone(),
            points,
            angle,
        });
    }

    positioned
}

/// Calculate triangle vertex positions from side lengths (legacy function)
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

/// View state for pan, zoom, and rotation operations
#[derive(Clone, Debug)]
pub struct ViewState {
    /// Pan offset (translation)
    pub pan: Pos2,
    /// Zoom level (1.0 = 100%)
    pub zoom: f32,
    /// Rotation angle in radians (around canvas center)
    pub rotation: f32,
    /// Canvas size (should be updated from UI when available)
    pub canvas_size: Vec2,
}

impl Default for ViewState {
    fn default() -> Self {
        Self {
            pan: Pos2::new(0.0, 0.0),
            zoom: 1.0,
            rotation: 0.0,
            canvas_size: Vec2::new(800.0, 600.0),
        }
    }
}

impl ViewState {
    /// Update canvas size from UI
    pub fn set_canvas_size(&mut self, size: Vec2) {
        self.canvas_size = size;
    }

    /// Canvas center point
    pub fn canvas_center(&self) -> Pos2 {
        Pos2::new(self.canvas_size.x / 2.0, self.canvas_size.y / 2.0)
    }

    /// Convert model coordinates to screen coordinates
    /// Applies: zoom -> rotation (around origin) -> pan
    pub fn model_to_screen(&self, model_pos: Pos2) -> Pos2 {
        // Apply zoom
        let zoomed_x = model_pos.x * self.zoom;
        let zoomed_y = model_pos.y * self.zoom;

        // Apply rotation around canvas center
        let center = self.canvas_center();
        let cos_r = self.rotation.cos();
        let sin_r = self.rotation.sin();

        // Translate to center, rotate, translate back
        let centered_x = zoomed_x + self.pan.x - center.x;
        let centered_y = zoomed_y + self.pan.y - center.y;

        let rotated_x = centered_x * cos_r - centered_y * sin_r;
        let rotated_y = centered_x * sin_r + centered_y * cos_r;

        Pos2::new(rotated_x + center.x, rotated_y + center.y)
    }

    /// Convert screen coordinates to model coordinates
    /// Inverse of model_to_screen
    pub fn screen_to_model(&self, screen_pos: Pos2) -> Pos2 {
        let center = self.canvas_center();
        let cos_r = self.rotation.cos();
        let sin_r = self.rotation.sin();

        // Inverse rotation around center
        let centered_x = screen_pos.x - center.x;
        let centered_y = screen_pos.y - center.y;

        let unrotated_x = centered_x * cos_r + centered_y * sin_r;
        let unrotated_y = -centered_x * sin_r + centered_y * cos_r;

        // Inverse pan and zoom
        let model_x = (unrotated_x + center.x - self.pan.x) / self.zoom;
        let model_y = (unrotated_y + center.y - self.pan.y) / self.zoom;

        Pos2::new(model_x, model_y)
    }

    /// Fit view to show all triangles
    ///
    /// Calculates bounding box of all connected triangles and adjusts
    /// pan and zoom to fit them within the canvas.
    pub fn fit_to_triangles(&mut self, triangles: &[ParsedTriangle]) {
        if triangles.is_empty() {
            return;
        }

        // Calculate all triangle positions with proper connections
        let positioned = calculate_all_triangle_positions(triangles);

        // Calculate bounding box from all triangle vertices
        let mut min_x = f32::MAX;
        let mut min_y = f32::MAX;
        let mut max_x = f32::MIN;
        let mut max_y = f32::MIN;

        for tri in &positioned {
            for point in &tri.points {
                min_x = min_x.min(point.x);
                min_y = min_y.min(point.y);
                max_x = max_x.max(point.x);
                max_y = max_y.max(point.y);
            }
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::csv::{parse_csv, ConnectionType};

    fn approx_eq(a: f32, b: f32, epsilon: f32) -> bool {
        (a - b).abs() < epsilon
    }

    #[test]
    fn test_calculate_triangle_points_at_basic() {
        // 3-4-5 right triangle at origin with angle PI (pointing left)
        let points = calculate_triangle_points_at(3.0, 4.0, 5.0, Pos2::ZERO, PI);

        // CA at origin
        assert!(approx_eq(points[0].x, 0.0, 0.01));
        assert!(approx_eq(points[0].y, 0.0, 0.01));

        // AB at (-3, 0) since angle is PI
        assert!(approx_eq(points[1].x, -3.0, 0.01));
        assert!(approx_eq(points[1].y, 0.0, 0.01));
    }

    #[test]
    fn test_calculate_all_triangle_positions_independent() {
        let csv = r#"Header1
Header2
Header3
Header4
1, 6.0, 5.0, 4.0, -1, -1"#;

        let result = parse_csv(csv).unwrap();
        let positioned = calculate_all_triangle_positions(&result.triangles);

        assert_eq!(positioned.len(), 1);
        assert!(positioned[0].data.is_independent());
        // Independent triangle starts at origin with angle PI
        assert!(approx_eq(positioned[0].points[0].x, 0.0, 0.01));
        assert!(approx_eq(positioned[0].points[0].y, 0.0, 0.01));
    }

    #[test]
    fn test_calculate_all_triangle_positions_connected() {
        // Test B-side and C-side connections
        let csv = r#"Header1
Header2
Header3
Header4
1, 6.0, 5.0, 4.0, -1, -1
2, 5.0, 4.0, 3.0, 1, 1
3, 4.0, 3.5, 3.0, 1, 2"#;

        let result = parse_csv(csv).unwrap();
        let positioned = calculate_all_triangle_positions(&result.triangles);

        assert_eq!(positioned.len(), 3);

        // Triangle 1 is independent
        assert!(positioned[0].data.is_independent());

        // Triangle 2 connects to parent 1's B side
        assert_eq!(positioned[1].data.connection_type, ConnectionType::ToParentB);
        // Its base point should be at parent's BC point
        let parent_bc = positioned[0].points[2];
        assert!(approx_eq(positioned[1].points[0].x, parent_bc.x, 0.01));
        assert!(approx_eq(positioned[1].points[0].y, parent_bc.y, 0.01));

        // Triangle 3 connects to parent 1's C side
        assert_eq!(positioned[2].data.connection_type, ConnectionType::ToParentC);
        // Its base point should be at parent's CA point
        let parent_ca = positioned[0].points[0];
        assert!(approx_eq(positioned[2].points[0].x, parent_ca.x, 0.01));
        assert!(approx_eq(positioned[2].points[0].y, parent_ca.y, 0.01));
    }

    #[test]
    fn test_real_world_csv_triangle_connections() {
        // Real-world CSV format from ヘロン calculation software
        let csv = r#"koujiname,
rosenname, 新規路線
gyousyaname,
zumennum, 1/1
1,6.42,5.93,8.7,0,3
2,8.7,8.18,1.1,1,2
3,8.18,8.05,1.2,2,1
ListAngle, 89.53419
ListScale, 1.0
"#;

        let result = parse_csv(csv).unwrap();
        let positioned = calculate_all_triangle_positions(&result.triangles);

        assert_eq!(positioned.len(), 3);

        // Triangle 1: independent (parent=0)
        assert!(positioned[0].data.is_independent());

        // Triangle 2: connects to parent 1's C side (connection_type=2)
        // Its A side (8.7) should match parent's C side (8.7)
        assert_eq!(positioned[1].data.side_a, 8.7);
        assert_eq!(positioned[0].data.side_c, 8.7);
        // Base point should be parent's CA point
        let parent1_ca = positioned[0].points[0];
        assert!(approx_eq(positioned[1].points[0].x, parent1_ca.x, 0.01));
        assert!(approx_eq(positioned[1].points[0].y, parent1_ca.y, 0.01));

        // Triangle 3: connects to parent 2's B side (connection_type=1)
        // Its A side (8.18) should match parent's B side (8.18)
        assert_eq!(positioned[2].data.side_a, 8.18);
        assert_eq!(positioned[1].data.side_b, 8.18);
        // Base point should be parent 2's BC point
        let parent2_bc = positioned[1].points[2];
        assert!(approx_eq(positioned[2].points[0].x, parent2_bc.x, 0.01));
        assert!(approx_eq(positioned[2].points[0].y, parent2_bc.y, 0.01));
    }
}

