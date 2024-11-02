package org.example.app;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public abstract class Animal {
	private final String name;
	private List<String> skills;
	private final Date birthDate;

	public Animal(String name, List<String> skills, Date birthDate) {
		this.name = name;
		this.skills = new ArrayList<>(skills);
		this.birthDate = birthDate;
	}

	public String getName() {
		return name;
	}

	public List<String> getSkills() {
		return new ArrayList<>(skills);
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void addSkill(String newSkill) {
		skills.add(newSkill);
	}

	public abstract String getTableName();
	public abstract String getAnimalClass();

	public abstract void displayCommands();
	public abstract void teachNewCommand(String command);

	public void setSkills(List<String> updatedSkills) {
		this.skills = updatedSkills;
	}
}
