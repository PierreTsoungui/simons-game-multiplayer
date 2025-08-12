const controllerIdElement = document.getElementById('controllerId');
const params = new URLSearchParams(window.location.search);
let controllerId = params.get('id');
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
let playerName = null;

let isLoading= true;

document.addEventListener("DOMContentLoaded", () => {
    fetchWaitingAreaData();
    isLoading=false;

    updateLCD('Welcome', 'to', 'Simon', 'our lovely guest😄');
});

function fetchWaitingAreaData() {
    fetch('http://localhost:8080/api/waitingArea')
        .then(response => {
            if (!response.ok) console.log("Fehler beim Abrufen der Daten");
            return response.json();
        })
        .then(data => {


            const tbody = document.getElementById("lobbyTableBody");
            tbody.innerHTML = ""; // leeren
            updateWaitingArea(data.waitingAreaData,true)

        })
        .catch(error => {
            console.error("Fehler beim Laden der Waiting Area:", error);
        });
}
//HANDLE GUEST MODE
fetch('http://localhost:8080/api/guestMode')
    .then(response => {
        if (!response.ok) {
            console.error("Fehler beim Abrufen der Guest Mode Daten");
            return;
        }
        return response.json();
    })
    .then(data => {
        if (data && data.waitingAreaData) {
            console.log("Guest Mode Waiting Area Data:", data.waitingAreaData);
            updateWaitingArea(data.waitingAreaData, true);
        } else {
            console.warn("Keine Daten für den Guest Mode erhalten");
        }
    })
    .catch(error => {
        console.error("Fehler beim Laden der Guest Mode Waiting Area:", error);
    });

// HANDLE GUEST MODE

function  updateWaitingArea(data,isNotMqtt=false){

    const tbody = document.getElementById("lobbyTableBody");
    if(isNotMqtt){
        tbody.innerHTML = "";
    }

    data.forEach(player => {
        const row = document.createElement('tr');

        const nameCell = document.createElement('td');
        nameCell.textContent = player.playerName;
        row.appendChild(nameCell);

        const controllerCell = document.createElement('td');
        controllerCell.textContent = player.controllerId;
        row.appendChild(controllerCell);

        const statusCell = document.createElement('td');
        statusCell.textContent = player.status ? 'ready' : 'not ready';
        row.appendChild(statusCell);

        const roundCell = document.createElement('td');
        roundCell.textContent = player.round;
        row.appendChild(roundCell);

        const timeCell = document.createElement('td');
        timeCell.textContent = player.totalMoveTime;
        row.appendChild(timeCell);

        const scoreCell = document.createElement('td');
        scoreCell.textContent = player.score;
        row.appendChild(scoreCell);

        tbody.appendChild(row);
    });

}
// MQTT
let mqttMessagePrefix = window.__ENV__.MQTT_MESSAGE_PREFIX || 'simon/game/';
const client = mqtt.connect("ws://" + window.__ENV__.MQTT_BROKER_URL + ":" + window.__ENV__.MQTT_BROKER_PORT , {
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
    client.subscribe(mqttMessagePrefix + 'simon/game/events/#', err => {
        if (err) console.error('❌ Subscription Error:', err);
        else console.log('📡 Subscribed to: notifyStatusChange');


    });
})

client.on("message",(topic, payload)=>{
    const msg = JSON.parse(payload.toString());
    console.log(`📥 Message received [${topic}]:`, msg);
    if(topic===mqttMessagePrefix +'simon/game/events/status/changed'){
        const { controllerId, status}= msg
        const rows= document.querySelectorAll("#lobbyTableBody tr")
        rows.forEach(row =>{
            const controllerCell= row.children[1];
            if(controllerCell.textContent===controllerId){
                const statusCell= row.children[2];
                statusCell.textContent= status? "ready":"not ready"
            }
        });

    }else if( topic===mqttMessagePrefix +'simon/game/events/playerJoined'){
        if(isloading){
            return;
        }
        updateWaitingArea(msg.waitingAreaData);
    }
    if(topic===mqttMessagePrefix + 'simon/game/events/players/progress') {
        console.log(`📥 Message received [${topic}]:`, msg);
        const {controllerId, round, status, totalMoveTime, score} = msg

        const rows = document.querySelectorAll("#lobbyTableBody tr")
        rows.forEach(row => {
            const controllerCell = row.children[1];
            if (controllerCell.textContent === controllerId) {
                const statusCell = row.children[2];
                statusCell.textContent = status ? "ready" : "not ready"
                const roundCell= row.children[3]
                roundCell.textContent=round;
                const moveTimeCell= row.children[4]
                moveTimeCell.textContent= totalMoveTime;
                const scoreCell = row.children[5];
                scoreCell.textContent = score || '0'; // Assuming score is part of the message
            }
        });
    }
    if(topic===mqttMessagePrefix+ "simon/game/events/waitingAreaUpdated"){

        const {waitingAreaData} = msg
        updateWaitingArea(waitingAreaData, true);
    }
})

//============================================ Controller Edit Modal ============================================

window.addEventListener("unload", () => {

    const data = JSON.stringify({ controllerId: controllerId });
    //navigator.sendBeacon("http://localhost:8080/api/controller/disconnect", data);
    myPublishMessage("simon/game/disconnect", data, false);
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

    } else if (topic === mqttMessagePrefix + `simon/game/${controllerId}/playerData`) {
        //localStorage.setItem("playerName", msg.name);
        updateLCD('Welcome', 'to', 'Simon', `${msg.playerName}`);
    }else if (topic === mqttMessagePrefix + `simon/game/${controllerId}/playerElimination`) {
        const {round, totalMoveTime, score }=msg
        updateLCD(`Game over!`, `Round: ${round}`, `Total time: ${totalMoveTime}ms`, `Score: ${score}`);

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
    let timeLeft = maxTime;
    updateLCD(`Round: ${currentRound}`, "Your turn!", `Time: ${maxTime}s`, `Left: ${timeLeft}s`);

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
        // updateLCD(`Round: ${currentRound}`, "Your turn!", `Time: ${maxTime}s`, `Left: ${timeLeft}s`);
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


updateLCD('Welcome', 'to', 'Simon', 'our lovely guest😄');