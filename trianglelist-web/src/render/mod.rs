//! Rendering module for triangle visualization
//!
//! Provides functions for drawing triangles, text, and dimensions
//! using egui's Painter API with CAD-compatible text alignment.

pub mod canvas;
pub mod text;
pub mod color;

pub use canvas::*;
pub use text::*;
pub use color::*;

