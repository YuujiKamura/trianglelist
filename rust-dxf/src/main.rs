mod dxf;
pub mod longitudinal;

pub use dxf::{DxfLine, DxfText, DxfWriter, HorizontalAlignment, VerticalAlignment};
pub use longitudinal::{
    DrawingConfig, LongitudinalDrawingGenerator, LongitudinalProfile, SlopeAnnotation, StationPoint,
};

fn main() {
    // Example usage - basic DXF
    println!("=== Basic DXF Example ===");
    let lines = vec![
        DxfLine::new(0.0, 0.0, 100.0, 100.0),
        DxfLine::with_style(0.0, 100.0, 100.0, 0.0, 3, "Layer1"),
    ];

    let texts = vec![DxfText::new(50.0, 50.0, "Sample Text")];

    let writer = DxfWriter::new();
    let output = writer.write(&lines, &texts);
    println!("{}", output);

    // Longitudinal profile example
    println!("\n=== Longitudinal Profile Example ===");
    let points = vec![
        StationPoint::new("No.0", 0.0, 100.0, 100.0),
        StationPoint::new("No.1", 20.0, 102.5, 101.0),
        StationPoint::new("No.2", 40.0, 101.0, 102.0),
        StationPoint::new("No.3", 60.0, 103.5, 102.5),
        StationPoint::new("No.4", 80.0, 102.0, 103.0),
    ];

    let profile = LongitudinalProfile::with_settings(
        points,
        "縦断図 - 市道○○線",
        1000.0,
        100.0,
        90.0,
    );

    let generator = LongitudinalDrawingGenerator::new();
    let longitudinal_output = generator.generate(&profile);
    println!("{}", longitudinal_output);
}
