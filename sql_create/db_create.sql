DROP DATABASE IF EXISTS student_management;

CREATE DATABASE student_management;

USE student_management;

-- postgresql syntax
CREATE TABLE students (
    student_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT,
    major VARCHAR(100),
    avatar_url VARCHAR(255)，
    count_attendance INT DEFAULT 0，
    count_absence INT DEFAULT 0,
    count_late INT DEFAULT 0
);