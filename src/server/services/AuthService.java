package server.services;

import java.sql.*;
import java.util.ArrayList;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNickname(String login, String pass) {
        String query = String.format("select username from user\n" +
                "where login = '%s'\n" +
                "and password_hash = '%s'", login, MD5(pass));
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Integer getUserIdByNickname(String username) {
        String query = String.format("select id from user where username = '%s'", username);
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return Integer.parseInt(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static void addUser(String username, String login, String passwordHash) {
        String query = "insert into user (username, login, password_hash) values (?, ?, ?)";
        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, login);
            stmt.setString(3, passwordHash);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<String> getUserBlacklist(String username) {

        ArrayList<String> blockedUsernames = new ArrayList<>();

        String query = String.format(
                "select u2.username from blacklist b " +
                        "inner join user u1 on b.user_id = u1.id " +
                        "inner join user u2 on b.blocked_user_id = u2.id " +
                        "where u1.username = '%s'", username);

        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                blockedUsernames.add(rs.getString(1));
            }
            return blockedUsernames;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean addToBlacklist(String username, String blockedUsername) {
        Integer userId = getUserIdByNickname(username);
        Integer blockedUserId = getUserIdByNickname(blockedUsername);

        if (userId != null && blockedUserId != null) {
            String query = "insert into blacklist (user_id, blocked_user_id) values (?, ?)";
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setInt(2, blockedUserId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    public static boolean removeFromBlacklist(String username, String blockedUsername) {
        Integer userId = getUserIdByNickname(username);
        Integer blockedUserId = getUserIdByNickname(blockedUsername);

        if (userId != null && blockedUserId != null) {
            String query = "delete from blacklist where user_id = ? and blocked_user_id = ?";
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setInt(2, blockedUserId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    public static boolean changeLogin(String username, String login) {
        Integer userId = getUserIdByNickname(username);

        if (userId != null && login != null) {
            String query = "update user set login = ? where id = ?";
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, login);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }
}
