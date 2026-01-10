//! CAD-compatible text rendering
//!
//! Provides text drawing functions with DXF-compatible alignment and rotation.
//! Supports BBOX-based rendering where text scales with view transformations.

use eframe::egui::{Color32, Painter, Pos2, Vec2};

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

/// Draws text with CAD-compatible alignment
///
/// Note: egui does not support text rotation. Text is always drawn horizontally.
/// For rotated text, use Canvas 2D API (text_canvas2d.rs) instead.
///
/// # Arguments
/// * `painter` - egui Painter
/// * `text` - Text string to draw
/// * `pos` - Base position (DXF alignment point)
/// * `height` - Text height in pixels
/// * `_rotation_degrees` - Ignored (egui doesn't support rotation)
/// * `color` - Text color
/// * `align` - Text alignment
pub fn draw_text_cad_style(
    painter: &Painter,
    text: &str,
    pos: Pos2,
    height: f32,
    _rotation_degrees: f32,  // Ignored - egui doesn't support text rotation
    color: Color32,
    align: TextAlign,
) {
    // Convert alignment to egui's Align2
    let align2 = egui::Align2([
        match align.horizontal {
            HorizontalAlign::Left => egui::Align::LEFT,
            HorizontalAlign::Center => egui::Align::Center,
            HorizontalAlign::Right => egui::Align::RIGHT,
        },
        match align.vertical {
            VerticalAlign::Top => egui::Align::TOP,
            VerticalAlign::Middle => egui::Align::Center,
            VerticalAlign::Bottom | VerticalAlign::Baseline => egui::Align::BOTTOM,
        },
    ]);

    painter.text(
        pos,
        align2,
        text,
        egui::FontId::monospace(height),
        color,
    );
}

/// Text entity with model-coordinate bounding box
///
/// This enables CAD-equivalent text rendering where text scales
/// naturally with view transformations (zoom, pan, rotation).
#[derive(Clone, Debug)]
pub struct TextEntity {
    /// Text content
    pub text: String,
    /// Insertion point in model coordinates
    pub position: Pos2,
    /// Text height in model coordinates (DXF text height)
    pub height: f32,
    /// Width factor (default 1.0, >1 stretches horizontally)
    pub width_factor: f32,
    /// Rotation angle in degrees (counterclockwise from X-axis)
    pub rotation: f32,
    /// Text alignment
    pub alignment: TextAlign,
    /// Text color (CSS string for Canvas 2D)
    pub color: String,
}

impl TextEntity {
    /// Create a new text entity
    pub fn new(text: &str, x: f32, y: f32, height: f32) -> Self {
        Self {
            text: text.to_string(),
            position: Pos2::new(x, y),
            height,
            width_factor: 1.0,
            rotation: 0.0,
            alignment: TextAlign::new(HorizontalAlign::Left, VerticalAlign::Baseline),
            color: "#ffffff".to_string(),
        }
    }

    pub fn with_rotation(mut self, rotation: f32) -> Self {
        self.rotation = rotation;
        self
    }

    pub fn with_alignment(mut self, alignment: TextAlign) -> Self {
        self.alignment = alignment;
        self
    }

    pub fn with_color(mut self, color: &str) -> Self {
        self.color = color.to_string();
        self
    }

    pub fn with_width_factor(mut self, factor: f32) -> Self {
        self.width_factor = factor;
        self
    }

    /// Calculate approximate text width in model coordinates
    ///
    /// Uses a simple approximation: width = height * char_count * width_factor * 0.6
    /// The 0.6 factor approximates monospace character aspect ratio.
    pub fn approx_width(&self) -> f32 {
        self.height * self.text.chars().count() as f32 * self.width_factor * 0.6
    }

    /// Calculate the bounding box corners in model coordinates
    ///
    /// Returns 4 corners: [bottom-left, bottom-right, top-right, top-left]
    /// before rotation is applied.
    pub fn bbox_corners(&self) -> [Pos2; 4] {
        let width = self.approx_width();
        let height = self.height;

        // Calculate offset based on alignment
        let (offset_x, offset_y) = match (self.alignment.horizontal, self.alignment.vertical) {
            (HorizontalAlign::Left, VerticalAlign::Bottom | VerticalAlign::Baseline) => (0.0, 0.0),
            (HorizontalAlign::Left, VerticalAlign::Middle) => (0.0, -height / 2.0),
            (HorizontalAlign::Left, VerticalAlign::Top) => (0.0, -height),
            (HorizontalAlign::Center, VerticalAlign::Bottom | VerticalAlign::Baseline) => (-width / 2.0, 0.0),
            (HorizontalAlign::Center, VerticalAlign::Middle) => (-width / 2.0, -height / 2.0),
            (HorizontalAlign::Center, VerticalAlign::Top) => (-width / 2.0, -height),
            (HorizontalAlign::Right, VerticalAlign::Bottom | VerticalAlign::Baseline) => (-width, 0.0),
            (HorizontalAlign::Right, VerticalAlign::Middle) => (-width, -height / 2.0),
            (HorizontalAlign::Right, VerticalAlign::Top) => (-width, -height),
        };

        // Unrotated corners relative to insertion point
        let corners_local = [
            Vec2::new(offset_x, offset_y),                      // bottom-left
            Vec2::new(offset_x + width, offset_y),              // bottom-right
            Vec2::new(offset_x + width, offset_y + height),     // top-right
            Vec2::new(offset_x, offset_y + height),             // top-left
        ];

        // Apply rotation
        let rot_rad = self.rotation.to_radians();
        let cos_r = rot_rad.cos();
        let sin_r = rot_rad.sin();

        corners_local.map(|v| {
            let rotated = Vec2::new(
                v.x * cos_r - v.y * sin_r,
                v.x * sin_r + v.y * cos_r,
            );
            self.position + rotated
        })
    }
}

/// Transform text entity BBOX to screen coordinates and calculate rendering parameters
///
/// Returns (screen_position, screen_height, screen_rotation, is_flipped) for Canvas 2D rendering.
/// `is_flipped` indicates if the coordinate system is mirrored (determinant negative),
/// which would cause text to appear backwards.
pub fn transform_text_bbox(
    entity: &TextEntity,
    model_to_screen: impl Fn(Pos2) -> Pos2,
) -> (Pos2, f32, f32, bool) {
    let corners = entity.bbox_corners();

    // Transform all corners to screen space
    let screen_corners: [Pos2; 4] = corners.map(|c| model_to_screen(c));

    // Calculate screen-space dimensions from transformed corners
    // Use bottom-left to bottom-right for width direction
    let width_vec = screen_corners[1] - screen_corners[0];
    let height_vec = screen_corners[3] - screen_corners[0];

    // Check if coordinate system is flipped (mirrored) using cross product
    // If determinant (cross product z-component) is negative, it's flipped
    let det = width_vec.x * height_vec.y - width_vec.y * height_vec.x;
    let is_flipped = det < 0.0;

    // Screen height is the length of the height vector
    let screen_height = height_vec.length();

    // Screen rotation from width vector direction
    let screen_rotation = width_vec.y.atan2(width_vec.x).to_degrees();

    // Screen position: use the transformed insertion point
    let screen_pos = model_to_screen(entity.position);

    (screen_pos, screen_height, screen_rotation, is_flipped)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn approx_eq(a: f32, b: f32, epsilon: f32) -> bool {
        (a - b).abs() < epsilon
    }

    // TextEntity tests
    #[test]
    fn test_text_entity_new() {
        let entity = TextEntity::new("test", 10.0, 20.0, 5.0);
        assert_eq!(entity.text, "test");
        assert_eq!(entity.position.x, 10.0);
        assert_eq!(entity.position.y, 20.0);
        assert_eq!(entity.height, 5.0);
        assert_eq!(entity.rotation, 0.0);
        assert_eq!(entity.width_factor, 1.0);
    }

    #[test]
    fn test_text_entity_with_rotation() {
        let entity = TextEntity::new("test", 0.0, 0.0, 5.0)
            .with_rotation(45.0);
        assert_eq!(entity.rotation, 45.0);
    }

    #[test]
    fn test_text_entity_approx_width() {
        let entity = TextEntity::new("ABC", 0.0, 0.0, 10.0);
        // width = height * char_count * width_factor * 0.6
        // = 10.0 * 3 * 1.0 * 0.6 = 18.0
        assert!(approx_eq(entity.approx_width(), 18.0, 0.01));
    }

    // bbox_corners tests
    #[test]
    fn test_bbox_corners_no_rotation_left_bottom() {
        let entity = TextEntity::new("AB", 0.0, 0.0, 10.0)
            .with_alignment(TextAlign::new(HorizontalAlign::Left, VerticalAlign::Bottom));

        let corners = entity.bbox_corners();
        let width = entity.approx_width(); // 12.0

        // bottom-left at origin
        assert!(approx_eq(corners[0].x, 0.0, 0.01));
        assert!(approx_eq(corners[0].y, 0.0, 0.01));
        // bottom-right
        assert!(approx_eq(corners[1].x, width, 0.01));
        assert!(approx_eq(corners[1].y, 0.0, 0.01));
        // top-right
        assert!(approx_eq(corners[2].x, width, 0.01));
        assert!(approx_eq(corners[2].y, 10.0, 0.01));
        // top-left
        assert!(approx_eq(corners[3].x, 0.0, 0.01));
        assert!(approx_eq(corners[3].y, 10.0, 0.01));
    }

    #[test]
    fn test_bbox_corners_center_middle() {
        let entity = TextEntity::new("AB", 0.0, 0.0, 10.0)
            .with_alignment(TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle));

        let corners = entity.bbox_corners();
        let width = entity.approx_width(); // 12.0

        // Centered horizontally and vertically
        assert!(approx_eq(corners[0].x, -width / 2.0, 0.01));
        assert!(approx_eq(corners[0].y, -5.0, 0.01));
    }

    #[test]
    fn test_bbox_corners_90deg_rotation() {
        let entity = TextEntity::new("A", 0.0, 0.0, 10.0)
            .with_rotation(90.0)
            .with_alignment(TextAlign::new(HorizontalAlign::Left, VerticalAlign::Bottom));

        let corners = entity.bbox_corners();

        // After 90 degree rotation, x becomes -y, y becomes x
        // bottom-left (0, 0) rotated 90deg stays at (0, 0)
        assert!(approx_eq(corners[0].x, 0.0, 0.01));
        assert!(approx_eq(corners[0].y, 0.0, 0.01));
        // bottom-right (width, 0) rotated 90deg becomes (0, width)
        let width = entity.approx_width();
        assert!(approx_eq(corners[1].x, 0.0, 0.01));
        assert!(approx_eq(corners[1].y, width, 0.01));
    }

    // transform_text_bbox tests
    #[test]
    fn test_transform_text_bbox_identity() {
        let entity = TextEntity::new("AB", 10.0, 20.0, 5.0)
            .with_alignment(TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle));

        // Identity transformation
        let (pos, height, rotation, is_flipped) = transform_text_bbox(&entity, |p| p);

        assert!(approx_eq(pos.x, 10.0, 0.01));
        assert!(approx_eq(pos.y, 20.0, 0.01));
        assert!(approx_eq(height, 5.0, 0.01));
        assert!(approx_eq(rotation, 0.0, 0.1));
        assert!(!is_flipped);
    }

    #[test]
    fn test_transform_text_bbox_scale() {
        let entity = TextEntity::new("AB", 10.0, 20.0, 5.0);

        // Scale by 2x
        let (pos, height, _rotation, _is_flipped) = transform_text_bbox(&entity, |p| {
            Pos2::new(p.x * 2.0, p.y * 2.0)
        });

        assert!(approx_eq(pos.x, 20.0, 0.01));
        assert!(approx_eq(pos.y, 40.0, 0.01));
        assert!(approx_eq(height, 10.0, 0.01)); // Height doubles
    }

    #[test]
    fn test_transform_text_bbox_flip_detection() {
        let entity = TextEntity::new("AB", 0.0, 0.0, 5.0);

        // Y-axis flip (mirror)
        let (_pos, _height, _rotation, is_flipped) = transform_text_bbox(&entity, |p| {
            Pos2::new(p.x, -p.y)
        });

        assert!(is_flipped); // Should detect flip
    }

    // TextAlign tests
    #[test]
    fn test_text_align_from_dxf() {
        let align = TextAlign::from_dxf(1, 2);
        assert_eq!(align.horizontal, HorizontalAlign::Center);
        assert_eq!(align.vertical, VerticalAlign::Middle);

        let align2 = TextAlign::from_dxf(0, 0);
        assert_eq!(align2.horizontal, HorizontalAlign::Left);
        assert_eq!(align2.vertical, VerticalAlign::Baseline);
    }
}

