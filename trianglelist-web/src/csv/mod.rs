//! CSV Module for Triangle Data
//!
//! This module provides CSV parsing functionality for triangle definitions.
//!
//! # CSV Format Support
//!
//! ## Minimal Format (4 columns)
//! ```csv
//! Number, SideA, SideB, SideC
//! 1, 6.0, 5.0, 4.0
//! ```
//!
//! ## Extended Format (6 columns)
//! ```csv
//! Number, SideA, SideB, SideC, ParentNumber, ConnectionType
//! 1, 6.0, 5.0, 4.0, -1, -1
//! 2, 5.0, 4.0, 3.0, 1, 1
//! ```
//!
//! # Connection Types
//! - `-1`: Independent (no connection)
//! - `1`: Connected to parent's B side
//! - `2`: Connected to parent's C side

pub mod column;
pub mod parser;

pub use column::*;
pub use parser::*;
