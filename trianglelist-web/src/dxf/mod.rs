//! DXF generation module
//!
//! Provides functionality to convert triangles to DXF format and download
//!
//! # Overview
//!
//! This module provides two main components:
//!
//! - `TriangleToDxfConverter`: Converts `ParsedTriangle` data to DXF entities (lines and texts)
//! - `download_dxf`: Downloads DXF content as a file in the browser
//!
//! # Example
//!
//! ```no_run
//! use trianglelist_web::dxf::{TriangleToDxfConverter, download_dxf};
//! use trianglelist_web::csv::ParsedTriangle;
//!
//! // Convert triangles to DXF entities
//! let converter = TriangleToDxfConverter::new();
//! let triangles = vec![
//!     ParsedTriangle::independent(1, 3.0, 4.0, 5.0),
//! ];
//! let (lines, texts) = converter.convert(&triangles);
//!
//! // Generate DXF content
//! let writer = dxf::DxfWriter::new();
//! let dxf_content = writer.write(&lines, &texts);
//!
//! // Download (WASM only)
//! #[cfg(target_arch = "wasm32")]
//! download_dxf(&dxf_content, "triangles.dxf").expect("Failed to download DXF");
//! ```

pub mod converter;
pub mod download;

pub use converter::TriangleToDxfConverter;
pub use download::download_dxf;

