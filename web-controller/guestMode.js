const controllerIdElement = document.getElementById('controllerId');
const params = new URLSearchParams(window.location.search);
let controllerId = params.get('id');
let isGameActive = false;
let expectedSequence = [];
let currentRound = null;
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

function  updateWaitingArea(data,isLoading=false){

    const tbody = document.getElementById("lobbyTableBody");
    if(isLoading){
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

//============================================ MQTT ============================================

let mqttMessagePrefix = window.__ENV__.MQTT_MESSAGE_PREFIX || 'simon/game/';
const client = mqtt.connect("ws://" + window.__ENV__.MQTT_BROKER_URL + ":" + window.__ENV__.MQTT_BROKER_PORT , {
    username: window.__ENV__.MQTT_USERNAME,
    password: window.__ENV__.MQTT_PASSWORD,

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
        if(isLoading){
            return;
        }
        console.log("UNten")
        console.log(msg.waitingAreaData)
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

    if (topic === mqttMessagePrefix + `simon/game/events/info`) {
        updateLCD('Game', 'starts', 'in', '3s...');
        console.log('ℹ️ Info:', msg.info);
    } else if (topic === mqttMessagePrefix + `simon/game/events/start`) {
        handleTopics(msg);
    } else if (topic === mqttMessagePrefix + `simon/game/events/sequence`) {
        handleTopics(msg);

    }
    if(topic=== mqttMessagePrefix+`simon/game/events/stop`){
        updateLCD('END ', 'OF ', 'GAME', '');
    }

})



// Handle start and sequence topics
function handleTopics(msg) {
    const { round, sequence, inputTimeLimit } = msg;
    if (isGameActive && currentRound === round) {
        console.debug(`Ignoring duplicate round ${round} message`);
        return;
    }
    resetInput(); // Clear previous state
    currentRound = round;
    expectedSequence = sequence;
    maxTime = Math.floor(inputTimeLimit / 1000);
    playColorSequence(sequence, round);
}

// Reset input state
function resetInput() {
    if (countdownInterval) {
        clearInterval(countdownInterval);
        countdownInterval = null;
        console.debug("Cleared countdown interval");
    }
    maxTime = null;
    currentRound = null;
    showCountdownOnDisplay(0);

}

// Play color sequence and start timer
async function playColorSequence(colors, currentRound) {

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
    if (countdownInterval) {
        clearInterval(countdownInterval);
        console.debug("Cleared existing countdown interval");
    }
    countdownInterval = setInterval(() => {
        timeLeft--;
        showCountdownOnDisplay(timeLeft);
        if (timeLeft <= 0) {
            clearInterval(countdownInterval);
            countdownInterval = null;
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



