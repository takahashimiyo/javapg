import java.util.ArrayList; // Todoを複数入れるリストを作るための機能を読み込みます
import java.util.List; // Todoを並べて扱うリストの型を読み込みます

class Todo { // Todo（やること1件）の情報をまとめるクラスです
    String title; // やることのタイトルを保存します
    boolean done; // 済んだかどうかを保存します（trueは済み、falseは未完了です）

    Todo(String title, boolean done) { // Todoを作るときにタイトルと済みかどうかを受け取ります
        this.title = title; // 受け取ったタイトルをこのTodoに保存します
        this.done = done; // 受け取った状態をこのTodoに保存します
    } // Todoを作る処理はここまでです

    String toItem() { // TodoをHTMLのliタグで囲んだ1行に変換します
        if (done) { // 済んでいるかを確認します
            return "<li>[完了] " + title + "</li>"; // 済んでいればタイトルの前に[済]を付けて返します
        } // 済みの場合の処理はここまでです
        return "<li>" + title + "</li>"; // 未完了ならタイトルをそのままliタグで囲んで返します
    } // 1行に変換する処理はここまでです
} // Todoクラスはここまでです

public class Main { // プログラムの開始地点を持つクラスです
    public static void main(String[] args) { // Javaが最初に実行する処理です
        List<Todo> todos = new ArrayList<>(); // Todoを入れる空のリスト（並べて保存する入れ物）を作ります
        todos.add(new Todo("牛乳を買う", true)); // 未完了のTodoを1件追加します
        todos.add(new Todo("ゴミを出す", true)); // 済んだTodoを1件追加します
        todos.add(new Todo("エージェント登録をする", false));

        for (Todo todo : todos) { // リストからTodoを1件ずつ取り出して繰り返します
            System.out.println(todo.toItem()); // 変換した1行をターミナル（文字を表示する画面）に出します
        } // 繰り返しはここまでです
    } // プログラムの開始処理はここまでです
} // Mainクラスはここまでです