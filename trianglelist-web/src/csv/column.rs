//! CSV Column Definitions
//!
//! Defines constants for CSV column indices and minimum required columns.

/// Column index for triangle number (ID)
pub const COL_NUMBER: usize = 0;

/// Column index for side A length
pub const COL_SIDE_A: usize = 1;

/// Column index for side B length
pub const COL_SIDE_B: usize = 2;

/// Column index for side C length
pub const COL_SIDE_C: usize = 3;

/// Column index for parent triangle number
pub const COL_PARENT_NUMBER: usize = 4;

/// Column index for connection type
pub const COL_CONNECTION_TYPE: usize = 5;

/// Minimum columns required for basic (independent) triangles
pub const MIN_COLUMNS_BASIC: usize = 4;

/// Minimum columns required for connected triangles
pub const MIN_COLUMNS_CONNECTED: usize = 6;

/// Connection type: independent (no parent)
pub const CONNECTION_INDEPENDENT: i32 = -1;

/// Connection type: connect to parent's B side
pub const CONNECTION_TO_B: i32 = 1;

/// Connection type: connect to parent's C side
pub const CONNECTION_TO_C: i32 = 2;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_column_indices() {
        assert_eq!(COL_NUMBER, 0);
        assert_eq!(COL_SIDE_A, 1);
        assert_eq!(COL_SIDE_B, 2);
        assert_eq!(COL_SIDE_C, 3);
        assert_eq!(COL_PARENT_NUMBER, 4);
        assert_eq!(COL_CONNECTION_TYPE, 5);
    }

    #[test]
    fn test_min_columns() {
        assert_eq!(MIN_COLUMNS_BASIC, 4);
        assert_eq!(MIN_COLUMNS_CONNECTED, 6);
    }

    #[test]
    fn test_connection_types() {
        assert_eq!(CONNECTION_INDEPENDENT, -1);
        assert_eq!(CONNECTION_TO_B, 1);
        assert_eq!(CONNECTION_TO_C, 2);
    }
}
