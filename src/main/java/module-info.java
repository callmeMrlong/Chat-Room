module org.example.org.example.org.example.chatroom {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.net.http;
    requires java.prefs;


    exports org.example.chatroom.server;
    opens org.example.chatroom.server to javafx.fxml;
    exports org.example.chatroom.client;
    opens org.example.chatroom.client to javafx.fxml;
}