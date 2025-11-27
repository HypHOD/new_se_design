package org.example;

public class TryDb {
    public static void main(String[] args) {
        StudentCountController dbController = new StudentCountController();
        StudentRandomCount randomCount = new StudentRandomCount(dbController);
        randomCount.randomQueryStudent(0, 2, 2);
    }

}
