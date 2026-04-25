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
