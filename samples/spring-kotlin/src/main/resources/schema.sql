CREATE TABLE IF NOT EXISTS `user` (
    `username` VARCHAR(20) NOT NULL,
    `password` VARCHAR(100) NULL,
    PRIMARY KEY (`username`)
);

INSERT INTO `user` (username, password) VALUES ('Bob', 'password1');
INSERT INTO `user` (username, password) VALUES ('Alice', 'password2');
