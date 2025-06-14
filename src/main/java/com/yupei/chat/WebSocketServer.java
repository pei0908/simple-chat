package com.yupei.chat;

import org.glassfish.tyrus.server.Server;
import java.util.Collections;

public class WebSocketServer {
    public static void main(String[] args) {
        Server server = new Server("localhost", 9000, "/", Collections.emptyMap(), ChatServer.class);
        try {
            server.start();
            System.out.println("WebSocket 服务器已启动，端口：9000");
            System.in.read(); // 阻塞等待退出
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}