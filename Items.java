public class Items { // Itemsという名前のクラス（プログラムのまとまり）を作ります。
    public static void main(String[] args) { // Javaが最初に実行する場所です。
        String[] todos = { // todosという配列（複数の値を並べて入れるもの）を作ります。
                "牛乳を買う", // 1件目のTodoです。
                "", // 空の項目も配列に入れて動作を確認します。
                "パンを買う", // 3件目のTodoです。
                "掃除をする" // 4件目のTodoです。
        }; // 配列の内容はここまでです。
        boolean[] done = { true, false, false, false }; // Todoが済んだかを表す配列（trueは済み、falseは未完了）です。

        for (int i = 0; i < todos.length; i++) { // iを0から始め、配列の件数分くり返します。
            if (!todos[i].isEmpty()) { // i番目の文字列が空でないときだけ、次の行を実行します。
                System.out.println("<li>" + (done[i] ? "[済] " : "") + todos[i] + "</li>"); // 済みなら[済]を付けて<li>と</li>で表示します。
            } // 空でないときの処理はここまでです。
        } // くり返しはここまでです。
    } // mainメソッド（実行する処理）はここまでです。
} // Itemsクラスはここまでです。
