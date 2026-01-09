//! DXF generation module
//!
//! Provides functionality to convert triangles to DXF format and download

pub mod converter;
pub mod download;

pub use converter::TriangleToDxfConverter;
pub use download::download_dxf;

