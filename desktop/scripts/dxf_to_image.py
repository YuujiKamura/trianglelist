#!/usr/bin/env python3
"""DXF to SVG/PNG converter using ezdxf.

Usage:
    python dxf_to_image.py <input.dxf> <output_path> [--format svg|png|both] [--dpi 300] [--bg white|black]

The output_path should be a file path without extension (extension is added based on format).

Exit codes:
    0: Success
    1: Invalid arguments
    2: File not found
    3: Conversion error
"""

import sys
import os
import argparse
import json


def convert_dxf(input_path: str, output_path: str, fmt: str = "png", dpi: int = 300, bg: str = "white") -> dict:
    """Convert DXF file to SVG and/or PNG."""
    import ezdxf
    from ezdxf.addons.drawing import Frontend, RenderContext

    if not os.path.exists(input_path):
        raise FileNotFoundError(f"DXF file not found: {input_path}")

    doc = ezdxf.readfile(input_path)
    msp = doc.modelspace()

    entities = list(msp)
    entity_count = len(entities)

    result = {
        "input": input_path,
        "entity_count": entity_count,
        "outputs": []
    }

    if fmt in ("svg", "both"):
        svg_path = output_path + ".svg"
        _render_svg(doc, msp, svg_path, bg)
        result["outputs"].append({"format": "svg", "path": svg_path})

    if fmt in ("png", "both"):
        png_path = output_path + ".png"
        width, height = _render_png(doc, msp, png_path, dpi, bg)
        result["outputs"].append({"format": "png", "path": png_path, "width": width, "height": height})

    return result


def _render_svg(doc, msp, output_path: str, bg: str):
    """Render DXF to SVG using ezdxf's native SVG backend."""
    from ezdxf.addons.drawing import Frontend, RenderContext, svg, layout
    import re

    backend = svg.SVGBackend()
    ctx = RenderContext(doc)
    frontend = Frontend(ctx, backend)
    frontend.draw_layout(msp)

    page = layout.Page(0, 0)  # auto-detect size
    svg_string = backend.get_string(page)

    if bg == "white":
        # Add white background and invert white strokes to black
        vb_match = re.search(r'viewBox="([^"]*)"', svg_string)
        if vb_match:
            parts = vb_match.group(1).split()
            if len(parts) == 4:
                vx, vy, vw, vh = parts
                bg_rect = f'<rect x="{vx}" y="{vy}" width="{vw}" height="{vh}" fill="white"/>'
                svg_string = re.sub(r'(</defs>)', r'\1' + bg_rect, svg_string, count=1)

        # Replace white strokes/fills with black for readability on white bg
        svg_string = svg_string.replace("#ffffff", "#000000")
        svg_string = svg_string.replace("stroke: #ffffff", "stroke: #000000")
        svg_string = svg_string.replace("fill: #ffffff", "fill: #000000")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(svg_string)


def _render_png(doc, msp, output_path: str, dpi: int, bg: str) -> tuple:
    """Render DXF to PNG using matplotlib backend."""
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from ezdxf.addons.drawing import Frontend, RenderContext
    from ezdxf.addons.drawing import matplotlib as mpl_backend

    fig = plt.figure()
    ax = fig.add_axes([0, 0, 1, 1])

    ctx = RenderContext(doc)
    out = mpl_backend.MatplotlibBackend(ax)
    frontend = Frontend(ctx, out)
    frontend.draw_layout(msp)

    if bg == "white":
        fig.patch.set_facecolor("white")
        ax.set_facecolor("white")
    else:
        fig.patch.set_facecolor("black")
        ax.set_facecolor("black")

    ax.autoscale()
    ax.set_aspect("equal")
    ax.axis("off")

    fig.savefig(output_path, dpi=dpi, bbox_inches="tight", pad_inches=0.1,
                facecolor=fig.get_facecolor())

    # Get image dimensions
    from PIL import Image
    with Image.open(output_path) as img:
        width, height = img.size

    plt.close(fig)
    return width, height


def main():
    parser = argparse.ArgumentParser(description="Convert DXF to SVG/PNG using ezdxf")
    parser.add_argument("input", help="Input DXF file path")
    parser.add_argument("output", help="Output path (without extension)")
    parser.add_argument("--format", choices=["svg", "png", "both"], default="png",
                       help="Output format (default: png)")
    parser.add_argument("--dpi", type=int, default=300,
                       help="DPI for PNG output (default: 300)")
    parser.add_argument("--bg", choices=["white", "black"], default="white",
                       help="Background color (default: white)")

    args = parser.parse_args()

    try:
        result = convert_dxf(args.input, args.output, args.format, args.dpi, args.bg)
        print(json.dumps(result))
        sys.exit(0)
    except FileNotFoundError as e:
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(2)
    except Exception as e:
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(3)


if __name__ == "__main__":
    main()
