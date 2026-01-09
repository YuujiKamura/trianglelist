//! CAD-quality text rendering using Canvas 2D API
//!
//! Provides precise text drawing with zoom/pan support, rotation, and DXF-compatible alignment.
//! This module uses Canvas 2D API directly for native CAD-quality rendering.

use wasm_bindgen::{JsValue, JsCast};
use web_sys::{CanvasRenderingContext2d, HtmlCanvasElement};
use crate::render::canvas::ViewState;
use crate::render::text::{TextAlign, HorizontalAlign, VerticalAlign};

/// CAD-quality text renderer using Canvas 2D API
pub struct Canvas2dTextRenderer {
    ctx: CanvasRenderingContext2d,
}

impl Canvas2dTextRenderer {
    /// Create a new Canvas2dTextRenderer from a canvas element
    pub fn new(canvas: &HtmlCanvasElement) -> Result<Self, JsValue> {
        let ctx = canvas
            .get_context("2d")?
            .ok_or_else(|| JsValue::from_str("Failed to get 2d context"))?
            .dyn_into::<CanvasRenderingContext2d>()?;
        
        Ok(Self { ctx })
    }

    /// Draw text with CAD-compatible alignment, rotation, and view transform
    ///
    /// # Arguments
    /// * `text` - Text string to draw
    /// * `model_x` - X position in model coordinates
    /// * `model_y` - Y position in model coordinates
    /// * `model_height` - Text height in model coordinates
    /// * `rotation_degrees` - Rotation angle in degrees (0 = horizontal)
    /// * `align` - Text alignment
    /// * `color` - Text color (CSS color string, e.g., "white", "#ffffff", "rgb(255,255,255)")
    /// * `view_state` - View state for zoom/pan transform
    /// * `zoom_dependent_size` - If true, text size scales with zoom; if false, maintains constant screen size
    pub fn draw_text_cad_style(
        &self,
        text: &str,
        model_x: f64,
        model_y: f64,
        model_height: f64,
        rotation_degrees: f64,
        align: TextAlign,
        color: &str,
        view_state: &ViewState,
        zoom_dependent_size: bool,
    ) -> Result<(), JsValue> {
        self.ctx.save();

        // Apply view transform (pan and zoom)
        self.ctx.translate(view_state.pan.x as f64, view_state.pan.y as f64)?;
        self.ctx.scale(view_state.zoom as f64, view_state.zoom as f64)?;

        // Move to text position in model coordinates
        self.ctx.translate(model_x, model_y)?;

        // Apply rotation
        if rotation_degrees.abs() > 1e-6 {
            self.ctx.rotate(rotation_degrees.to_radians())?;
        }

        // Determine text size
        let font_size = if zoom_dependent_size {
            // Scale with zoom (text grows/shrinks with geometry)
            model_height * view_state.zoom as f64
        } else {
            // Constant screen size (text remains readable at all zoom levels)
            model_height
        };

        // Set font
        self.ctx.set_font(&format!("{:.2}px monospace", font_size));

        // Set color
        self.ctx.set_fill_style_str(color);

        // Measure text for alignment
        let metrics = self.ctx.measure_text(text)?;
        let text_width = metrics.width();
        
        // Estimate text height (Canvas API doesn't provide exact height)
        // Typical monospace font has aspect ratio of ~1.2 (height/width per character)
        // For better accuracy, we use font size as base
        let text_height = font_size * 1.2;

        // Calculate alignment offset
        let (offset_x, offset_y) = self.calculate_alignment_offset(align, text_width, text_height);

        // Apply alignment offset
        self.ctx.translate(offset_x, offset_y)?;

        // Draw text at origin (after all transforms)
        self.ctx.fill_text(text, 0.0, 0.0)?;

        self.ctx.restore();
        Ok(())
    }

    /// Calculate alignment offset based on text alignment
    fn calculate_alignment_offset(
        &self,
        align: TextAlign,
        text_width: f64,
        text_height: f64,
    ) -> (f64, f64) {
        // Horizontal alignment
        let offset_x = match align.horizontal {
            HorizontalAlign::Left => 0.0,
            HorizontalAlign::Center => -text_width / 2.0,
            HorizontalAlign::Right => -text_width,
        };

        // Vertical alignment
        // Canvas 2D API draws text from baseline, so we need to adjust
        // Baseline is at y=0, text extends upward (negative y) for most characters
        let offset_y = match align.vertical {
            VerticalAlign::Baseline => 0.0,
            VerticalAlign::Bottom => -text_height * 0.8,  // Move down from baseline
            VerticalAlign::Middle => -text_height * 0.4,  // Move to middle
            VerticalAlign::Top => -text_height,            // Move to top
        };

        (offset_x, offset_y)
    }

    /// Clear the canvas
    pub fn clear(&self, width: f64, height: f64) -> Result<(), JsValue> {
        self.ctx.clear_rect(0.0, 0.0, width, height);
        Ok(())
    }

    /// Set high DPI support
    pub fn setup_high_dpi(&self, canvas: &HtmlCanvasElement) -> Result<(), JsValue> {
        let window = web_sys::window()
            .ok_or_else(|| JsValue::from_str("Failed to get window"))?;
        let dpr = window.device_pixel_ratio();
        
        let rect = canvas.get_bounding_client_rect();
        let width = rect.width() * dpr;
        let height = rect.height() * dpr;

        // Set actual size in memory (scaled for DPI)
        canvas.set_width(width as u32);
        canvas.set_height(height as u32);

        // Scale the context to match device pixel ratio
        self.ctx.scale(dpr, dpr)?;

        Ok(())
    }

    /// Enable/disable text antialiasing
    pub fn set_text_antialiasing(&self, enabled: bool) {
        self.ctx.set_image_smoothing_enabled(enabled);
    }
}

/// Helper function to convert egui Color32 to CSS color string
pub fn color32_to_css(color: eframe::egui::Color32) -> String {
    format!(
        "rgb({},{},{})",
        color.r(),
        color.g(),
        color.b()
    )
}
