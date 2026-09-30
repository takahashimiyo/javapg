import com.sun.net.httpserver.HttpServer; // Webサーバーを使うための部品を読み込みます。
import java.net.InetSocketAddress; // ポート番号を指定するための部品を読み込みます。
import java.net.URLDecoder; // URLの記号表記を元の文字に戻す部品を読み込みます。
import java.util.ArrayList; // リストを作る部品を読み込みます。
import java.util.List; // リストを使う部品を読み込みます。

public class App { // Appという名前のプログラムです。
    public static void main(String[] args) throws Exception { // プログラムの開始位置です。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。【1】
        server.createContext("/", exchange -> { // トップページに来たときの処理をここに書きます。【1】
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の返事をUTF-8の文字として設定します。
            if (path.equals("/hello")) { // パスが「/hello」か比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLの「?」より後ろを取り出します。
                String name = query == null ? "ゲスト" : query.substring(5); // 値がないときは「ゲスト」にします。
                name = URLDecoder.decode(name, "UTF-8"); // %で始まる表記を元の文字に戻します。
                message = "こんにちは、" + name + "さん！"; // 名前をあいさつに入れます。

            } else if (path.equals("/todos")) { // /todosのパスか比べます。
                List<String> todos = new ArrayList<>(); // Todoを入れるリストを作ります。
                todos.add("牛乳を買う"); // 1件目のTodoを入れます。
                todos.add("卵を買う"); // 2件目のTodoを入れます。
                todos.add("パンを買う"); // 3件目のTodoを入れます。
                todos.add("コーヒーを買う"); // 4件目のTodoを入れます。
                String html = "<ul>"; // HTMLのリストを始めます。
                for (String todo : todos) { // Todoを1件ずつ取り出します。
                    html += "<li>" + todo + "</li>"; // TodoをHTMLの項目として追加します。
                }
                html += "</ul>"; // HTMLのリストを閉じます。
                message = html; // 組み立てたHTMLを返事にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // /todosの返事をUTF-8のHTMLとして設定します。

            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else if (path.equals("/menu")) {
                message = "今日の定食はカレー！";
            } else {
                message = "ページが見つかりません";
            }
            byte[] body = message.getBytes("UTF-8"); // 文字をUTF-8のデータにします。【毎】
            exchange.sendResponseHeaders(200, body.length); // 正常の返事とデータの長さを送ります。【毎】
            exchange.getResponseBody().write(body); // 文字のデータをブラウザーへ送ります。【毎】
            exchange.getResponseBody().close(); // 返事を送り終えたことを伝えます。【毎】
        }); // トップページの処理を登録します。【毎】
        server.start(); // サーバーを起動します。【1】
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことをターミナルに表示します。【1】
    } // mainの処理はここまでです。
} // Appクラスはここまでです。
