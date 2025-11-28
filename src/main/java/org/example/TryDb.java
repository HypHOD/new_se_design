package org.example;

public class TryDb {
    public static void main(String[] args) {
        StudentQueryController dbController = new StudentQueryController();
        StudentRandomQuery randomCount = new StudentRandomQuery(dbController);
        randomCount.randomQueryStudent(0, 2, 4);
    }

}
