DROP TABLE IF EXISTS diversion_record;
DROP TABLE IF EXISTS validation_submission;
DROP TABLE IF EXISTS validation_item;
DROP TABLE IF EXISTS daily_task;
DROP TABLE IF EXISTS chapter_review_record;
DROP TABLE IF EXISTS chapter_material;
DROP TABLE IF EXISTS chapter_step;
DROP TABLE IF EXISTS learning_chapter;
DROP TABLE IF EXISTS knowledge_node;
DROP TABLE IF EXISTS learning_goal;
DROP TABLE IF EXISTS learner_user;

CREATE TABLE IF NOT EXISTS learner_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    feishu_open_id VARCHAR(64) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS learning_goal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    topic VARCHAR(200) NOT NULL,
    target_level VARCHAR(100),
    daily_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS learner_ai_provider_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    api_key_ciphertext TEXT NOT NULL,
    api_key_masked VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_active TINYINT(1) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    goal_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    node_name VARCHAR(100) NOT NULL,
    sequence_no INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS learning_chapter (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goal_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL,
    chapter_no INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS chapter_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chapter_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    instruction_text TEXT,
    status VARCHAR(20) NOT NULL,
    feedback_note TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS chapter_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chapter_id BIGINT NOT NULL,
    material_type VARCHAR(30) NOT NULL,
    file_path VARCHAR(300),
    content_text TEXT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS chapter_review_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chapter_id BIGINT NOT NULL,
    review_status VARCHAR(20) NOT NULL,
    last_reviewed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS daily_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL,
    chapter_id BIGINT NULL,
    task_date DATE NOT NULL,
    step_type VARCHAR(30) NULL,
    step_order INT NULL,
    goal_text VARCHAR(500) NOT NULL,
    study_material TEXT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS validation_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL,
    round_no INT NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    difficulty_level VARCHAR(20),
    question_text TEXT NOT NULL,
    answer_key TEXT,
    quality_status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS validation_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    submitted_answer TEXT,
    is_correct TINYINT(1),
    submitted_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS diversion_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    result_code VARCHAR(30) NOT NULL,
    reason_text VARCHAR(500),
    created_at DATETIME NOT NULL
);
