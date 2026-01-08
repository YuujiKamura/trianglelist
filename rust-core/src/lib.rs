//! Rust core library for triangle list calculations.
//!
//! This library provides geometry calculation functions that mirror
//! the functionality in the Kotlin codebase.

pub mod geometry;
pub mod model;

pub use geometry::*;
pub use model::{Alignment, ConnParam, ConnectionSide};
