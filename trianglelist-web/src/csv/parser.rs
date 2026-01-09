//! CSV Parser for Triangle Data
//!
//! Parses CSV text content containing triangle definitions.
//! Supports both minimal 4-column format and extended 6-column format.

use super::column::*;
use serde::{Deserialize, Serialize};

/// Number of header rows to skip in CSV files
const HEADER_ROWS: usize = 4;

/// Metadata line prefixes to skip (from ヘロン calculation software)
const METADATA_PREFIXES: &[&str] = &[
    "ListAngle",
    "ListScale",
    "TextSize",
    "ListRotation",
    "ListOffset",
];

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

        // Skip metadata lines (ListAngle, ListScale, TextSize, etc.)
        if METADATA_PREFIXES.iter().any(|prefix| trimmed.starts_with(prefix)) {
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

        // Parent number 0 or -1 means independent
        if parent_number <= 0 || connection_type == ConnectionType::Independent {
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
    fn test_real_world_csv_with_metadata_lines() {
        // Real-world CSV format from ヘロン calculation software
        // Has metadata lines at the end (ListAngle, ListScale, TextSize)
        // Parent number 0 means independent (not -1)
        let csv = r#"koujiname,
rosenname, 新規路線
gyousyaname,
zumennum, 1/1
1,6.42,5.93,8.7,0,3,,2.000609,4.2508297,false,4,0,0,0,1,1,3,1,1,2,false,false,89.53419,0.0,0.0,89.53419,0,false
2,8.7,8.18,1.1,1,2,,2.347401,2.0356534,false,4,0,0,0,3,3,1,2,0,2,false,false,46.56686,0.0,0.0,89.53419,0,false
3,8.18,8.05,1.2,2,1,,3.0649095,1.7833027,false,4,0,0,0,3,3,1,1,0,2,false,false,53.153996,1.076046,-0.22830561,89.53419,0,false
ListAngle, 89.53419
ListScale, 1.0
TextSize, 33.0
"#;

        let result = parse_csv(csv);
        assert!(result.is_ok(), "Parse failed: {:?}", result.err());

        let parsed = result.unwrap();
        assert_eq!(parsed.triangles.len(), 3);

        // First triangle is independent (parent=0)
        let t1 = &parsed.triangles[0];
        assert_eq!(t1.number, 1);
        assert_eq!(t1.side_a, 6.42);
        assert_eq!(t1.side_b, 5.93);
        assert_eq!(t1.side_c, 8.7);
        assert!(t1.is_independent());

        // Second triangle connects to parent 1
        let t2 = &parsed.triangles[1];
        assert_eq!(t2.number, 2);
        assert_eq!(t2.parent_number, 1);
        assert_eq!(t2.connection_type, ConnectionType::ToParentC);
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

    #[test]
    fn test_minimal_csv_compatibility() {
        // Test compatibility with minimal.csv format
        let csv = r#"koujiname, 最小形式テスト
rosenname, テスト路線
gyousyaname, テスト業者
zumennum, 1
1, 6.0, 5.0, 4.0
2, 5.5, 4.5, 3.5
3, 4.0, 3.5, 3.0"#;

        let result = parse_csv(csv).unwrap();
        assert_eq!(result.triangles.len(), 3);

        // Verify first triangle
        let t1 = &result.triangles[0];
        assert_eq!(t1.number, 1);
        assert_eq!(t1.side_a, 6.0);
        assert_eq!(t1.side_b, 5.0);
        assert_eq!(t1.side_c, 4.0);
        assert!(t1.is_independent());

        // Verify second triangle
        let t2 = &result.triangles[1];
        assert_eq!(t2.number, 2);
        assert_eq!(t2.side_a, 5.5);
        assert_eq!(t2.side_b, 4.5);
        assert_eq!(t2.side_c, 3.5);
        assert!(t2.is_independent());

        // Verify third triangle
        let t3 = &result.triangles[2];
        assert_eq!(t3.number, 3);
        assert_eq!(t3.side_a, 4.0);
        assert_eq!(t3.side_b, 3.5);
        assert_eq!(t3.side_c, 3.0);
        assert!(t3.is_independent());
    }

    #[test]
    fn test_connected_csv_compatibility() {
        // Test compatibility with connected.csv format
        let csv = r#"koujiname, 接続形式テスト
rosenname, テスト路線
gyousyaname, テスト業者
zumennum, 1
1, 6.0, 5.0, 4.0, -1, -1
2, 5.0, 4.0, 3.0, 1, 1
3, 4.0, 3.5, 3.0, 1, 2
4, 4.0, 3.5, 3.0, 2, 1
5, 3.0, 2.5, 2.0, 2, 2
6, 3.5, 3.0, 2.5, 3, 1
7, 3.0, 2.5, 2.0, 3, 2"#;

        let result = parse_csv(csv).unwrap();
        assert_eq!(result.triangles.len(), 7);

        // Verify first triangle (independent)
        let t1 = &result.triangles[0];
        assert_eq!(t1.number, 1);
        assert!(t1.is_independent());

        // Verify second triangle (connects to parent 1's B side)
        let t2 = &result.triangles[1];
        assert_eq!(t2.number, 2);
        assert_eq!(t2.parent_number, 1);
        assert_eq!(t2.connection_type, ConnectionType::ToParentB);
        assert_eq!(t2.side_a, 5.0); // Should match parent's B side

        // Verify third triangle (connects to parent 1's C side)
        let t3 = &result.triangles[2];
        assert_eq!(t3.number, 3);
        assert_eq!(t3.parent_number, 1);
        assert_eq!(t3.connection_type, ConnectionType::ToParentC);
        assert_eq!(t3.side_a, 4.0); // Should match parent's C side

        // Verify fourth triangle (connects to parent 2's B side)
        let t4 = &result.triangles[3];
        assert_eq!(t4.number, 4);
        assert_eq!(t4.parent_number, 2);
        assert_eq!(t4.connection_type, ConnectionType::ToParentB);
        assert_eq!(t4.side_a, 4.0); // Should match parent t2's B side

        // Verify fifth triangle (connects to parent 2's C side)
        let t5 = &result.triangles[4];
        assert_eq!(t5.number, 5);
        assert_eq!(t5.parent_number, 2);
        assert_eq!(t5.connection_type, ConnectionType::ToParentC);
        assert_eq!(t5.side_a, 3.0); // Should match parent t2's C side

        // Verify sixth triangle (connects to parent 3's B side)
        let t6 = &result.triangles[5];
        assert_eq!(t6.number, 6);
        assert_eq!(t6.parent_number, 3);
        assert_eq!(t6.connection_type, ConnectionType::ToParentB);
        assert_eq!(t6.side_a, 3.5); // Should match parent t3's B side

        // Verify seventh triangle (connects to parent 3's C side)
        let t7 = &result.triangles[6];
        assert_eq!(t7.number, 7);
        assert_eq!(t7.parent_number, 3);
        assert_eq!(t7.connection_type, ConnectionType::ToParentC);
        assert_eq!(t7.side_a, 3.0); // Should match parent t3's C side

        // Verify all triangles are valid
        for triangle in &result.triangles {
            assert!(triangle.is_valid_triangle(), "Triangle {} should be valid", triangle.number);
        }
    }
}
