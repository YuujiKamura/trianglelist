//! trianglelist-web - Web version of Triangle List Calculator
//!
//! This crate provides a WebAssembly-based triangle list calculator
//! built with egui and eframe.

pub mod app;
pub mod csv;
pub mod dxf;
pub mod render;

pub use app::TriangleListApp;

#[cfg(target_arch = "wasm32")]
use wasm_bindgen::{prelude::*, JsCast};
#[cfg(target_arch = "wasm32")]
use web_sys::HtmlCanvasElement;

/// WASM entry point - starts the application
#[cfg(target_arch = "wasm32")]
#[wasm_bindgen(start)]
pub fn start() -> Result<(), JsValue> {
    // Set up panic hook for better error messages in browser console
    console_error_panic_hook::set_once();

    // Initialize logging for WASM
    console_log::init_with_level(log::Level::Debug)
        .expect("Failed to initialize console_log");

    // Get the canvas element from DOM
    let window = web_sys::window().ok_or_else(|| JsValue::from_str("Failed to get window"))?;
    let document = window
        .document()
        .ok_or_else(|| JsValue::from_str("Failed to get document"))?;
    let canvas = document
        .get_element_by_id("trianglelist-canvas")
        .ok_or_else(|| JsValue::from_str("Failed to get canvas element"))?
        .dyn_into::<HtmlCanvasElement>()
        .map_err(|_| JsValue::from_str("Failed to cast to HtmlCanvasElement"))?;

    // Start the eframe application
    let web_options = eframe::WebOptions::default();

    wasm_bindgen_futures::spawn_local(async {
        eframe::WebRunner::new()
            .start(
                canvas,
                web_options,
                Box::new(|cc| Ok(Box::new(TriangleListApp::new(cc)))),
            )
            .await
            .expect("Failed to start eframe");
    });

    Ok(())
}

