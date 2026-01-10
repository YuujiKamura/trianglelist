//! TriangleList Application
//!
//! This module defines the main application struct and implements
//! the eframe::App trait for UI rendering.

use eframe::egui;

use crate::csv::{parse_csv, ParseError, ParseResult, ParsedTriangle};
use crate::dxf::{download_dxf, TriangleToDxfConverter};
use crate::render::{ViewState, draw_triangle, draw_triangle_number};
use crate::render::color::{default_triangle_fill, DEFAULT_TRIANGLE_STROKE, DEFAULT_TEXT_COLOR};
use crate::render::text_canvas2d::{Canvas2dTextRenderer, color32_to_css};
use crate::render::text::{TextAlign, HorizontalAlign, VerticalAlign};
use crate::road_section::{
    StationData, RoadSectionGeometry, RoadSectionConfig,
    parse_road_section_csv, calculate_road_section, detect_csv_type, CsvType,
    geometry_to_dxf,
};
use crate::render::road_section::{draw_road_section_bbox, draw_road_section_canvas2d, draw_road_section_egui};
use eframe::egui::Pos2;
use wasm_bindgen::JsCast;

#[cfg(target_arch = "wasm32")]
use std::cell::RefCell;
#[cfg(target_arch = "wasm32")]
use wasm_bindgen::prelude::*;

/// Global storage for file content loaded from file picker
#[cfg(target_arch = "wasm32")]
thread_local! {
    static PENDING_FILE_CONTENT: RefCell<Option<String>> = RefCell::new(None);
    static FILE_CALLBACK_CLOSURE: RefCell<Option<Closure<dyn FnMut(String)>>> = RefCell::new(None);
}

/// Display mode for the application
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum DisplayMode {
    /// Triangle mesh mode
    Triangle,
    /// Road section (面積展開図) mode
    RoadSection,
}

impl Default for DisplayMode {
    fn default() -> Self {
        Self::Triangle
    }
}

/// The main TriangleList application struct.
///
/// This struct holds the application state and implements the eframe::App trait
/// for rendering the UI.
pub struct TriangleListApp {
    /// Current display mode
    display_mode: DisplayMode,
    /// Parsed triangles from CSV (for Triangle mode)
    triangles: Vec<ParsedTriangle>,
    /// Parsed station data (for RoadSection mode)
    stations: Vec<StationData>,
    /// Calculated road section geometry
    road_section_geometry: Option<RoadSectionGeometry>,
    /// Error message if parsing failed
    error_message: Option<String>,
    /// Warning messages from parsing
    warnings: Vec<String>,
    /// Whether file drop zone is being hovered
    is_drop_hover: bool,
    /// CSV text input for manual entry
    csv_input: String,
    /// View state for pan and zoom
    view_state: ViewState,
    /// Whether to show triangle visualization
    show_canvas: bool,
    /// Whether to show dimension lines
    show_dimensions: bool,
    /// Use Canvas 2D API for text rendering (CAD quality)
    use_canvas2d_text: bool,
    /// Canvas 2D text renderer (initialized when needed)
    canvas2d_text_renderer: Option<Canvas2dTextRenderer>,
    /// Current mouse position in model coordinates (for status bar)
    mouse_model_pos: Option<Pos2>,
    /// Show side panel
    show_side_panel: bool,
    /// Use BBOX-based text scaling (CAD equivalent, text scales with zoom)
    use_bbox_text: bool,
    /// Auto-adjust text direction to be readable (not upside-down)
    auto_readable_text: bool,
}

impl Default for TriangleListApp {
    fn default() -> Self {
        Self {
            display_mode: DisplayMode::default(),
            triangles: Vec::new(),
            stations: Vec::new(),
            road_section_geometry: None,
            error_message: None,
            warnings: Vec::new(),
            is_drop_hover: false,
            csv_input: String::new(),
            view_state: ViewState::default(),
            show_canvas: false,
            show_dimensions: true,
            use_canvas2d_text: false, // Default to egui for now
            canvas2d_text_renderer: None,
            mouse_model_pos: None,
            show_side_panel: true,
            use_bbox_text: true, // Default to CAD-style scaling
            auto_readable_text: true, // Default to readable text direction
        }
    }
}

const STORAGE_KEY_CSV: &str = "trianglelist_csv_cache";

impl TriangleListApp {
    /// Creates a new TriangleListApp instance.
    ///
    /// # Arguments
    /// * `_cc` - The eframe CreationContext, providing access to egui context and storage
    ///
    /// # Returns
    /// A new TriangleListApp instance
    pub fn new(_cc: &eframe::CreationContext<'_>) -> Self {
        let mut app = Self::default();

        // Set up file callback for mobile file picker
        #[cfg(target_arch = "wasm32")]
        Self::setup_file_callback();

        // Auto-hide side panel on mobile devices
        #[cfg(target_arch = "wasm32")]
        {
            if Self::is_mobile_device() {
                app.show_side_panel = false;
                log::info!("Mobile device detected, hiding side panel by default");
            }
        }

        // Try to restore cached CSV data
        #[cfg(target_arch = "wasm32")]
        {
            if let Some(csv_data) = Self::load_csv_from_storage() {
                log::info!("Restoring cached CSV data ({} bytes)", csv_data.len());
                app.csv_input = csv_data.clone();
                let result = parse_csv(&csv_data);
                app.handle_parse_result(result);
            }
        }

        app
    }

    /// Detect if running on a mobile device
    #[cfg(target_arch = "wasm32")]
    fn is_mobile_device() -> bool {
        use web_sys::window;

        if let Some(window) = window() {
            // Check screen width
            if let Ok(inner_width) = window.inner_width() {
                if let Some(width) = inner_width.as_f64() {
                    if width < 768.0 {
                        return true;
                    }
                }
            }

            // Also check user agent for touch devices
            if let Some(navigator) = window.navigator().user_agent().ok() {
                let ua_lower = navigator.to_lowercase();
                if ua_lower.contains("mobile") || ua_lower.contains("android") || ua_lower.contains("iphone") || ua_lower.contains("ipad") {
                    return true;
                }
            }
        }
        false
    }

    /// Save CSV data to localStorage
    #[cfg(target_arch = "wasm32")]
    fn save_csv_to_storage(csv_data: &str) {
        use web_sys::window;
        if let Some(window) = window() {
            if let Ok(Some(storage)) = window.local_storage() {
                let _ = storage.set_item(STORAGE_KEY_CSV, csv_data);
                log::info!("CSV data cached to localStorage");
            }
        }
    }

    /// Load CSV data from localStorage
    #[cfg(target_arch = "wasm32")]
    fn load_csv_from_storage() -> Option<String> {
        use web_sys::window;
        if let Some(window) = window() {
            if let Ok(Some(storage)) = window.local_storage() {
                if let Ok(Some(data)) = storage.get_item(STORAGE_KEY_CSV) {
                    return Some(data);
                }
            }
        }
        None
    }

    /// Clear cached CSV data
    #[cfg(target_arch = "wasm32")]
    fn clear_csv_storage() {
        use web_sys::window;
        if let Some(window) = window() {
            if let Ok(Some(storage)) = window.local_storage() {
                let _ = storage.remove_item(STORAGE_KEY_CSV);
            }
        }
    }

    /// Set up the file callback for receiving file content from JavaScript
    #[cfg(target_arch = "wasm32")]
    fn setup_file_callback() {
        use wasm_bindgen::JsValue;
        use web_sys::window;

        FILE_CALLBACK_CLOSURE.with(|closure_cell| {
            // Only set up the callback once
            if closure_cell.borrow().is_some() {
                return;
            }

            // Create the callback closure
            let callback = Closure::wrap(Box::new(move |content: String| {
                log::info!("File content received: {} bytes", content.len());
                PENDING_FILE_CONTENT.with(|cell| {
                    *cell.borrow_mut() = Some(content);
                });
            }) as Box<dyn FnMut(String)>);

            // Register it with JavaScript
            if let Some(window) = window() {
                let _ = js_sys::Reflect::set(
                    &window,
                    &JsValue::from_str("onCsvFileLoaded"),
                    callback.as_ref(),
                );
                log::info!("File callback registered");
            }

            // Store the closure to prevent it from being dropped
            *closure_cell.borrow_mut() = Some(callback);
        });
    }

    /// Check for pending file content from file picker
    #[cfg(target_arch = "wasm32")]
    fn take_pending_file_content() -> Option<String> {
        PENDING_FILE_CONTENT.with(|cell| {
            cell.borrow_mut().take()
        })
    }

    /// Open file picker dialog
    #[cfg(target_arch = "wasm32")]
    fn open_file_picker() {
        use wasm_bindgen::JsValue;
        use web_sys::window;

        // Ensure callback is registered before opening picker
        Self::setup_file_callback();

        if let Some(window) = window() {
            if let Ok(func) = js_sys::Reflect::get(&window, &JsValue::from_str("openFilePicker")) {
                if let Some(func) = func.dyn_ref::<js_sys::Function>() {
                    let _ = func.call0(&window);
                }
            }
        }
    }

    #[cfg(not(target_arch = "wasm32"))]
    fn open_file_picker() {
        log::warn!("File picker not available on this platform");
    }

    /// Initialize Canvas 2D text renderer
    ///
    /// Attempts to get the canvas element from the DOM and create a renderer.
    /// Initialize Canvas 2D renderer with the text-overlay canvas.
    /// This uses a separate canvas layer on top of egui's WebGL canvas
    /// to enable proper text rotation via Canvas 2D API.
    fn init_canvas2d_renderer(&mut self) {
        #[cfg(target_arch = "wasm32")]
        {
            use web_sys::window;

            if let Some(window) = window() {
                if let Some(document) = window.document() {
                    // Use the dedicated text-overlay canvas for Canvas 2D text rendering
                    if let Some(canvas) = document.get_element_by_id("text-overlay") {
                        if let Ok(canvas_element) = canvas.dyn_into::<web_sys::HtmlCanvasElement>() {
                            // Get the actual display size from bounding client rect
                            let rect = canvas_element.get_bounding_client_rect();
                            let width = rect.width() as u32;
                            let height = rect.height() as u32;

                            // Set canvas internal resolution to match display size
                            canvas_element.set_width(width);
                            canvas_element.set_height(height);
                            web_sys::console::log_1(&format!("text-overlay canvas size: {}x{}", width, height).into());

                            match Canvas2dTextRenderer::new(&canvas_element) {
                                Ok(renderer) => {
                                    self.canvas2d_text_renderer = Some(renderer);
                                    log::info!("Canvas 2D text renderer initialized with text-overlay canvas");
                                }
                                Err(e) => {
                                    log::warn!("Failed to initialize Canvas 2D renderer: {:?}", e);
                                }
                            }
                        }
                    } else {
                        log::warn!("text-overlay canvas not found");
                    }
                }
            }
        }

        #[cfg(not(target_arch = "wasm32"))]
        {
            log::warn!("Canvas 2D API is only available in WASM builds");
            self.use_canvas2d_text = false;
        }
    }

    /// Sync text overlay canvas size with display size (only when changed)
    #[cfg(target_arch = "wasm32")]
    fn sync_overlay_canvas_size(&self) {
        use web_sys::window;

        if let Some(window) = window() {
            if let Some(document) = window.document() {
                if let Some(overlay) = document.get_element_by_id("text-overlay") {
                    if let Ok(overlay_element) = overlay.dyn_into::<web_sys::HtmlCanvasElement>() {
                        // Get actual display size
                        let rect = overlay_element.get_bounding_client_rect();
                        let new_width = rect.width() as u32;
                        let new_height = rect.height() as u32;

                        // Only update if size changed (setting size resets canvas state)
                        if overlay_element.width() != new_width || overlay_element.height() != new_height {
                            overlay_element.set_width(new_width);
                            overlay_element.set_height(new_height);
                            log::debug!("Overlay canvas resized to {}x{}", new_width, new_height);
                        }
                    }
                }
            }
        }
    }

    /// Clear text overlay canvas (call at start of each frame)
    #[cfg(target_arch = "wasm32")]
    fn clear_text_overlay(&self) {
        use web_sys::window;

        if let Some(window) = window() {
            if let Some(document) = window.document() {
                if let Some(overlay) = document.get_element_by_id("text-overlay") {
                    if let Ok(overlay_element) = overlay.dyn_into::<web_sys::HtmlCanvasElement>() {
                        if let Ok(Some(ctx)) = overlay_element.get_context("2d") {
                            if let Ok(ctx) = ctx.dyn_into::<web_sys::CanvasRenderingContext2d>() {
                                ctx.clear_rect(
                                    0.0, 0.0,
                                    overlay_element.width() as f64,
                                    overlay_element.height() as f64
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    #[cfg(not(target_arch = "wasm32"))]
    fn clear_text_overlay(&self) {
        // No-op for non-WASM builds
    }

    /// Try to initialize Canvas 2D renderer with egui's canvas element (legacy fallback)
    #[cfg(target_arch = "wasm32")]
    #[allow(dead_code)]
    fn try_init_with_egui_canvas(&mut self, document: &web_sys::Document) {
        // Legacy fallback - try common canvas element IDs
        let canvas_ids = ["canvas", "egui_canvas", "trianglelist-canvas"];

        for canvas_id in &canvas_ids {
            if let Some(canvas) = document.get_element_by_id(canvas_id) {
                if let Ok(canvas_element) = canvas.dyn_into::<web_sys::HtmlCanvasElement>() {
                    match Canvas2dTextRenderer::new(&canvas_element) {
                        Ok(renderer) => {
                            self.canvas2d_text_renderer = Some(renderer);
                            log::info!("Canvas 2D text renderer initialized with canvas: {}", canvas_id);
                            return;
                        }
                        Err(e) => {
                            log::debug!("Failed to initialize with canvas {}: {:?}", canvas_id, e);
                        }
                    }
                }
            }
        }

        // If no canvas found, try to find any canvas element
        if let Ok(canvases) = document.query_selector_all("canvas") {
            for i in 0..canvases.length() {
                if let Some(canvas) = canvases.get(i) {
                    if let Ok(canvas_element) = canvas.dyn_into::<web_sys::HtmlCanvasElement>() {
                        match Canvas2dTextRenderer::new(&canvas_element) {
                            Ok(renderer) => {
                                self.canvas2d_text_renderer = Some(renderer);
                                log::info!("Canvas 2D text renderer initialized with found canvas");
                                return;
                            }
                            Err(_) => continue,
                        }
                    }
                }
            }
        }

        log::warn!("Could not find suitable canvas element for Canvas 2D text rendering");
        self.use_canvas2d_text = false;
    }

    #[cfg(not(target_arch = "wasm32"))]
    fn try_init_with_egui_canvas(&mut self, _document: &web_sys::Document) {
        // No-op for non-WASM
    }

    /// Handles parsed CSV result
    fn handle_parse_result(&mut self, result: Result<ParseResult, ParseError>) {
        match result {
            Ok(parse_result) => {
                self.triangles = parse_result.triangles;
                self.warnings = parse_result.warnings;
                self.error_message = None;
                self.show_canvas = !self.triangles.is_empty();

                // Fit view to triangles
                if !self.triangles.is_empty() {
                    self.view_state.fit_to_triangles(&self.triangles);
                }

                log::info!("Successfully parsed {} triangles", self.triangles.len());
            }
            Err(e) => {
                self.error_message = Some(e.to_string());
                self.triangles.clear();
                self.warnings.clear();
                self.show_canvas = false;
                log::error!("CSV parse error: {}", e);
            }
        }
    }

    /// Parse CSV and cache to localStorage
    fn parse_and_cache_csv(&mut self, csv_data: &str) {
        // Detect CSV type
        let csv_type = detect_csv_type(csv_data);
        log::info!("Detected CSV type: {:?}", csv_type);

        match csv_type {
            CsvType::Triangle | CsvType::Unknown => {
                // Try parsing as triangle data
                let result = parse_csv(csv_data);
                if result.is_ok() {
                    #[cfg(target_arch = "wasm32")]
                    Self::save_csv_to_storage(csv_data);
                    self.display_mode = DisplayMode::Triangle;
                    // Clear road section data
                    self.stations.clear();
                    self.road_section_geometry = None;
                }
                self.handle_parse_result(result);
            }
            CsvType::RoadSection => {
                // Parse as road section data
                match parse_road_section_csv(csv_data) {
                    Ok(stations) => {
                        #[cfg(target_arch = "wasm32")]
                        Self::save_csv_to_storage(csv_data);

                        self.display_mode = DisplayMode::RoadSection;
                        self.stations = stations;

                        // Calculate geometry
                        let config = RoadSectionConfig::default();
                        let geometry = calculate_road_section(&self.stations, &config);

                        // Fit view to geometry
                        self.view_state.fit_to_road_section(&geometry);

                        self.road_section_geometry = Some(geometry);
                        self.show_canvas = true;
                        self.error_message = None;

                        // Clear triangle data
                        self.triangles.clear();
                        self.warnings.clear();

                        log::info!("Parsed {} stations for road section", self.stations.len());
                    }
                    Err(e) => {
                        self.error_message = Some(format!("Road section parse error: {}", e));
                        self.stations.clear();
                        self.road_section_geometry = None;
                        self.show_canvas = false;
                    }
                }
            }
        }
    }

    /// Renders the file drop zone
    fn render_drop_zone(&mut self, ui: &mut egui::Ui) {
        let drop_zone_color = if self.is_drop_hover {
            egui::Color32::from_rgb(100, 149, 237) // Cornflower blue when hovering
        } else {
            egui::Color32::from_rgb(60, 60, 60) // Dark gray normally
        };

        let (rect, response) = ui.allocate_exact_size(
            egui::vec2(ui.available_width(), 100.0),
            egui::Sense::hover(),
        );

        if ui.is_rect_visible(rect) {
            ui.painter().rect_filled(rect, 8.0, drop_zone_color);
            ui.painter().rect_stroke(
                rect,
                8.0,
                egui::Stroke::new(2.0, egui::Color32::from_rgb(100, 100, 100)),
            );

            let text = if self.is_drop_hover {
                "Drop CSV file here!"
            } else {
                "Drag & Drop CSV file here\nor paste CSV content below"
            };

            ui.painter().text(
                rect.center(),
                egui::Align2::CENTER_CENTER,
                text,
                egui::FontId::proportional(16.0),
                egui::Color32::WHITE,
            );
        }

        // Update hover state based on whether files are being dragged
        self.is_drop_hover = response.hovered();
    }

    /// Renders the CSV text input area
    fn render_csv_input(&mut self, ui: &mut egui::Ui) {
        ui.add_space(10.0);
        ui.label("Or paste CSV content:");

        let text_edit = egui::TextEdit::multiline(&mut self.csv_input)
            .desired_rows(6)
            .desired_width(ui.available_width())
            .font(egui::TextStyle::Monospace)
            .hint_text("Paste CSV content here...");

        ui.add(text_edit);

        ui.horizontal(|ui| {
            if ui.button("Parse CSV").clicked() && !self.csv_input.is_empty() {
                self.parse_and_cache_csv(&self.csv_input.clone());
            }

            if ui.button("Clear").clicked() {
                self.csv_input.clear();
                self.triangles.clear();
                self.error_message = None;
                self.warnings.clear();
                self.show_canvas = false;
                #[cfg(target_arch = "wasm32")]
                Self::clear_csv_storage();
            }

            if ui.button("Load Sample").clicked() {
                self.csv_input = SAMPLE_CSV.to_string();
            }

            if ui.button("Load Road Section").clicked() {
                self.csv_input = SAMPLE_ROAD_SECTION_CSV.to_string();
            }
        });
    }

    /// Renders error messages
    fn render_error(&self, ui: &mut egui::Ui) {
        if let Some(ref error) = self.error_message {
            ui.add_space(10.0);
            ui.colored_label(egui::Color32::RED, format!("Error: {}", error));
        }
    }

    /// Renders warning messages
    fn render_warnings(&self, ui: &mut egui::Ui) {
        for warning in &self.warnings {
            ui.colored_label(egui::Color32::YELLOW, warning);
        }
    }

    /// Renders the canvas with triangles
    fn render_canvas(&mut self, ui: &mut egui::Ui) {
        use crate::render::canvas::calculate_all_triangle_positions;

        // Sync and clear text overlay at the start of each frame
        #[cfg(target_arch = "wasm32")]
        self.sync_overlay_canvas_size();
        self.clear_text_overlay();

        let (response, painter) = ui.allocate_painter(
            ui.available_size(),
            egui::Sense::drag().union(egui::Sense::hover()),
        );

        let rect = response.rect;
        self.view_state.set_canvas_size(rect.size());

        // Update mouse position for status bar
        if let Some(pointer_pos) = response.hover_pos() {
            let relative_vec = pointer_pos - rect.min;
            let relative_pos = Pos2::new(relative_vec.x, relative_vec.y);
            let model_pos = self.view_state.screen_to_model(relative_pos);
            self.mouse_model_pos = Some(model_pos);
        } else {
            self.mouse_model_pos = None;
        }

        // Handle pan (drag) or rotation (Shift+drag)
        if response.dragged_by(egui::PointerButton::Primary) {
            let shift_held = ui.input(|i| i.modifiers.shift);
            if shift_held {
                // Shift+drag: rotate around canvas center
                let delta = response.drag_delta();
                // Horizontal drag controls rotation (0.01 rad per pixel)
                self.view_state.rotation += delta.x * 0.01;
            } else {
                // Normal drag: pan (rotate delta to match view rotation)
                let delta = response.drag_delta();
                let cos_r = self.view_state.rotation.cos();
                let sin_r = self.view_state.rotation.sin();
                let rotated_delta = egui::Vec2::new(
                    delta.x * cos_r + delta.y * sin_r,
                    -delta.x * sin_r + delta.y * cos_r,
                );
                self.view_state.pan += rotated_delta;
            }
        }

        // Handle zoom (mouse wheel)
        if ui.input(|i| i.zoom_delta()) != 1.0 {
            let zoom_factor = ui.input(|i| i.zoom_delta());
            self.view_state.zoom *= zoom_factor;

            // Adjust pan to zoom around the center of the canvas
            let center_screen = rect.center();
            let center_model = self.view_state.screen_to_model(center_screen);
            self.view_state.pan = Pos2::new(
                center_screen.x - center_model.x * self.view_state.zoom,
                center_screen.y - center_model.y * self.view_state.zoom,
            );
        }

        // Draw background
        painter.rect_filled(rect, 0.0, egui::Color32::from_rgb(30, 30, 30));

        // Render based on display mode
        match self.display_mode {
            DisplayMode::RoadSection => {
                self.render_road_section(&painter);
                return;
            }
            DisplayMode::Triangle => {
                // Continue with triangle rendering below
            }
        }

        // Calculate all triangle positions with proper connections
        let positioned_triangles = calculate_all_triangle_positions(&self.triangles);

        for positioned in &positioned_triangles {
            // Convert model coordinates to screen coordinates
            let points_screen: [Pos2; 3] = [
                self.view_state.model_to_screen(positioned.points[0]),
                self.view_state.model_to_screen(positioned.points[1]),
                self.view_state.model_to_screen(positioned.points[2]),
            ];

            // Draw triangle
            draw_triangle(
                &painter,
                points_screen,
                default_triangle_fill(),
                DEFAULT_TRIANGLE_STROKE,
                1.0,
            );

            // Draw dimension values if enabled
            if self.show_dimensions {
                use crate::csv::ConnectionType;
                let side_lengths = [
                    positioned.data.side_a,
                    positioned.data.side_b,
                    positioned.data.side_c,
                ];
                // Skip side A dimension if this triangle is connected to a parent
                let skip_side_a = positioned.data.connection_type != ConnectionType::Independent;

                if let Some(ref text_renderer) = self.canvas2d_text_renderer {
                    if self.use_bbox_text {
                        // BBOX-based: text scales with zoom (CAD equivalent)
                        use crate::render::dimension::{draw_triangle_dimensions_bbox, BboxDimensionStyle};
                        let dim_style = BboxDimensionStyle::default();
                        draw_triangle_dimensions_bbox(
                            text_renderer,
                            positioned.points,
                            side_lengths,
                            &self.view_state,
                            &dim_style,
                            skip_side_a,
                            self.auto_readable_text,
                        );
                    } else {
                        // Fixed size: text stays readable at any zoom
                        use crate::render::dimension::{draw_triangle_dimensions, DimensionStyle};
                        let dim_style = DimensionStyle::default();
                        draw_triangle_dimensions(
                            text_renderer,
                            positioned.points,
                            side_lengths,
                            &self.view_state,
                            &dim_style,
                            skip_side_a,
                        );
                    }
                } else {
                    // Fallback: draw dimensions using egui (no rotation)
                    use crate::render::text::{draw_text_cad_style, TextAlign, HorizontalAlign, VerticalAlign};
                    let dim_style_color = egui::Color32::WHITE;

                    // Draw each dimension at edge midpoint
                    let edges = [
                        (0, 1, side_lengths[0], skip_side_a),  // Side A
                        (1, 2, side_lengths[1], false),        // Side B
                        (2, 0, side_lengths[2], false),        // Side C
                    ];

                    for (i, j, length, skip) in edges {
                        if skip { continue; }
                        let p1 = points_screen[i];
                        let p2 = points_screen[j];
                        let mid = Pos2::new((p1.x + p2.x) * 0.5, (p1.y + p2.y) * 0.5);

                        // Calculate perpendicular offset
                        let dir = (p2 - p1).normalized();
                        let perp = egui::Vec2::new(-dir.y, dir.x);
                        let text_pos = mid + perp * 12.0;

                        // Calculate angle for text direction adjustment
                        let mut angle = dir.y.atan2(dir.x);
                        if angle > std::f32::consts::FRAC_PI_2 || angle < -std::f32::consts::FRAC_PI_2 {
                            angle += std::f32::consts::PI;
                        }

                        let text = format!("{:.2}", length);
                        let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle);
                        draw_text_cad_style(
                            &painter,
                            &text,
                            text_pos,
                            12.0,
                            angle.to_degrees(),
                            dim_style_color,
                            align,
                        );
                    }
                }
            }

            // Draw triangle number at centroid
            let centroid_model = positioned.centroid();
            let centroid_screen = self.view_state.model_to_screen(centroid_model);

            // Use Canvas 2D API for text if enabled, otherwise use egui
            if self.use_canvas2d_text {
                if let Some(ref text_renderer) = self.canvas2d_text_renderer {
                    let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle);
                    let color = color32_to_css(DEFAULT_TEXT_COLOR);
                    let _ = text_renderer.draw_text(
                        &positioned.data.number.to_string(),
                        centroid_screen.x as f64,
                        centroid_screen.y as f64,
                        14.0,
                        0.0,
                        align,
                        &color,
                    );
                }
            } else {
                // Use egui Painter (existing implementation)
                draw_triangle_number(
                    &painter,
                    centroid_screen,
                    positioned.data.number,
                    14.0,
                    DEFAULT_TEXT_COLOR,
                );
            }
        }
    }

    /// Renders the triangle list
    fn render_triangle_list(&mut self, ui: &mut egui::Ui) {
        if self.triangles.is_empty() {
            return;
        }

        // DXF download button
        ui.add_space(10.0);
        ui.horizontal(|ui| {
            if ui.button("Download DXF").clicked() {
                self.download_dxf_file();
            }
        });
        ui.add_space(10.0);

        ui.add_space(10.0);
        ui.separator();
        ui.add_space(10.0);

        ui.heading(format!("Parsed Triangles ({})", self.triangles.len()));
        ui.add_space(5.0);

        egui::ScrollArea::vertical()
            .max_height(300.0)
            .show(ui, |ui| {
                egui::Grid::new("triangle_grid")
                    .num_columns(7)
                    .spacing([10.0, 4.0])
                    .striped(true)
                    .show(ui, |ui| {
                        // Header
                        ui.strong("No.");
                        ui.strong("Side A");
                        ui.strong("Side B");
                        ui.strong("Side C");
                        ui.strong("Parent");
                        ui.strong("Connection");
                        ui.strong("Status");
                        ui.end_row();

                        // Data rows
                        for triangle in &self.triangles {
                            ui.label(triangle.number.to_string());
                            ui.label(format!("{:.2}", triangle.side_a));
                            ui.label(format!("{:.2}", triangle.side_b));
                            ui.label(format!("{:.2}", triangle.side_c));

                            if triangle.is_independent() {
                                ui.label("-");
                                ui.label("Independent");
                            } else {
                                ui.label(triangle.parent_number.to_string());
                                let conn_text = match triangle.connection_type {
                                    crate::csv::ConnectionType::ToParentB => "→ Parent B",
                                    crate::csv::ConnectionType::ToParentC => "→ Parent C",
                                    _ => "-",
                                };
                                ui.label(conn_text);
                            }

                            if triangle.is_valid_triangle() {
                                ui.colored_label(egui::Color32::GREEN, "Valid");
                            } else {
                                ui.colored_label(egui::Color32::RED, "Invalid");
                            }

                            ui.end_row();
                        }
                    });
            });
    }

    /// Renders road section geometry
    fn render_road_section(&self, painter: &egui::Painter) {
        if let Some(ref geometry) = self.road_section_geometry {
            if let Some(ref text_renderer) = self.canvas2d_text_renderer {
                if self.use_bbox_text {
                    // BBOX-based: text scales with zoom (CAD equivalent)
                    draw_road_section_bbox(painter, text_renderer, geometry, &self.view_state, self.auto_readable_text);
                } else {
                    // Fixed size: text stays readable at any zoom
                    draw_road_section_canvas2d(painter, text_renderer, geometry, &self.view_state);
                }
            } else {
                // Fallback to egui (no text rotation)
                draw_road_section_egui(painter, geometry, &self.view_state);
            }
        }
    }

    /// Downloads triangles as DXF file
    ///
    /// This function converts the parsed triangles to DXF format and triggers
    /// a download in the browser (WASM only). On non-WASM platforms, this
    /// function logs the DXF content information but does not perform any
    /// file operations.
    ///
    /// # Errors
    ///
    /// If the conversion or download fails, an error is logged and set in
    /// `error_message` for display to the user.
    fn download_dxf_file(&mut self) {
        // Handle based on display mode
        match self.display_mode {
            DisplayMode::RoadSection => {
                self.download_road_section_dxf();
                return;
            }
            DisplayMode::Triangle => {
                // Continue with triangle export below
            }
        }

        if self.triangles.is_empty() {
            log::warn!("No triangles to export");
            self.error_message = Some("No triangles to export".to_string());
            return;
        }

        // Convert triangles to DXF entities
        let converter = TriangleToDxfConverter::new();
        let (lines, texts) = converter.convert(&self.triangles);

        // Check if conversion produced any entities
        if lines.is_empty() && texts.is_empty() {
            log::warn!("DXF conversion produced no entities");
            self.error_message = Some("Failed to convert triangles to DXF entities".to_string());
            return;
        }

        // Generate DXF content
        let writer = dxf::DxfWriter::new();
        let dxf_content = writer.write(&lines, &texts);

        // Download the file
        #[cfg(target_arch = "wasm32")]
        {
            if let Err(e) = download_dxf(&dxf_content, "triangles.dxf") {
                log::error!("Failed to download DXF: {:?}", e);
                self.error_message = Some(format!("Failed to download DXF file: {:?}", e));
            } else {
                log::info!("DXF file downloaded successfully ({} lines, {} texts)", lines.len(), texts.len());
                self.error_message = None;
            }
        }

        #[cfg(not(target_arch = "wasm32"))]
        {
            log::info!("DXF content generated ({} lines, {} texts)", lines.len(), texts.len());
            // In non-WASM environments, you might want to write to a file
        }
    }

    /// Downloads road section as DXF file
    fn download_road_section_dxf(&mut self) {
        if let Some(ref geometry) = self.road_section_geometry {
            let (lines, texts) = geometry_to_dxf(geometry);

            if lines.is_empty() && texts.is_empty() {
                log::warn!("Road section DXF has no entities");
                self.error_message = Some("No road section data to export".to_string());
                return;
            }

            // Generate DXF content
            let writer = dxf::DxfWriter::new();
            let dxf_content = writer.write(&lines, &texts);

            // Download the file
            #[cfg(target_arch = "wasm32")]
            {
                if let Err(e) = download_dxf(&dxf_content, "road_section.dxf") {
                    log::error!("Failed to download DXF: {:?}", e);
                    self.error_message = Some(format!("Failed to download DXF file: {:?}", e));
                } else {
                    log::info!("Road section DXF downloaded ({} lines, {} texts)", lines.len(), texts.len());
                    self.error_message = None;
                }
            }

            #[cfg(not(target_arch = "wasm32"))]
            {
                log::info!("Road section DXF generated ({} lines, {} texts)", lines.len(), texts.len());
            }
        } else {
            self.error_message = Some("No road section data to export".to_string());
        }
    }
}

impl eframe::App for TriangleListApp {
    /// Called each frame to update and render the UI.
    ///
    /// # Arguments
    /// * `ctx` - The egui context for rendering
    /// * `_frame` - The eframe Frame (unused in WASM)
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        // Initialize Canvas 2D renderer on first frame (DOM should be ready by now)
        if self.canvas2d_text_renderer.is_none() {
            self.init_canvas2d_renderer();
        }

        // Handle dropped files
        ctx.input(|i| {
            if !i.raw.dropped_files.is_empty() {
                for file in &i.raw.dropped_files {
                    if let Some(bytes) = &file.bytes {
                        // Skip UTF-8 BOM if present
                        let bytes_without_bom = if bytes.starts_with(&[0xEF, 0xBB, 0xBF]) {
                            &bytes[3..]
                        } else {
                            bytes.as_ref()
                        };

                        // Try UTF-8 first
                        let content = if let Ok(utf8_content) = std::str::from_utf8(bytes_without_bom) {
                            Some(utf8_content.to_string())
                        } else {
                            // Fallback to Shift-JIS (common for Japanese Windows files)
                            let (decoded, _, had_errors) = encoding_rs::SHIFT_JIS.decode(bytes_without_bom);
                            if !had_errors {
                                log::info!("File decoded as Shift-JIS");
                                Some(decoded.into_owned())
                            } else {
                                None
                            }
                        };

                        if let Some(content) = content {
                            self.csv_input = content.clone();
                            self.parse_and_cache_csv(&content);
                        } else {
                            self.error_message = Some("ファイルの読み込みに失敗しました。UTF-8またはShift-JISで保存してください。".to_string());
                        }
                    }
                }
            }

            // Update hover state for drop zone
            self.is_drop_hover = !i.raw.hovered_files.is_empty();
        });

        // Check for file content from file picker (mobile support)
        #[cfg(target_arch = "wasm32")]
        if let Some(content) = Self::take_pending_file_content() {
            log::info!("Processing file from picker: {} bytes", content.len());
            self.csv_input = content.clone();
            self.parse_and_cache_csv(&content);
        }

        // Side panel for settings and file operations
        if self.show_side_panel {
            egui::SidePanel::right("side_panel")
                .resizable(true)
                .default_width(300.0)
                .show(ctx, |ui| {
                    ui.heading("Settings");
                    ui.separator();

                    // File operations
                    ui.group(|ui| {
                        ui.heading("File");
                        ui.add_space(5.0);
                        if ui.button("Load CSV File").clicked() {
                            Self::open_file_picker();
                        }
                        ui.add_space(5.0);
                        if ui.button("Download DXF").clicked() {
                            self.download_dxf_file();
                        }
                        ui.add_space(5.0);
                        if ui.button("Clear").clicked() {
                            self.csv_input.clear();
                            self.triangles.clear();
                            self.stations.clear();
                            self.road_section_geometry = None;
                            self.error_message = None;
                            self.warnings.clear();
                            self.show_canvas = false;
                            #[cfg(target_arch = "wasm32")]
                            Self::clear_csv_storage();
                        }
                    });
                    ui.add_space(10.0);

                    // Sample data
                    ui.group(|ui| {
                        ui.heading("Samples");
                        ui.add_space(5.0);
                        if ui.button("Triangle Sample").clicked() {
                            self.csv_input = SAMPLE_CSV.to_string();
                            self.parse_and_cache_csv(&self.csv_input.clone());
                        }
                        ui.add_space(5.0);
                        if ui.button("Road Section Sample").clicked() {
                            self.csv_input = SAMPLE_ROAD_SECTION_CSV.to_string();
                            self.parse_and_cache_csv(&self.csv_input.clone());
                        }
                    });
                    ui.add_space(10.0);

                    // Display options
                    ui.group(|ui| {
                        ui.heading("Display");
                        ui.add_space(5.0);
                        ui.checkbox(&mut self.show_dimensions, "Show Dimension Lines");
                        ui.add_space(5.0);
                        if ui.checkbox(&mut self.use_canvas2d_text, "Canvas 2D Text (CAD Quality)").changed() {
                            if self.use_canvas2d_text {
                                self.init_canvas2d_renderer();
                            } else {
                                self.canvas2d_text_renderer = None;
                            }
                        }
                        ui.add_space(5.0);
                        ui.checkbox(&mut self.use_bbox_text, "Text scales with zoom");
                        ui.add_space(5.0);
                        ui.checkbox(&mut self.auto_readable_text, "Auto-readable text direction");
                    });
                    ui.add_space(10.0);

                    // View controls
                    if self.show_canvas && !self.triangles.is_empty() {
                        ui.group(|ui| {
                            ui.heading("View");
                            ui.add_space(5.0);
                            if ui.button("Fit View").clicked() {
                                self.view_state.fit_to_triangles(&self.triangles);
                            }
                            ui.add_space(5.0);
                            ui.horizontal(|ui| {
                                ui.label("Zoom:");
                                ui.label(format!("{:.1}%", self.view_state.zoom * 100.0));
                            });
                            ui.add_space(5.0);
                            ui.horizontal(|ui| {
                                ui.label("Pan:");
                                ui.label(format!("({:.1}, {:.1})", self.view_state.pan.x, self.view_state.pan.y));
                            });
                        });
                        ui.add_space(10.0);
                    }

                    // Triangle info
                    if !self.triangles.is_empty() {
                        ui.group(|ui| {
                            ui.heading("Statistics");
                            ui.add_space(5.0);
                            ui.label(format!("Triangles: {}", self.triangles.len()));
                            let total_area: f64 = self.triangles.iter()
                                .map(|t| {
                                    // Heron's formula
                                    let s = (t.side_a + t.side_b + t.side_c) / 2.0;
                                    (s * (s - t.side_a) * (s - t.side_b) * (s - t.side_c)).max(0.0).sqrt()
                                })
                                .sum();
                            ui.label(format!("Total Area: {:.2} m²", total_area));
                        });
                    }
                });
        }

        // Status bar at the bottom
        egui::TopBottomPanel::bottom("status_bar")
            .resizable(false)
            .show(ctx, |ui| {
                ui.horizontal(|ui| {
                    // Mouse position in model coordinates
                    if let Some(pos) = self.mouse_model_pos {
                        ui.label(format!("Model: ({:.2}, {:.2})", pos.x, pos.y));
                    } else {
                        ui.label("Model: (--, --)");
                    }

                    ui.separator();

                    // Zoom level
                    ui.label(format!("Zoom: {:.1}%", self.view_state.zoom * 100.0));

                    ui.separator();

                    // Mode and count
                    match self.display_mode {
                        DisplayMode::Triangle => {
                            ui.label(format!("Mode: Triangle | Count: {}", self.triangles.len()));
                        }
                        DisplayMode::RoadSection => {
                            ui.label(format!("Mode: Road Section | Stations: {}", self.stations.len()));
                        }
                    }

                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        if ui.button("Toggle Side Panel").clicked() {
                            self.show_side_panel = !self.show_side_panel;
                        }
                    });
                });
            });

        // Main panel
        egui::CentralPanel::default().show(ctx, |ui| {
            let has_data = match self.display_mode {
                DisplayMode::Triangle => !self.triangles.is_empty(),
                DisplayMode::RoadSection => self.road_section_geometry.is_some(),
            };

            if !self.show_canvas || !has_data {
                // Initial view: file input
                ui.vertical_centered(|ui| {
                    ui.add_space(20.0);
                    ui.heading("TriangleList Web");
                    ui.add_space(10.0);
                    ui.label("Triangle mesh editor - CSV Parser");
                    ui.add_space(20.0);
                });

                // Drop zone
                self.render_drop_zone(ui);

                // CSV text input
                self.render_csv_input(ui);

                // Error display
                self.render_error(ui);

                // Warnings display
                self.render_warnings(ui);

                // Triangle list display if we have triangles
                if !self.triangles.is_empty() {
                    ui.add_space(10.0);
                    if ui.button("Show Canvas").clicked() {
                        self.show_canvas = true;
                    }
                    ui.add_space(10.0);
                    self.render_triangle_list(ui);
                }
            } else {
                // Canvas view
                self.render_canvas(ui);
            }
        });
    }

    /// Called when the application is about to be saved/closed.
    /// Used for persisting application state.
    fn save(&mut self, _storage: &mut dyn eframe::Storage) {
        // Save application state here if persistence is needed
    }
}

/// Sample CSV content for testing
const SAMPLE_CSV: &str = r#"番号
辺A
辺B
辺C
1, 6.0, 5.0, 4.0, -1, -1
2, 5.0, 4.0, 3.0, 1, 1
3, 4.0, 3.5, 3.0, 1, 2
4, 3.0, 4.0, 5.0, 2, 1
"#;

const SAMPLE_ROAD_SECTION_CSV: &str = r#"測点名,累積延長,左幅員,右幅員
No.1,0.0,2.5,2.5
No.1+10,10.0,2.5,3.0
No.2,20.0,3.0,3.0
No.2+10,30.0,2.5,2.5
No.3,40.0,2.5,2.5
"#;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_app_creation() {
        let app = TriangleListApp::default();
        assert!(app.triangles.is_empty());
        assert!(app.error_message.is_none());
        assert!(app.warnings.is_empty());
    }

    #[test]
    fn test_sample_csv_parsing() {
        let result = parse_csv(SAMPLE_CSV);
        assert!(result.is_ok());
        let parsed = result.unwrap();
        assert_eq!(parsed.triangles.len(), 4);
    }
}
