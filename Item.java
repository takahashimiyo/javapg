public class Item { // Itemという名前のクラス（プログラムのまとまり）を作ります
    public static void main(String[] args) { // プログラムを実行したとき最初に動く場所です
        String title = "Openworkに登録する"; // String（文字列）型の変数titleに文字を入れます
        String html = "<li>" + title + "</li>"; // +で文字列をつなぎ、HTMLの1行を作ります
        boolean done = false;
        int count = 3;
        System.out.println(html); // 作った文字列をターミナルに1行表示します
        System.out.println(done);
        System.out.println("いま" + count + "件");
    } // mainのまとまりを閉じます
} // Itemのまとまりを閉じます
