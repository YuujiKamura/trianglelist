//! Triangle structure definition
//!
//! Represents a triangle with three edge lengths and optional connection information.

use super::PointXY;

/// Connection type between triangles
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ConnectionType {
    /// Independent triangle (no connection)
    #[default]
    Independent = -1,
    /// Connect to parent's B edge
    ParentBEdge = 1,
    /// Connect to parent's C edge
    ParentCEdge = 2,
}

impl ConnectionType {
    /// Create from i32 value
    pub fn from_i32(value: i32) -> Self {
        match value {
            1 => ConnectionType::ParentBEdge,
            2 => ConnectionType::ParentCEdge,
            _ => ConnectionType::Independent,
        }
    }
}

/// Triangle with three edge lengths
#[derive(Debug, Clone, PartialEq)]
pub struct Triangle {
    /// Triangle number (1-indexed)
    number: i32,
    /// Length of edge A (connection edge with parent)
    pub length_a: f32,
    /// Length of edge B (free edge)
    pub length_b: f32,
    /// Length of edge C (free edge)
    pub length_c: f32,
    /// Parent triangle number (-1 if independent)
    pub parent_number: i32,
    /// Connection type to parent
    pub connection_type: ConnectionType,
    /// Triangle name (optional)
    pub name: String,
    /// Vertex positions (calculated)
    points: [PointXY; 3],
    /// Base angle (degrees) - rotation angle of the triangle
    angle: f32,
    /// Internal angle at vertex A (degrees)
    angle_ca: f32,
    /// Internal angle at vertex B (degrees)
    angle_ab: f32,
    /// Internal angle at vertex C (degrees)
    angle_bc: f32,
}

impl Default for Triangle {
    fn default() -> Self {
        Self {
            number: 0,
            length_a: 0.0,
            length_b: 0.0,
            length_c: 0.0,
            parent_number: -1,
            connection_type: ConnectionType::Independent,
            name: String::new(),
            points: [PointXY::zero(), PointXY::zero(), PointXY::zero()],
            angle: 180.0,
            angle_ca: 0.0,
            angle_ab: 0.0,
            angle_bc: 0.0,
        }
    }
}

impl Triangle {
    /// Create a new independent triangle with three edge lengths
    pub fn new(length_a: f32, length_b: f32, length_c: f32) -> Self {
        let mut tri = Self {
            number: 0,
            length_a,
            length_b,
            length_c,
            parent_number: -1,
            connection_type: ConnectionType::Independent,
            name: String::new(),
            points: [PointXY::zero(), PointXY::zero(), PointXY::zero()],
            angle: 180.0,
            angle_ca: 0.0,
            angle_ab: 0.0,
            angle_bc: 0.0,
        };
        tri.calculate_points();
        tri.calculate_internal_angles();
        tri
    }

    /// Create a triangle connected to a parent
    pub fn with_parent(
        length_a: f32,
        length_b: f32,
        length_c: f32,
        parent_number: i32,
        connection_type: ConnectionType,
    ) -> Self {
        let mut tri = Self {
            number: 0,
            length_a,
            length_b,
            length_c,
            parent_number,
            connection_type,
            name: String::new(),
            points: [PointXY::zero(), PointXY::zero(), PointXY::zero()],
            angle: 180.0,
            angle_ca: 0.0,
            angle_ab: 0.0,
            angle_bc: 0.0,
        };
        tri.calculate_points();
        tri.calculate_internal_angles();
        tri
    }

    /// Check if the triangle has valid edge lengths (triangle inequality)
    pub fn is_valid(&self) -> bool {
        if self.length_a <= 0.0 || self.length_b <= 0.0 || self.length_c <= 0.0 {
            return false;
        }
        self.length_a + self.length_b > self.length_c
            && self.length_b + self.length_c > self.length_a
            && self.length_c + self.length_a > self.length_b
    }

    /// Get the triangle number
    pub fn number(&self) -> i32 {
        self.number
    }

    /// Set the triangle number
    pub fn set_number(&mut self, number: i32) {
        self.number = number;
    }

    /// Calculate the area using Heron's formula
    pub fn area(&self) -> f32 {
        if !self.is_valid() {
            return 0.0;
        }
        let s = (self.length_a + self.length_b + self.length_c) / 2.0;
        let area_squared =
            s * (s - self.length_a) * (s - self.length_b) * (s - self.length_c);
        if area_squared < 0.0 {
            0.0
        } else {
            area_squared.sqrt()
        }
    }

    /// Get the vertex points
    pub fn points(&self) -> &[PointXY; 3] {
        &self.points
    }

    /// Calculate vertex positions based on edge lengths
    /// Point A is at origin, Point B is on the positive X axis
    fn calculate_points(&mut self) {
        if !self.is_valid() {
            return;
        }

        // Point A at origin
        self.points[0] = PointXY::zero();

        // Point B on X axis at distance length_c from A
        self.points[1] = PointXY::new(self.length_c, 0.0);

        // Point C calculated using cosine rule
        // cos(A) = (b² + c² - a²) / (2bc)
        let cos_a = (self.length_b * self.length_b + self.length_c * self.length_c
            - self.length_a * self.length_a)
            / (2.0 * self.length_b * self.length_c);
        let sin_a = (1.0 - cos_a * cos_a).sqrt();

        self.points[2] = PointXY::new(self.length_b * cos_a, self.length_b * sin_a);
    }

    /// Calculate internal angles using the law of cosines
    /// 
    /// Calculates the three internal angles of the triangle:
    /// - angle_ca: angle at vertex A (opposite to side A)
    /// - angle_ab: angle at vertex B (opposite to side B)
    /// - angle_bc: angle at vertex C (opposite to side C)
    fn calculate_internal_angles(&mut self) {
        if !self.is_valid() {
            return;
        }

        use std::f32::consts::PI;

        // Angle at vertex A (opposite to side A)
        // cos(A) = (b² + c² - a²) / (2bc)
        let cos_a = (self.length_b * self.length_b + self.length_c * self.length_c
            - self.length_a * self.length_a)
            / (2.0 * self.length_b * self.length_c);
        self.angle_ca = cos_a.clamp(-1.0, 1.0).acos() * 180.0 / PI;

        // Angle at vertex B (opposite to side B)
        // cos(B) = (a² + c² - b²) / (2ac)
        let cos_b = (self.length_a * self.length_a + self.length_c * self.length_c
            - self.length_b * self.length_b)
            / (2.0 * self.length_a * self.length_c);
        self.angle_ab = cos_b.clamp(-1.0, 1.0).acos() * 180.0 / PI;

        // Angle at vertex C (opposite to side C)
        // cos(C) = (a² + b² - c²) / (2ab)
        let cos_c = (self.length_a * self.length_a + self.length_b * self.length_b
            - self.length_c * self.length_c)
            / (2.0 * self.length_a * self.length_b);
        self.angle_bc = cos_c.clamp(-1.0, 1.0).acos() * 180.0 / PI;
    }

    /// Get the base angle (rotation angle) in degrees
    pub fn angle(&self) -> f32 {
        self.angle
    }

    /// Set the base angle (rotation angle) in degrees
    pub fn set_angle(&mut self, angle: f32) {
        self.angle = angle;
    }

    /// Get the internal angle at vertex A (degrees)
    pub fn angle_ca(&self) -> f32 {
        self.angle_ca
    }

    /// Get the internal angle at vertex B (degrees)
    pub fn angle_ab(&self) -> f32 {
        self.angle_ab
    }

    /// Get the internal angle at vertex C (degrees)
    pub fn angle_bc(&self) -> f32 {
        self.angle_bc
    }

    /// Check if this triangle is independent (not connected to any parent)
    pub fn is_independent(&self) -> bool {
        self.parent_number < 1 || self.connection_type == ConnectionType::Independent
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const EPSILON: f32 = 0.001;

    fn approx_eq(a: f32, b: f32) -> bool {
        (a - b).abs() < EPSILON
    }

    #[test]
    fn test_new_triangle() {
        let tri = Triangle::new(3.0, 4.0, 5.0);
        assert!(approx_eq(tri.length_a, 3.0));
        assert!(approx_eq(tri.length_b, 4.0));
        assert!(approx_eq(tri.length_c, 5.0));
        assert!(tri.is_valid());
    }

    #[test]
    fn test_invalid_triangle() {
        let tri = Triangle::new(1.0, 2.0, 10.0);
        assert!(!tri.is_valid());
    }

    #[test]
    fn test_zero_edge() {
        let tri = Triangle::new(0.0, 4.0, 5.0);
        assert!(!tri.is_valid());
    }

    #[test]
    fn test_area_345() {
        let tri = Triangle::new(3.0, 4.0, 5.0);
        // 3-4-5 right triangle area = (3 * 4) / 2 = 6
        assert!(approx_eq(tri.area(), 6.0));
    }

    #[test]
    fn test_area_equilateral() {
        let tri = Triangle::new(2.0, 2.0, 2.0);
        // Equilateral triangle with side 2: area = (√3 / 4) * 2² = √3
        let expected = 3.0_f32.sqrt();
        assert!(approx_eq(tri.area(), expected));
    }

    #[test]
    fn test_area_invalid() {
        let tri = Triangle::new(1.0, 2.0, 10.0);
        assert!(approx_eq(tri.area(), 0.0));
    }

    #[test]
    fn test_with_parent() {
        let tri = Triangle::with_parent(5.0, 4.0, 3.0, 1, ConnectionType::ParentBEdge);
        assert_eq!(tri.parent_number, 1);
        assert_eq!(tri.connection_type, ConnectionType::ParentBEdge);
        assert!(!tri.is_independent());
    }

    #[test]
    fn test_independent() {
        let tri = Triangle::new(3.0, 4.0, 5.0);
        assert!(tri.is_independent());
    }

    #[test]
    fn test_set_number() {
        let mut tri = Triangle::new(3.0, 4.0, 5.0);
        tri.set_number(1);
        assert_eq!(tri.number(), 1);
    }

    #[test]
    fn test_connection_type_from_i32() {
        assert_eq!(ConnectionType::from_i32(-1), ConnectionType::Independent);
        assert_eq!(ConnectionType::from_i32(1), ConnectionType::ParentBEdge);
        assert_eq!(ConnectionType::from_i32(2), ConnectionType::ParentCEdge);
        assert_eq!(ConnectionType::from_i32(99), ConnectionType::Independent);
    }

    #[test]
    fn test_points_calculation() {
        let tri = Triangle::new(3.0, 4.0, 5.0);
        let points = tri.points();
        // Point A at origin
        assert!(approx_eq(points[0].x, 0.0));
        assert!(approx_eq(points[0].y, 0.0));
        // Point B on X axis at distance 5 (length_c)
        assert!(approx_eq(points[1].x, 5.0));
        assert!(approx_eq(points[1].y, 0.0));
    }

    #[test]
    fn test_internal_angles_345() {
        // 3-4-5 right triangle
        // Angle opposite to side 3 (A) should be ~36.87°
        // Angle opposite to side 4 (B) should be ~53.13°
        // Angle opposite to side 5 (C) should be 90°
        let tri = Triangle::new(3.0, 4.0, 5.0);
        assert!(approx_eq(tri.angle_bc(), 90.0)); // Angle at C (opposite to side 5)
        assert!(approx_eq(tri.angle_ca(), 36.87)); // Angle at A (opposite to side 3)
        assert!(approx_eq(tri.angle_ab(), 53.13)); // Angle at B (opposite to side 4)
        
        // Sum of all angles should be 180°
        assert!(approx_eq(tri.angle_ca() + tri.angle_ab() + tri.angle_bc(), 180.0));
    }

    #[test]
    fn test_internal_angles_equilateral() {
        // Equilateral triangle: all angles should be 60°
        let tri = Triangle::new(2.0, 2.0, 2.0);
        assert!(approx_eq(tri.angle_ca(), 60.0));
        assert!(approx_eq(tri.angle_ab(), 60.0));
        assert!(approx_eq(tri.angle_bc(), 60.0));
    }

    #[test]
    fn test_angle_getter_setter() {
        let mut tri = Triangle::new(3.0, 4.0, 5.0);
        assert!(approx_eq(tri.angle(), 180.0)); // Default angle
        tri.set_angle(90.0);
        assert!(approx_eq(tri.angle(), 90.0));
    }
}
