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

CREATE TABLE IF NOT EXISTS gameData (
                                        gameId INT UNSIGNED,
                                        roundNr INT UNSIGNED,
                                        playerId INT UNSIGNED,
                                        score INT UNSIGNED DEFAULT 0,
                                        duration TIME,
                                        PRIMARY KEY (gameId, roundNr, playerId),
                                        FOREIGN KEY (playerId) REFERENCES players(playerId) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS  highScore
(
    gameId    INT,
    bestScore INT DEFAULT 0,
    playerId  INT UNSIGNED,
    FOREIGN KEY (gameId) REFERENCES gameData(gameId),
    FOREIGN KEY (playerId) REFERENCES players(playerId) ON DELETE CASCADE,
    PRIMARY KEY (gameId, playerId)

);


select * from players

