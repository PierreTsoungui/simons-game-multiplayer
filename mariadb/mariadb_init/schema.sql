/*CREATE TABLE IF NOT EXISTS objects (
    id INT AUTO_INCREMENT PRIMARY KEY,
   message VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 );*/

CREATE TABLE IF NOT EXISTS  players
(
    playerId   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    playerName VARCHAR(100) UNIQUE NOT NULL CHECK (TRIM(playerName) <> ''),

    password_hash VARCHAR(255)  NOT NULL CHECK (TRIM(password_hash) <> '')


);
CREATE TABLE game_lock (
                           id INT PRIMARY KEY,
                           is_locked BOOLEAN NOT NULL
);
CREATE TABLE IF NOT EXISTS game
(
    gameId  VARCHAR(50)  PRIMARY KEY ,
    isGameActive BOOLEAN DEFAULT  FALSE,
    startTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    endTime DATETIME DEFAULT NULL
);

INSERT INTO game_lock (id, is_locked) VALUES (1, FALSE);

CREATE TABLE IF NOT EXISTS  highScore
(
    gameId  VARCHAR(50) ,
    score INT DEFAULT 0,
    playerId  INT UNSIGNED,
    duration TIME,
    FOREIGN KEY (playerId) REFERENCES players(playerId) ON DELETE CASCADE,
    PRIMARY KEY (gameId, playerId)
);


 SELECT * from game_lock


 DROP table  highScore

select *
from game_lock

select * FROM  highScore
