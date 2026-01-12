//! Longitudinal profile drawing generator
//!
//! This module provides functionality for generating civil engineering standard
//! longitudinal profile drawings in DXF format.
//!
//! # Features
//! - Slope change point annotations (ΔV, i, L)
//! - Station labels at graph top
//! - Title block with scale information
//! - Elevation labels at start/end points
//!
//! # Example
//! ```
//! use dxf::longitudinal::{StationPoint, LongitudinalProfile, LongitudinalDrawingGenerator};
//!
//! let points = vec![
//!     StationPoint::new("No.0", 0.0, 100.0, 100.0),
//!     StationPoint::new("No.1", 20.0, 102.0, 101.5),
//!     StationPoint::new("No.2", 40.0, 101.5, 102.0),
//! ];
//!
//! let profile = LongitudinalProfile::new(points);
//! let generator = LongitudinalDrawingGenerator::new();
//! let dxf_content = generator.generate(&profile);
//! ```

use crate::dxf::entities::{DxfLine, DxfText, HorizontalAlignment, VerticalAlignment};
use crate::dxf::writer::DxfWriter;

// =============================================================================
// Constants
// =============================================================================

/// Epsilon for horizontal distance comparisons (meters)
/// Used to avoid division by zero when calculating slopes
const HORIZONTAL_DIST_EPSILON: f64 = 0.001;

/// Epsilon for grade difference comparisons (percent)
/// Used to filter insignificant grade changes in annotations
const GRADE_DIFF_EPSILON: f64 = 0.001;

// AutoCAD Color Index (ACI) values
/// Gray color for grid lines
const COLOR_GRAY: i32 = 8;
/// Green color for ground elevation line
const COLOR_GREEN: i32 = 3;
/// Red color for planned elevation line
const COLOR_RED: i32 = 1;
/// Blue color for slope annotations
const COLOR_BLUE: i32 = 5;

// =============================================================================
// Data Structures
// =============================================================================

/// Station point data for longitudinal profile
#[derive(Clone, Debug, PartialEq)]
pub struct StationPoint {
    /// Station name (e.g., "No.0", "No.1+5.0")
    pub name: String,
    /// Horizontal distance from start point (meters)
    pub distance: f64,
    /// Ground elevation (meters)
    pub ground_elevation: f64,
    /// Planned elevation (meters)
    pub planned_elevation: f64,
}

impl StationPoint {
    /// Creates a new station point
    pub fn new(name: &str, distance: f64, ground_elevation: f64, planned_elevation: f64) -> Self {
        Self {
            name: name.to_string(),
            distance,
            ground_elevation,
            planned_elevation,
        }
    }
}

/// Slope annotation data for grade break points
#[derive(Clone, Debug, PartialEq)]
pub struct SlopeAnnotation {
    /// Station point index
    pub point_index: usize,
    /// Grade difference from previous segment (ΔV in %)
    pub delta_v: f64,
    /// Current slope percentage (i in %)
    pub slope_percent: f64,
    /// Segment length from this point (L in meters)
    pub segment_length: f64,
}

/// Longitudinal profile data
#[derive(Clone, Debug)]
pub struct LongitudinalProfile {
    /// Station points along the profile
    pub points: Vec<StationPoint>,
    /// Drawing title
    pub title: String,
    /// Horizontal scale (1:n)
    pub horizontal_scale: f64,
    /// Vertical scale (1:n)
    pub vertical_scale: f64,
    /// Datum level (DL) for elevation reference
    pub datum_level: f64,
}

impl LongitudinalProfile {
    /// Creates a new longitudinal profile with default settings
    pub fn new(points: Vec<StationPoint>) -> Self {
        let datum_level = Self::calculate_datum_level(&points);
        Self {
            points,
            title: String::from("縦断図"),
            horizontal_scale: 1000.0,
            vertical_scale: 100.0,
            datum_level,
        }
    }

    /// Creates a new longitudinal profile with custom settings
    pub fn with_settings(
        points: Vec<StationPoint>,
        title: &str,
        horizontal_scale: f64,
        vertical_scale: f64,
        datum_level: f64,
    ) -> Self {
        Self {
            points,
            title: title.to_string(),
            horizontal_scale,
            vertical_scale,
            datum_level,
        }
    }

    /// Calculates recommended datum level from points
    fn calculate_datum_level(points: &[StationPoint]) -> f64 {
        if points.is_empty() {
            return 0.0;
        }
        let min_elevation = points
            .iter()
            .map(|p| p.ground_elevation.min(p.planned_elevation))
            .fold(f64::INFINITY, f64::min);
        // Round down to nearest 10m below minimum
        (min_elevation / 10.0).floor() * 10.0 - 10.0
    }

    /// Calculates slope annotations for all grade break points
    pub fn calculate_slope_annotations(&self) -> Vec<SlopeAnnotation> {
        let mut annotations = Vec::new();
        if self.points.len() < 2 {
            return annotations;
        }

        let mut prev_slope: Option<f64> = None;

        for (i, window) in self.points.windows(2).enumerate() {
            let p1 = &window[0];
            let p2 = &window[1];

            let horizontal_dist = p2.distance - p1.distance;
            let elevation_diff = p2.planned_elevation - p1.planned_elevation;

            // Calculate slope percentage (i)
            let slope_percent = if horizontal_dist.abs() > HORIZONTAL_DIST_EPSILON {
                (elevation_diff / horizontal_dist) * 100.0
            } else {
                0.0
            };

            // Calculate delta_v (grade difference from previous segment)
            let delta_v = match prev_slope {
                Some(prev) => slope_percent - prev,
                None => 0.0,
            };

            annotations.push(SlopeAnnotation {
                point_index: i,
                delta_v,
                slope_percent,
                segment_length: horizontal_dist,
            });

            prev_slope = Some(slope_percent);
        }

        annotations
    }

    /// Returns the start distance of the profile
    fn start_distance(&self) -> f64 {
        self.points.first().map(|p| p.distance).unwrap_or(0.0)
    }
}

/// Drawing configuration for longitudinal profile
#[derive(Clone, Debug)]
pub struct DrawingConfig {
    // Text heights
    /// Text height for station labels
    pub station_label_height: f64,
    /// Text height for elevation labels
    pub elevation_label_height: f64,
    /// Text height for slope annotations
    pub slope_annotation_height: f64,
    /// Text height for title
    pub title_height: f64,

    // Graph area layout
    /// X-axis offset for graph area from origin
    pub graph_offset_x: f64,
    /// Y-axis offset for graph area from origin
    pub graph_offset_y: f64,
    /// Width of the graph area (mm in drawing)
    pub graph_width: f64,
    /// Height of the graph area (mm in drawing)
    pub graph_height: f64,

    // Layout offsets
    /// Padding above graph for elevation range calculation
    pub elevation_padding: f64,
    /// Y offset for station labels above graph top
    pub station_label_offset_y: f64,
    /// X offset for elevation labels from graph edge
    pub elevation_label_offset_x: f64,
    /// Y offset for title above graph top
    pub title_offset_y: f64,
    /// Y spacing between title and scale text
    pub scale_text_offset_y: f64,
    /// Half-height of tick marks at grade break points
    pub tick_mark_half_height: f64,
    /// Y offset for slope annotation text below point
    pub slope_annotation_offset_y: f64,
    /// X offset for slope annotation text from point
    pub slope_annotation_offset_x: f64,
    /// Y spacing between annotation lines
    pub annotation_line_spacing: f64,

    // Layer names
    /// Layer name for ground line
    pub layer_ground: String,
    /// Layer name for planned line
    pub layer_planned: String,
    /// Layer name for annotations
    pub layer_annotation: String,
    /// Layer name for grid
    pub layer_grid: String,
}

impl Default for DrawingConfig {
    fn default() -> Self {
        Self {
            // Text heights
            station_label_height: 2.5,
            elevation_label_height: 2.5,
            slope_annotation_height: 2.0,
            title_height: 5.0,

            // Graph area layout
            graph_offset_x: 50.0,
            graph_offset_y: 30.0,
            graph_width: 250.0,
            graph_height: 150.0,

            // Layout offsets
            elevation_padding: 5.0,
            station_label_offset_y: 5.0,
            elevation_label_offset_x: 3.0,
            title_offset_y: 20.0,
            scale_text_offset_y: 7.0,
            tick_mark_half_height: 2.0,
            slope_annotation_offset_y: 8.0,
            slope_annotation_offset_x: 2.0,
            annotation_line_spacing: 3.0,

            // Layer names
            layer_ground: String::from("GROUND"),
            layer_planned: String::from("PLANNED"),
            layer_annotation: String::from("ANNOTATION"),
            layer_grid: String::from("GRID"),
        }
    }
}

/// Longitudinal profile drawing generator
pub struct LongitudinalDrawingGenerator {
    config: DrawingConfig,
}

impl LongitudinalDrawingGenerator {
    /// Creates a new generator with default configuration
    pub fn new() -> Self {
        Self {
            config: DrawingConfig::default(),
        }
    }

    /// Creates a new generator with custom configuration
    pub fn with_config(config: DrawingConfig) -> Self {
        Self { config }
    }

    /// Generates DXF content for the longitudinal profile
    pub fn generate(&self, profile: &LongitudinalProfile) -> String {
        let mut lines: Vec<DxfLine> = Vec::new();
        let mut texts: Vec<DxfText> = Vec::new();

        // Generate all drawing elements
        self.generate_grid(&mut lines, profile);
        self.generate_ground_line(&mut lines, profile);
        self.generate_planned_line(&mut lines, profile);
        self.generate_station_labels(&mut texts, profile);
        self.generate_elevation_labels(&mut texts, profile);
        self.generate_slope_annotations(&mut lines, &mut texts, profile);
        self.generate_title_block(&mut texts, profile);

        // Write to DXF
        let writer = DxfWriter::new();
        writer.write(&lines, &texts)
    }

    /// Converts world coordinates to drawing coordinates
    ///
    /// Distance is normalized relative to the start point, so profiles
    /// starting at non-zero chainage (e.g., chain 1000) are correctly
    /// positioned with the first point at the left edge of the graph.
    fn world_to_drawing(&self, profile: &LongitudinalProfile, distance: f64, elevation: f64) -> (f64, f64) {
        let total_distance = self.get_total_distance(profile);
        let start_distance = profile.start_distance();
        let elevation_range = self.get_elevation_range(profile);

        // Normalize distance relative to start point
        let relative_distance = distance - start_distance;
        let x = if total_distance > 0.0 {
            self.config.graph_offset_x + (relative_distance / total_distance) * self.config.graph_width
        } else {
            self.config.graph_offset_x
        };

        let y = if elevation_range > 0.0 {
            self.config.graph_offset_y
                + ((elevation - profile.datum_level) / elevation_range) * self.config.graph_height
        } else {
            self.config.graph_offset_y
        };

        (x, y)
    }

    /// Gets total horizontal distance
    fn get_total_distance(&self, profile: &LongitudinalProfile) -> f64 {
        match (profile.points.first(), profile.points.last()) {
            (Some(first), Some(last)) => last.distance - first.distance,
            _ => 0.0,
        }
    }

    /// Gets elevation range for scaling
    fn get_elevation_range(&self, profile: &LongitudinalProfile) -> f64 {
        if profile.points.is_empty() {
            return 1.0;
        }
        let max_elev = profile
            .points
            .iter()
            .map(|p| p.ground_elevation.max(p.planned_elevation))
            .fold(f64::NEG_INFINITY, f64::max);
        max_elev - profile.datum_level + self.config.elevation_padding
    }

    /// Generates grid lines
    fn generate_grid(&self, lines: &mut Vec<DxfLine>, profile: &LongitudinalProfile) {
        // Vertical grid lines at each station
        for point in &profile.points {
            let (x, y_bottom) = self.world_to_drawing(profile, point.distance, profile.datum_level);
            let (_, y_top) = self.world_to_drawing(
                profile,
                point.distance,
                profile.datum_level + self.get_elevation_range(profile),
            );
            lines.push(
                DxfLine::new(x, y_bottom, x, y_top)
                    .color(COLOR_GRAY)
                    .layer(&self.config.layer_grid),
            );
        }

        // Horizontal grid lines at datum level
        let (x_start, y_datum) = self.world_to_drawing(
            profile,
            profile.points.first().map(|p| p.distance).unwrap_or(0.0),
            profile.datum_level,
        );
        let (x_end, _) = self.world_to_drawing(
            profile,
            profile.points.last().map(|p| p.distance).unwrap_or(0.0),
            profile.datum_level,
        );
        lines.push(
            DxfLine::new(x_start, y_datum, x_end, y_datum)
                .color(COLOR_GRAY)
                .layer(&self.config.layer_grid),
        );
    }

    /// Generates ground elevation line
    fn generate_ground_line(&self, lines: &mut Vec<DxfLine>, profile: &LongitudinalProfile) {
        for window in profile.points.windows(2) {
            let p1 = &window[0];
            let p2 = &window[1];

            let (x1, y1) = self.world_to_drawing(profile, p1.distance, p1.ground_elevation);
            let (x2, y2) = self.world_to_drawing(profile, p2.distance, p2.ground_elevation);

            lines.push(
                DxfLine::new(x1, y1, x2, y2)
                    .color(COLOR_GREEN)
                    .layer(&self.config.layer_ground),
            );
        }
    }

    /// Generates planned elevation line
    fn generate_planned_line(&self, lines: &mut Vec<DxfLine>, profile: &LongitudinalProfile) {
        for window in profile.points.windows(2) {
            let p1 = &window[0];
            let p2 = &window[1];

            let (x1, y1) = self.world_to_drawing(profile, p1.distance, p1.planned_elevation);
            let (x2, y2) = self.world_to_drawing(profile, p2.distance, p2.planned_elevation);

            lines.push(
                DxfLine::new(x1, y1, x2, y2)
                    .color(COLOR_RED)
                    .layer(&self.config.layer_planned),
            );
        }
    }

    /// Generates station labels at graph top
    fn generate_station_labels(&self, texts: &mut Vec<DxfText>, profile: &LongitudinalProfile) {
        let y_top = self.config.graph_offset_y
            + self.config.graph_height
            + self.config.station_label_offset_y;

        for point in &profile.points {
            let (x, _) = self.world_to_drawing(profile, point.distance, profile.datum_level);

            // Station name label
            texts.push(
                DxfText::new(x, y_top, &point.name)
                    .height(self.config.station_label_height)
                    .rotation(90.0)
                    .align_h(HorizontalAlignment::Left)
                    .align_v(VerticalAlignment::Middle)
                    .layer(&self.config.layer_annotation),
            );
        }
    }

    /// Generates elevation labels at start and end points
    fn generate_elevation_labels(&self, texts: &mut Vec<DxfText>, profile: &LongitudinalProfile) {
        if profile.points.is_empty() {
            return;
        }

        let label_offset = self.config.elevation_label_offset_x;

        // Start point elevations
        let start_point = &profile.points[0];
        let (x_start, y_ground_start) =
            self.world_to_drawing(profile, start_point.distance, start_point.ground_elevation);
        let (_, y_planned_start) =
            self.world_to_drawing(profile, start_point.distance, start_point.planned_elevation);

        texts.push(
            DxfText::new(
                x_start - label_offset,
                y_ground_start,
                &format!("GL={:.3}", start_point.ground_elevation),
            )
            .height(self.config.elevation_label_height)
            .align_h(HorizontalAlignment::Right)
            .align_v(VerticalAlignment::Middle)
            .layer(&self.config.layer_annotation),
        );

        texts.push(
            DxfText::new(
                x_start - label_offset,
                y_planned_start,
                &format!("FH={:.3}", start_point.planned_elevation),
            )
            .height(self.config.elevation_label_height)
            .align_h(HorizontalAlignment::Right)
            .align_v(VerticalAlignment::Middle)
            .layer(&self.config.layer_annotation),
        );

        // End point elevations
        if profile.points.len() > 1 {
            let end_point = profile.points.last().unwrap();
            let (x_end, y_ground_end) =
                self.world_to_drawing(profile, end_point.distance, end_point.ground_elevation);
            let (_, y_planned_end) =
                self.world_to_drawing(profile, end_point.distance, end_point.planned_elevation);

            texts.push(
                DxfText::new(
                    x_end + label_offset,
                    y_ground_end,
                    &format!("GL={:.3}", end_point.ground_elevation),
                )
                .height(self.config.elevation_label_height)
                .align_h(HorizontalAlignment::Left)
                .align_v(VerticalAlignment::Middle)
                .layer(&self.config.layer_annotation),
            );

            texts.push(
                DxfText::new(
                    x_end + label_offset,
                    y_planned_end,
                    &format!("FH={:.3}", end_point.planned_elevation),
                )
                .height(self.config.elevation_label_height)
                .align_h(HorizontalAlignment::Left)
                .align_v(VerticalAlignment::Middle)
                .layer(&self.config.layer_annotation),
            );
        }

        // Datum level label
        let (x_datum, y_datum) = self.world_to_drawing(
            profile,
            profile.points.first().map(|p| p.distance).unwrap_or(0.0),
            profile.datum_level,
        );
        texts.push(
            DxfText::new(
                x_datum - label_offset,
                y_datum,
                &format!("DL={:.1}", profile.datum_level),
            )
            .height(self.config.elevation_label_height)
            .align_h(HorizontalAlignment::Right)
            .align_v(VerticalAlignment::Middle)
            .layer(&self.config.layer_annotation),
        );
    }

    /// Generates slope change point annotations (ΔV, i, L)
    fn generate_slope_annotations(
        &self,
        lines: &mut Vec<DxfLine>,
        texts: &mut Vec<DxfText>,
        profile: &LongitudinalProfile,
    ) {
        let annotations = profile.calculate_slope_annotations();
        let tick_half = self.config.tick_mark_half_height;
        let offset_x = self.config.slope_annotation_offset_x;
        let offset_y = self.config.slope_annotation_offset_y;
        let line_spacing = self.config.annotation_line_spacing;

        for annotation in &annotations {
            let point = &profile.points[annotation.point_index];
            let (x, y) = self.world_to_drawing(profile, point.distance, point.planned_elevation);

            // Draw vertical tick mark at grade break point
            lines.push(
                DxfLine::new(x, y - tick_half, x, y + tick_half)
                    .color(COLOR_BLUE)
                    .layer(&self.config.layer_annotation),
            );

            // Slope annotation text (i and L)
            let annotation_y = y - offset_y;

            // Slope percentage (i)
            let slope_text = if annotation.slope_percent >= 0.0 {
                format!("i=+{:.2}%", annotation.slope_percent)
            } else {
                format!("i={:.2}%", annotation.slope_percent)
            };
            texts.push(
                DxfText::new(x + offset_x, annotation_y, &slope_text)
                    .height(self.config.slope_annotation_height)
                    .align_h(HorizontalAlignment::Left)
                    .align_v(VerticalAlignment::Top)
                    .color(COLOR_BLUE)
                    .layer(&self.config.layer_annotation),
            );

            // Segment length (L)
            texts.push(
                DxfText::new(
                    x + offset_x,
                    annotation_y - line_spacing,
                    &format!("L={:.2}m", annotation.segment_length),
                )
                .height(self.config.slope_annotation_height)
                .align_h(HorizontalAlignment::Left)
                .align_v(VerticalAlignment::Top)
                .color(COLOR_BLUE)
                .layer(&self.config.layer_annotation),
            );

            // Grade difference (ΔV) - only show at actual grade break points
            if annotation.delta_v.abs() > GRADE_DIFF_EPSILON {
                let delta_text = if annotation.delta_v >= 0.0 {
                    format!("ΔV=+{:.2}%", annotation.delta_v)
                } else {
                    format!("ΔV={:.2}%", annotation.delta_v)
                };
                texts.push(
                    DxfText::new(
                        x + offset_x,
                        annotation_y - line_spacing * 2.0,
                        &delta_text,
                    )
                    .height(self.config.slope_annotation_height)
                    .align_h(HorizontalAlignment::Left)
                    .align_v(VerticalAlignment::Top)
                    .color(COLOR_BLUE)
                    .layer(&self.config.layer_annotation),
                );
            }
        }
    }

    /// Generates title block
    fn generate_title_block(&self, texts: &mut Vec<DxfText>, profile: &LongitudinalProfile) {
        // Title at top-left
        let title_x = self.config.graph_offset_x;
        let title_y = self.config.graph_offset_y
            + self.config.graph_height
            + self.config.title_offset_y;

        texts.push(
            DxfText::new(title_x, title_y, &profile.title)
                .height(self.config.title_height)
                .align_h(HorizontalAlignment::Left)
                .align_v(VerticalAlignment::Baseline)
                .layer(&self.config.layer_annotation),
        );

        // Scale information below title
        let scale_text = format!(
            "縮尺 横1:{:.0} 縦1:{:.0}",
            profile.horizontal_scale, profile.vertical_scale
        );
        texts.push(
            DxfText::new(
                title_x,
                title_y - self.config.scale_text_offset_y,
                &scale_text,
            )
            .height(self.config.elevation_label_height)
            .align_h(HorizontalAlignment::Left)
            .align_v(VerticalAlignment::Baseline)
            .layer(&self.config.layer_annotation),
        );
    }
}

impl Default for LongitudinalDrawingGenerator {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn create_test_points() -> Vec<StationPoint> {
        vec![
            StationPoint::new("No.0", 0.0, 100.0, 100.0),
            StationPoint::new("No.1", 20.0, 102.0, 101.0),
            StationPoint::new("No.2", 40.0, 101.5, 102.0),
            StationPoint::new("No.3", 60.0, 103.0, 102.5),
        ]
    }

    #[test]
    fn test_station_point_new() {
        let point = StationPoint::new("No.0", 0.0, 100.0, 99.5);
        assert_eq!(point.name, "No.0");
        assert_eq!(point.distance, 0.0);
        assert_eq!(point.ground_elevation, 100.0);
        assert_eq!(point.planned_elevation, 99.5);
    }

    #[test]
    fn test_longitudinal_profile_new() {
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);

        assert_eq!(profile.title, "縦断図");
        assert_eq!(profile.horizontal_scale, 1000.0);
        assert_eq!(profile.vertical_scale, 100.0);
        assert!(profile.datum_level < 100.0); // Should be below minimum elevation
    }

    #[test]
    fn test_longitudinal_profile_with_settings() {
        let points = create_test_points();
        let profile = LongitudinalProfile::with_settings(
            points,
            "Test Profile",
            500.0,
            50.0,
            90.0,
        );

        assert_eq!(profile.title, "Test Profile");
        assert_eq!(profile.horizontal_scale, 500.0);
        assert_eq!(profile.vertical_scale, 50.0);
        assert_eq!(profile.datum_level, 90.0);
    }

    #[test]
    fn test_slope_annotation_calculation() {
        let points = vec![
            StationPoint::new("No.0", 0.0, 100.0, 100.0),
            StationPoint::new("No.1", 20.0, 102.0, 102.0), // +2m over 20m = +10%
            StationPoint::new("No.2", 40.0, 101.0, 101.0), // -1m over 20m = -5%
        ];
        let profile = LongitudinalProfile::new(points);
        let annotations = profile.calculate_slope_annotations();

        assert_eq!(annotations.len(), 2);

        // First segment: 0% -> +10%
        assert_eq!(annotations[0].point_index, 0);
        assert!((annotations[0].slope_percent - 10.0).abs() < 0.01);
        assert!((annotations[0].delta_v - 0.0).abs() < 0.01); // First segment has no delta
        assert!((annotations[0].segment_length - 20.0).abs() < 0.01);

        // Second segment: +10% -> -5% (delta = -15%)
        assert_eq!(annotations[1].point_index, 1);
        assert!((annotations[1].slope_percent - (-5.0)).abs() < 0.01);
        assert!((annotations[1].delta_v - (-15.0)).abs() < 0.01);
        assert!((annotations[1].segment_length - 20.0).abs() < 0.01);
    }

    #[test]
    fn test_empty_profile() {
        let profile = LongitudinalProfile::new(vec![]);
        let annotations = profile.calculate_slope_annotations();
        assert!(annotations.is_empty());
    }

    #[test]
    fn test_single_point_profile() {
        let points = vec![StationPoint::new("No.0", 0.0, 100.0, 100.0)];
        let profile = LongitudinalProfile::new(points);
        let annotations = profile.calculate_slope_annotations();
        assert!(annotations.is_empty());
    }

    #[test]
    fn test_drawing_generator_creates_valid_dxf() {
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);
        let generator = LongitudinalDrawingGenerator::new();
        let output = generator.generate(&profile);

        // Check DXF structure
        assert!(output.contains("SECTION"));
        assert!(output.contains("HEADER"));
        assert!(output.contains("ENTITIES"));
        assert!(output.contains("EOF"));

        // Check for expected content
        assert!(output.contains("LINE"));
        assert!(output.contains("TEXT"));

        // Check for layer names
        assert!(output.contains("GROUND"));
        assert!(output.contains("PLANNED"));
        assert!(output.contains("ANNOTATION"));
    }

    #[test]
    fn test_drawing_contains_station_labels() {
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);
        let generator = LongitudinalDrawingGenerator::new();
        let output = generator.generate(&profile);

        // Should contain station names
        assert!(output.contains("No.0"));
        assert!(output.contains("No.1"));
        assert!(output.contains("No.2"));
        assert!(output.contains("No.3"));
    }

    #[test]
    fn test_drawing_contains_elevation_labels() {
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);
        let generator = LongitudinalDrawingGenerator::new();
        let output = generator.generate(&profile);

        // Should contain elevation labels (GL and FH)
        assert!(output.contains("GL="));
        assert!(output.contains("FH="));
        assert!(output.contains("DL="));
    }

    #[test]
    fn test_drawing_contains_slope_annotations() {
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);
        let generator = LongitudinalDrawingGenerator::new();
        let output = generator.generate(&profile);

        // Should contain slope annotation indicators
        assert!(output.contains("i="));
        assert!(output.contains("L="));
    }

    #[test]
    fn test_drawing_contains_title_block() {
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);
        let generator = LongitudinalDrawingGenerator::new();
        let output = generator.generate(&profile);

        // Should contain title
        assert!(output.contains("縦断図"));
        // Should contain scale info
        assert!(output.contains("縮尺"));
    }

    #[test]
    fn test_custom_config() {
        let config = DrawingConfig {
            station_label_height: 3.0,
            elevation_label_height: 3.0,
            slope_annotation_height: 2.5,
            title_height: 6.0,
            graph_offset_x: 100.0,
            graph_offset_y: 50.0,
            graph_width: 300.0,
            graph_height: 200.0,
            elevation_padding: 10.0,
            station_label_offset_y: 8.0,
            elevation_label_offset_x: 5.0,
            title_offset_y: 25.0,
            scale_text_offset_y: 10.0,
            tick_mark_half_height: 3.0,
            slope_annotation_offset_y: 10.0,
            slope_annotation_offset_x: 3.0,
            annotation_line_spacing: 4.0,
            layer_ground: String::from("CUSTOM_GROUND"),
            layer_planned: String::from("CUSTOM_PLANNED"),
            layer_annotation: String::from("CUSTOM_ANNOTATION"),
            layer_grid: String::from("CUSTOM_GRID"),
        };

        let generator = LongitudinalDrawingGenerator::with_config(config);
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);
        let output = generator.generate(&profile);

        // Should use custom layer names
        assert!(output.contains("CUSTOM_GROUND"));
        assert!(output.contains("CUSTOM_PLANNED"));
        assert!(output.contains("CUSTOM_ANNOTATION"));
    }

    #[test]
    fn test_datum_level_calculation() {
        let points = vec![
            StationPoint::new("No.0", 0.0, 95.0, 96.0),
            StationPoint::new("No.1", 20.0, 98.0, 97.0),
        ];
        let profile = LongitudinalProfile::new(points);

        // Datum should be below minimum (95.0), rounded to 10m
        assert!(profile.datum_level <= 85.0);
    }

    #[test]
    fn test_non_zero_start_distance() {
        // Test that profiles starting at non-zero chainage are correctly positioned
        let points = vec![
            StationPoint::new("No.50", 1000.0, 100.0, 100.0),
            StationPoint::new("No.51", 1020.0, 102.0, 101.0),
            StationPoint::new("No.52", 1040.0, 101.5, 102.0),
        ];
        let profile = LongitudinalProfile::new(points);
        let generator = LongitudinalDrawingGenerator::new();

        // First point should be at graph_offset_x
        let (x_first, _) = generator.world_to_drawing(&profile, 1000.0, 100.0);
        assert!((x_first - generator.config.graph_offset_x).abs() < 0.001);

        // Last point should be at graph_offset_x + graph_width
        let (x_last, _) = generator.world_to_drawing(&profile, 1040.0, 100.0);
        let expected_x_last = generator.config.graph_offset_x + generator.config.graph_width;
        assert!((x_last - expected_x_last).abs() < 0.001);
    }

    #[test]
    fn test_get_total_distance_with_match() {
        let points = create_test_points();
        let profile = LongitudinalProfile::new(points);
        let generator = LongitudinalDrawingGenerator::new();

        let total = generator.get_total_distance(&profile);
        assert!((total - 60.0).abs() < 0.001);

        // Empty profile should return 0
        let empty_profile = LongitudinalProfile::new(vec![]);
        let empty_total = generator.get_total_distance(&empty_profile);
        assert!((empty_total - 0.0).abs() < 0.001);
    }
}
