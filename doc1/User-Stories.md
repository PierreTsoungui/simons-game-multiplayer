# Informatics Project "Simon Goes Multiplayer" - Summer Semester 2025

<div style="margin-top: 5em;"></div>

## Mandatory User Stories and Sub-Tasks

- [Gameplay and Game Logic](#gameplay-and-game-logic)
- [Frontend](#frontend)
- [Controller](#controller)

<div style="margin-top: 10em;"></div>

# Gameplay and Game Logic

## 1. Visualization of the Color Sequence

**As a player, I want to see the current color sequence displayed on the NeoPixel LEDs and the Web-Controller so that I can press the colors in the correct order.**

### Sub-Tasks
- **Random Color Sequence Programming**
  - Generate a random color sequence with a specified length.
  - The game is organized in rounds. The first round starts with one color. Each round, a new color is added.
  - Develop control logic for the LEDs and the web-controller UI to display the color sequence sequentially.
- **Game Logic Implementation**
  - Adjust the game logic in the backend to generate and send the color sequence to the controller based on the game state.

## 2. Response to Player Input

**As a player, I want to make my inputs using the colored buttons to mimic the displayed color sequence.**

### Sub-Tasks
- **Button Input Logic**
  - Implement logic to detect and process button presses.
- **Input Verification**
  - Develop a method in the backend to verify whether the pressed colors match the displayed sequence.

## 3. Game Termination on Errors

**As a player, I want to receive immediate feedback if I press the wrong color so that I know I have been eliminated.**

### Sub-Tasks
- **Error Detection and Notification**
  - Develop logic in both the backend and controller (web and hardware) to detect incorrect inputs and notify the player accordingly.

## 4. Timed Player Actions

**As a player, I want clear signals indicating when to start and when the input time has expired to complete my inputs promptly.**

### Sub-Tasks
- **Countdown Implementation**
  - Program a countdown at the start of each round, visible on the OLED display.
- **Timeout Logic**
  - Develop a method in the Web-Controller or Hardware-Controller to detect the end of the input time and automatically disqualify players who are too slow.
- **Performance Time Scoring**
  - Track the time each player takes to complete their input.
  - Add the total move time to the player's performance data.
  - Use this timing information to distinguish players who reached the same round and score, favoring the faster player in the high-score ranking.

## 5. Database Integration for Player Data

**As the backend, I want to store player data such as names, RFID tag numbers, and personal high scores in a MariaDB so that this data can be managed securely and persistently.**

### Sub-Tasks
- **Creation of ER Diagrams**
  - Design and document the database structure using the Entity-Relationship Model (ERM).
- **Database Schema Creation**
  - Design and create the schema for player data in MariaDB.
- **JDBC Integration**
  - Implement the JDBC connection to MariaDB for reading and writing data.

## 6. Synchronous Game Control

**As the backend, I want to ensure that all players receive the same color sequence so that the game is fair and runs synchronously.**

### Sub-Tasks
- **Synchronization Logic**
  - Program backend logic to synchronize color sequences across all connected controllers.
- **Consistency Checking**
  - Test the implementation to ensure that all players indeed receive the same sequence.

<div style="margin-top: 10em;"></div>

# Frontend

## 1. Frontend-based User Registration

**As a player without an RFID card, I need to sign up in the frontend to be able to log in.**

### Sub-Tasks
- **Frontend Registration Interface**
  - Create a user interface in the frontend allowing the creation of a new user account with a username and a password.
- **Backend Password Hashing**
  - The password needs to be hashed before saving it in the database.
- **Validation**
  - In case of errors during registration, validation messages need to be shown.
  - The validation needs to be handled in both the front- and backend.

## 2. Frontend-based Player Login

**As a player without an RFID card, I want to assign my account to a controller via the frontend so that I can participate in the game.**

### Sub-Tasks
- **Frontend Login Interface**
  - Create a user interface in the frontend allowing players without RFID cards to assign their account to an available controller.
  - The available controllers are shown in a drop-down menu.
- **Validation**
  - In case of errors during sign-in, validation messages need to be shown.
  - The validation needs to be handled in both the front- and backend.
- **Backend Assignment Logic**
  - Implement backend logic to process player-to-controller assignments initiated via the frontend.

## 3. Frontend Interface for Game Information

**As a player, I want a clear and intuitive user interface so that I can easily understand game information such as current rounds, winners, and active players.**

### Sub-Tasks
- **Frontend Design**
  - Use the given wireframes as a first design.
  - Design and implement the user interface using HTML/Bootstrap.
- **Data Binding**
  - Develop logic to display game information in the frontend retrieved via REST APIs.

## 4. Frontend-based Web-Controller Creation

**As a player without a controller, I want to use a web-based software controller to take part in the game.**

### Sub-Tasks
- **Frontend Controller**
  - A new web controller should be automatically created upon clicking the drop-down entry.
  - Use the given web-controller template. Feel free to adapt it to your needs.
- **For the general communication between the web controller and backend, use mqtts.js**
- **Backend Controller Creation Logic**
  - If the drop-down for the web-controller is chosen at login, automatically create a web-controller and assign the logged-in user.
  - Update the active players list.
  

## 5. Game Start via Frontend

**As a player, I want to start the game using a button in the frontend so that the start occurs simultaneously and synchronously for all players.**

### Sub-Tasks
- **Frontend Start-Button Implementation**
  - Develop and integrate a start button into the frontend interface.
  - Implement logic to send a start signal to the backend when the button is pressed.
  - The start button should be present for logged-in and logged-out users.
- **Backend Start Logic**
  - Program the backend action to initialize the game when the start signal is received from the frontend.
  - Synchronize the game start across all connected controllers to ensure all players begin at the same time.

<div style="margin-top: 10em;"></div>

# Controller

## 1. Hardware-Controller Initialization and Registration

**As a controller, I want to automatically register with my controller ID at startup so that the backend is aware of my status and availability.**

### Sub-Tasks
- **Controller-ID Registration**
  - Implement logic within the controller to automatically register with the backend using its unique ID upon startup.
- **Backend Registration Management**
  - Develop a method in the backend to process and manage incoming registrations from controllers.

## 2. RFID-based Player Login

**As a player with an RFID card, I want to be able to log in at a controller so that the backend can pair my user ID with the respective controller for the current session.**

### Sub-Tasks
- **RFID Login Implementation**
  - Integrate the RFID reader into the controller software to detect RFID cards.
  - Transmit RFID data to the backend for identification and session pairing.
- **Backend Pairing Logic**
  - Develop backend logic to receive RFID data and perform pairing with the appropriate controller.
- **Frontend Update of Player List**
  - Update the list of available players in the frontend.
  - The user is shown with the paired controller address and its state.
  - Users logged in with the RFID are not shown as "logged in" users in the frontend!

## 3. Display of Meta Information on the Controller

**As a player, I want to see my name and the current points/rounds on the OLED display so that I can track my progress during the game.**

### Sub-Tasks
- **OLED Display Control**
  - Program the control of the OLED display to show text.
  - Implement the display of player name, points, and round.
- **Data Transmission to Display**
  - Develop logic for transmitting relevant data from the backend to the controller over MQTT.

<div style="margin-top: 5em;"></div>

---

<div style="margin-top: 5em;"></div>

# Additional (Optional) User Stories

## Z1. Display of Game and Round Results

**As a player, I want to see clear displays of results after each round and at the end of the game so that I can understand my progress and the outcome of the game.**

### Sub-Tasks
- **Implementation of Result Displays**
  - Develop logic in the backend to calculate game and round results.
  - Display these results in the frontend using dynamic components.
  - Display these results on the OLED Display of the Hardware Controller.

## Z2. Difficulty Settings and Inactivity Timeout

**As a player, I want to be able to adjust the difficulty of the game and have the game automatically pause or end if inactive to ensure fair game balance and an appropriate gameplay experience.**

### Sub-Tasks
- **Difficulty Setting Implementation**
  - Develop a feature in both backend and frontend allowing adjustment of game levels.
- **Inactivity Timeout**
  - Implement timeout logic that automatically pauses or ends the game when no activity is detected.

## Z3. Play Against a Bot

**As a player, I want to be able to play against artificial intelligence to test my skills even without human opponents.**

### Sub-Tasks
- **Bot Implementation**
  - Develop a bot that operates stochastically or through a neural network.
- **Bot Integration into the Game**
  - Integrate the bot into the existing game, including adjustments to the user interface and backend.

## Z4. Swagger Documentation of REST APIs

**As a developer, I want detailed and interactive documentation of the REST APIs to facilitate development and testing.**

### Sub-Tasks
- **Creation of Swagger Documentation**
  - Set up Swagger UI to document the REST APIs and enable interactive testing.

## Z5. Security Against SQL Injection

**As a developer, I want to secure the database queries against SQL injections to ensure the application's security.**

### Sub-Tasks
- **Implementation of Security Measures**
  - Apply best practices and techniques, such as prepared statements, to prevent SQL injections.

## Z6. Take Part as a Guest

**As a guest, I want to watch a game without a controller.**

### Sub-Tasks
- **Frontend UI Expansion**
  - Add an option to log in as a guest.
- **Backend Logic Implementation**
  - The backend needs to handle a guest and let them watch the game without taking part.

## Z7. User Profiles

**As a registered player, I want to change my user details myself with a user profile page.**

### Sub-Tasks
- **Frontend UI Expansion**
  - Add a menu icon to the frontend.
  - Implement a popup window with the given data for the logged-in user.
  - Allow changing personal information except for the username and user ID.
- **Backend Logic Implementation**
  - Add the backend logic to handle database changes for the new user data.
- **Validation**
  - Add input validation to the forms.

## Z8. Global High Score in Frontend and Hardware Controller

**As a player, I want to see the high score of the game.**

### Sub-Tasks
- **Frontend High Score Table**
  - Add a high score table with the player names and the scores.
  - Show the top 10 scores.
- **Database Expansion**
  - Add a database table to save the best scores.
- **Hardware Controller**
  - Show the current high score on the controller after pressing the menu for it.
- **Backend Logic**
  - Update the high score lists for the controller and the frontend.

# Non-Functional Requirements

**The project documentation is to be fully included in the README.md of your GitLab project. The following contents must be documented:**
- Information on how to start up the application.
- ERM diagram including a brief description.
- Description of the RESTful API.
- Listing of fulfilled and unfulfilled requirements/user stories.
- If applicable/implemented: Listing of fulfilled optional user stories.
