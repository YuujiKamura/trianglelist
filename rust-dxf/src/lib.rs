//! DXF file generation library
//!
//! This library provides functionality to generate DXF files from geometric entities.
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
//! assert!(dxf_content.contains("LINE"));
//! assert!(dxf_content.contains("TEXT"));
//! ```

pub mod dxf;

pub use dxf::entities::{DxfLine, DxfText, HorizontalAlignment, VerticalAlignment};
pub use dxf::writer::DxfWriter;

