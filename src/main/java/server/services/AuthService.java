package server.services;

import com.sun.istack.internal.NotNull;
import core.config.ConfigLoader;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;

public class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class);

    private static Connection connection;
    private static Statement stmt;
    private static String jdbcUrl = ConfigLoader.load().getProperty("datasource.url");

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(jdbcUrl);
            stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.error("JDBC connection error." + e);
        }
    }

    public static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("JDBC disconnection error." + e);
        }
    }

    public static String getUsername(String login, String pass) {
        String query = String.format(
                "select username from user\n" +
                "where login = '%s'\n" +
                "and password_hash = '%s'", login, MD5(pass));
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.error("getUsername query error." + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.error("Sql connection closed error." + e);
            }
        }
        return null;
    }

    public static Boolean isAdmin(@NotNull String username) {
        String query = String.format("select admin from user where username = '%s'", username);
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return Integer.parseInt(rs.getString(1)) == 1;
            }
        } catch (SQLException e) {
            LOGGER.error("isAdmin query error." + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.error("Sql connection closed error." + e);
            }
        }
        return false;
    }

    public static Boolean isBlocked(@NotNull String username) {
        String query = String.format("select blocked from user where username = '%s'", username);
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return Integer.parseInt(rs.getString(1)) == 1;
            }
        } catch (SQLException e) {
            LOGGER.error("isBlocked query error." + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.error("Sql connection closed error." + e);
            }
        }
        return false;
    }

    private static Integer getIdByUsername(String username) {
        String query = String.format("select id from user where username = '%s'", username);
        try {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return Integer.parseInt(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.error("getIdByUsername query error." + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.error("Sql connection closed error." + e);
            }
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
        } catch (java.security.NoSuchAlgorithmException ignored) { }
        return null;
    }

    public static void addUser(String username, String login, String passwordHash) {
        String query = "insert into user (username, login, password_hash, admin) values (?, ?, ?, ?)";
        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, login);
            stmt.setString(3, passwordHash);
            stmt.setString(4, "0");
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("getIdByUsername query error." + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.error("Sql connection closed error." + e);
            }
        }
    }

    public static ArrayList<String> getUserBlacklist(String username) {
        ArrayList<String> blockedUsernameList = new ArrayList<>();
        String query = String.format(
                "select u2.username from blacklist b " +
                        "inner join user u1 on b.user_id = u1.id " +
                        "inner join user u2 on b.blocked_user_id = u2.id " +
                        "where u1.username = '%s'", username);
        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                blockedUsernameList.add(rs.getString(1));
            }
            return blockedUsernameList;
        } catch (SQLException e) {
            LOGGER.error("getUserBlacklist query error." + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.error("Sql connection closed error." + e);
            }
        }
        return null;
    }

    public static ArrayList<String> getBlockedUsers() {
        ArrayList<String> blockedUsers = new ArrayList<>();
        String query = "select username from user where blocked = 1";
        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                blockedUsers.add(rs.getString(1));
            }
            return blockedUsers;
        } catch (SQLException e) {
            LOGGER.error("getBlockedUsers query error." + e);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.error("Sql connection closed error." + e);
            }
        }
        return null;
    }

    public static void addToBlacklist(String fromUser, String toUser) {
        Integer userId = getIdByUsername(fromUser);
        Integer blockedUserId = getIdByUsername(toUser);
        if (userId != null && blockedUserId != null) {
            String query = "insert into blacklist (user_id, blocked_user_id) values (?, ?)";
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setInt(2, blockedUserId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("addToBlacklist query error." + e);
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOGGER.error("Sql connection closed error." + e);
                }
            }
        }
    }

    public static void removeFromBlacklist(String fromUser, String toUser) {
        Integer userId = getIdByUsername(fromUser);
        Integer blockedUserId = getIdByUsername(toUser);
        if (userId != null && blockedUserId != null) {
            String query = "delete from blacklist where user_id = ? and blocked_user_id = ?";
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setInt(2, blockedUserId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("removeFromBlacklist query error." + e);
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOGGER.error("Sql connection closed error." + e);
                }
            }
        }
    }

    public static void changeLogin(String username, String login) {
        Integer userId = getIdByUsername(username);
        if (userId != null && login != null) {
            String query = "update user set login = ? where id = ?";
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, login);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("changeLogin query error." + e);
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOGGER.error("Sql connection closed error." + e);
                }
            }
        }
    }

    public static void blockUser(String username) {
        Integer userId = getIdByUsername(username);
        if (userId != null) {
            String query = "update user set blocked = ? where id = ?";
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, "1");
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("blockUser query error." + e);
            } finally {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOGGER.error("Sql connection closed error." + e);
                }
            }
        }
    }
}
