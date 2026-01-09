//! DXF download functionality for web browsers
//!
//! Provides functions to download DXF content as a file in the browser.
//!
//! # Overview
//!
//! This module provides browser-based file download functionality using the
//! web-sys Blob API. It creates a temporary blob URL, triggers a download
//! through a hidden anchor element, and ensures proper cleanup of resources.
//!
//! # Example
//!
//! ```no_run
//! use trianglelist_web::dxf::download_dxf;
//!
//! let dxf_content = "0\nSECTION\n2\nHEADER\n...";
//! #[cfg(target_arch = "wasm32")]
//! download_dxf(dxf_content, "triangles.dxf")
//!     .expect("Failed to download DXF file");
//! ```
//!
//! # Errors
//!
//! This function may return errors if:
//! - The browser window or document is not available
//! - The blob cannot be created from the content
//! - The object URL cannot be created
//! - The anchor element cannot be created or manipulated
//!
//! All errors are wrapped in `JsValue` for WASM compatibility.

#[cfg(target_arch = "wasm32")]
use wasm_bindgen::prelude::*;
#[cfg(target_arch = "wasm32")]
use web_sys::{Blob, Url, HtmlAnchorElement};

/// Downloads DXF content as a file in the browser
///
/// This function creates a Blob from the DXF content, creates a temporary
/// object URL, triggers a download through a hidden anchor element, and
/// cleans up all resources (including on error).
///
/// # Arguments
///
/// * `dxf_content` - The DXF file content as a string
/// * `filename` - The filename for the download (e.g., "triangles.dxf")
///
/// # Returns
///
/// * `Ok(())` if the download was successfully triggered
/// * `Err(JsValue)` if any error occurred during the download process
///
/// # Errors
///
/// Returns an error if:
/// - The browser window or document is not available
/// - The blob cannot be created from the content
/// - The object URL cannot be created
/// - The anchor element cannot be created or manipulated
/// - Cleanup operations fail
///
/// # Resource Management
///
/// This function ensures proper cleanup of resources:
/// - The anchor element is removed from the DOM
/// - The object URL is revoked to prevent memory leaks
/// - Cleanup occurs even if errors are encountered
///
/// # Platform Compatibility
///
/// This function is only available on WASM targets. On other platforms,
/// use the non-WASM version which returns `Ok(())` without performing
/// any operations.
#[cfg(target_arch = "wasm32")]
pub fn download_dxf(dxf_content: &str, filename: &str) -> Result<(), JsValue> {
    // Validate inputs
    if dxf_content.is_empty() {
        return Err(JsValue::from_str("DXF content cannot be empty"));
    }
    if filename.is_empty() {
        return Err(JsValue::from_str("Filename cannot be empty"));
    }

    // Get window and document
    let window = web_sys::window()
        .ok_or_else(|| JsValue::from_str("Failed to get window - browser environment required"))?;
    let document = window
        .document()
        .ok_or_else(|| JsValue::from_str("Failed to get document - browser environment required"))?;

    // Create a Blob with the DXF content
    let array = js_sys::Array::new();
    array.push(&JsValue::from_str(dxf_content));
    let options = {
        let opts = web_sys::BlobPropertyBag::new();
        opts.set_type("text/plain");
        opts
    };
    let blob = Blob::new_with_str_sequence_and_options(&array, &options)
        .map_err(|e| {
            JsValue::from_str(&format!("Failed to create blob from DXF content: {:?}", e))
        })?;

    // Create a temporary URL for the blob
    let url = Url::create_object_url_with_blob(&blob)
        .map_err(|e| {
            JsValue::from_str(&format!("Failed to create object URL from blob: {:?}", e))
        })?;

    // Ensure URL is cleaned up even on error
    let result = (|| -> Result<(), JsValue> {
        // Create a temporary anchor element and trigger download
        let body = document
            .body()
            .ok_or_else(|| JsValue::from_str("Failed to get document body"))?;

        let anchor = document
            .create_element("a")
            .map_err(|e| JsValue::from_str(&format!("Failed to create anchor element: {:?}", e)))?
            .dyn_into::<HtmlAnchorElement>()
            .map_err(|e| {
                JsValue::from_str(&format!("Failed to cast element to HtmlAnchorElement: {:?}", e))
            })?;

        anchor.set_href(&url);
        anchor.set_download(filename);
        anchor
            .style()
            .set_property("display", "none")
            .map_err(|e| {
                JsValue::from_str(&format!("Failed to set anchor style: {:?}", e))
            })?;

        body
            .append_child(&anchor)
            .map_err(|e| JsValue::from_str(&format!("Failed to append anchor to body: {:?}", e)))?;

        anchor.click();

        // Clean up anchor element
        body
            .remove_child(&anchor)
            .map_err(|e| JsValue::from_str(&format!("Failed to remove anchor from body: {:?}", e)))?;

        Ok(())
    })();

    // Always revoke the object URL, even on error
    if let Err(revoke_err) = Url::revoke_object_url(&url) {
        // Log the error but don't fail if cleanup fails
        web_sys::console::error_1(&JsValue::from_str(&format!(
            "Failed to revoke object URL: {:?}",
            revoke_err
        )));
    }

    result
}

/// Placeholder for non-WASM targets (does nothing)
///
/// This function is provided for cross-platform compatibility. On non-WASM
/// targets (e.g., native desktop applications), this function simply returns
/// `Ok(())` without performing any operations.
///
/// # Arguments
///
/// * `_dxf_content` - Ignored on non-WASM targets
/// * `_filename` - Ignored on non-WASM targets
///
/// # Returns
///
/// Always returns `Ok(())` on non-WASM targets.
#[cfg(not(target_arch = "wasm32"))]
pub fn download_dxf(_dxf_content: &str, _filename: &str) -> Result<(), String> {
    // In non-WASM environments, this would typically write to a file
    // For now, just return Ok
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_download_dxf_non_wasm() {
        // Test that non-WASM version returns Ok
        assert!(download_dxf("test content", "test.dxf").is_ok());
    }

    #[test]
    #[cfg(not(target_arch = "wasm32"))]
    fn test_download_dxf_empty_content_non_wasm() {
        // Non-WASM version should handle empty content gracefully
        assert!(download_dxf("", "test.dxf").is_ok());
    }
}

