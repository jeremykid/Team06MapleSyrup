package ca.ualberta.app.models;

public class User {

	public static boolean loginStatus = false;
	public static QuestionList favorite;
	public static Author author = null;

	public User() {
		favorite = new QuestionList();
	}
}
