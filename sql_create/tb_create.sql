-- postgresql syntax
CREATE TABLE students (
    student_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT,
    major VARCHAR(100),
    avatar_url VARCHAR(255),
    attendance INT DEFAULT 0,
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
        'https://p.sda1.dev/29/fe1d0dc07782ced2d858390009dba1fe/8887BDF5BCD9261C4A1536487B895A51.jpg',
        0,
        0,
        0
    ),
    (
        '2025002',
        'stu_002',
        '18',
        'cs',
        'https://p.sda1.dev/29/3f4266fb06c3c3e957bcfc4821dc7d02/B6F8E21A627ECB40E54C510F68F612B1.jpg',
        0,
        0,
        0
    ),
    (
        '2025003',
        'stu_3',
        '18',
        'cs',
        'https://p.sda1.dev/29/0b5e3c6d631851dd87e18b5f5c7a7b02/D219CCC36491C45CA0F81E7E0E495D56.jpg',
        0,
        0,
        0
    ),
    (
        '2025004',
        'stu_4',
        '18',
        'cs',
        'https://p.sda1.dev/29/45146c3f1848ec0908aab70881c63978/E330F81AEC18E0D166A93AE4FD527691.jpg',
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
    ),
    (
        '2025007',
        'stu_6',
        '18',
        'cs',
        'https://example.com',
        1,
        1,
        0
    );