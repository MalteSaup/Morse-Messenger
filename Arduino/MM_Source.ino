#define BTSerial Serial1


// Sendervariablen:
#define txPin 2       // Auf welchem Pin senden wir?
String input;         // String der über BT vom Handy kommt
String output;        // Zeichenkette des aktuell zu sendenden Buchstabens
int cycletime = 40;   // Länge eines Punktes in ms
String lastInput;     // Nötig für die Repeat-Funktionalität

// Empfängervariablen
#define rxPin A3            // Auf welchem Pin empfangen wir?
int lastRx = 0;             // Bool der letzten Flanke (HIGH=1/LOW=0)
int currRx = 0;             // Bool der aktuellen Flanke (HIGH=1/LOW=0)
int lastCharBegin = 0;      // Wann hat der letzte Charakter angefangen? In ms
int lastBreakBegin = 0;     // Wann hat die letzte Pause angefangen? in ms
int charTime = 0;           // Länge des letzten Charakters in ms
unsigned int breakTime = 0; // Länge der letzten Pause in ms (Kann lang werden)
String receivedLetter = {}; // Bisher empfangene Zeichenkette des aktuellen Buchstabens
int clk = 0;                // Der aktuell vermutete Sendetakt des Senders (wird laufend neu berechnet)
int currRxAnalog = 0;       // Aktueller analoger Wert des Lichtsensors
int lastRxAnalog = 0;       // Letzter analoger Wert des Lichtsensors, genutzt für Smoothing
int minRx = 1000;           // aktuelle Untergrenze der Hüllkurve
int maxRx = 1;              // aktuelle Obergrenze der Hüllkurve
int divider = 0;            // Mitte von minRx und MaxRx. Alles größer ist HIGH, alles niedriger ist LOW
String Nachricht = {};      // Die insgesamt empfangene Nachricht.
String cycleSet;            // Temp-Variable

long timeOfLastReceive = 0;
long timeOfLastAck = 0;
boolean recActive;
boolean ackActive;

int multiTemp = 0;          // Temp-Variable

// Alphabet und Morsecode
//-----------------------------------------------------------------
char *letterCode[] = {"-.-.-", ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".-.-.", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----", ".-.-.-", "--..--", "..--..", "-....-", "...-."};
char letter[] = { ' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '>', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '.', ',', '?', '|', ':'};
//-----------------------------------------------------------------


void setup() {
  Serial.begin(9600);
  Serial.println("begin");
  Serial.setTimeout(3);
  BTSerial.setTimeout(3);

  pinMode(txPin, OUTPUT);
  pinMode(4, INPUT_PULLUP);
  BTSerial.begin(9600);

}





void loop() {
  updateSensor();
  updateBT();

  if (lastRx != currRx) {                                     // Wenn sich was im Signal verändert hat, starte interpret()
    timeOfLastReceive = millis();
    interpret();
    lastRx = currRx;
  }

  if ((input != "") && ((millis() - timeOfLastReceive) > 3000)) {
    wordSend();                                               // Falls wir irgendwas empfangen haben, schicke es mit wordSend()
  }
  checkStatus();                                              // Überprüft in regelmäßigen Abständen die Verbindung
}





void checkStatus() {
  if ((millis() - timeOfLastReceive) < 45000) {
    recActive = HIGH;
  }
  else {
    recActive = LOW;
  }
  if ((millis() - timeOfLastAck) < (50000 + random(2000, 20000))) {
    ackActive = HIGH;
  }
  else {
    ackActive = LOW;
    if ((millis() - timeOfLastReceive) > 5000) {
      charSend(' ');
      charSend('>');
      punkt();
      timeOfLastAck += 10000;
    }
  }
  digitalWrite(7, recActive);
  digitalWrite(8, ackActive);
}





void updateSensor() {
  lastRxAnalog = currRxAnalog;
  currRxAnalog = analogRead(rxPin);
  minRx++;
  maxRx--;

  if (currRxAnalog < minRx) {        // falls minPeak: Wert aktualisieren, divider aktualisieren
    minRx = currRxAnalog;
    divider = (maxRx + minRx) / 2;
  }
  
  if (currRxAnalog > maxRx) {        // falls maxPeak: Wert aktualisieren, divider aktualisieren
    maxRx = currRxAnalog;
    divider = (maxRx + minRx) / 2;
  }

  if ((currRxAnalog + lastRxAnalog) / 2 > divider + 5 && lastRx == LOW) currRx = HIGH; // divider +5 / -5 um das Grundrauschen des Sensors aufzufangen
  if ((currRxAnalog + lastRxAnalog) / 2 < divider - 5 && lastRx == HIGH) currRx = LOW;
}





void updateBT() {
  if (input != "")  return; //Falls wir noch was zu senden haben (einen repeat z.b.)

  input = BTSerial.readStringUntil("\n");

  if (input == "")  return;

  Serial.print(input);

  // Abfang von Kontrollcharakteren
  //---------------------------------------------------
  if (input.startsWith("CLK:")) {
    cycleSet = input.substring(4);
    cycletime = cycleSet.toInt();
    Serial.print("New Cycletime: ");
    Serial.println(cycletime);
    input = "";
    return;
  }

  if (input.startsWith("OPEN")) {
    Serial.print("OPENING");
    digitalWrite(2, HIGH);
    delay(10000);
    digitalWrite(2, LOW);
    input = "";
    return;
  }
  //---------------------------------------------------
  // Stringvorverarbeitung
  input.toLowerCase();                          // Morse kann nur Kleinbuchstaben
  input = ' ' + input;                          // An den Anfang kommt ein Leerzeichen als Kalibrierung
  input = input.substring(0, input.length() - 1) + '>';
  lastInput = input;
  Serial.println(input);
}





void interpret() {                             // Interpret wird aufgerufen wenn sich was im Signal verändert hat (Wechsel von HIGH/LOW) und interpretiert was das war
  if (lastRx < currRx) {                       // Falls zutreffend sind wir am Zeichenbeginn/Pausenende
    breakTime = millis() - lastBreakBegin;     // BreakTime = Wie lange war die Pause?
    lastCharBegin = millis();

    if (breakTime > 400) {                     // Wenn die Pause länger als 7 Takte war -> Nachrichtende. Alles neu.
      receivedLetter = "";
      Nachricht = "";
      clk = 0;
      Serial.println("NEW MESSAGE:");
    }

    if ((breakTime > 3 * clk)) {               // Check auf Buchstabenende, drei Zeichen Pause. Übersetzung
      translateLetter();
    }
  }

  if (lastRx > currRx) {                       // Falls zutreffend sind wir am Zeichenende/Pausenanfang und können das Zeichen interpretieren.
    charTime = millis() - lastCharBegin;
    lastBreakBegin = millis();

    if (receivedLetter.length() > 6) {         // Failsafe. Buchstaben sind max. 6 Zeichen lang. Falls was längeres auftaucht gibt es Probleme und wir und müssen resetten.
      Serial.print("_");
      receivedLetter = "";
      minRx = 1000;
      maxRx = 1;
    }

    if (charTime >= 2 * clk) {
      clk = charTime / 3;
      receivedLetter += '-';
      Serial.print("-");
    }

    if (charTime < 2 * clk) {
      clk = charTime;
      receivedLetter += '.';
      Serial.print(".");
    }
  }
}





void translateLetter() {                     // Übersetzt das empfangene Nachricht, checkt vorher ob der Kontrollcharakter für "Ende" empfangen wurde
  if (receivedLetter == ".-.-.") {          // '>' Nachrichtenende
    receivedLetter == "";
    received();
    return;
  }

  if (receivedLetter == "-....-") {        // ':' Acknowledge vom Enpfänger
    receivedLetter == "";
    Serial.println("ACK:");
    BTSerial.println("ACK:");
    timeOfLastAck = millis();
    return;
  }

  for (int j = 0; j < (sizeof(letterCode) / sizeof(letterCode[0])); j++) {
    if (receivedLetter == letterCode[j]) {
      Nachricht += letter[j];
      Serial.println(letter[j]);
      receivedLetter = "";
    }
  }
}





void received() {
  if (Nachricht.substring(1, 5) == "usr:") {
    Nachricht = "USR:" + Nachricht.substring(5, Nachricht.length());
  }

  if (Nachricht.startsWith(" ::")) {
    Serial.println("repeat triggered");
    repeat();
    return;
  }

  if (Nachricht != " ") {
    Serial.println(Nachricht);
    BTSerial.println(Nachricht);
  }

  delay(200);
  multiTemp = cycletime;
  cycletime = 80;
  charSend('|');    //Acknowledge
  punkt();
  cycletime = multiTemp;
}





void repeat() {
  Serial.println("REPEATING");
  cycletime = cycletime * 1.5;
  input = lastInput;
  delay(1000);
}





void wordSend() {
  for (int i = 0; i < input.length(); i++) {
    charSend(input.charAt(i));
  }
  punkt();
  input = "";
  Serial.println("SENT:");
  BTSerial.println("SENT:");
}





void charSend(char toSend) {                    // Ich bekomme den zu sendenden Buchstaben
  for (int j = 0; j < sizeof(letter); j++) {    // Buchstaben in Array suchen, entsprechende Zeichenfolge in Output schreiben
    if (toSend == letter[j]) {
      output = letterCode[j];
    }
  }
  for (int k = 0; k <= output.length(); k++) {  // Sendecode
    zPause();
    if (output.charAt(k) == '.') punkt();
    if (output.charAt(k) == '-') strich();
  }
  Serial.print(toSend);
  bPause();
}





void punkt() {
  digitalWrite(txPin, HIGH);
  Serial.print(".");
  delay(cycletime);
  digitalWrite(txPin, LOW);
}

void strich() {
  digitalWrite(txPin, HIGH);
  Serial.print("-");
  delay(cycletime * 3);
  digitalWrite(txPin, LOW);
}

void zPause() {   // Pause zwischen Zeichen
  delay(cycletime);
}

void bPause() {   // Pause zwischen Buchstaben
  Serial.println("/");
  delay(cycletime * 3);
}
