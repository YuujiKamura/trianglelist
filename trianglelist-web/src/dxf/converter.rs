//! Triangle to DXF converter
//!
//! Converts ParsedTriangle data to DXF entities (lines and texts)

use crate::csv::ParsedTriangle;
use dxf::dxf::entities::{DxfLine, DxfText, HorizontalAlignment, VerticalAlignment};

/// Converts triangles to DXF entities
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
    pub fn new() -> Self {
        Self::default()
    }

    /// Converts triangles to DXF lines and texts
    ///
    /// # Arguments
    /// * `triangles` - List of parsed triangles
    ///
    /// # Returns
    /// Tuple of (lines, texts) for DXF output
    pub fn convert(&self, triangles: &[ParsedTriangle]) -> (Vec<DxfLine>, Vec<DxfText>) {
        let mut lines = Vec::new();
        let mut texts = Vec::new();

        for triangle in triangles {
            // Calculate triangle points (simplified - using side lengths)
            // This is a placeholder implementation
            // In a real scenario, you'd use the actual calculated coordinates
            let points = self.calculate_triangle_points(triangle);

            // Add triangle edges as lines
            lines.push(DxfLine::new(
                points[0].0, points[0].1,
                points[1].0, points[1].1,
            ).color(self.line_color).layer(&self.line_layer));

            lines.push(DxfLine::new(
                points[1].0, points[1].1,
                points[2].0, points[2].1,
            ).color(self.line_color).layer(&self.line_layer));

            lines.push(DxfLine::new(
                points[2].0, points[2].1,
                points[0].0, points[0].1,
            ).color(self.line_color).layer(&self.line_layer));

            // Add triangle number text at centroid
            let centroid = self.calculate_centroid(&points);
            texts.push(DxfText::new(
                centroid.0, centroid.1,
                &triangle.number.to_string(),
            )
                .height(self.number_text_height)
                .color(self.number_color)
                .align_h(HorizontalAlignment::Center)
                .align_v(VerticalAlignment::Middle)
                .layer(&self.number_layer));

            // Add side length texts (optional - can be enabled later)
            // For now, we'll skip side length texts to keep it simple
        }

        (lines, texts)
    }

    /// Calculates triangle points from side lengths
    /// This is a simplified implementation - assumes first point at origin
    /// and second point along X-axis
    fn calculate_triangle_points(&self, triangle: &ParsedTriangle) -> [(f64, f64); 3] {
        // Point 1: Origin
        let p1 = (0.0, 0.0);

        // Point 2: Along X-axis at distance side_c
        let p2 = (triangle.side_c, 0.0);

        // Point 3: Calculate using law of cosines
        // cos(A) = (b² + c² - a²) / (2bc)
        let cos_a = (triangle.side_b * triangle.side_b
            + triangle.side_c * triangle.side_c
            - triangle.side_a * triangle.side_a)
            / (2.0 * triangle.side_b * triangle.side_c);
        let sin_a = (1.0 - cos_a * cos_a).sqrt();

        let p3_x = triangle.side_b * cos_a;
        let p3_y = triangle.side_b * sin_a;
        let p3 = (p3_x, p3_y);

        [p1, p2, p3]
    }

    /// Calculates centroid of triangle points
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
}

