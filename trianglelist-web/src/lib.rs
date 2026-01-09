//! trianglelist-web - Web version of Triangle List Calculator
//!
//! This crate provides a WebAssembly entry point for the TriangleList application
//! using egui/eframe for the user interface.

mod app;

use app::TriangleListApp;
use wasm_bindgen::prelude::*;

/// WASM entry point - called when the WASM module is loaded
#[wasm_bindgen(start)]
pub fn main() {
    // Set panic hook for better error messages in browser console
    console_error_panic_hook::set_once();

    // Initialize console logging for wasm
    #[cfg(target_arch = "wasm32")]
    console_log::init_with_level(log::Level::Debug).expect("Failed to initialize console_log");

    // Configure eframe options
    let web_options = eframe::WebOptions::default();

    // Start the eframe application
    wasm_bindgen_futures::spawn_local(async {
        let result = eframe::WebRunner::new()
            .start(
                "the_canvas_id",
                web_options,
                Box::new(|cc| Ok(Box::new(TriangleListApp::new(cc)))),
            )
            .await;

        // Handle startup errors
        if let Err(e) = result {
            log::error!("Failed to start eframe: {:?}", e);
        }

        // Hide loading screen after initialization
        hide_loading();
    });
}

/// Calls the JavaScript hideLoading function to remove the loading screen
fn hide_loading() {
    if let Some(window) = web_sys::window() {
        let _ = js_sys::Reflect::get(&window, &JsValue::from_str("hideLoading"))
            .ok()
            .and_then(|func| func.dyn_ref::<js_sys::Function>().map(|f| f.call0(&window)));
    }
}
