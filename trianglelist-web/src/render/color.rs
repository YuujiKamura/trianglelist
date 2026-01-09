//! DXF color palette (ACI - AutoCAD Color Index)
//!
//! Converts DXF color numbers (0-255) to egui Color32

use eframe::egui::Color32;

/// Converts DXF ACI (AutoCAD Color Index) to egui Color32
/// 
/// # Arguments
/// * `aci` - DXF color index (0-255)
/// 
/// # Returns
/// Color32 corresponding to the ACI color
pub fn dxf_color_to_egui(aci: i32) -> Color32 {
    match aci {
        0 => Color32::from_rgb(0, 0, 0),       // Black / ByBlock
        1 => Color32::from_rgb(255, 0, 0),     // Red
        2 => Color32::from_rgb(255, 255, 0),   // Yellow
        3 => Color32::from_rgb(0, 255, 0),     // Green
        4 => Color32::from_rgb(0, 255, 255),   // Cyan
        5 => Color32::from_rgb(0, 0, 255),     // Blue
        6 => Color32::from_rgb(255, 0, 255),   // Magenta
        7 => Color32::from_rgb(255, 255, 255), // White / ByLayer
        // Standard colors 8-15 (darker versions)
        8 => Color32::from_rgb(128, 128, 128), // Dark Gray
        9 => Color32::from_rgb(192, 192, 192), // Light Gray
        // For other colors, use a simple mapping
        _ if aci >= 10 && aci <= 249 => {
            // Map to a color based on the index
            let r = ((aci * 7) % 256) as u8;
            let g = ((aci * 11) % 256) as u8;
            let b = ((aci * 13) % 256) as u8;
            Color32::from_rgb(r, g, b)
        }
        _ => Color32::from_rgb(255, 255, 255), // Default to white
    }
}

/// Default triangle fill color (DXF color 4 - Cyan)
pub fn default_triangle_fill() -> Color32 {
    Color32::from_rgba_unmultiplied(0, 255, 255, 64)
}

/// Default triangle stroke color (DXF color 7 - White)
pub const DEFAULT_TRIANGLE_STROKE: Color32 = Color32::from_rgb(255, 255, 255);

/// Default text color (DXF color 7 - White)
pub const DEFAULT_TEXT_COLOR: Color32 = Color32::from_rgb(255, 255, 255);

