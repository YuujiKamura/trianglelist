//! Geometry calculation functions for triangle coordinate calculations.
//!
//! Provides mathematical functions implementing the law of cosines,
//! interior angle calculations, and related geometry operations.

use std::f64::consts::PI;

/// A 2D point with x and y coordinates.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Point {
    pub x: f64,
    pub y: f64,
}

impl Point {
    /// Creates a new point with the given coordinates.
    pub fn new(x: f64, y: f64) -> Self {
        Self { x, y }
    }

    /// Returns the distance from this point to another point.
    pub fn distance_to(&self, other: &Point) -> f64 {
        let dx = other.x - self.x;
        let dy = other.y - self.y;
        (dx * dx + dy * dy).sqrt()
    }

    /// Returns a new point offset from this point by the given distance and angle (radians).
    pub fn offset(&self, distance: f64, angle_rad: f64) -> Point {
        Point {
            x: self.x + distance * angle_rad.cos(),
            y: self.y + distance * angle_rad.sin(),
        }
    }

    /// Subtracts another point from this point, returning a vector.
    pub fn subtract(&self, other: &Point) -> Point {
        Point {
            x: self.x - other.x,
            y: self.y - other.y,
        }
    }

    /// Returns the magnitude (length) of this point as a vector.
    pub fn magnitude(&self) -> f64 {
        (self.x * self.x + self.y * self.y).sqrt()
    }

    /// Returns the inner (dot) product with another point.
    pub fn inner_product(&self, other: &Point) -> f64 {
        self.x * other.x + self.y * other.y
    }
}

/// Calculates an angle using the law of cosines given three side lengths.
///
/// Given sides a, b, c, calculates the angle opposite to side c.
/// Uses: cos(C) = (a² + b² - c²) / (2ab)
///
/// # Arguments
/// * `a` - Length of side a
/// * `b` - Length of side b
/// * `c` - Length of side c (opposite to the angle being calculated)
///
/// # Returns
/// The angle in radians opposite to side c.
pub fn cosine_rule_angle(a: f64, b: f64, c: f64) -> f64 {
    let cos_c = (a * a + b * b - c * c) / (2.0 * a * b);
    // Clamp to [-1, 1] to handle floating point errors
    cos_c.clamp(-1.0, 1.0).acos()
}

/// Calculates a side length using the law of cosines given two sides and the included angle.
///
/// Uses: c² = a² + b² - 2ab*cos(C)
///
/// # Arguments
/// * `a` - Length of side a
/// * `b` - Length of side b
/// * `angle_rad` - The included angle in radians between sides a and b
///
/// # Returns
/// The length of the side opposite to the given angle.
pub fn cosine_rule_side(a: f64, b: f64, angle_rad: f64) -> f64 {
    (a * a + b * b - 2.0 * a * b * angle_rad.cos()).sqrt()
}

/// Calculates the interior angle at vertex p2 given three points.
///
/// The angle is measured at p2, formed by the vectors p2->p1 and p2->p3.
///
/// # Arguments
/// * `p1` - First point
/// * `p2` - Vertex point (where the angle is measured)
/// * `p3` - Third point
///
/// # Returns
/// The interior angle in degrees.
pub fn internal_angle(p1: &Point, p2: &Point, p3: &Point) -> f64 {
    let v1 = p1.subtract(p2);
    let v2 = p3.subtract(p2);
    let dot = v1.inner_product(&v2);
    let mag_product = v1.magnitude() * v2.magnitude();

    if mag_product == 0.0 {
        return 0.0;
    }

    let cos_angle = (dot / mag_product).clamp(-1.0, 1.0);
    cos_angle.acos() * 180.0 / PI
}

/// Calculates the angle (in radians) from p1 to p2.
///
/// Returns the angle measured counterclockwise from the positive x-axis.
///
/// # Arguments
/// * `p1` - Starting point
/// * `p2` - Ending point
///
/// # Returns
/// The angle in radians in the range [-π, π].
pub fn angle_between_points(p1: &Point, p2: &Point) -> f64 {
    let dx = p2.x - p1.x;
    let dy = p2.y - p1.y;
    dy.atan2(dx)
}

/// Calculates the area of a triangle using Heron's formula.
///
/// Uses: Area = √(s(s-a)(s-b)(s-c)) where s = (a+b+c)/2
///
/// # Arguments
/// * `a` - Length of side a
/// * `b` - Length of side b
/// * `c` - Length of side c
///
/// # Returns
/// The area of the triangle.
pub fn triangle_area(a: f64, b: f64, c: f64) -> f64 {
    let s = (a + b + c) / 2.0;
    let area_sq = s * (s - a) * (s - b) * (s - c);
    if area_sq < 0.0 {
        return 0.0;
    }
    area_sq.sqrt()
}

/// Validates whether three side lengths can form a valid triangle.
///
/// Checks the triangle inequality theorem: the sum of any two sides
/// must be greater than the third side.
///
/// # Arguments
/// * `a` - Length of side a
/// * `b` - Length of side b
/// * `c` - Length of side c
///
/// # Returns
/// `true` if the sides can form a valid triangle, `false` otherwise.
pub fn is_valid_triangle(a: f64, b: f64, c: f64) -> bool {
    a > 0.0 && b > 0.0 && c > 0.0 && (a + b > c) && (b + c > a) && (c + a > b)
}

/// Calculates the centroid (center of mass) of a triangle.
///
/// The centroid is at the average of the three vertices.
///
/// # Arguments
/// * `p1` - First vertex
/// * `p2` - Second vertex
/// * `p3` - Third vertex
///
/// # Returns
/// The centroid point.
pub fn centroid(p1: &Point, p2: &Point, p3: &Point) -> Point {
    Point {
        x: (p1.x + p2.x + p3.x) / 3.0,
        y: (p1.y + p2.y + p3.y) / 3.0,
    }
}

/// Calculates point BC (third vertex) of a triangle given the base points and side lengths.
///
/// Given base point (point_a) and point_ab (second vertex), calculates the third
/// vertex using the law of cosines.
///
/// # Arguments
/// * `point_a` - First vertex (base point)
/// * `point_ab` - Second vertex
/// * `side_a` - Length of side A (from point_a to point_ab)
/// * `side_b` - Length of side B (from point_ab to point_bc)
/// * `side_c` - Length of side C (from point_bc to point_a)
///
/// # Returns
/// The calculated third vertex (point_bc).
pub fn calculate_point_bc(
    point_a: &Point,
    point_ab: &Point,
    side_a: f64,
    side_b: f64,
    side_c: f64,
) -> Point {
    // Calculate theta: angle from point_ab to point_a
    let theta = (point_a.y - point_ab.y).atan2(point_a.x - point_ab.x);

    // Calculate alpha using law of cosines: angle at point_ab
    let pow_a = side_a * side_a;
    let pow_b = side_b * side_b;
    let pow_c = side_c * side_c;
    let alpha = ((pow_a + pow_b - pow_c) / (2.0 * side_a * side_b)).acos();

    // Calculate the angle for point_bc
    let angle = theta + alpha;

    // Calculate offset from point_ab
    let offset_x = side_b * angle.cos();
    let offset_y = side_b * angle.sin();

    Point {
        x: point_ab.x + offset_x,
        y: point_ab.y + offset_y,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const EPSILON: f64 = 1e-9;

    fn approx_eq(a: f64, b: f64) -> bool {
        (a - b).abs() < EPSILON
    }

    #[test]
    fn test_cosine_rule_angle_equilateral() {
        // Equilateral triangle: all angles should be 60 degrees (π/3 radians)
        let angle = cosine_rule_angle(1.0, 1.0, 1.0);
        assert!(approx_eq(angle, PI / 3.0));
    }

    #[test]
    fn test_cosine_rule_angle_right_triangle() {
        // 3-4-5 right triangle: angle opposite to side 5 should be 90 degrees
        let angle = cosine_rule_angle(3.0, 4.0, 5.0);
        assert!(approx_eq(angle, PI / 2.0));
    }

    #[test]
    fn test_cosine_rule_side() {
        // Right triangle with legs 3 and 4, hypotenuse should be 5
        let side = cosine_rule_side(3.0, 4.0, PI / 2.0);
        assert!(approx_eq(side, 5.0));
    }

    #[test]
    fn test_internal_angle() {
        // Right angle at origin
        let p1 = Point::new(1.0, 0.0);
        let p2 = Point::new(0.0, 0.0);
        let p3 = Point::new(0.0, 1.0);
        let angle = internal_angle(&p1, &p2, &p3);
        assert!(approx_eq(angle, 90.0));
    }

    #[test]
    fn test_angle_between_points() {
        let p1 = Point::new(0.0, 0.0);
        let p2 = Point::new(1.0, 1.0);
        let angle = angle_between_points(&p1, &p2);
        assert!(approx_eq(angle, PI / 4.0)); // 45 degrees
    }

    #[test]
    fn test_triangle_area_3_4_5() {
        // 3-4-5 right triangle: area = (3 * 4) / 2 = 6
        let area = triangle_area(3.0, 4.0, 5.0);
        assert!(approx_eq(area, 6.0));
    }

    #[test]
    fn test_triangle_area_equilateral() {
        // Equilateral triangle with side 2: area = (√3/4) * 4 = √3
        let area = triangle_area(2.0, 2.0, 2.0);
        assert!(approx_eq(area, 3.0_f64.sqrt()));
    }

    #[test]
    fn test_is_valid_triangle() {
        assert!(is_valid_triangle(3.0, 4.0, 5.0));
        assert!(is_valid_triangle(1.0, 1.0, 1.0));
        assert!(!is_valid_triangle(1.0, 2.0, 3.0)); // Degenerate
        assert!(!is_valid_triangle(1.0, 1.0, 3.0)); // Invalid
        assert!(!is_valid_triangle(0.0, 1.0, 1.0)); // Zero side
        assert!(!is_valid_triangle(-1.0, 1.0, 1.0)); // Negative side
    }

    #[test]
    fn test_centroid() {
        let p1 = Point::new(0.0, 0.0);
        let p2 = Point::new(3.0, 0.0);
        let p3 = Point::new(0.0, 3.0);
        let c = centroid(&p1, &p2, &p3);
        assert!(approx_eq(c.x, 1.0));
        assert!(approx_eq(c.y, 1.0));
    }

    #[test]
    fn test_calculate_point_bc() {
        // Create a 3-4-5 right triangle
        // When base is at origin and second point is on positive x-axis,
        // the third point is calculated following Kotlin's convention
        let point_a = Point::new(0.0, 0.0);
        let point_ab = Point::new(3.0, 0.0);

        let point_bc = calculate_point_bc(&point_a, &point_ab, 3.0, 4.0, 5.0);

        // Verify the third point forms a valid 3-4-5 triangle
        // point_bc is at (3, -4) following the calculation convention
        assert!(approx_eq(point_bc.x, 3.0));
        assert!(approx_eq(point_bc.y, -4.0));

        // Verify distances form the expected triangle
        let dist_a = point_a.distance_to(&point_ab);
        let dist_b = point_ab.distance_to(&point_bc);
        let dist_c = point_bc.distance_to(&point_a);
        assert!(approx_eq(dist_a, 3.0));
        assert!(approx_eq(dist_b, 4.0));
        assert!(approx_eq(dist_c, 5.0));
    }

    #[test]
    fn test_point_distance() {
        let p1 = Point::new(0.0, 0.0);
        let p2 = Point::new(3.0, 4.0);
        assert!(approx_eq(p1.distance_to(&p2), 5.0));
    }

    #[test]
    fn test_point_offset() {
        let p = Point::new(0.0, 0.0);
        let offset = p.offset(1.0, 0.0);
        assert!(approx_eq(offset.x, 1.0));
        assert!(approx_eq(offset.y, 0.0));
    }
}
