//! Triangle to DXF converter
//!
//! Converts `ParsedTriangle` data to DXF entities (lines and texts).
//!
//! # Overview
//!
//! This module provides the `TriangleToDxfConverter` struct which converts
//! triangle mesh data into DXF format. It generates LINE entities for triangle
//! edges and TEXT entities for triangle numbers.
//!
//! # Example
//!
//! ```no_run
//! use trianglelist_web::dxf::TriangleToDxfConverter;
//! use trianglelist_web::csv::ParsedTriangle;
//!
//! let converter = TriangleToDxfConverter::new();
//! let triangles = vec![
//!     ParsedTriangle::independent(1, 3.0, 4.0, 5.0),
//!     ParsedTriangle::independent(2, 5.0, 12.0, 13.0),
//! ];
//!
//! let (lines, texts) = converter.convert(&triangles);
//! // lines contains LINE entities for triangle edges
//! // texts contains TEXT entities for triangle numbers
//! ```

use crate::csv::ParsedTriangle;
use dxf::dxf::entities::{DxfLine, DxfText, HorizontalAlignment, VerticalAlignment};

/// Converts triangles to DXF entities
///
/// This converter transforms triangle mesh data into DXF format by generating
/// LINE entities for triangle edges and TEXT entities for triangle numbers.
///
/// # Configuration
///
/// The converter supports customization of:
/// - Line colors and layers
/// - Text colors, layers, and heights
/// - Number and side length text styling
#[derive(Debug, Clone)]
pub struct TriangleToDxfConverter {
    /// Default color for triangle lines
    pub line_color: i32,
    /// Default layer for triangle lines
    pub line_layer: String,
    /// Default color for triangle numbers
    pub number_color: i32,
    /// Default layer for triangle numbers
    pub number_layer: String,
    /// Default color for side length texts
    pub side_length_color: i32,
    /// Default layer for side length texts
    pub side_length_layer: String,
    /// Text height for triangle numbers
    pub number_text_height: f64,
    /// Text height for side lengths
    pub side_length_text_height: f64,
}

impl Default for TriangleToDxfConverter {
    fn default() -> Self {
        Self {
            line_color: 7, // White
            line_layer: "0".to_string(),
            number_color: 7, // White
            number_layer: "0".to_string(),
            side_length_color: 7, // White
            side_length_layer: "0".to_string(),
            number_text_height: 2.0,
            side_length_text_height: 1.5,
        }
    }
}

impl TriangleToDxfConverter {
    /// Creates a new converter with default settings
    ///
    /// # Returns
    ///
    /// A new `TriangleToDxfConverter` with default configuration:
    /// - Colors: White (7)
    /// - Layers: "0"
    /// - Number text height: 2.0
    /// - Side length text height: 1.5
    pub fn new() -> Self {
        Self::default()
    }

    /// Converts triangles to DXF lines and texts
    ///
    /// This function processes a slice of triangles and converts them into
    /// DXF entities. Each triangle generates 3 LINE entities (one for each edge)
    /// and 1 TEXT entity (for the triangle number).
    ///
    /// # Arguments
    ///
    /// * `triangles` - List of parsed triangles to convert
    ///
    /// # Returns
    ///
    /// A tuple containing:
    /// - `Vec<DxfLine>`: LINE entities for all triangle edges
    /// - `Vec<DxfText>`: TEXT entities for all triangle numbers
    ///
    /// # Edge Cases
    ///
    /// - Empty input: Returns empty vectors
    /// - Invalid triangles: Invalid triangles are still processed, but the
    ///   resulting DXF may not be geometrically correct
    ///
    /// # Panics
    ///
    /// This function may panic if triangle side lengths are invalid (e.g., NaN
    /// or negative) or if mathematical operations produce invalid results.
    pub fn convert(&self, triangles: &[ParsedTriangle]) -> (Vec<DxfLine>, Vec<DxfText>) {
        if triangles.is_empty() {
            return (Vec::new(), Vec::new());
        }

        let mut lines = Vec::with_capacity(triangles.len() * 3);
        let mut texts = Vec::with_capacity(triangles.len());

        for triangle in triangles {
            // Validate triangle before processing
            if !triangle.is_valid_triangle() {
                log::warn!(
                    "Skipping invalid triangle {}: sides {:?}, {:?}, {:?}",
                    triangle.number,
                    triangle.side_a,
                    triangle.side_b,
                    triangle.side_c
                );
                continue;
            }

            // Calculate triangle points (simplified - using side lengths)
            // This is a placeholder implementation
            // In a real scenario, you'd use the actual calculated coordinates
            let points = match self.calculate_triangle_points(triangle) {
                Ok(points) => points,
                Err(e) => {
                    log::error!(
                        "Failed to calculate points for triangle {}: {}",
                        triangle.number,
                        e
                    );
                    continue;
                }
            };

            // Add triangle edges as lines
            lines.push(
                DxfLine::new(points[0].0, points[0].1, points[1].0, points[1].1)
                    .color(self.line_color)
                    .layer(&self.line_layer),
            );

            lines.push(
                DxfLine::new(points[1].0, points[1].1, points[2].0, points[2].1)
                    .color(self.line_color)
                    .layer(&self.line_layer),
            );

            lines.push(
                DxfLine::new(points[2].0, points[2].1, points[0].0, points[0].1)
                    .color(self.line_color)
                    .layer(&self.line_layer),
            );

            // Add triangle number text at centroid
            let centroid = self.calculate_centroid(&points);
            texts.push(
                DxfText::new(centroid.0, centroid.1, &triangle.number.to_string())
                    .height(self.number_text_height)
                    .color(self.number_color)
                    .align_h(HorizontalAlignment::Center)
                    .align_v(VerticalAlignment::Middle)
                    .layer(&self.number_layer),
            );

            // Add side length texts (optional - can be enabled later)
            // For now, we'll skip side length texts to keep it simple
        }

        (lines, texts)
    }

    /// Calculates triangle points from side lengths
    ///
    /// This is a simplified implementation that assumes:
    /// - First point at origin (0, 0)
    /// - Second point along X-axis at distance side_c
    /// - Third point calculated using law of cosines
    ///
    /// # Arguments
    ///
    /// * `triangle` - The triangle to calculate points for
    ///
    /// # Returns
    ///
    /// * `Ok([(f64, f64); 3])` - Array of three points (x, y) if calculation succeeds
    /// * `Err(String)` - Error message if calculation fails
    ///
    /// # Errors
    ///
    /// Returns an error if:
    /// - Any side length is NaN or infinite
    /// - The triangle violates the triangle inequality
    /// - Mathematical operations produce invalid results
    fn calculate_triangle_points(
        &self,
        triangle: &ParsedTriangle,
    ) -> Result<[(f64, f64); 3], String> {
        // Validate inputs
        if triangle.side_a.is_nan()
            || triangle.side_a.is_infinite()
            || triangle.side_b.is_nan()
            || triangle.side_b.is_infinite()
            || triangle.side_c.is_nan()
            || triangle.side_c.is_infinite()
        {
            return Err("Side lengths must be finite numbers".to_string());
        }

        if triangle.side_a <= 0.0 || triangle.side_b <= 0.0 || triangle.side_c <= 0.0 {
            return Err("Side lengths must be positive".to_string());
        }

        // Point 1: Origin
        let p1 = (0.0, 0.0);

        // Point 2: Along X-axis at distance side_c
        let p2 = (triangle.side_c, 0.0);

        // Point 3: Calculate using law of cosines
        // cos(A) = (b² + c² - a²) / (2bc)
        let a_sq = triangle.side_a * triangle.side_a;
        let b_sq = triangle.side_b * triangle.side_b;
        let c_sq = triangle.side_c * triangle.side_c;
        let denominator = 2.0 * triangle.side_b * triangle.side_c;

        if denominator == 0.0 {
            return Err("Invalid triangle: zero denominator in cosine calculation".to_string());
        }

        let cos_a = (b_sq + c_sq - a_sq) / denominator;

        // Clamp cos_a to valid range [-1, 1] to avoid NaN from sqrt
        let cos_a_clamped = cos_a.max(-1.0).min(1.0);
        let sin_a = (1.0 - cos_a_clamped * cos_a_clamped).sqrt();

        // Validate that sin_a is a valid number
        if sin_a.is_nan() || sin_a.is_infinite() {
            return Err(format!(
                "Invalid triangle geometry: sin(A) = {} for sides {}, {}, {}",
                sin_a, triangle.side_a, triangle.side_b, triangle.side_c
            ));
        }

        let p3_x = triangle.side_b * cos_a_clamped;
        let p3_y = triangle.side_b * sin_a;
        let p3 = (p3_x, p3_y);

        Ok([p1, p2, p3])
    }

    /// Calculates centroid of triangle points
    ///
    /// The centroid is the average of the three triangle vertices.
    ///
    /// # Arguments
    ///
    /// * `points` - Array of three points (x, y)
    ///
    /// # Returns
    ///
    /// The centroid point as `(x, y)`.
    fn calculate_centroid(&self, points: &[(f64, f64); 3]) -> (f64, f64) {
        let x = (points[0].0 + points[1].0 + points[2].0) / 3.0;
        let y = (points[0].1 + points[1].1 + points[2].1) / 3.0;
        (x, y)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::csv::ConnectionType;

    #[test]
    fn test_convert_empty() {
        let converter = TriangleToDxfConverter::new();
        let (lines, texts) = converter.convert(&[]);

        assert_eq!(lines.len(), 0);
        assert_eq!(texts.len(), 0);
    }

    #[test]
    fn test_convert_single_triangle() {
        let converter = TriangleToDxfConverter::new();
        let triangle = ParsedTriangle::independent(1, 3.0, 4.0, 5.0);
        let (lines, texts) = converter.convert(&[triangle]);

        // Should have 3 lines (triangle edges)
        assert_eq!(lines.len(), 3);

        // Should have 1 text (triangle number)
        assert_eq!(texts.len(), 1);
        assert_eq!(texts[0].text, "1");
    }

    #[test]
    fn test_convert_multiple_triangles() {
        let converter = TriangleToDxfConverter::new();
        let triangles = vec![
            ParsedTriangle::independent(1, 3.0, 4.0, 5.0),
            ParsedTriangle::independent(2, 5.0, 12.0, 13.0),
        ];
        let (lines, texts) = converter.convert(&triangles);

        // Should have 6 lines (3 per triangle)
        assert_eq!(lines.len(), 6);

        // Should have 2 texts (one per triangle)
        assert_eq!(texts.len(), 2);
    }

    #[test]
    fn test_convert_invalid_triangle() {
        let converter = TriangleToDxfConverter::new();
        // Create an invalid triangle (violates triangle inequality)
        let triangle = ParsedTriangle::independent(1, 1.0, 1.0, 5.0);
        let (lines, texts) = converter.convert(&[triangle]);

        // Invalid triangles should be skipped
        assert_eq!(lines.len(), 0);
        assert_eq!(texts.len(), 0);
    }

    #[test]
    fn test_calculate_centroid() {
        let converter = TriangleToDxfConverter::new();
        let points = [(0.0, 0.0), (3.0, 0.0), (0.0, 4.0)];
        let centroid = converter.calculate_centroid(&points);

        assert!((centroid.0 - 1.0).abs() < 1e-10);
        assert!((centroid.1 - 4.0 / 3.0).abs() < 1e-10);
    }

    #[test]
    fn test_calculate_triangle_points_valid() {
        let converter = TriangleToDxfConverter::new();
        let triangle = ParsedTriangle::independent(1, 3.0, 4.0, 5.0);
        let result = converter.calculate_triangle_points(&triangle);

        assert!(result.is_ok());
        let points = result.unwrap();
        assert_eq!(points.len(), 3);
        assert_eq!(points[0], (0.0, 0.0)); // First point at origin
        assert_eq!(points[1], (5.0, 0.0)); // Second point along X-axis
        // Third point should be valid
        assert!(!points[2].0.is_nan());
        assert!(!points[2].1.is_nan());
    }

    #[test]
    fn test_calculate_triangle_points_invalid() {
        let converter = TriangleToDxfConverter::new();
        let triangle = ParsedTriangle::independent(1, f64::NAN, 4.0, 5.0);
        let result = converter.calculate_triangle_points(&triangle);

        assert!(result.is_err());
    }

    #[test]
    fn test_calculate_triangle_points_zero_sides() {
        let converter = TriangleToDxfConverter::new();
        let triangle = ParsedTriangle::independent(1, 0.0, 4.0, 5.0);
        let result = converter.calculate_triangle_points(&triangle);

        assert!(result.is_err());
    }

    #[test]
    fn test_default_converter() {
        let converter = TriangleToDxfConverter::default();
        assert_eq!(converter.line_color, 7);
        assert_eq!(converter.line_layer, "0");
        assert_eq!(converter.number_text_height, 2.0);
    }

    #[test]
    fn test_converter_new() {
        let converter = TriangleToDxfConverter::new();
        assert_eq!(converter.line_color, 7);
        assert_eq!(converter.number_text_height, 2.0);
    }
}

