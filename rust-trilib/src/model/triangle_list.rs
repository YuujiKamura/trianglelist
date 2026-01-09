//! TriangleList management structure
//!
//! Manages a collection of triangles with automatic numbering and various operations.

use std::ops::Index;

use super::Triangle;

/// A collection of triangles with automatic numbering
#[derive(Debug, Clone, Default)]
pub struct TriangleList {
    /// Internal storage of triangles
    triangles: Vec<Triangle>,
}

impl TriangleList {
    /// Create a new empty TriangleList
    pub fn new() -> Self {
        Self {
            triangles: Vec::new(),
        }
    }

    /// Create a TriangleList with an initial triangle
    pub fn with_first(triangle: Triangle) -> Self {
        let mut list = Self::new();
        list.add(triangle);
        list
    }

    /// Add a triangle to the list with automatic numbering
    ///
    /// Returns the assigned number (1-indexed)
    pub fn add(&mut self, mut triangle: Triangle) -> i32 {
        let number = (self.triangles.len() + 1) as i32;
        triangle.set_number(number);
        self.triangles.push(triangle);
        number
    }

    /// Get a triangle by number (1-indexed)
    ///
    /// Returns None if the number is out of range
    pub fn get(&self, number: i32) -> Option<&Triangle> {
        if number < 1 {
            return None;
        }
        self.triangles.get((number - 1) as usize)
    }

    /// Get a mutable reference to a triangle by number (1-indexed)
    ///
    /// Returns None if the number is out of range
    pub fn get_mut(&mut self, number: i32) -> Option<&mut Triangle> {
        if number < 1 {
            return None;
        }
        self.triangles.get_mut((number - 1) as usize)
    }

    /// Get the number of triangles in the list
    pub fn size(&self) -> usize {
        self.triangles.len()
    }

    /// Check if the list is empty
    pub fn is_empty(&self) -> bool {
        self.triangles.is_empty()
    }

    /// Clear all triangles from the list
    pub fn clear(&mut self) {
        self.triangles.clear();
    }

    /// Remove a triangle by number (1-indexed)
    ///
    /// Returns the removed triangle if successful, None otherwise.
    /// Note: This operation renumbers all subsequent triangles.
    pub fn remove(&mut self, number: i32) -> Option<Triangle> {
        if number < 1 || number as usize > self.triangles.len() {
            return None;
        }

        let removed = self.triangles.remove((number - 1) as usize);

        // Renumber all triangles after the removed one
        for (i, tri) in self.triangles.iter_mut().enumerate() {
            tri.set_number((i + 1) as i32);
        }

        Some(removed)
    }

    /// Calculate the total area of all triangles
    pub fn total_area(&self) -> f32 {
        let area: f32 = self.triangles.iter().map(|t| t.area()).sum();
        // Round to 2 decimal places like the Kotlin version
        (area * 100.0).round() / 100.0
    }

    /// Get an iterator over the triangles
    pub fn iter(&self) -> impl Iterator<Item = &Triangle> {
        self.triangles.iter()
    }

    /// Get a mutable iterator over the triangles
    pub fn iter_mut(&mut self) -> impl Iterator<Item = &mut Triangle> {
        self.triangles.iter_mut()
    }

    /// Get the last triangle in the list
    pub fn last(&self) -> Option<&Triangle> {
        self.triangles.last()
    }

    /// Get the first triangle in the list
    pub fn first(&self) -> Option<&Triangle> {
        self.triangles.first()
    }
}

/// Index trait implementation for Vec-like access (1-indexed)
///
/// # Panics
/// Panics if the index is out of bounds (< 1 or > size)
impl Index<i32> for TriangleList {
    type Output = Triangle;

    fn index(&self, number: i32) -> &Self::Output {
        self.get(number)
            .unwrap_or_else(|| panic!("Triangle number {} is out of bounds", number))
    }
}

/// Index trait for usize (0-indexed, like Vec)
impl Index<usize> for TriangleList {
    type Output = Triangle;

    fn index(&self, index: usize) -> &Self::Output {
        &self.triangles[index]
    }
}

/// IntoIterator implementation for TriangleList
impl IntoIterator for TriangleList {
    type Item = Triangle;
    type IntoIter = std::vec::IntoIter<Triangle>;

    fn into_iter(self) -> Self::IntoIter {
        self.triangles.into_iter()
    }
}

/// IntoIterator implementation for &TriangleList
impl<'a> IntoIterator for &'a TriangleList {
    type Item = &'a Triangle;
    type IntoIter = std::slice::Iter<'a, Triangle>;

    fn into_iter(self) -> Self::IntoIter {
        self.triangles.iter()
    }
}

/// IntoIterator implementation for &mut TriangleList
impl<'a> IntoIterator for &'a mut TriangleList {
    type Item = &'a mut Triangle;
    type IntoIter = std::slice::IterMut<'a, Triangle>;

    fn into_iter(self) -> Self::IntoIter {
        self.triangles.iter_mut()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const EPSILON: f32 = 0.01;

    fn approx_eq(a: f32, b: f32) -> bool {
        (a - b).abs() < EPSILON
    }

    #[test]
    fn test_new_empty() {
        let list = TriangleList::new();
        assert!(list.is_empty());
        assert_eq!(list.size(), 0);
    }

    #[test]
    fn test_add_and_get() {
        let mut list = TriangleList::new();
        let number = list.add(Triangle::new(3.0, 4.0, 5.0));

        assert_eq!(number, 1);
        assert_eq!(list.size(), 1);

        let tri = list.get(1).unwrap();
        assert_eq!(tri.number(), 1);
        assert!(approx_eq(tri.length_a, 3.0));
    }

    #[test]
    fn test_add_multiple() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));
        list.add(Triangle::new(6.0, 7.0, 8.0));

        assert_eq!(list.size(), 3);
        assert_eq!(list.get(1).unwrap().number(), 1);
        assert_eq!(list.get(2).unwrap().number(), 2);
        assert_eq!(list.get(3).unwrap().number(), 3);
    }

    #[test]
    fn test_get_out_of_range() {
        let list = TriangleList::new();
        assert!(list.get(0).is_none());
        assert!(list.get(1).is_none());
        assert!(list.get(-1).is_none());
    }

    #[test]
    fn test_clear() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        assert_eq!(list.size(), 2);
        list.clear();
        assert!(list.is_empty());
        assert_eq!(list.size(), 0);
    }

    #[test]
    fn test_remove() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));
        list.add(Triangle::new(6.0, 7.0, 8.0));

        let removed = list.remove(2);
        assert!(removed.is_some());
        assert_eq!(list.size(), 2);

        // Check renumbering
        assert_eq!(list.get(1).unwrap().number(), 1);
        assert_eq!(list.get(2).unwrap().number(), 2);
        // The triangle that was #3 is now #2
        assert!(approx_eq(list.get(2).unwrap().length_a, 6.0));
    }

    #[test]
    fn test_remove_invalid() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));

        assert!(list.remove(0).is_none());
        assert!(list.remove(2).is_none());
        assert!(list.remove(-1).is_none());
        assert_eq!(list.size(), 1);
    }

    #[test]
    fn test_total_area() {
        let mut list = TriangleList::new();
        // 3-4-5 triangle area = 6.0
        list.add(Triangle::new(3.0, 4.0, 5.0));
        // Equilateral triangle side 2, area = √3 ≈ 1.732
        list.add(Triangle::new(2.0, 2.0, 2.0));

        let total = list.total_area();
        let expected = 6.0 + 3.0_f32.sqrt();
        assert!(approx_eq(total, expected));
    }

    #[test]
    fn test_total_area_empty() {
        let list = TriangleList::new();
        assert!(approx_eq(list.total_area(), 0.0));
    }

    #[test]
    fn test_index_i32() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        assert_eq!(list[1_i32].number(), 1);
        assert_eq!(list[2_i32].number(), 2);
    }

    #[test]
    fn test_index_usize() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        assert_eq!(list[0_usize].number(), 1);
        assert_eq!(list[1_usize].number(), 2);
    }

    #[test]
    #[should_panic(expected = "out of bounds")]
    fn test_index_panic() {
        let list = TriangleList::new();
        let _ = list[1_i32];
    }

    #[test]
    fn test_iterator() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        let numbers: Vec<i32> = list.iter().map(|t| t.number()).collect();
        assert_eq!(numbers, vec![1, 2]);
    }

    #[test]
    fn test_for_loop() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        let mut count = 0;
        for tri in &list {
            count += 1;
            assert_eq!(tri.number(), count);
        }
        assert_eq!(count, 2);
    }

    #[test]
    fn test_into_iterator() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        let triangles: Vec<Triangle> = list.into_iter().collect();
        assert_eq!(triangles.len(), 2);
    }

    #[test]
    fn test_first_and_last() {
        let mut list = TriangleList::new();
        assert!(list.first().is_none());
        assert!(list.last().is_none());

        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        assert_eq!(list.first().unwrap().number(), 1);
        assert_eq!(list.last().unwrap().number(), 2);
    }

    #[test]
    fn test_with_first() {
        let list = TriangleList::with_first(Triangle::new(3.0, 4.0, 5.0));
        assert_eq!(list.size(), 1);
        assert_eq!(list.get(1).unwrap().number(), 1);
    }

    #[test]
    fn test_get_mut() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));

        if let Some(tri) = list.get_mut(1) {
            tri.name = "Modified".to_string();
        }

        assert_eq!(list.get(1).unwrap().name, "Modified");
    }

    #[test]
    fn test_iter_mut() {
        let mut list = TriangleList::new();
        list.add(Triangle::new(3.0, 4.0, 5.0));
        list.add(Triangle::new(5.0, 5.0, 5.0));

        for tri in list.iter_mut() {
            tri.name = format!("T{}", tri.number());
        }

        assert_eq!(list.get(1).unwrap().name, "T1");
        assert_eq!(list.get(2).unwrap().name, "T2");
    }
}
