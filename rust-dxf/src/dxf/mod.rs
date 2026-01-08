//! DXF module
//!
//! This module provides functionality for generating DXF (Drawing Exchange Format) files.
//!
//! # Example
//!
//! ```
//! use dxf::{DxfLine, DxfText, DxfWriter};
//!
//! let lines = vec![
//!     DxfLine::new(0.0, 0.0, 100.0, 100.0),
//!     DxfLine::with_style(0.0, 100.0, 100.0, 0.0, 3, "Layer1"),
//! ];
//!
//! let texts = vec![
//!     DxfText::new(50.0, 50.0, "Center"),
//! ];
//!
//! let writer = DxfWriter::new();
//! let dxf_content = writer.write(&lines, &texts);
//! ```

pub mod entities;
pub mod writer;

// Re-export main types for convenience
pub use entities::{DxfLine, DxfText};
pub use writer::DxfWriter;
