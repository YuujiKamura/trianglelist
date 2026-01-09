//! TriangleList Application
//!
//! This module defines the main application struct and implements
//! the eframe::App trait for UI rendering.

use eframe::egui;

use crate::csv::{parse_csv, ParseError, ParseResult, ParsedTriangle};
use crate::render::ViewState;

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

    /// Renders the triangle list
    fn render_triangle_list(&self, ui: &mut egui::Ui) {
        if self.triangles.is_empty() {
            return;
        }

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

        egui::CentralPanel::default().show(ctx, |ui| {
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

            // Triangle list display
            self.render_triangle_list(ui);
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
