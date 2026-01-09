//! CAD-quality text rendering using Canvas 2D API
//!
//! Provides precise text drawing with rotation and DXF-compatible alignment.
//! Uses screen coordinates directly - coordinate transformation should be done
//! by the caller using ViewState::model_to_screen().

use wasm_bindgen::{JsValue, JsCast};
use web_sys::{CanvasRenderingContext2d, HtmlCanvasElement};
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

    /// Draw text at screen coordinates with rotation
    ///
    /// Simple API matching the working demo. Caller should convert model
    /// coordinates to screen coordinates using ViewState::model_to_screen().
    ///
    /// # Arguments
    /// * `text` - Text string to draw
    /// * `screen_x` - X position in screen coordinates
    /// * `screen_y` - Y position in screen coordinates
    /// * `font_size` - Font size in pixels
    /// * `rotation_degrees` - Rotation angle in degrees (0 = horizontal)
    /// * `align` - Text alignment
    /// * `color` - Text color (CSS color string)
    pub fn draw_text(
        &self,
        text: &str,
        screen_x: f64,
        screen_y: f64,
        font_size: f64,
        rotation_degrees: f64,
        align: TextAlign,
        color: &str,
    ) -> Result<(), JsValue> {
        self.ctx.save();

        // Move to text position
        self.ctx.translate(screen_x, screen_y)?;

        // Apply rotation
        if rotation_degrees.abs() > 1e-6 {
            self.ctx.rotate(rotation_degrees.to_radians())?;
        }

        // Set font and color
        self.ctx.set_font(&format!("{:.1}px monospace", font_size));
        self.ctx.set_fill_style_str(color);

        // Set alignment using Canvas 2D native properties
        self.ctx.set_text_align(match align.horizontal {
            HorizontalAlign::Left => "left",
            HorizontalAlign::Center => "center",
            HorizontalAlign::Right => "right",
        });
        self.ctx.set_text_baseline(match align.vertical {
            VerticalAlign::Top => "top",
            VerticalAlign::Middle => "middle",
            VerticalAlign::Bottom | VerticalAlign::Baseline => "bottom",
        });

        // Draw text at origin (after transforms)
        self.ctx.fill_text(text, 0.0, 0.0)?;

        self.ctx.restore();
        Ok(())
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
