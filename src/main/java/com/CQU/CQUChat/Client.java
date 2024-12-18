package com.CQU.CQUChat;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.CloseableHttpResponse;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8888;

    private static String clientName;
    private static JFrame frame;

    private static List<JFrame> windows = new ArrayList<>();

    private static JList<String> clientList;
    private static DefaultListModel<String> clientListModel;
    private static List<String> selectedClients = new ArrayList<>();
    private static List<String> onlineClients = new ArrayList<>();
    private static Socket socket;
    private static BufferedWriter writer;
    private static String lastFileName = "";
    static JTextField userTextField;
    private static JPasswordField passwordField;
    private static JButton signupButton;
    private static JButton loginButton;

    public static void main(String[] args) {
        startClient();
    }

    public static void startClient() {
        frame = new JFrame("CQUChat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        // 标题
        JLabel titleLabel = new JLabel("Welcome to CQUChat", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        frame.add(titleLabel, BorderLayout.NORTH);

        // 登录面板
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(240, 240, 255)); // 柔和的背景色

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username: "), gbc);

        gbc.gridx = 1;
        userTextField = new JTextField(20);
        panel.add(userTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Password: "), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);

        frame.add(panel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 240, 255));

        signupButton = new JButton("Sign Up");
        signupButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton = new JButton("Log In");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        buttonPanel.add(signupButton);
        buttonPanel.add(loginButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        signupButtonListeners();
        loginButtonListeners();
    }

    private static void loginButtonListeners() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nickname = userTextField.getText();
                String password = new String(passwordField.getPassword());

                // 使用 Apache HttpClient
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    String url = "http://" + SERVER_IP + ":8083/login?nickname=" + nickname + "&password=" + password;
                    HttpGet request = new HttpGet(url);

                    // 发送请求并获取响应
                    try (CloseableHttpResponse response = client.execute(request)) {
                        SwingUtilities.invokeLater(() -> {
                            if (response.getEntity().getContentLength() != 0) {
                                JOptionPane.showMessageDialog(frame, "Login Successful");
                                frame.setVisible(false);
                                try {
                                    openOnlineClientsWindow();
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            } else {
                                JOptionPane.showMessageDialog(frame, "Login Failed");
                            }
                        });
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private static void signupButtonListeners() {
        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nickname = userTextField.getText();
                String password = new String(passwordField.getPassword());

                // 使用 Apache HttpClient
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    String url = "http://" + SERVER_IP + ":8083/signup?nickname=" + nickname + "&password=" + password;
                    HttpGet request = new HttpGet(url);

                    // 发送请求并获取响应
                    try (CloseableHttpResponse response = client.execute(request)) {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        SwingUtilities.invokeLater(() -> {
                            if (responseBody.equals("1")) {
                                JOptionPane.showMessageDialog(frame, "Signup Successful");
                                frame.setVisible(false);
                                try {
                                    openOnlineClientsWindow();
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            } else {
                                JOptionPane.showMessageDialog(frame, "Signup Failed");
                            }
                        });
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    static void openOnlineClientsWindow() throws IOException {
        frame = new JFrame("在线聊天室");
        frame.setSize(400, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255)); // 柔和背景色

        // 标题
        JLabel titleLabel = new JLabel("Online Users", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(173, 216, 230)); // 淡蓝背景色
        titleLabel.setForeground(Color.WHITE); // 白色字体
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 用户列表
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setFont(new Font("SimSun", Font.PLAIN, 16));
        clientList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        clientList.setBackground(new Color(255, 255, 255));
        clientList.setBorder(BorderFactory.createLineBorder(new Color(173, 216, 230), 2));
        clientList.setForeground(new Color(0, 102, 204));

        JScrollPane clientListScrollPane = new JScrollPane(clientList);
        clientListScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(173, 216, 230), 1),
                "Users Online",
                0,
                0,
                new Font("SimSun", Font.BOLD, 14),
                new Color(0, 102, 204)));
        mainPanel.add(clientListScrollPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(240, 248, 255));

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("SimSun", Font.BOLD, 14));
        closeButton.setBackground(new Color(173, 216, 230));
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> frame.dispose());
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        connectToServer();
        startListening();

        clientList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (SwingUtilities.isLeftMouseButton(evt)) {
                    int[] selectedIndices = clientList.getSelectedIndices();
                    selectedClients.clear();
                    for (int index : selectedIndices) {
                        selectedClients.add(clientListModel.getElementAt(index));
                    }
                } else if (SwingUtilities.isRightMouseButton(evt)) {
                    openChatRoom();
                }
            }
        });
    }

    private static void connectToServer() throws IOException {
        socket = new Socket(SERVER_IP, SERVER_PORT);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        clientName = userTextField.getText();
        writer.write(clientName);
        writer.newLine();
        writer.flush();
    }

    private static void startListening() {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = reader.readLine()) != null) {
                    handleIncomingMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void openChatRoom() {
        if (!selectedClients.isEmpty()) {
            // Send the selectedClients list to the server and request a new group window
            StringBuilder createGroupCommand = new StringBuilder("/openChatRoom ");
            String userInput = JOptionPane.showInputDialog("Enter ChatRoomName:");
            createGroupCommand.append(userInput).append(" ");
            for (String client : selectedClients) {
                createGroupCommand.append(client).append(" ");
            }
            sendMessage(createGroupCommand.toString());
        }
    }

    private static void sendMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleIncomingMessage(String message) {
        if (message.startsWith("/clientlist")) {
            updateClientList(message);
        } else if (message.startsWith("/grouplist")) {
            updateGroupList(message);
        } else if (message.startsWith("/file")) {
            handleFileTransferCommand(message);
        } else {
            sendMessageToChatRoom(message);
        }
    }

    private static void updateClientList(String message) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            String[] parts = message.split(" ");
            for (int i = 1; i < parts.length; i++) {
                onlineClients.add(parts[i]);
                clientListModel.addElement(parts[i]);
            }
        });
    }

    private static void updateGroupList(String message) {
        // Format: /grouplist groupName member1 member2 ...
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(" ");
            String groupName = parts[1];
            DefaultListModel<String> groupListModel = new DefaultListModel<>();
            for (int i = 2; i < parts.length; i++) {
                groupListModel.addElement(parts[i]);
            }
            openGroupChatWindow(groupName, groupListModel);
        });
    }

    private static void sendMessageToChatRoom(String message) {
        // 解析消息格式 "chatRoomName context"
        String[] parts = message.split(" ", 2);
        String chatRoomName = parts[0].substring(1, parts[0].length() - 1); // 去除chatRoomName前的 '['
        String context = parts[1];
        // 在对应的聊天室窗口中显示消息
        SwingUtilities.invokeLater(() -> {
            for (JFrame window : windows) {
                if (window.getTitle().equals("Chat Room: " + chatRoomName)) {
                    JTextPane groupChatArea = findGroupChatArea(window);
                    StyledDocument doc = groupChatArea.getStyledDocument();
                    try {
                        Style style = doc.addStyle("MessageStyle", null);
                        StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
                        String msg = context + "\n";
                        doc.setParagraphAttributes(doc.getLength(), msg.length(), style, false);
                        doc.insertString(doc.getLength(), context + "\n", null);
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        });
    }

    private static JTextPane findGroupChatArea(JFrame window) {
        for (Component component : window.getContentPane().getComponents()) {
            if (component instanceof JScrollPane) {
                Component[] scrollPaneComponents = ((JScrollPane) component).getViewport().getComponents();
                for (Component scrollPaneComponent : scrollPaneComponents) {
                    if (scrollPaneComponent instanceof JTextPane) {
                        return (JTextPane) scrollPaneComponent;
                    }
                }
            }
        }
        throw new IllegalStateException("Could not find JTextPane in the given JFrame.");
    }

    private static void openGroupChatWindow(String groupName, DefaultListModel<String> groupListModel) {
        // 创建聊天窗口
        JFrame groupChatFrame = new JFrame("Chat Room: " + groupName);
        groupChatFrame.setSize(600, 400); // 调整窗口高度以适应新按钮
        groupChatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        groupChatFrame.setLocationRelativeTo(null);

        // 创建成员列表模型并设置
        DefaultListModel<String> groupListModel2 = new DefaultListModel<>();
        for (int i = 0; i < groupListModel.getSize(); i++) {
            groupListModel2.add(i, groupListModel.get(i));
        }
        groupListModel2.add(0, "聊天室成员");
        for (int i = 0; i < groupListModel2.getSize(); i++) {
            if (groupListModel2.get(i).equals(clientName)) {
                groupListModel2.remove(i);
                groupListModel2.add(groupListModel.getSize(), "你：" + clientName);
            }
        }

        // 成员列表
        JList<String> groupList = new JList<>(groupListModel2);
        JScrollPane groupListScrollPane = new JScrollPane(groupList);
        groupListScrollPane.setPreferredSize(new Dimension(100, 250));
        groupList.setBackground(new Color(240, 240, 240));
        groupList.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); // 设置支持中文的字体

        // 聊天区域
        JTextPane groupChatArea = new JTextPane();
        groupChatArea.setEditable(false);
        groupChatArea.setBackground(new Color(245, 245, 245));
        groupChatArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); // 设置支持中文的字体
        StyledDocument doc = groupChatArea.getStyledDocument();
        JScrollPane groupChatScrollPane = new JScrollPane(groupChatArea);

        // 消息输入框
        JTextArea groupMessageField = new JTextArea(5, 20);
        groupMessageField.setLineWrap(true);
        groupMessageField.setWrapStyleWord(true);
        groupMessageField.setBackground(new Color(255, 255, 255));
        groupMessageField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14)); // 设置支持中文的字体
        groupMessageField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // 发送按钮
        JButton groupSendButton = new JButton("Send");
        groupSendButton.setBackground(new Color(70, 130, 180));
        groupSendButton.setForeground(Color.WHITE);
        groupSendButton.setFont(new Font("Arial", Font.BOLD, 14));
        groupSendButton.setFocusPainted(false);
        groupSendButton.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180)));
        groupSendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendGroupMessage(groupName, groupMessageField.getText());
                try {
                    Style style = doc.addStyle("MessageStyle", null);
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
                    String msg = groupMessageField.getText() + "\n";
                    doc.setParagraphAttributes(doc.getLength(), msg.length(), style, false);
                    doc.insertString(doc.getLength(), " 我： " + groupMessageField.getText() + "\n", null);
                    groupChatArea.setCaretPosition(doc.getLength()); // 自动滚动到底部
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
                groupMessageField.setText("");
            }
        });

        // 发送文件按钮
        JButton sendFileButton = new JButton("Send File");
        sendFileButton.setBackground(new Color(34, 139, 34));
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendFileButton.setFocusPainted(false);
        sendFileButton.setBorder(BorderFactory.createLineBorder(new Color(34, 139, 34)));
        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(groupChatFrame);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendFile(selectedFile.getAbsolutePath(), groupName, selectedFile.getName(), groupChatArea);
                }
            }
        });

        // 快捷回复按钮
        JButton quickReplyButton = new JButton("Quick Reply");
        quickReplyButton.setBackground(new Color(255, 165, 0));
        quickReplyButton.setForeground(Color.WHITE);
        quickReplyButton.setFont(new Font("Arial", Font.BOLD, 14));
        quickReplyButton.setFocusPainted(false);
        quickReplyButton.setBorder(BorderFactory.createLineBorder(new Color(255, 165, 0)));
        quickReplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] quickReplies = { "Hello", "How are you?", "Goodbye" };
                String selectedReply = (String) JOptionPane.showInputDialog(
                        groupChatFrame,
                        "Select a quick reply:",
                        "Quick Reply",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        quickReplies,
                        quickReplies[0]);
                if (selectedReply != null) {
                    groupMessageField.setText(selectedReply);
                }
            }
        });

        // 表情按钮
        JButton emojiButton = new JButton("Emoji");
        emojiButton.setBackground(new Color(255, 215, 0));
        emojiButton.setForeground(Color.WHITE);
        emojiButton.setFont(new Font("Arial", Font.BOLD, 14));
        emojiButton.setFocusPainted(false);
        emojiButton.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0)));
        emojiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] emojis = { ":)", ":D", ":(", ":P" };
                String selectedEmoji = (String) JOptionPane.showInputDialog(
                        groupChatFrame,
                        "Select an emoji:",
                        "Emoji",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        emojis,
                        emojis[0]);
                if (selectedEmoji != null) {
                    groupMessageField.setText(groupMessageField.getText() + " " + selectedEmoji);
                }
            }
        });

        // 布局设置
        groupChatFrame.setLayout(new BorderLayout(10, 10));
        groupChatFrame.setBackground(new Color(240, 240, 240));

        // 添加组件
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        panel.setBackground(new Color(240, 240, 240));
        panel.add(groupMessageField);
        panel.add(groupSendButton);
        panel.add(quickReplyButton);
        panel.add(emojiButton);

        // 整体布局
        groupChatFrame.add(sendFileButton, BorderLayout.NORTH);
        groupChatFrame.add(groupListScrollPane, BorderLayout.EAST);
        groupChatFrame.add(groupChatScrollPane, BorderLayout.CENTER);
        groupChatFrame.add(panel, BorderLayout.SOUTH);

        groupChatFrame.setVisible(true);
        windows.add(groupChatFrame);
    }

    private static void showImage(JTextPane pane, String filePath) {
        StyledDocument doc = pane.getStyledDocument();
        pane.setCaretPosition(doc.getLength());
        System.out.println("-------------------" + filePath);
        ImageIcon image = new ImageIcon(filePath);
        // 可能需要调整图片大小以适应窗口
        pane.insertIcon(image);
        try {
            doc.insertString(doc.getLength(), "\n", null); // 在图片后添加一个换行
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static void sendGroupMessage(String groupName, String message) {
        try {
            writer.write("[" + groupName + "] " + " " + clientName + "：" + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendGroupFile(String groupName, String message) {
        try {
            writer.write("[" + groupName + "] " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleFileTransferCommand(String command) {
        // 解析文件传输命令，格式："/file groupName fileName bytesRead base64EncodedData"
        String[] parts = command.split(" ");
        String groupName = parts[1];
        String fileName = parts[2];
        int bytesRead = Integer.parseInt(parts[3]);
        String base64EncodedData = parts[4];

        // 处理文件传输
        handleIncomingFile(groupName, fileName, bytesRead, base64EncodedData);
    }

    private static void handleIncomingFile(String groupName, String fileName, int bytesRead, String base64EncodedData) {
        SwingUtilities.invokeLater(() -> {
            for (JFrame window : windows) {
                if (window.getTitle().equals("Chat Room: " + groupName)) {
                    boolean flag = false;
                    JTextPane groupChatArea = findGroupChatArea(window);
                    if (("thelastone" + lastFileName).equals(fileName)) {
                        StyledDocument doc = groupChatArea.getStyledDocument();
                        try {
                            Style style = doc.addStyle("MessageStyle", null);
                            StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
                            String msg = " 我接收到文件：" + lastFileName + "\n";
                            doc.setParagraphAttributes(doc.getLength(), msg.length(), style, false);
                            doc.insertString(doc.getLength(), " 我接收到文件：" + lastFileName + "\n", null);
                        } catch (BadLocationException e) {
                            throw new RuntimeException(e);
                        }
                        sendGroupFile(groupName, " " + clientName + "接收到文件：" + lastFileName);
                        flag = true;
                        byte[] fileBytes = Base64.getDecoder().decode(base64EncodedData);
                        saveFile(flag, groupChatArea, groupName, lastFileName, fileBytes);
                        break;
                    }
                    lastFileName = fileName;
                    // 将base64编码的数据解码为字节数组
                    byte[] fileBytes = Base64.getDecoder().decode(base64EncodedData);
                    saveFile(flag, groupChatArea, groupName, lastFileName, fileBytes);
                    break;
                }
            }
        });
    }

    private static void saveFile(boolean flag, JTextPane jTextPane, String groupName, String fileName,
                                 byte[] fileBytes) {
        String fileExtension = getFileExtension(fileName);
        if (flag && isImageFile(fileExtension)) {
            // 如果图片文件已经传输完毕，显示图片
            showImage(jTextPane, "./src/main/resources/file/" + groupName + "_" + clientName + "_" + fileName);
        } else {
            // 保存文件到本地，可以根据需要修改保存的路径
            String savePath = "./src/main/resources/file/" + groupName + "_" + clientName + "_" + fileName;
            try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(
                    new FileOutputStream(savePath, true))) {
                fileOutputStream.write(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendFile(String filePath, String groupName, String filename, JTextPane chatRoomArea) {
        try (BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            StyledDocument doc = chatRoomArea.getStyledDocument();
            Style style = doc.addStyle("MessageStyle", null);
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
            String msg = " 我发送了文件：" + filename + "\n";
            doc.setParagraphAttributes(doc.getLength(), msg.length(), style, false);
            doc.insertString(doc.getLength(), " 我发送了文件：" + filename + "\n", null);
            sendGroupFile(groupName, clientName + " 发送了文件：" + filename);
            String fileExtension = getFileExtension(filename);
            if (isImageFile(fileExtension)) {
                // 如果是图片文件，显示图片
                showImage(chatRoomArea, filePath);
            }
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                // 发送文件内容到服务器
                sendFileBytes(groupName, buffer, bytesRead, filename);
            }
            // 文件发送完后再发送一个结束标志位
            buffer = ("EndOfFile").getBytes();
            sendFileBytes(groupName, buffer, buffer.length, "thelastone" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    private static boolean isImageFile(String fileExtension) {
        // 假设图片文件的扩展名为 jpg、png、gif 等
        return Arrays.asList("jpg", "png", "gif").contains(fileExtension.toLowerCase());
    }

    private static void sendFileBytes(String groupName, byte[] buffer, int bytesRead, String filename) {
        byte[] actualData = Arrays.copyOf(buffer, bytesRead);
        // 将文件内容发送到服务器，格式："/file groupName fileName bytesRead base64EncodedData"
        String message = "/file " + groupName + " " + filename + " " + bytesRead + " "
                + Base64.getEncoder().encodeToString(actualData);
        sendMessage(message);
    }

}
