use trianglelist_core::{ConnParam, Alignment};

fn main() {
    // B辺接続の例
    let b_conn = ConnParam::b_edge(5.0);
    println!("B辺接続: side={}, len_a={}", b_conn.side, b_conn.len_a);

    // C辺接続の例
    let c_conn = ConnParam::c_edge(4.0);
    println!("C辺接続: side={}, len_a={}", c_conn.side, c_conn.len_a);

    // 独立（非接続）の例
    let independent = ConnParam::independent();
    println!("独立: is_connected={}", independent.is_connected());

    // 配置定数の確認
    println!("配置: LEFT={}, CENTER={}, RIGHT={}",
             Alignment::LEFT, Alignment::CENTER, Alignment::RIGHT);
}
