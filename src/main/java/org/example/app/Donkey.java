package org.example.app;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Donkey extends Animal {
	public Donkey(String name, List<String> skills, Date birthDate) {
		super(name, skills, birthDate);
	}

	@Override
	public String getTableName() {
		return "donkeys";
	}

	@Override
	public String getAnimalClass() {
		return "pet";
	}

	@Override
	public void displayCommands() {
		System.out.println("Список команд для осла:");
		System.out.println(String.join(", ", getSkills()));
	}

	@Override
	public void teachNewCommand(String command) {
		List<String> updatedSkills = new ArrayList<>(getSkills());
		updatedSkills.add(command);
		setSkills(updatedSkills);
	}
}
