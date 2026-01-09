//! DXF file generation library
//!
//! This library provides functionality to generate DXF files from geometric entities.

pub mod dxf;

pub use dxf::entities::{DxfLine, DxfText, HorizontalAlignment, VerticalAlignment};
pub use dxf::writer::DxfWriter;

