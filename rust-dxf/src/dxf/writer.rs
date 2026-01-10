//! DXF file writer
//!
//! This module provides functionality to generate DXF text output
//! from DxfLine, DxfText, DxfCircle, and DxfLwPolyline entities.

use std::fmt::Write;

use crate::dxf::entities::{DxfCircle, DxfLine, DxfLwPolyline, DxfText};

/// DXF file writer
///
/// Generates DXF format text from line and text entities.
pub struct DxfWriter;

impl DxfWriter {
    /// Creates a new DxfWriter instance
    pub fn new() -> Self {
        Self
    }

    /// Generates DXF text from lines and texts
    ///
    /// # Arguments
    /// * `lines` - List of DxfLine entities
    /// * `texts` - List of DxfText entities
    ///
    /// # Returns
    /// DXF format string
    pub fn write(&self, lines: &[DxfLine], texts: &[DxfText]) -> String {
        let mut output = String::new();

        // Header section
        self.write_header(&mut output);

        // Entities section
        self.write_entities_start(&mut output);

        // Write lines
        for line in lines {
            self.write_line(&mut output, line);
        }

        // Write texts
        for text in texts {
            self.write_text(&mut output, text);
        }

        // End entities and file
        self.write_end(&mut output);

        output
    }

    /// Writes the DXF header section
    fn write_header(&self, output: &mut String) {
        writeln!(output, "0\nSECTION").unwrap();
        writeln!(output, "2\nHEADER").unwrap();
        writeln!(output, "9\n$ACADVER").unwrap();
        writeln!(output, "1\nAC1015").unwrap();
        writeln!(output, "9\n$INSUNITS").unwrap();
        writeln!(output, "70\n4").unwrap(); // 4 = Millimeters
        writeln!(output, "0\nENDSEC").unwrap();
    }

    /// Writes the start of the entities section
    fn write_entities_start(&self, output: &mut String) {
        writeln!(output, "0\nSECTION").unwrap();
        writeln!(output, "2\nENTITIES").unwrap();
    }

    /// Writes a LINE entity
    fn write_line(&self, output: &mut String, line: &DxfLine) {
        writeln!(output, "0\nLINE").unwrap();
        writeln!(output, "8\n{}", line.layer).unwrap();
        writeln!(output, "62\n{}", line.color).unwrap();
        writeln!(output, "10\n{}", line.x1).unwrap();
        writeln!(output, "20\n{}", line.y1).unwrap();
        writeln!(output, "11\n{}", line.x2).unwrap();
        writeln!(output, "21\n{}", line.y2).unwrap();
    }

    /// Writes a TEXT entity
    fn write_text(&self, output: &mut String, text: &DxfText) {
        use crate::dxf::entities::{HorizontalAlignment, VerticalAlignment};

        writeln!(output, "0\nTEXT").unwrap();
        writeln!(output, "8\n{}", text.layer).unwrap();
        writeln!(output, "62\n{}", text.color).unwrap();
        writeln!(output, "10\n{}", text.x).unwrap();
        writeln!(output, "20\n{}", text.y).unwrap();
        writeln!(output, "40\n{}", text.height).unwrap();
        writeln!(output, "1\n{}", text.text).unwrap();
        writeln!(output, "50\n{}", text.rotation).unwrap();

        // For non-default alignment, specify second alignment point (group 11/21)
        // DXF spec: when alignment != Left/Baseline, use group 11/21 for alignment
        let needs_second_point = text.align_h != HorizontalAlignment::Left
            || text.align_v != VerticalAlignment::Baseline;

        if needs_second_point {
            writeln!(output, "11\n{}", text.x).unwrap();
            writeln!(output, "21\n{}", text.y).unwrap();
        }

        writeln!(output, "72\n{}", text.align_h as i32).unwrap();
        writeln!(output, "73\n{}", text.align_v as i32).unwrap();
    }

    /// Writes a CIRCLE entity
    fn write_circle(&self, output: &mut String, circle: &DxfCircle) {
        writeln!(output, "0\nCIRCLE").unwrap();
        writeln!(output, "8\n{}", circle.layer).unwrap();
        writeln!(output, "62\n{}", circle.color).unwrap();
        writeln!(output, "10\n{}", circle.x).unwrap();
        writeln!(output, "20\n{}", circle.y).unwrap();
        writeln!(output, "30\n0").unwrap(); // Z coordinate
        writeln!(output, "40\n{}", circle.radius).unwrap();
    }

    /// Writes a LWPOLYLINE entity
    fn write_lwpolyline(&self, output: &mut String, polyline: &DxfLwPolyline) {
        if polyline.vertices.is_empty() {
            return; // Skip empty polylines
        }

        writeln!(output, "0\nLWPOLYLINE").unwrap();
        writeln!(output, "8\n{}", polyline.layer).unwrap();
        writeln!(output, "62\n{}", polyline.color).unwrap();
        writeln!(output, "90\n{}", polyline.vertices.len()).unwrap(); // Number of vertices
        writeln!(output, "70\n{}", if polyline.closed { 1 } else { 0 }).unwrap(); // Closed flag

        // Write each vertex
        for (x, y) in &polyline.vertices {
            writeln!(output, "10\n{}", x).unwrap();
            writeln!(output, "20\n{}", y).unwrap();
        }
    }

    /// Generates DXF text from all entity types
    ///
    /// # Arguments
    /// * `lines` - List of DxfLine entities
    /// * `texts` - List of DxfText entities
    /// * `circles` - List of DxfCircle entities
    /// * `polylines` - List of DxfLwPolyline entities
    ///
    /// # Returns
    /// DXF format string
    pub fn write_all(
        &self,
        lines: &[DxfLine],
        texts: &[DxfText],
        circles: &[DxfCircle],
        polylines: &[DxfLwPolyline],
    ) -> String {
        let mut output = String::new();

        // Header section
        self.write_header(&mut output);

        // Entities section
        self.write_entities_start(&mut output);

        // Write lines
        for line in lines {
            self.write_line(&mut output, line);
        }

        // Write texts
        for text in texts {
            self.write_text(&mut output, text);
        }

        // Write circles
        for circle in circles {
            self.write_circle(&mut output, circle);
        }

        // Write polylines
        for polyline in polylines {
            self.write_lwpolyline(&mut output, polyline);
        }

        // End entities and file
        self.write_end(&mut output);

        output
    }

    /// Writes the end of the entities section and EOF
    fn write_end(&self, output: &mut String) {
        writeln!(output, "0\nENDSEC").unwrap();
        writeln!(output, "0\nEOF").unwrap();
    }
}

impl Default for DxfWriter {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dxf::entities::{HorizontalAlignment, VerticalAlignment};

    #[test]
    fn test_write_empty() {
        let writer = DxfWriter::new();
        let output = writer.write(&[], &[]);

        assert!(output.contains("SECTION"));
        assert!(output.contains("HEADER"));
        assert!(output.contains("$ACADVER"));
        assert!(output.contains("AC1015"));
        assert!(output.contains("$INSUNITS"));
        assert!(output.contains("ENTITIES"));
        assert!(output.contains("ENDSEC"));
        assert!(output.contains("EOF"));
    }

    #[test]
    fn test_write_single_line() {
        let writer = DxfWriter::new();
        let lines = vec![DxfLine::new(0.0, 0.0, 100.0, 100.0)];
        let output = writer.write(&lines, &[]);

        assert!(output.contains("LINE"));
        assert!(output.contains("10\n0"));
        assert!(output.contains("20\n0"));
        assert!(output.contains("11\n100"));
        assert!(output.contains("21\n100"));
    }

    #[test]
    fn test_write_line_with_style() {
        let writer = DxfWriter::new();
        let lines = vec![DxfLine::with_style(1.0, 2.0, 3.0, 4.0, 5, "TestLayer")];
        let output = writer.write(&lines, &[]);

        assert!(output.contains("LINE"));
        assert!(output.contains("8\nTestLayer"));
        assert!(output.contains("62\n5"));
    }

    #[test]
    fn test_write_single_text() {
        let writer = DxfWriter::new();
        let texts = vec![DxfText::new(50.0, 50.0, "Hello World")];
        let output = writer.write(&[], &texts);

        assert!(output.contains("TEXT"));
        assert!(output.contains("10\n50"));
        assert!(output.contains("20\n50"));
        assert!(output.contains("1\nHello World"));
    }

    #[test]
    fn test_write_text_with_builder() {
        let writer = DxfWriter::new();
        let texts = vec![DxfText::new(10.0, 20.0, "Styled")
            .height(2.5)
            .rotation(45.0)
            .color(3)
            .align_h(HorizontalAlignment::Center)
            .align_v(VerticalAlignment::Middle)
            .layer("TextLayer")];
        let output = writer.write(&[], &texts);

        assert!(output.contains("TEXT"));
        assert!(output.contains("8\nTextLayer"));
        assert!(output.contains("62\n3"));
        assert!(output.contains("40\n2.5"));
        assert!(output.contains("50\n45"));
        assert!(output.contains("72\n1"));
        assert!(output.contains("73\n2"));
        // Non-default alignment should have second alignment point
        assert!(output.contains("11\n10"));
        assert!(output.contains("21\n20"));
    }

    #[test]
    fn test_write_text_default_alignment_no_second_point() {
        let writer = DxfWriter::new();
        // Default alignment (Left/Baseline) should NOT have group 11/21
        let texts = vec![DxfText::new(30.0, 40.0, "Default")];
        let output = writer.write(&[], &texts);

        assert!(output.contains("TEXT"));
        assert!(output.contains("10\n30"));
        assert!(output.contains("20\n40"));
        // Should NOT contain second alignment point for default alignment
        let text_section_start = output.find("1\nDefault").unwrap();
        let after_text = &output[text_section_start..];
        assert!(!after_text.contains("11\n30"));
    }

    #[test]
    fn test_write_text_vertical_alignment_has_second_point() {
        let writer = DxfWriter::new();
        // Even Left + Middle should have second alignment point
        let texts = vec![DxfText::new(50.0, 60.0, "Vertical")
            .align_h(HorizontalAlignment::Left)
            .align_v(VerticalAlignment::Middle)];
        let output = writer.write(&[], &texts);

        // Should have second alignment point because vertical is not Baseline
        assert!(output.contains("11\n50"));
        assert!(output.contains("21\n60"));
    }

    #[test]
    fn test_write_multiple_entities() {
        let writer = DxfWriter::new();
        let lines = vec![
            DxfLine::new(0.0, 0.0, 10.0, 10.0),
            DxfLine::new(10.0, 10.0, 20.0, 20.0),
        ];
        let texts = vec![
            DxfText::new(5.0, 5.0, "Point 1"),
            DxfText::new(15.0, 15.0, "Point 2"),
        ];
        let output = writer.write(&lines, &texts);

        // Count occurrences of LINE
        let line_count = output.matches("0\nLINE\n").count();
        assert_eq!(line_count, 2);

        // Count occurrences of TEXT
        let text_count = output.matches("0\nTEXT\n").count();
        assert_eq!(text_count, 2);
    }

    #[test]
    fn test_header_content() {
        let writer = DxfWriter::new();
        let output = writer.write(&[], &[]);

        // Check header structure
        assert!(output.starts_with("0\nSECTION\n2\nHEADER\n"));
        assert!(output.contains("9\n$ACADVER\n1\nAC1015\n"));
        assert!(output.contains("9\n$INSUNITS\n70\n4\n"));
    }

    #[test]
    fn test_default_implementation() {
        let writer = DxfWriter::default();
        let output = writer.write(&[], &[]);
        assert!(output.contains("EOF"));
    }

    #[test]
    fn test_write_single_circle() {
        let writer = DxfWriter::new();
        let circles = vec![DxfCircle::new(50.0, 50.0, 10.0)];
        let output = writer.write_all(&[], &[], &circles, &[]);

        assert!(output.contains("CIRCLE"));
        assert!(output.contains("10\n50"));
        assert!(output.contains("20\n50"));
        assert!(output.contains("30\n0")); // Z coordinate
        assert!(output.contains("40\n10")); // Radius
    }

    #[test]
    fn test_write_circle_with_style() {
        let writer = DxfWriter::new();
        let circles = vec![DxfCircle::new(100.0, 200.0, 25.0)
            .color(3)
            .layer("CircleLayer")];
        let output = writer.write_all(&[], &[], &circles, &[]);

        assert!(output.contains("CIRCLE"));
        assert!(output.contains("8\nCircleLayer"));
        assert!(output.contains("62\n3"));
        assert!(output.contains("40\n25")); // Radius
    }

    #[test]
    fn test_write_lwpolyline_open() {
        let writer = DxfWriter::new();
        let polylines = vec![DxfLwPolyline::new(vec![
            (0.0, 0.0),
            (10.0, 0.0),
            (10.0, 10.0),
        ])];
        let output = writer.write_all(&[], &[], &[], &polylines);

        assert!(output.contains("LWPOLYLINE"));
        assert!(output.contains("90\n3")); // 3 vertices
        assert!(output.contains("70\n0")); // Open (not closed)
    }

    #[test]
    fn test_write_lwpolyline_closed() {
        let writer = DxfWriter::new();
        let polylines = vec![DxfLwPolyline::closed(vec![
            (0.0, 0.0),
            (10.0, 0.0),
            (10.0, 10.0),
            (0.0, 10.0),
        ])];
        let output = writer.write_all(&[], &[], &[], &polylines);

        assert!(output.contains("LWPOLYLINE"));
        assert!(output.contains("90\n4")); // 4 vertices
        assert!(output.contains("70\n1")); // Closed
    }

    #[test]
    fn test_write_lwpolyline_with_style() {
        let writer = DxfWriter::new();
        let polylines = vec![DxfLwPolyline::new(vec![(0.0, 0.0), (100.0, 100.0)])
            .color(5)
            .layer("OutlineLayer")];
        let output = writer.write_all(&[], &[], &[], &polylines);

        assert!(output.contains("8\nOutlineLayer"));
        assert!(output.contains("62\n5"));
    }

    #[test]
    fn test_write_lwpolyline_empty_skipped() {
        let writer = DxfWriter::new();
        let polylines = vec![DxfLwPolyline::new(vec![])]; // Empty
        let output = writer.write_all(&[], &[], &[], &polylines);

        // Should not contain LWPOLYLINE for empty vertices
        assert!(!output.contains("LWPOLYLINE"));
    }

    #[test]
    fn test_write_all_mixed_entities() {
        let writer = DxfWriter::new();
        let lines = vec![DxfLine::new(0.0, 0.0, 10.0, 10.0)];
        let texts = vec![DxfText::new(5.0, 5.0, "Label")];
        let circles = vec![DxfCircle::new(20.0, 20.0, 5.0)];
        let polylines = vec![DxfLwPolyline::closed(vec![
            (30.0, 30.0),
            (40.0, 30.0),
            (40.0, 40.0),
        ])];

        let output = writer.write_all(&lines, &texts, &circles, &polylines);

        // Count each entity type
        assert_eq!(output.matches("0\nLINE\n").count(), 1);
        assert_eq!(output.matches("0\nTEXT\n").count(), 1);
        assert_eq!(output.matches("0\nCIRCLE\n").count(), 1);
        assert_eq!(output.matches("0\nLWPOLYLINE\n").count(), 1);
    }
}
