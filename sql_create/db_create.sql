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
    attendance INT DEFAULT 0，
    absence INT DEFAULT 0,
    late INT DEFAULT 0
);

INSERT INTO
    students (
        student_id,
        name,
        age,
        major,
        avatar_url,
        attendance,
        absence,
        late
    )
VALUES (
        '2025001',
        'stu_001',
        '18',
        'cs',
        'https://example.2025001.com',
        0,
        0,
        0
    ),
    (
        '2025001',
        'stu_002',
        '18',
        'cs',
        'https://example.002.com',
        0,
        0,
        0
    ),
    (
        '2025003',
        'stu_3',
        '18',
        'cs',
        'https://example.com',
        0,
        0,
        0
    ),
    (
        '2025004',
        'stu_4',
        '18',
        'cs',
        'https://example.com',
        0,
        0,
        0
    ),
    (
        '2025005',
        'stu_5',
        '18',
        'cs',
        'https://example.com',
        0,
        0,
        0
    ),
    (
        '2025006',
        'stu_6',
        '18',
        'cs',
        'https://example.com',
        0,
        0,
        0
    );