package org.example.app;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Hamster extends Animal {
	public Hamster(String name, List<String> skills, Date birthDate) {
		super(name, skills, birthDate);
	}

	@Override
	public String getTableName() {
		return "hamsters";
	}

	@Override
	public String getAnimalClass() {
		return "pet";
	}

	@Override
	public void displayCommands() {
		System.out.println("Список команд для хомяка:");
		System.out.println(String.join(", ", getSkills()));
	}

	@Override
	public void teachNewCommand(String command) {
		List<String> updatedSkills = new ArrayList<>(getSkills());
		updatedSkills.add(command);
		setSkills(updatedSkills);
	}
}