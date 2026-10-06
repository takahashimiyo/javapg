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

public class App {
    static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ SQLite database
    static Connection connection; // ★ SQLite connection

    public static void main(String[] args) throws Exception {
        connection = DriverManager.getConnection(DB_URL); // ★ connect to SQLite
        try (Statement statement = connection.createStatement()) { // ★ create table if needed
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String message;

            if (path.equals("/add") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String title = "";
                for (String field : body.split("&")) {
                    if (field.startsWith("todo=")) {
                        title = URLDecoder.decode(field.substring(5), StandardCharsets.UTF_8);
                        break;
                    }
                }
                if (!title.trim().isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO todos (title, done) VALUES (?, 0)")) { // ★ insert
                        statement.setString(1, title);
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
                String html = "<!doctype html><html><head><meta charset='UTF-8'><title>わたしのTodo</title>"
                        + "<style>body{max-width:640px;margin:24px 0;padding:0 16px;font-size:16px}"
                        + "h1{font-size:24px}ul{list-style:none;padding-left:0}li{margin:8px 0}"
                        + "input[type=checkbox]{accent-color:black}</style></head><body>"
                        + "<h1>わたしのTodo</h1><p>あと" + remaining + "件です</p>"
                        + "<form method='post' action='/add'><input name='todo'><button>Add</button></form>";
                if (todos.isEmpty()) {
                    html += "<p>No todos yet.</p>";
                } else {
                    html += "<ul>";
                    for (Todo todo : todos) {
                        String checked = todo.done ? " checked" : "";
                        html += "<li><input type='checkbox'" + checked
                                + " onclick=\"if(this.checked){location.href='/done?id=" + todo.id
                                + "'}else{this.checked=true}\"> " + escapeHtml(todo.title)
                                + " <a href='/done?id=" + todo.id + "'>Done</a>"
                                + " <a href='/delete?id=" + todo.id + "'>Delete</a></li>";
                    }
                    html += "</ul>";
                }
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
        server.start();
        System.out.println("Server started: http://localhost:8080 (stop with Ctrl+C)");
    }

    static List<Todo> loadTodos() throws java.sql.SQLException { // ★ SELECT from SQLite
        List<Todo> todos = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, title, done FROM todos ORDER BY id");
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                todos.add(new Todo(results.getInt("id"), results.getString("title"),
                        results.getInt("done") != 0));
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

    static class Todo {
        final int id;
        final String title;
        final boolean done;

        Todo(int id, String title, boolean done) {
            this.id = id;
            this.title = title;
            this.done = done;
        }
    }
}
