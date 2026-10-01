import com.sun.net.httpserver.HttpServer; // Webサーバーを使うための部品を読み込みます。
import java.net.InetSocketAddress; // ポート番号を指定するための部品を読み込みます。
import java.net.URLDecoder; // URLの記号表記を元の文字に戻す部品を読み込みます。
import java.nio.charset.StandardCharsets; // 文字コードを指定する部品を読み込みます。
import java.util.ArrayList; // リストを作る部品を読み込みます。
import java.util.List; // リストを使う部品を読み込みます。

public class App { // Appという名前のプログラムです。
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを保存するリストです。
    static int nextId = 1; // ★変更 次に使う番号です。

    public static void main(String[] args) throws Exception { // プログラムの開始位置です。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番ポートでサーバーを用意します。【1】
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更 サンプルのTodoを追加します。
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更 サンプルのTodoを作ります。
        egg.setDone(true); // ★変更 卵を買うTodoを完了にします。
        todos.add(egg); // ★変更 卵を買うTodoを追加します。
        server.createContext("/", exchange -> { // トップページに来たときの処理をここに書きます。【1】
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // GETやPOSTなどの方法を取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の返事をUTF-8の文字として設定します。
            if (path.equals("/add") && method.equals("POST")) { // POSTでTodo追加の依頼が来たか調べます。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られた本文をUTF-8で読み込みます。
                String value = body.substring(5); // 「todo=」の後ろを取り出します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 URL表記を元の日本語に戻します。
                if (!title.trim().isEmpty()) { // ★変更 入力が空でないときだけ追加します。
                    todos.add(new Todo(nextId, title)); // ★変更 Todoをリストに追加します。
                    nextId++; // ★変更 次の番号に進めます。
                }
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先をトップページにします。
                exchange.sendResponseHeaders(303, -1); // トップページへ移動する返事を送ります。
                exchange.close(); // 通信を閉じます。
                return; // この分岐の処理を終えます。
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加 GETで完了の依頼が来たか調べます。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLの「?」以降を取り出します。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idが指定されているか調べます。
                    try { // ★追加 idを数字に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idの値を数字にします。
                        for (Todo todo : todos) { // ★追加 Todoを1件ずつ確認します。
                            if (todo.getId() == id) { // ★追加 idが一致するか調べます。
                                todo.setDone(true); // ★追加 一致したTodoを完了にします。
                                break; // ★追加 一致したTodoの確認を終えます。
                            }
                        }
                    } catch (NumberFormatException e) { // ★追加 数字でないidは何もしません。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先をトップページにします。
                exchange.sendResponseHeaders(303, -1); // ★追加 トップページへ戻す返事を送ります。
                exchange.close(); // ★追加 通信を閉じます。
                return; // ★追加 この分岐の処理を終えます。
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加 GETで削除の依頼が来たか調べます。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLの「?」以降を取り出します。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idが指定されているか調べます。
                    try { // ★追加 idを数字に変換します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idの値を数字にします。
                        todos.removeIf(todo -> todo.getId() == id); // ★追加 idが一致するTodoをリストから削除します。
                    } catch (NumberFormatException e) { // ★追加 数字でないidは何もしません。
                    }
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先をトップページにします。
                exchange.sendResponseHeaders(303, -1); // ★追加 トップページへ戻す返事を送ります。
                exchange.close(); // ★追加 通信を閉じます。
                return; // ★追加 この分岐の処理を終えます。
            } else if (path.equals("/")) { // トップページを開いたときの表示を作ります。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // Todo追加フォームと一覧を作ります。
                for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します。
                    String mark = ""; // ★変更 完了印を用意します。
                    if (todo.isDone()) { // ★変更 完了しているか調べます。
                        mark = " ✔"; // ★変更 完了印を設定します。
                    }
                    html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId()
                            + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                                                                                                // Todoを一覧に加え、id付きの完了・削除リンクを付けます。
                }
                html += "</ul>"; // 一覧を閉じます。
                message = html; // 作ったHTMLを返事にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // トップページをUTF-8のHTMLとして設定します。
            } else { // ★変更 該当するページがないときの返事です。
                message = "ページが見つかりません"; // ★変更 見つからないページの返事です。
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

class Todo { // ★変更 Todoの情報をまとめるクラスです。
    private final int id; // ★変更 Todoの番号です。
    private final String title; // ★変更 Todoの内容です。
    private boolean done; // ★変更 Todoが完了したかどうかです。

    Todo(int id, String title) { // ★変更 Todoを作ります。
        this.id = id; // ★変更 番号を保存します。
        this.title = title; // ★変更 内容を保存します。
        this.done = false; // ★変更 最初は未完了にします。
    }

    int getId() { // ★変更 番号を読み出します。
        return id; // ★変更 番号を返します。
    }

    String getTitle() { // ★変更 内容を読み出します。
        return title; // ★変更 内容を返します。
    }

    boolean isDone() { // ★変更 完了状態を読み出します。
        return done; // ★変更 完了状態を返します。
    }

    void setDone(boolean done) { // ★変更 完了状態を書き換えます。
        this.done = done; // ★変更 完了状態を保存します。
    }
} // ★変更 Todoクラスはここまでです。
