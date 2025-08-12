// Connect to Mosquitto server, subscribe and receive messages on topic 'simon/game/#'
// https://github.com/mqttjs/MQTT.js
const client = mqtt.connect('ws://localhost:9001', {
    username: 'your_mqtt_username',
    password: 'your_mqtt_password'
});

client.on('connect', () => {
    console.log('✅ Connected to MQTT broker');
    client.subscribe('simon/game/#', err => {
        if (err) console.error('❌ Subscribe Fehler:', err);
    });
    updateLCD("PLAY---","SE","test")
});


function playToneb(frequency, duration = 300) {
    const context= new (window.AudioContext || window.webkitAudioContext)();
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
async function playColorSequence(colors) {
    // LCD-Anzeige aktualisieren
    updateLCD('Playing...', 'Sequence:', colors.join(' > '));

    // Buttons deaktivieren während der Wiedergabe
    document.querySelectorAll('.color-button').forEach(btn => {
        btn.disabled = true;
    });

    for (const color of colors) {
        const button = document.querySelector(`.color-button[data-color="${color}"]`);
        if (button) {
            button.classList.add("active");
            const tone = parseInt(button.getAttribute("data-tone"), 10);
            playToneb(tone);
            await new Promise(resolve => setTimeout(resolve, 800)); // Längere Anzeige
            button.classList.remove("active");
            await new Promise(resolve => setTimeout(resolve, 200));
        }
    }


    // Buttons wieder aktivieren
    document.querySelectorAll('.color-button').forEach(btn => {
        btn.disabled = false;
    });

    updateLCD('Your turn!', 'Repeat the', 'sequence');

}
    function updateLCD(textLine1, textLine2, textLine3) {
    document.querySelectorAll('.lcd-line')[0].textContent = textLine1;
    document.querySelectorAll('.lcd-line')[1].textContent = textLine2;
    document.querySelectorAll('.lcd-line')[2].textContent = textLine3;
}
client.on('message', (topic, message) => {
    const msg = message.toString();
    console.log(`📥 Message received [${topic}]:`, msg);

    try {
        const data = JSON.parse(msg);

        // Sequenz-Anzeige
        if (topic.includes('sequence') && data.sequence) {
            updateLCD(
                `Round ${data.round}`,
                'Memorize:',
                data.sequence.join(' > ')
            );
            playColorSequence(data.sequence.map(c => c.toLowerCase()));
        }

        // Eingabefeedback
        if (topic.includes('input')) {
            const result = JSON.parse(msg);
            const feedback = result.correct ? '✓ Correct!' : '✗ Wrong!';
            updateLCD(
                `Player: ${result.player}`,
                feedback,
                `Round ${data.round}`
            );
        }
    } catch (e) {
        console.error('Error parsing message:', e);
    }
});


