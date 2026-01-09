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
use eframe::egui::Pos2;
use wasm_bindgen::JsCast;

/// The main TriangleList application struct.
///
/// This struct holds the application state and implements the eframe::App trait
/// for rendering the UI.
pub struct TriangleListApp {
    /// Parsed triangles from CSV
    triangles: Vec<ParsedTriangle>,
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
}

impl Default for TriangleListApp {
    fn default() -> Self {
        Self {
            triangles: Vec::new(),
            error_message: None,
            warnings: Vec::new(),
            is_drop_hover: false,
            csv_input: String::new(),
            view_state: ViewState::default(),
            show_canvas: false,
            show_dimensions: false,
            use_canvas2d_text: false, // Default to egui for now
            canvas2d_text_renderer: None,
            mouse_model_pos: None,
            show_side_panel: true,
        }
    }
}

impl TriangleListApp {
    /// Creates a new TriangleListApp instance.
    ///
    /// # Arguments
    /// * `_cc` - The eframe CreationContext, providing access to egui context and storage
    ///
    /// # Returns
    /// A new TriangleListApp instance
    pub fn new(_cc: &eframe::CreationContext<'_>) -> Self {
        // Customize egui style if needed
        // cc.egui_ctx.set_visuals(egui::Visuals::dark());

        Self::default()
    }

    /// Initialize Canvas 2D text renderer
    /// 
    /// Attempts to get the canvas element from the DOM and create a renderer.
    /// Note: egui uses WebGL internally, so we need to create a separate canvas layer
    /// for Canvas 2D API text rendering, or use egui's canvas if accessible.
    fn init_canvas2d_renderer(&mut self) {
        #[cfg(target_arch = "wasm32")]
        {
            use web_sys::window;
            
            if let Some(window) = window() {
                if let Some(document) = window.document() {
                    // Try to get the canvas element from DOM
                    // First try the main canvas element
                    if let Some(canvas) = document.get_element_by_id("trianglelist-canvas") {
                        if let Ok(canvas_element) = canvas.dyn_into::<web_sys::HtmlCanvasElement>() {
                            match Canvas2dTextRenderer::new(&canvas_element) {
                                Ok(renderer) => {
                                    self.canvas2d_text_renderer = Some(renderer);
                                    log::info!("Canvas 2D text renderer initialized with trianglelist-canvas");
                                }
                                Err(e) => {
                                    log::warn!("Failed to initialize Canvas 2D renderer: {:?}", e);
                                    // Try to find egui's canvas (usually has id "canvas" or similar)
                                    self.try_init_with_egui_canvas(&document);
                                }
                            }
                        } else {
                            self.try_init_with_egui_canvas(&document);
                        }
                    } else {
                        self.try_init_with_egui_canvas(&document);
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

    /// Try to initialize Canvas 2D renderer with egui's canvas element
    #[cfg(target_arch = "wasm32")]
    fn try_init_with_egui_canvas(&mut self, document: &web_sys::Document) {
        
        // egui usually creates a canvas with id "canvas" or finds the first canvas
        // Try common canvas element IDs
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
                let result = parse_csv(&self.csv_input);
                self.handle_parse_result(result);
            }

            if ui.button("Clear").clicked() {
                self.csv_input.clear();
                self.triangles.clear();
                self.error_message = None;
                self.warnings.clear();
            }

            if ui.button("Load Sample").clicked() {
                self.csv_input = SAMPLE_CSV.to_string();
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
        use crate::render::canvas::calculate_triangle_points;

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

        // Handle pan (drag)
        if response.dragged_by(egui::PointerButton::Primary) {
            self.view_state.pan += response.drag_delta();
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

        // Draw triangles
        // For now, use a simple layout: place independent triangles in a row
        let mut current_x = 0.0f32;
        let base_y = 0.0f32;

        for triangle in &self.triangles {
            // Calculate triangle points (in model coordinates)
            let points_model = calculate_triangle_points(triangle);
            
            // Offset for layout (simple row layout for now)
            let offset = Pos2::new(current_x, base_y);
            let offset_vec = offset.to_vec2();
            let points_offset: [Pos2; 3] = [
                points_model[0] + offset_vec,
                points_model[1] + offset_vec,
                points_model[2] + offset_vec,
            ];

            // Convert to screen coordinates
            let points_screen: [Pos2; 3] = [
                self.view_state.model_to_screen(points_offset[0]),
                self.view_state.model_to_screen(points_offset[1]),
                self.view_state.model_to_screen(points_offset[2]),
            ];

            // Draw triangle
            draw_triangle(
                &painter,
                points_screen,
                default_triangle_fill(),
                DEFAULT_TRIANGLE_STROKE,
                1.0,
            );

            // Draw dimension lines if enabled
            if self.show_dimensions {
                use crate::render::dimension::{draw_triangle_dimensions, DimensionStyle};
                let dim_style = DimensionStyle::default();
                let side_lengths = [
                    triangle.side_a,
                    triangle.side_b,
                    triangle.side_c,
                ];
                draw_triangle_dimensions(
                    &painter,
                    points_screen,
                    side_lengths,
                    &self.view_state,
                    &dim_style,
                );
            }

            // Draw triangle number at centroid
            let centroid_model = Pos2::new(
                (points_offset[0].x + points_offset[1].x + points_offset[2].x) / 3.0,
                (points_offset[0].y + points_offset[1].y + points_offset[2].y) / 3.0,
            );
            
            // Use Canvas 2D API for text if enabled, otherwise use egui
            if self.use_canvas2d_text {
                // Try to get canvas element and render with Canvas 2D API
                if let Some(ref text_renderer) = self.canvas2d_text_renderer {
                    let align = TextAlign::new(HorizontalAlign::Center, VerticalAlign::Middle);
                    let color = color32_to_css(DEFAULT_TEXT_COLOR);
                    let _ = text_renderer.draw_text_cad_style(
                        &triangle.number.to_string(),
                        centroid_model.x as f64,
                        centroid_model.y as f64,
                        14.0, // Model coordinate height
                        0.0,  // No rotation
                        align,
                        &color,
                        &self.view_state,
                        true, // Zoom-dependent size
                    );
                }
            } else {
                // Use egui Painter (existing implementation)
                let centroid_screen = self.view_state.model_to_screen(centroid_model);
                draw_triangle_number(
                    &painter,
                    centroid_screen,
                    triangle.number,
                    14.0,
                    DEFAULT_TEXT_COLOR,
                );
            }

            // Update position for next triangle (simple spacing)
            let max_x = points_offset[0].x.max(points_offset[1].x).max(points_offset[2].x);
            current_x = max_x + triangle.side_c as f32 * 0.2; // Add spacing
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
}

impl eframe::App for TriangleListApp {
    /// Called each frame to update and render the UI.
    ///
    /// # Arguments
    /// * `ctx` - The egui context for rendering
    /// * `_frame` - The eframe Frame (unused in WASM)
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        // Handle dropped files
        ctx.input(|i| {
            if !i.raw.dropped_files.is_empty() {
                for file in &i.raw.dropped_files {
                    if let Some(bytes) = &file.bytes {
                        if let Ok(content) = std::str::from_utf8(bytes) {
                            let result = parse_csv(content);
                            self.handle_parse_result(result);
                        } else {
                            self.error_message = Some("Failed to read file as UTF-8".to_string());
                        }
                    }
                }
            }

            // Update hover state for drop zone
            self.is_drop_hover = !i.raw.hovered_files.is_empty();
        });

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
                        ui.button("Load CSV File").on_hover_text("Click to select CSV file");
                        ui.add_space(5.0);
                        if ui.button("Download DXF").clicked() {
                            self.download_dxf_file();
                        }
                        ui.add_space(5.0);
                        if ui.button("Clear").clicked() {
                            self.csv_input.clear();
                            self.triangles.clear();
                            self.error_message = None;
                            self.warnings.clear();
                            self.show_canvas = false;
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
                    
                    // Triangle count
                    ui.label(format!("Triangles: {}", self.triangles.len()));
                    
                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        if ui.button("Toggle Side Panel").clicked() {
                            self.show_side_panel = !self.show_side_panel;
                        }
                    });
                });
            });

        // Main panel
        egui::CentralPanel::default().show(ctx, |ui| {
            if !self.show_canvas || self.triangles.is_empty() {
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
