const controllerIdElement = document.getElementById('controllerId');
const params = new URLSearchParams(window.location.search);
let controllerId = params.get('id');
let mqttMessagePrefix = window.__ENV__.MQTT_MESSAGE_PREFIX || 'simon/game/';
let isGameActive = false;
let expectedSequence = [];
let playerInput = [];
let isPlayerTurn = false;
let inputStartTime = null;
let inputCompleted = false;
let currentRound = null;
let inputTopics = "simon/game/input";
let maxTime = null;
let countdownInterval = null;

// Generate controller ID if not provided
if (!controllerId) {
    controllerId = 'web-' + Math.random().toString(36).substring(2, 8);
    params.set('id', controllerId);
    window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`);
}
controllerIdElement.innerText = controllerId;

window.addEventListener("unload", () => {
    const data = JSON.stringify({ controllerId: controllerId });
    navigator.sendBeacon("http://localhost:8080/api/controller/disconnect", data);
});

// MQTT client setup
const client = mqtt.connect("ws://" + window.__ENV__.MQTT_BROKER_URL + ":" + window.__ENV__.MQTT_BROKER_PORT, {
    clientId: controllerId,
    username: window.__ENV__.MQTT_USERNAME,
    password: window.__ENV__.MQTT_PASSWORD,
    will: {
        topic: mqttMessagePrefix +"simon/game/disconnect",
        payload: JSON.stringify({ controllerId: controllerId }),
        qos: 1,
        retain: false
    }
});

client.on('connect', () => {
    console.log('✅ Connected to MQTT broker');
    client.subscribe(mqttMessagePrefix + 'simon/game/' + controllerId + '/+', err => {
        if (err) {
            console.error('❌ Subscribe Fehler:', err);
        } else {
            console.log(`📡 Subscribed to: simon/game/${controllerId}/+`);
            myPublishMessage("simon/game/registerController", controllerId,false);
        }
    });
});


client.on('error', (err) => {
    console.error('MQTT Fehler:', err);
});

client.on('close', () => {
    console.log('MQTT Verbindung geschlossen');
});

client.on('offline', () => {
    console.log('MQTT Client offline');
});

// Handle color button clicks
document.querySelectorAll(".color-button").forEach(btn => {
    btn.addEventListener("click", () => {
        if (!isGameActive) {
            const color = btn.getAttribute("data-color");
            if (color === "green") {
                myPublishMessage("simon/game/player/status", "true", true);
            } else if (color === "red") {
                myPublishMessage("simon/game/player/status", "false", true);
            }
            return;
        }
        if (!isPlayerTurn) return;

        const color = btn.getAttribute("data-color");
        playerInput.push(color.toUpperCase());
        verifyPlayerInput();

        btn.classList.add('active');
        playTone(btn.getAttribute("data-tone"));
        setTimeout(() => btn.classList.remove('active'), 300);
    });
});

// Reset input state
function resetInput() {
    if (countdownInterval) {
        clearInterval(countdownInterval);
        countdownInterval = null;
        console.debug("Cleared countdown interval");
    }
    inputCompleted = false;
    inputStartTime = null;
    maxTime = null;
    currentRound = null;
    playerInput = [];
    showCountdownOnDisplay(0);
    console.debug("Input state reset");
}

// Verify player input
function verifyPlayerInput() {
    const currentIndex = playerInput.length - 1;
    if (playerInput[currentIndex] !== expectedSequence[currentIndex]) {
        const inputEndTime = Date.now();
        const inputDuration = inputEndTime - inputStartTime;
        handleWrongInput(inputDuration);
    } else {
        handleCorrectSequence();
    }
}

// Handle wrong input
function handleWrongInput(inputDuration) {
    isGameActive = false;
    isPlayerTurn = false;
    updateLCD('Wrong input!', '', '', 'Game over!');
    flashButtons('#e74c3c');
    sendPlayerInput(playerInput, inputDuration, true);
    resetInput();
}

// Handle correct sequence
function handleCorrectSequence() {
    if (playerInput.length === expectedSequence.length) {
        inputCompleted = true;
        const inputEndTime = Date.now();
        const inputDuration = inputEndTime - inputStartTime;
        console.debug("Input duration:", inputDuration);
        sendPlayerInput(playerInput, inputDuration, false);
        updateLCD('Correct!', 'Next round...', '', '');
        myPublishMessage("simon/game/player/status", "true", true);
        flashButtons('#2ecc71');
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
            showCountdownOnDisplay(0);
        }
    }
}

// Send player input to backend
function sendPlayerInput(inputArray, duration, isFinished) {
    const message = {
        input: inputArray,
        timeInMillis: duration,
        complete: isFinished
    };
    myPublishMessage(inputTopics, JSON.stringify(message), true);
}

// Handle MQTT messages
client.on('message', (topic, messageBuffer) => {
    const raw = messageBuffer.toString();
    let msg;
    try {
        msg = JSON.parse(raw);
    } catch (e) {
        console.error('❌ Fehler beim Parsen der Nachricht:', e);
        return;
    }
    console.debug(`📥 Message received [${topic}]:`, msg);

    if (topic === mqttMessagePrefix + `simon/game/${controllerId}/info`) {
        updateLCD('Game', 'starts', 'in', '3s...');
        console.log('ℹ️ Info:', msg.info);
    } else if (topic === mqttMessagePrefix + `simon/game/${controllerId}/start`) {
        handleTopics(msg);
    } else if (topic === mqttMessagePrefix + `simon/game/${controllerId}/sequence`) {
        handleTopics(msg);
    }
});

// Handle start and sequence topics
function handleTopics(msg) {
    const { round, sequence, inputTimeLimit } = msg;
    if (isGameActive && currentRound === round) {
        console.debug(`Ignoring duplicate round ${round} message`);
        return;
    }
    resetInput(); // Clear previous state
    isGameActive = true;
    currentRound = round;
    expectedSequence = sequence;
    maxTime = Math.floor(inputTimeLimit / 1000);
    playColorSequence(sequence, round);
}

// Play color sequence and start timer
async function playColorSequence(colors, currentRound) {
    isGameActive = true;
    isPlayerTurn = false;

    for (const color of colors) {
        const button = document.querySelector(`.color-button[data-color="${color.toLowerCase()}"]`);
        if (button) {
            button.classList.add("active");
            const tone = parseInt(button.getAttribute("data-tone"), 10);
            playTone(tone);
            await new Promise(resolve => setTimeout(resolve, 400));
            button.classList.remove("active");
            await new Promise(resolve => setTimeout(resolve, 100));
        }
    }

    isPlayerTurn = true;
    playerInput = [];
    let timeLeft = maxTime;
    updateLCD(`Round: ${currentRound}`, "Your turn! Press the colors in order", `Time: ${maxTime}s`, `Left: ${timeLeft}s`);

    // Start countdown timer
    inputStartTime = Date.now();
    //let timeLeft = maxTime; // Local timeLeft to avoid shared state
    console.debug("StartTime:", inputStartTime);
    if (countdownInterval) {
        clearInterval(countdownInterval);
        console.debug("Cleared existing countdown interval");
    }
    countdownInterval = setInterval(() => {
        timeLeft--;
        showCountdownOnDisplay(timeLeft);
        // updateLCD(`Round: ${currentRound}`, "Your turn! Press the colors in order", `Time: ${maxTime}s`, `Left: ${timeLeft}s`);
        if (timeLeft <= 0) {
            clearInterval(countdownInterval);
            countdownInterval = null;
            if (!inputCompleted) {
                console.debug("Input not completed:", inputCompleted);
                handleFailure("Timeout - Eingabe nicht abgeschlossen");
            } else {
                resetInput();
            }
        }
    }, 1000);
}

// Handle failure
function handleFailure(reason) {
    console.log("Spieler hat verloren:", reason);
    isGameActive = false;
    isPlayerTurn = false;
    const inputDuration = Date.now() - inputStartTime;
    sendPlayerInput(playerInput, inputDuration, true);
    resetInput();
    updateLCD(reason, '', '', 'Game over!');
    flashButtons('#e74c3c');
}

// Update LCD display
function updateLCD(textLine1, textLine2, textLine3, textLine4) {
    const lines = document.querySelectorAll('.lcd-line');
    lines[0].textContent = textLine1 || '';
    lines[1].textContent = textLine2 || '';
    lines[2].textContent = textLine3 || '';
    lines[3].textContent = textLine4 || '';
}

// Show countdown on display
function showCountdownOnDisplay(timeLeft) {
    const lines = document.querySelectorAll('.lcd-line');
    lines[3].textContent = timeLeft ? `${timeLeft}s` : '';
}

// Publish MQTT message
function myPublishMessage(topic, message, appendControllerId = false) {
    let fullTopic = mqttMessagePrefix + topic;
    if (appendControllerId) {
        fullTopic += `/${controllerId}`;
    }
    client.publish(fullTopic, message);
    console.debug(`📤 Published to ${fullTopic}:`, message);
}

// Play tone for button press
function playTone(frequency, duration = 300) {
    const context = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = context.createOscillator();
    const gainNode = context.createGain();
    oscillator.type = 'sine';
    oscillator.frequency.value = frequency;
    oscillator.connect(gainNode);
    gainNode.connect(context.destination);
    oscillator.start();
    setTimeout(() => {
        oscillator.stop();
        context.close();
    }, duration);
}

// Flash buttons for feedback
function flashButtons(color) {
    const buttons = document.querySelectorAll('.color-button');
    buttons.forEach(btn => {
        const originalColor = btn.style.backgroundColor;
        btn.style.backgroundColor = color;
        setTimeout(() => {
            btn.style.backgroundColor = originalColor;
        }, 500);
    });
}

// Handle key mappings
const keyMap = {
    q: "green",
    w: "red",
    a: "yellow",
    s: "blue"
};
document.addEventListener("keydown", (event) => {
    const color = keyMap[event.key.toLowerCase()];
    if (color) {
        document.querySelector(`.color-button[data-color="${color}"]`)?.click();
    }
});



updateLCD('Welcome', 'to', 'Simon');

//Handle user's menu
const userMenuButton = document.getElementById('userMenuButton');
const userMenuDropdown = document.getElementById('userMenuDropdown');
const menuPlayerName = document.getElementById('menuPlayerName');
const editProfileBtn = document.getElementById('editProfileBtn');

//We do load the player's name from the local storage here
const playerName = localStorage.getItem("playerName") || 'Username';
console.log(playerName);
userMenuButton.textContent = `👤 ${playerName} ▼`;
menuPlayerName.textContent = playerName;

//We handle drop down here
userMenuButton.addEventListener('click', function(e) {
    e.stopPropagation();// Prevent click from propagating to the document

    userMenuDropdown.style.display = userMenuDropdown.style.display === 'block' ? 'none' : 'block';// handle dropdown visibility
});


// Hide dropdown when clicking outside
document.addEventListener('click', function() {
    userMenuDropdown.style.display = 'none';
});

// Edit profile button
editProfileBtn.addEventListener('click', function(e) {
    e.stopPropagation(); // Prevent click from propagating to the document
    const oldModal = document.getElementById('editProfileModal');
    if (oldModal) oldModal.remove(); // Remove old modal if it exists


    // Modal HTML with password fields
    const modalHtml = `
<div id="editProfileModal" style="position:fixed;top:0;left:0;width:100vw;height:100vh;background:#0006;display:flex;align-items:center;justify-content:center;z-index:200;">
  <div style="background:#fff;padding:2em 2em 1em 2em;border-radius:10px;min-width:300px;box-shadow:0 4px 32px #0004;position:relative; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color:#000;">
    <h2 style="margin-top:0; margin-bottom:1em;">Edit Profile</h2>
    <form id="profileForm" style="display:flex;flex-direction:column;">
      
      <label for="editPassword" style="font-weight:600;">New Password:</label>
      <input type="password" id="editPassword" name="editPassword" placeholder="Leave blank to keep current" style="width:100%;padding:0.5em;margin-bottom:1em; border:1px solid #2e7d32; border-radius:4px; outline:none;" required>
      
      <label for="editPasswordConfirm" style="font-weight:600;">Confirm New Password:</label>
      <input type="password" id="editPasswordConfirm" name="editPasswordConfirm" placeholder="Repeat new password" style="width:100%;padding:0.5em;margin-bottom:1.5em; border:1px solid #2e7d32; border-radius:4px; outline:none;" required>
      
      <div style="display:flex;justify-content:space-between;">
        <button type="submit" style="
          padding:0.5em 2em;
          background-color: #81c784;
          color: white;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          font-weight: 600;
          transition: background-color 0.3s ease;
        "
        onmouseover="this.style.backgroundColor='#66bb6a'"
        onmouseout="this.style.backgroundColor='#81c784'">
          Save
        </button>
        
        <button type="button" id="closeProfileModal" style="
          padding:0.5em 2em;
          background-color: #a5d6a7;
          color: #2e7d32;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          font-weight: 600;
          transition: background-color 0.3s ease;
        "
        onmouseover="this.style.backgroundColor='#81c784'"
        onmouseout="this.style.backgroundColor='#a5d6a7'">
          Cancel
        </button>
      </div>
    </form>
  </div>
</div>
`;
    document.body.insertAdjacentHTML('beforeend', modalHtml);


    // Handle form submission
    document.getElementById('profileForm').addEventListener('submit', async function (ev) {
        ev.preventDefault();// Prevent default form submission
        const newPassword = document.getElementById('editPassword').value;
        const confirmPassword = document.getElementById('editPasswordConfirm').value;

        if (newPassword || confirmPassword) {
            if (newPassword !== confirmPassword) {
                alert("Passwords do not match!");
                return;
            }
            if (newPassword.length < 4) {
                alert("Password must contain at least 4 characters!");
                return;
            }
        }
        const playerId = localStorage.getItem('playerId');
        if (!playerId) {
            alert("No player is logged in!");
        }

        //Building the different payloads for the transactions
        const payload = {id: playerId, password: newPassword};

        myPublishMessage("simon/game/players/updatePassword",JSON.stringify(payload), false);

    });
    // Close modal button
    document.getElementById('closeProfileModal').addEventListener('click', function () {
        document.getElementById('editProfileModal').remove(); // Remove modal
    });
})