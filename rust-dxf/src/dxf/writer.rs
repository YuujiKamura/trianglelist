//! DXF file writer
//!
//! This module provides functionality to generate DXF text output
//! from DxfLine and DxfText entities.

use crate::dxf::entities::{DxfLine, DxfText};

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
        output.push_str("0\n");
        output.push_str("SECTION\n");
        output.push_str("2\n");
        output.push_str("HEADER\n");
        output.push_str("9\n");
        output.push_str("$ACADVER\n");
        output.push_str("1\n");
        output.push_str("AC1015\n");
        output.push_str("9\n");
        output.push_str("$INSUNITS\n");
        output.push_str("70\n");
        output.push_str("4\n"); // 4 = Millimeters
        output.push_str("0\n");
        output.push_str("ENDSEC\n");
    }

    /// Writes the start of the entities section
    fn write_entities_start(&self, output: &mut String) {
        output.push_str("0\n");
        output.push_str("SECTION\n");
        output.push_str("2\n");
        output.push_str("ENTITIES\n");
    }

    /// Writes a LINE entity
    fn write_line(&self, output: &mut String, line: &DxfLine) {
        output.push_str("0\n");
        output.push_str("LINE\n");
        output.push_str("8\n");
        output.push_str(&line.layer);
        output.push('\n');
        output.push_str("62\n");
        output.push_str(&line.color.to_string());
        output.push('\n');
        output.push_str("10\n");
        output.push_str(&line.x1.to_string());
        output.push('\n');
        output.push_str("20\n");
        output.push_str(&line.y1.to_string());
        output.push('\n');
        output.push_str("11\n");
        output.push_str(&line.x2.to_string());
        output.push('\n');
        output.push_str("21\n");
        output.push_str(&line.y2.to_string());
        output.push('\n');
    }

    /// Writes a TEXT entity
    fn write_text(&self, output: &mut String, text: &DxfText) {
        output.push_str("0\n");
        output.push_str("TEXT\n");
        output.push_str("8\n");
        output.push_str(&text.layer);
        output.push('\n');
        output.push_str("62\n");
        output.push_str(&text.color.to_string());
        output.push('\n');
        output.push_str("10\n");
        output.push_str(&text.x.to_string());
        output.push('\n');
        output.push_str("20\n");
        output.push_str(&text.y.to_string());
        output.push('\n');
        output.push_str("40\n");
        output.push_str(&text.height.to_string());
        output.push('\n');
        output.push_str("1\n");
        output.push_str(&text.text);
        output.push('\n');
        output.push_str("50\n");
        output.push_str(&text.rotation.to_string());
        output.push('\n');
        output.push_str("72\n");
        output.push_str(&text.align_h.to_string());
        output.push('\n');
        output.push_str("73\n");
        output.push_str(&text.align_v.to_string());
        output.push('\n');
    }

    /// Writes the end of the entities section and EOF
    fn write_end(&self, output: &mut String) {
        output.push_str("0\n");
        output.push_str("ENDSEC\n");
        output.push_str("0\n");
        output.push_str("EOF\n");
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
    fn test_write_text_with_style() {
        let writer = DxfWriter::new();
        let texts = vec![DxfText::with_style(
            10.0, 20.0, "Styled", 2.5, 45.0, 3, 1, 2, "TextLayer",
        )];
        let output = writer.write(&[], &texts);

        assert!(output.contains("TEXT"));
        assert!(output.contains("8\nTextLayer"));
        assert!(output.contains("62\n3"));
        assert!(output.contains("40\n2.5"));
        assert!(output.contains("50\n45"));
        assert!(output.contains("72\n1"));
        assert!(output.contains("73\n2"));
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
        let line_count = output.matches("\n0\nLINE\n").count();
        assert_eq!(line_count, 2);

        // Count occurrences of TEXT
        let text_count = output.matches("\n0\nTEXT\n").count();
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
}
