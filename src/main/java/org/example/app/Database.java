package org.example.app;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Database {
	private static final String DATABASE_NAME = "human_friends";
	private static final String CONFIG_FILE_PATH = "src/main/resources/config.properties";
	private static final Map<String, String> TABLE_NAMES = Map.of(
			"собака", "dogs",
			"кот", "cats",
			"хомяк", "hamsters",
			"лошадь", "horses",
			"верблюд", "camels",
			"осел", "donkeys"
	);

	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	private boolean dbDebug;
	private Connection connection;

	public Database() {
		loadConfig();
		initializeDatabase();
	}

	private void loadConfig() {
		Properties properties = new Properties();
		try (FileInputStream input = new FileInputStream(CONFIG_FILE_PATH)) {
			properties.load(input);
			dbUrl = properties.getProperty("db.url");
			dbUser = properties.getProperty("db.user");
			dbPassword = properties.getProperty("db.password");
			dbDebug = Boolean.parseBoolean(properties.getProperty("db.debug", "true"));
		} catch (IOException e) {
			System.err.println("Ошибка загрузки конфигурации: " + e.getMessage());
		}
	}

	void initializeDatabase() {
		try {
			connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
			try (Statement statement = connection.createStatement()) {

				if (dbDebug) {
					statement.executeUpdate("DROP DATABASE IF EXISTS `" + DATABASE_NAME + "`;");
					System.out.println("База данных сброшена.");
				}

				statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DATABASE_NAME + "`;");
				System.out.println("База данных создана.");

				connection = DriverManager.getConnection(dbUrl + DATABASE_NAME, dbUser, dbPassword);
				System.out.println("Подключено к базе данных " + DATABASE_NAME);

				createTables();
			}
		} catch (SQLException e) {
			System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
		}
	}

	private void createTables() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS `animals` (
					id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY UNIQUE,
					animals_class VARCHAR(30)
				);
			""");

			for (String tableName : TABLE_NAMES.values()) {
				createAnimalTable(statement, tableName);
				createAnimalSkillsTable(statement, tableName);
			}

			statement.executeUpdate("""
				INSERT INTO `animals` (`id`, `animals_class`)
				VALUES (1, 'pet'), (2, 'pack')
				ON DUPLICATE KEY UPDATE id=id;
			""");

			System.out.println("Таблицы успешно созданы.");
		}
	}

	private void createAnimalTable(Statement statement, String tableName) throws SQLException {
		String sql = String.format("""
			CREATE TABLE IF NOT EXISTS %s (
				id INT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
				name VARCHAR(50) NOT NULL UNIQUE,
				birth_date DATE NOT NULL,
				animal_class_id INT UNSIGNED NOT NULL,
				FOREIGN KEY (animal_class_id) REFERENCES animals (id) ON DELETE CASCADE
			);
		""", tableName);

		statement.executeUpdate(sql);
	}

	private void createAnimalSkillsTable(Statement statement, String animalTableName) throws SQLException {
		String skillsTableName = animalTableName + "_skills";
		String sql = String.format("""
        CREATE TABLE IF NOT EXISTS %s (
            id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY UNIQUE,
            animal_id INT UNSIGNED NOT NULL,
            skill VARCHAR(100) NOT NULL,
            FOREIGN KEY (animal_id) REFERENCES %s (id) ON DELETE CASCADE
        );
    """, skillsTableName, animalTableName);

		statement.executeUpdate(sql);
	}


	public void addNewAnimal(Animal animal) throws SQLException {
		String tableName = animal.getTableName();
		String insertAnimalQuery = "INSERT INTO " + tableName + " (name, birth_date, animal_class_id) VALUES (?, ?, ?)";

		int animalClassId = getAnimalClassId(animal.getAnimalClass());

		try (PreparedStatement animalStatement = connection.prepareStatement(insertAnimalQuery, Statement.RETURN_GENERATED_KEYS)) {
			animalStatement.setString(1, animal.getName());
			animalStatement.setDate(2, new java.sql.Date(animal.getBirthDate().getTime()));
			animalStatement.setInt(3, animalClassId);
			animalStatement.executeUpdate();

			try (ResultSet generatedKeys = animalStatement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					int animalId = generatedKeys.getInt(1);
					addSkillsForAnimal(tableName, animalId, animal.getSkills());
				}
			}
			System.out.println("Животное успешно добавлено");
		}
	}


	private void addSkillsForAnimal(String tableName, int animalId, List<String> skills) throws SQLException {
		String skillsTableName = tableName + "_skills";
		String insertSkillQuery = "INSERT INTO " + skillsTableName + " (animal_id, skill) VALUES (?, ?)";

		try (PreparedStatement skillStatement = connection.prepareStatement(insertSkillQuery)) {
			for (String skill : skills) {
				skill = skill.trim();
				if (!skill.isEmpty()) {
					skillStatement.setInt(1, animalId);
					skillStatement.setString(2, skill);
					skillStatement.addBatch();
				}
			}
			skillStatement.executeBatch();
		}
	}


	public int getAnimalClassId(String animalClassName) throws SQLException {
		String query = "SELECT id FROM animals WHERE animals_class = ?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setString(1, animalClassName);
			ResultSet resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				return resultSet.getInt("id");
			} else {
				throw new SQLException("Класс животного не найден: " + animalClassName);
			}
		}
	}

	public void displayAllAnimals() throws SQLException {
		String query = TABLE_NAMES.values().stream()
				.map(tableName -> String.format("SELECT '%s' AS animal_class, name AS animal_name FROM %s", tableName, tableName))
				.collect(Collectors.joining(" UNION ALL "));

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(query)) {

			Map<String, List<String>> animalsMap = new HashMap<>();
			int totalAnimalsCount = 0;

			while (resultSet.next()) {
				String animalClass = resultSet.getString("animal_class");
				String animalName = resultSet.getString("animal_name");
				animalsMap.computeIfAbsent(animalClass, k -> new ArrayList<>()).add(animalName);
				totalAnimalsCount++;
			}

			if (animalsMap.isEmpty()) {
				System.out.println("В базе данных нет животных.");
			} else {
				animalsMap.forEach((className, names) ->
						System.out.println(className + ": " + String.join(", ", names))
				);
				System.out.println("Общее количество животных: " + totalAnimalsCount);
			}
		}
	}


	public void displayAnimalCommands(String animalType, String animalName) throws SQLException {
		String tableName = TABLE_NAMES.get(animalType.toLowerCase());
		if (tableName == null) {
			System.out.println("Неизвестный тип животного: " + animalType);
			return;
		}

		String animalQuery = "SELECT id FROM " + tableName + " WHERE name = ?";
		try (PreparedStatement animalStmt = connection.prepareStatement(animalQuery)) {
			animalStmt.setString(1, animalName);
			try (ResultSet animalResult = animalStmt.executeQuery()) {
				if (animalResult.next()) {
					int animalId = animalResult.getInt("id");

					String skillsTableName = tableName + "_skills";
					String skillsQuery = "SELECT skill FROM " + skillsTableName + " WHERE animal_id = ?";
					try (PreparedStatement skillsStmt = connection.prepareStatement(skillsQuery)) {
						skillsStmt.setInt(1, animalId);
						try (ResultSet skillsResult = skillsStmt.executeQuery()) {
							List<String> skills = new ArrayList<>();
							while (skillsResult.next()) {
								skills.add(skillsResult.getString("skill"));
							}

							if (skills.isEmpty()) {
								System.out.println("У животного " + animalName + " нет команд.");
							} else {
								System.out.println("Команды для " + animalName + ": " + String.join(", ", skills));
							}
						}
					}
				} else {
					System.out.println("Животное с именем " + animalName + " не найдено.");
				}
			}
		}
	}

	public void addCommandForAnimal(String animalType, String animalName, String newCommand) throws SQLException {
		String tableName = TABLE_NAMES.get(animalType.toLowerCase());
		if (tableName == null) {
			System.out.println("Неизвестный тип животного: " + animalType);
			return;
		}

		String animalQuery = "SELECT id FROM " + tableName + " WHERE name = ?";
		try (PreparedStatement animalStmt = connection.prepareStatement(animalQuery)) {
			animalStmt.setString(1, animalName);
			try (ResultSet animalResult = animalStmt.executeQuery()) {
				if (animalResult.next()) {
					int animalId = animalResult.getInt("id");

					String skillsTableName = tableName + "_skills";
					String insertSkillQuery = "INSERT INTO " + skillsTableName + " (animal_id, skill) VALUES (?, ?)";
					try (PreparedStatement skillStmt = connection.prepareStatement(insertSkillQuery)) {
						skillStmt.setInt(1, animalId);
						skillStmt.setString(2, newCommand);
						skillStmt.executeUpdate();
						System.out.println("Команда успешно добавлена для " + animalName + ".");
					}
				} else {
					System.out.println("Животное с именем " + animalName + " не найдено.");
				}
			}
		}
	}


	public Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
		}
		return connection;
	}

	public void closeConnection() {
		if (connection != null) {
			try {
				connection.close();
				System.out.println("Соединение с базой данных закрыто.");
			} catch (SQLException e) {
				System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
			}
		}
	}
}

