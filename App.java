import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection; // ★ SQLite connection
import java.sql.DriverManager; // ★ SQLite connection
import java.sql.PreparedStatement; // ★ Prepared SQL
import java.sql.ResultSet; // ★ SELECT results
import java.sql.Statement; // ★ table creation
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class App {
    static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ SQLite database
    static Connection connection; // ★ SQLite connection

    public static void main(String[] args) throws Exception {
        connection = DriverManager.getConnection(DB_URL); // ★ connect to SQLite
        try (Statement statement = connection.createStatement()) { // ★ create table if needed
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER, due_date TEXT)");
            try {
                statement.executeUpdate("ALTER TABLE todos ADD COLUMN due_date TEXT");
            } catch (java.sql.SQLException alreadyExists) {
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String message;

            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String title = "";
                String dueDate = "";
                for (String field : body.split("&")) {
                    int equals = field.indexOf('=');
                    if (equals < 0)
                        continue;
                    String key = URLDecoder.decode(field.substring(0, equals), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(field.substring(equals + 1), StandardCharsets.UTF_8);
                    if (key.equals("todo"))
                        title = value;
                    if (key.equals("dueDate"))
                        dueDate = value;
                }
                if (!title.trim().isEmpty() && isValidDate(dueDate)) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO todos (title, done, due_date) VALUES (?, 0, ?)")) {
                        statement.setString(1, title);
                        statement.setString(2, dueDate);
                        statement.executeUpdate();
                    } catch (java.sql.SQLException e) { // ★ translate SQL error for HttpHandler
                        throw new java.io.IOException("Could not add Todo", e);
                    }
                }
                redirect(exchange);
                return;
            } else if (path.equals("/done") && method.equals("GET")) {
                int id = queryId(exchange.getRequestURI().getQuery());
                if (id >= 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE todos SET done = 1 WHERE id = ?")) { // ★ update
                        statement.setInt(1, id);
                        statement.executeUpdate();
                    } catch (java.sql.SQLException e) { // ★ translate SQL error for HttpHandler
                        throw new java.io.IOException("Could not mark Todo done", e);
                    }
                }
                redirect(exchange);
                return;
            } else if (path.equals("/delete") && method.equals("GET")) {
                int id = queryId(exchange.getRequestURI().getQuery());
                if (id >= 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM todos WHERE id = ?")) { // ★ delete
                        statement.setInt(1, id);
                        statement.executeUpdate();
                    } catch (java.sql.SQLException e) { // ★ translate SQL error for HttpHandler
                        throw new java.io.IOException("Could not delete Todo", e);
                    }
                }
                redirect(exchange);
                return;
            } else if (path.equals("/delete-done") && method.equals("POST")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM todos WHERE done = 1")) {
                    statement.executeUpdate();
                } catch (java.sql.SQLException e) {
                    throw new java.io.IOException("Could not delete completed Todos", e);
                }
                redirect(exchange);
                return;
            } else if (path.equals("/")) {
                List<Todo> todos; // ★ SELECT results
                try {
                    todos = loadTodos(); // ★ read list from SQLite
                } catch (java.sql.SQLException e) { // ★ translate SQL error for HttpHandler
                    throw new java.io.IOException("Could not load Todos", e);
                }

                int remaining = 0;
                for (Todo todo : todos) {
                    if (!todo.done)
                        remaining++;
                }
                String html = "<!doctype html><html><head><meta charset='UTF-8'><title>Todo</title>"
                        + "<style>body{max-width:640px;margin:24px 0;padding:0 16px;font-size:16px;"
                        + "background-color:#fffdd0;font-family:'Noto Sans JP',sans-serif}"
                        + "h1{font-size:24px}h2{font-size:23px;border-top:1px solid #ddd;padding-top:8px}"
                        + "ul{list-style:none;padding-left:0}li{margin:8px 0}"
                        + ".overdue{color:red}"
                        + ".overdue-label{color:red;font-size:19px}"
                        + ".remaining{font-size:19px;color:blue}"
                        + "input[type=checkbox]{accent-color:black}"
                        + "form#todo-form>div{margin-bottom:6px}"
                        + "form#todo-form input,form#todo-form button{font-size:17px;padding:3px}"
                        + ".action-button{display:inline-block;padding:3px 8px;margin-left:5px;"
                        + "border:1px solid #888;border-radius:3px;background:#d9f2f7;color:#111;"
                        + "text-decoration:none;font-size:13px}</style></head><body>"
                        + "<h1>私のTodo</h1>"
                        + "<form id='todo-form' method='post' action='/add'>"
                        + "<div><label>Todo <input name='todo' required></label></div>"
                        + "<div><label>期限 <input type='date' name='dueDate' required></label></div>"
                        + "<div><button>追加</button></div></form>";
                html += "<p class='remaining'>残り: " + remaining + "件</p>";
                if (todos.isEmpty()) {
                    html += "<p>※Todoが登録されていません</p>";
                } else {
                    String currentDueDate = null;
                    for (Todo todo : todos) {
                        String dueDate = todo.dueDate == null ? "No date" : todo.dueDate;
                        if (!dueDate.equals(currentDueDate)) {
                            if (currentDueDate != null)
                                html += "</ul>";
                            boolean overdueDate = isOverdue(dueDate) && hasIncompleteTodo(todos, todo.dueDate);
                            html += "<h2>" + escapeHtml(dueDate)
                                    + (overdueDate ? " <span class='overdue-label'>期限切れ</span>" : "")
                                    + "</h2><ul>";
                            currentDueDate = dueDate;
                        }
                        String checked = todo.done ? " checked" : "";
                        boolean overdueTodo = !todo.done && isOverdue(todo.dueDate);
                        html += "<li><input type='checkbox'" + checked
                                + " onclick=\"if(this.checked){location.href='/done?id=" + todo.id
                                + "'}else{this.checked=true}\"> <span"
                                + (overdueTodo ? " class='overdue'" : "") + ">"
                                + escapeHtml(todo.title) + "</span>";
                        if (!todo.done)
                            html += " <a class='action-button' href='/done?id=" + todo.id + "'>\u5B8C\u4E86</a>";
                        html += " <a class='action-button' href='/delete?id=" + todo.id + "'>\u524A\u9664</a></li>";
                    }
                    html += "</ul>";
                }
                html += "<form method='post' action='/delete-done' style='margin-top:24px'>"
                        + "<button type='submit' style='font-size:17px'>完了したTodoを一括削除</button></form>";
                message = html + "</body></html>";
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else {
                message = "Page not found";
            }

            byte[] response = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });
        server.createContext("/api/todos", exchange -> { // Todo一覧APIの入口を追加する
            if (!exchange.getRequestMethod().equals("GET")) { // GET以外のリクエストを拒否する
                exchange.sendResponseHeaders(405, -1); // メソッド不許可を返す
                exchange.close(); // レスポンスを閉じる
                return; // 以降の処理を終了する
            } // メソッド判定を終える
            List<Todo> todos; // Todo一覧を格納する変数を宣言する
            try { // データベースからTodo一覧を読み込む
                todos = loadTodos(); // 既存の一覧取得メソッドを呼び出す
            } catch (java.sql.SQLException e) { // SQL例外をHTTPハンドラー用の例外に変換する
                throw new java.io.IOException("Could not load Todos", e); // 読み込みエラーを通知する
            } // Todo一覧の読み込みを終える
            StringBuilder json = new StringBuilder("["); // JSON配列の作成を始める
            for (int i = 0; i < todos.size(); i++) { // 全Todoを順番にJSONへ変換する
                Todo todo = todos.get(i); // 現在のTodoを取り出す
                if (i > 0)
                    json.append(","); // 2件目以降の前に区切り文字を追加する
                json.append("{\"title\":\"").append(escapeJson(todo.title)) // タイトルをJSON文字列として追加する
                        .append("\",\"done\":").append(todo.done).append("}"); // 完了状態を追加してオブジェクトを閉じる
            } // 全Todoの変換を終える
            json.append("]"); // JSON配列を閉じる
            byte[] response = json.toString().getBytes(StandardCharsets.UTF_8); // JSONをUTF-8のバイト列にする
            exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetを付けずにContent-Typeを設定する
            exchange.sendResponseHeaders(200, response.length); // 成功とレスポンス長を送信する
            exchange.getResponseBody().write(response); // JSON本文を書き込む
            exchange.close(); // レスポンスを閉じる
        }); // APIの入口を定義する

        server.start();
        System.out.println("Server started: http://localhost:8080 (stop with Ctrl+C)");
    }

    static List<Todo> loadTodos() throws java.sql.SQLException { // ★ SELECT from SQLite
        List<Todo> todos = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, title, done, due_date FROM todos ORDER BY due_date IS NULL, due_date, id");
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                todos.add(new Todo(results.getInt("id"), results.getString("title"),
                        results.getInt("done") != 0, results.getString("due_date")));
            }
        }
        return todos;
    }

    static int queryId(String query) {
        if (query != null && query.startsWith("id=")) {
            try {
                return Integer.parseInt(query.substring(3));
            } catch (NumberFormatException ignored) {
                // Ignore invalid IDs.
            }
        }
        return -1;
    }

    static boolean isValidDate(String value) {
        try {
            return value != null && LocalDate.parse(value).toString().equals(value);
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    static boolean isOverdue(String value) {
        try {
            return value != null && LocalDate.parse(value).isBefore(LocalDate.now());
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    static boolean hasIncompleteTodo(List<Todo> todos, String dueDate) {
        for (Todo item : todos) {
            if (!item.done && java.util.Objects.equals(item.dueDate, dueDate))
                return true;
        }
        return false;
    }

    static void redirect(HttpExchange exchange) throws java.io.IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String escapeJson(String text) { // JSON文字列内で必要な文字をエスケープする
        StringBuilder escaped = new StringBuilder(); // エスケープ後の文字列を作成する
        for (int i = 0; i < text.length(); i++) { // タイトルを1文字ずつ確認する
            char c = text.charAt(i); // 現在の文字を取り出す
            switch (c) { // 特殊文字ごとの変換を行う
                case '"':
                    escaped.append("\\\"");
                    break; // 引用符をエスケープする
                case '\\':
                    escaped.append("\\\\");
                    break; // バックスラッシュをエスケープする
                case '\b':
                    escaped.append("\\b");
                    break; // バックスペースをエスケープする
                case '\f':
                    escaped.append("\\f");
                    break; // フォームフィードをエスケープする
                case '\n':
                    escaped.append("\\n");
                    break; // 改行をエスケープする
                case '\r':
                    escaped.append("\\r");
                    break; // 復帰文字をエスケープする
                case '\t':
                    escaped.append("\\t");
                    break; // タブをエスケープする
                default: // その他の文字を処理する
                    if (c < 0x20) { // JSONでそのまま使えない制御文字を確認する
                        escaped.append(String.format("\\u%04x", (int) c)); // 制御文字をUnicode表記にする
                    } else { // 通常の文字を処理する
                        escaped.append(c); // 文字をそのまま追加する
                    } // 制御文字の判定を終える
            } // 特殊文字の変換を終える
        } // 全文字の確認を終える
        return escaped.toString(); // エスケープ済み文字列を返す
    } // JSONエスケープメソッドを定義する

    static class Todo {
        final int id;
        final String title;
        final boolean done;
        final String dueDate;

        Todo(int id, String title, boolean done, String dueDate) {
            this.id = id;
            this.title = title;
            this.done = done;
            this.dueDate = dueDate;
        }
    }
}
