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
    angle_a: f32,
    /// Internal angle at vertex B (degrees)
    angle_b: f32,
    /// Internal angle at vertex C (degrees)
    angle_c: f32,
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
            angle_a: 0.0,
            angle_b: 0.0,
            angle_c: 0.0,
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
            angle_a: 0.0,
            angle_b: 0.0,
            angle_c: 0.0,
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
            angle_a: 0.0,
            angle_b: 0.0,
            angle_c: 0.0,
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
    /// - angle_a: angle at vertex A (opposite to side A)
    /// - angle_b: angle at vertex B (opposite to side B)
    /// - angle_c: angle at vertex C (opposite to side C)
    fn calculate_internal_angles(&mut self) {
        if !self.is_valid() {
            return;
        }

        let a2 = self.length_a.powi(2);
        let b2 = self.length_b.powi(2);
        let c2 = self.length_c.powi(2);

        // Angle at vertex A (opposite to side A)
        // cos(A) = (b² + c² - a²) / (2bc)
        let cos_a = (b2 + c2 - a2) / (2.0 * self.length_b * self.length_c);
        self.angle_a = cos_a.clamp(-1.0, 1.0).acos().to_degrees();

        // Angle at vertex B (opposite to side B)
        // cos(B) = (a² + c² - b²) / (2ac)
        let cos_b = (a2 + c2 - b2) / (2.0 * self.length_a * self.length_c);
        self.angle_b = cos_b.clamp(-1.0, 1.0).acos().to_degrees();

        // Angle at vertex C (opposite to side C)
        // cos(C) = (a² + b² - c²) / (2ab)
        let cos_c = (a2 + b2 - c2) / (2.0 * self.length_a * self.length_b);
        self.angle_c = cos_c.clamp(-1.0, 1.0).acos().to_degrees();
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
    pub fn angle_a(&self) -> f32 {
        self.angle_a
    }

    /// Get the internal angle at vertex B (degrees)
    pub fn angle_b(&self) -> f32 {
        self.angle_b
    }

    /// Get the internal angle at vertex C (degrees)
    pub fn angle_c(&self) -> f32 {
        self.angle_c
    }

    /// Check if this triangle is independent (not connected to any parent)
    pub fn is_independent(&self) -> bool {
        self.parent_number < 1 || self.connection_type == ConnectionType::Independent
    }

    /// Calculate points for a connected triangle based on parent triangle
    /// 
    /// This method calculates the coordinates of a triangle that is connected to a parent triangle.
    /// The connection side determines which edge of the parent is used for connection.
    /// 
    /// # Arguments
    /// * `parent` - The parent triangle to connect to
    /// * `connection_side` - The side of the parent to connect to (1 = B edge, 2 = C edge)
    /// 
    /// # Returns
    /// `Ok(())` if successful, or an error if the connection is invalid
    pub fn calculate_points_connected(&mut self, parent: &Triangle, connection_side: i32) -> Result<(), String> {
        
        if !parent.is_valid() {
            return Err("Parent triangle is invalid".to_string());
        }
        if !self.is_valid() {
            return Err("Triangle is invalid".to_string());
        }

        let parent_points = parent.points();
        let parent_angle = parent.angle();

        // Determine which points and lengths to use based on connection side
        let (base_point, direction_point, length_base, length_direction, length_third) = match connection_side {
            1 => {
                // Connect to parent's B edge
                // Parent: point[0] (CA) -> point[1] (AB) is B edge
                // Child: point[0] (CA) is at parent's point[1] (AB), use B length
                let base = parent_points[1]; // Parent's AB (end of B edge)
                let direction = parent_points[2]; // Parent's BC (for direction)
                
                // For connection to B edge, child's A edge = parent's B length
                // Child's point layout: point[0] = base, point[1] = direction, point[2] = calculated
                (
                    base,
                    direction,
                    self.length_b, // Child's B length (will be used as base)
                    self.length_c, // Child's C length (will be used as direction)
                    self.length_a, // Child's A length (connection edge, already set from parent)
                )
            }
            2 => {
                // Connect to parent's C edge
                // Parent: point[0] (CA) -> point[2] (BC) is C edge
                // Child: point[0] (CA) is at parent's point[0] (CA), use C length
                let base = parent_points[0]; // Parent's CA (start of C edge)
                let direction = parent_points[2]; // Parent's BC (end of C edge, for direction)
                
                // For connection to C edge, child's A edge = parent's C length
                (
                    base,
                    direction,
                    self.length_c, // Child's C length (will be used as base)
                    self.length_a, // Child's A length (will be used as direction)
                    self.length_b, // Child's B length (connection edge, already set from parent)
                )
            }
            0 => {
                // Independent triangle - use standard calculation
                self.calculate_points();
                return Ok(());
            }
            _ => {
                return Err(format!("Invalid connection side: {}", connection_side));
            }
        };

        // Calculate the angle from base point to direction point
        let dx = (direction_point.x - base_point.x) as f64;
        let dy = (direction_point.y - base_point.y) as f64;
        let theta = dy.atan2(dx);

        // Calculate alpha using cosine rule: cos(alpha) = (b² + c² - a²) / (2bc)
        let a2 = (length_third * length_third) as f64;
        let b2 = (length_base * length_base) as f64;
        let c2 = (length_direction * length_direction) as f64;
        let alpha = ((b2 + c2 - a2) / (2.0 * length_base as f64 * length_direction as f64)).acos();

        // Calculate the angle for the third point
        let angle_rad = theta + alpha;

        // Set points based on connection side
        match connection_side {
            1 => {
                // Connect to parent's B edge
                self.points[0] = base_point;
                self.points[1] = direction_point;
                // Calculate third point using cosine rule
                self.points[2] = PointXY::new(
                    base_point.x + (length_base as f64 * angle_rad.cos()) as f32,
                    base_point.y + (length_base as f64 * angle_rad.sin()) as f32,
                );
                
                // Update angle: parent angle - angle_ca
                self.angle = parent_angle - self.angle_a;
                if self.angle < 0.0 {
                    self.angle += 360.0;
                }
                if self.angle >= 360.0 {
                    self.angle -= 360.0;
                }
            }
            2 => {
                // Connect to parent's C edge
                self.points[0] = base_point;
                // Calculate second point
                self.points[1] = PointXY::new(
                    base_point.x + (length_base as f64 * angle_rad.cos()) as f32,
                    base_point.y + (length_base as f64 * angle_rad.sin()) as f32,
                );
                self.points[2] = direction_point;
                
                // Update angle: parent angle + angle_ca
                self.angle = parent_angle + self.angle_a;
                if self.angle >= 360.0 {
                    self.angle -= 360.0;
                }
                if self.angle < 0.0 {
                    self.angle += 360.0;
                }
            }
            _ => unreachable!(),
        }

        // Recalculate internal angles after setting points
        self.calculate_internal_angles();
        
        Ok(())
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
        assert!(approx_eq(tri.angle_c(), 90.0)); // Angle at C (opposite to side 5)
        assert!(approx_eq(tri.angle_a(), 36.87)); // Angle at A (opposite to side 3)
        assert!(approx_eq(tri.angle_b(), 53.13)); // Angle at B (opposite to side 4)
        
        // Sum of all angles should be 180°
        assert!(approx_eq(tri.angle_a() + tri.angle_b() + tri.angle_c(), 180.0));
    }

    #[test]
    fn test_internal_angles_equilateral() {
        // Equilateral triangle: all angles should be 60°
        let tri = Triangle::new(2.0, 2.0, 2.0);
        assert!(approx_eq(tri.angle_a(), 60.0));
        assert!(approx_eq(tri.angle_b(), 60.0));
        assert!(approx_eq(tri.angle_c(), 60.0));
    }

    #[test]
    fn test_angle_getter_setter() {
        let mut tri = Triangle::new(3.0, 4.0, 5.0);
        assert!(approx_eq(tri.angle(), 180.0)); // Default angle
        tri.set_angle(90.0);
        assert!(approx_eq(tri.angle(), 90.0));
    }

    #[test]
    fn test_calculate_points_connected_b_edge() {
        // Create a parent triangle: 3-4-5 right triangle
        let parent = Triangle::new(3.0, 4.0, 5.0);
        let parent_points = parent.points();
        
        // Create a child triangle connected to parent's B edge
        // Child's A edge should match parent's B edge (length 4)
        let mut child = Triangle::new(4.0, 3.0, 5.0); // A=4 (parent's B), B=3, C=5
        
        // Calculate connected points
        let result = child.calculate_points_connected(&parent, 1);
        assert!(result.is_ok());
        
        let child_points = child.points();
        
        // Child's point[0] should be at parent's point[1] (AB)
        assert!(approx_eq(child_points[0].x, parent_points[1].x));
        assert!(approx_eq(child_points[0].y, parent_points[1].y));
    }

    #[test]
    fn test_calculate_points_connected_c_edge() {
        // Create a parent triangle: 3-4-5 right triangle
        let parent = Triangle::new(3.0, 4.0, 5.0);
        let parent_points = parent.points();
        
        // Create a child triangle connected to parent's C edge
        // Child's A edge should match parent's C edge (length 5)
        let mut child = Triangle::new(5.0, 3.0, 4.0); // A=5 (parent's C), B=3, C=4
        
        // Calculate connected points
        let result = child.calculate_points_connected(&parent, 2);
        assert!(result.is_ok());
        
        let child_points = child.points();
        
        // Child's point[0] should be at parent's point[0] (CA)
        assert!(approx_eq(child_points[0].x, parent_points[0].x));
        assert!(approx_eq(child_points[0].y, parent_points[0].y));
    }

    #[test]
    fn test_calculate_points_connected_independent() {
        // Independent triangle (connection_side = 0) should use standard calculation
        let parent = Triangle::new(3.0, 4.0, 5.0);
        let mut child = Triangle::new(3.0, 4.0, 5.0);
        
        let result = child.calculate_points_connected(&parent, 0);
        assert!(result.is_ok());
        
        // Points should be recalculated (may be same or different based on implementation)
        let new_points = child.points();
        // At minimum, they should be valid
        assert!(child.is_valid());
        // Points array should have 3 points
        assert_eq!(new_points.len(), 3);
    }

    #[test]
    fn test_calculate_points_connected_invalid_parent() {
        let invalid_parent = Triangle::new(1.0, 2.0, 10.0); // Invalid triangle
        let mut child = Triangle::new(3.0, 4.0, 5.0);
        
        let result = child.calculate_points_connected(&invalid_parent, 1);
        assert!(result.is_err());
        assert!(result.unwrap_err().contains("Parent triangle is invalid"));
    }

    #[test]
    fn test_calculate_points_connected_invalid_child() {
        let parent = Triangle::new(3.0, 4.0, 5.0);
        let invalid_child = Triangle::new(1.0, 2.0, 10.0); // Invalid triangle
        let mut child = invalid_child;
        
        let result = child.calculate_points_connected(&parent, 1);
        assert!(result.is_err());
        assert!(result.unwrap_err().contains("Triangle is invalid"));
    }

    #[test]
    fn test_calculate_points_connected_invalid_side() {
        let parent = Triangle::new(3.0, 4.0, 5.0);
        let mut child = Triangle::new(3.0, 4.0, 5.0);
        
        let result = child.calculate_points_connected(&parent, 99);
        assert!(result.is_err());
        assert!(result.unwrap_err().contains("Invalid connection side"));
    }
}
