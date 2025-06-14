package com.yupei.chat;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@ServerEndpoint("/chat")
public class ChatServer {
    private static final Map<String, Session> userSessionMap = new ConcurrentHashMap<>();
    private static final Map<Session, String> sessionUserMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        // 连接后提示客户端发送 SETNAME
        sendMessage(session, "系统：欢迎，请发送 'SETNAME:xxx' 来设置您的用户名");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 处理用户名设置或更改
        if (message.startsWith("SETNAME:")) {
            String newName = message.substring("SETNAME:".length()).trim();

            // 名称冲突检查
            if (userSessionMap.containsKey(newName)) {
                sendMessage(session, "系统：该用户名已被占用，请换一个");
                return;
            }

            String oldName = sessionUserMap.get(session);
            if (oldName != null) {
                // 移除旧绑定
                userSessionMap.remove(oldName);
            }

            // 绑定新用户名
            userSessionMap.put(newName, session);
            sessionUserMap.put(session, newName);

            // 根据是否已有旧名，分别广播“加入”或“更名”事件
            if (oldName == null) {
                // 首次设置用户名
                broadcast("系统：" + newName + " 加入了聊天室");
            } else {
                // 更改用户名
                sendMessage(session, "系统：用户名更改成功，你的用户ID是 " + newName);
                broadcast("系统：" + oldName + " 更改了名字为 " + newName);
            }

            // 更新并广播在线用户列表
            broadcastOnlineUsers();
            return;
        }

        // 未设置用户名则拒绝其他消息
        if (!sessionUserMap.containsKey(session)) {
            sendMessage(session, "系统：请先发送 'SETNAME:你的名字' 来设置用户名");
            return;
        }

        // 处理私信功能
        if (message.startsWith("to")) {
            int colonIndex = message.indexOf(":");
            if (colonIndex == -1) {
                sendMessage(session, "系统：格式错误，正确格式是 to用户名:消息内容");
                return;
            }

            String targetUser = message.substring(2, colonIndex).trim();
            String msgContent = message.substring(colonIndex + 1).trim();
            Session targetSession = userSessionMap.get(targetUser);

            if (targetSession != null && targetSession.isOpen()) {
                String senderUser = sessionUserMap.get(session);
                sendMessage(targetSession, "私信来自 " + senderUser + "：" + msgContent);
                sendMessage(session, "你发给 " + targetUser + "：" + msgContent);
            } else {
                sendMessage(session, "系统：用户 " + targetUser + " 不在线或不存在");
            }
            return;
        }

        // 处理群聊消息
        String sender = sessionUserMap.get(session);
        broadcast(sender + "：" + message);
    }

    @OnClose
    public void onClose(Session session) {
        String name = sessionUserMap.remove(session);
        if (name != null) {
            userSessionMap.remove(name);
            broadcast("系统：" + name + " 离开了聊天室");
            broadcastOnlineUsers();
        }
    }

    private void sendMessage(Session session, String message) {
        session.getAsyncRemote().sendText(message);
    }

    private void broadcast(String message) {
        userSessionMap.values().forEach(s -> s.getAsyncRemote().sendText(message));
    }

    private void broadcastOnlineUsers() {
        String list = "__userlist__:" + String.join(",", userSessionMap.keySet());
        broadcast(list);
    }
}
