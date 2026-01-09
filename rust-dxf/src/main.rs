mod dxf;

pub use dxf::{DxfLine, DxfText, DxfWriter};

fn main() {
    // Example usage
    let lines = vec![
        DxfLine::new(0.0, 0.0, 100.0, 100.0),
        DxfLine::with_style(0.0, 100.0, 100.0, 0.0, 3, "Layer1"),
    ];

    let texts = vec![DxfText::new(50.0, 50.0, "Sample Text")];

    let writer = DxfWriter::new();
    let output = writer.write(&lines, &texts);

    println!("{}", output);
}
