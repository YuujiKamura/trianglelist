//! DXF download functionality for web browsers
//!
//! Provides functions to download DXF content as a file in the browser

#[cfg(target_arch = "wasm32")]
use wasm_bindgen::prelude::*;
#[cfg(target_arch = "wasm32")]
use web_sys::{Blob, Url, HtmlAnchorElement};

/// Downloads DXF content as a file in the browser
///
/// # Arguments
/// * `dxf_content` - The DXF file content as a string
/// * `filename` - The filename for the download (e.g., "triangles.dxf")
#[cfg(target_arch = "wasm32")]
pub fn download_dxf(dxf_content: &str, filename: &str) -> Result<(), JsValue> {
    // Create a Blob with the DXF content
    let array = js_sys::Array::new();
    array.push(&JsValue::from_str(dxf_content));
    let options = {
        let mut opts = web_sys::BlobPropertyBag::new();
        opts.set_type("text/plain");
        opts
    };
    let blob = Blob::new_with_str_sequence_and_options(&array, &options)?;

    // Create a temporary URL for the blob
    let url = Url::create_object_url_with_blob(&blob)?;

    // Create a temporary anchor element and trigger download
    let window = web_sys::window().ok_or_else(|| JsValue::from_str("Failed to get window"))?;
    let document = window.document().ok_or_else(|| JsValue::from_str("Failed to get document"))?;
    
    let anchor = document
        .create_element("a")?
        .dyn_into::<HtmlAnchorElement>()?;
    
    anchor.set_href(&url);
    anchor.set_download(filename);
    anchor.style().set_property("display", "none")?;
    
    document.body()
        .ok_or_else(|| JsValue::from_str("Failed to get body"))?
        .append_child(&anchor)?;
    
    anchor.click();
    
    // Clean up
    document.body()
        .ok_or_else(|| JsValue::from_str("Failed to get body"))?
        .remove_child(&anchor)?;
    
    Url::revoke_object_url(&url)?;

    Ok(())
}

/// Placeholder for non-WASM targets (does nothing)
#[cfg(not(target_arch = "wasm32"))]
pub fn download_dxf(_dxf_content: &str, _filename: &str) -> Result<(), String> {
    // In non-WASM environments, this would typically write to a file
    // For now, just return Ok
    Ok(())
}

