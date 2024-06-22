package org.example;
import java.sql.*;
import java.util.Scanner;

public class Main {
    private static final String url = "jdbc:mysql://localhost:3306/librari";
    private static final String user = "root";
    private static final String password = "Porfik 10";

    private static Connection con;
    private static PreparedStatement preparedStatement;
    private Savepoint save;
    static String query;

    public static void main (String[] args){
        try {
            con = DriverManager.getConnection(url, user, password);
            con.setAutoCommit(false); // Выключаем автозафиксацию
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
//            //close connection ,stmt and resultset here
//            try { con.close(); } catch(SQLException se) { /*can't do anything */ }
//            try { stmt.close(); } catch(SQLException se) { /*can't do anything */ }
        }
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Введите число\n " +
                    "1 - Добавить читателя\n" +
                    "2 - Посмотреть читателей\n" +
                    "3 - Добавить книгу\n" +
                    "4 - Посмотреть книгу\n" +
                    "5 - Дать книгу в прокат\n" +
                    "6 - Вернуть книгу\n" +
                    "7 - Посмотреть заимствованные книги");
            query = scanner.nextLine();
            switch (query){
                case "1":
                    System.out.println("Введите имя:");
                    String name = scanner.nextLine();
                    System.out.println("Введите email:");
                    String email = scanner.nextLine();
                    System.out.println("Введите дату рождения:");
                    Date dateOfBirth = Date.valueOf(scanner.nextLine());
                    addUser(name,email,dateOfBirth);
                    break;
                case "2":
                    getAllUsers();
                    break;
                case "3":
                    System.out.println("Введите название книги:");
                    String title = scanner.nextLine();
                    System.out.println("Введите автора:");
                    String author = scanner.nextLine();
                    System.out.println("Введите кол-во страниц:");
                    int pages = scanner.nextInt();
                    addBook(title,author,pages);
                    break;
                case "4":
                    getAllBooks();
                    break;
                case "5":
                    System.out.println("Введите id читателя:");
                    int userId = scanner.nextInt();
                    System.out.println("Введите id книги:");
                    int bookId = scanner.nextInt();
                    borrowBook(userId,bookId);
                    break;
                case "6":
                    System.out.println("Введите id книги:");
                    int borrowingId = scanner.nextInt();
                    returnBook(borrowingId);
                    break;
                case "7":
                    getAllBorrowings();
                    break;
            }
        }
    }
    private static void addUser(String name, String email, Date dateOfBirth){
        query = "insert into users ( `name`, `email`, date_of_birth)  VALUES (?, ?, ?)";

        try {
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setString(1,name);
            preparedStatement.setString(2,email);
            preparedStatement.setDate(3,dateOfBirth);
            preparedStatement.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static void getAllUsers(){
        query = "SELECT * FROM users";


        try (PreparedStatement preparedStatement = con.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            // Обработка результата
            while (resultSet.next()) {
                int id = resultSet.getInt("user_id"); // Пример: предположим, что есть столбец id типа INT
                String name = resultSet.getString("name");
                String email = resultSet.getString("email");
                Date dateOfBirth = resultSet.getDate("date_of_birth");

                // Делайте что-то с полученными данными, например, выводите их на экран
                System.out.println("User ID: " + id + ", Name: " + name + ", Email: " + email + ", Date of Birth: " + dateOfBirth);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving users from database.", e);
        }

    }

    private static void addBook(String title, String author, int pages){
        query = "insert into books (`title`,`author`,pages) values(?,?,?)";
        try {
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setString(1,title);
            preparedStatement.setString(2,author);
            preparedStatement.setInt(3,pages);

            preparedStatement.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void getAllBooks(){
        query = "SELECT * FROM books";

        try (PreparedStatement preparedStatement = con.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            // Обработка результата
            while (resultSet.next()) {
                int id = resultSet.getInt("book_id"); // Пример: предположим, что есть столбец id типа INT
                String title = resultSet.getString("title");
                String author = resultSet.getString("author");
                int pages = resultSet.getInt("pages");
                int available = resultSet.getInt("available");

                // Делайте что-то с полученными данными, например, выводите их на экран
                System.out.println("Book ID: " + id + ", Title: " + title + ", Author: " + author + ", Pages: " + pages + ", Available: " + available + "\n\n");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving users from database.", e);
        }
    }

    private static void borrowBook(int userId, int bookId) {
        String check = "select available from books where book_id = ?";
        query = "INSERT INTO borrowings (`user_id`, `book_id`, `borrowing_date`) VALUES (?, ?, ?)";

        try {
            java.util.Date currentDate = new java.util.Date();
            java.sql.Date sqlDate = new java.sql.Date(currentDate.getTime());

            preparedStatement = con.prepareStatement(check);
            preparedStatement.setInt(1,bookId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                if (!resultSet.getBoolean("available")) {
                    System.out.println("Недоступно");
                } else {
                    preparedStatement.setInt(1, userId);

                    preparedStatement = con.prepareStatement(query);
                    preparedStatement.setInt(1, userId);
                    preparedStatement.setInt(2, bookId);
                    preparedStatement.setDate(3, sqlDate);


                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Record inserted successfully.");
                    } else {
                        System.out.println("Failed to insert record.");
                    }

                    query = "UPDATE books SET available = ? WHERE book_id = ?";
                    preparedStatement = con.prepareStatement(query);
                    preparedStatement.setBoolean(1, false);
                    preparedStatement.setInt(2, userId);
                    preparedStatement.executeUpdate();

                    con.commit();
                }
            }
        } catch (SQLIntegrityConstraintViolationException e){
            System.out.println("Такого пользователя/книги нет!!");
        } catch (SQLException e) {
            throw new RuntimeException("Error borrowing book.", e);
        }
    }
    private static void returnBook(int borrowingId){
        query = "update books set available = 1 where book_id = ? ";
        java.util.Date currentDate = new java.util.Date();
        java.sql.Date sqlDate = new java.sql.Date(currentDate.getTime());

        try {
            preparedStatement = con.prepareStatement(query);

            preparedStatement.setInt(1,borrowingId);
            preparedStatement.executeUpdate();

            query = "update borrowings set return_date = ? where borrowing_id = ?";
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setDate(1,sqlDate);
            preparedStatement.setInt(2,borrowingId);
            preparedStatement.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static void getAllBorrowings(){
        query = "select * from books where available = 0";

        try {
            preparedStatement = con.prepareStatement(query);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("book_id"); // Пример: предположим, что есть столбец id типа INT
                String title = resultSet.getString("title");
                String author = resultSet.getString("author");
                int pages = resultSet.getInt("pages");
                int available = resultSet.getInt("available");

                System.out.println("Book ID: " + id + ", Title: " + title + ", Author: " + author + ", Pages: " + pages + ", Available: " + available );
            }
            System.out.println("\n\n");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}