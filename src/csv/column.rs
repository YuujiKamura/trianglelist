//! CSV column definitions for triangle data
//!
//! This module defines column indices for parsing triangle CSV files.
//!
//! # CSV Formats
//!
//! - **Minimal format**: NUMBER, LENGTH_A, LENGTH_B, LENGTH_C (4 columns)
//! - **With connection**: + PARENT_NUMBER, CONNECTION_TYPE (6 columns)
//! - **Full format**: 28 columns (includes layout information)
//!
//! # Connection Specification
//!
//! Triangle edges have specific roles:
//! - **A edge**: Shared edge with parent (connection edge). Matches parent's edge when created
//! - **B/C edges**: Free edges. Other triangles can connect here
//!
//! CONNECTION_TYPE values:
//! - `-1`: Independent (no connection)
//! - `1`: Connect to parent's B edge
//! - `2`: Connect to parent's C edge
//!
//! Note: Connection to A edge doesn't exist by design
//! (parent's A edge is already shared with its parent)

/// CSV column indices for triangle data
///
/// Defines the position of each field in a triangle CSV row.
pub struct TriangleColumn;

impl TriangleColumn {
    // === Required (necessary for shape generation) ===

    /// Triangle serial number
    pub const NUMBER: usize = 0;
    /// Length of edge A (base edge, shared edge when connected)
    pub const LENGTH_A: usize = 1;
    /// Length of edge B
    pub const LENGTH_B: usize = 2;
    /// Length of edge C
    pub const LENGTH_C: usize = 3;

    // === Connection information ===

    /// Target triangle number for connection (-1 = not connected)
    pub const PARENT_NUMBER: usize = 4;
    /// Connection type (1=parent's B edge, 2=parent's C edge, -1=not connected)
    /// Note: A edge connection does not exist
    pub const CONNECTION_TYPE: usize = 5;

    // === Display information ===

    /// Triangle name (T1, T2, etc.)
    pub const NAME: usize = 6;
    /// X coordinate of point number (display position)
    pub const POINT_NUMBER_X: usize = 7;
    /// Y coordinate of point number (display position)
    pub const POINT_NUMBER_Y: usize = 8;
    /// Whether point number was manually moved by user
    pub const POINT_NUMBER_MOVED: usize = 9;
    /// Display color (color palette number 1-8)
    pub const COLOR: usize = 10;

    // === Dimension line placement ===

    /// Horizontal dimension placement for edge A
    pub const DIM_HORIZONTAL_A: usize = 11;
    /// Horizontal dimension placement for edge B
    pub const DIM_HORIZONTAL_B: usize = 12;
    /// Horizontal dimension placement for edge C
    pub const DIM_HORIZONTAL_C: usize = 13;
    /// Vertical dimension placement for edge A
    pub const DIM_VERTICAL_A: usize = 14;
    /// Vertical dimension placement for edge B
    pub const DIM_VERTICAL_B: usize = 15;
    /// Vertical dimension placement for edge C
    pub const DIM_VERTICAL_C: usize = 16;

    // === Connection parameter details ===

    /// Connection side (which edge to connect)
    pub const CONN_PARAM_SIDE: usize = 17;
    /// Connection type detail
    pub const CONN_PARAM_TYPE: usize = 18;
    /// Connection position (Left/Center/Right)
    pub const CONN_PARAM_LCR: usize = 19;

    // === Dimension flags ===

    /// Whether dimension 1 was moved by user
    pub const DIM_FLAG_1_MOVED: usize = 20;
    /// Whether dimension 2 was moved by user
    pub const DIM_FLAG_2_MOVED: usize = 21;

    // === Angle and coordinates ===

    /// Triangle angle
    pub const ANGLE: usize = 22;
    /// X coordinate of CA point
    pub const POINT_CA_X: usize = 23;
    /// Y coordinate of CA point
    pub const POINT_CA_Y: usize = 24;
    /// Local angle
    pub const ANGLE_LOCAL: usize = 25;

    // === Survey point ===

    /// Horizontal dimension survey point
    pub const DIM_HORIZONTAL_S: usize = 26;
    /// Whether survey point flag was moved by user
    pub const DIM_FLAG_S_MOVED: usize = 27;

    // === Column count definitions ===

    /// Minimum column count for independent triangles
    pub const MIN_REQUIRED: usize = 4;
    /// Column count with connection information
    pub const WITH_CONNECTION: usize = 6;
    /// Full format column count
    pub const FULL: usize = 28;
}

/// Connection type constants
///
/// Defines the type of connection between triangles.
pub struct ConnectionType;

impl ConnectionType {
    /// Independent triangle (no connection)
    pub const INDEPENDENT: i32 = -1;
    /// Connect to parent's B edge
    pub const PARENT_B_EDGE: i32 = 1;
    /// Connect to parent's C edge
    pub const PARENT_C_EDGE: i32 = 2;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_triangle_column_indices() {
        // Verify required columns
        assert_eq!(TriangleColumn::NUMBER, 0);
        assert_eq!(TriangleColumn::LENGTH_A, 1);
        assert_eq!(TriangleColumn::LENGTH_B, 2);
        assert_eq!(TriangleColumn::LENGTH_C, 3);

        // Verify connection columns
        assert_eq!(TriangleColumn::PARENT_NUMBER, 4);
        assert_eq!(TriangleColumn::CONNECTION_TYPE, 5);

        // Verify column counts
        assert_eq!(TriangleColumn::MIN_REQUIRED, 4);
        assert_eq!(TriangleColumn::WITH_CONNECTION, 6);
        assert_eq!(TriangleColumn::FULL, 28);
    }

    #[test]
    fn test_connection_type_values() {
        assert_eq!(ConnectionType::INDEPENDENT, -1);
        assert_eq!(ConnectionType::PARENT_B_EDGE, 1);
        assert_eq!(ConnectionType::PARENT_C_EDGE, 2);
    }
}
