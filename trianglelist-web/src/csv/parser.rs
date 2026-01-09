//! CSV Parser for Triangle Data
//!
//! Parses CSV text content containing triangle definitions.
//! Supports both minimal 4-column format and extended 6-column format.

use super::column::*;
use serde::{Deserialize, Serialize};

/// Number of header rows to skip in CSV files
const HEADER_ROWS: usize = 4;

/// Represents the connection type between triangles
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectionType {
    /// Independent triangle (no parent connection)
    Independent,
    /// Connected to parent's B side
    ToParentB,
    /// Connected to parent's C side
    ToParentC,
}

impl ConnectionType {
    /// Creates a ConnectionType from an integer value
    pub fn from_i32(value: i32) -> Self {
        match value {
            CONNECTION_TO_B => ConnectionType::ToParentB,
            CONNECTION_TO_C => ConnectionType::ToParentC,
            _ => ConnectionType::Independent,
        }
    }

    /// Converts to integer value
    pub fn to_i32(&self) -> i32 {
        match self {
            ConnectionType::Independent => CONNECTION_INDEPENDENT,
            ConnectionType::ToParentB => CONNECTION_TO_B,
            ConnectionType::ToParentC => CONNECTION_TO_C,
        }
    }
}

/// Parsed triangle data from CSV
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct ParsedTriangle {
    /// Triangle number/ID
    pub number: i32,
    /// Length of side A (shared with parent if connected)
    pub side_a: f64,
    /// Length of side B
    pub side_b: f64,
    /// Length of side C
    pub side_c: f64,
    /// Parent triangle number (-1 if independent)
    pub parent_number: i32,
    /// Connection type
    pub connection_type: ConnectionType,
}

impl ParsedTriangle {
    /// Creates an independent triangle (no parent connection)
    pub fn independent(number: i32, side_a: f64, side_b: f64, side_c: f64) -> Self {
        Self {
            number,
            side_a,
            side_b,
            side_c,
            parent_number: -1,
            connection_type: ConnectionType::Independent,
        }
    }

    /// Creates a connected triangle
    pub fn connected(
        number: i32,
        side_a: f64,
        side_b: f64,
        side_c: f64,
        parent_number: i32,
        connection_type: ConnectionType,
    ) -> Self {
        Self {
            number,
            side_a,
            side_b,
            side_c,
            parent_number,
            connection_type,
        }
    }

    /// Checks if this triangle is independent (no parent)
    pub fn is_independent(&self) -> bool {
        self.connection_type == ConnectionType::Independent
    }

    /// Validates triangle inequality (sum of any two sides > third side)
    pub fn is_valid_triangle(&self) -> bool {
        self.side_a + self.side_b > self.side_c
            && self.side_b + self.side_c > self.side_a
            && self.side_c + self.side_a > self.side_b
    }
}

/// Error type for CSV parsing
#[derive(Debug, Clone, PartialEq)]
pub enum ParseError {
    /// Not enough columns in a row
    InsufficientColumns { row: usize, expected: usize, found: usize },
    /// Failed to parse a number
    InvalidNumber { row: usize, column: &'static str, value: String },
    /// Invalid triangle (fails triangle inequality)
    InvalidTriangle { row: usize, number: i32 },
    /// Empty CSV content
    EmptyContent,
    /// Invalid parent reference
    InvalidParentReference { row: usize, number: i32, parent: i32 },
}

impl std::fmt::Display for ParseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ParseError::InsufficientColumns { row, expected, found } => {
                write!(f, "Row {}: expected at least {} columns, found {}", row, expected, found)
            }
            ParseError::InvalidNumber { row, column, value } => {
                write!(f, "Row {}: invalid {} value '{}'", row, column, value)
            }
            ParseError::InvalidTriangle { row, number } => {
                write!(f, "Row {}: triangle {} violates triangle inequality", row, number)
            }
            ParseError::EmptyContent => {
                write!(f, "CSV content is empty or contains only headers")
            }
            ParseError::InvalidParentReference { row, number, parent } => {
                write!(f, "Row {}: triangle {} references non-existent parent {}", row, number, parent)
            }
        }
    }
}

impl std::error::Error for ParseError {}

/// Result of parsing a CSV file
#[derive(Debug, Clone)]
pub struct ParseResult {
    /// Successfully parsed triangles
    pub triangles: Vec<ParsedTriangle>,
    /// Warnings (non-fatal issues)
    pub warnings: Vec<String>,
}

/// Parses CSV text content into a list of triangles
///
/// # Arguments
/// * `content` - The CSV text content
///
/// # Returns
/// A Result containing ParseResult on success, or ParseError on failure
pub fn parse_csv(content: &str) -> Result<ParseResult, ParseError> {
    let mut triangles = Vec::new();
    let mut warnings = Vec::new();

    for (line_index, line) in content.lines().enumerate() {
        // Skip header rows
        if line_index < HEADER_ROWS {
            continue;
        }

        let trimmed = line.trim();

        // Skip empty lines
        if trimmed.is_empty() {
            continue;
        }

        // Skip comment lines (starting with # or //)
        if trimmed.starts_with('#') || trimmed.starts_with("//") {
            continue;
        }

        let row_num = line_index + 1; // 1-indexed for error messages

        // Parse the row
        match parse_row(trimmed, row_num) {
            Ok(triangle) => {
                // Validate triangle inequality
                if !triangle.is_valid_triangle() {
                    return Err(ParseError::InvalidTriangle {
                        row: row_num,
                        number: triangle.number,
                    });
                }

                // Validate parent reference for connected triangles
                if !triangle.is_independent() {
                    let parent_exists = triangles.iter().any(|t: &ParsedTriangle| t.number == triangle.parent_number);
                    if !parent_exists {
                        return Err(ParseError::InvalidParentReference {
                            row: row_num,
                            number: triangle.number,
                            parent: triangle.parent_number,
                        });
                    }
                }

                triangles.push(triangle);
            }
            Err(e) => return Err(e),
        }
    }

    if triangles.is_empty() {
        return Err(ParseError::EmptyContent);
    }

    // Check for orphaned triangles (connected but parent not in list)
    for triangle in &triangles {
        if !triangle.is_independent() {
            let parent_exists = triangles.iter().any(|t| t.number == triangle.parent_number);
            if !parent_exists {
                warnings.push(format!(
                    "Warning: Triangle {} references parent {} which is not in the list",
                    triangle.number, triangle.parent_number
                ));
            }
        }
    }

    Ok(ParseResult { triangles, warnings })
}

/// Parses a single CSV row into a ParsedTriangle
fn parse_row(line: &str, row_num: usize) -> Result<ParsedTriangle, ParseError> {
    let columns: Vec<&str> = line.split(',').map(|s| s.trim()).collect();

    // Check minimum columns
    if columns.len() < MIN_COLUMNS_BASIC {
        return Err(ParseError::InsufficientColumns {
            row: row_num,
            expected: MIN_COLUMNS_BASIC,
            found: columns.len(),
        });
    }

    // Parse required fields
    let number = parse_i32(columns[COL_NUMBER], row_num, "number")?;
    let side_a = parse_f64(columns[COL_SIDE_A], row_num, "side_a")?;
    let side_b = parse_f64(columns[COL_SIDE_B], row_num, "side_b")?;
    let side_c = parse_f64(columns[COL_SIDE_C], row_num, "side_c")?;

    // Check if extended format with connection info
    if columns.len() >= MIN_COLUMNS_CONNECTED {
        let parent_number = parse_i32(columns[COL_PARENT_NUMBER], row_num, "parent_number")?;
        let connection_type_val = parse_i32(columns[COL_CONNECTION_TYPE], row_num, "connection_type")?;
        let connection_type = ConnectionType::from_i32(connection_type_val);

        if parent_number == -1 || connection_type == ConnectionType::Independent {
            Ok(ParsedTriangle::independent(number, side_a, side_b, side_c))
        } else {
            Ok(ParsedTriangle::connected(
                number,
                side_a,
                side_b,
                side_c,
                parent_number,
                connection_type,
            ))
        }
    } else {
        // Basic format - independent triangle
        Ok(ParsedTriangle::independent(number, side_a, side_b, side_c))
    }
}

/// Parse an i32 from a string
fn parse_i32(s: &str, row: usize, column: &'static str) -> Result<i32, ParseError> {
    s.parse::<i32>().map_err(|_| ParseError::InvalidNumber {
        row,
        column,
        value: s.to_string(),
    })
}

/// Parse an f64 from a string
fn parse_f64(s: &str, row: usize, column: &'static str) -> Result<f64, ParseError> {
    s.parse::<f64>().map_err(|_| ParseError::InvalidNumber {
        row,
        column,
        value: s.to_string(),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_4_column_format() {
        let csv = r#"Header1
Header2
Header3
Header4
1, 6.0, 5.0, 4.0
2, 5.0, 4.0, 3.0
3, 3.0, 4.0, 5.0"#;

        let result = parse_csv(csv).unwrap();
        assert_eq!(result.triangles.len(), 3);

        let t1 = &result.triangles[0];
        assert_eq!(t1.number, 1);
        assert_eq!(t1.side_a, 6.0);
        assert_eq!(t1.side_b, 5.0);
        assert_eq!(t1.side_c, 4.0);
        assert!(t1.is_independent());
    }

    #[test]
    fn test_parse_6_column_format() {
        let csv = r#"番号
辺A
辺B
辺C
1, 6.0, 5.0, 4.0, -1, -1
2, 5.0, 4.0, 3.0, 1, 1
3, 4.0, 3.5, 3.0, 1, 2"#;

        let result = parse_csv(csv).unwrap();
        assert_eq!(result.triangles.len(), 3);

        // First triangle is independent
        let t1 = &result.triangles[0];
        assert_eq!(t1.number, 1);
        assert!(t1.is_independent());

        // Second triangle connects to parent 1's B side
        let t2 = &result.triangles[1];
        assert_eq!(t2.number, 2);
        assert_eq!(t2.parent_number, 1);
        assert_eq!(t2.connection_type, ConnectionType::ToParentB);
        assert_eq!(t2.side_a, 5.0); // Should match parent's B side

        // Third triangle connects to parent 1's C side
        let t3 = &result.triangles[2];
        assert_eq!(t3.number, 3);
        assert_eq!(t3.parent_number, 1);
        assert_eq!(t3.connection_type, ConnectionType::ToParentC);
        assert_eq!(t3.side_a, 4.0); // Should match parent's C side
    }

    #[test]
    fn test_skip_comments_and_empty_lines() {
        let csv = r#"Header1
Header2
Header3
Header4
# This is a comment
1, 6.0, 5.0, 4.0

// Another comment
2, 5.0, 4.0, 3.0"#;

        let result = parse_csv(csv).unwrap();
        assert_eq!(result.triangles.len(), 2);
    }

    #[test]
    fn test_invalid_triangle_inequality() {
        let csv = r#"Header1
Header2
Header3
Header4
1, 1.0, 1.0, 10.0"#;

        let result = parse_csv(csv);
        assert!(matches!(result, Err(ParseError::InvalidTriangle { .. })));
    }

    #[test]
    fn test_invalid_parent_reference() {
        let csv = r#"Header1
Header2
Header3
Header4
1, 6.0, 5.0, 4.0, -1, -1
2, 5.0, 4.0, 3.0, 99, 1"#;

        let result = parse_csv(csv);
        assert!(matches!(result, Err(ParseError::InvalidParentReference { .. })));
    }

    #[test]
    fn test_empty_content() {
        let csv = r#"Header1
Header2
Header3
Header4"#;

        let result = parse_csv(csv);
        assert!(matches!(result, Err(ParseError::EmptyContent)));
    }

    #[test]
    fn test_insufficient_columns() {
        let csv = r#"Header1
Header2
Header3
Header4
1, 6.0, 5.0"#;

        let result = parse_csv(csv);
        assert!(matches!(result, Err(ParseError::InsufficientColumns { .. })));
    }

    #[test]
    fn test_connection_type_conversion() {
        assert_eq!(ConnectionType::from_i32(-1), ConnectionType::Independent);
        assert_eq!(ConnectionType::from_i32(1), ConnectionType::ToParentB);
        assert_eq!(ConnectionType::from_i32(2), ConnectionType::ToParentC);
        assert_eq!(ConnectionType::from_i32(99), ConnectionType::Independent); // Unknown defaults to Independent
    }

    #[test]
    fn test_triangle_validity() {
        // Valid 3-4-5 right triangle
        let valid = ParsedTriangle::independent(1, 3.0, 4.0, 5.0);
        assert!(valid.is_valid_triangle());

        // Invalid triangle (violates inequality)
        let invalid = ParsedTriangle::independent(2, 1.0, 1.0, 10.0);
        assert!(!invalid.is_valid_triangle());
    }
}
