//! trianglelist-web - Web version of Triangle List Calculator
//!
//! This crate provides a WebAssembly-based triangle list calculator
//! built with egui and eframe.

pub mod app;
pub mod csv;

pub use app::TriangleListApp;

#[cfg(target_arch = "wasm32")]
use wasm_bindgen::prelude::*;

/// WASM entry point - starts the application
#[cfg(target_arch = "wasm32")]
#[wasm_bindgen(start)]
pub fn start() -> Result<(), JsValue> {
    // Set up panic hook for better error messages in browser console
    console_error_panic_hook::set_once();

    // Initialize logging for WASM
    console_log::init_with_level(log::Level::Debug)
        .expect("Failed to initialize console_log");

    // Start the eframe application
    let web_options = eframe::WebOptions::default();

    wasm_bindgen_futures::spawn_local(async {
        eframe::WebRunner::new()
            .start(
                "trianglelist-canvas",
                web_options,
                Box::new(|cc| Ok(Box::new(TriangleListApp::new(cc)))),
            )
            .await
            .expect("Failed to start eframe");
    });

    Ok(())
}

